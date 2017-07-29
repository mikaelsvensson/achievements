package se.devscout.achievements.server.resources;

import org.modelmapper.ModelMapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

abstract class AbstractResource {
    final ModelMapper dtoMapper;

    @Context
    protected UriInfo uriInfo;

    AbstractResource() {
        dtoMapper = new ModelMapper();
        dtoMapper.getConfiguration().setFieldMatchingEnabled(true);
    }

    protected <S, T> T map(S input, Class<T> destinationType) {
        return input != null ? dtoMapper.map(input, destinationType) : null;
    }
}
