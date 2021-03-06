package se.devscout.achievements.server.cli;

import com.google.common.collect.ImmutableMultimap;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.auth.Roles;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.UuidString;

import java.io.PrintWriter;
import java.util.Collections;

public class BoostrapDataTask extends DatabaseTask {
    private final OrganizationsDao organizationsDao;
    private final PeopleDao peopleDao;
    private final AchievementsDao achievementsDao;
    private final AchievementStepsDao achievementStepsDao;

    public BoostrapDataTask(SessionFactory sessionFactory, OrganizationsDao organizationsDao, PeopleDao peopleDao, AchievementsDao achievementsDao, AchievementStepsDao achievementStepsDao) {
        super("bootstrap-data", sessionFactory);
        this.organizationsDao = organizationsDao;
        this.peopleDao = peopleDao;
        this.achievementsDao = achievementsDao;
        this.achievementStepsDao = achievementStepsDao;
    }

    @Override
    protected void execute(ImmutableMultimap<String, String> parameters, PrintWriter output, Session session) throws Exception {
        final var organization = createOrganization(output, "Monsters, Inc.");
        createPerson(output, organization, "James P. Sullivan");
        createPerson(output, organization, "Mike Wazowski");
        createPerson(output, organization, "Randall Boggs");
        createPerson(output, organization, "Celia Mae");
        createPerson(output, organization, "Roz");

        final var o = createOrganization(output, "Common Names Ltd.");
        for (var lastName : new String[]{"Smith", "Johnson", "Williams", "Jones", "Brown"}) {
            for (var firstName : new String[]{"Mary", "Patricia", "Linda", "Barbara", "Elizabeth"}) {
                createPerson(output, o, firstName + " " + lastName);
            }
        }

        final var achievementBike = createAchievement(output, "Ride bike", "easy");
        createAchievementStep(output, achievementBike, "Get the bike");
        createAchievementStep(output, achievementBike, "Learn to ride it");
        final var achievementMotorcycle = createAchievement(output, "Ride motorcycle", "easy");
        createAchievementStep(output, achievementMotorcycle, achievementBike);
        createAchievementStep(output, achievementMotorcycle, "Study the traffic laws");
        createAchievementStep(output, achievementMotorcycle, "Pass driving exam");
        createAchievementStep(output, achievementMotorcycle, "Pass written exam");
    }

    private Organization createOrganization(PrintWriter output, String name) throws DaoException {
        final var organization = organizationsDao.create(new OrganizationProperties(name));
        output.format("Created organization %s (id %s)%n",
                organization.getName(),
                UuidString.toString(organization.getId()));
        return organization;
    }

    private void createPerson(PrintWriter output, Organization organization, String name) throws DaoException {
        final var person = peopleDao.create(organization, new PersonProperties(name, Roles.READER));
        output.format("Created person %s (id %s)%n", person.getName(), person.getId());
    }

    private AchievementStep createAchievementStep(PrintWriter output, Achievement achievement, String description) throws DaoException {
        final var step = achievementStepsDao.create(achievement, new AchievementStepProperties(description));
        output.format("Created regular achievement step (id %s)%n", step.getId());
        return step;
    }

    private AchievementStep createAchievementStep(PrintWriter output, Achievement achievement, Achievement prerequisiteAchievement) throws DaoException {
        final var step = achievementStepsDao.create(achievement, new AchievementStepProperties(prerequisiteAchievement));
        output.format("Created prerequisite achievement step (id %s)%n", step.getId());
        return step;
    }

    private Achievement createAchievement(PrintWriter output, String name, String tag) throws DaoException {
        final var achievement = achievementsDao.create(new AchievementProperties(name, Collections.singleton(tag)));
        output.format("Created achievement %s (id %s)%n", achievement.getName(), UuidString.toString(achievement.getId()));
        return achievement;
    }
}
