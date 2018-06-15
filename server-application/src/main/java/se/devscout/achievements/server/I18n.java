package se.devscout.achievements.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class I18n {

    private final JsonNode tree;

    public I18n(String resourcePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        tree = mapper.readTree(Resources.toString(Resources.getResource(resourcePath), Charsets.UTF_8));
    }

    public String get(String path) {
        final String[] fields = StringUtils.split(path, '.');
        JsonNode node = tree;
        for (String field : fields) {
            node = node.path(field);
        }
        if (node != null && !node.isMissingNode() && node.isValueNode()) {
            return node.asText();
        }
        return null;
    }
}
