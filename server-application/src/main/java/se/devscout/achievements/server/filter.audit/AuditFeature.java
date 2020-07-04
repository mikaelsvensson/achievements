package se.devscout.achievements.server.filter.audit;

import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import se.devscout.achievements.server.data.dao.AuditingDao;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class AuditFeature implements DynamicFeature {
    static final String REQUEST_CONTEXT_PROPERTY_NAME = "AuditFeature.payloadStream";

    private final AuditingDao auditingDao;

    private HibernateBundle hibernate;

    public AuditFeature(AuditingDao auditingDao, HibernateBundle hibernate) {
        this.auditingDao = auditingDao;
        this.hibernate = hibernate;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final var method = resourceInfo.getResourceMethod();
        if (method.isAnnotationPresent(Audited.class)) {
            final var annotation = method.getAnnotation(Audited.class);

            if (annotation.logRequest()) {
                context.register(new AuditRequestFilter());
            }
            context.register(new UnitOfWorkAwareProxyFactory(hibernate).create(AuditResponseFilter.class, AuditingDao.class, auditingDao));
        }
    }
}
