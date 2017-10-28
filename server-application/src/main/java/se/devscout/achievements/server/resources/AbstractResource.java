package se.devscout.achievements.server.resources;

import com.google.common.base.MoreObjects;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.NameTokenizers;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.api.AchievementStepDTO;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.Organization;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

abstract class AbstractResource {
    final ModelMapper toDtoMapper;
    final ModelMapper fromDtoMapper;

    @Context
    protected UriInfo uriInfo;

    AbstractResource() {
        toDtoMapper = new ModelMapper();
        toDtoMapper.getConfiguration().setFieldMatchingEnabled(true);
        toDtoMapper.getConfiguration().setDestinationNameTokenizer(NameTokenizers.CAMEL_CASE);

        fromDtoMapper = new ModelMapper();
        fromDtoMapper.getConfiguration().setFieldMatchingEnabled(true);
        fromDtoMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.CAMEL_CASE);
    }

    protected <S, T> T map(S src, Class<T> destinationType) {
        if (src != null) {
            final T dest = MoreObjects.firstNonNull(
                    toDtoMapper.map(src, destinationType),
                    fromDtoMapper.map(src, destinationType));
            if (src instanceof AchievementStep && dest instanceof AchievementStepDTO) {
                AchievementStep entity = (AchievementStep) src;
                AchievementStepDTO dto = (AchievementStepDTO) dest;
                if (entity.getPrerequisiteAchievement() != null) {
                    dto.prerequisite_achievement = UuidString.toString(entity.getPrerequisiteAchievement().getId());
                }
            }
            if (src instanceof Achievement && dest instanceof AchievementDTO) {
                Achievement entity = (Achievement) src;
                AchievementDTO dto = (AchievementDTO) dest;
                dto.id = UuidString.toString(entity.getId());
                for (int i = 0; i < entity.getSteps().size(); i++) {
                    AchievementStep step = entity.getSteps().get(i);
                    if (step.getPrerequisiteAchievement() != null) {
                        dto.steps.get(i).prerequisite_achievement = UuidString.toString(step.getPrerequisiteAchievement().getId());
                    }
                }
            }
            if (src instanceof Organization && dest instanceof OrganizationDTO) {
                Organization entity = (Organization) src;
                OrganizationDTO dto = (OrganizationDTO) dest;
                dto.id = UuidString.toString(entity.getId());
            }
            return dest;
        } else {
            return null;
        }
    }
}
