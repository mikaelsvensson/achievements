package se.devscout.achievements.server.data.htmlprovider;

import org.jsoup.Jsoup;
import se.devscout.achievements.server.data.dao.CachedHtmlDao;
import se.devscout.achievements.server.data.dao.DaoException;
import se.devscout.achievements.server.data.model.CachedHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;

public class DatabaseCachedHtmlProvider implements HtmlProvider {

    private final CachedHtmlDao cachedHtmlDao;

    public DatabaseCachedHtmlProvider(CachedHtmlDao cachedHtmlDao) {
        this.cachedHtmlDao = cachedHtmlDao;
    }

    @Override
    public String get(URI uri) throws IOException {
        final var cachedResponse = Optional.ofNullable(
                cachedHtmlDao.get(uri)
                        .filter(this::isYoungerThan24Hours)
                        .orElseGet(() -> {
                            // No cached record found OR record found is older than 24 hours.
                            try {
                                final var rawHtml = Jsoup.connect(uri.toString()).execute().body();
                                return cachedHtmlDao.set(URI.create(uri.toString()), rawHtml);
                            } catch (IOException | DaoException e) {
                                // Return null to signal that we failed to fetch data both from cache AND from actual website.
                                return null;
                            }
                        }));
        if (cachedResponse.isPresent()) {
            return cachedResponse.get().getRawHtml();
        } else {
            throw new IOException("Could not get HTML.");
        }
    }

    private boolean isYoungerThan24Hours(CachedHttpResponse chr) {
        return chr.getDateTime().isAfter(OffsetDateTime.now().minusHours(24));
    }
}
