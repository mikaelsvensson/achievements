package se.devscout.achievements.server.data;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SlugGeneratorTest {

    private final SlugGenerator generator = new SlugGenerator();

    @Test
    public void toSlug_accented() {
        assertThat(generator.toSlug("\u00e5\u00e4\u00f6\u00fc\u00e9\u00e8\u00c5\u00c4\u00d6\u00dc\u00c9\u00c8")).isEqualTo("aaoueeaaouee");
    }

    @Test
    public void toSlug_sentence() {
        assertThat(generator.toSlug("Alice and Bob makes a good team.")).isEqualTo("alice-and-bob-makes-a-good-team");
    }

    @Test
    public void toSlug_nonLetters() {
        assertThat(generator.toSlug("abc#\u20ac%&def")).isEqualTo("abcdef");
    }

    @Test
    public void toSlug_whitespace() {
        assertThat(generator.toSlug("\t\nAl   \n    ice ")).isEqualTo("al-ice");
    }
}