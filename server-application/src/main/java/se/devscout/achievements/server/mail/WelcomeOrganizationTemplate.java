package se.devscout.achievements.server.mail;

import com.google.common.collect.ImmutableMap;

import java.net.URI;

public class WelcomeOrganizationTemplate extends Template {
    public WelcomeOrganizationTemplate() {
        super("assets/email.welcome-organization.html");
    }

    public String render(final URI achievementsLink,
                         final URI peopleAddLink,
                         final URI peopleImportLink,
                         final URI peopleFindLink,
                         final URI aboutLink) {
        return render(ImmutableMap.<String, String>builder()
                .put("achievementsLink", achievementsLink.toString())
                .put("peopleAddLink", peopleAddLink.toString())
                .put("peopleImportLink", peopleImportLink.toString())
                .put("peopleFindLink", peopleFindLink.toString())
                .put("aboutLink", aboutLink.toString())
                .build());
    }
}
