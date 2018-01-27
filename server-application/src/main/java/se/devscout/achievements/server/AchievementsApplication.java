package se.devscout.achievements.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.email.EmailIdentityProvider;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtSignUpTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceImpl;
import se.devscout.achievements.server.auth.openid.GoogleTokenValidator;
import se.devscout.achievements.server.auth.openid.MicrosoftTokenValidator;
import se.devscout.achievements.server.auth.openid.OpenIdIdentityProvider;
import se.devscout.achievements.server.cli.BoostrapDataTask;
import se.devscout.achievements.server.cli.ImportScoutBadgesTask;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.health.IsAliveHealthcheck;
import se.devscout.achievements.server.mail.SmtpSender;
import se.devscout.achievements.server.resources.*;
import se.devscout.achievements.server.resources.auth.*;
import se.devscout.achievements.server.resources.exceptionhandling.CallbackResourceExceptionMapper;
import se.devscout.achievements.server.resources.exceptionhandling.JerseyViolationExceptionMapper;
import se.devscout.achievements.server.resources.exceptionhandling.ValidationExceptionMapper;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.client.ClientBuilder;
import java.sql.Connection;
import java.util.EnumSet;
import java.util.List;

public class AchievementsApplication extends Application<AchievementsApplicationConfiguration> {
    private final HibernateBundle<AchievementsApplicationConfiguration> hibernate = new HibernateBundle<AchievementsApplicationConfiguration>(
            Organization.class,
            Person.class,
            Group.class,
            GroupMembership.class,
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

        if (config.isAutoMigrateDatabase()) {
            ManagedDataSource ds = config.getDataSourceFactory().build(environment.metrics(), "migrations");
            try (Connection connection = ds.getConnection()) {
                Liquibase migrator = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
                migrator.update("");
            }
        }

        final OrganizationsDao organizationsDao = new OrganizationsDaoImpl(sessionFactory, config.getMaxOrganizationCount());
        final AchievementsDao achievementsDao = new AchievementsDaoImpl(sessionFactory);
        final AchievementStepsDao achievementStepsDao = new AchievementStepsDaoImpl(sessionFactory);
        final AchievementStepProgressDao progressDao = new AchievementStepProgressDaoImpl(sessionFactory);
        final PeopleDao peopleDao = new PeopleDaoImpl(sessionFactory);
        final GroupsDao groupsDao = new GroupsDaoImpl(sessionFactory);
        final GroupMembershipsDao membershipsDao = new GroupMembershipsDaoImpl(sessionFactory);
        final CredentialsDao credentialsDao = getCredentialsDao(sessionFactory);

        environment.jersey().register(new CallbackResourceExceptionMapper(config.getGuiApplicationHost()));
        environment.jersey().register(ValidationExceptionMapper.class);
        environment.jersey().register(JerseyViolationExceptionMapper.class);

        final JwtTokenService jwtTokenService = new JwtTokenServiceImpl(config.getAuthentication().getJwtSigningSecret());
        final JwtSignInTokenService signInTokenService = new JwtSignInTokenService(jwtTokenService);
        final JwtSignUpTokenService signUpTokenService = new JwtSignUpTokenService(jwtTokenService);

        environment.jersey().register(createAuthFeature(hibernate, credentialsDao, jwtTokenService));

        environment.jersey().register(RolesAllowedDynamicFeature.class);
        //If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));

        environment.jersey().register(new OrganizationsResource(organizationsDao, achievementsDao));
        environment.jersey().register(new AchievementsResource(achievementsDao, progressDao));
        environment.jersey().register(new AchievementStepsResource(achievementStepsDao, achievementsDao));
        environment.jersey().register(new AchievementStepProgressResource(achievementStepsDao, achievementsDao, peopleDao, progressDao));
        environment.jersey().register(new PeopleResource(peopleDao, organizationsDao, achievementsDao, environment.getObjectMapper(), groupsDao, membershipsDao));
        environment.jersey().register(new GroupsResource(groupsDao, organizationsDao, achievementsDao, environment.getObjectMapper()));
        environment.jersey().register(new GroupMembershipsResource(groupsDao, peopleDao, organizationsDao, membershipsDao));
        environment.jersey().register(new MyResource(peopleDao, groupsDao, achievementsDao));
        environment.jersey().register(new StatsResource(organizationsDao));
        environment.jersey().register(new SignInResource(signInTokenService, credentialsDao));
        environment.jersey().register(new ExternalIdpResource(
                ImmutableMap.of("google",
                        new OpenIdIdentityProvider(
                                "https://accounts.google.com/o/oauth2/v2/auth",
                                config.getAuthentication().getGoogleClientId(),
                                config.getAuthentication().getGoogleClientSecret(),
                                ClientBuilder.newClient(),
                                "https://www.googleapis.com/oauth2/v4/token",
                                new GoogleTokenValidator(config.getAuthentication().getGoogleClientId())),
                        "microsoft",
                        new OpenIdIdentityProvider(
                                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                                config.getAuthentication().getMicrosoftClientId(),
                                config.getAuthentication().getMicrosoftClientSecret(),
                                ClientBuilder.newClient(),
                                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                                new MicrosoftTokenValidator()),
                        "email",
                        new EmailIdentityProvider(jwtTokenService, new SmtpSender(config.getSmtp()), config.getGuiApplicationHost())),
                credentialsDao,
                peopleDao,
                organizationsDao,
                config.getGuiApplicationHost(),
                config.getServerApplicationHost(),
                signInTokenService,
                signUpTokenService));

        environment.healthChecks().register("alive", new IsAliveHealthcheck());

        environment.admin().addTask(new BoostrapDataTask(sessionFactory, organizationsDao, peopleDao, achievementsDao, achievementStepsDao));
        environment.admin().addTask(new ImportScoutBadgesTask(sessionFactory, achievementsDao, achievementStepsDao));

        initCorsHeaders(environment);
    }

    protected CredentialsDao getCredentialsDao(SessionFactory sessionFactory) {
        return new CredentialsDaoImpl(sessionFactory);
    }

    public static AuthDynamicFeature createAuthFeature(HibernateBundle<AchievementsApplicationConfiguration> hibernate, CredentialsDao credentialsDao, JwtTokenService jwtTokenService) {

        PasswordAuthenticator tokenAuthenticator = new UnitOfWorkAwareProxyFactory(hibernate).create(PasswordAuthenticator.class, CredentialsDao.class, credentialsDao);
        final JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(new JwtSignInTokenService(jwtTokenService));

        final Authorizer<User> authorizer = (user, role) -> user.getRoles().contains(role);

        List<AuthFilter> filters = Lists.newArrayList(
                new BasicCredentialAuthFilter.Builder<User>()
                        .setAuthenticator(tokenAuthenticator)
                        .setPrefix("Basic")
//                        .setRealm("Achievements")
                        .setAuthorizer(authorizer)
//                        .setUnauthorizedHandler(new DefaultUnauthorizedHandler())
                        .buildAuthFilter(),
                new OAuthCredentialAuthFilter.Builder<User>()
                        .setAuthenticator(jwtAuthenticator)
                        .setAuthorizer(authorizer)
                        .setPrefix("JWT")
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
        bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));
        bootstrap.addBundle(new MigrationsBundle<AchievementsApplicationConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(AchievementsApplicationConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    public static void main(String[] args) throws Exception {
        new AchievementsApplication().run(args);
    }

}
