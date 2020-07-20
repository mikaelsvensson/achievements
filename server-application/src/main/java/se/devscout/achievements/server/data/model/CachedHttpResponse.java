package se.devscout.achievements.server.data.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cached_http_responses")
public class CachedHttpResponse {
    @Id
    @Column(name = "resource_uri", length = 2048, nullable = false)
    private String resourceUri;

    @Column(name = "raw_html", length = 1_000_000, nullable = false)
    private String rawHtml;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime dateTime;

    public CachedHttpResponse() {
    }

    public CachedHttpResponse(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public CachedHttpResponse(String resourceUri, String rawHtml, OffsetDateTime dateTime) {
        this.resourceUri = resourceUri;
        this.rawHtml = rawHtml;
        this.dateTime = dateTime;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public String getRawHtml() {
        return rawHtml;
    }

    public void setRawHtml(String rawHtml) {
        this.rawHtml = rawHtml;
    }

    public OffsetDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(OffsetDateTime dateTime) {
        this.dateTime = dateTime;
    }
}
