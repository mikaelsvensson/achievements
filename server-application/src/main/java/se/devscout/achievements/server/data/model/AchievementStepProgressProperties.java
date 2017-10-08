package se.devscout.achievements.server.data.model;

import javax.persistence.*;
import java.util.Set;

@MappedSuperclass
public class AchievementStepProgressProperties {
    public AchievementStepProgressProperties() {
    }

    public AchievementStepProgressProperties(boolean completed, String note) {
        this.completed = completed;
        this.note = note;
    }

    private boolean completed;

//    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(name = "achievementstepprogress_substeps", joinColumns = @JoinColumn(name = "achievementstepprogress_id"))
//    @Column(name = "substep", length = 50)
//    private Set<String> subSteps;

    private String note;

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

//    public Set<String> getSubSteps() {
//        return subSteps;
//    }
//
//    public void setSubSteps(Set<String> subSteps) {
//        this.subSteps = subSteps;
//    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
