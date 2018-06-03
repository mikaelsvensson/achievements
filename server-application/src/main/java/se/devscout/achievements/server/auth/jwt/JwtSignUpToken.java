package se.devscout.achievements.server.auth.jwt;

import se.devscout.achievements.server.resources.UuidString;

public class JwtSignUpToken {
    private UuidString organizationId;
    private String organizationName;

    public JwtSignUpToken(UuidString organizationId, String organizationName) {
        this.organizationId = organizationId;
        this.organizationName = organizationName;
    }

    public UuidString getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }
}
