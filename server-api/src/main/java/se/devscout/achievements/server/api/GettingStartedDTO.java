package se.devscout.achievements.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GettingStartedDTO {
    public boolean is_only_person_in_organization;
    public boolean is_welcome_letter_sent_to_people;
    public boolean is_progress_set_for_one_person;
    public boolean is_group_created;
    public boolean is_password_credential_created;
    public boolean is_password_set;

    public GettingStartedDTO(@JsonProperty("is_only_person_in_organization") boolean isOnlyPersonInOrganization,
                             @JsonProperty("is_welcome_letter_sent_to_people") boolean isWelcomeLetterSentToPeople,
                             @JsonProperty("is_progress_set_for_one_person") boolean isProgressSetForOnePerson,
                             @JsonProperty("is_password_credential_created") boolean isPasswordCredentialCreated,
                             @JsonProperty("is_password_set") boolean isPasswordSet,
                             @JsonProperty("is_group_created") boolean isGroupCreated) {
        this.is_only_person_in_organization = isOnlyPersonInOrganization;
        this.is_welcome_letter_sent_to_people = isWelcomeLetterSentToPeople;
        this.is_progress_set_for_one_person = isProgressSetForOnePerson;
        this.is_group_created = isGroupCreated;
        this.is_password_credential_created = isPasswordCredentialCreated;
        this.is_password_set = isPasswordSet;
    }
}
