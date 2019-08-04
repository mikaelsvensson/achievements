package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import se.devscout.achievements.server.api.UnsuccessfulDTO;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.resources.SetPasswordDTO;
import se.devscout.achievements.server.resources.UuidString;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationAcceptanceTest {

    @ClassRule
    public static final DropwizardAppRule<AchievementsApplicationConfiguration> RULE =
            new DropwizardAppRule<>(
                    MockAchievementsApplication.class,
                    ResourceHelpers.resourceFilePath("server-test-configuration.yaml"));

    private static final String RANDOM_ORG_ID = UuidString.toString(UUID.randomUUID());
    private static final String RANDOM_ACHIEVEMENT_ID = UuidString.toString(UUID.randomUUID());
    private static final String IDENTITY_PROVIDER_NAME = CredentialsType.PASSWORD.name().toLowerCase();
    private static ImmutableSet<String> PUBLIC_RESOURCES;

    @BeforeClass
    public static void name() {
        PUBLIC_RESOURCES = ImmutableSet.<String>builder()
                .add(String.format("http://localhost:%d/api/organizations/signup", RULE.getLocalPort()))
                .add(String.format("http://localhost:%d/api/organizations/%s/signup", RULE.getLocalPort(), RANDOM_ORG_ID))
                .add(String.format("http://localhost:%d/api/organizations/%s/basic", RULE.getLocalPort(), RANDOM_ORG_ID))
                .add(String.format("http://localhost:%d/api/achievements", RULE.getLocalPort()))
                .add(String.format("http://localhost:%d/api/achievements/%s", RULE.getLocalPort(), RANDOM_ACHIEVEMENT_ID))
                .add(String.format("http://localhost:%d/api/achievements/%s/steps", RULE.getLocalPort(), RANDOM_ACHIEVEMENT_ID))
                .add(String.format("http://localhost:%d/api/achievements/%s/steps/1", RULE.getLocalPort(), RANDOM_ACHIEVEMENT_ID))
                .add(String.format("http://localhost:%d/api/auth/%s/signin", RULE.getLocalPort(), IDENTITY_PROVIDER_NAME))
                .add(String.format("http://localhost:%d/api/auth/%s/signin/callback", RULE.getLocalPort(), IDENTITY_PROVIDER_NAME))
                .add(String.format("http://localhost:%d/api/auth/%s/signup", RULE.getLocalPort(), IDENTITY_PROVIDER_NAME))
                .add(String.format("http://localhost:%d/api/auth/%s/signup/callback", RULE.getLocalPort(), IDENTITY_PROVIDER_NAME))
                .add(String.format("http://localhost:%d/api/auth/%s/metadata", RULE.getLocalPort(), IDENTITY_PROVIDER_NAME))
                .add(String.format("http://localhost:%d/api/my/forgot-password", RULE.getLocalPort()))
                .add(String.format("http://localhost:%d/api/my/send-set-password-link", RULE.getLocalPort()))
                .build();
    }

    @Test
    public void oneTimePassword_successfulFirstFailedSecondRequest() {
        Client client = RULE.client();
        Response response1 = client
                .target(String.format("http://localhost:%d/api/my/profile", RULE.getLocalPort()))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "OneTime " + base64("onetimepassword1"))
                .get();

        assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK_200);

        Response response2 = client
                .target(String.format("http://localhost:%d/api/my/profile", RULE.getLocalPort()))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "OneTime " + base64("onetimepassword1"))
                .get();

        // Verify that second request using same one-time password fails.
        assertThat(response2.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
    }

    @Test
    public void oneTimePassword_failedFirstFailedSecondRequest() {
        Client client = RULE.client();
        Response response1 = client
                .target(String.format("http://localhost:%d/api/my/password", RULE.getLocalPort()))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "OneTime " + base64("onetimepassword3"))
                .post(Entity.json(new SetPasswordDTO(null, "good_password", "bad_password")));

        assertThat(response1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

        Response response2 = client
                .target(String.format("http://localhost:%d/api/my/profile", RULE.getLocalPort()))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "OneTime " + base64("onetimepassword3"))
                .get();

        // Verify that second request using same one-time password fails.
        assertThat(response2.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
    }

    @Test
    @Ignore("We accept the risk that Trudy can change the password of Alice if Trudy gets hold of Alice's one-time password.")
    public void oneTimePassword_correctPasswordForAnotherUser() {
        Client client = RULE.client();
        Response response = client
                .target(String.format("http://localhost:%d/api/my/profile", RULE.getLocalPort()))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "OneTime " + base64("onetimepassword2"))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
    }

    @Test
    public void oneTimePassword_incorrectPassword() {
        Client client = RULE.client();
        Response response = client
                .target(String.format("http://localhost:%d/api/my/profile", RULE.getLocalPort()))
                .request()
                .header(HttpHeaders.AUTHORIZATION, "OneTime " + base64("the-wrong-password"))
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
    }

    private String base64(String str) {
        return BaseEncoding.base64().encode(str.getBytes(Charsets.UTF_8));
    }

    @Test
    public void authenticationRequired_httpHeaderMissing_expect401() {
        Client client = RULE.client();

        List<String> failedResources = new ArrayList<>();

        for (Pair<String, String> endpoint : getAuthenticatedApplicationEndpoints()) {
            final String verb = endpoint.getRight();
            final String resource = endpoint.getLeft();
            Response response = client
                    .target(resource)
                    .request()
                    .build(verb, getMockEntity(verb))
                    .invoke();
            if (response.getStatus() != HttpStatus.UNAUTHORIZED_401) {
                failedResources.add(verb + " " + resource + " returned " + response.getStatus());
            }

        }
        assertThat(failedResources).hasSize(0);
    }

    private Entity<?> getMockEntity(String verb) {
        return "POST".equals(verb) || "PUT".equals(verb) ? Entity.json(new UnsuccessfulDTO("DTO type is not important", HttpStatus.BAD_REQUEST_400)) : null;
    }

    @Test
    public void authenticationRequired_correctCredentialsProvided_failIf401Returned() {
        Client client = RULE.client();

        List<String> failedResources = new ArrayList<>();

        for (Pair<String, String> endpoint : getAuthenticatedApplicationEndpoints()) {
            final String verb = endpoint.getRight();
            final String resource = endpoint.getLeft();
            Response response = client
                    .target(resource)
                    .register(MockUtil.AUTH_FEATURE_EDITOR)
                    .request()
                    .build(verb, getMockEntity(verb))
                    .invoke();
            if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                failedResources.add(verb + " " + resource + " returned " + response.getStatus());
            }

        }
        assertThat(failedResources).hasSize(0);
    }

    @Test
    public void authenticationRequired_incorrectCredentialsProvided_expect401() {
        Client client = RULE.client();

        List<String> failedResources = new ArrayList<>();

        for (Pair<String, String> endpoint : getAuthenticatedApplicationEndpoints()) {
            final String verb = endpoint.getRight();
            final String resource = endpoint.getLeft();
            Response response = client
                    .target(resource)
                    .request()
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + base64("user:incorrect-password"))
                    .build(verb, getMockEntity(verb))
                    .invoke();
            if (response.getHeaderString("WWW-Authenticate") != null) {
                failedResources.add(verb + " " + resource + " should not have returned the WWW-Authenticate header");
            }
            if (response.getStatus() != HttpStatus.UNAUTHORIZED_401) {
                failedResources.add(verb + " " + resource + " returned " + response.getStatus());
            }
            if (response.readEntity(UnsuccessfulDTO.class).status != HttpStatus.UNAUTHORIZED_401) {
                failedResources.add(verb + " " + resource + " returned a payload with the incorrect status code");
            }

        }
        assertThat(failedResources).hasSize(0);
    }

    private List<Pair<String, String>> getAuthenticatedApplicationEndpoints() {
        final ImmutableMap<String, String> randomUriParameterValues = ImmutableMap.<String, String>builder()
                .put("organizationId", RANDOM_ORG_ID)
                .put("achievementId", RANDOM_ACHIEVEMENT_ID)
                .put("stepId", String.valueOf(1))
                .put("personId", String.valueOf(2))
                .put("groupId", String.valueOf(3))
                .put("identityProvider", IDENTITY_PROVIDER_NAME)
                .build();
        List<Pair<String, String>> endpoints = new ArrayList<>();
        final Set<Object> singletons = RULE.getEnvironment().jersey().getResourceConfig().getSingletons();
        for (Object singleton : singletons) {
            if (singleton.getClass().isAnnotationPresent(Path.class)) {

                String basePath = UriBuilder.fromResource(singleton.getClass()).buildFromMap(randomUriParameterValues).toString();

                for (Method method : singleton.getClass().getDeclaredMethods()) {
                    String path = basePath;
                    if (method.isAnnotationPresent(Path.class)) {
                        path += (path.length() > 0 ? "/" : "") + UriBuilder.fromMethod(singleton.getClass(), method.getName()).buildFromMap(randomUriParameterValues).toString();
                    }
                    final Optional<Class<? extends Annotation>> httpVerb = Stream.of(POST.class, GET.class, PUT.class, DELETE.class).filter(method::isAnnotationPresent).findFirst();
                    if (httpVerb.isPresent()) {
                        final String uri = String.format("http://localhost:%d/api/%s", RULE.getLocalPort(), path);
                        if (!PUBLIC_RESOURCES.contains(uri)) {
                            endpoints.add(Pair.of(uri, httpVerb.get().getSimpleName()));
                        }
                    }
                }
            }
        }
        return endpoints;
    }

}
