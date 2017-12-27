package se.devscout.achievements.server.data.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

import static se.devscout.achievements.server.data.model.PersonProperties.CUSTOM_IDENTIFIER_COLNAME;

@Entity
@Table(
        name = "person",
        uniqueConstraints = @UniqueConstraint(name = "idx_person_customid", columnNames = {"organization_id", CUSTOM_IDENTIFIER_COLNAME}))
@NamedQueries({
        @NamedQuery(name = "Person.getByOrganization", query = "SELECT p FROM Person p where p.organization = :organization"),
        @NamedQuery(name = "Person.getByCustomId", query = "SELECT p FROM Person p WHERE p.customIdentifier = :customId AND p.organization = :organization"),
        @NamedQuery(name = "Person.getByEmail", query = "SELECT p FROM Person p WHERE p.email = :email")
})
public class Person extends PersonProperties {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    @NotNull
    private Organization organization;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Credentials> credentials = new HashSet<>();

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
