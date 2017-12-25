package se.devscout.achievements.server.api;

public class PersonBaseDTO {
    public Integer id;
    public String name;

    public PersonBaseDTO() {
    }

    public PersonBaseDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
