package se.devscout.achievements.server.mail;

public interface EmailSender {
    void send(String to, String subject, String body) throws EmailSenderException;
}
