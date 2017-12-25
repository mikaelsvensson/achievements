package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.User;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.DaoException;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrganizationsResource extends AbstractResource {
    private OrganizationsDao dao;
    private AchievementsDao achievementsDao;

    public OrganizationsResource(OrganizationsDao dao, AchievementsDao achievementsDao) {
        this.dao = dao;
        this.achievementsDao = achievementsDao;
    }

    @GET
    @Path("{organizationId}")
    @UnitOfWork
    public OrganizationDTO get(@PathParam("organizationId") UuidString id, @Auth User user) {
        try {
            final Organization organization = dao.read(id.getUUID());
            return map(organization, OrganizationDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("{organizationId}/basic")
    @UnitOfWork
    public OrganizationBaseDTO getBasic(@PathParam("organizationId") UuidString id) {
        try {
            final Organization organization = dao.read(id.getUUID());
            return map(organization, OrganizationBaseDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("{organizationId}/achievement-summary")
    @UnitOfWork
    public OrganizationAchievementSummaryDTO getAchievementSummary(@PathParam("organizationId") UuidString id, @Auth User user) {
        try {
            final Organization organization = dao.read(id.getUUID());

            final OrganizationAchievementSummaryDTO summary = new OrganizationAchievementSummaryDTO();

            final List<Achievement> achievements = achievementsDao.findWithProgressForOrganization(organization);
            for (Achievement achievement : achievements) {
                final int stepCount = achievement.getSteps().size();
                final Map<Person, Long> completedStepsByPerson = achievement.getSteps().stream()
                        .flatMap(achievementStep -> achievementStep.getProgressList().stream())
                        .filter(AchievementStepProgressProperties::isCompleted)
                        .collect(Collectors.toMap(
                                AchievementStepProgress::getPerson,
                                progressValue -> 1L,
                                (u, u2) -> u + u2));
                final OrganizationAchievementSummaryDTO.ProgressSummaryDTO progressSummary = new OrganizationAchievementSummaryDTO.ProgressSummaryDTO();

                List<OrganizationAchievementSummaryDTO.PersonProgressDTO> progressDetailed = null;
                if (stepCount > 0) {
                    progressSummary.people_completed = (int) completedStepsByPerson.entrySet().stream()
                            .filter(entry -> entry.getValue() == stepCount)
                            .count();

                    progressSummary.people_started = (int) completedStepsByPerson.entrySet().stream()
                            .filter(entry -> entry.getValue() < stepCount)
                            .count();

                    progressDetailed = completedStepsByPerson.entrySet().stream().map(entry -> {
                        final OrganizationAchievementSummaryDTO.PersonProgressDTO personProgress = new OrganizationAchievementSummaryDTO.PersonProgressDTO();
                        personProgress.percent = (int) Math.round(100.0 * entry.getValue() / stepCount);
                        personProgress.person = new PersonBaseDTO(entry.getKey().getId(), entry.getKey().getName());
                        return personProgress;
                    }).collect(Collectors.toList());
                }

                final OrganizationAchievementSummaryDTO.AchievementSummaryDTO achievementSummary = new OrganizationAchievementSummaryDTO.AchievementSummaryDTO();
                achievementSummary.achievement = map(achievement, AchievementBaseDTO.class);
                achievementSummary.progress_summary = progressSummary;
                achievementSummary.progress_detailed = progressDetailed;
                summary.achievements.add(achievementSummary);
            }

            return summary;
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @GET
    @UnitOfWork
    public List<OrganizationDTO> find(@QueryParam("filter") String filter, @Auth User user) {
        try {
            return dao.find(filter).stream().map(o -> map(o, OrganizationDTO.class)).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @UnitOfWork
    public Response create(OrganizationDTO input,
                           @Auth User user) {
        try {
            final Organization organization = dao.create(map(input, OrganizationProperties.class));
            final URI location = uriInfo.getRequestUriBuilder().path(UuidString.toString(organization.getId())).build();
            return Response
                    .created(location)
                    .entity(map(organization, OrganizationDTO.class))
                    .build();
        } catch (DaoException e) {
            return Response.serverError().build();
        }
    }

    @PUT
    @UnitOfWork
    @Path("{organizationId}")
    public Response update(@PathParam("organizationId") UuidString id,
                           OrganizationDTO input,
                           @Auth User user) {
        try {
            final Organization organization = dao.update(id.getUUID(), map(input, OrganizationProperties.class));
            return Response
                    .ok()
                    .entity(map(organization, OrganizationDTO.class))
                    .build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException("Could not find " + id.toString());
        }
    }

    @DELETE
    @UnitOfWork
    @Path("{organizationId}")
    public Response delete(@PathParam("organizationId") UuidString id,
                           @Auth User user) {
        try {
            dao.delete(id.getUUID());
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }
}
