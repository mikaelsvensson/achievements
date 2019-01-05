package se.devscout.achievements.server.mail.template;

import com.google.common.collect.ImmutableMap;

import java.net.URI;

public class WelcomeUserTemplate extends Template {
    public WelcomeUserTemplate() {
        super("assets/email.welcome-user.html");
    }

    public String render(final URI aboutLink, final String email, final boolean isGoogleAccount, final boolean isMicrosoftAccount, final boolean isEmailAccount, final URI loginLink) {
        return render(ImmutableMap.<String, String>builder()
                .put("loginLink", loginLink.toString())
                .put("isGoogleAccount", toBoolean(isGoogleAccount))
                .put("isMicrosoftAccount", toBoolean(isMicrosoftAccount))
                .put("isEmailAccount", toBoolean(isEmailAccount))
                // TODO: Escape e-mail address to prevent HTML injection attacks
                .put("emailAddress", email)
                .put("aboutLink", aboutLink.toString())
                .build());
    }

    private String toBoolean(boolean value) {
        return value ? "true" : "";
    }
}
