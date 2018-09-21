package se.devscout.achievements.server.resources;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class WelcomeUserTemplateTest {

    private WelcomeUserTemplate template;

    @Before
    public void setUp() throws Exception {
        template = new WelcomeUserTemplate();
    }

    @Test
    public void render_googleOnly() {
        final String actual = template.render(
                URI.create("http://host/about/"),
                "alice@example.com",
                true,
                false,
                false,
                URI.create("http://host/login/"));

        assertThat(actual).contains("<a href=\"http://host/about/\">http://host/about/</a>");
        assertThat(actual).contains("<a href=\"http://host/login/\">http://host/login/</a>");
        assertThat(actual).contains("Logga in med Google");
        assertThat(actual).doesNotContain("Logga in med Microsoft");
        assertThat(actual).doesNotContain("Logga in med e-post");
        assertThat(actual).doesNotContain("alice@example.com");
    }

    @Test
    public void render_microsoftOnly() {
        final String actual = template.render(
                URI.create("http://host/about/"),
                "alice@example.com",
                false,
                true,
                false,
                URI.create("http://host/login/"));

        assertThat(actual).contains("<a href=\"http://host/about/\">http://host/about/</a>");
        assertThat(actual).contains("<a href=\"http://host/login/\">http://host/login/</a>");
        assertThat(actual).doesNotContain("Logga in med Google");
        assertThat(actual).contains("Logga in med Microsoft");
        assertThat(actual).doesNotContain("Logga in med e-post");
        assertThat(actual).doesNotContain("alice@example.com");
    }

    @Test
    public void render_noAuthMethods() {
        final String actual = template.render(
                URI.create("http://host/about/"),
                "alice@example.com",
                false,
                false,
                false,
                URI.create("http://host/login/"));

        assertThat(actual).contains("<a href=\"http://host/about/\">http://host/about/</a>");
        assertThat(actual).doesNotContain("<a href=\"http://host/login/\">http://host/login/</a>");
        assertThat(actual).doesNotContain("Logga in med Google");
        assertThat(actual).doesNotContain("Logga in med Microsoft");
        assertThat(actual).doesNotContain("Logga in med e-post");
        assertThat(actual).doesNotContain("alice@example.com");
    }

    @Test
    public void render_googleAndEmail() {
        final String actual = template.render(
                URI.create("http://host/about/"),
                "alice@example.com",
                true,
                false,
                true,
                URI.create("http://host/login/"));

        assertThat(actual).contains("<a href=\"http://host/about/\">http://host/about/</a>");
        assertThat(actual).contains("<a href=\"http://host/login/\">http://host/login/</a>");
        assertThat(actual).contains("Logga in med Google");
        assertThat(actual).doesNotContain("Logga in med Microsoft");
        assertThat(actual).contains("Logga in med e-post");
        assertThat(actual).contains("alice@example.com");
    }
}