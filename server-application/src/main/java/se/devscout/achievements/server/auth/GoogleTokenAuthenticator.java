package se.devscout.achievements.server.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.DaoException;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsProperties;
import se.devscout.achievements.server.data.model.IdentityProvider;
import se.devscout.achievements.server.data.model.Person;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Reference: https://developers.google.com/identity/sign-in/web/backend-auth
 */
public class GoogleTokenAuthenticator implements Authenticator<String, User> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTokenAuthenticator.class);

    private final CredentialsDao credentialsDao;
    private final PeopleDao peopleDao;
    private final String googleClientId;

    public GoogleTokenAuthenticator(CredentialsDao credentialsDao, PeopleDao peopleDao, String googleClientId) {
        requireNonNull(credentialsDao);
        requireNonNull(peopleDao);
        requireNonNull(googleClientId);

        this.credentialsDao = credentialsDao;
        this.peopleDao = peopleDao;
        this.googleClientId = googleClientId;
    }

    @Override
    @UnitOfWork
    public Optional<User> authenticate(String token) throws AuthenticationException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                Payload payload = idToken.getPayload();

                String userId = payload.getSubject();

                String email = payload.getEmail();
                boolean emailVerified = payload.getEmailVerified();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String locale = (String) payload.get("locale");
                LOGGER.info("User authenticated using Google, email={0}, emailVerified={1}, name={2}, locale={3}, picture={4}",
                        email,
                        emailVerified,
                        name,
                        locale,
                        pictureUrl);

                try {
                    Credentials credentials = credentialsDao.get(IdentityProvider.GOOGLE, userId);
                    return Optional.of(new User(credentials.getPerson().getId(), credentials.getId(), email));
                } catch (ObjectNotFoundException e) {
                    final Person person;
                    final List<Person> people = peopleDao.getByEmail(email);
                    if (people.isEmpty()) {
                        //TODO: Potentially refactor this class so that a person can be created for an organization the first time he/she logs in. However, should it be possible to add oneself to an organization without prior approval from someone in that organization?
                        return Optional.empty();
                    } else if (people.size() == 1) {
                        person = people.get(0);
                    } else {
                        throw new AuthenticationException("More than one person uses this e-mail address. Unable to determine which person actually logged in.");
                    }
                    try {
                        final CredentialsProperties properties = new CredentialsProperties();
                        properties.setProvider(IdentityProvider.GOOGLE);
                        properties.setUsername(userId);
                        Credentials credentials = credentialsDao.create(person, properties);
                        return Optional.of(new User(credentials.getPerson().getId(), credentials.getId(), email));
                    } catch (DaoException e1) {
                        throw new AuthenticationException(e1);
                    }
                }
            } else {
                LOGGER.info("Invalid Google token");
                return Optional.empty();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new AuthenticationException(e);
        }
    }
}
