package se.devscout.achievements.server.auth;

import se.devscout.achievements.server.data.model.CredentialsType;

public class ValidationResult {
    private String userEmail;
    private String userId;
    private boolean isValid;

    public static final ValidationResult INVALID = new ValidationResult(null, null, false, null, new byte[0]);
    private byte[] credentialsData;
    private CredentialsType credentialsType;

    public ValidationResult(String userEmail, String userId, boolean isValid, CredentialsType credentialsType, byte[] credentialsData) {
        this.userEmail = userEmail;
        this.userId = userId;
        this.isValid = isValid;
        this.credentialsType = credentialsType;
        this.credentialsData = credentialsData;
    }

    public CredentialsType getCredentialsType() {
        return credentialsType;
    }

    public byte[] getCredentialsData() {
        return credentialsData;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isValid() {
        return isValid;
    }
}
