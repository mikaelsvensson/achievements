package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.CachedHttpResponse;

import java.net.URI;
import java.util.Optional;

public interface CachedHtmlDao {
    Optional<CachedHttpResponse> get(URI uri);

    CachedHttpResponse set(URI uri, String body) throws DaoException;
}
