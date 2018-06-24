package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.AbstractAuditRecord;
import se.devscout.achievements.server.data.model.HttpAuditRecord;
import se.devscout.achievements.server.data.model.StepProgressAuditRecord;

import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.UUID;

public interface AuditingDao {
//    List<HttpAuditRecord> readSignIn(UUID organizationId);

    List<StepProgressAuditRecord> readStepProgress(UUID achievementId);

//    List<StepProgressAuditRecord> readStepProgress(UUID achievementId, Long userId);

//    List<HttpAuditRecord> readForgotPassword(Duration timespan);

//    List<Person> readActiveUsers(Duration timespan);

//    List<Person> readNotVeryActiveUsers(Duration timespan);

//    List<Person> readNoProfileUpdate(Duration timespan);

    StepProgressAuditRecord create(UUID trackingId, Integer userId, Integer stepId, Integer personId, String data, String httpMethod, int responseCode);

    HttpAuditRecord create(UUID trackingId, Integer userId, UriInfo uriInfo, String data, String httpMethod, String resourceUri, int responseCode);

    List<AbstractAuditRecord> readLatest(int limit);
}
