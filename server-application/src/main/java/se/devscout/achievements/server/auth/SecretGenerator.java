package se.devscout.achievements.server.auth;

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
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                byte[] salt = new byte[32];
                new SecureRandom().nextBytes(salt);
                final int iterations = 1000;
                final int keyLength = 256;
                PBEKeySpec spec = new PBEKeySpec(plainTextPassword, salt, iterations, keyLength);
                SecretKey key = skf.generateSecret(spec);
                final ByteArrayDataOutput output = ByteStreams.newDataOutput();
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
                final byte[] salt = new byte[32];
                final ByteArrayDataInput input = ByteStreams.newDataInput(storedSecret);
                input.readFully(salt, 0, salt.length);
                final int interations = input.readInt();
                final int keyLength = input.readInt();
                final byte[] storedKey = new byte[storedSecret.length - 4 - 4 - salt.length];
                input.readFully(storedKey);

                PBEKeySpec spec = new PBEKeySpec(plainTextPassword, salt, interations, keyLength);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                SecretKey key = skf.generateSecret(spec);
                final byte[] passwordKey = key.getEncoded();
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
