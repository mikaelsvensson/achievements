package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.data.dao.AuditingDao;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.filter.audit.Audited;
import se.devscout.achievements.server.resources.auth.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class AuditFeatureTest {

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    @Path("endpoint/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class TestResource {
        @GET
        @Path("private")
        @Audited(logRequest = true)
        public Response goodResource(@Auth User user) {
            return Response.noContent().build();
        }

        @GET
        @Path("public")
        @Audited(logRequest = true)
        public Response goodResource() {
            return Response.noContent().build();
        }
    }

    private final AuditingDao auditingDao = mock(AuditingDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao, Boolean.TRUE, auditingDao)
            .addResource(new TestResource())
            .build();

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
    }


    @Test
    public void credentialsRequired_goodCredentials_happyPath() throws Exception {
        final Response response = resources
                .target("/endpoint/private")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(auditingDao).create(any(), any(Integer.class), any(UriInfo.class), any(String.class), any(String.class), any(String.class), anyInt());
    }

    @Test
    public void credentialsRequired_badCredentials_happyPath() throws Exception {

        doThrow(new ObjectNotFoundException()).when(credentialsDao).get(eq(CredentialsType.PASSWORD), eq("invalid_user"));

        final Response response = resources
                .target("/endpoint/private")
                .register(HttpAuthenticationFeature.basic("invalid_user", "password"))
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);

        verify(auditingDao, never()).create(any(), any(Integer.class), any(UriInfo.class), any(String.class), any(String.class), any(String.class), anyInt());
    }

    @Test
    public void credentialsRequired_noCredentials_happyPath() throws Exception {
        final Response response = resources
                .target("/endpoint/private")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);

        verify(auditingDao, never()).create(any(), any(Integer.class), any(UriInfo.class), any(String.class), any(String.class), any(String.class), anyInt());
    }

    @Test
    public void credentialsOptional_goodCredentials_happyPath() throws Exception {
        final Response response = resources
                .target("/endpoint/public")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(auditingDao, never()).create(any(), any(Integer.class), any(UriInfo.class), any(String.class), any(String.class), any(String.class), anyInt());
    }

    @Test
    public void credentialsOptional_badCredentials_happyPath() throws Exception {
        final Response response = resources
                .target("/endpoint/public")
                .register(HttpAuthenticationFeature.basic("invalid_user", "password"))
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(auditingDao, never()).create(any(), any(Integer.class), any(UriInfo.class), any(String.class), any(String.class), any(String.class), anyInt());
    }

    @Test
    public void credentialsOptional_noCredentials_happyPath() throws Exception {
        final Response response = resources
                .target("/endpoint/public")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);

        verify(auditingDao, never()).create(any(), any(Integer.class), any(UriInfo.class), any(String.class), any(String.class), any(String.class), anyInt());
    }
}