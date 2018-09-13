package se.devscout.achievements.server.data.importer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.io.input.BOMInputStream;
import org.junit.Test;
import se.devscout.achievements.server.api.PersonDTO;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RepetDataSourceTest {

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

        // TODO: Encode non-ASCII characters in source code?

        // Assert some of the people in the import file.
        assertPerson(people, "Backman, Edla", "Spårare");
        assertPerson(people, "Hammarström Donner, Astrid", "Spårare", "Tonår");
        assertPerson(people, "Nylén Almén, Ulla-Britta", "Spårare");
        assertPerson(people, "Lindborg Vikman, Malin", "Upptäckare");
        assertPerson(people, "Appelqvist Almström, Egil", "Upptäckare");
        assertPerson(people, "Söderlind, Karoline", "Upptäckare");
        assertPerson(people, "von Albert, Stina", "Tonår");
        assertPerson(people, "Grönstedt, Ebba", "Tonår");
        assertPerson(people, "Edman, Karl-Axel", "Tonår");
    }

    private void assertPerson(List<PersonDTO> people, String name, String... groups) {
        final List<PersonDTO> matches = people.stream().filter(p -> p.name.equals(name)).collect(Collectors.toList());
        assertThat(matches).hasSize(1);
        final PersonDTO person = matches.get(0);
        assertThat(person.name).isEqualTo(name);
        assertThat(person.custom_identifier).isEqualTo(name);
        assertThat(person.groups).hasSize(groups.length);
        for (int i = 0; i < groups.length; i++) {
            assertThat(person.groups.get(i).name).isEqualTo(groups[i]);
        }
    }
}