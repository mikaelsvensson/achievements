package se.devscout.achievements.server.data.model;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class GroupMembershipId implements Serializable {

    public GroupMembershipId() {
    }

    public GroupMembershipId(Group group, Person person) {
        this.group = group;
        this.person = person;
    }

    private Group group;

    private Person person;

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

}
