package se.devscout.achievements.server.data.htmlprovider;

import java.io.IOException;
import java.net.URI;

public interface HtmlProvider {
    String get(URI uri) throws IOException;
}
