package se.devscout.achievements.server.data.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "credentials",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"type", "user_id"})
        })
@NamedQueries({
        @NamedQuery(
                name = "Credentials.getByUsername",
                query = "SELECT c FROM Credentials c WHERE c.type = :type AND c.userId = :userId"
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

    public Credentials(String username, CredentialsType provider, byte[] secret, Person person) {
        super(username, provider, secret);
        this.person = person;
    }

    public Credentials(String username, CredentialsType provider, byte[] secret) {
        super(username, provider, secret);
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
