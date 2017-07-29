package se.devscout.achievements.server.data.model;

import org.hibernate.annotations.NaturalId;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@MappedSuperclass
public class PersonProperties {

    @Size(min = 1, max = 100)
    @Column(length = 100)
    @NotEmpty
    private String name;

    @ElementCollection
    @Size(max = 10)
    @CollectionTable(name = "person_attributes", joinColumns = @JoinColumn(name = "person_id"))
    private Set<PersonAttribute> attributes = new HashSet<>();

    public PersonProperties() {
    }

    public PersonProperties(String name) {
        this.name = name;
    }

    public PersonProperties(String name, Set<PersonAttribute> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<PersonAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<PersonAttribute> attributes) {
        this.attributes = attributes;
    }

    public void apply(PersonProperties that) {
        name = that.name;
        attributes.clear();
        attributes.addAll(that.attributes);
    }
}
