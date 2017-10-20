package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignupResponseDTO {
    public PersonDTO person;
    public OrganizationDTO organization;

    public SignupResponseDTO(@JsonProperty("person") PersonDTO person, @JsonProperty("organization") OrganizationDTO organization) {
        this.person = person;
        this.organization = organization;
    }
}
