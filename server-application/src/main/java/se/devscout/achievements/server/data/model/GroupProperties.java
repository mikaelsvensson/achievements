package se.devscout.achievements.server.data.model;

import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass
public class GroupProperties {

    static final String NAME_COLNAME = "name";

    @Column(name = NAME_COLNAME, length = 100)
    @Size(min = 1, max = 100)
    @NotEmpty
    private String name;

    public GroupProperties() {
    }

    public GroupProperties(String name) {
        this.name = name;
    }

    public void apply(GroupProperties that) {
        name = that.name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
