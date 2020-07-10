package se.devscout.achievements.server.resources.auth;

import com.google.common.collect.Sets;
import se.devscout.achievements.server.api.AuthTokenDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.jwt.JwtSignInToken;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.resources.AbstractResource;

import java.util.Collections;
import java.util.UUID;

public class AbstractAuthResource extends AbstractResource {
    private final JwtSignInTokenService signInTokenService;
    private final CredentialsDao credentialsDao;

    public AbstractAuthResource(JwtSignInTokenService signInTokenService, CredentialsDao credentialsDao) {
        this.signInTokenService = signInTokenService;
        this.credentialsDao = credentialsDao;
    }

    protected AuthTokenDTO createTokenDTO(CredentialsType credentialsType, String userId) throws ExternalIdpCallbackException {
        try {
            final var credentials = credentialsDao.get(credentialsType, userId);
            return generateTokenResponse(credentials);
        } catch (ObjectNotFoundException e) {
            throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.UNKNOWN_USER, "Could not find user.", e);
        }
    }

    protected AuthTokenDTO createTokenDTO(UUID credentialsId) throws ExternalIdpCallbackException {
        try {
            final var credentials = credentialsDao.read(credentialsId);
            return generateTokenResponse(credentials);
        } catch (ObjectNotFoundException e) {
            throw new ExternalIdpCallbackException(ExternalIdpCallbackExceptionType.UNKNOWN_USER, "Could not find user.", e);
        }
    }

    protected AuthTokenDTO generateTokenResponse(Credentials credentials) {
        final var person = credentials.getPerson();
        final var role = person.getRole();
        final var roles = Sets.union(
                // Self:
                Collections.singleton(role),
                // Implied roles:
                Roles.IMPLICIT_ROLES.getOrDefault(role, Collections.emptySet())
        );
        return new AuthTokenDTO(signInTokenService.encode(
                new JwtSignInToken(
                        person.getName(),
                        person.getId(),
                        credentials.getId(),
                        roles,
                        person.getOrganization().getId())));
    }

}
