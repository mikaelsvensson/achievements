package se.devscout.achievements.server.data.importer;

import se.devscout.achievements.server.api.PersonDTO;

import java.io.Reader;
import java.util.List;

public interface PeopleDataSource {
    List<PersonDTO> read(Reader reader) throws PeopleDataSourceException;
}
