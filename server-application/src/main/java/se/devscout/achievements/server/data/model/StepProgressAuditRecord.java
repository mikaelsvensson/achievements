package se.devscout.achievements.server.data.model;

import javax.persistence.*;

@Entity
@DiscriminatorValue("step_progress")
@NamedQueries({
        @NamedQuery(
                name = "StepProgressAuditRecord.byAchievement",
                query = "" +
                        "SELECT p " +
                        "FROM StepProgressAuditRecord p " +
                        "WHERE p.step.achievement.id = :achievementId"
        )
})
public class StepProgressAuditRecord extends AbstractAuditRecord {

    @ManyToOne
    @JoinColumn(name = "step_id")
    private AchievementStep step;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    public StepProgressAuditRecord() {
    }

    public StepProgressAuditRecord(Person user, String data, AchievementStep step, Person person, String httpMethod, int responseCode) {
        super(user, data, httpMethod, responseCode);
        this.step = step;
        this.person = person;
    }

    public AchievementStep getStep() {
        return step;
    }

    public void setStep(AchievementStep step) {
        this.step = step;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
