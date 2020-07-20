package se.devscout.achievements.server.data.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.model.CachedHttpResponse;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;

public class CachedHtmlDaoImpl extends AbstractDAO<CachedHttpResponse> implements CachedHtmlDao {
    public CachedHtmlDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<CachedHttpResponse> get(URI uri) {
        final var res = get(uri.toString());
        if (res != null) {
            return Optional.of(res);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public CachedHttpResponse set(URI uri, String body) throws DaoException {
        final var res = get(uri).orElse(new CachedHttpResponse(uri.toString()));
        res.setRawHtml(body);
        res.setDateTime(OffsetDateTime.now());
        return persist(res);
    }
}
