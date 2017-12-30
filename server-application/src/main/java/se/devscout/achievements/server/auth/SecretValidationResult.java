package se.devscout.achievements.server.auth;

public class SecretValidationResult {
    private String userEmail;
    //TODO: Rename to userId to avoid confusion with "some name that might be understandable to humans", like the email address.
    private String userName;
    private boolean isValid;

    public static final SecretValidationResult INVALID = new SecretValidationResult(null, null, false);

    public SecretValidationResult(String userEmail, String userName, boolean isValid) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.isValid = isValid;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public boolean isValid() {
        return isValid;
    }
}
