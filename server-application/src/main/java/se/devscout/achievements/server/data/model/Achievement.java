package se.devscout.achievements.server.data.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "achievements")
@NamedQueries({@NamedQuery(name = "Achievement.find", query = "SELECT a FROM Achievement a WHERE a.name LIKE :name")})
public class Achievement extends AchievementProperties {
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    @Id
    private UUID id;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "achievement")
    private List<AchievementStep> steps = new ArrayList<>();

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
}
