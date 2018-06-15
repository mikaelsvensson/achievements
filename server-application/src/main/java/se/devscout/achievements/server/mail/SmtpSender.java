package se.devscout.achievements.server.mail;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
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
            HtmlEmail email = createEmail();
            email.addTo(to);
            email.setSubject(subject);
            email.setCharset("UTF-8"); // Specifying a character set seems to be important for Gmail, otherwise it tends to show a the-message-has-been-cropped-message.
            email.setHtmlMsg(body);
            email.send();
        } catch (EmailException e) {
            throw new EmailSenderException("Could not send e-mail to " + to, e);
        }
    }

    private HtmlEmail createEmail() throws EmailException {
        HtmlEmail email = new HtmlEmail();
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
