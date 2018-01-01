package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PersonDTO extends PersonBaseDTO {
    public String email;
    public OrganizationBaseDTO organization;
    public String custom_identifier;
    public String role;

    public PersonDTO() {
        super();
    }

    public PersonDTO(Integer id,
                     String name) {
        this(id, name, null, null, null, null);
    }

    public PersonDTO(Integer id,
                     String name,
                     String role) {
        this(id, name, null, null, null, role);
    }

    public PersonDTO(@JsonProperty("id") Integer id,
                     @JsonProperty("name") String name,
                     @JsonProperty("email") String email,
                     @JsonProperty("custom_identifier") String custom_identifier,
                     @JsonProperty("organization") OrganizationBaseDTO organization,
                     @JsonProperty("role") String role) {
        super(id, name);
        this.email = email;
        this.organization = organization;
        this.custom_identifier = custom_identifier;
        this.role = role;
    }
}
