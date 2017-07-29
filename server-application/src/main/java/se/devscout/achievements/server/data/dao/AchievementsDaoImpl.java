package se.devscout.achievements.server.data.dao;

import org.hibernate.SessionFactory;
import org.modelmapper.ModelMapper;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;

import java.util.UUID;

public class AchievementsDaoImpl extends DaoImpl<Achievement> implements AchievementsDao {
    public AchievementsDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Achievement get(String id) throws ObjectNotFoundException {
        return getEntity(UUID.fromString(id));
    }

    @Override
    public Achievement create(AchievementProperties properties) {
        return persist(new ModelMapper().map(properties, Achievement.class));
    }

    @Override
    public Achievement update(String id, AchievementProperties properties) throws ObjectNotFoundException {
        final Achievement achievement = get(id);
        achievement.apply(properties);
        return super.persist(achievement);
    }

    @Override
    public void delete(String id) throws ObjectNotFoundException {
        final Achievement achievement = get(id);
        super.currentSession().delete(achievement);
    }
}
