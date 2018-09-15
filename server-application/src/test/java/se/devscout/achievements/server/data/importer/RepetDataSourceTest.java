package se.devscout.achievements.server.data.importer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.input.BoundedInputStream;
import org.junit.Before;
import org.junit.Test;
import se.devscout.achievements.dataimporter.SlugGenerator;
import se.devscout.achievements.server.api.PersonDTO;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RepetDataSourceTest {

    private final SlugGenerator slugGenerator = new SlugGenerator();
    private RepetDataSource dataSource;

    @Before
    public void setUp() throws Exception {
        dataSource = new RepetDataSource();
    }

    @Test
    public void read_happyPath() throws PeopleDataSourceException, IOException {
        final List<PersonDTO> people = dataSource.read(getReader(getTestDataStream()));

        // Assert some of the people in the import file.
        assertPerson(people, "Backman, Edla", "Sp\u00e5rare");
        assertPerson(people, "Hammarstr\u00f6m Donner, Astrid", "Sp\u00e5rare", "Ton\u00e5r");
        assertPerson(people, "Nyl\u00e9n Alm\u00e9n, Ulla-Britta", "Sp\u00e5rare");
        assertPerson(people, "Lindborg Vikman, Malin", "Uppt\u00e4ckare");
        assertPerson(people, "Appelqvist Almstr\u00f6m, Egil", "Uppt\u00e4ckare");
        assertPerson(people, "S\u00f6derlind, Karoline", "Uppt\u00e4ckare");
        assertPerson(people, "von Albert, Stina", "Ton\u00e5r");
        assertPerson(people, "Gr\u00f6nstedt, Ebba", "Ton\u00e5r");
        assertPerson(people, "Edman, Karl-Axel", "Ton\u00e5r");
    }

    @Test(expected = PeopleDataSourceException.class)
    public void read_truncatedInput() throws IOException, PeopleDataSourceException {
        dataSource.read(getReader(new BoundedInputStream(getTestDataStream(), 1000)));
    }

    @Test(expected = PeopleDataSourceException.class)
    public void read_noInput() throws PeopleDataSourceException {
        dataSource.read(new StringReader(""));
    }

    @Test(expected = PeopleDataSourceException.class)
    public void read_jsonInsteadOfXml() throws PeopleDataSourceException, JsonProcessingException {
        final String json = new ObjectMapper().writeValueAsString(new PersonDTO(1, "Alice", "the-boss"));
        dataSource.read(new StringReader(json));
    }

    private BufferedReader getReader(InputStream stream) {
        return new BufferedReader(
                new InputStreamReader(
                        stream,
                        Charsets.UTF_8));
    }

    private InputStream getTestDataStream() throws IOException {
        // BOMInputStream required since Java normally does not support the BOM first in XML files exported from Repet
        return new BOMInputStream(
                Resources.getResource("batchupsert-repet-narvarolista.xml").openStream());
    }

    private void assertPerson(List<PersonDTO> people, String name, String... groups) {
        final List<PersonDTO> matches = people.stream()
                .filter(p -> p.name.equals(name))
                .collect(Collectors.toList());

        assertThat(matches).hasSize(1);

        final PersonDTO person = matches.get(0);
        assertThat(person.name).isEqualTo(name);
        assertThat(person.custom_identifier).isEqualTo(slugGenerator.toSlug(name));
        assertThat(person.groups).hasSize(groups.length);
        for (int i = 0; i < groups.length; i++) {
            assertThat(person.groups.get(i).name).isEqualTo(groups[i]);
        }
    }
}