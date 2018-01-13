package se.devscout.achievements.server.data.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@IdClass(GroupMembershipId.class)
@Table(name = "group_memberships")
@NamedQueries({
        @NamedQuery(name = "GroupMembership.getByGroup", query = "SELECT gm FROM GroupMembership gm where gm.group = :grp"),
        @NamedQuery(name = "GroupMembership.getByPerson", query = "SELECT gm FROM GroupMembership gm WHERE gm.person = :person")
})
public class GroupMembership {
    @Id
    @ManyToOne
    @NotNull
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Id
    @NotNull
    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    public GroupMembership() {
    }

    public GroupMembership(Group group, Person person, GroupRole role) {
        this.group = group;
        this.person = person;
        this.role = role;
    }

    private GroupRole role;

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public GroupRole getRole() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }

}
