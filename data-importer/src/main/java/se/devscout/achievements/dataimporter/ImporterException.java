package se.devscout.achievements.dataimporter;

public class ImporterException extends Exception {
    public ImporterException(String message) {
        super(message);
    }

    public ImporterException(String message, Throwable cause) {
        super(message, cause);
    }
}
