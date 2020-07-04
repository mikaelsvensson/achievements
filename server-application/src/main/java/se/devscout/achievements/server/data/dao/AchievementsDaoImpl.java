package se.devscout.achievements.server.data.dao;

import com.google.common.base.Strings;
import org.apache.commons.lang3.text.StrTokenizer;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.modelmapper.ModelMapper;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.AchievementProperties;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class AchievementsDaoImpl extends DaoImpl<Achievement, UUID> implements AchievementsDao {
    public AchievementsDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Achievement read(UUID id) throws ObjectNotFoundException {
        return getEntity(id);
    }

    @Override
    public List<Achievement> readAll() {
        return readAll(Achievement.class);
    }

    @Override
    public Achievement create(AchievementProperties properties) {
        return persist(new ModelMapper().map(properties, Achievement.class));
    }

    @Override
    public Achievement update(UUID id, AchievementProperties properties) throws ObjectNotFoundException {
        final var achievement = read(id);
        achievement.apply(properties);
        return super.persist(achievement);
    }

    @Override
    public void delete(UUID id) throws ObjectNotFoundException {
        final var achievement = read(id);
        super.currentSession().delete(achievement);
    }

    @Override
    public List<Achievement> find(String searchQuery) {
        final var tokens = getSearchTokens(searchQuery);

        final var dbQuery = getSearchQuery(tokens);

        return dbQuery.getResultList();
    }

    private String[] getSearchTokens(String searchQuery) {
        searchQuery = Strings.nullToEmpty(searchQuery).trim().toLowerCase();
        if (Strings.isNullOrEmpty(searchQuery)) {
            throw new IllegalArgumentException("Search condition was not specified.");
        }
        return new StrTokenizer(searchQuery, ' ', '"').getTokenArray();
    }

    private Query<Achievement> getSearchQuery(String[] tokens) {
        final List<String> params = new ArrayList<>();
        var whereClause = new StringBuilder();
        for (var token : tokens) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("(");
            whereClause.append("   LOWER(a.name) LIKE LOWER(?)");
            whereClause.append("   OR LOWER(a.description) LIKE LOWER(?)");
            whereClause.append("   OR LOWER(?) MEMBER OF a.tags");
            whereClause.append(")");

            params.add("%" + token + "%");
            params.add("%" + token + "%");
            params.add(token);
        }
        final var dbQuery = query("" +
                "SELECT " +
                "   a " +
                "FROM " +
                "   Achievement a " +
                "WHERE " +
                "   " + whereClause.toString() + " " +
                "ORDER BY " +
                "   a.name");
        for (var i = 0; i < params.size(); i++) {
            var param = params.get(i);
            dbQuery.setParameter(i, param);
        }
        return dbQuery;
    }

    @Override
    public List<Achievement> findWithProgressForOrganization(Organization organization) {
        requireNonNull(organization);

        return namedQuery("Achievement.findWithProgressForOrganization")
                .setParameter("organization", organization)
                .getResultList();
    }

    @Override
    public List<Achievement> findWithProgressForPerson(Person person) {
        requireNonNull(person);

        final List<Achievement> list = namedQuery("Achievement.findWithProgressForPerson")
                .setParameter("person", person)
                .getResultList();

        return list;
    }

    @Override
    public void addAwardedTo(Achievement achievement, Person person) {
        achievement.addAwardFor(person);
        super.persist(achievement);
    }

    @Override
    public void removeAwardedTo(Achievement achievement, Person person) {
        achievement.removeAwardFor(person);
        super.persist(achievement);
    }
}
