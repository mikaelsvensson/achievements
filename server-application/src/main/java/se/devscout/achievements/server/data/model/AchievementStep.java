package se.devscout.achievements.server.data.model;

import javax.persistence.*;

@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", length = 10)
@Table(name = "achievement_steps")
@Entity
public abstract class AchievementStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;
}
