package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PersonDTO {
    public String id;
    public String name;
    public String email;

    public PersonDTO() {
    }

    public PersonDTO(String id,
                     String name) {
        this(id, name, null);
    }
    public PersonDTO(@JsonProperty("id") String id,
                     @JsonProperty("name") String name,
                     @JsonProperty("email") String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
}
