package se.devscout.achievements.server.cli;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.dataimporter.Importer;
import se.devscout.achievements.server.data.dao.AchievementStepsDao;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.model.AchievementProperties;
import se.devscout.achievements.server.data.model.AchievementStepProperties;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import java.io.PrintWriter;
import java.util.stream.Collectors;

public class ImportScoutBadgesTask extends DatabaseTask {

    private final AchievementsDao achievementsDao;
    private final AchievementStepsDao achievementStepsDao;

    public ImportScoutBadgesTask(SessionFactory sessionFactory, AchievementsDao achievementsDao, AchievementStepsDao achievementStepsDao) {
        super("import-badges", sessionFactory);
        this.achievementsDao = achievementsDao;
        this.achievementStepsDao = achievementStepsDao;
    }

    @Override
    protected void execute(ImmutableMultimap<String, String> parameters, PrintWriter output, Session session) throws Exception {
        final var importer = new Importer();
        final var list = importer.get();
        for (var dto : list) {
            final var properties = new AchievementProperties();
            properties.setDescription(dto.description);
            properties.setImage(dto.image);
            properties.setName(dto.name);
            if (dto.tags != null) {
                properties.setTags(Sets.newHashSet(dto.tags));
            }
            final var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(properties);
            if (violations.isEmpty()) {
                final var achievement = achievementsDao.create(properties);
                if (dto.steps != null) {
                    for (var stepDto : dto.steps) {
                        final var stepProperties = new AchievementStepProperties();
                        stepProperties.setDescription(stepDto.description);
                        achievementStepsDao.create(achievement, stepProperties);
                    }
                }
            } else {
                System.out.println("Skipped badge because of " + violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", ")));
            }
        }
    }
}
