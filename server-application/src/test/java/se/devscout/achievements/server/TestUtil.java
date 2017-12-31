package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.resources.authenticator.User;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import java.net.URI;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtil {
    public static final ConfigOverride NO_HBM_2_DDL = ConfigOverride.config("database.properties.hibernate.hbm2ddl.auto", "none");

    public static ConfigOverride getPrivateDatabase() {
        return ConfigOverride.config("database.url",
                "jdbc:h2:mem:unit_test_" + UUID.randomUUID().toString() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    }

    public static ResourceTestRule.Builder resourceTestRule(CredentialsDao credentialsDao) {
        return resourceTestRule(credentialsDao, Boolean.TRUE);
    }

    public static ResourceTestRule.Builder resourceTestRule(CredentialsDao credentialsDao, Boolean followRedirects) {
        final AuthDynamicFeature authFeature = AchievementsApplication.createAuthFeature(
                mockHibernateBundle(),
                credentialsDao,
                mock(JwtTokenService.class),
                "fake_google_client_id");

        return ResourceTestRule.builder()
                .setClientConfigurator(clientConfig -> clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, followRedirects))
                .addProvider(authFeature)
                .addProvider(RolesAllowedDynamicFeature.class)
                .addProvider(new AuthValueFactoryProvider.Binder<>(User.class));
    }

    private static HibernateBundle<AchievementsApplicationConfiguration> mockHibernateBundle() {
        return new HibernateBundle<AchievementsApplicationConfiguration>(Achievement.class) {

            @Override
            public PooledDataSourceFactory getDataSourceFactory(AchievementsApplicationConfiguration achievementsApplicationConfiguration) {
                return mock(PooledDataSourceFactory.class);
            }

            @Override
            public SessionFactory getSessionFactory() {
                final SessionFactory sessionFactory = mock(SessionFactory.class);
                when(sessionFactory.openSession()).thenReturn(mock(Session.class));
                return sessionFactory;
            }

            @Override
            protected String name() {
                return "mocked-bundle";
            }
        };
    }

    static Invocation.Builder request(Client client, String location) {
        return request(client, URI.create(location));
    }

    static Invocation.Builder request(Client client, URI location) {
        return client
                .target(location)
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)));
    }
}
