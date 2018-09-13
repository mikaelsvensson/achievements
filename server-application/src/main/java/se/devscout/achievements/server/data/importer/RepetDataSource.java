package se.devscout.achievements.server.data.importer;

import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import se.devscout.achievements.server.api.GroupBaseDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.auth.Roles;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepetDataSource implements PeopleDataSource {

    private DocumentBuilder documentBuilder;

    public RepetDataSource() throws ParserConfigurationException {
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        documentBuilder = builderFactory.newDocumentBuilder();
    }

    @Override
    public List<PersonDTO> read(Reader reader) throws PeopleDataSourceException {
        final ArrayList<PersonDTO> people = Lists.newArrayList();
        final Document document = readXml(reader);
        // TODO: Encode non-ASCII characters in source code?
        final NodeList groupElements = document.getDocumentElement().getElementsByTagNameNS("G7_närvarolista", "table2");
        for (int i = 0; i < groupElements.getLength(); i++) {
            final Element groupElement = (Element) groupElements.item(i);
            final String groupName = groupElement.getAttribute("textbox188");
            final NodeList peopleElements = groupElement.getElementsByTagNameNS("G7_närvarolista", "Detail");
            for (int x = 0; x < peopleElements.getLength(); x++) {
                final Element personElement = (Element) peopleElements.item(x);
                final String name = personElement.getAttribute("textbox171");
                final Optional<PersonDTO> match = people.stream().filter(p -> p.custom_identifier.equals(name)).findFirst();
                if (match.isPresent()) {
                    final GroupBaseDTO group = new GroupBaseDTO();
                    group.name = groupName;
                    match.get().groups.add(group);
                } else {
                    final PersonDTO person = new PersonDTO();
                    person.name = person.custom_identifier = name;
                    person.role = Roles.READER;
                    final GroupBaseDTO group = new GroupBaseDTO();
                    group.name = groupName;
                    person.groups = Lists.newArrayList(group);
                    people.add(person);
                }
            }
        }
        return people;
    }

    private Document readXml(Reader reader) throws PeopleDataSourceException {
        try {
            final InputSource source = new InputSource(reader);
            return documentBuilder.parse(source);
        } catch (SAXException | IOException | RuntimeException e) {
            throw new PeopleDataSourceException("Could not read XML file", e);
        }
    }
}
