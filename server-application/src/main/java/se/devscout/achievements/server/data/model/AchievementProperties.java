package se.devscout.achievements.server.data.model;

import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@MappedSuperclass
public class AchievementProperties {

    @Size(min = 1, max = 100)
    @Column(unique = true, length = 100)
    @NaturalId(mutable = true)
    private String name;

    @Size(min = 1, max = 10_000)
    @Column(length = 10_000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "achievement_tags", joinColumns = @JoinColumn(name = "achivement_id"))
    @Column(name = "tag", length = 50)
    private Set<String> tags = new HashSet<>();

    @Column(length = 5_000)
    private URI image;

    public AchievementProperties() {
    }

    public AchievementProperties(String name) {
        this.name = name;
    }

    public AchievementProperties(String name, Set<String> tags) {
        this.name = name;
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public URI getImage() {
        return image;
    }

    public void setImage(URI image) {
        this.image = image;
    }

    public void apply(AchievementProperties that) {
        name = that.name;
        description = that.description;
        tags.clear();
        tags.addAll(that.tags);
        image = that.image;
//        steps.clear();
//        steps.addAll(that.steps);
    }
}