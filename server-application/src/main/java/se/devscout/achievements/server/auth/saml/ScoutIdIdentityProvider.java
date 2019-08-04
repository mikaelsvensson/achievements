package se.devscout.achievements.server.auth.saml;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.onelogin.saml2.Auth;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.auth.IdentityProvider;
import se.devscout.achievements.server.auth.IdentityProviderException;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.data.model.CredentialsType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ScoutIdIdentityProvider implements IdentityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScoutIdIdentityProvider.class);
    private final URI serverApplicationHost;

    public ScoutIdIdentityProvider(URI serverApplicationHost) {
        this.serverApplicationHost = serverApplicationHost;
    }

    @Override
    public URI getRedirectUri(HttpServletRequest req, HttpServletResponse resp, String callbackState, URI callbackUri) throws IdentityProviderException {
        try {
            Auth auth = createSamlAuth(req, resp);
            final URI redirectUri = URI.create(auth.login(callbackUri.toString(), false, false, true, false));
            final String requestId = auth.getLastRequestId();
            LOGGER.info("SAML request id: {}", requestId);
            return redirectUri;
        } catch (IOException e) {
            throw new IdentityProviderException("Error in SAML handler.", e);
        } catch (SettingsException e) {
            throw new IdentityProviderException("Settings exception in SAML handler.", e);
        }
    }

    @Override
    public ValidationResult handleCallback(HttpServletRequest req, HttpServletResponse resp) throws IdentityProviderException {
        try {
            Auth auth = createSamlAuth(req, resp);
            auth.processResponse();
            if (!auth.isAuthenticated()) {
                throw new IdentityProviderException("Could not perform authentication using SAML.");
            }
            List<String> errors = auth.getErrors();
            if (!errors.isEmpty()) {
                LOGGER.info(StringUtils.join(errors, ", "));
                if (auth.isDebugActive()) {
                    String errorReason = auth.getLastErrorReason();
                    if (errorReason != null && !errorReason.isEmpty()) {
                        LOGGER.info(auth.getLastErrorReason());
                    }
                }
                throw new IdentityProviderException("Error in SAML handler: " + StringUtils.join(errors, ", "));
            } else {
                Map<String, List<String>> attributes = auth.getAttributes();
                String nameId = auth.getNameId();
                String nameIdFormat = auth.getNameIdFormat();
                String sessionIndex = auth.getSessionIndex();
                String nameidNameQualifier = auth.getNameIdNameQualifier();
                String nameidSPNameQualifier = auth.getNameIdSPNameQualifier();

                LOGGER.info("attributes: {}", attributes);
                LOGGER.info("nameId: {}", nameId);
                LOGGER.info("nameIdFormat: {}", nameIdFormat);
                LOGGER.info("sessionIndex: {}", sessionIndex);
                LOGGER.info("nameidNameQualifier: {}", nameidNameQualifier);
                LOGGER.info("nameidSPNameQualifier: {}", nameidSPNameQualifier);

                String callbackState = req.getParameter("RelayState");

                if (attributes.isEmpty()) {
                    LOGGER.info("You don't have any attributes");
                } else {
                    Collection<String> keys = attributes.keySet();
                    for (String name : keys) {
                        LOGGER.info(name);
                        List<String> values = attributes.get(name);
                        for (String value : values) {
                            LOGGER.info(" - " + value);
                        }
                    }
                }
                return new ValidationResult("scoutid@example.com", "scoutid@example.com", true, CredentialsType.SCOUT_ID, null, callbackState);
            }
        } catch (Exception e) {
            throw new IdentityProviderException("Error in SAML handler.", e);
        }
    }

    @Override
    public Response getMetadataResponse() throws IdentityProviderException {
        try {
            final Auth auth = createSamlAuth(null, null);
            Saml2Settings settings = auth.getSettings();
            settings.setSPValidationOnly(true);
            String metadata = settings.getSPMetadata();
            List<String> errors = Saml2Settings.validateMetadata(metadata);
            if (errors.isEmpty()) {
                return Response.ok(metadata).type(MediaType.APPLICATION_XML_TYPE.withCharset("UTF-8")).build();
            } else {
                throw new IdentityProviderException(errors.stream().collect(Collectors.joining(", ")));
            }
        } catch (Exception e) {
            throw new IdentityProviderException("SAML metadata exception.", e);
        }
    }

    private Auth createSamlAuth(HttpServletRequest request, HttpServletResponse response) throws IdentityProviderException {
        final Saml2Settings settings = new SettingsBuilder()
                .fromProperties(loadProperties("onelogin-saml-defaults.properties"))
                .fromProperties(loadProperties("onelogin-saml-scoutid.properties"))
                .fromValues(ImmutableMap.<String, Object>builder()
                        .put("onelogin.saml2.sp.entityid", getUrl("metadata"))
                        .put("onelogin.saml2.sp.assertion_consumer_service.url", getUrl("callback"))
                        .put("onelogin.saml2.sp.single_logout_service.url", "")
                        .put("onelogin.saml2.debug", Boolean.TRUE)
                        .build())
                .build();

        try {
            return new Auth(settings, request, response);
        } catch (SettingsException e) {
            throw new IdentityProviderException("Could not initiate SAML object.", e);
        }
    }

    private String getUrl(String s) {
        return URI.create(StringUtils.appendIfMissing(serverApplicationHost.toString(), "/") + "api/auth/scoutid/" + s).toString();
    }

    private static Properties loadProperties(String resourceName) {
        try (InputStream stream = Resources.asByteSource(Resources.getResource(resourceName)).openBufferedStream();) {
            final Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
        }
        return null;
    }
}
