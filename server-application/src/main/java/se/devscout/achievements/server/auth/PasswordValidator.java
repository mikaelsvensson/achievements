package se.devscout.achievements.server.auth;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

public class PasswordValidator implements SecretValidator {

    private byte[] storedSecret;

    public PasswordValidator() {
    }

    public PasswordValidator(byte[] storedSecret) {
        this.storedSecret = storedSecret;
    }

    public PasswordValidator(SecretGenerator generator, final char[] plainTextPassword) throws IOException {
        setSecret(generator, plainTextPassword);
    }

    @Override
    public boolean validate(char[] secret) {
        try {
            final ByteArrayInputStream stream = new ByteArrayInputStream(storedSecret);
            final byte id = (byte) stream.read();
            final SecretGenerator generator = Stream.of(SecretGenerator.values()).filter(secretGenerator -> secretGenerator.getId() == id).findFirst().get();
            return generator.validatePassword(secret, ByteStreams.toByteArray(stream));
        } catch (IOException e) {
            //TODO: Log exception?
            return false;
        }
    }

    @Override
    public byte[] getSecret() {
        return storedSecret;
    }

    private void setSecret(SecretGenerator generator, final char[] plainTextPassword) throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(generator.getId());
        stream.write(generator.generateSecret(plainTextPassword));
        storedSecret = stream.toByteArray();
    }

}
