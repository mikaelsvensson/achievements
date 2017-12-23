package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrganizationBaseDTO
{
    public String name;

    public OrganizationBaseDTO() {
    }

    public OrganizationBaseDTO(@JsonProperty("name") String name) {
        this.name = name;
    }
}
