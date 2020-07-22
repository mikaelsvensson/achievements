package se.devscout.achievements.server.data.htmlprovider;

import com.google.common.hash.Hashing;
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
        final var tempFilePath = Paths.get(toFileName(uri));
        if (!tempFilePath.toFile().exists()) {
            final Connection.Response response = Jsoup.connect(uri.toString()).execute();
            Files.writeString(tempFilePath, response.body(), StandardCharsets.UTF_8);
        }
        return Files.readString(tempFilePath, StandardCharsets.UTF_8);
    }

    private static String toFileName(URI uri) {
        return "FileCachedHtmlProvider." + Hashing.md5().hashString(uri.toString(), StandardCharsets.UTF_8).toString() + ".html";
    }
}
