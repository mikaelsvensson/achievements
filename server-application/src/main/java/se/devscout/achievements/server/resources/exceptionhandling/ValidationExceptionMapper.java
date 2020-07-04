package se.devscout.achievements.server.resources.exceptionhandling;

import se.devscout.achievements.server.api.UnsuccessfulDTO;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.stream.Collectors;

public class ValidationExceptionMapper extends GenericExceptionMapper<ValidationException> {

    @Override
    protected UnsuccessfulDTO createDto(ValidationException exception) {
        if (exception instanceof ConstraintViolationException) {
            var constraintViolationException = (ConstraintViolationException) exception;
            var message = constraintViolationException.getConstraintViolations().stream()
                    .map(o -> MessageFormat.format("{0} is not a valid value for {1}", o.getInvalidValue(), o.getPropertyPath().toString()))
                    .collect(Collectors.joining(", "));

            return new UnsuccessfulDTO(message, Response.Status.BAD_REQUEST.getStatusCode());
        } else {
            return new UnsuccessfulDTO(exception.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }
}
