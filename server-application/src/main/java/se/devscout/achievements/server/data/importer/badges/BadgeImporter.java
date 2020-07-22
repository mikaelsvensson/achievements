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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BadgeImporter {

    public static final List<String> URLS = Arrays.asList(
            "http://www.scouterna.se/ledascouting/leda-scouting/intressemarken/",
            "http://www.scouterna.se/ledascouting/leda-scouting/spararscout/",
            "http://www.scouterna.se/ledascouting/leda-scouting/upptackarscout/",
            "http://www.scouterna.se/ledascouting/leda-scouting/aventyrarscout/",
            "http://www.scouterna.se/ledascouting/leda-scouting/utmanarscout/"
    );

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

    public static final ImmutableMap<String, String> CSS_CLASS_TAGS = ImmutableMap.<String, String>builder()
            .put("type-marke-bevis", "bevismärke")
            .put("type-marke-deltagande", "deltagandemärke")
            .put("type-marke-intresse", "intressemärke")
            .put("type-marke-tillhorighet", "tillhörighetsmärke")
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

    public Collection<AchievementDTO> get() throws BadgeImporterException {
        final var allBadges = new ArrayList<AchievementDTO>();

        for (String url : URLS) {
            try {
                Jsoup.parse(htmlProvider.get(URI.create(url)))
                        .select("article[class*=marke-]")
                        .stream()
                        .map(this::parseArticle)
                        .forEach(allBadges::add);
            } catch (IOException e) {
                throw new BadgeImporterException("Could not process URL " + url, e);
            }
        }
        return allBadges.stream()
                // Remove duplicates, using a Map, by always picking first badge when two have the same slug (simplified name).
                .collect(Collectors.toMap(dto -> dto.slug, dto -> dto, (dto1, dto2) -> dto1))
                .values();
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
                .replace(" Målspår: ", "\n## tags\n")
                .replace(" Åldersgrupp ", "\n## tags\n")
                .replace(" Inledning ", "\n## description\n")
                .replace(" Innehåll ", "\n## description\n")
                .replace(" Innehåll: ", "\n## description\n")
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
                .of(
                        // Stream of tags extracted from "tags part":
                        COMMA_OR_PERIOD.splitAsStream(tagsText.toString()),

                        // String of tags based on certain keywords found in description text:
                        AUTOMATIC_TAGS.entrySet()
                                .stream()
                                .flatMap(entry ->
                                        dto.description.toLowerCase().contains(entry.getKey())
                                                ? entry.getValue().stream()
                                                : Stream.empty()),

                        // Stream with one or zero tags based on the CSS classes of the <article> element containing badge data:
                        CSS_CLASS_TAGS.entrySet()
                                .stream()
                                .filter(entry -> article.classNames().contains(entry.getKey()))
                                .findFirst()
                                .stream()
                                .map(Map.Entry::getValue)
                )
                .flatMap(stream -> stream) // <-- Hack to merge the multiple streams above into one.
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
