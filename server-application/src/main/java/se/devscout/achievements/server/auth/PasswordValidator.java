package se.devscout.achievements.server.auth;

import com.google.common.io.ByteStreams;
import se.devscout.achievements.server.data.model.IdentityProvider;

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
    public SecretValidationResult validate(char[] secret) {
        try {
            final ByteArrayInputStream stream = new ByteArrayInputStream(storedSecret);
            final byte id = (byte) stream.read();
            final SecretGenerator generator = Stream.of(SecretGenerator.values()).filter(secretGenerator -> secretGenerator.getId() == id).findFirst().get();
            final boolean valid = generator.validatePassword(secret, ByteStreams.toByteArray(stream));
            return new SecretValidationResult(null, null, valid);
        } catch (IOException e) {
            //TODO: Log exception?
            //TODO: Use SecretValidationResult.INVALID instead?
            return new SecretValidationResult(null, null, false);
        }
    }

    @Override
    public byte[] getSecret() {
        return storedSecret;
    }

    @Override
    public IdentityProvider getIdentityProvider() {
        return IdentityProvider.PASSWORD;
    }

    private void setSecret(SecretGenerator generator, final char[] plainTextPassword) throws IOException {
        if (plainTextPassword == null || plainTextPassword.length == 0) {
            throw new IOException("Password cannot be empty");
        }
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(generator.getId());
        stream.write(generator.generateSecret(plainTextPassword));
        storedSecret = stream.toByteArray();
    }

}
