package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.*;

import java.util.UUID;

public interface CredentialsDao extends CrudDao<Credentials, CredentialsProperties, Person, UUID> {
    Credentials get(IdentityProvider provider, String username) throws ObjectNotFoundException;
}
