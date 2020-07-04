package se.devscout.achievements.server.data.dao;

import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.model.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class GroupsDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Organization.class)
            .addEntityClass(Person.class)
            .addEntityClass(Credentials.class)
            .addEntityClass(Group.class)
            .addEntityClass(GroupMembership.class)
            .addEntityClass(Achievement.class)
            .addEntityClass(AchievementStep.class)
            .addEntityClass(AchievementStepProgress.class)
            .addEntityClass(StepProgressAuditRecord.class)
            .build();

    private GroupsDaoImpl dao;
    private OrganizationsDaoImpl peopleDao;

    private Organization organization;
    private OrganizationsDaoImpl organizationDao;
    private Organization organization2;

    @Before
    public void setUp() throws Exception {
        dao = new GroupsDaoImpl(database.getSessionFactory());
        organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        organization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization")));
        organization2 = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("The Second Company")));
    }


    @Test
    public void read_happyPath() {
        final var expected = database.inTransaction(() -> dao.create(organization, new GroupProperties("name")));
        final var actual = database.inTransaction(() -> dao.read(expected.getId()));
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getName()).isEqualTo(expected.getName());
    }

    @Test
    public void readAll_happyPath() {
        var organization2 = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Org 2")));
        var group1 = database.inTransaction(() -> dao.create(organization, new GroupProperties("name1")));
        var group2 = database.inTransaction(() -> dao.create(organization, new GroupProperties("name2")));
        var group3 = database.inTransaction(() -> dao.create(organization2, new GroupProperties("name3")));

        final var groups = dao.readAll();

        assertThat(groups.stream().map(Group::getId).collect(Collectors.toList())).containsExactlyInAnyOrder(
                group1.getId(),
                group2.getId(),
                group3.getId());
    }

    @Test
    public void create_happyPath() {
        var group = database.inTransaction(() -> dao.create(organization, new GroupProperties("name")));

        assertThat(group.getId()).isGreaterThan(0);
    }

    @Test
    public void create_sameGroupNameInTwoOrganizations_happyPath() throws DaoException {
        database.inTransaction(() -> dao.create(organization, new GroupProperties("name")));
        dao.create(organization2, new GroupProperties("name"));
    }

    @Test(expected = DuplicateCustomIdentifier.class)
    public void create_sameGroupNameInSameOrganizations_expectConflict() throws DaoException {
        database.inTransaction(() -> dao.create(organization, new GroupProperties("name")));
        dao.create(organization, new GroupProperties("name"));
    }

    @Test
    public void update_happyPath() throws DaoException, ObjectNotFoundException {
        var group = database.inTransaction(() -> dao.create(organization, new GroupProperties("name")));

        dao.update(group.getId(), new GroupProperties("updated name"));

        var actual = database.inTransaction(() -> dao.read(group.getId()));
        assertThat(actual.getName()).isEqualTo("updated name");
    }

    @Test
    public void delete_happyPath() throws ObjectNotFoundException {
        var group1 = database.inTransaction(() -> dao.create(organization, new GroupProperties("name1")));
        var group2 = database.inTransaction(() -> dao.create(organization, new GroupProperties("name2")));

        dao.delete(group2.getId());

        dao.read(group1.getId());
        try {
            dao.read(group2.getId());
            fail();
        } catch (ObjectNotFoundException e) {
            // Expected
        }
    }

    @Test
    public void getByParent_happyPath() {
        var organization2 = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Org 2")));
        var group1 = database.inTransaction(() -> dao.create(organization, new GroupProperties("name1")));
        var group2 = database.inTransaction(() -> dao.create(organization, new GroupProperties("name2")));
        var group3 = database.inTransaction(() -> dao.create(organization2, new GroupProperties("name3")));

        final var groups = dao.getByParent(organization);

        assertThat(groups.stream().map(Group::getId).collect(Collectors.toList())).containsExactlyInAnyOrder(
                group1.getId(),
                group2.getId());
    }
}