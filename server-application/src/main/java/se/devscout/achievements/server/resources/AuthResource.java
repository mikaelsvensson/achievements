package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.JwtAuthenticator;
import se.devscout.achievements.server.auth.User;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource extends AbstractResource {
    private final JwtAuthenticator authenticator;
    private CredentialsDao credentialsDao;

    public AuthResource(JwtAuthenticator authenticator, CredentialsDao credentialsDao) {
        this.authenticator = authenticator;
        this.credentialsDao = credentialsDao;
    }

    @POST
    @Path("token")
    @UnitOfWork
    public Response createToken(@Auth User user) {
        try {
            final Credentials credentials = credentialsDao.read(user.getUsedCredentialsId());

            final String token = authenticator.generateToken(
                    user.getName(),
                    credentials.getPerson().getOrganization().getId(),
                    credentials.getPerson().getId());
            return Response
                    .ok()
                    .entity(new AuthTokenDTO(token))
                    .build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

}
