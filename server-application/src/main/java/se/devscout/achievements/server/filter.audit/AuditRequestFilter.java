package se.devscout.achievements.server.filter.audit;

import org.apache.commons.io.input.TeeInputStream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.ByteArrayOutputStream;

@Priority(Priorities.AUTHENTICATION - 1)
class AuditRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        requestContext.setEntityStream(new TeeInputStream(requestContext.getEntityStream(), outputStream));
        requestContext.setProperty(AuditFeature.REQUEST_CONTEXT_PROPERTY_NAME, outputStream);
    }
}
