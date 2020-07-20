package se.devscout.achievements.server.data.dao;

import io.dropwizard.testing.junit.DAOTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.model.CachedHttpResponse;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class CachedHtmlDaoImplTest {

    @Rule
    public DAOTestRule database = DAOTestRule.newBuilder()
            .setShowSql(true)
            .addEntityClass(CachedHttpResponse.class)
            .build();

    private CachedHtmlDaoImpl dao;

    @Before
    public void setUp() throws Exception {
        dao = new CachedHtmlDaoImpl(database.getSessionFactory());
    }

    @Test
    public void setAndGet_happyPath() throws DaoException {
        final var uri = URI.create("http://example.com/test");

        final var get1 = database.inTransaction(() -> dao.get(uri));
        assertThat(get1.isPresent()).isFalse();

        final var set1 = database.inTransaction(() -> dao.set(uri, "<html></html>"));
        assertThat(set1.getRawHtml()).isEqualTo("<html></html>");
        assertThat(set1.getResourceUri()).isEqualTo(uri.toString());
        assertThat(set1.getDateTime()).isNotNull();

        final var get2 = database.inTransaction(() -> dao.get(uri));
        assertThat(get2.isPresent()).isTrue();
        assertThat(get2.get().getRawHtml()).isEqualTo("<html></html>");
        assertThat(get2.get().getResourceUri()).isEqualTo(uri.toString());
        assertThat(get2.get().getDateTime()).isNotNull();
    }
}