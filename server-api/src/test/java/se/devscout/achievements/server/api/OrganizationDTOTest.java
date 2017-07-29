package se.devscout.achievements.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.FixtureHelpers;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import se.devscout.achievements.server.api.OrganizationDTO;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationDTOTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        mapper = Jackson.newObjectMapper();
    }

    @Test
    public void serializeExisting() throws Exception {
        final OrganizationDTO org = new OrganizationDTO("alice", "Alice's Organization");

        final String expected = mapper.writeValueAsString(
                mapper.readValue(FixtureHelpers.fixture("fixtures/organization/organization-existing.json"), OrganizationDTO.class));

        Assertions.assertThat(mapper.writeValueAsString(org)).isEqualTo(expected);
    }

    @Test
    public void serializeNew() throws Exception {
        final OrganizationDTO org = new OrganizationDTO(null, "Alice's Organization");

        final String expected = mapper.writeValueAsString(
                mapper.readValue(FixtureHelpers.fixture("fixtures/organization/organization-new.json"), OrganizationDTO.class));

        Assertions.assertThat(mapper.writeValueAsString(org)).isEqualTo(expected);
    }
}