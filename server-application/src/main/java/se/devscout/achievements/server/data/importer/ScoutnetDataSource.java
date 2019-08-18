package se.devscout.achievements.server.data.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import se.devscout.achievements.server.api.GroupBaseDTO;
import se.devscout.achievements.server.api.PersonAttributeDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.auth.Roles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class ScoutnetDataSource extends CsvDataSource {

    private ObjectMapper objectMapper;

    public ScoutnetDataSource(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected PersonDTO mapColumns(Map<String, String> map) {

        final PersonDTO dto = new PersonDTO();
        dto.name = StringUtils.trim(map.getOrDefault("F\u00f6rnamn", "") + " " + map.getOrDefault("Efternamn", ""));
        dto.custom_identifier = map.get("Medlemsnr.");
        dto.email = map.get("Prim\u00e4r e-postadress");
        dto.role = Roles.READER;

        String group = map.get("Patrull");
        if (StringUtils.isNotEmpty(group)) {
            dto.groups = Collections.singletonList(new GroupBaseDTO(null, group));
        } else {
            dto.groups = new ArrayList<>();
        }

        String department = map.get("Avdelning");
        if (StringUtils.isNotEmpty(department)) {
            dto.attributes = Collections.singletonList(new PersonAttributeDTO("Avdelning", department));
        }

        return dto;
    }
}
