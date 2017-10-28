package se.devscout.achievements.server.cli;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.resources.UuidString;

import java.io.PrintWriter;
import java.util.Base64;
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
        final Organization organization = createOrganization(output, "Monsters, Inc.");
        createPerson(output, organization, "James P. Sullivan");
        createPerson(output, organization, "Mike Wazowski");
        createPerson(output, organization, "Randall Boggs");
        createPerson(output, organization, "Celia Mae");
        createPerson(output, organization, "Roz");

        final Achievement achievementBike = createAchievement(output, "Ride bike", "easy");
        createAchievementStep(output, achievementBike, "Get the bike");
        createAchievementStep(output, achievementBike, "Learn to ride it");
        final Achievement achievementMotorcycle = createAchievement(output, "Ride motorcycle", "easy");
        createAchievementStep(output, achievementMotorcycle, achievementBike);
        createAchievementStep(output, achievementMotorcycle, "Study the traffic laws");
        createAchievementStep(output, achievementMotorcycle, "Pass driving exam");
        createAchievementStep(output, achievementMotorcycle, "Pass written exam");
    }

    private Organization createOrganization(PrintWriter output, String name) throws DaoException {
        final Organization organization = organizationsDao.create(new OrganizationProperties(name));
        output.format("Created organization %s (id %s)%n",
                organization.getName(),
                UuidString.toString(organization.getId()));
        return organization;
    }

    private void createPerson(PrintWriter output, Organization organization, String name) {
        final Person person = peopleDao.create(organization, new PersonProperties(name));
        output.format("Created person %s (id %s)%n", person.getName(), person.getId());
    }

    private AchievementStep createAchievementStep(PrintWriter output, Achievement achievement, String description) {
        final AchievementStep step = achievementStepsDao.create(achievement, new AchievementStepProperties(description));
        output.format("Created regular achievement step (id %s)%n", step.getId());
        return step;
    }

    private AchievementStep createAchievementStep(PrintWriter output, Achievement achievement, Achievement prerequisiteAchievement) {
        final AchievementStep step = achievementStepsDao.create(achievement, new AchievementStepProperties(prerequisiteAchievement));
        output.format("Created prerequisite achievement step (id %s)%n", step.getId());
        return step;
    }

    private Achievement createAchievement(PrintWriter output, String name, String tag) throws DaoException {
        final Achievement achievement = achievementsDao.create(new AchievementProperties(name, Collections.singleton(tag)));
        output.format("Created achievement %s (id %s)%n", achievement.getName(), UuidString.toString(achievement.getId()));
        return achievement;
    }
}
