package se.devscout.achievements.server.resources;

public class OpenIdCallbackStateToken {
    private String email;
    private UuidString organizationId;
    private String organizationName;

    public OpenIdCallbackStateToken(String email) {
        this.email = email;
    }

    public OpenIdCallbackStateToken(String email, UuidString organizationId) {
        this.email = email;
        this.organizationId = organizationId;
    }

    public OpenIdCallbackStateToken(String email, String organizationName) {
        this.email = email;
        this.organizationName = organizationName;
    }

    public OpenIdCallbackStateToken(String email, UuidString organizationId, String organizationName) {
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
