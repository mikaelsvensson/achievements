package se.devscout.achievements.server.data.model;

import com.google.common.base.Objects;

import javax.persistence.*;
import java.io.Serializable;

@Table(name = "achievement_step_progress")
@Entity
@NamedQueries({
        @NamedQuery(
                name = "AchievementStepProgress.byAchievement",
                query = "SELECT p FROM AchievementStepProgress p WHERE p.step.achievement = :achievement"
        )
})
public class AchievementStepProgress extends AchievementStepProgressProperties {

    @EmbeddedId
    private Key id;

    @MapsId("stepId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id")
    private AchievementStep step;

    @MapsId("personId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    public AchievementStepProgress() {
    }

    public AchievementStepProgress(int value, String note, AchievementStep step, Person person) {
        super(value, note);
        this.step = step;
        this.person = person;
        this.id = new Key(step, person);
    }

    public Person getPerson() {
        return person;
    }

    public Key getId() {
        return id;
    }

    public AchievementStep getStep() {
        return step;
    }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "step_id")
        private Integer stepId;

        @Column(name = "person_id")
        private Integer personId;

        public Key() {
        }

        public Key(AchievementStep achievementStep, Person person) {
            this.stepId = achievementStep != null ? achievementStep.getId() : null;
            this.personId = person != null ? person.getId() : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equal(stepId, key.stepId) &&
                    Objects.equal(personId, key.personId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(stepId, personId);
        }
    }
}
