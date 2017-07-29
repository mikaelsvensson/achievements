package se.devscout.achievements.server.data.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "person")
@NamedQueries({
        @NamedQuery(name = "Person.getByOrganization", query = "SELECT p FROM Person p where p.organization.id = :organizationId")
})
public class Person extends PersonProperties {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    @NotNull
    private Organization organization;

    public Person() {
    }

    public Person(String name) {
        this(null, name);
    }

    public Person(Integer id, String name) {
        super(name);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
