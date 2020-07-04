package se.devscout.achievements.server.resources;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.MockUtil;
import se.devscout.achievements.server.TestUtil;
import se.devscout.achievements.server.api.OrganizationAchievementSummaryDTO;
import se.devscout.achievements.server.api.OrganizationBaseDTO;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static se.devscout.achievements.server.MockUtil.*;

public class OrganizationsResourceTest {

    private final OrganizationsDao dao = mock(OrganizationsDao.class);

    private final AchievementsDao achievementsDao = mock(AchievementsDao.class);

    private final CredentialsDao credentialsDao = mock(CredentialsDao.class);

    private final PeopleDao peopleDao = mock(PeopleDao.class);

//    private final AuthResourceUtil authResourceUtil = new AuthResourceUtil(mock(JwtAuthenticator.class), credentialsDao, peopleDao, dao, new CredentialsValidatorFactory("google_client_id"));

    @Rule
    public final ResourceTestRule resources = TestUtil.resourceTestRule(credentialsDao)
            .addResource(new OrganizationsResource(dao, achievementsDao,/*, authResourceUtil*/peopleDao))
            .build();

    @Before
    public void setUp() throws Exception {
        MockUtil.setupDefaultCredentials(credentialsDao);
    }

    @Test
    public void get_happyPath() throws Exception {
        final var uuid = UUID.randomUUID();
        when(dao.read(eq(uuid))).thenReturn(new Organization(uuid, "Alice's Organization"));
        final var dto = request("/organizations/" + UuidString.toString(uuid), MockUtil.AUTH_FEATURE_READER).get(OrganizationDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.name).isEqualTo("Alice's Organization");
    }

    @Test
    public void getBasic_noUser_happyPath() throws Exception {
        final var uuid = UUID.randomUUID();
        when(dao.read(eq(uuid))).thenReturn(new Organization(uuid, "Alice's Organization"));
        final var dto = resources.client().target("/organizations/" + UuidString.toString(uuid) + "/basic").request().get(OrganizationBaseDTO.class);
        assertThat(dto.name).isEqualTo("Alice's Organization");
    }

    @Test
    public void create_happyPath() throws Exception {
        when(dao.create(any(OrganizationProperties.class))).thenAnswer(invocation -> new Organization(UUID.randomUUID(), ((OrganizationProperties) invocation.getArgument(0)).getName()));
        final var response = request("/organizations", MockUtil.AUTH_FEATURE_EDITOR).post(Entity.json(new OrganizationDTO(null, "Bob's Club")));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
        final var dto = response.readEntity(OrganizationDTO.class);
        assertThat(dto.id).isNotNull();
        assertThat(dto.name).isEqualTo("Bob's Club");
    }

    private Invocation.Builder request(String path, HttpAuthenticationFeature authFeature) {
        return request(path, Collections.EMPTY_MAP, authFeature);
    }

    private Invocation.Builder request(String path, Map<String, String> queryParameters, HttpAuthenticationFeature authFeature) {
        var webTarget = resources.target(path);
        for (var entry : queryParameters.entrySet()) {
            webTarget = webTarget.queryParam(entry.getKey(), entry.getValue());
        }
        return webTarget
                .register(authFeature)
                .request();
    }

