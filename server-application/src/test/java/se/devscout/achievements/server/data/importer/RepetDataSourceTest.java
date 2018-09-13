package se.devscout.achievements.server.data.importer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.io.input.BOMInputStream;
import org.junit.Test;
import se.devscout.achievements.dataimporter.SlugGenerator;
import se.devscout.achievements.server.api.PersonDTO;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RepetDataSourceTest {

    private final SlugGenerator slugGenerator = new SlugGenerator();

    @Test
    public void read() throws ParserConfigurationException, PeopleDataSourceException, IOException {
        final List<PersonDTO> people = new RepetDataSource().read(
                new BufferedReader(
                        new InputStreamReader(
                                // BOMInputStream required since Java normally does not support the BOM first in XML files exported from Repet
                                new BOMInputStream(
                                        Resources.getResource("batchupsert-repet-narvarolista.xml").openStream()
                                ),
                                Charsets.UTF_8)));

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