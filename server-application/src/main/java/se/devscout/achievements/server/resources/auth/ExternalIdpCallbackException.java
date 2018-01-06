package se.devscout.achievements.server.resources.auth;

public class ExternalIdpCallbackException extends Exception {
    private final ExternalIdpCallbackExceptionType type;

    public ExternalIdpCallbackException(ExternalIdpCallbackExceptionType type, String message, Exception cause) {
        super(message, cause);
        this.type = type;
    }

    public ExternalIdpCallbackException(ExternalIdpCallbackExceptionType type, Exception cause) {
        super(null, cause);
        this.type = type;
    }

    public ExternalIdpCallbackException(ExternalIdpCallbackExceptionType type, String message) {
        this(type, message, null);
    }

    public ExternalIdpCallbackException(Exception cause) {
        this(ExternalIdpCallbackExceptionType.UNSPECIFIED, cause);
    }

    public ExternalIdpCallbackExceptionType getType() {
        return type;
    }
}
