package se.devscout.achievements.server.data.dao;

import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuditingDaoImpl extends DaoImpl<AbstractAuditRecord, Integer> implements AuditingDao {
    public AuditingDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public List<StepProgressAuditRecord> readStepProgress(UUID achievementId) {
        return list(namedQuery("StepProgressAuditRecord.byAchievement")
                .setParameter("achievementId", achievementId));
    }

    @Override
    public HttpAuditRecord create(UUID trackingId, Integer userId, UriInfo uriInfo, String data, String httpMethod, String resourceUri, int responseCode) {
        final var record = new HttpAuditRecord(
                Optional.ofNullable(userId).map(id -> currentSession().getReference(Person.class, id)).orElse(null),
                data,
                httpMethod,
                resourceUri,
                responseCode
        );
        return (HttpAuditRecord) persist(record);
    }

    @Override
    public List<AbstractAuditRecord> readLatest(int limit) {
        return list(namedQuery("AbstractAuditRecord.readAllReverse").setMaxResults(limit));
    }

    @Override
    public StepProgressAuditRecord create(UUID trackingId, Integer userId, Integer stepId, Integer personId, String data, String httpMethod, int responseCode) {
        final var user = currentSession().getReference(Person.class, userId);
        final var person = currentSession().getReference(Person.class, personId);
        final var step = currentSession().getReference(AchievementStep.class, stepId);
        final var record = new StepProgressAuditRecord(
                user,
                data,
                step,
                person,
                httpMethod,
                responseCode
        );
        user.getAuditRecords().add(record);
        step.getAuditRecords().add(record);
        return (StepProgressAuditRecord) persist(record);
    }

}
