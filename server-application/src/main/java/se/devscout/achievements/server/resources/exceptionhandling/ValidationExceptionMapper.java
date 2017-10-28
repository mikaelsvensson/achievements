package se.devscout.achievements.server.resources.exceptionhandling;

import se.devscout.achievements.server.api.UnsuccessfulDTO;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

public class ValidationExceptionMapper extends GenericExceptionMapper<ValidationException> {

    @Override
    protected UnsuccessfulDTO createDto(ValidationException exception) {
        return new UnsuccessfulDTO(exception.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
    }
}
