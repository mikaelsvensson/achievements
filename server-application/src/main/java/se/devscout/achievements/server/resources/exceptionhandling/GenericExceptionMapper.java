package se.devscout.achievements.server.resources.exceptionhandling;

import se.devscout.achievements.server.api.UnsuccessfulDTO;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class GenericExceptionMapper<E extends Exception> implements ExceptionMapper<E> {
    @Override
    public Response toResponse(E exception) {
        UnsuccessfulDTO dto = createDto(exception);
        return Response.status(dto.status).entity(dto).build();
    }

    protected abstract UnsuccessfulDTO createDto(E exception);

}
