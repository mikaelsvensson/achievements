package se.devscout.achievements.server.cli;

import com.google.common.collect.ImmutableMultimap;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.api.ScouternaSeBadgeDTO;
import se.devscout.achievements.server.data.DtoMapper;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.CachedHtmlDao;
import se.devscout.achievements.server.data.htmlprovider.DatabaseCachedHtmlProvider;
import se.devscout.achievements.server.data.importer.badges.BadgeImporter;
import se.devscout.achievements.server.data.importer.badges.BadgeImporterException;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ReadScouternaSeBadgesTask extends DatabaseTask {
    private final BadgeImporter badgeImporter;
    private final DtoMapper dtoMapper;
    private final AchievementsDao achievementsDao;

    public ReadScouternaSeBadgesTask(SessionFactory sessionFactory, AchievementsDao achievementsDao, CachedHtmlDao cachedHtmlDao) {
        super("read-scouterna-se", sessionFactory);
        this.badgeImporter = new BadgeImporter(new DatabaseCachedHtmlProvider(cachedHtmlDao));
        this.achievementsDao = achievementsDao;
        dtoMapper = new DtoMapper();
    }

    @Override
    protected void execute(ImmutableMultimap<String, String> parameters, PrintWriter output, Session session) throws Exception {
        try {
            final var parsedAchievements = badgeImporter.get();
            final var storedAchievements = achievementsDao.readAll().stream().map(o -> dtoMapper.map(o, AchievementDTO.class)).collect(Collectors.toList());

            final var list = parsedAchievements.stream()
                    .map(parsedAchievement -> new ScouternaSeBadgeDTO(
                            parsedAchievement,
                            storedAchievements.stream()
                                    .filter(sa -> sa.slug.equals(parsedAchievement.slug))
                                    .findFirst()
                                    .orElse(null)))
                    .collect(Collectors.toCollection(ArrayList::new));

            list.addAll(storedAchievements.stream()
                    .filter(sa -> list.stream()
                            .noneMatch(pa -> pa.fromScouternaSe.slug.equals(sa.slug)))
                    .map(sa -> new ScouternaSeBadgeDTO(null, sa))
                    .collect(Collectors.toList()));

            output.printf("%d badges%n", list.size());
        } catch (BadgeImporterException e) {
            e.printStackTrace(output);
        }

    }
}
