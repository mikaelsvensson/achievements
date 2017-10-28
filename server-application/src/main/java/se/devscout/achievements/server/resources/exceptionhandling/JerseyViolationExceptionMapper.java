package se.devscout.achievements.server.resources.exceptionhandling;

import io.dropwizard.jersey.validation.JerseyViolationException;
import se.devscout.achievements.server.api.UnsuccessfulDTO;

import javax.ws.rs.core.Response;

public class JerseyViolationExceptionMapper extends GenericExceptionMapper<JerseyViolationException> {
    @Override
    protected UnsuccessfulDTO createDto(JerseyViolationException exception) {
        return new UnsuccessfulDTO(exception.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
    }
}
