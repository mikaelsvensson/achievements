package se.devscout.achievements.server.data.model;

import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@MappedSuperclass
public class PersonProperties {

    static final String CUSTOM_IDENTIFIER_COLNAME = "custom_identifier";

    @Size(min = 1, max = 100)
    @Column(length = 100)
    @NotEmpty
    private String name;

    @Size(max = 100)
    @Column(length = 100)
    @EmailAddress
    //TODO: Changes to email address should propagate to credentials if user signs in using email address.
    private String email;

    @ElementCollection
    @Size(max = 10)
    @CollectionTable(name = "person_attributes", joinColumns = @JoinColumn(name = "person_id"))
    private Set<PersonAttribute> attributes = new HashSet<>();

    @Column(name = CUSTOM_IDENTIFIER_COLNAME)
    @Size(max = 100)
    private String customIdentifier;

    @Size(max = 100)
    @NotNull
    private String role;

    public PersonProperties() {
    }

    public PersonProperties(String name, String email, Set<PersonAttribute> attributes, String customIdentifier, String role) {
        this.name = name;
        this.email = email;
        this.attributes = attributes;
        this.customIdentifier = customIdentifier;
        this.role = role;
    }

    public PersonProperties(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public PersonProperties(String name, String customIdentifier, String role) {
        this.name = name;
        this.customIdentifier = customIdentifier;
        this.role = role;
    }

    public PersonProperties(String name, Set<PersonAttribute> attributes, String role) {
        this.name = name;
        this.attributes = attributes;
        this.role = role;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void apply(PersonProperties that) {
        name = that.name;
        email = that.email;
        customIdentifier = that.customIdentifier;
        role = that.role;
        attributes.clear();
        if (that.attributes != null) {
            attributes.addAll(that.attributes);
        }
    }

    public String getCustomIdentifier() {
        return customIdentifier;
    }

    public void setCustomIdentifier(String customIdentifier) {
        this.customIdentifier = customIdentifier;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
