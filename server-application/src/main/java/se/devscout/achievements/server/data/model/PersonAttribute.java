package se.devscout.achievements.server.data.model;

import com.google.common.base.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

@Embeddable
public class PersonAttribute {
    @Column(length = 30)
    @Size(max = 30)
    public String key;

    @Column(length = 100)
    @Size(max = 100)
    public String value;

    public PersonAttribute() {
    }

    public PersonAttribute(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonAttribute)) return false;
        var that = (PersonAttribute) o;
        return Objects.equal(key, that.key) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, value);
    }
}