package se.devscout.achievements.server.api;

public class PersonAttributeDTO {
    public String key;
    public String value;

    public PersonAttributeDTO() {
    }

    public PersonAttributeDTO(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
