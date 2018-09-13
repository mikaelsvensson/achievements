package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpsertPersonResultDTO {
    public PersonBaseDTO person;
    public boolean isNew;

    public UpsertPersonResultDTO() {
    }

    public UpsertPersonResultDTO(@JsonProperty("person") PersonBaseDTO person,
                                 @JsonProperty("new") boolean isNew) {
        this.person = person;
        this.isNew = isNew;
    }
}
