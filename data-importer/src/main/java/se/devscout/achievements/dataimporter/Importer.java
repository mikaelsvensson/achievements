package se.devscout.achievements.dataimporter;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import se.devscout.achievements.server.api.AchievementDTO;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
                dto.description = article.select("p[class=content-preamble]").text();
//                dto.steps = Collections.singletonList(new AchievementStepDTO(article.select("p[class=content-preamble]").text()));
                dto.image = URI.create(article.select("p[class*=marke-image] > img").attr("src"));
                achievements.add(dto);
            }
            return achievements;
        } catch (IOException e) {
            throw new ImporterException("Could not process URL", e);
        }
    }
}
