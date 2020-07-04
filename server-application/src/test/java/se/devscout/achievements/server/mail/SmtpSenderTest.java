package se.devscout.achievements.server.mail;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SmtpSenderTest {

    private SmtpSender sender;

    @Before
    public void setUp() throws Exception {
        var configuration = mock(SmtpSenderConfiguration.class);
        when(configuration.getMaxMailsPerSenderPerMinute()).thenReturn(1);
        when(configuration.getPort()).thenReturn(1337);
        when(configuration.getFromAddress()).thenReturn("bob@example.com");

        sender = new SmtpSender(configuration);
    }

    @Test
    public void send_happyPath() {
        try {
            sender.send("ANONYMOUS", "alice@example.com", "", "");
            fail("Exception expected");
        } catch (EmailSenderException e) {
            // Should fail because of lack of configuration (no host and so on)
            assertThat(e.getMessage()).contains("Could not send");
        }
    }

    @Test
    public void send_rateLimiting() {
        try {
            sender.send("ANONYMOUS", "alice@example.com", "", "");
            fail("Exception expected");
        } catch (EmailSenderException e) {
            // Should fail because of lack of configuration (no host and so on)
            assertThat(e.getMessage()).contains("Could not send");
        }

        try {
            sender.send("ANONYMOUS", "alice@example.com", "", "");
            fail("Exception expected");
        } catch (EmailSenderException e) {
            // Should fail because the limit is 1 mail/minute
            assertThat(e.getMessage()).contains("Rate limit");
        }
    }
}