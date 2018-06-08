package se.devscout.achievements.server.resources;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Provider
public class ResourceRequestRateLimiter implements ContainerRequestFilter {

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
            requestContext.abortWith(Response.status(429).build());
        }
    }
}
