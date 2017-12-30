package se.devscout.achievements.server;

import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.SecretValidatorFactory;
import se.devscout.achievements.server.cli.BoostrapDataTask;
import se.devscout.achievements.server.cli.ImportScoutBadgesTask;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.health.IsAliveHealthcheck;
import se.devscout.achievements.server.resources.*;
import se.devscout.achievements.server.resources.authenticator.GoogleTokenAuthenticator;
import se.devscout.achievements.server.resources.authenticator.JwtAuthenticator;
import se.devscout.achievements.server.resources.authenticator.PasswordAuthenticator;
import se.devscout.achievements.server.resources.authenticator.User;
import se.devscout.achievements.server.resources.exceptionhandling.JerseyViolationExceptionMapper;
import se.devscout.achievements.server.resources.exceptionhandling.ValidationExceptionMapper;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.List;

public class AchievementsApplication extends Application<AchievementsApplicationConfiguration> {
    private final HibernateBundle<AchievementsApplicationConfiguration> hibernate = new HibernateBundle<AchievementsApplicationConfiguration>(
            Organization.class,
            Person.class,
            Credentials.class,
            Achievement.class,
            AchievementStep.class,
            AchievementStepProgress.class
    ) {
        public DataSourceFactory getDataSourceFactory(AchievementsApplicationConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    public void run(AchievementsApplicationConfiguration config, Environment environment) throws Exception {
        final SessionFactory sessionFactory = hibernate.getSessionFactory();

        final OrganizationsDao organizationsDao = new OrganizationsDaoImpl(sessionFactory, config.getMaxOrganizationCount());
        final AchievementsDao achievementsDao = new AchievementsDaoImpl(sessionFactory);
        final AchievementStepsDao achievementStepsDao = new AchievementStepsDaoImpl(sessionFactory);
        final AchievementStepProgressDao progressDao = new AchievementStepProgressDaoImpl(sessionFactory);
        final PeopleDao peopleDao = new PeopleDaoImpl(sessionFactory);
        final CredentialsDao credentialsDao = getCredentialsDao(sessionFactory);

        environment.jersey().register(ValidationExceptionMapper.class);
        environment.jersey().register(JerseyViolationExceptionMapper.class);

        final JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(config.getAuthentication().getJwtSigningSecret());

        environment.jersey().register(createAuthFeature(hibernate, credentialsDao, jwtAuthenticator, config.getAuthentication().getGoogleClientId()));

//        environment.jersey().register(RolesAllowedDynamicFeature.class);
        //If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));

        AuthResourceUtil authResourceUtil = new AuthResourceUtil(
                jwtAuthenticator,
                credentialsDao,
                peopleDao,
                organizationsDao,
                new SecretValidatorFactory(config.getAuthentication().getGoogleClientId()));

        environment.jersey().register(new OrganizationsResource(organizationsDao, achievementsDao, authResourceUtil));
        environment.jersey().register(new AchievementsResource(achievementsDao, progressDao));
        environment.jersey().register(new AchievementStepsResource(achievementStepsDao, achievementsDao));
        environment.jersey().register(new AchievementStepProgressResource(achievementStepsDao, achievementsDao, peopleDao, progressDao));
        environment.jersey().register(new PeopleResource(peopleDao, organizationsDao, achievementsDao));
        environment.jersey().register(new MyResource(peopleDao, achievementsDao));
        environment.jersey().register(new StatsResource(organizationsDao));
        environment.jersey().register(new SignInResource(authResourceUtil));

        environment.healthChecks().register("alive", new IsAliveHealthcheck());

        environment.admin().addTask(new BoostrapDataTask(sessionFactory, organizationsDao, peopleDao, achievementsDao, achievementStepsDao));
        environment.admin().addTask(new ImportScoutBadgesTask(sessionFactory, achievementsDao, achievementStepsDao));

        initCorsHeaders(environment);
    }

    protected CredentialsDao getCredentialsDao(SessionFactory sessionFactory) {
        return new CredentialsDaoImpl(sessionFactory);
    }

    public static AuthDynamicFeature createAuthFeature(HibernateBundle<AchievementsApplicationConfiguration> hibernate, CredentialsDao credentialsDao, JwtAuthenticator jwtAuthenticator, String googleClientId) {

        PasswordAuthenticator tokenAuthenticator = new UnitOfWorkAwareProxyFactory(hibernate).create(PasswordAuthenticator.class, CredentialsDao.class, credentialsDao);
        GoogleTokenAuthenticator googleTokenAuthenticator = new UnitOfWorkAwareProxyFactory(hibernate).create(GoogleTokenAuthenticator.class, new Class[]{CredentialsDao.class, String.class}, new Object[]{credentialsDao, googleClientId});

        List<AuthFilter> filters = Lists.newArrayList(
                new BasicCredentialAuthFilter.Builder<User>()
                        .setAuthenticator(tokenAuthenticator)
                        .setPrefix("Basic")
//                        .setRealm("Achievements")
//                        .setAuthorizer(new DefaultAuthorizer())
//                        .setUnauthorizedHandler(new DefaultUnauthorizedHandler())
                        .buildAuthFilter(),
                new OAuthCredentialAuthFilter.Builder<User>()
                        .setAuthenticator(jwtAuthenticator)
                        .setPrefix("JWT")
                        .buildAuthFilter(),
                new OAuthCredentialAuthFilter.Builder<User>()
                        .setAuthenticator(googleTokenAuthenticator)
                        .setPrefix("Google")
                        .buildAuthFilter()
        );
        return new AuthDynamicFeature(new ChainedAuthFilter<>(filters));
    }

    private void initCorsHeaders(Environment env) {
        FilterRegistration.Dynamic filter = env.servlets().addFilter("CORSFilter", CrossOriginFilter.class);
        filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, env.getApplicationContext().getContextPath() + "*");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "Origin, Content-Type, Accept, Authorization");
        filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
    }

    @Override
    public String getName() {
        return "achievements";
    }

    @Override
    public void initialize(Bootstrap<AchievementsApplicationConfiguration> bootstrap) {
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(new AssetsBundle("/assets/", "/"));
        bootstrap.addBundle(new MigrationsBundle<AchievementsApplicationConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(AchievementsApplicationConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });

    }

    public static void main(String[] args) throws Exception {
        new AchievementsApplication().run(args);
    }

}
