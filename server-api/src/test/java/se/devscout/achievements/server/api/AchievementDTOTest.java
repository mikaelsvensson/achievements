package se.devscout.achievements.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.FixtureHelpers;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AchievementDTOTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        mapper = Jackson.newObjectMapper();
    }

    @Test
    public void serializeExisting() throws Exception {
        final List<AchievementStepDTO> steps = Stream.of(
                "Pour 1 litre of water into pot.",
                "Turn on stove.",
                "Put pot on stove.",
                "Wait for water to start boiling.",
                "Put pasta in water.",
                "Wait the number of minutes suggested on your pasta package.",
                "Turn off stove.",
                "Pour pasta and water over colinder."
        ).map(AchievementStepDTO::new).collect(Collectors.toList());
        steps.add(AchievementStepDTO.withPrerequisite("SOME_ID"));

        final AchievementDTO org = new AchievementDTO(
                "Learn how to cook pasta",
                steps);

        final String expected = mapper.writeValueAsString(
                mapper.readValue(FixtureHelpers.fixture("fixtures/achievements/achievement-new.json"), AchievementDTO.class));

        Assertions.assertThat(mapper.writeValueAsString(org)).isEqualTo(expected);
    }

    @Test
    public void serializeNew() throws Exception {
        final List<AchievementStepDTO> steps = Stream.of(
                "Pour 1 litre of water into pot.",
                "Turn on stove.",
                "Put pot on stove.",
                "Wait for water to start boiling.",
                "Put pasta in water.",
                "Wait the number of minutes suggested on your pasta package.",
                "Turn off stove.",
                "Pour pasta and water over colinder."
        ).map(AchievementStepDTO::new).collect(Collectors.toList());
        steps.add(AchievementStepDTO.withPrerequisite("SOME_ID"));

        final AchievementDTO org = new AchievementDTO(
                "Learn how to cook pasta",
                steps);

        final String expected = mapper.writeValueAsString(
                mapper.readValue(FixtureHelpers.fixture("fixtures/achievements/achievement-new.json"), AchievementDTO.class));

        Assertions.assertThat(mapper.writeValueAsString(org)).isEqualTo(expected);
    }
}