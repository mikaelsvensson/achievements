package se.devscout.achievements.server.mail;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.resources.RateLimiter;

public class SmtpSender implements EmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpSender.class);
    private RateLimiter rateLimiter;

    private final SmtpSenderConfiguration configuration;

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
            var email = createEmail();
            email.addTo(to);
            email.setSubject(subject);
            email.setCharset("UTF-8"); // Specifying a character set seems to be important for Gmail, otherwise it tends to show a the-message-has-been-cropped-message.
            email.setHtmlMsg(body);
            final var messageId = email.send();
            LOGGER.info("Sent e-mail {}.", messageId);
        } catch (EmailException e) {
            throw new EmailSenderException("Could not send e-mail to " + to, e);
        }
    }

    private HtmlEmail createEmail() throws EmailException {
        var email = new HtmlEmail();
        email.setSocketConnectionTimeout(5_000);
        email.setSocketTimeout(5_000);
        email.setHostName(configuration.getHost());
        email.setSmtpPort(configuration.getPort());
        email.setSslSmtpPort(String.valueOf(configuration.getPort()));
        email.setAuthenticator(new DefaultAuthenticator(configuration.getUsername(), configuration.getPassword()));
        email.setSSLOnConnect(configuration.isSsl());
        email.setFrom(configuration.getFromAddress());
        return email;
    }

}
