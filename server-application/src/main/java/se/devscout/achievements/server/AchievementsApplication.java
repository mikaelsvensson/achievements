package se.devscout.achievements.server;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.*;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.sentry.Sentry;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.api.UnsuccessfulDTO;
import se.devscout.achievements.server.auth.email.EmailIdentityProvider;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.jwt.JwtSignUpTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenService;
import se.devscout.achievements.server.auth.jwt.JwtTokenServiceImpl;
import se.devscout.achievements.server.auth.openid.GoogleTokenValidator;
import se.devscout.achievements.server.auth.openid.MicrosoftTokenValidator;
import se.devscout.achievements.server.auth.openid.OpenIdIdentityProvider;
import se.devscout.achievements.server.cli.BoostrapDataTask;
import se.devscout.achievements.server.cli.HttpAuditTask;
import se.devscout.achievements.server.cli.ImportScoutBadgesTask;
import se.devscout.achievements.server.cli.ImportScouternaBadgesTask;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.filter.audit.AuditFeature;
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
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
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
            AchievementStepProgress.class,
            HttpAuditRecord.class,
            StepProgressAuditRecord.class
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
        final AuditingDao auditingDao = new AuditingDaoImpl(sessionFactory);
        final GroupMembershipsDao membershipsDao = new GroupMembershipsDaoImpl(sessionFactory);
        final CredentialsDao credentialsDao = getCredentialsDao(sessionFactory);

        environment.jersey().register(new CallbackResourceExceptionMapper(config.getGuiApplicationHost()));
        environment.jersey().register(ValidationExceptionMapper.class);
        environment.jersey().register(JerseyViolationExceptionMapper.class);

        final JwtTokenService jwtTokenService = new JwtTokenServiceImpl(config.getAuthentication().getJwtSigningSecret());
        final JwtSignInTokenService signInTokenService = new JwtSignInTokenService(jwtTokenService);
        final JwtSignUpTokenService signUpTokenService = new JwtSignUpTokenService(jwtTokenService);

        environment.jersey().register(createAuthFeature(hibernate, credentialsDao, jwtTokenService));

        initFilterCorsHeaders(environment);

        initSentry();

        if (config.getRateLimiting() != null) {
            initFilterRateLimiter(environment, config.getRateLimiting());
        }
        environment.jersey().register(new AuditFeature(auditingDao, hibernate));

        environment.jersey().register(RolesAllowedDynamicFeature.class);
        //If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));

        final SmtpSender emailSender = new SmtpSender(config.getSmtp());
        final I18n i18n = new I18n("texts.sv.yaml");

        environment.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        environment.jersey().register(new OrganizationsResource(organizationsDao, achievementsDao, peopleDao));
        environment.jersey().register(new AchievementsResource(achievementsDao, progressDao, auditingDao, peopleDao));
        environment.jersey().register(new AchievementStepsResource(achievementStepsDao, achievementsDao));
        environment.jersey().register(new AchievementStepProgressResource(achievementStepsDao, achievementsDao, peopleDao, progressDao));
        environment.jersey().register(new PeopleResource(peopleDao, organizationsDao, achievementsDao, environment.getObjectMapper(), groupsDao, membershipsDao, config.getGuiApplicationHost(), emailSender, i18n));
        environment.jersey().register(new GroupsResource(groupsDao, organizationsDao, achievementsDao, environment.getObjectMapper()));
        environment.jersey().register(new GroupMembershipsResource(groupsDao, peopleDao, organizationsDao, membershipsDao));
        environment.jersey().register(new MyResource(peopleDao, groupsDao, achievementsDao, credentialsDao, emailSender, config.getGuiApplicationHost(), signInTokenService, i18n));
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
                        new EmailIdentityProvider(
                                jwtTokenService,
                                emailSender,
                                config.getGuiApplicationHost(),
                                credentialsDao)),
                credentialsDao,
                peopleDao,
                organizationsDao,
                config.getGuiApplicationHost(),
                config.getServerApplicationHost(),
                signInTokenService,
                signUpTokenService,
                emailSender,
                i18n));

        environment.healthChecks().register("alive", new IsAliveHealthcheck());

        environment.admin().addTask(new BoostrapDataTask(sessionFactory, organizationsDao, peopleDao, achievementsDao, achievementStepsDao));
        environment.admin().addTask(new ImportScoutBadgesTask(sessionFactory, achievementsDao, achievementStepsDao));
        environment.admin().addTask(new ImportScouternaBadgesTask(sessionFactory, achievementsDao, achievementStepsDao));
        environment.admin().addTask(new HttpAuditTask(sessionFactory, auditingDao));
    }

    private void initSentry() {
        Sentry.init();
    }

    private void initFilterRateLimiter(Environment environment, AchievementsApplicationConfiguration.RateLimiting config) {
        final RateLimiter rateLimiter = new RateLimiter(config.getRequestsPerMinute(), config.getBurstLimit());

        final ServletRequestRateLimiter servletRequestRateLimiter = new ServletRequestRateLimiter(rateLimiter);

        environment.servlets()
                .addFilter("RateLimiter", servletRequestRateLimiter)
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/api/*");

        environment.jersey().register(new DynamicFeature() {
            @Override
            public void configure(ResourceInfo resourceInfo, FeatureContext context) {
                if (resourceInfo.getResourceMethod().isAnnotationPresent(RateLimited.class)) {
                    final RateLimited annotation = resourceInfo.getResourceMethod().getAnnotation(RateLimited.class);

                    final RateLimiter rateLimiter = new RateLimiter(
                            annotation.requestsPerMinute(),
                            annotation.burstLimit());

                    context.register(new ResourceRequestRateLimiter(rateLimiter));
                }
            }
        });
    }

    protected CredentialsDao getCredentialsDao(SessionFactory sessionFactory) {
        return new CredentialsDaoImpl(sessionFactory);
    }

    public static AuthDynamicFeature createAuthFeature(HibernateBundle<AchievementsApplicationConfiguration> hibernate, CredentialsDao credentialsDao, JwtTokenService jwtTokenService) {

        PasswordAuthenticator tokenAuthenticator = new UnitOfWorkAwareProxyFactory(hibernate).create(PasswordAuthenticator.class, CredentialsDao.class, credentialsDao);
        OnetimePasswordAuthenticator onetimePasswordAuthenticator = new UnitOfWorkAwareProxyFactory(hibernate).create(OnetimePasswordAuthenticator.class, CredentialsDao.class, credentialsDao);
        final JwtAuthenticator jwtAuthenticator = new JwtAuthenticator(new JwtSignInTokenService(jwtTokenService));

        final Authorizer<User> authorizer = (user, role) -> user.getRoles().contains(role);

        final UnauthorizedHandler unauthorizedHandler = (prefix, realm) -> Response
                .status(Response.Status.UNAUTHORIZED.getStatusCode())
                .entity(new UnsuccessfulDTO(
                        "Not good enough. You need credentials for this request.",
                        Response.Status.UNAUTHORIZED.getStatusCode()))
                .build();

        List<AuthFilter> filters = Lists.newArrayList(
                new BasicCredentialAuthFilter.Builder<User>()
                        .setPrefix("Basic")
                        .setAuthenticator(tokenAuthenticator)
                        .setAuthorizer(authorizer)
                        .setUnauthorizedHandler(unauthorizedHandler)
                        .buildAuthFilter(),
                new OAuthCredentialAuthFilter.Builder<User>()
                        .setPrefix("OneTime")
                        .setAuthenticator(onetimePasswordAuthenticator)
                        .setAuthorizer(authorizer)
                        .setUnauthorizedHandler(unauthorizedHandler)
                        .buildAuthFilter(),
                new OAuthCredentialAuthFilter.Builder<User>()
                        .setPrefix("JWT")
                        .setAuthenticator(jwtAuthenticator)
                        .setAuthorizer(authorizer)
                        .setUnauthorizedHandler(unauthorizedHandler)
                        .buildAuthFilter()
        );
        return new AuthDynamicFeature(new ChainedAuthFilter<>(filters));
    }

    private void initFilterCorsHeaders(Environment env) {
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
        bootstrap.addBundle(new MultiPartBundle());
    }

    public static void main(String[] args) throws Exception {
        new AchievementsApplication().run(args);
    }

}
