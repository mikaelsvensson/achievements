package se.devscout.achievements.server.data.model;

import com.google.common.base.Objects;

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
        @NamedQuery(name = "Person.hasBeenAwarded", query = "SELECT p FROM Person p WHERE :achievement MEMBER OF p.awards AND p.organization = :organization"),
        @NamedQuery(name = "Person.getByEmail", query = "SELECT p FROM Person p WHERE LOWER(p.email) = LOWER(:email)")
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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupMembership> memberships = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AchievementStepProgress> achievementStepProgress = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StepProgressAuditRecord> auditRecords = new HashSet<>();

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE})
    @JoinTable(name = "person_awardedachievements",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "achievement_id"))
    protected Set<Achievement> awards = new HashSet<>();

    public Person() {
    }

    public Person(String name, String role) {
        this(null, name, role);
    }

    public Person(Integer id, String name, String role) {
        super(name, role);
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

    public Set<GroupMembership> getMemberships() {
        return memberships;
    }

    public void setMemberships(Set<GroupMembership> memberships) {
        this.memberships = memberships;
    }

    public Set<Credentials> getCredentials() {
        return credentials;
    }

    public void setCredentials(Set<Credentials> credentials) {
        this.credentials = credentials;
    }

    public Set<AchievementStepProgress> getAchievementStepProgress() {
        return achievementStepProgress;
    }

    public void setAchievementStepProgress(Set<AchievementStepProgress> achievementStepProgress) {
        this.achievementStepProgress = achievementStepProgress;
    }

    public Set<StepProgressAuditRecord> getAuditRecords() {
        return auditRecords;
    }

    public void setAuditRecords(Set<StepProgressAuditRecord> auditRecords) {
        this.auditRecords = auditRecords;
    }

    public Set<Achievement> getAwards() {
        return awards;
    }

    public void setAwards(Set<Achievement> awards) {
        this.awards = awards;
    }

    public void addAwardFor(Achievement achievement) {
        awards.add(achievement);
        achievement.getAwardedTo().add(this);
    }

    public void removeAwardFor(Achievement achievement) {
        awards.remove(achievement);
        achievement.getAwardedTo().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        var that = (Person) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
