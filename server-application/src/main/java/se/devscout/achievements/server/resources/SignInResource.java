package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.resources.authenticator.TokenGenerator;
import se.devscout.achievements.server.resources.authenticator.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("signin")
public class SignInResource extends AbstractResource {
    private TokenGenerator tokenGenerator;


    public SignInResource(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    @POST
    @UnitOfWork
    public Response createToken(@Auth User user) throws OpenIdResourceCallbackException {
        return Response
                .ok()
                .entity(tokenGenerator.createToken(user))
                .build();
    }
}
