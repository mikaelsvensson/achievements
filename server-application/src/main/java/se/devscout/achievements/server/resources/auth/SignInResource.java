package se.devscout.achievements.server.resources.auth;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.data.dao.CredentialsDao;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("signin")
public class SignInResource extends AbstractAuthResource {
    public SignInResource(JwtSignInTokenService signInTokenService, CredentialsDao credentialsDao) {
        super(signInTokenService, credentialsDao);
    }

    @POST
    @UnitOfWork
    public Response createToken(@Auth User user) throws ExternalIdpCallbackException {
        return Response
                .ok()
                .entity(createTokenDTO(user.getCredentialsId()))
                .build();
    }
}
