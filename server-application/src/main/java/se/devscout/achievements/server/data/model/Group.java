package se.devscout.achievements.server.data.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import static se.devscout.achievements.server.data.model.GroupProperties.NAME_COLNAME;

@Entity
@Table(
        name = "groups",
        uniqueConstraints = @UniqueConstraint(name = "idx_group_organization", columnNames = {"organization_id", NAME_COLNAME}))
@NamedQueries({
        @NamedQuery(name = "Group.getByOrganization", query = "SELECT g FROM se.devscout.achievements.server.data.model.Group g where g.organization = :organization"),
        @NamedQuery(name = "Group.getByName", query = "SELECT g FROM se.devscout.achievements.server.data.model.Group g where g.name = :name AND g.organization = :organization")
})
public class Group extends GroupProperties {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    @NotNull
    private Organization organization;

//    @OneToMany(fetch = FetchType.LAZY, mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<GroupMembership> members = new HashSet<>();

    public Group() {
    }

    public Group(String name) {
        this(null, name);
    }

    public Group(Integer id, String name) {
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

//    public Set<GroupMembership> getMembers() {
//        return members;
//    }
//
//    public void setMembers(Set<GroupMembership> members) {
//        this.members = members;
//    }
}
