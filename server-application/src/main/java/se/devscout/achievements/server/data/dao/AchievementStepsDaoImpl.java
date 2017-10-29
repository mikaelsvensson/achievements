package se.devscout.achievements.server.data.dao;

import org.hibernate.SessionFactory;
import org.modelmapper.ModelMapper;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProperties;

import java.util.List;

public class AchievementStepsDaoImpl extends DaoImpl<AchievementStep, Integer> implements AchievementStepsDao {
    public AchievementStepsDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public AchievementStep read(Integer id) throws ObjectNotFoundException {
        try {
            return getEntity(id);
        } catch (NumberFormatException e) {
            //TODO: Log this. Could be simple client error or hacking attempt.
            throw new ObjectNotFoundException();
        }
    }

    @Override
    public List<AchievementStep> readAll() {
        return readAll(AchievementStep.class);
    }

    @Override
    public AchievementStep create(Achievement parent, AchievementStepProperties properties) {
        final AchievementStep step = new ModelMapper().map(properties, AchievementStep.class);
        step.setAchievement(parent);
        return persist(step);
    }

    @Override
    public AchievementStep update(Integer id, AchievementStepProperties properties) throws ObjectNotFoundException {
        final AchievementStep step = read(id);
        step.apply(properties);
        return super.persist(step);
    }

    @Override
    public void delete(Integer id) throws ObjectNotFoundException {
        final AchievementStep step = read(id);
        super.currentSession().delete(step);
    }

    @Override
    public List<AchievementStep> getByParent(Achievement parent) {
        return namedQuery("AchievementStep.getByAchievement")
                .setParameter("achievement", parent)
                .getResultList();
    }
}
