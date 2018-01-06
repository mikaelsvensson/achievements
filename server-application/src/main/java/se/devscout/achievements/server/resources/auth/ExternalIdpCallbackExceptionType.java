package se.devscout.achievements.server.resources.auth;

public enum ExternalIdpCallbackExceptionType {
    UNSPECIFIED,
    UNKNOWN_USER,
    UNKNOWN_ORGANIZATION,
    ORGANIZATION_EXISTS,
    INVALID_INPUT,
    SYSTEM_ERROR
}
