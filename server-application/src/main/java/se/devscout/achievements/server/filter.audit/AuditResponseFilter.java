package se.devscout.achievements.server.filter.audit;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.data.dao.AuditingDao;
import se.devscout.achievements.server.data.model.AbstractAuditRecord;
import se.devscout.achievements.server.resources.AchievementStepProgressResource;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

@Priority(Priorities.AUTHENTICATION - 1)
class AuditResponseFilter implements ContainerResponseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditResponseFilter.class);
    private final AuditingDao dao;

    public AuditResponseFilter(AuditingDao dao) {
        this.dao = dao;
    }

    @Override
    @UnitOfWork(transactional = false)
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        try {
            final ByteArrayOutputStream baos = (ByteArrayOutputStream) requestContext.getProperty(AuditFeature.REQUEST_CONTEXT_PROPERTY_NAME);
            String payload = null;
            if (baos != null) {
                payload = new String(baos.toByteArray());
            }
            final AbstractAuditRecord logRecord = createRecord(requestContext, payload, responseContext);
            final String msg = String.format(
                    "Request by %s for %s:%s returned %d",
                    requestContext.getSecurityContext().getUserPrincipal(),
                    logRecord.getClass().getSimpleName(),
                    requestContext.getUriInfo().getPathParameters(),
                    responseContext.getStatus());
            LOGGER.info(msg);
        } catch (Exception e) {
            LOGGER.warn("Could not log request", e);
        }
    }

    private AbstractAuditRecord createRecord(ContainerRequestContext requestContext, String data, ContainerResponseContext responseContext) throws Exception {
        final UriInfo uriInfo = requestContext.getUriInfo();
        final Integer userId = Optional.ofNullable(requestContext.getSecurityContext().getUserPrincipal())
                .map(principal -> ((User) principal).getPersonId())
                .orElse(null);

        if (userId != null) {
            final boolean isSuccessfulHttpStatus = responseContext.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
            final boolean isStepProgressRequest = uriInfo.getMatchedResources().stream().anyMatch(AchievementStepProgressResource.class::isInstance);
            if (isSuccessfulHttpStatus && isStepProgressRequest) {
                return dao.create(
                        null,
                        userId,
                        Optional.ofNullable(uriInfo.getPathParameters().getFirst("stepId")).map(Integer::valueOf).orElse(null),
                        Optional.ofNullable(uriInfo.getPathParameters().getFirst("personId")).map(Integer::valueOf).orElse(null),
                        data,
                        requestContext.getMethod(),
                        responseContext.getStatus());
            } else {
                return dao.create(
                        null,
                        userId,
                        uriInfo,
                        data,
                        requestContext.getMethod(),
                        requestContext.getUriInfo().getRequestUri().toString(),
                        responseContext.getStatus());
            }
        } else {
            throw new Exception("No user");
        }
    }
}
