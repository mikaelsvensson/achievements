package se.devscout.achievements.server.auth;

import se.devscout.achievements.server.data.model.CredentialsType;

public class ValidationResult {
    private final String userEmail;
    private final String userId;
    private final boolean isValid;

    public static final ValidationResult INVALID = new ValidationResult(null, null, false, null, new byte[0]);
    private final byte[] credentialsData;
    private final CredentialsType credentialsType;
    private String callbackState;

    public ValidationResult(String userEmail, String userId, boolean isValid, CredentialsType credentialsType, byte[] credentialsData) {
        this(userEmail, userId, isValid, credentialsType, credentialsData, null);
    }

    public ValidationResult(String userEmail, String userId, boolean isValid, CredentialsType credentialsType, byte[] credentialsData, String callbackState) {
        this.userEmail = userEmail;
        this.userId = userId;
        this.isValid = isValid;
        this.credentialsType = credentialsType;
        this.credentialsData = credentialsData;
        this.callbackState = callbackState;
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

    public String getCallbackState() {
        return callbackState;
    }

    public ValidationResult withCallbackState(String callbackState) {
        this.callbackState = callbackState;
        return this;
    }
}
