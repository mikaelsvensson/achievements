package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import se.devscout.achievements.server.api.UnsuccessfulDTO;
import se.devscout.achievements.server.data.model.CredentialsType;
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
    private static final ImmutableSet<String> PUBLIC_RESOURCES = ImmutableSet.<String>builder()
            .add("http://localhost:9000/api/organizations/signup")
            .add("http://localhost:9000/api/organizations/" + RANDOM_ORG_ID + "/signup")
            .add("http://localhost:9000/api/organizations/" + RANDOM_ORG_ID + "/basic")
            .add("http://localhost:9000/api/achievements")
            .add("http://localhost:9000/api/achievements/" + RANDOM_ACHIEVEMENT_ID)
            .add("http://localhost:9000/api/achievements/" + RANDOM_ACHIEVEMENT_ID + "/steps")
            .add("http://localhost:9000/api/achievements/" + RANDOM_ACHIEVEMENT_ID + "/steps/1")
            .add("http://localhost:9000/api/openid/" + IDENTITY_PROVIDER_NAME + "/signin")
            .add("http://localhost:9000/api/openid/" + IDENTITY_PROVIDER_NAME + "/signin/callback")
            .add("http://localhost:9000/api/openid/" + IDENTITY_PROVIDER_NAME + "/signup")
            .add("http://localhost:9000/api/openid/" + IDENTITY_PROVIDER_NAME + "/signup/callback")
            .build();

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
                    .request()
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
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
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:incorrect-password".getBytes(Charsets.UTF_8)))
                    .build(verb, getMockEntity(verb))
                    .invoke();
            if (response.getStatus() != HttpStatus.UNAUTHORIZED_401) {
                failedResources.add(verb + " " + resource + " returned " + response.getStatus());
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
                    final Optional<Class<? extends Annotation>> httpVerb = Stream.of(POST.class, GET.class, PUT.class, DELETE.class).filter(a -> method.isAnnotationPresent(a)).findFirst();
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
