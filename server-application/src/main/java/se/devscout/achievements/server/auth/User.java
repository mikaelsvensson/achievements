package se.devscout.achievements.server.auth;

import se.devscout.achievements.server.data.model.Credentials;

import java.security.Principal;

public class User implements Principal {
    private String name;
    private final Credentials credentials;

    public User(String name, Credentials credentials) {
        this.name = name;
        this.credentials = credentials;
    }

    @Override
    public String getName() {
        return name;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
