package se.devscout.achievements.server.data.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import se.devscout.achievements.server.auth.SecretValidator;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "credentials",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "username"})
        })
@NamedQueries({
        @NamedQuery(
                name = "Credentials.getByUsername",
                query = "SELECT c FROM Credentials c WHERE c.provider = :provider AND c.username = :username"
        ),
        @NamedQuery(
                name = "Credentials.getByPerson",
                query = "SELECT c FROM Credentials c WHERE c.person = :person"
        )
})
public class Credentials extends CredentialsProperties {
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    @Id
    @Type(type = "uuid-binary")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    private Person person;

    public Credentials() {
    }

    public Credentials(String username, SecretValidator validator, Person person) {
        super(username, validator);
        this.person = person;
    }

    public Credentials(String username, SecretValidator validator) {
        super(username, validator);
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
