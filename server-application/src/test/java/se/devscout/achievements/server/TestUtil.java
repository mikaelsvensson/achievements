package se.devscout.achievements.server;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.User;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.model.Achievement;

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
        return ResourceTestRule.builder()
                //            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addProvider(AchievementsApplication.createAuthFeature(mockHibernateBundle(), credentialsDao))
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
}
