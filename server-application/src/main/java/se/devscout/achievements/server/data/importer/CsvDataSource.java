package se.devscout.achievements.server.data.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import se.devscout.achievements.server.api.GroupBaseDTO;
import se.devscout.achievements.server.api.PersonAttributeDTO;
import se.devscout.achievements.server.api.PersonDTO;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvDataSource implements PeopleDataSource {

    private ObjectMapper objectMapper;

    public CsvDataSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PersonDTO> read(Reader reader) throws PeopleDataSourceException {
        try {
            CsvSchema schema = CsvSchema.emptySchema().withHeader(); // use first row as header; otherwise defaults are fine
            final List<Map<String, String>> values = Lists.newArrayList(
                    new CsvMapper().readerFor(Map.class)
                            .with(schema)
                            .readValues(reader));
            return values.stream()
                    .map(this::mapColumns)
                    .filter(p -> !Strings.isNullOrEmpty(p.name))
                    .collect(Collectors.toList());
        } catch (IOException | RuntimeException e) {
            throw new PeopleDataSourceException("Could not read data", e);
        }
    }

    protected PersonDTO mapColumns(Map<String, String> map) {
        final String rawGroups = map.remove("groups");
        final PersonDTO dto = objectMapper.convertValue(map, PersonDTO.class);
        dto.attributes = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().startsWith("attr.")) {
                dto.attributes.add(new PersonAttributeDTO(entry.getKey().substring("attr.".length()), entry.getValue()));
            }
        }
        if (rawGroups != null) {
            dto.groups = Stream.of(StringUtils.split(rawGroups, ',')).map(grp -> new GroupBaseDTO(null, grp.trim())).collect(Collectors.toList());
        }
        return dto;
    }
}
