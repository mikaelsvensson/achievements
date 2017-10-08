package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PersonDTO {
    public String name;

    public PersonDTO() {
    }

    public PersonDTO(@JsonProperty("name") String name) {
        this.name = name;
    }
}
