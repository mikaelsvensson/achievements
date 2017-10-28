package se.devscout.achievements.server.api;

import java.util.UUID;

public class SignupDTO {
    public UUID existing_organization_id;
    public String new_organization_name;
    public String name;
    public String email;
    public String password;

    public SignupDTO() {
    }

    public SignupDTO(UUID existing_organization_id, String name, String password, String email) {
        this.existing_organization_id = existing_organization_id;
        this.name = name;
        this.password = password;
        this.email = email;
    }

    public SignupDTO(String new_organization_name, String name, String password, String email) {
        this.new_organization_name = new_organization_name;
        this.name = name;
        this.password = password;
        this.email = email;
    }
}
