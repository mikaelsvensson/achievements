package se.devscout.achievements.server.auth.jwt;

public class JwtEmailAddressToken {
    private String email;

    public JwtEmailAddressToken(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
