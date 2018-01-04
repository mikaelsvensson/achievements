package se.devscout.achievements.server.resources;

import com.google.common.base.MoreObjects;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.NameTokenizers;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class AbstractResource {
    final ModelMapper toDtoMapper;
    final ModelMapper fromDtoMapper;

    @Context
    protected UriInfo uriInfo;

    AbstractResource() {
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
                Achievement entity = (Achievement) src;
                AchievementBaseDTO dto = (AchievementBaseDTO) dest;
                dto.id = UuidString.toString(entity.getId());
            }
            if (src instanceof Achievement && dest instanceof AchievementDTO) {
                mapAchievementExtras((Achievement) src, (AchievementDTO) dest);
            }
            if (src instanceof Organization && dest instanceof OrganizationDTO) {
                Organization entity = (Organization) src;
                OrganizationDTO dto = (OrganizationDTO) dest;
                dto.id = UuidString.toString(entity.getId());
            }
            if (src instanceof PersonDTO && dest instanceof PersonProperties) {
                mapPersonExtras((PersonDTO) src, (PersonProperties) dest);
            }
            if (src instanceof PersonProperties && dest instanceof PersonDTO) {
                mapPersonExtras((PersonProperties) src, (PersonDTO) dest);
            }
            return dest;
        } else {
            return null;
        }
    }

    private void mapAchievementStepExtras(AchievementStep src, AchievementStepDTO dest) {
        if (src.getPrerequisiteAchievement() != null) {
            dest.prerequisite_achievement = UuidString.toString(src.getPrerequisiteAchievement().getId());
        }
    }

    private void mapAchievementExtras(Achievement src, AchievementDTO dest) {
        for (int i = 0; i < src.getSteps().size(); i++) {
            AchievementStep step = src.getSteps().get(i);
            mapAchievementStepExtras(step, dest.steps.get(i));
        }
    }

    private void mapPersonExtras(PersonDTO src, PersonProperties dest) {
        if (src.attr != null) {
            dest.setAttributes(src.attr.entrySet().stream()
                    .map(entry -> new PersonAttribute(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet()));
        }
    }

    private void mapPersonExtras(PersonProperties src, PersonDTO dest) {
        if (src.getAttributes() != null) {
            dest.attr = src.getAttributes().stream()
                    .collect(Collectors.toMap(PersonAttribute::getKey, PersonAttribute::getValue));
        }
    }

    protected OrganizationAchievementSummaryDTO createAchievementSummaryDTO(List<Achievement> achievements, Integer personFilter) {
        final OrganizationAchievementSummaryDTO summary = new OrganizationAchievementSummaryDTO();
        for (Achievement achievement : achievements) {
            final int stepCount = achievement.getSteps().size();
            final Map<Person, Long> completedStepsByPerson = achievement.getSteps().stream()
                    .flatMap(achievementStep -> achievementStep.getProgressList().stream())
                    .filter(progress -> personFilter == null || progress.getPerson().getId().equals(personFilter))
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
    }
}
