package se.devscout.achievements.server.data.dao;

import com.google.common.collect.Sets;
import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.model.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PeopleDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Organization.class)
            .addEntityClass(Person.class)
            .addEntityClass(Credentials.class)
            .build();

    private PeopleDaoImpl dao;

    private Organization testOrganization;
    private Organization otherOrganization;

    @Before
    public void setUp() throws Exception {
        dao = new PeopleDaoImpl(database.getSessionFactory());
        OrganizationsDaoImpl organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        testOrganization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization")));
        otherOrganization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Other Organization")));
    }

    @Test
    public void get_happyPath() throws Exception {
        Integer aliceUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", Roles.READER))).getId();
        final Person actual = dao.read(aliceUuid);
        assertThat(actual.getName()).isEqualTo("Alice");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_notFound() throws Exception {
        dao.read(-1);
    }

    @Test
    public void getByOrganization_happyPath() throws Exception {
        Integer aliceUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", Roles.READER))).getId();
        Integer amandaUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Amanda", Roles.READER))).getId();
        Integer bobUuid = database.inTransaction(() -> dao.create(otherOrganization, new PersonProperties("Bob", Roles.READER))).getId();

        final List<Person> actualA = dao.getByParent(testOrganization);
        assertThat(actualA.stream().map(Person::getId).collect(Collectors.toList())).containsExactlyInAnyOrder(aliceUuid, amandaUuid);

        final List<Person> actualB = dao.getByParent(otherOrganization);
        assertThat(actualB.stream().map(Person::getId).collect(Collectors.toList())).containsExactlyInAnyOrder(bobUuid);
    }

    @Test
    public void getByOrganization_incorrectId_expectEmptyList() throws Exception {
        final List<Person> actual = dao.getByParent(null);
        assertThat(actual).isEmpty();
    }

    @Test
    public void getByEmail_addressExists_happyPath() throws Exception {
        database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", "alice@example.com", Collections.emptySet(), "alice", Roles.READER))).getId();

        final List<Person> actual = dao.getByEmail("alice@example.com");
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    public void getByEmail_addressIsMissing_happyPath() throws Exception {
        database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Alice", "alice@example.com", Collections.emptySet(), "alice", Roles.READER))).getId();

        final List<Person> actual = dao.getByEmail("bob@example.com");
        assertThat(actual).hasSize(0);
    }

    @Test
    public void delete_happyPath() throws Exception {
        Integer id = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Bob", Roles.READER))).getId();
        database.inTransaction(() -> {
            try {
                dao.delete(id);
            } catch (ObjectNotFoundException e) {
                fail();
            }
        });
        database.inTransaction(() -> {
            try {
                dao.read(id);
                fail();
            } catch (ObjectNotFoundException e) {
            }
        });
    }

    @Test(expected = ObjectNotFoundException.class)
    public void delete_notFound() throws Exception {
        dao.delete(-1);
    }

    @Test
    public void create_happyPath() throws Exception {
        final Person result = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Carol", "carol@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), null, Roles.READER)));
        final Person actual = database.inTransaction(() -> dao.read(result.getId()));
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getName()).isEqualTo("Carol");
        assertThat(actual.getEmail()).isEqualTo("carol@example.com");

        assertThat(actual.getAttributes()).hasSize(2);
        assertThat(actual.getAttributes()).contains(new PersonAttribute("favourite_colour", "green"));
        assertThat(actual.getAttributes()).contains(new PersonAttribute("role", "administrator"));
    }

    @Test
    public void create_sameNameAsExistingPerson_happyPath() throws Exception {
        final Person c1 = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Carol", "carol1@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), null, Roles.READER)));
        final Person c2 = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Carol", "carol2@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), null, Roles.READER)));
        assertThat(c1.getId()).isNotEqualTo(c2.getId());
    }

    @Test(expected = DuplicateCustomIdentifier.class)
    public void create_duplicateCustomId_notAllowed() throws Exception {
        try {
            dao.create(testOrganization, new PersonProperties("Carol1", "carol1@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), "carol", Roles.READER));
        } catch (Exception e) {
            fail("Exception was not expected");
        }
        dao.create(testOrganization, new PersonProperties("Carol2", "carol2@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "green"), new PersonAttribute("role", "administrator")), "carol", Roles.READER));
    }

    @Test
    public void update_personWithoutAttributes_happyPath() throws Exception {
        Integer objectUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Belinda", Roles.READER))).getId();

        database.inTransaction(() -> dao.update(objectUuid, new PersonProperties("Becky", Roles.READER)));

        final Person actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Becky");
        assertThat(actual.getAttributes()).isEmpty();
    }

    @Test(expected = DuplicateCustomIdentifier.class)
    public void update_duplicateCustomId_notAllowed() throws Exception {
        Integer objectUuid = null;

        try {
            database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Belinda Cooper", "belinda_id", Roles.READER))).getId();
            objectUuid = database.inTransaction(() -> dao.create(testOrganization, new PersonProperties("Belinda Jones", Roles.READER))).getId();
        } catch (Exception e) {
            fail("Exception was not expected");
        }

        dao.update(objectUuid, new PersonProperties("Belinda Jones", "belinda_id", Roles.READER));
    }

    @Test
    public void update_personWithAttributes_happyPath() throws Exception {
        Integer objectUuid = database.inTransaction(() -> {
            final PersonProperties initialProperties = new PersonProperties("Dave", "dave@example.com", Sets.newHashSet(new PersonAttribute("favourite_colour", "orange"), new PersonAttribute("role", "administrator")), null, Roles.READER);
            return dao.create(testOrganization, initialProperties);
        }).getId();

        database.inTransaction(() -> {
            final PersonProperties updatedProperties = new PersonProperties("David", null, Sets.newHashSet(new PersonAttribute("favourite_colour", "blue"), new PersonAttribute("title", "administrator")), "dave", Roles.READER);
            return dao.update(objectUuid, updatedProperties);
        });

        final Person actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("David");
        assertThat(actual.getEmail()).isNull();
        assertThat(actual.getCustomIdentifier()).isEqualTo("dave");
        assertThat(actual.getAttributes()).hasSize(2);
        assertThat(actual.getAttributes()).contains(new PersonAttribute("favourite_colour", "blue"));
        assertThat(actual.getAttributes()).contains(new PersonAttribute("title", "administrator"));
    }
}