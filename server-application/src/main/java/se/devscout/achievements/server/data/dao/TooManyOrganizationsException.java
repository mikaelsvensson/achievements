package se.devscout.achievements.server.data.dao;

public class TooManyOrganizationsException extends DaoException {
    public TooManyOrganizationsException(String message) {
        super(message);
    }
}
