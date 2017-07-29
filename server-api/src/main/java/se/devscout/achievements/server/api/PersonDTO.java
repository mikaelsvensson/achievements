package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PersonDTO {
    public String id;
    public String name;

    public PersonDTO() {
    }

    public PersonDTO(@JsonProperty("id") String id,
                     @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
