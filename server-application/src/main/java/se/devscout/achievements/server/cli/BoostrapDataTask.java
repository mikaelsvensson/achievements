package se.devscout.achievements.server.cli;

import com.google.common.collect.ImmutableMultimap;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.OrganizationProperties;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.data.model.PersonProperties;

import java.io.PrintWriter;

public class BoostrapDataTask extends DatabaseTask {
    private final OrganizationsDao organizationsDao;
    private final PeopleDao peopleDao;

    public BoostrapDataTask(SessionFactory sessionFactory, OrganizationsDao organizationsDao, PeopleDao peopleDao) {
        super("bootstrap-data", sessionFactory);
        this.organizationsDao = organizationsDao;
        this.peopleDao = peopleDao;
    }

    @Override
    protected void execute(ImmutableMultimap<String, String> parameters, PrintWriter output, Session session) throws Exception {
        final Organization organization = createOrganization(output, "Monsters, Inc.");
        createPerson(output, organization, "James P. Sullivan");
        createPerson(output, organization, "Mike Wazowski");
        createPerson(output, organization, "Randall Boggs");
        createPerson(output, organization, "Celia Mae");
        createPerson(output, organization, "Roz");
    }

    private Organization createOrganization(PrintWriter output, String name) throws se.devscout.achievements.server.data.dao.DaoException {
        final Organization organization = organizationsDao.create(new OrganizationProperties(name));
        output.format("Created organization %s (id %s)%n", organization.getName(), organization.getId());
        return organization;
    }

    private void createPerson(PrintWriter output, Organization organization, String name) {
        final Person person = peopleDao.create(organization, new PersonProperties(name));
        output.format("Created person %s (id %s)%n", person.getName(), person.getId());
    }
}
