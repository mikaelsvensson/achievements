package se.devscout.achievements.server.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SetPasswordDTO {
    public String current_password;
    public String new_password;
    public String new_password_confirm;

    public SetPasswordDTO() {
    }

    public SetPasswordDTO(@JsonProperty("current_password")
                                  String current_password,
                          @JsonProperty("new_password")
                                  String new_password,
                          @JsonProperty("new_password_confirm")
                                  String new_password_confirm) {
        this.current_password = current_password;
        this.new_password = new_password;
        this.new_password_confirm = new_password_confirm;
    }
}
