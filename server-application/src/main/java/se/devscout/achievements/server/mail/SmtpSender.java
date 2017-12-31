package se.devscout.achievements.server.mail;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class SmtpSender implements EmailSender {

    private SmtpSenderConfiguration configuration;

    public SmtpSender(SmtpSenderConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void send(String to, String subject, String body) throws EmailSenderException {
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
