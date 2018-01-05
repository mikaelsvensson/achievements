package se.devscout.achievements.server.resources.authenticator;

import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.OpenIdResourceCallbackException;
import se.devscout.achievements.server.resources.OpenIdResourceCallbackExceptionType;

public class TokenGenerator {
    private final JwtAuthenticator authenticator;
    private CredentialsDao credentialsDao;

    public TokenGenerator(JwtAuthenticator authenticator, CredentialsDao credentialsDao) {
        this.authenticator = authenticator;
        this.credentialsDao = credentialsDao;
    }

    public AuthTokenDTO createToken(CredentialsType credentialsType, String userId) throws OpenIdResourceCallbackException {
        try {
            final Person person = credentialsDao.get(credentialsType, userId).getPerson();
            return generateTokenResponse(person);
        } catch (ObjectNotFoundException e) {
            throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.UNKNOWN_USER, "Could not find user.", e);
        }
    }

    public AuthTokenDTO createToken(User user) throws OpenIdResourceCallbackException {
        try {
            final Person person = credentialsDao.read(user.getCredentialsId()).getPerson();
            return generateTokenResponse(person);
        } catch (ObjectNotFoundException e) {
            throw new OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType.UNKNOWN_USER, "Could not find user.", e);
        }
    }

    public AuthTokenDTO generateTokenResponse(Person person) {
        return new AuthTokenDTO(authenticator.generateToken(
                person.getName(),
                person.getOrganization() != null ? person.getOrganization().getId() : null,
                person.getId(),
                person.getRole()));
    }

}
