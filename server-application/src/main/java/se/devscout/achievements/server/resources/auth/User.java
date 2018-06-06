package se.devscout.achievements.server.resources.auth;

import se.devscout.achievements.server.data.model.CredentialsType;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public class User implements Principal {
    //    private String personName;
    private int personId;
    private final UUID credentialsId;
    private Set<String> roles;
    private CredentialsType credentialsTypeUsed;

    public User(int personId, UUID credentialsId, String personName, Set<String> roles, CredentialsType credentialsTypeUsed) {
        this.personId = personId;
//        this.personName = personName;
        this.credentialsId = credentialsId;
        this.roles = roles;
        this.credentialsTypeUsed = credentialsTypeUsed;
    }

    @Override
    public String getName() {
        return "User " + credentialsId;
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

    public CredentialsType getCredentialsTypeUsed() {
        return credentialsTypeUsed;
    }
}
