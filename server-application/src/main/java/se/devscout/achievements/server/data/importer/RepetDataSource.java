package se.devscout.achievements.server.data.importer;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import se.devscout.achievements.server.data.SlugGenerator;
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

public class RepetDataSource implements PeopleDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepetDataSource.class);
    private final SlugGenerator slugGenerator = new SlugGenerator();
    private DocumentBuilder documentBuilder;

    public RepetDataSource() throws ParserConfigurationException {
        initSafeDocumentParser();
    }

    /**
     * See https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#Java
     */
    private void initSafeDocumentParser() throws ParserConfigurationException {
        final var builderFactory = DocumentBuilderFactory.newInstance();
        String FEATURE = null;
        try {
            // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all XML entity attacks are prevented
            // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
            FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
            builderFactory.setFeature(FEATURE, true);

            // If you can't completely disable DTDs, then at least do the following:
            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
            // JDK7+ - http://xml.org/sax/features/external-general-entities
            FEATURE = "http://xml.org/sax/features/external-general-entities";
            builderFactory.setFeature(FEATURE, false);

            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
            // JDK7+ - http://xml.org/sax/features/external-parameter-entities
            FEATURE = "http://xml.org/sax/features/external-parameter-entities";
            builderFactory.setFeature(FEATURE, false);

            // Disable external DTDs as well
            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            builderFactory.setFeature(FEATURE, false);

            // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
            builderFactory.setXIncludeAware(false);
            builderFactory.setExpandEntityReferences(false);

            // And, per Timothy Morgan: "If for some reason support for inline DOCTYPEs are a requirement, then
            // ensure the entity settings are disabled (as shown above) and beware that SSRF attacks
            // (http://cwe.mitre.org/data/definitions/918.html) and denial
            // of service attacks (such as billion laughs or decompression bombs via "jar:") are a risk."

            // remaining parser logic
            builderFactory.setNamespaceAware(true);
            documentBuilder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // This should catch a failed setFeature feature
            LOGGER.info("ParserConfigurationException was thrown. The feature '" + FEATURE + "' is probably not supported by your XML processor.");
            throw e;
        }
    }

    @Override
    public List<PersonDTO> read(Reader reader) throws PeopleDataSourceException {
        final ArrayList<PersonDTO> people = Lists.newArrayList();
        final var document = readXml(reader);
        final var groupElements = document.getDocumentElement().getElementsByTagNameNS("G7_n\u00e4rvarolista", "table2");
        for (var i = 0; i < groupElements.getLength(); i++) {
            final var groupElement = (Element) groupElements.item(i);
            final var groupName = groupElement.getAttribute("textbox188");
            final var peopleElements = groupElement.getElementsByTagNameNS("G7_n\u00e4rvarolista", "Detail");
            for (var x = 0; x < peopleElements.getLength(); x++) {
                final var personElement = (Element) peopleElements.item(x);
                final var name = personElement.getAttribute("textbox171");
                final var match = people.stream().filter(p -> p.custom_identifier.equals(toCustomIdentifier(name))).findFirst();
                if (match.isPresent()) {
                    final var group = new GroupBaseDTO();
                    group.name = groupName;
                    match.get().groups.add(group);
                } else {
                    final var person = new PersonDTO();
                    person.name = toFirstLastName(name);
                    person.custom_identifier = toCustomIdentifier(name);
                    person.role = Roles.READER;
                    final var group = new GroupBaseDTO();
                    group.name = groupName;
                    person.groups = Lists.newArrayList(group);
                    people.add(person);
                }
            }
        }
        return people;
    }

    private String toFirstLastName(String sourceName) {
        var pos = sourceName.indexOf(", ");
        if (pos >= 0) {
            return sourceName.substring(pos + 2) + " " + sourceName.substring(0, pos);
        }
        return sourceName;
    }

    private String toCustomIdentifier(String text) {
        return slugGenerator.toSlug(text);
    }

    private Document readXml(Reader reader) throws PeopleDataSourceException {
        try {
            final var source = new InputSource(reader);
            return documentBuilder.parse(source);
        } catch (SAXException | IOException | RuntimeException e) {
            throw new PeopleDataSourceException("Could not read XML file", e);
        }
    }
}
