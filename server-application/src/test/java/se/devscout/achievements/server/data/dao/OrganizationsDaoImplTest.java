package se.devscout.achievements.server.data.dao;

import io.dropwizard.testing.junit.DAOTestRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.model.*;

import javax.persistence.EntityExistsException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class OrganizationsDaoImplTest {
    private static final long MAX_ORG_COUNT = 5L;

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Organization.class)
            .addEntityClass(Person.class)
            .addEntityClass(PersonAttribute.class)
            .addEntityClass(GroupMembership.class)
            .addEntityClass(Group.class)
            .addEntityClass(Credentials.class)
            .addEntityClass(Achievement.class)
            .addEntityClass(AchievementStep.class)
            .addEntityClass(AchievementStepProgress.class)
            .addEntityClass(StepProgressAuditRecord.class)
            .build();

    private OrganizationsDaoImpl dao;

    @Before
    public void setUp() throws Exception {
        dao = new OrganizationsDaoImpl(database.getSessionFactory(), MAX_ORG_COUNT);
    }

    @Test
    public void get_happyPath() throws Exception {
        var aliceUuid = database.inTransaction(() -> dao.create(new OrganizationProperties("Alice's Organization"))).getId();
        final var actual = dao.read(aliceUuid);
        assertThat(actual.getName()).isEqualTo("Alice's Organization");
    }

    @Test
    public void find_happyPath() throws Exception {
        var burnsUuid = database.inTransaction(() -> dao.create(new OrganizationProperties("Burns Industries"))).getId();
        var buynlargeUuid = database.inTransaction(() -> dao.create(new OrganizationProperties("Buy n Large"))).getId();
        var monstersUuid = database.inTransaction(() -> dao.create(new OrganizationProperties("Monsters, Inc."))).getId();
        var planetUuid = database.inTransaction(() -> dao.create(new OrganizationProperties("Planet Express, Inc."))).getId();
        final var actual = dao.find("Bu");
        var returnedUuids = actual.stream().map(Organization::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(burnsUuid, buynlargeUuid);
    }

    @Test
    public void all_happyPath() throws Exception {
        var burnsUuid = database.inTransaction(() -> dao.create(new OrganizationProperties("Burns Industries"))).getId();
        var buynlargeUuid = database.inTransaction(() -> dao.create(new OrganizationProperties("Buy n Large"))).getId();

        //SUT
        final var actual = dao.all();

        var returnedUuids = actual.stream().map(Organization::getId).collect(Collectors.toList());
        assertThat(returnedUuids).containsExactlyInAnyOrder(burnsUuid, buynlargeUuid);
    }

    @Test
    public void find_noCondition_expectException() throws Exception {
        for (var name : new String[]{null, "", "\t", "     ", "\n"}) {
            try {
                dao.find(name);
                fail("Expected IllegalArgumentException when using condition '" + name + "'.");
            } catch (IllegalArgumentException e) {
                // Expected.
            }
        }
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_notFound() throws Exception {
        dao.read(UUID.randomUUID());
    }

    @Test
    public void create_happyPath() throws Exception {
        final var actual = dao.create(new OrganizationProperties("Bob and friends"));
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getName()).isEqualTo("Bob and friends");
    }

    @Test(expected = EntityExistsException.class)
    public void create_duplicate() throws Exception {
        database.inTransaction(() -> dao.create(new OrganizationProperties("New")));
        database.inTransaction(() -> dao.create(new OrganizationProperties("New")));
    }

    @Test(expected = TooManyOrganizationsException.class)
    public void create_tooManyOrganizations() throws Exception {
        for (var i = 0; i < MAX_ORG_COUNT; i++) {
            database.inTransaction(() -> dao.create(new OrganizationProperties(RandomStringUtils.random(10))));
        }
        dao.create(new OrganizationProperties(RandomStringUtils.random(10)));
    }

    @Test
    public void delete_happyPath() throws Exception {
        var id = database.inTransaction(() -> dao.create(new OrganizationProperties("Bob's Partnership"))).getId();
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
        dao.delete(UUID.randomUUID());
    }

    @Test
    public void update_happyPath() throws Exception {
        var objectUuid = database.inTransaction(() -> dao.create(new OrganizationProperties("Bob's Company"))).getId();

        database.inTransaction(() -> dao.update(objectUuid, new OrganizationProperties("Alice's Company")));

        final var actual = database.inTransaction(() -> dao.read(objectUuid));
        assertThat(actual.getId()).isEqualTo(objectUuid);
        assertThat(actual.getName()).isEqualTo("Alice's Company");
    }

}