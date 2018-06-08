package se.devscout.achievements.server.mail;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import se.devscout.achievements.server.resources.RateLimiter;

public class SmtpSender implements EmailSender {

    private RateLimiter rateLimiter;

    private SmtpSenderConfiguration configuration;

    public SmtpSender(SmtpSenderConfiguration configuration) {
        this.configuration = configuration;
        if (configuration != null && configuration.getMaxMailsPerSenderPerMinute() > 0) {
            this.rateLimiter = new RateLimiter(configuration.getMaxMailsPerSenderPerMinute(), 0);
        }
    }

    @Override
    public void send(String clientId, String to, String subject, String body) throws EmailSenderException {
        if (rateLimiter != null && !rateLimiter.accept(clientId)) {
            throw new EmailSenderException("Rate limit exceeded.");
        }
        try {
            Email email = createEmail();
            email.addTo(to);
            email.setSubject(subject);
            email.setMsg(body);
            email.send();
        } catch (EmailException e) {
            throw new EmailSenderException("Could not send e-mail to " + to, e);
        }
    }

    private Email createEmail() throws EmailException {
        Email email = new SimpleEmail();
        email.setSocketConnectionTimeout(5_000);
        email.setHostName(configuration.getHost());
        email.setSmtpPort(configuration.getPort());
        email.setSslSmtpPort(String.valueOf(configuration.getPort()));
        email.setAuthenticator(new DefaultAuthenticator(configuration.getUsername(), configuration.getPassword()));
        email.setSSLOnConnect(configuration.isSsl());
        email.setFrom(configuration.getFromAddress());
        return email;
    }

}
