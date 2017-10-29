package se.devscout.achievements.dataimporter;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import se.devscout.achievements.server.api.AchievementDTO;
import se.devscout.achievements.server.api.AchievementStepDTO;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Importer {
    public Importer() {
    }

    public List<AchievementDTO> get() throws ImporterException {
        try {
            final ArrayList<AchievementDTO> achievements = new ArrayList<>();
            final File cacheFile = new File("temp.html");
            if (!cacheFile.exists()) {
                final Connection.Response response = Jsoup.connect("http://www.scouterna.se/ledascouting/leda-scouting/intressemarken/").execute();
                Files.write(cacheFile.toPath(), response.body().getBytes("UTF-8"));
            }
            final Document doc = Jsoup.parse(cacheFile, "UTF-8");
            final Elements articles = doc.select("article[class*=marke-intresse]");
            for (Element article : articles) {
                final AchievementDTO dto = new AchievementDTO();
                dto.name = article.select("h2 span").get(0).text();
                final Element descriptionElement = article.select("p[class=content-preamble]").first();
                if (descriptionElement != null) {
                    dto.description = descriptionElement.text();
                }

                final Element trackHeaderElement = article.select("h3:containsOwn(M\u00e5lsp\u00e5r)").first();
                if (trackHeaderElement != null) {
                    final String tracks = trackHeaderElement.nextSibling().toString();
                    dto.tags = Stream.of(tracks.split("[,.]")).map(String::trim).map(String::toLowerCase).filter((s) -> !s.isEmpty()).collect(Collectors.toList());
                }

                StringBuilder sb = new StringBuilder();
                final Element contentElement = article.select("h3:containsOwn(Inneh)").first();
                if (contentElement != null) {
                    getMarkdown(sb, contentElement.nextSibling());
                }
                dto.description = sb.toString();
                dto.steps = Collections.singletonList(new AchievementStepDTO("Hela märket"));
                dto.image = URI.create(article.select("p[class*=marke-image] > img").attr("src"));
                achievements.add(dto);
            }
            return achievements;
        } catch (IOException e) {
            throw new ImporterException("Could not process URL", e);
        }
    }

    private void getMarkdown(StringBuilder sb, Node startNode) {
        for (Node node = startNode; node != null && !node.attr("style").contains("clear"); node = node.nextSibling()) {
            if (node instanceof Element) {
                Element element = (Element) node;
                if ("li".equals(element.tagName().toLowerCase())) {
                    final String text = element.text().trim();
                    if (text.length() > 0) {
                        sb.append(" - ").append(text).append('\n');
                    }
                } else {
                    if (element.childNodeSize() > 0) {
                        getMarkdown(sb, element.childNode(0));
                    }
                    sb.append('\n');
                }
            } else if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                final String text = textNode.text().trim();
                if (text.length() > 0) {
                    sb.append(text).append('\n');
                }
            } else {
                throw new IllegalArgumentException("Cannot handle " + node.getClass().getSimpleName());
            }
        }
    }
}
