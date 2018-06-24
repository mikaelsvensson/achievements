package se.devscout.achievements.server.data.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("http")
public class HttpAuditRecord extends AbstractAuditRecord {

    @Column(name = "resource_uri", length = 2048)
    private String resourceUri;

    public HttpAuditRecord() {
    }

    public HttpAuditRecord(Person user, String data, String httpMethod, String resourceUri, int responseCode) {
        super(user, data, httpMethod, responseCode);
        this.resourceUri = resourceUri;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

}
