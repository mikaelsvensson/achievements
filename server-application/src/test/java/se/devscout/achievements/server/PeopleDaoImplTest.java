package se.devscout.achievements.server;

import com.google.common.collect.Sets;
import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.OrganizationsDaoImpl;
import se.devscout.achievements.server.data.dao.PeopleDaoImpl;
import se.devscout.achievements.server.data.model.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PeopleDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Organization.class)
            .addEntityClass(Person.class)
            .build();

    private PeopleDaoImpl dao;

    private Organization testOrganization;

    @Before
    public void setUp() throws Exception {
        dao = new PeopleDaoImpl(database.getSessionFactory());
        OrganizationsDaoImpl organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        testOrganization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization")));
    }

    @Test
    public void get_happyPath() throws Exception {
        UUID aliceUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice"))).getId();
        final Person actual = dao.get(aliceUuid.toString());
        assertThat(actual.getName()).isEqualTo("Alice");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_notFound() throws Exception {
        dao.get(UUID.randomUUID().toString());
    }

    @Test
    public void delete_happyPath() throws Exception {
        UUID id = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Bob"))).getId();
        database.inTransaction(() -> {
            try {
                dao.delete(id.toString());
            } catch (ObjectNotFoundException e) {
                fail();
            }
        });
        database.inTransaction(() -> {
            try {
                dao.get(id.toString());
                fail();
            } catch (ObjectNotFoundException e) {
            }
        });
    }

    @Test(expected = ObjectNotFoundException.class)
    public void delete_notFound() throws Exception {
        dao.delete(UUID.randomUUID().toString());
    }

    @Test
    public void create_happyPath() throws Exception {
        final Person result = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Carol", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")))));
        final Person actual = database.inTransaction(() -> dao.get(result.getId().toString()));
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getName()).isEqualTo("Carol");

        assertThat(actual.getAttributes()).hasSize(2);
        assertThat(actual.getAttributes().contains(new PersonAttribute("favourite_colour", "green")));
        assertThat(actual.getAttributes().contains(new PersonAttribute("role", "administrator")));
    }

    @Test
    public void update_personWithoutAttributes_happyPath() throws Exception {
        UUID objectUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Belinda"))).getId();

        database.inTransaction(() -> dao.update(objectUuid.toString(), new PersonProperties("Becky")));

        final Person actual = database.inTransaction(() -> dao.get(objectUuid.toString()));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Becky");
        assertThat(actual.getAttributes()).isEmpty();
    }

    @Test
    public void update_personWithAttributes_happyPath() throws Exception {
        UUID objectUuid = database.inTransaction(() -> {
            final PersonProperties initialProperties = new PersonProperties("Dave", Sets.newHashSet(new PersonAttribute("favourite_colour", "orange"), new PersonAttribute("role", "administrator")));
            return dao.create(testOrganization, initialProperties);
        }).getId();

        database.inTransaction(() -> {
            final PersonProperties updatedProperties = new PersonProperties("David", Sets.newHashSet(new PersonAttribute("favourite_colour", "blue"), new PersonAttribute("title", "administrator")));
            return dao.update(objectUuid.toString(), updatedProperties);
        });

        final Person actual = database.inTransaction(() -> dao.get(objectUuid.toString()));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("David");
        assertThat(actual.getAttributes()).hasSize(2);
        assertThat(actual.getAttributes().contains(new PersonAttribute("favourite_colour", "blue")));
        assertThat(actual.getAttributes().contains(new PersonAttribute("title", "administrator")));
    }
}