package se.devscout.achievements.server.mail.template;

import com.google.common.collect.ImmutableMap;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public class SigninTemplate extends Template {
    public SigninTemplate() {
        super("assets/email.signin-email.html");
    }

    public String render(URI link, Duration linkValidTime) {
        final Map<String, String> parameters = ImmutableMap.of(
                "link", link.toString(),
                "validTime", String.valueOf(linkValidTime.toMinutes()));

        return render(parameters);
    }
}
