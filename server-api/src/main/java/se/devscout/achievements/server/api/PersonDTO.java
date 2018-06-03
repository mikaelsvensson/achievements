package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonDTO extends PersonBaseDTO {
    public String email;
    public OrganizationBaseDTO organization;
    public String custom_identifier;
    public String role;
    public List<PersonAttributeDTO> attributes;
    public List<GroupBaseDTO> groups;
    public boolean is_password_credential_created;
    public boolean is_password_set;

    public PersonDTO() {
        super();
    }

    public PersonDTO(Integer id,
                     String name) {
        this(id, name, null, null, null, null, false, false, null, null);
    }

    public PersonDTO(Integer id,
                     String name,
                     String role) {
        this(id, name, null, null, null, role, false, false, null, null);
    }

    public PersonDTO(@JsonProperty("id") Integer id,
                     @JsonProperty("name") String name,
                     @JsonProperty("email") String email,
                     @JsonProperty("custom_identifier") String customIdentifier,
                     @JsonProperty("organization") OrganizationBaseDTO organization,
                     @JsonProperty("role") String role,
                     @JsonProperty("is_password_credential_created") boolean isPasswordCredentialCreated,
                     @JsonProperty("is_password_set") boolean isPasswordSet,
                     @JsonProperty("attributes") List<PersonAttributeDTO> attributes,
                     @JsonProperty("groups") List<GroupBaseDTO> groups) {
        super(id, name);
        this.email = email;
        this.organization = organization;
        this.custom_identifier = customIdentifier;
        this.role = role;
        this.is_password_credential_created = isPasswordCredentialCreated;
        this.is_password_set = isPasswordSet;
        this.attributes = attributes;
        this.groups = groups;
    }
}
