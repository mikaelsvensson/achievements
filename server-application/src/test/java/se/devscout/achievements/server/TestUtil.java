package se.devscout.achievements.server;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.data.dao.AuditingDao;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.filter.audit.AuditFeature;
import se.devscout.achievements.server.resources.auth.User;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
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
        return resourceTestRule(credentialsDao, followRedirects, mock(AuditingDao.class));
    }

    public static ResourceTestRule.Builder resourceTestRule(CredentialsDao credentialsDao, Boolean followRedirects, AuditingDao auditingDao) {
        final var hibernateBundle = mockHibernateBundle();
        final var authFeature = AchievementsApplication.createAuthFeature(
                hibernateBundle,
                credentialsDao,
                new JwtSignInTokenService(mock(JwtTokenService.class))
        );
        return ResourceTestRule.builder()
                .setClientConfigurator(clientConfig -> clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, followRedirects))
                .addProvider(authFeature)
                .addProvider(new AuditFeature(auditingDao, hibernateBundle))
                .addProvider(RolesAllowedDynamicFeature.class)
                .addProvider(new AuthValueFactoryProvider.Binder<>(User.class));
    }

    private static HibernateBundle<AchievementsApplicationConfiguration> mockHibernateBundle() {
        return new HibernateBundle<>(Achievement.class) {

            @Override
            public PooledDataSourceFactory getDataSourceFactory(AchievementsApplicationConfiguration achievementsApplicationConfiguration) {
                return mock(PooledDataSourceFactory.class);
            }

            @Override
            public SessionFactory getSessionFactory() {
                final var sessionFactory = mock(SessionFactory.class);
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

    static Invocation.Builder request(Client client, String location, HttpAuthenticationFeature auth) {
        return request(client, URI.create(location), auth);
    }

    static Invocation.Builder request(Client client, URI location) {
        return request(client, location, MockUtil.AUTH_FEATURE_EDITOR);
    }

    static Invocation.Builder request(Client client, URI location, HttpAuthenticationFeature auth) {
        return client
                .target(location)
                .register(auth)
                .request();
    }

    static String[] getHttpAuditLog(Client client, int adminPort) {
        var getResponse = request(client, String.format("http://localhost:%d/tasks/http-log", adminPort)).post(null);

        final var dto = getResponse.readEntity(String.class);
        return StringUtils.split(dto, '\n');
    }
}
