package se.devscout.achievements.server.data.dao;

public class DuplicateCustomIdentifier extends DaoException {
    public DuplicateCustomIdentifier(String message) {
        super(message);
    }
}
