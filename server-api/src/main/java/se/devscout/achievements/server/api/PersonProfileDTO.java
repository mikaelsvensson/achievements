package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PersonProfileDTO {
    public OrganizationDTO organization;
    public PersonDTO person;
    public GettingStartedDTO getting_started;

    public PersonProfileDTO(@JsonProperty("organization") OrganizationDTO organization,
                            @JsonProperty("person") PersonDTO person,
                            @JsonProperty("getting_started") GettingStartedDTO gettingStarted) {
        this.organization = organization;
        this.person = person;
        this.getting_started = gettingStarted;
    }
}
