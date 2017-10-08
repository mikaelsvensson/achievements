package se.devscout.achievements.server;

import com.google.common.collect.Lists;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.MyResource;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class MyResourceTest {

    private static final int ZERO = 0;
    private final PeopleDao peopleDao = mock(PeopleDao.class);
    private final OrganizationsDao organizationsDao = mock(OrganizationsDao.class);

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new MyResource(peopleDao, organizationsDao))
            .build();

    @Test
    public void getMyPeople_happyPath() throws Exception {
        final Organization org1 = mockOrganization("Monsters Inc.");
        final Person person1 = mockPerson(org1, "Alice");
        final Organization org2 = mockOrganization("Cyberdyne Systems");
        final Person person2a = mockPerson(org2, "Bob");
        final Person person2b = mockPerson(org2, "Carol");

        when(peopleDao.getByParent(eq(org1))).thenReturn(Lists.newArrayList(person1));
        when(peopleDao.getByParent(eq(org2))).thenReturn(Lists.newArrayList(person2a, person2b));
        when(organizationsDao.all()).thenReturn(Lists.newArrayList(org1, org2));

        final Response response = resources
                .target("/my/people/")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);

        final List<PersonDTO> dto = response.readEntity(new GenericType<List<PersonDTO>>() {
        });
        assertThat(dto).hasSize(3);
        assertThat(dto.get(0).id).isNotNull();
        assertThat(dto.get(0).id).isNotEqualTo(ZERO);
        assertThat(dto.get(1).id).isNotNull();
        assertThat(dto.get(1).id).isNotEqualTo(ZERO);
        assertThat(dto.get(2).id).isNotNull();
        assertThat(dto.get(2).id).isNotEqualTo(ZERO);

        verify(peopleDao).getByParent(eq(org1));
        verify(peopleDao).getByParent(eq(org2));
    }


    private Person mockPerson(Organization org, String name) throws ObjectNotFoundException {
        final Integer uuid = getRandomNonZeroValue();

        final Person person = mock(Person.class);
        when(person.getId()).thenReturn(uuid);
        when(person.getOrganization()).thenReturn(org);
        when(person.getName()).thenReturn(name);

        when(peopleDao.read(eq(uuid))).thenReturn(person);

        return person;
    }

    private int getRandomNonZeroValue() {
        return new Random().nextInt(10000) + 1;
    }

    private Organization mockOrganization(String name) throws ObjectNotFoundException {
        final UUID uuid = UUID.randomUUID();

        final Organization orgA = mock(Organization.class);
        when(orgA.getId()).thenReturn(uuid);
        when(orgA.getName()).thenReturn(name);

        when(organizationsDao.read(eq(uuid))).thenReturn(orgA);

        return orgA;
    }

}