    @Test
    public void create_tooManyOrganizations() throws Exception {
        when(dao.create(any(OrganizationProperties.class))).thenThrow(new TooManyOrganizationsException("Too many"));
        final var response = request("/organizations", MockUtil.AUTH_FEATURE_EDITOR).post(Entity.json(new OrganizationDTO(null, "Will not be created")));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void find_happyPath() throws Exception {
        when(dao.find(anyString())).thenReturn(Collections.singletonList(mock(Organization.class)));
        final var response = request("/organizations", Collections.singletonMap("filter", "something"), MockUtil.AUTH_FEATURE_READER).get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        verify(dao).find(eq("something"));
    }

    @Test
    public void find_invalidFilterString_expectBadRequest() throws Exception {
        when(dao.find(anyString())).thenThrow(new IllegalArgumentException("Bad data"));
        final var response = request("/organizations", Collections.singletonMap("filter", " "), MockUtil.AUTH_FEATURE_READER).get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        verify(dao).find(eq(" "));
    }

    @Test
    public void achievementSummary_twoAchievementTwoSteps_successful() throws Exception {
        final var orgOther = mockOrganization("Trudy's Organization");
        final var personOther = mockPerson(orgOther, "Trudy", Roles.READER);

        final var org = mockOrganization("Alice's Organization");
        when(dao.read(eq(org.getId()))).thenReturn(org);
        final var person1 = mockPerson(org, "Alice", Roles.READER);
        final var person2 = mockPerson(org, "Bob", Roles.READER);
        final var person3 = mockPerson(org, "Carol", Roles.READER);
        final var a1p1 = mockProgress(true, person1);
        final var a1p2 = mockProgress(false, person2);
        final var a1p3 = mockProgress(true, person1);
        final var a1p4 = mockProgress(true, person2);
        final var a1p5 = mockProgress(true, personOther);
        final var a1s1 = mockStep(a1p1, a1p2);
        final var a1s2 = mockStep(a1p3, a1p4, a1p5);
        final var a1 = mockAchievement("Climb mountain", a1s1, a1s2);

        final var a2p1 = mockProgress(false, person2);
        final var a2p2 = mockProgress(true, person3);
        final var a2p3 = mockProgress(false, person2);
        final var a2p4 = mockProgress(50, person3);
        final var s2 = mockStep(a2p1, a2p2);
        final var s3 = mockStep(a2p3, a2p4);
        final var a2 = mockAchievement("Cook egg", s2, s3);

        final var a3s1 = mockStep();
        final var a3 = mockAchievement("Peel a banana", a3s1);

        when(achievementsDao.findWithProgressForOrganization(any(Organization.class)))
                .thenReturn(Arrays.asList(a1, a2, a3));

        when(peopleDao.getByAwardedAchievement(eq(org), eq(a1))).thenReturn(Collections.emptyList());
        when(peopleDao.getByAwardedAchievement(eq(org), eq(a2))).thenReturn(Collections.singletonList(person3));
        when(peopleDao.getByAwardedAchievement(eq(org), eq(a3))).thenReturn(Collections.singletonList(person3));

        final var dto = resources.client()
                .target("/organizations/" + UuidString.toString(org.getId()) + "/achievement-summary")
                .register(MockUtil.AUTH_FEATURE_EDITOR)
                .request()
                .get(OrganizationAchievementSummaryDTO.class);
        assertThat(dto.achievements).hasSize(3);

        dto.achievements.get(0).progress_detailed.sort(Comparator.comparing(o -> o.person.name));
        assertThat(dto.achievements.get(0).achievement.name).isEqualTo("Climb mountain");
        assertThat(dto.achievements.get(0).progress_summary.people_completed).isEqualTo(1);
        assertThat(dto.achievements.get(0).progress_summary.people_started).isEqualTo(1);
        assertThat(dto.achievements.get(0).progress_summary.people_awarded).isEqualTo(0);
        assertThat(dto.achievements.get(0).progress_detailed.get(0).person.name).isEqualTo("Alice");
        assertThat(dto.achievements.get(0).progress_detailed.get(0).percent).isEqualTo(100);
        assertThat(dto.achievements.get(0).progress_detailed.get(0).awarded).isFalse();
        assertThat(dto.achievements.get(0).progress_detailed.get(1).person.name).isEqualTo("Bob");
        assertThat(dto.achievements.get(0).progress_detailed.get(1).percent).isEqualTo(50);
        assertThat(dto.achievements.get(0).progress_detailed.get(1).awarded).isFalse();

        assertThat(dto.achievements.get(1).achievement.name).isEqualTo("Cook egg");
        assertThat(dto.achievements.get(1).progress_summary.people_completed).isEqualTo(0);
        assertThat(dto.achievements.get(1).progress_summary.people_started).isEqualTo(1);
        assertThat(dto.achievements.get(1).progress_summary.people_awarded).isEqualTo(1);
        assertThat(dto.achievements.get(1).progress_detailed.get(0).person.name).isEqualTo("Carol");
        assertThat(dto.achievements.get(1).progress_detailed.get(0).percent).isEqualTo(75);
        assertThat(dto.achievements.get(1).progress_detailed.get(0).awarded).isTrue();

        assertThat(dto.achievements.get(2).achievement.name).isEqualTo("Peel a banana");
        assertThat(dto.achievements.get(2).progress_summary.people_completed).isEqualTo(0);
        assertThat(dto.achievements.get(2).progress_summary.people_started).isEqualTo(0);
        assertThat(dto.achievements.get(2).progress_summary.people_awarded).isEqualTo(1);
        assertThat(dto.achievements.get(2).progress_detailed.get(0).person.name).isEqualTo("Carol");
        assertThat(dto.achievements.get(2).progress_detailed.get(0).percent).isEqualTo(0);
        assertThat(dto.achievements.get(2).progress_detailed.get(0).awarded).isTrue();
    }

}