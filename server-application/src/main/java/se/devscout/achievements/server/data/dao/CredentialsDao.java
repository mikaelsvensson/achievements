package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsProperties;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.data.model.Person;

import java.util.UUID;

public interface CredentialsDao extends CrudDao<Credentials, CredentialsProperties, Person, UUID> {
    Credentials get(CredentialsType provider, String username) throws ObjectNotFoundException;
}
