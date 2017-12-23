package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PersonProfileDTO {
    public OrganizationDTO organization;
    public PersonDTO person;

    public PersonProfileDTO(@JsonProperty("organization") OrganizationDTO organization,
                            @JsonProperty("person") PersonDTO person) {
        this.organization = organization;
        this.person = person;
    }
}
