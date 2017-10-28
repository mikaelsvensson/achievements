package se.devscout.achievements.server.resources;

import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidStringTest {
    @Test
    public void fromUUID() throws Exception {
        final UUID uuid = UUID.fromString("bb1879bc-9967-4a50-aff1-25ee57a86346");
        assertThat(new UuidString(uuid).getValue()).isEqualTo("uxh5vJlnSlCv8SXuV6hjRg");
    }

    @Test
    public void toUUID() throws Exception {
        final UUID uuid = UUID.fromString("bb1879bc-9967-4a50-aff1-25ee57a86346");
        assertThat(new UuidString("uxh5vJlnSlCv8SXuV6hjRg").getUUID()).isEqualTo(uuid);
    }
}