package se.devscout.achievements.server.data.importer.badges;

import com.google.common.collect.ImmutableMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.api.AchievementStepDTO;
import se.devscout.achievements.server.data.SlugGenerator;
import se.devscout.achievements.server.data.htmlprovider.FileCachedHtmlProvider;
import se.devscout.achievements.server.data.htmlprovider.HtmlProvider;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BadgeImporter {

    public static final String URL = "http://www.scouterna.se/ledascouting/leda-scouting/intressemarken/";

    public static final ImmutableMap<String, List<String>> AUTOMATIC_TAGS = ImmutableMap.<String, List<String>>builder()
            .put("spårarscout", Collections.singletonList("spårare"))
            .put("upptäckarscout", Collections.singletonList("upptäckare"))
            .put("äventyrarscout", Collections.singletonList("äventyrare"))
            .put("utmanarscout", Collections.singletonList("utmanare"))
            .put("roverscout", Collections.singletonList("rover"))
            .put("spårarscout och uppåt", Arrays.asList("spårare", "upptäckare", "äventyrare", "utmanare", "rover"))
            .put("upptäckarscout och uppåt", Arrays.asList("upptäckare", "äventyrare", "utmanare", "rover"))
            .put("äventyrarscout och uppåt", Arrays.asList("äventyrare", "utmanare", "rover"))
            .build();

    public static final Pattern COMMA_OR_PERIOD = Pattern.compile("[,.]");
    private final HtmlProvider htmlProvider;

    public BadgeImporter(HtmlProvider htmlProvider) {
        this.htmlProvider = htmlProvider;
    }

    public static void main(String[] args) {
        try {
            var badgeImporter = new BadgeImporter(new FileCachedHtmlProvider());
            var achievements = badgeImporter.get();
            achievements.forEach(achievementDTO -> {
                System.out.println(achievementDTO.name);
                System.out.println("-".repeat(achievementDTO.name.length()));
                System.out.println("Image: " + achievementDTO.image);
                System.out.println("Tags: " + String.join(", ", achievementDTO.tags));
                System.out.println("Description:\n" + achievementDTO.description);
                achievementDTO.steps.forEach(achievementStepDTO -> {
                    System.out.println("Step: " + achievementStepDTO.description);
                });
            });
            System.out.println(achievements.size());
        } catch (BadgeImporterException e) {
            e.printStackTrace();
//            System.err.println(e.getMessage());
        }
    }

    public List<AchievementDTO> get() throws BadgeImporterException {
        try {
            return Jsoup
                    .parse(htmlProvider.get(URI.create(URL)))
                    .select("article[class*=marke-intresse]")
                    .stream()
                    .map(this::parseArticle)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new BadgeImporterException("Could not process URL", e);
        }
    }

    private AchievementDTO parseArticle(Element article) {
        AchievementDTO dto = new AchievementDTO();

        dto.name = article.select("h2 span").get(0).text();
        final var intermediary = article.text()
                .replace("Rekommenderad åldersgrupp:", "\n## tags\n")
                .replace(" Länktips ", "\n## description\n")
                .replace(" I det ingår att:", "\n## steps\n")
                .replace(" Kriterier ", "\n## steps\n")
                .replace(" Målspår ", "\n## tags\n")
                .replace(" Åldersgrupp ", "\n## tags\n")
                .replace(" Inledning ", "\n## description\n")
                .replace(" Innehåll ", "\n## description\n")
                .replace(": ", ":\n")
                .transform(s -> {
                    final var abbreviations = Arrays.asList(" t.ex.", " ex.", " m.fl.");
                    var step1 = abbreviations.stream().reduce(s, (res, abbr) -> res.replace(abbr, "@@" + abbr.hashCode()));
                    var step2 = step1.replace(". ", ".\n");
                    return abbreviations.stream().reduce(step2, (res, abbr) -> res.replace("@@" + abbr.hashCode(), abbr));
                })
                .replaceAll("^(" + dto.name + "\\s+){2}", "")
                .replaceAll("För att få .* (bör|ska) (du|scouterna)[^\\n]*:", "\n$0\n## steps\n")
                .replace("Köp i shopen", "");

        final var descriptionText = new StringBuilder();
        final var tagsText = new StringBuilder();
        final var stepsText = new StringBuilder();

        StringBuilder currentText = descriptionText;

        for (String line : intermediary.lines().collect(Collectors.toList())) {
            if (line.startsWith("## ")) {
                currentText = switch (line) {
                    case "## description" -> descriptionText;
                    case "## tags" -> tagsText;
                    case "## steps" -> stepsText;
                    default -> throw new IllegalStateException("Unexpected value: " + line);
                };
            } else {
                currentText.append(line).append('\n');
            }
        }

        dto.description = descriptionText.toString();
        dto.steps = stepsText.toString().lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AchievementStepDTO::new)
                .collect(Collectors.toList());

        dto.tags = Stream
                .concat(
                        COMMA_OR_PERIOD.splitAsStream(tagsText.toString()),
                        AUTOMATIC_TAGS.entrySet().stream().flatMap(entry ->
                                dto.description.toLowerCase().contains(entry.getKey())
                                        ? entry.getValue().stream()
                                        : Stream.empty())
                )
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        dto.image = URI.create(article.select("p[class*=marke-image] > img").attr("src"));

        dto.slug = SlugGenerator.toSlug(dto.name);

        return dto;
    }
}
