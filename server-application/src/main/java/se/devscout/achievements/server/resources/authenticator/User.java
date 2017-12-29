package se.devscout.achievements.server.resources.authenticator;

import java.security.Principal;
import java.util.UUID;

public class User implements Principal {
    private String personName;
    private int personId;
    private final UUID credentialsId;

    public User(int personId, UUID credentialsId, String personName) {
        this.personId = personId;
        this.personName = personName;
        this.credentialsId = credentialsId;
    }

    @Override
    public String getName() {
        return personName;
    }

    public UUID getCredentialsId() {
        return credentialsId;
    }

    public int getPersonId() {
        return personId;
    }
}
