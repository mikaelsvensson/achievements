package se.devscout.achievements.server.resources.exceptionhandling;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.resources.auth.ExternalIdpCallbackException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;

public class CallbackResourceExceptionMapper implements ExceptionMapper<ExternalIdpCallbackException> {
    private final URI guiApplicationHost;

    public CallbackResourceExceptionMapper(URI guiApplicationHost) {
        this.guiApplicationHost = guiApplicationHost;
    }

    @Override
    public Response toResponse(ExternalIdpCallbackException exception) {
        LoggerFactory.getLogger(CallbackResourceExceptionMapper.class).info("Authentication callback cause exception.", exception);

        var subPath = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, exception.getType().name());
        final var location = URI.create(StringUtils.appendIfMissing(guiApplicationHost.toString(), "/") + "#signin-failed/" + subPath);
        return Response.seeOther(location).build();
    }
}
