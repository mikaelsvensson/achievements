package se.devscout.achievements.server.resources;

import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidStringTest {
    @Test
    public void fromUUID() throws Exception {
        final var uuid = UUID.fromString("47498aea-4291-43b8-8277-4d480d5ed84a");
        assertThat(new UuidString(uuid).getValue()).isEqualTo("i5eyv2scsfb3ratxjvea2xwyji");
    }

    @Test
    public void toUUID() throws Exception {
        final var uuid = UUID.fromString("47498aea-4291-43b8-8277-4d480d5ed84a");
        assertThat(new UuidString("i5eyv2scsfb3ratxjvea2xwyji").getUUID()).isEqualTo(uuid);
    }

    @Test
    public void nullValues() {
        assertThat(new UuidString((String) null).getUUID()).isNull();
        assertThat(new UuidString((String) null).getValue()).isNull();
        assertThat(new UuidString((UUID) null).getUUID()).isNull();
        assertThat(new UuidString((UUID) null).getValue()).isNull();
    }
}