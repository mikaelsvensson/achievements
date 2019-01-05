package se.devscout.achievements.server.mail.template;

import com.google.common.collect.ImmutableMap;

import java.net.URI;
import java.util.Map;

public class SetPasswordTemplate extends Template {
    public SetPasswordTemplate() {
        super("assets/email.set-password.html");
    }

    public String render(URI link) {
        final Map<String, String> parameters = ImmutableMap.of(
                "link", link.toString());

        return render(parameters);
    }
}
