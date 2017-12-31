package se.devscout.achievements.server.auth;

public class ValidationResult {
    private String userEmail;
    private String userId;
    private boolean isValid;

    public static final ValidationResult INVALID = new ValidationResult(null, null, false);

    public ValidationResult(String userEmail, String userId, boolean isValid) {
        this.userEmail = userEmail;
        this.userId = userId;
        this.isValid = isValid;
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
