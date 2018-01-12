package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GroupDTO extends GroupBaseDTO {
    public List<PersonBaseDTO> people;
    public OrganizationBaseDTO organization;

    public GroupDTO() {
        this(null, null, null, null);
    }

    public GroupDTO(Integer id,
                    String name) {
        this(id, name, null, null);
    }

    public GroupDTO(@JsonProperty("id") Integer id,
                    @JsonProperty("name") String name,
                    @JsonProperty("organization") OrganizationBaseDTO organization,
                    @JsonProperty("people") List<PersonBaseDTO> people) {
        super(id, name);
        this.people = people;
    }
}
