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
import se.devscout.achievements.server.auth.saml.ScoutIdIdentityProvider;
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
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
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
        final var sessionFactory = hibernate.getSessionFactory();

        if (config.isAutoMigrateDatabase()) {
            var ds = config.getDataSourceFactory().build(environment.metrics(), "migrations");
            try (var connection = ds.getConnection()) {
                var migrator = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
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
        final var credentialsDao = getCredentialsDao(sessionFactory);

        environment.jersey().register(new CallbackResourceExceptionMapper(config.getGuiApplicationHost()));
        environment.jersey().register(ValidationExceptionMapper.class);
        environment.jersey().register(JerseyViolationExceptionMapper.class);

        final JwtTokenService jwtTokenService = new JwtTokenServiceImpl(config.getAuthentication().getJwtSigningSecret());
        final var signInTokenService = new JwtSignInTokenService(jwtTokenService);
        final var signUpTokenService = new JwtSignUpTokenService(jwtTokenService);

        environment.jersey().register(createAuthFeature(hibernate, credentialsDao, jwtTokenService));

        initFilterCorsHeaders(environment);

        initJerseyParameterFix(environment);

        initSentry();

        if (config.getRateLimiting() != null) {
            initFilterRateLimiter(environment, config.getRateLimiting());
        }

        environment.jersey().register(new AuditFeature(auditingDao, hibernate));

        environment.jersey().register(RolesAllowedDynamicFeature.class);
        //If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));

        final var emailSender = new SmtpSender(config.getSmtp());
        final var i18n = new I18n("texts.sv.yaml");

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
                                "https://www.googleapis.com/oauth2/v4/token",
                                new GoogleTokenValidator(config.getAuthentication().getGoogleClientId()),
                                config.getServerApplicationHost()),
                        "microsoft",
                        new OpenIdIdentityProvider(
                                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                                config.getAuthentication().getMicrosoftClientId(),
                                config.getAuthentication().getMicrosoftClientSecret(),
                                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                                new MicrosoftTokenValidator(),
                                config.getServerApplicationHost()),
                        "email",
                        new EmailIdentityProvider(
                                jwtTokenService,
                                emailSender,
                                config.getGuiApplicationHost(),
                                credentialsDao),
                        "scoutid",
                        new ScoutIdIdentityProvider(
                                config.getServerApplicationHost())),
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

    private void initJerseyParameterFix(Environment environment) {
        final Filter filter = new JerseyParameterFixFilter();
        environment.servlets()
                .addFilter("JerseyParameterFixFilter", filter)
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/api/*");
    }

    private void initSentry() {
        Sentry.init();
    }

    private void initFilterRateLimiter(Environment environment, AchievementsApplicationConfiguration.RateLimiting config) {
        final var rateLimiter = new RateLimiter(config.getRequestsPerMinute(), config.getBurstLimit());

        final var servletRequestRateLimiter = new ServletRequestRateLimiter(rateLimiter);

        environment.servlets()
                .addFilter("RateLimiter", servletRequestRateLimiter)
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/api/*");

        environment.jersey().register(new DynamicFeature() {
            @Override
            public void configure(ResourceInfo resourceInfo, FeatureContext context) {
                if (resourceInfo.getResourceMethod().isAnnotationPresent(RateLimited.class)) {
                    final var annotation = resourceInfo.getResourceMethod().getAnnotation(RateLimited.class);

                    final var rateLimiter = new RateLimiter(
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

        var tokenAuthenticator = new UnitOfWorkAwareProxyFactory(hibernate).create(PasswordAuthenticator.class, CredentialsDao.class, credentialsDao);
        var onetimePasswordAuthenticator = new UnitOfWorkAwareProxyFactory(hibernate).create(OnetimePasswordAuthenticator.class, CredentialsDao.class, credentialsDao);
        final var jwtAuthenticator = new JwtAuthenticator(new JwtSignInTokenService(jwtTokenService));

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
        var filter = env.servlets().addFilter("CORSFilter", CrossOriginFilter.class);
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
