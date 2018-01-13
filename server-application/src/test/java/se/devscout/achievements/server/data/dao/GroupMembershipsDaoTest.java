package se.devscout.achievements.server.data.dao;

import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.model.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupMembershipsDaoTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(Organization.class)
            .addEntityClass(Person.class)
            .addEntityClass(Credentials.class)
            .addEntityClass(GroupMembership.class)
            .addEntityClass(Group.class)
            .build();

    private GroupMembershipsDaoImpl dao;
    private PeopleDaoImpl peopleDao;
    private GroupsDaoImpl groupsDao;

    private Person alice;
    private Person bob;
    private Person carol;

    private Group developers;

    @Before
    public void setUp() throws Exception {
        dao = new GroupMembershipsDaoImpl(database.getSessionFactory());
        OrganizationsDaoImpl organizationDao = new OrganizationsDaoImpl(database.getSessionFactory(), 100L);
        Organization organization = database.inTransaction(() -> organizationDao.create(new OrganizationProperties("Test Organization")));
        peopleDao = new PeopleDaoImpl(database.getSessionFactory());
        groupsDao = new GroupsDaoImpl(database.getSessionFactory());

        alice = database.inTransaction(() -> peopleDao.create(organization, new PersonProperties("Alice", Roles.READER)));
        bob = database.inTransaction(() -> peopleDao.create(organization, new PersonProperties("Bob", Roles.READER)));
        carol = database.inTransaction(() -> peopleDao.create(organization, new PersonProperties("Carol", Roles.READER)));

        developers = database.inTransaction(() -> groupsDao.create(organization, new GroupProperties("Developers")));
    }

    @Test
    public void addRemove_happyPath() {
        List<GroupMembership> memberships1 = database.inTransaction(() -> dao.getMemberships(developers));
        assertThat(memberships1).isEmpty();

        // SUT: Add first group member
        database.inTransaction(() -> dao.add(alice, developers, GroupRole.MEMBER));

        List<GroupMembership> memberships2 = database.inTransaction(() -> dao.getMemberships(developers));
        assertThat(memberships2).hasSize(1);
        assertThat(memberships2.get(0).getPerson().getName()).isEqualTo("Alice");
        assertThat(memberships2.get(0).getRole()).isEqualTo(GroupRole.MEMBER);

        // SUT: Change role of existing membership
        database.inTransaction(() -> dao.add(alice, developers, GroupRole.MANAGER));

        List<GroupMembership> memberships3 = database.inTransaction(() -> dao.getMemberships(developers));
        assertThat(memberships3).hasSize(1);
        assertThat(memberships3.get(0).getPerson().getName()).isEqualTo("Alice");
        assertThat(memberships3.get(0).getRole()).isEqualTo(GroupRole.MANAGER);

        // SUT: Add second member to group
        database.inTransaction(() -> dao.add(bob, developers, GroupRole.MEMBER));

        List<GroupMembership> memberships4 = database.inTransaction(() -> dao.getMemberships(developers));
        assertThat(memberships4).hasSize(2);
        assertThat(memberships4.get(0).getPerson().getName()).isEqualTo("Alice");
        assertThat(memberships4.get(0).getRole()).isEqualTo(GroupRole.MANAGER);
        assertThat(memberships4.get(1).getPerson().getName()).isEqualTo("Bob");
        assertThat(memberships4.get(1).getRole()).isEqualTo(GroupRole.MEMBER);

        // SUT: Remove initial user from group
        database.inTransaction(() -> dao.remove(alice, developers));

        List<GroupMembership> memberships5 = database.inTransaction(() -> dao.getMemberships(developers));
        assertThat(memberships5).hasSize(1);
        assertThat(memberships5.get(0).getPerson().getName()).isEqualTo("Bob");
        assertThat(memberships5.get(0).getRole()).isEqualTo(GroupRole.MEMBER);

        // SUT: Remove user who is actually not in the group
        database.inTransaction(() -> dao.remove(carol, developers));

        List<GroupMembership> memberships6 = database.inTransaction(() -> dao.getMemberships(developers));
        assertThat(memberships6).hasSize(1);
        assertThat(memberships6.get(0).getPerson().getName()).isEqualTo("Bob");
        assertThat(memberships6.get(0).getRole()).isEqualTo(GroupRole.MEMBER);
    }

}