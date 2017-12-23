package se.devscout.achievements.server.data.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Table(name = "achievement_steps")
@Entity
@NamedQueries({
        @NamedQuery(name = "AchievementStep.getByAchievement", query = "SELECT step FROM AchievementStep step where step.achievement = :achievement")
})
public class AchievementStep extends AchievementStepProperties {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "achievement_id")
    @NotNull
    private Achievement achievement;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "step")
    private List<AchievementStepProgress> progressList = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Achievement getAchievement() {
        return achievement;
    }

    public void setAchievement(Achievement achievement) {
        this.achievement = achievement;
    }

    public List<AchievementStepProgress> getProgressList() {
        return progressList;
    }
}
