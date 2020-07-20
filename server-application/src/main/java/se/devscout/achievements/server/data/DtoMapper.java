package se.devscout.achievements.server.data;

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
import se.devscout.achievements.server.resources.UuidString;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DtoMapper {
    final ModelMapper toDtoMapper;
    final ModelMapper fromDtoMapper;

    public DtoMapper() {
        toDtoMapper = new ModelMapper();
        toDtoMapper.getConfiguration().setFieldMatchingEnabled(true);
        toDtoMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.CAMEL_CASE);
        toDtoMapper.getConfiguration().setDestinationNameTokenizer(NameTokenizers.UNDERSCORE);

        fromDtoMapper = new ModelMapper();
        fromDtoMapper.getConfiguration().setFieldMatchingEnabled(true);
        fromDtoMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
        fromDtoMapper.getConfiguration().setDestinationNameTokenizer(NameTokenizers.CAMEL_CASE);
    }

    public <S, T> T map(S src, Class<T> destinationType) {
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
}
