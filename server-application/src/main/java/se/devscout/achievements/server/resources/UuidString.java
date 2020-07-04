package se.devscout.achievements.server.resources;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;

import java.util.Arrays;
import java.util.UUID;

public class UuidString {
    private static final BaseEncoding ENCODING = BaseEncoding.base32().omitPadding();

    private final String value;

    private final UUID uuid;

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
        final var bytes = ENCODING.decode(value.toUpperCase());
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Could not convert " + value.length() + " characters to 16 bytes.");
        }
        return new UUID(
                Longs.fromByteArray(Arrays.copyOfRange(bytes, 0, 8)),
                Longs.fromByteArray(Arrays.copyOfRange(bytes, 8, 16)));
    }

    public static String toString(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        final var idBytes = Bytes.concat(
                Longs.toByteArray(uuid.getMostSignificantBits()),
                Longs.toByteArray(uuid.getLeastSignificantBits()));
        return ENCODING.encode(idBytes).toLowerCase();
    }

    @Override
    public String toString() {
        return value;
    }
}
