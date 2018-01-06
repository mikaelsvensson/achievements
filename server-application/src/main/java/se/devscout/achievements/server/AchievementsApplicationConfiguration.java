package se.devscout.achievements.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import se.devscout.achievements.server.mail.SmtpSenderConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;

public class AchievementsApplicationConfiguration extends Configuration {
    private Long maxOrganizationCount;

    private URI serverApplicationHost;

    private URI guiApplicationHost;

    public URI getServerApplicationHost() {
        return serverApplicationHost;
    }

    public void setServerApplicationHost(URI serverApplicationHost) {
        this.serverApplicationHost = serverApplicationHost;
    }

    public URI getGuiApplicationHost() {
        return guiApplicationHost;
    }

    public void setGuiApplicationHost(URI guiApplicationHost) {
        this.guiApplicationHost = guiApplicationHost;
    }

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();
    private SmtpSenderConfiguration smtp;

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public Long getMaxOrganizationCount() {
        return maxOrganizationCount;
    }

    public void setMaxOrganizationCount(Long maxOrganizationCount) {
        this.maxOrganizationCount = maxOrganizationCount;
    }

    private AuthConfig authentication;

    public AuthConfig getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AuthConfig authentication) {
        this.authentication = authentication;
    }

    public SmtpSenderConfiguration getSmtp() {
        return smtp;
    }

    public void setSmtp(SmtpSenderConfiguration smtp) {
        this.smtp = smtp;
    }

    public static class AuthConfig {
        private String googleClientId;
        private String googleClientSecret;
        private String microsoftClientId;
        private String microsoftClientSecret;
        private String jwtSigningSecret;


        public String getGoogleClientId() {
            return googleClientId;
        }

        public void setGoogleClientId(String googleClientId) {
            this.googleClientId = googleClientId;
        }

        public String getJwtSigningSecret() {
            return jwtSigningSecret;
        }

        public void setJwtSigningSecret(String jwtSigningSecret) {
            this.jwtSigningSecret = jwtSigningSecret;
        }

        public String getGoogleClientSecret() {
            return googleClientSecret;
        }

        public void setGoogleClientSecret(String googleClientSecret) {
            this.googleClientSecret = googleClientSecret;
        }

        public String getMicrosoftClientId() {
            return microsoftClientId;
        }

        public void setMicrosoftClientId(String microsoftClientId) {
            this.microsoftClientId = microsoftClientId;
        }

        public String getMicrosoftClientSecret() {
            return microsoftClientSecret;
        }

        public void setMicrosoftClientSecret(String microsoftClientSecret) {
            this.microsoftClientSecret = microsoftClientSecret;
        }
    }
}
