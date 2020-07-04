package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.NameTokenizers;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractResource {
    final ModelMapper toDtoMapper;
    final ModelMapper fromDtoMapper;

    @Context
    protected UriInfo uriInfo;

    protected AbstractResource() {
        toDtoMapper = new ModelMapper();
        toDtoMapper.getConfiguration().setFieldMatchingEnabled(true);
        toDtoMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.CAMEL_CASE);
        toDtoMapper.getConfiguration().setDestinationNameTokenizer(NameTokenizers.UNDERSCORE);

        fromDtoMapper = new ModelMapper();
        fromDtoMapper.getConfiguration().setFieldMatchingEnabled(true);
        fromDtoMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
        fromDtoMapper.getConfiguration().setDestinationNameTokenizer(NameTokenizers.CAMEL_CASE);
    }

    protected <S, T> T map(S src, Class<T> destinationType) {
        if (src != null) {
            final T dest;
            if (src.getClass().getSimpleName().endsWith("DTO")) {
                dest = fromDtoMapper.map(src, destinationType);
            } else if (destinationType.getSimpleName().endsWith("DTO")) {
                dest = toDtoMapper.map(src, destinationType);
            } else {
                dest = MoreObjects.firstNonNull(
                        toDtoMapper.map(src, destinationType),
                        fromDtoMapper.map(src, destinationType));
            }
            if (src instanceof AchievementStep && dest instanceof AchievementStepDTO) {
                mapAchievementStepExtras((AchievementStep) src, (AchievementStepDTO) dest);
            }
            if (src instanceof Achievement && dest instanceof AchievementBaseDTO) {
                var entity = (Achievement) src;
                var dto = (AchievementBaseDTO) dest;
                dto.id = UuidString.toString(entity.getId());
            }
            if (src instanceof Achievement && dest instanceof AchievementDTO) {
                mapAchievementExtras((Achievement) src, (AchievementDTO) dest);
            }
            if (src instanceof Organization && dest instanceof OrganizationDTO) {
                var entity = (Organization) src;
                var dto = (OrganizationDTO) dest;
                dto.id = UuidString.toString(entity.getId());
            }
            if (src instanceof PersonDTO && dest instanceof PersonProperties) {
                mapPersonExtras((PersonDTO) src, (PersonProperties) dest);
            }
            if (src instanceof PersonProperties && dest instanceof PersonDTO) {
                mapPersonExtras((PersonProperties) src, (PersonDTO) dest);
            }
//            if (src instanceof StepProgressRequestLogRecordDTO && dest instanceof StepProgressAuditRecord) {
//                mapPersonExtras((StepProgressRequestLogRecordDTO) src, (StepProgressAuditRecord) dest);
//            }
            if (src instanceof StepProgressAuditRecord && dest instanceof StepProgressRequestLogRecordDTO) {
                mapAuditExtras((StepProgressAuditRecord) src, (StepProgressRequestLogRecordDTO) dest);
            }
            return dest;
        } else {
            return null;
        }
    }

    private void mapAuditExtras(StepProgressAuditRecord src, StepProgressRequestLogRecordDTO dest) {
        dest.user = map(src.getUser(), PersonBaseDTO.class);
        dest.person = map(src.getPerson(), PersonBaseDTO.class);
        dest.step = map(src.getStep(), AchievementStepDTO.class);
        try {
            dest.data = new ObjectMapper().readValue(src.getData(), ProgressDTO.class);
        } catch (IOException e) {
            // TODO: Should this exception be accounted for?
        }
    }

    private void mapAchievementStepExtras(AchievementStep src, AchievementStepDTO dest) {
        if (src.getPrerequisiteAchievement() != null) {
            dest.prerequisite_achievement = UuidString.toString(src.getPrerequisiteAchievement().getId());
        }
    }

    private void mapAchievementExtras(Achievement src, AchievementDTO dest) {
        for (var i = 0; i < src.getSteps().size(); i++) {
            var step = src.getSteps().get(i);
            mapAchievementStepExtras(step, dest.steps.get(i));
        }
    }

    private void mapPersonExtras(PersonDTO src, PersonProperties dest) {
        if (src.attributes != null) {
            dest.setAttributes(src.attributes.stream()
                    .map(entry -> map(entry, PersonAttribute.class))
                    .collect(Collectors.toSet()));
        }
        if (Strings.isNullOrEmpty(src.custom_identifier)) {
            dest.setCustomIdentifier(null);
        }
    }

    private void mapPersonExtras(PersonProperties src, PersonDTO dest) {
        if (src.getAttributes() != null) {
            dest.attributes = src.getAttributes().stream()
                    .map(attr -> new PersonAttributeDTO(attr.key, attr.value))
                    .collect(Collectors.toList());
        }
        if (Strings.isNullOrEmpty(src.getCustomIdentifier())) {
            dest.custom_identifier = null;
        }
        if (src instanceof Person) {
            var person = (Person) src;
            dest.groups = person.getMemberships().stream()
                    .map(groupMembership -> new GroupBaseDTO(
                            groupMembership.getGroup().getId(),
                            groupMembership.getGroup().getName()))
                    .collect(Collectors.toList());
        }
    }

    protected OrganizationAchievementSummaryDTO createAchievementSummaryDTO(List<Achievement> achievements, Person personFilter, Organization organization, PeopleDao peopleDao) {
        final Map<UUID, Set<Person>> peopleAwardedByAchievement;
        if (personFilter == null) {
            peopleAwardedByAchievement = achievements.stream()
                    .collect(Collectors.toMap(
                            Achievement::getId,
                            achievement -> Sets.newHashSet(peopleDao.getByAwardedAchievement(organization, achievement))));
        } else {
            final var awarded = personFilter.getAwards().stream()
                    .map(Achievement::getId)
                    .collect(Collectors.toSet());
            peopleAwardedByAchievement = achievements.stream()
                    .collect(Collectors.toMap(
                            Achievement::getId,
                            achievement -> awarded.contains(achievement.getId()) ? Collections.singleton(personFilter) : Collections.emptySet()));
        }
        final var summary = new OrganizationAchievementSummaryDTO();
        for (var achievement : achievements) {
            final var stepCount = achievement.getSteps().size();
            final var progressSumByPerson = achievement.getSteps().stream()
                    .flatMap(achievementStep -> achievementStep.getProgressList().stream())
                    .filter(progress -> progress.getPerson().getOrganization().equals(organization))
                    .filter(progress -> personFilter == null || progress.getPerson().equals(personFilter))
                    .filter(progress -> progress.getValue() > 0)
                    .collect(Collectors.toMap(
                            AchievementStepProgress::getPerson,
                            AchievementStepProgress::getValue,
                            (u, u2) -> u + u2));

            if (stepCount > 0) {
                final var progressSummary = new OrganizationAchievementSummaryDTO.ProgressSummaryDTO();

                progressSummary.people_completed = (int) progressSumByPerson.entrySet().stream()
                        .filter(entry -> entry.getValue() == stepCount * AchievementStepProgress.PROGRESS_COMPLETED)
                        .count();

                progressSummary.people_started = (int) progressSumByPerson.entrySet().stream()
                        .filter(entry -> 0 < entry.getValue() && entry.getValue() < stepCount * AchievementStepProgress.PROGRESS_COMPLETED)
                        .count();

                final var peopleAwardedThisAchievement = peopleAwardedByAchievement.getOrDefault(achievement.getId(), Sets.newHashSet());

                progressSummary.people_awarded = peopleAwardedThisAchievement.size();

                if (progressSummary.people_started + progressSummary.people_completed > 0 || progressSummary.people_awarded > 0) {
                    var progressDetailed = progressSumByPerson.entrySet().stream().map(entry -> {
                        final var personProgress = new OrganizationAchievementSummaryDTO.PersonProgressDTO();
                        personProgress.percent = (int) Math.round(1.0 * entry.getValue() / stepCount);
                        personProgress.person = new PersonBaseDTO(entry.getKey().getId(), entry.getKey().getName());
                        personProgress.awarded = peopleAwardedThisAchievement.stream().anyMatch(person -> person.getId().equals(entry.getKey().getId()));
                        return personProgress;
                    }).collect(Collectors.toList());

                    final var progressDetailedOnlyAwarded = peopleAwardedThisAchievement.stream()
                            .filter(person -> progressDetailed.stream().noneMatch(dto -> person.getId().equals(dto.person.id)))
                            .map(person -> {
                                final var personProgress = new OrganizationAchievementSummaryDTO.PersonProgressDTO();
                                personProgress.percent = 0;
                                personProgress.person = new PersonBaseDTO(person.getId(), person.getName());
                                personProgress.awarded = true;
                                return personProgress;
                            }).collect(Collectors.toList());

                    final var achievementSummary = new OrganizationAchievementSummaryDTO.AchievementSummaryDTO();
                    achievementSummary.achievement = map(achievement, AchievementBaseDTO.class);
                    achievementSummary.progress_summary = progressSummary;
                    achievementSummary.progress_detailed = Lists.newArrayList(Iterables.concat(progressDetailed, progressDetailedOnlyAwarded));
                    summary.achievements.add(achievementSummary);
                }
            }
        }
        return summary;
    }
}
