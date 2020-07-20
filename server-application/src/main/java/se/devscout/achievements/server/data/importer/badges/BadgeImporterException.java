package se.devscout.achievements.server.data.importer.badges;

public class BadgeImporterException extends Exception {
    public BadgeImporterException(String message) {
        super(message);
    }

    public BadgeImporterException(String message, Throwable cause) {
        super(message, cause);
    }
}
