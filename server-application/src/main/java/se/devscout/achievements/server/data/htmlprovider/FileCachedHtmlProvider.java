package se.devscout.achievements.server.data.htmlprovider;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileCachedHtmlProvider implements HtmlProvider {

    @Override
    public String get(URI uri) throws IOException {
        final var tempFilePath = Paths.get("temp.html");
        if (!tempFilePath.toFile().exists()) {
            final Connection.Response response = Jsoup.connect(uri.toString()).execute();
            Files.writeString(tempFilePath, response.body(), StandardCharsets.UTF_8);
        }
        return Files.readString(tempFilePath, StandardCharsets.UTF_8);
    }
}
