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
import se.devscout.achievements.server.data.DtoMapper;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractResource {
    private final DtoMapper dtoMapper;

    @Context
    protected UriInfo uriInfo;

    protected AbstractResource() {
        dtoMapper = new DtoMapper();
    }

    protected <S, T> T map(S src, Class<T> destinationType) {
        return dtoMapper.map(src, destinationType);
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
                            Integer::sum));

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
