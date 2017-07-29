package se.devscout.achievements.server.data.model;

import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass
public class OrganizationProperties {

    @Size(min = 1, max = 100)
    @Column(unique = true, length = 100)
    @NaturalId(mutable = true)
    private String name;

    public OrganizationProperties() {
    }

    public OrganizationProperties(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void apply(OrganizationProperties that) {
        name = that.name;
    }
}
