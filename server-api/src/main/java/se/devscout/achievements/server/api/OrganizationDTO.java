package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrganizationDTO {
    public String id;
    public String name;

    public OrganizationDTO() {
    }

    public OrganizationDTO(@JsonProperty("id") String id,
                           @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
