package se.devscout.achievements.server.auth.jwt;

import se.devscout.achievements.server.resources.UuidString;

public class JwtSignUpToken {
    private String email;
    private UuidString organizationId;
    private String organizationName;

    public JwtSignUpToken(String email) {
        this.email = email;
    }

    public JwtSignUpToken(String email, UuidString organizationId, String organizationName) {
        this.email = email;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
    }

    public String getEmail() {
        return email;
    }

    public UuidString getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }
}
