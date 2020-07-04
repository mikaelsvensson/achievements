package se.devscout.achievements.server.data.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import se.devscout.achievements.server.api.PersonDTO;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ScoutnetDataSourceTest {
    private ScoutnetDataSource dataSource;

    @Before
    public void setUp() throws Exception {
        dataSource = new ScoutnetDataSource(new ObjectMapper());
    }

    @Test
    public void read_normalReport_happyPath() throws PeopleDataSourceException, IOException {
        final var people = dataSource.read(getReader(getTestDataStream("scoutnet-members-export-normal.csv")));

        // Assert some of the people in the import file.
        assertPerson(people, "Martina Str\u00f6mberg", "1000006", "Testpatrull");
        assertPerson(people, "Johanna Ekholm", "1000008");
        assertPerson(people, "Camilla Val\u00e9n", "1000007", "Testpatrull");
    }

    @Test
    public void read_simpleReport_happyPath() throws PeopleDataSourceException, IOException {
        final var people = dataSource.read(getReader(getTestDataStream("scoutnet-members-export-simple.csv")));

        // Assert some of the people in the import file.
        assertPerson(people, "Martina Str\u00f6mberg", "1000006");
        assertPerson(people, "Johanna Ekholm", "1000008");
        assertPerson(people, "Camilla Val\u00e9n", "1000007");
    }

    private BufferedReader getReader(InputStream stream) {
        return new BufferedReader(
                new InputStreamReader(
                        stream,
                        Charsets.UTF_8));
    }

    private InputStream getTestDataStream(String resourceName) throws IOException {
        return new BufferedInputStream(
                Resources.getResource(resourceName).openStream());
    }

    private void assertPerson(List<PersonDTO> people, String name, String customId, String... groups) {
        final var matches = people.stream()
                .filter(p -> p.name.equals(name))
                .collect(Collectors.toList());

        assertThat(matches).hasSize(1);

        final var person = matches.get(0);
        assertThat(person.name).isEqualTo(name);
        assertThat(person.custom_identifier).isEqualTo(customId);
        assertThat(person.groups).hasSize(groups.length);
        for (var i = 0; i < groups.length; i++) {
            assertThat(person.groups.get(i).name).isEqualTo(groups[i]);
        }
    }

}