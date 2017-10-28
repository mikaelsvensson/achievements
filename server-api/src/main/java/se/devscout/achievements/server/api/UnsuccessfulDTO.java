package se.devscout.achievements.server.api;

public class UnsuccessfulDTO {
    public final String message;

    public int status;

    public UnsuccessfulDTO(String message, int status) {
        this.message = message;
        this.status = status;
    }
}
