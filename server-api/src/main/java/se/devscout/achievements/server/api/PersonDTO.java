package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonDTO extends PersonBaseDTO {
    public String email;
    public OrganizationBaseDTO organization;
    public String custom_identifier;
    public String role;
    public Map<String, String> attr;

    public PersonDTO() {
        super();
    }

    public PersonDTO(Integer id,
                     String name) {
        this(id, name, null, null, null, null, null);
    }

    public PersonDTO(Integer id,
                     String name,
                     String role) {
        this(id, name, null, null, null, role, null);
    }

    public PersonDTO(@JsonProperty("id") Integer id,
                     @JsonProperty("name") String name,
                     @JsonProperty("email") String email,
                     @JsonProperty("custom_identifier") String custom_identifier,
                     @JsonProperty("organization") OrganizationBaseDTO organization,
                     @JsonProperty("role") String role,
                     @JsonProperty("attr") Map<String, String> attr) {
        super(id, name);
        this.email = email;
        this.organization = organization;
        this.custom_identifier = custom_identifier;
        this.role = role;
        this.attr = attr;
    }
}
