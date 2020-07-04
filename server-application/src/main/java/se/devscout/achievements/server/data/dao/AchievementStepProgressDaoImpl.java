package se.devscout.achievements.server.data.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.model.*;

import java.util.List;

public class AchievementStepProgressDaoImpl extends AbstractDAO<AchievementStepProgress> implements AchievementStepProgressDao {
    public AchievementStepProgressDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public List<AchievementStepProgress> get(Achievement achievement) {
        return namedQuery("AchievementStepProgress.byAchievement")
                .setParameter("achievement", achievement)
                .getResultList();
    }

    @Override
    public AchievementStepProgress get(AchievementStep achievementStep, Person person) throws ObjectNotFoundException {
        final var progress = get(new AchievementStepProgress.Key(achievementStep, person));
        if (progress != null) {
            return progress;
        }
        throw new ObjectNotFoundException();
    }

    @Override
    public AchievementStepProgress set(AchievementStep achievementStep, Person person, AchievementStepProgressProperties properties) throws ObjectNotFoundException {
        try {
            final var progress = get(achievementStep, person);
            progress.setNote(properties.getNote());
            progress.setValue(properties.getValue());
            person.getAchievementStepProgress().add(progress);
            return progress;
        } catch (ObjectNotFoundException e) {
            final var progress = new AchievementStepProgress(
                    properties.getValue(),
                    properties.getNote(),
                    achievementStep,
                    person);
            persist(progress);
            person.getAchievementStepProgress().add(progress);
            return progress;
        }
    }

    @Override
    public void unset(AchievementStep achievementStep, Person person) throws ObjectNotFoundException {
        if (achievementStep == null || person == null) {
            throw new IllegalArgumentException("Both achievement step and person must be specified.");
        }
        final var progress = get(achievementStep, person);
        person.getAchievementStepProgress().remove(progress);
        currentSession().delete(progress);
    }
}
