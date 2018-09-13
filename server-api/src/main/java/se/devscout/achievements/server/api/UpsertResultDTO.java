package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UpsertResultDTO {
    public List<UpsertPersonResultDTO> people;
    public String uploadId;

    public UpsertResultDTO(@JsonProperty("people") List<UpsertPersonResultDTO> people,
                           @JsonProperty("upload_id") String uploadId) {
        this.people = people;
        this.uploadId = uploadId;
    }
}
