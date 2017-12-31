package se.devscout.achievements.server.auth.openid;

//TODO: These properties are standardized by OpenId Connect, right?
public class TokenResponse {
    public String token_type;
    public String scope;
    public long expires_in;
    public String access_token;
    public String refresh_token;
    public String id_token;
}
