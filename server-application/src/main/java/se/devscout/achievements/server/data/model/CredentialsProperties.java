package se.devscout.achievements.server.data.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass
public class CredentialsProperties {
    @Column(name = "user_id")
    private String userId;
    @Basic
    @Column(length = 1024)
    @Size(max = 1024)
    private byte[] data;

    private CredentialsType type;

    public CredentialsProperties() {
    }

    public CredentialsProperties(String userId, CredentialsType type, byte[] data) {
        this.userId = userId;
        this.type = type;
        this.data = data;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public CredentialsType getType() {
        return type;
    }

    public void setType(CredentialsType type) {
        this.type = type;
    }

}
