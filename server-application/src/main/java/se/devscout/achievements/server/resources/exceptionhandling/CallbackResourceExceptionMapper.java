package se.devscout.achievements.server.resources.exceptionhandling;

import com.google.common.base.CaseFormat;
import org.slf4j.LoggerFactory;
import se.devscout.achievements.server.resources.OpenIdResourceCallbackException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;

public class CallbackResourceExceptionMapper implements ExceptionMapper<OpenIdResourceCallbackException> {
    @Override
    public Response toResponse(OpenIdResourceCallbackException exception) {
        LoggerFactory.getLogger(CallbackResourceExceptionMapper.class).info("Authentication callback cause exception.", exception);

        String subPath = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, exception.getType().name());
        //TODO: Make redirection host configurable
        return Response.temporaryRedirect(URI.create("http://localhost:63344/#signin-failed/" + subPath)).build();
    }
}
