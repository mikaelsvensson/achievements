package se.devscout.achievements.server.mail;

public class EmailSenderException extends Exception {
    public EmailSenderException(String message) {
        super(message);
    }

    public EmailSenderException(String message, Exception cause) {
        super(message, cause);
    }
}
