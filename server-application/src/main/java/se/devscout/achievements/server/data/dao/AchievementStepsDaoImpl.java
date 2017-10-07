package se.devscout.achievements.server.data.dao;

import org.hibernate.SessionFactory;
import org.modelmapper.ModelMapper;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementStep;
import se.devscout.achievements.server.data.model.AchievementStepProperties;

import java.util.List;

public class AchievementStepsDaoImpl extends DaoImpl<AchievementStep> implements AchievementStepsDao {
    public AchievementStepsDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public AchievementStep get(String id) throws ObjectNotFoundException {
        return getEntity(Integer.valueOf(id));
    }

    @Override
    public AchievementStep create(Achievement parent, AchievementStepProperties properties) {
        final AchievementStep step = new ModelMapper().map(properties, AchievementStep.class);
        step.setAchievement(parent);
        return persist(step);
    }

    @Override
    public AchievementStep update(String id, AchievementStepProperties properties) throws ObjectNotFoundException {
        final AchievementStep step = get(id);
        step.apply(properties);
        return super.persist(step);
    }

    @Override
    public void delete(String id) throws ObjectNotFoundException {
        final AchievementStep step = get(id);
        super.currentSession().delete(step);
    }

    @Override
    public List<AchievementStep> getByParent(Achievement parent) {
        return namedQuery("AchievementStep.getByAchievement")
                .setParameter("achievement", parent)
                .getResultList();
    }
}
