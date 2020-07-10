package se.devscout.achievements.server.cli;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.CredentialsDao;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.CredentialsProperties;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.OrganizationProperties;
import se.devscout.achievements.server.data.model.PersonProperties;

import java.io.PrintWriter;
import java.util.List;

public class AddAdminUserTask extends DatabaseTask {
    private final OrganizationsDao organizationsDao;
    private final PeopleDao peopleDao;
    private final CredentialsDao credentialsDao;

    public AddAdminUserTask(SessionFactory sessionFactory, OrganizationsDao organizationsDao, PeopleDao peopleDao, CredentialsDao credentialsDao) {
        super("add-admin", sessionFactory);
        this.organizationsDao = organizationsDao;
        this.peopleDao = peopleDao;
        this.credentialsDao = credentialsDao;
    }

    @Override
    protected void execute(ImmutableMultimap<String, String> parameters, PrintWriter output, Session session) throws Exception {
        final var orgName = parameters.get("org").stream().findFirst().orElse("");
        final var userName = parameters.get("user").stream().findFirst().orElse("");
        if (Strings.isNullOrEmpty(orgName)) {
            throw new Exception("Organization name has not been specified using query parameter 'org'.");
        }
        if (Strings.isNullOrEmpty(userName)) {
            throw new Exception("Username has not been specified using query parameter 'user'.");
        }
        if (!EmailValidator.getInstance().isValid(userName)) {
            throw new Exception("Username should be an e-mail address.");
        }
        List<Organization> organizations = organizationsDao.find(orgName);
        final var organization = organizations.isEmpty()
                ? organizationsDao.create(new OrganizationProperties(orgName))
                : organizations.get(0);

        final var person = peopleDao.create(organization, new PersonProperties(
                userName,
                Roles.ADMIN));

        final var password = RandomStringUtils.randomAlphanumeric(20);
        final var passwordValidator = new PasswordValidator(SecretGenerator.PDKDF2, password.toCharArray());

        credentialsDao.create(person, new CredentialsProperties(
                userName,
                passwordValidator.getCredentialsType(),
                passwordValidator.getCredentialsData()));

        output.printf("Username: %s%nPassword: %s%n", userName, password);
    }
}
