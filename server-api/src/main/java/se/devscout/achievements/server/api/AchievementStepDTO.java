package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.URI;

@JsonTypeInfo(property = "type", include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = "ref", value = AchievementStepDTO.Reference.class),
        @JsonSubTypes.Type(name = "standard", value = AchievementStepDTO.Standard.class)
})
public class AchievementStepDTO {

    public static class Reference extends AchievementStepDTO {
        public URI achievement;

        public Reference() {
        }

        public Reference(@JsonProperty("achievement") URI achievement) {
            this.achievement = achievement;
        }
    }

    public static class Standard extends AchievementStepDTO {
        public String description;

        public Standard() {
        }

        public Standard(@JsonProperty("description") String description) {
            this.description = description;
        }
    }
}
