package se.devscout.achievements.server.auth.password;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.CredentialsValidator;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.model.CredentialsType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

//TODO: A bit confusing with both a PasswordValidator and PasswordAuthenticator class.
public class PasswordValidator implements CredentialsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordValidator.class);

    private byte[] storedSecret;

    public PasswordValidator() {
    }

    public PasswordValidator(byte[] storedSecret) {
        this.storedSecret = storedSecret;
    }

    public PasswordValidator(SecretGenerator generator, final char[] plainTextPassword) {
        try {
            setSecret(generator, plainTextPassword);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public ValidationResult validate(char[] data) {
        try {
            final ByteArrayInputStream stream = new ByteArrayInputStream(storedSecret);
            final byte id = (byte) stream.read();
            final SecretGenerator generator = Stream.of(SecretGenerator.values()).filter(secretGenerator -> secretGenerator.getId() == id).findFirst().get();
            final boolean valid = generator.validatePassword(data, ByteStreams.toByteArray(stream));
            return new ValidationResult(null, null, valid);
        } catch (IOException e) {
            LOGGER.warn("Problem when validating password", e);
            return ValidationResult.INVALID;
        }
    }

    @Override
    public byte[] getCredentialsData() {
        return storedSecret;
    }

    @Override
    public CredentialsType getCredentialsType() {
        return CredentialsType.PASSWORD;
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
