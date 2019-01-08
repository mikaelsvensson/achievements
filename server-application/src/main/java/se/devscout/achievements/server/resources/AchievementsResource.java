package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;
import se.devscout.achievements.server.data.model.AchievementStepProgressProperties;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.auth.User;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("achievements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AchievementsResource extends AbstractResource {
    private AchievementsDao dao;
    private AchievementStepProgressDao progressDao;
    private final AuditingDao auditingDao;
    private final PeopleDao peopleDao;

    public AchievementsResource(AchievementsDao dao, AchievementStepProgressDao progressDao, AuditingDao auditingDao, PeopleDao peopleDao) {
        this.dao = dao;
        this.progressDao = progressDao;
        this.auditingDao = auditingDao;
        this.peopleDao = peopleDao;
    }

    @GET
    @UnitOfWork
    @Path("{achievementId}/progress")
    public Map<String, ProgressDTO> getProgress(@PathParam("achievementId") UuidString id, @Auth User user) {
        try {
            final Achievement achievement = dao.read(id.getUUID());
            return progressDao
                    .get(achievement)
                    .stream()
                    .collect(Collectors.toMap(key -> key.getStep().getId() + "_" + key.getPerson().getId(), value -> map(value, ProgressDTO.class)));
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{achievementId}/progress-history")
    public List<StepProgressRequestLogRecordDTO> getProgressHistory(@PathParam("achievementId") UuidString id, @Auth User user) {
        try {
            final Achievement achievement = dao.read(id.getUUID());
            return auditingDao
                    .readStepProgress(achievement.getId())
                    .stream()
                    .map(record -> map(record, StepProgressRequestLogRecordDTO.class))
                    .collect(Collectors.toList());
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @RolesAllowed(Roles.READER)
    @UnitOfWork
    @Path("{achievementId}/awards")
    public List<PersonBaseDTO> getAwardedTo(@PathParam("achievementId") UuidString id,
                                            @Auth User user) {
        try {
            final Person userPerson = peopleDao.read(user.getPersonId());
            final Achievement achievement = dao.read(id.getUUID());
            return peopleDao.getByAwardedAchievement(userPerson.getOrganization(), achievement).stream()
                    .map(person -> map(person, PersonBaseDTO.class))
                    .collect(Collectors.toList());
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @POST
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{achievementId}/awards/{personId}")
    public void addAwardedTo(@PathParam("achievementId") UuidString id,
                             @PathParam("personId") Integer personId,
                             @Auth User user) {
        try {
            final Person person = peopleDao.read(personId);
            verifySameOrganization(user, person);

            final Achievement achievement = dao.read(id.getUUID());

            dao.addAwardedTo(achievement, person);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @DELETE
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{achievementId}/awards/{personId}")
    public void removeAwardedTo(@PathParam("achievementId") UuidString id,
                                @PathParam("personId") Integer personId,
                                @Auth User user) {
        try {
            final Person person = peopleDao.read(personId);
            verifySameOrganization(user, person);

            final Achievement achievement = dao.read(id.getUUID());

            dao.removeAwardedTo(achievement, person);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    private void verifySameOrganization(@Auth User user, Person person) throws ObjectNotFoundException {
        final UUID personOrgId = person.getOrganization().getId();
        final UUID userOrdId = peopleDao.read(user.getPersonId()).getOrganization().getId();
        if (userOrdId != personOrgId) {
            throw new NotFoundException("Person " + person.getId() + " not found in your organization.");
        }
    }

    @GET
    @UnitOfWork
    @Path("{achievementId}")
    public AchievementDTO get(@PathParam("achievementId") UuidString id) {
        try {
            return map(dao.read(id.getUUID()), AchievementDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @UnitOfWork
    public List<AchievementBaseDTO> find(@QueryParam("filter") String filter) {
        try {
            final List<Achievement> achievements = Strings.isNullOrEmpty(filter) ? dao.readAll() : dao.find(filter);
            return achievements.stream().map(o -> map(o, AchievementBaseDTO.class)).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    public Response create(AchievementDTO input, @Auth User user) {
        try {
            final Achievement achievement = dao.create(map(input, AchievementProperties.class));
            final URI location = uriInfo.getRequestUriBuilder().path(UuidString.toString(achievement.getId())).build();
            return Response
                    .created(location)
                    .entity(map(achievement, AchievementDTO.class))
                    .build();
        } catch (DaoException e) {
            return Response.serverError().build();
        }
    }

    @DELETE
    @RolesAllowed(Roles.EDITOR)
    @UnitOfWork
    @Path("{achievementId}")
    public Response delete(@PathParam("achievementId") UuidString id, @Auth User user) {
        try {
            verifyNotInProgress(id);

            dao.delete(id.getUUID());
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    private void verifyNotInProgress(UuidString id) throws ObjectNotFoundException {
        final Achievement achievement = dao.read(id.getUUID());
        final boolean isInProgressForOnePerson = achievement.getSteps().stream().flatMap(step -> step.getProgressList().stream()).anyMatch(AchievementStepProgressProperties::isCompleted);
        if (isInProgressForOnePerson) {
            throw new ClientErrorException(Response.Status.CONFLICT);
        }
    }
}
