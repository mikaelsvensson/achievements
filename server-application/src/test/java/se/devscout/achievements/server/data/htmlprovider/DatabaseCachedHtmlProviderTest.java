package se.devscout.achievements.server.data.htmlprovider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import se.devscout.achievements.server.data.dao.CachedHtmlDao;
import se.devscout.achievements.server.data.dao.DaoException;
import se.devscout.achievements.server.data.model.CachedHttpResponse;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseCachedHtmlProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    private final CachedHtmlDao dao = mock(CachedHtmlDao.class);

    @Test
    public void get_cachedDataIsUpToDate() throws IOException {
        // arrange
        final var uri = URI.create("http://example.com/cachedDataIsUpToDate.html");
        when(dao.get(eq(uri))).thenReturn(Optional.of(new CachedHttpResponse(
                "http://example.com/cachedDataIsUpToDate.html",
                "<html></html>",
                OffsetDateTime.now().minusHours(23))));

        // act
        final var actual = new DatabaseCachedHtmlProvider(dao).get(uri);

        // assert
        assertThat(actual).isEqualTo("<html></html>");
    }

    @Test
    public void get_cachedDataIsStale() throws IOException, DaoException {
        // arrange
        final var uri = UriBuilder.fromUri("http://localhost/cachedDataIsStale.html").port(wireMockRule.port()).build();
        when(dao.get(eq(uri))).thenReturn(Optional.of(new CachedHttpResponse(
                uri.toString(),
                "<html>Old</html>",
                OffsetDateTime.now().minusHours(25))));
        when(dao.set(eq(uri), eq("<html>New</html>"))).thenReturn(new CachedHttpResponse(
                uri.toString(),
                "<html>New</html>",
                OffsetDateTime.now()));
        stubFor(get(urlEqualTo("/cachedDataIsStale.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<html>New</html>")));

        // act
        final var actual = new DatabaseCachedHtmlProvider(dao).get(uri);

        // assert
        assertThat(actual).isEqualTo("<html>New</html>");
    }

    @Test(expected = IOException.class)
    public void get_badHttpResponse() throws IOException, DaoException {
        // arrange
        final var uri = UriBuilder.fromUri("http://localhost/badHttpResponse.html").port(wireMockRule.port()).build();
        when(dao.get(eq(uri))).thenReturn(Optional.of(new CachedHttpResponse(
                uri.toString(),
                "<html>Old</html>",
                OffsetDateTime.now().minusHours(25))));
        stubFor(get(urlEqualTo("/badHttpResponse.html"))
                .willReturn(aResponse().withStatus(404)));

        // act
        new DatabaseCachedHtmlProvider(dao).get(uri);

        // assert
        // Expect failure
    }

    @Test(expected = IOException.class)
    public void get_couldNotSave() throws IOException, DaoException {
        // arrange
        final var uri = UriBuilder.fromUri("http://localhost/badHttpResponse.html").port(wireMockRule.port()).build();
        when(dao.get(eq(uri))).thenReturn(Optional.of(new CachedHttpResponse(
                uri.toString(),
                "<html>Old</html>",
                OffsetDateTime.now().minusHours(25))));
        stubFor(get(urlEqualTo("/couldNotSave.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<html>New</html>")));
        when(dao.set(eq(uri), eq("<html>New</html>"))).thenThrow(new DaoException("Boom"));

        // act
        new DatabaseCachedHtmlProvider(dao).get(uri);

        // assert
        // Expect failure
    }

}