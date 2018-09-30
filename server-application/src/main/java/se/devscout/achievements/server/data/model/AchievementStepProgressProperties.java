package se.devscout.achievements.server.data.model;

import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@MappedSuperclass
public class AchievementStepProgressProperties {

    public static final int PROGRESS_COMPLETED = 100;
    public static final int PROGRESS_NOT_STARTED = 0;

    public AchievementStepProgressProperties() {
    }

    public AchievementStepProgressProperties(boolean completed, String note) {
        this(toProgress(completed), note);
    }

    public AchievementStepProgressProperties(int value, String note) {
        setValue(value);
        this.note = note;
    }

    // TODO: The "completed" property is sort-of redundant. Old audit records with JSON data might refer to it "indirectly" but that is about it.
    private Boolean completed;

    @Max(PROGRESS_COMPLETED)
    @Min(PROGRESS_NOT_STARTED)
    private Integer value;

//    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(name = "achievementstepprogress_substeps", joinColumns = @JoinColumn(name = "achievementstepprogress_id"))
//    @Column(name = "substep", length = 50)
//    private Set<String> subSteps;

    private String note;

    public boolean isCompleted() {
        return (completed != null && completed) || toCompleted(value);
    }

    public void setCompleted(boolean completed) {
        setValue(toProgress(completed));
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

    public int getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
        this.completed = toCompleted(value);
    }

    public static int toProgress(boolean completed) {
        return completed ? PROGRESS_COMPLETED : PROGRESS_NOT_STARTED;
    }

    private static boolean toCompleted(Integer progress) {
        return progress != null && progress == PROGRESS_COMPLETED;
    }
}
