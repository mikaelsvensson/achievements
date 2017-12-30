package se.devscout.achievements.server.auth;

public class ValidationResult {
    private String userEmail;
    //TODO: Rename to userId to avoid confusion with "some name that might be understandable to humans", like the email address.
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
