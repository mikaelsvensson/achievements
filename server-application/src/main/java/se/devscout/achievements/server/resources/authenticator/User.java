package se.devscout.achievements.server.resources.authenticator;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public class User implements Principal {
    private String personName;
    private int personId;
    private final UUID credentialsId;
    private Set<String> roles;

    public User(int personId, UUID credentialsId, String personName, Set<String> roles) {
        this.personId = personId;
        this.personName = personName;
        this.credentialsId = credentialsId;
        this.roles = roles;
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

    public Set<String> getRoles() {
        return roles;
    }
}
