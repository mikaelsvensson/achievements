package se.devscout.achievements.server.data.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "organization")
@NamedQueries({
        @NamedQuery(name = "Organization.all", query = "SELECT o FROM Organization o"),
        @NamedQuery(name = "Organization.find", query = "SELECT o FROM Organization o WHERE o.name LIKE :name")
})
public class Organization extends OrganizationProperties {
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    @Id
    private UUID id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Person> people = new HashSet<>();

    public Organization() {
    }

    public Organization(String name) {
        this(null, name);
    }

    public Organization(UUID id, String name) {
        super(name);
        this.id = id;
    }
}
