package se.devscout.achievements.server.resources.auth;

import com.google.common.collect.Sets;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.hibernate.UnitOfWork;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.model.CredentialsType;

import java.util.Collections;
import java.util.Optional;

public class PasswordAuthenticator implements Authenticator<BasicCredentials, User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordAuthenticator.class);
    private final CredentialsDao credentialsDao;

    public PasswordAuthenticator(CredentialsDao credentialsDao) {
        this.credentialsDao = credentialsDao;
    }

    @Override
    @UnitOfWork
    public Optional<User> authenticate(BasicCredentials basicCredentials) {
        try {
            final var credentials = credentialsDao.get(CredentialsType.PASSWORD, basicCredentials.getUsername());
            final var validator = new PasswordValidator(credentials.getData());
            final var validationResult = validator.validate(basicCredentials.getPassword().toCharArray());
            if (validationResult.isValid()) {
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
                return Optional.of(user);
            } else {
                return Optional.empty();
            }
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Could not find object when authenticating user. " + e.getMessage());
            return Optional.empty();
        } catch (HibernateException e) {
            LOGGER.error("Exception when trying to validate credentials", e);
            return Optional.empty();
        }
    }
}
