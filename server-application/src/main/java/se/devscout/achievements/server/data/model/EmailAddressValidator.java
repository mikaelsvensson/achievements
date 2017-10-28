package se.devscout.achievements.server.data.model;

import org.apache.commons.validator.routines.EmailValidator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EmailAddressValidator implements ConstraintValidator<EmailAddress, String> {

    @Override
    public void initialize(EmailAddress constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return EmailValidator.getInstance().isValid(value);
    }
}
