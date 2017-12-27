package se.devscout.achievements.server.auth;

import java.security.Principal;
import java.util.UUID;

public class User implements Principal {
    private String name;
    private final UUID usedCredentialsId;
    private int id;

    public User(int id, UUID usedCredentialsId, String name) {
        this.id = id;
        this.name = name;
        this.usedCredentialsId = usedCredentialsId;
    }

    @Override
    public String getName() {
        return name;
    }

    public UUID getUsedCredentialsId() {
        return usedCredentialsId;
    }

    public int getId() {
        return id;
    }
}
