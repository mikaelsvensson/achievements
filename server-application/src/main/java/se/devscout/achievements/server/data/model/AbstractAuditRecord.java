package se.devscout.achievements.server.data.model;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_log")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", length = 15)
@NamedQueries({
        @NamedQuery(
                name = "AbstractAuditRecord.readAllReverse",
                query = "SELECT r FROM AbstractAuditRecord r ORDER BY r.id DESC"
        )
})
public class AbstractAuditRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "response_code")
    private int responseCode;

    @ManyToOne
    private Person user;

    @Column(name = "timestamp")
    private OffsetDateTime dateTime;

    @Column(name = "data", length = 10_000, nullable = true)
    private String data;

    public AbstractAuditRecord() {
        this(null, OffsetDateTime.now(), null, null, 0);
    }

    public AbstractAuditRecord(Person user, String data, String httpMethod, int responseCode) {
        this(user, OffsetDateTime.now(), data, httpMethod, responseCode);
    }

    public AbstractAuditRecord(Person user, OffsetDateTime dateTime, String data, String httpMethod, int responseCode) {
        this.user = user;
        this.dateTime = dateTime;
        this.data = data;
        this.httpMethod = httpMethod;
        this.responseCode = responseCode;
    }

    public Long getId() {
        return id;
    }

    public Person getUser() {
        return user;
    }

    public void setUser(Person user) {
        this.user = user;
    }

    public OffsetDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(OffsetDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
}
