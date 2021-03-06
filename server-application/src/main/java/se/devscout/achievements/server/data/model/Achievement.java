package se.devscout.achievements.server.data.model;

import com.google.common.base.Objects;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "achievements")
@NamedQueries(
        {
                @NamedQuery(
                        name = "Achievement.findWithProgressForOrganization",
                        query = "SELECT DISTINCT " +
                                "  a " +
                                "FROM " +
                                "  Achievement a JOIN FETCH a.steps s " +
                                "WHERE " +
                                "  a.id IN (SELECT DISTINCT ta.id FROM Achievement ta JOIN ta.steps tas JOIN tas.progressList tp WHERE tp.person.organization = :organization)"),
                @NamedQuery(
                        name = "Achievement.findWithProgressForPerson",
                        query = "SELECT DISTINCT " +
                                "  a " +
                                "FROM " +
                                "  Achievement a JOIN FETCH a.steps s " +
                                "WHERE " +
                                "  a.id IN (SELECT DISTINCT ta.id FROM Achievement ta JOIN ta.steps tas JOIN tas.progressList tp WHERE tp.person = :person)")
        }
)
public class Achievement extends AchievementProperties {
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    @Id
    @Type(type = "uuid-binary")
    private UUID id;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "achievement")
    @OrderColumn(name = "sort_order")
    private List<AchievementStep> steps = new ArrayList<>();

    @ManyToMany(mappedBy = "awards")
    private Set<Person> awardedTo = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public List<AchievementStep> getSteps() {
        return steps;
    }

    public void setSteps(List<AchievementStep> steps) {
        this.steps = steps;
    }

    public Set<Person> getAwardedTo() {
        return awardedTo;
    }

    public void setAwardedTo(Set<Person> awardedTo) {
        this.awardedTo = awardedTo;
    }

    public void addAwardFor(Person person) {
        awardedTo.add(person);
        person.getAwards().add(this);
    }

    public void removeAwardFor(Person person) {
        awardedTo.remove(person);
        person.getAwards().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Achievement)) return false;
        var that = (Achievement) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
