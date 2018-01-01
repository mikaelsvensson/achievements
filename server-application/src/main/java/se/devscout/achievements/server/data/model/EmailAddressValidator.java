package se.devscout.achievements.server.data.model;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import org.apache.commons.validator.routines.EmailValidator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EmailAddressValidator implements ConstraintValidator<EmailAddress, String> {

    @Override
    public void initialize(EmailAddress constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (Strings.isNullOrEmpty(value)) {
            return true;
        }
        return EmailValidator.getInstance().isValid(value);
    }
}
