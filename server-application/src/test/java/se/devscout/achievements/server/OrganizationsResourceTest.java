package se.devscout.achievements.server;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.OrganizationAchievementSummaryDTO;
import se.devscout.achievements.server.api.OrganizationBaseDTO;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.auth.PasswordValidator;
import se.devscout.achievements.server.auth.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.OrganizationsResource;
import se.devscout.achievements.server.resources.UuidString;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static se.devscout.achievements.server.MockUtil.*;

public class OrganizationsResourceTest {

    private final OrganizationsDao dao = mock(OrganizationsDao.class);

    private final AchievementsDao achievementsDao = mock(AchievementsDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    private final PeopleDao peopleDao = mock(PeopleDao.class);

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao, peopleDao)
            .addResource(new OrganizationsResource(dao, achievementsDao))
            .build();

    @Before
    public void setUp() throws Exception {
        final Credentials credentials = new Credentials("username", new PasswordValidator(SecretGenerator.PDKDF2, "password".toCharArray()));
        when(credentialsDao.get(eq(IdentityProvider.PASSWORD), eq("user"))).thenReturn(credentials);
    }

    @Test
    public void get_happyPath() throws Exception {
        final UUID uuid = UUID.randomUUID();
        when(dao.read(eq(uuid))).thenReturn(new Organization(uuid, "Alice's Organization"));
        final OrganizationDTO dto = request("/organizations/" + UuidString.toString(uuid)).get(OrganizationDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.name).isEqualTo("Alice's Organization");
    }

    @Test
    public void getBasic_noUser_happyPath() throws Exception {
        final UUID uuid = UUID.randomUUID();
        when(dao.read(eq(uuid))).thenReturn(new Organization(uuid, "Alice's Organization"));
        final OrganizationBaseDTO dto = resources.client().target("/organizations/" + UuidString.toString(uuid) + "/basic").request().get(OrganizationBaseDTO.class);
        assertThat(dto.name).isEqualTo("Alice's Organization");
    }

    @Test
    public void create_happyPath() throws Exception {
        when(dao.create(any(OrganizationProperties.class))).thenAnswer(invocation -> new Organization(UUID.randomUUID(), ((OrganizationProperties) invocation.getArgument(0)).getName()));
        final Response response = request("/organizations").post(Entity.json(new OrganizationDTO(null, "Bob's Club")));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final OrganizationDTO dto = response.readEntity(OrganizationDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.name).isEqualTo("Bob's Club");
    }

    private Invocation.Builder request(String path) {
        return request(path, Collections.EMPTY_MAP);
    }

    private Invocation.Builder request(String path, Map<String, String> queryParameters) {
        WebTarget webTarget = resources.target(path);
        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            webTarget = webTarget.queryParam(entry.getKey(), entry.getValue());
        }
        return webTarget
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)));
    }

    @Test
    public void create_tooManyOrganizations() throws Exception {
        when(dao.create(any(OrganizationProperties.class))).thenThrow(new TooManyOrganizationsException("Too many"));
        final Response response = request("/organizations").post(Entity.json(new OrganizationDTO(null, "Will not be created")));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void find_happyPath() throws Exception {
        when(dao.find(anyString())).thenReturn(Collections.singletonList(mock(Organization.class)));
        final Response response = request("/organizations", Collections.singletonMap("filter", "something")).get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(dao).find(eq("something"));
    }

    @Test
    public void find_invalidFilterString_expectBadRequest() throws Exception {
        when(dao.find(anyString())).thenThrow(new IllegalArgumentException("Bad data"));
        final Response response = request("/organizations", Collections.singletonMap("filter", " ")).get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        verify(dao).find(eq(" "));
    }

    @Test
    public void achievementSummary_twoAchievementTwoSteps_successful() throws Exception {
        final Organization org = mockOrganization("Alice's Organization");
        when(dao.read(eq(org.getId()))).thenReturn(org);
        final Person person1 = mockPerson(org, "Alice");
        final Person person2 = mockPerson(org, "Bob");
        final Person person3 = mockPerson(org, "Carol");
        final AchievementStepProgress a1p1 = mockProgress(true, person1);
        final AchievementStepProgress a1p2 = mockProgress(false, person2);
        final AchievementStepProgress a1p3 = mockProgress(true, person1);
        final AchievementStepProgress a1p4 = mockProgress(true, person2);
        final AchievementStep a1s1 = mockStep(a1p1, a1p2);
        final AchievementStep a1s2 = mockStep(a1p3, a1p4);
        final Achievement a1 = mockAchievement("Climb mountain", a1s1, a1s2);

        final AchievementStepProgress a2p1 = mockProgress(false, person2);
        final AchievementStepProgress a2p2 = mockProgress(true, person3);
        final AchievementStepProgress a2p3 = mockProgress(false, person2);
        final AchievementStepProgress a2p4 = mockProgress(true, person3);
        final AchievementStep s2 = mockStep(a2p1, a2p2);
        final AchievementStep s3 = mockStep(a2p3, a2p4);
        final Achievement a2 = mockAchievement("Cook egg", s2, s3);

        when(achievementsDao.findWithProgressForOrganization(any(Organization.class)))
                .thenReturn(Arrays.asList(a1, a2));

        final OrganizationAchievementSummaryDTO dto = resources.client()
                .target("/organizations/" + UuidString.toString(org.getId()) + "/achievement-summary")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8)))
                .get(OrganizationAchievementSummaryDTO.class);
        assertThat(dto.achievements).hasSize(2);
        assertThat(dto.achievements.get(0).achievement.name).isEqualTo("Climb mountain");
        assertThat(dto.achievements.get(0).progress_summary.people_completed).isEqualTo(1);
        assertThat(dto.achievements.get(0).progress_summary.people_started).isEqualTo(1);

        dto.achievements.get(0).progress_detailed.sort(Comparator.comparing(o -> o.person.name));

        assertThat(dto.achievements.get(0).progress_detailed.get(0).person.name).isEqualTo("Alice");
        assertThat(dto.achievements.get(0).progress_detailed.get(0).percent).isEqualTo(100);
        assertThat(dto.achievements.get(0).progress_detailed.get(1).person.name).isEqualTo("Bob");
        assertThat(dto.achievements.get(0).progress_detailed.get(1).percent).isEqualTo(50);
        assertThat(dto.achievements.get(1).achievement.name).isEqualTo("Cook egg");
        assertThat(dto.achievements.get(1).progress_summary.people_completed).isEqualTo(1);
        assertThat(dto.achievements.get(1).progress_summary.people_started).isEqualTo(0);
        assertThat(dto.achievements.get(1).progress_detailed.get(0).person.name).isEqualTo("Carol");
        assertThat(dto.achievements.get(1).progress_detailed.get(0).percent).isEqualTo(100);
    }

}