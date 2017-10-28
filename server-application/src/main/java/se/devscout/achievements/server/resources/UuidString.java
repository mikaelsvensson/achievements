package se.devscout.achievements.server.resources;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class UuidString {
    private static Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static Base64.Decoder DECODER = Base64.getUrlDecoder();

    private String value;

    private UUID uuid;

    public UuidString(String value) {
        this.value = value;
        this.uuid = toUUID(value);
    }

    public UuidString(UUID value) {
        this.value = toString(value);
        this.uuid = value;
    }

    public String getValue() {
        return value;
    }

    public UUID getUUID() {
        return uuid;
    }


    public static UUID toUUID(String value) {
        if (value == null) {
            return null;
        }
        final byte[] bytes = DECODER.decode(value);
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Could not convert " + value.length() + " characters to 16 bytes.");
        }
        return new UUID(Longs.fromByteArray(Arrays.copyOfRange(bytes, 0, 8)), Longs.fromByteArray(Arrays.copyOfRange(bytes, 8, 16)));
    }

    public static String toString(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        final byte[] idBytes = Bytes.concat(Longs.toByteArray(uuid.getMostSignificantBits()), Longs.toByteArray(uuid.getLeastSignificantBits()));
        return ENCODER.encodeToString(idBytes);
    }

    @Override
    public String toString() {
        return value;
    }
}
