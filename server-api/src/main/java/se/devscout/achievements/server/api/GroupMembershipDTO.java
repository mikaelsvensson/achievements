package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.devscout.achievements.server.data.model.GroupRole;

public class GroupMembershipDTO {
    public GroupBaseDTO group;
    public PersonBaseDTO person;
    public GroupRole role;

    public GroupMembershipDTO() {
    }

    public GroupMembershipDTO(@JsonProperty("group") GroupBaseDTO group,
                              @JsonProperty("person") PersonBaseDTO person,
                              @JsonProperty("role") GroupRole role) {
        this.group = group;
        this.person = person;
        this.role = role;
    }
}
