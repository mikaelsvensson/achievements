package se.devscout.achievements.server.resources.auth;

import com.google.common.collect.Sets;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.hibernate.UnitOfWork;
import org.glassfish.jersey.internal.util.Base64;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.CredentialsType;

import java.util.Collections;
import java.util.Optional;

public class OnetimePasswordAuthenticator implements Authenticator<String, User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnetimePasswordAuthenticator.class);
    private final CredentialsDao credentialsDao;

    public OnetimePasswordAuthenticator(CredentialsDao credentialsDao) {
        this.credentialsDao = credentialsDao;
    }

    @Override
    @UnitOfWork
    public Optional<User> authenticate(String password) {
        try {
            final var credentials = credentialsDao.get(CredentialsType.ONETIME_PASSWORD, Base64.decodeAsString(password));
            final var role = credentials.getPerson().getRole();
            final var user = new User(
                    credentials.getPerson().getId(),
                    credentials.getId(),
                    credentials.getPerson().getName(),
                    Sets.union(
                            // Self:
                            Collections.singleton(role),
                            // Implied roles:
                            Roles.IMPLICIT_ROLES.getOrDefault(role, Collections.emptySet())
                    ),
                    credentials.getType());

            // TODO: Delete or disable used one-time passwords?
            credentialsDao.delete(credentials.getId());

            return Optional.of(user);
        } catch (ObjectNotFoundException e) {
            LOGGER.info("Could not find object when authenticating user. Message: " + e.getMessage());
            return Optional.empty();
        } catch (HibernateException e) {
            LOGGER.error("Exception when trying to validate credentials", e);
            return Optional.empty();
        }
    }
}
