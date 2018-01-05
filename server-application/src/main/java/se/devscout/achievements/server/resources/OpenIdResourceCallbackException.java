package se.devscout.achievements.server.resources;

public class OpenIdResourceCallbackException extends Exception {
    private final OpenIdResourceCallbackExceptionType type;

    public OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType type, String message, Exception cause) {
        super(message, cause);
        this.type = type;
    }

    public OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType type, Exception cause) {
        super(null, cause);
        this.type = type;
    }

    public OpenIdResourceCallbackException(OpenIdResourceCallbackExceptionType type, String message) {
        this(type, message, null);
    }

    public OpenIdResourceCallbackException(Exception cause) {
        this(OpenIdResourceCallbackExceptionType.UNSPECIFIED, cause);
    }

    public OpenIdResourceCallbackExceptionType getType() {
        return type;
    }
}
