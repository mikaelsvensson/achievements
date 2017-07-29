package se.devscout.achievements.server.data.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.net.URI;

@Entity
@DiscriminatorValue("reference")
public class AchievementReferenceStep extends AchievementStep {
    @Column(length = 5_000)
    private URI reference;

    public AchievementReferenceStep() {
    }

    public AchievementReferenceStep(URI reference) {
        this.reference = reference;
    }

    public URI getReference() {
        return reference;
    }

    public void setReference(URI reference) {
        this.reference = reference;
    }
}
