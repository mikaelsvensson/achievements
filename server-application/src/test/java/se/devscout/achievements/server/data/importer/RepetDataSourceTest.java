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
        assertPerson(people, "Edla Backman", "backman-edla", "Sp\u00e5rare");
        assertPerson(people, "Astrid Hammarstr\u00f6m Donner", "hammarstrom-donner-astrid", "Sp\u00e5rare", "Ton\u00e5r");
        assertPerson(people, "Ulla-Britta Nyl\u00e9n Alm\u00e9n", "nylen-almen-ulla-britta", "Sp\u00e5rare");
        assertPerson(people, "Malin Lindborg Vikman", "lindborg-vikman-malin", "Uppt\u00e4ckare");
        assertPerson(people, "Egil Appelqvist Almstr\u00f6m", "appelqvist-almstrom-egil", "Uppt\u00e4ckare");
        assertPerson(people, "Karoline S\u00f6derlind", "soderlind-karoline", "Uppt\u00e4ckare");
        assertPerson(people, "Stina von Albert", "von-albert-stina", "Ton\u00e5r");
        assertPerson(people, "Ebba Gr\u00f6nstedt", "gronstedt-ebba", "Ton\u00e5r");
        assertPerson(people, "Karl-Axel Edman", "edman-karl-axel", "Ton\u00e5r");
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

    private void assertPerson(List<PersonDTO> people, String name, String customId, String... groups) {
        final List<PersonDTO> matches = people.stream()
                .filter(p -> p.name.equals(name))
                .collect(Collectors.toList());

        assertThat(matches).hasSize(1);

        final PersonDTO person = matches.get(0);
        assertThat(person.name).isEqualTo(name);
        assertThat(person.custom_identifier).isEqualTo(customId);
        assertThat(person.groups).hasSize(groups.length);
        for (int i = 0; i < groups.length; i++) {
            assertThat(person.groups.get(i).name).isEqualTo(groups[i]);
        }
    }
}