package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrganizationDTO extends OrganizationBaseDTO {
    public String id;

    public OrganizationDTO() {
        super(null);
    }

    public OrganizationDTO(@JsonProperty("id") String id,
                           @JsonProperty("name") String name) {
        super(name);
        this.id = id;
    }
}
