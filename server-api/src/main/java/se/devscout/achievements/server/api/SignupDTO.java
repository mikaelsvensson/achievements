package se.devscout.achievements.server.api;

import java.util.UUID;

public class SignupDTO {
    public UUID existing_organization_id;
    public String new_organization_name;
    public String person_name;
    public String user_password;

    public SignupDTO() {
    }

    public SignupDTO(UUID existing_organization_id, String person_name, String user_password) {
        this.existing_organization_id = existing_organization_id;
        this.person_name = person_name;
        this.user_password = user_password;
    }

    public SignupDTO(String new_organization_name, String person_name, String user_password) {
        this.new_organization_name = new_organization_name;
        this.person_name = person_name;
        this.user_password = user_password;
    }
}
