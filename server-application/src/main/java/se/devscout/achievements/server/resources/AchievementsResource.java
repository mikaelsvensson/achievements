package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.AchievementsApplication;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.htmlprovider.DatabaseCachedHtmlProvider;
import se.devscout.achievements.server.data.importer.badges.BadgeImporter;
import se.devscout.achievements.server.data.importer.badges.BadgeImporterException;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.auth.User;
import se.devscout.achievements.server.util.diff.Diff;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Path("achievements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AchievementsResource extends AbstractResource {
    private final AchievementsDao dao;
    private final AchievementStepsDao stepsDao;
    private final AchievementStepProgressDao progressDao;
    private final AuditingDao auditingDao;
    private final PeopleDao peopleDao;
    private final BadgeImporter badgeImporter;

    public AchievementsResource(AchievementsDao dao,
                                AchievementStepsDao stepsDao,
                                AchievementStepProgressDao progressDao,
                                AuditingDao auditingDao,
                                PeopleDao peopleDao,
                                CachedHtmlDao cachedHtmlDao) {
        this.dao = dao;
        this.stepsDao = stepsDao;
        this.progressDao = progressDao;
        this.auditingDao = auditingDao;
        this.peopleDao = peopleDao;
        this.badgeImporter = new BadgeImporter(new DatabaseCachedHtmlProvider(cachedHtmlDao));
    }

    @GET
    @UnitOfWork
    @Path("{achievementId}/progress")
    public Map<String, ProgressDTO> getProgress(@PathParam("achievementId") UuidString id, @Auth User user) {
        try {
            final var achievement = dao.read(id.getUUID());
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
            final var achievement = dao.read(id.getUUID());
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
            final var userPerson = peopleDao.read(user.getPersonId());
            final var achievement = dao.read(id.getUUID());
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
            final var person = peopleDao.read(personId);
            verifySameOrganization(user, person);

            final var achievement = dao.read(id.getUUID());

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
            final var person = peopleDao.read(personId);
            verifySameOrganization(user, person);

            final var achievement = dao.read(id.getUUID());

            dao.removeAwardedTo(achievement, person);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    private void verifySameOrganization(@Auth User user, Person person) throws ObjectNotFoundException {
        final var personOrgId = person.getOrganization().getId();
        final var userOrdId = peopleDao.read(user.getPersonId()).getOrganization().getId();
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
            final var achievements = Strings.isNullOrEmpty(filter) ? dao.readAll() : dao.find(filter);
            return achievements.stream().map(o -> map(o, AchievementBaseDTO.class)).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @RolesAllowed(Roles.ADMIN)
    @UnitOfWork
    public Response create(AchievementDTO input, @Auth User user) {
        try {
            final var achievement = dao.create(map(input, AchievementProperties.class));

            updateSteps(input, achievement);

            final var location = uriInfo.getRequestUriBuilder().path(UuidString.toString(achievement.getId())).build();
            return Response
                    .created(location)
                    .entity(map(achievement, AchievementDTO.class))
                    .build();
        } catch (DaoException | ObjectNotFoundException e) {
            return Response.serverError().build();
        }
    }

    @DELETE
    @RolesAllowed(Roles.ADMIN)
    @UnitOfWork
    @Path("{achievementId}")
    public Response delete(@PathParam("achievementId") UuidString id,
                           @Auth User user,
                           @HeaderParam(AchievementsApplication.HEADER_IN_PROGRESS_CHECK) InProgressCheck inProgressCheck) {
        try {
            if (inProgressCheck != InProgressCheck.SKIP) {
                verifyNotInProgress(id);
            }

            dao.delete(id.getUUID());
            return Response.noContent().build();
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        }
    }

    @PUT
    @RolesAllowed(Roles.ADMIN)
    @UnitOfWork
    @Path("{achievementId}")
    public AchievementDTO update(@PathParam("achievementId") UuidString id,
                                 AchievementDTO input,
                                 @Auth User user,
                                 @HeaderParam(AchievementsApplication.HEADER_IN_PROGRESS_CHECK) InProgressCheck inProgressCheck) {
        try {
            if (inProgressCheck != InProgressCheck.SKIP) {
                verifyNotInProgress(id);
            }

            final var achievement = dao.read(id.getUUID());

            dao.update(id.getUUID(), map(input, AchievementProperties.class));

            updateSteps(input, achievement);

            return map(achievement, AchievementDTO.class);
        } catch (ObjectNotFoundException e) {
            throw new NotFoundException();
        } catch (DaoException e) {
            throw new InternalServerErrorException();
        }
    }

    private void updateSteps(AchievementDTO input, Achievement achievement) throws ObjectNotFoundException, DaoException {
        // Remove steps not present in input data
        final var existingSteps = achievement.getSteps();
        final var inputSteps = Optional.ofNullable(input.steps).orElse(Collections.emptyList());
        final var desiredStepIds = inputSteps.stream()
                .filter(Objects::nonNull) // Ignore null values from "steps list". Migration 17_achievement_steps_order_default_values.xml bootstraps the sort_order column in a way which causes Hibernate to add a lot of null values to the list.
                .filter(step -> step.id != null && step.id > 0)
                .map(step -> step.id)
                .collect(Collectors.toSet());
        final var removedStepIds = existingSteps.stream()
                .filter(Objects::nonNull) // Ignore null values from "steps list". Migration 17_achievement_steps_order_default_values.xml bootstraps the sort_order column in a way which causes Hibernate to add a lot of null values to the list.
                .map(AchievementStep::getId)
                .filter(stepId -> !desiredStepIds.contains(stepId))
                .collect(Collectors.toSet());
        for (Integer removedStepId : removedStepIds) {
            stepsDao.delete(removedStepId);
        }

        final var newStepList = new ArrayList<AchievementStep>();
        for (AchievementStepDTO step : inputSteps) {
            if (step.id != null && step.id > 0) {
                // Update existing step
                newStepList.add(stepsDao.update(step.id, map(step, AchievementStepProperties.class)));
            } else {
                // Create new step
                newStepList.add(stepsDao.create(achievement, map(step, AchievementStepProperties.class)));
            }
        }
        existingSteps.clear();
        existingSteps.addAll(newStepList);
    }

    private void verifyNotInProgress(UuidString id) throws ObjectNotFoundException {
        final var achievement = dao.read(id.getUUID());
        final var isInProgressForOnePerson = achievement.getSteps().stream()
                .filter(Objects::nonNull) // Ignore null values from "steps list". Migration 17_achievement_steps_order_default_values.xml bootstraps the sort_order column in a way which causes Hibernate to add a lot of null values to the list.
                .flatMap(step -> step.getProgressList().stream())
                .anyMatch(AchievementStepProgressProperties::isCompleted);
        if (isInProgressForOnePerson) {
            throw new ClientErrorException(Response.Status.CONFLICT);
        }
    }

    @GET
    @RolesAllowed(Roles.ADMIN)
    @UnitOfWork
    @Path("scouterna-se-badges")
    public Response readScouternaSeBadges(@Auth User user) {
        try {
            final var parsedAchievements = badgeImporter.get();
            final var storedAchievements = dao.readAll().stream().map(o -> map(o, AchievementDTO.class)).collect(Collectors.toList());

            final var list = parsedAchievements.stream()
                    .map(parsedAchievement -> new ScouternaSeBadgeDTO(
                            parsedAchievement,
                            storedAchievements.stream()
                                    .filter(sa -> sa.slug.equals(parsedAchievement.slug))
                                    .findFirst()
                                    .orElse(null),
                            null,
                            0))
                    .collect(Collectors.toCollection(ArrayList::new));

            list.addAll(storedAchievements.stream()
                    .filter(sa -> list.stream()
                            .noneMatch(pa -> pa.fromScouternaSe.slug.equals(sa.slug)))
                    .map(sa -> new ScouternaSeBadgeDTO(null, sa, null, 0))
                    .collect(Collectors.toList()));

            list.sort(Comparator.comparing(
                    dto -> Objects.requireNonNullElse(dto.fromScouternaSe, dto.fromDatabase),
                    (dto1, sto2) -> Objects.requireNonNullElse(dto1.name, "").compareTo(sto2.name)));

            // Calculate diff between text from www.scouterna.se and text stored in database
            list.stream()
                    // Only calculate diff for achievements which exist on "both sides"
                    .filter(dto -> dto.fromScouternaSe != null && dto.fromDatabase != null)
                    .forEach(dto -> {
                        dto.diffs = Diff.diff(asPlainText(dto.fromDatabase), asPlainText(dto.fromScouternaSe));
                    });

            list.stream()
                    .filter(dto -> dto.fromDatabase != null)
                    .forEach(dto -> {
                        try {
                            // Similar, but not identical, to se.devscout.achievements.server.resources.AchievementStepsResource.verifyNotInProgress
                            dto.affected_people_count = progressDao.get(dao.read(UuidString.toUUID(dto.fromDatabase.id))).stream()
                                    .filter(AchievementStepProgressProperties::isCompleted)
                                    .map(p -> p.getPerson().getId())
                                    .distinct()
                                    .count();
                        } catch (ObjectNotFoundException e) {
                            e.printStackTrace();
                        }
                    });

            return Response.ok(list).build();
        } catch (BadgeImporterException e) {
            return Response.serverError().build();
        }
    }

    private String asPlainText(AchievementDTO dto) {
        return String.join("\n",
                dto.name,
                dto.tags.stream().sorted().collect(Collectors.joining(", ")),
                dto.image.toString(),
                dto.description,
                Optional.ofNullable(dto.steps)
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(java.util.Objects::nonNull) // Ignore null values from "steps list". Migration 17_achievement_steps_order_default_values.xml bootstraps the sort_order column in a way which causes Hibernate to add a lot of null values to the list.
                        .map(step -> "- " + step.description)
                        .collect(Collectors.joining("\n"))
        ).replace("\n\n", "\n");
    }
}
