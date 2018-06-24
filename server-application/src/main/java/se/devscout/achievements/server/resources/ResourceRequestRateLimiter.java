package se.devscout.achievements.server.resources;

import se.devscout.achievements.server.api.UnsuccessfulDTO;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Provider
public class ResourceRequestRateLimiter implements ContainerRequestFilter {

    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    private final RateLimiter rateLimiter;

    public ResourceRequestRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final String user = Optional.ofNullable(requestContext.getSecurityContext().getUserPrincipal())
                .map(Principal::getName)
                .orElse("ANONYMOUS");
        if (!rateLimiter.accept(user)) {
            requestContext.abortWith(Response
                    .status(HTTP_STATUS_TOO_MANY_REQUESTS)
                    .entity(new UnsuccessfulDTO("Too many request", HTTP_STATUS_TOO_MANY_REQUESTS))
                    .build());
        }
    }
}
