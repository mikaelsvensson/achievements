package se.devscout.achievements.server.data.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass
public class CredentialsProperties {
    //TODO: Rename to userId to avoid confusion with "some name that might be understandable to humans"
    private String username;
    @Basic
    @Column(length = 1024)
    @Size(max = 1024)
    //TODO: Rename "identity provider data" and "secret" to "credentials data"?
    private byte[] secret;

    private IdentityProvider provider;

    public CredentialsProperties() {
    }

    public CredentialsProperties(String username, IdentityProvider provider, byte[] secret) {
        this.username = username;
        this.provider = provider;
        this.secret = secret;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getSecret() {
        return secret;
    }

    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    public IdentityProvider getProvider() {
        return provider;
    }

    public void setProvider(IdentityProvider provider) {
        this.provider = provider;
    }

}
