package se.devscout.achievements.server.auth.password;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public enum SecretGenerator {
    PDKDF2(1) {
        @Override
        // See https://www.owasp.org/index.php/Hashing_Java
        final byte[] generateSecret(final char[] plainTextPassword) {
            try {
                var skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                var salt = new byte[32];
                new SecureRandom().nextBytes(salt);
                final var iterations = 1000;
                final var keyLength = 256;
                var spec = new PBEKeySpec(plainTextPassword, salt, iterations, keyLength);
                var key = skf.generateSecret(spec);
                final var output = ByteStreams.newDataOutput();
                output.write(salt);
                output.writeInt(iterations);
                output.writeInt(keyLength);
                output.write(key.getEncoded());
                return output.toByteArray();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        boolean validatePassword(char[] plainTextPassword, byte[] storedSecret) {
            try {
                final var salt = new byte[32];
                final var input = ByteStreams.newDataInput(storedSecret);
                input.readFully(salt, 0, salt.length);
                final var interations = input.readInt();
                final var keyLength = input.readInt();
                final var storedKey = new byte[storedSecret.length - 4 - 4 - salt.length];
                input.readFully(storedKey);

                var spec = new PBEKeySpec(plainTextPassword, salt, interations, keyLength);
                var skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                var key = skf.generateSecret(spec);
                final var passwordKey = key.getEncoded();
                return Arrays.equals(passwordKey, storedKey);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                return false;
            }
        }
    };

    private final byte id;

    SecretGenerator(int id) {
        this.id = Integer.valueOf(id).byteValue();
    }

    abstract byte[] generateSecret(final char[] plainTextPassword);

    abstract boolean validatePassword(final char[] plainTextPassword, byte[] storedSecret);

    public byte getId() {
        return id;
    }
}
