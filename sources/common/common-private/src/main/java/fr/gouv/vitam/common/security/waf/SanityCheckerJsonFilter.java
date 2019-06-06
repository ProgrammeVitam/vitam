package fr.gouv.vitam.common.security.waf;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.stream.StreamUtils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Filter checker for body json
 */
@Priority(Priorities.AUTHORIZATION)
public class SanityCheckerJsonFilter implements ContainerRequestFilter {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SanityCheckerJsonFilter.class);
    private static final String CHECK_SANITY = "CHECK_SANITY";
    private static final String CODE_VITAM = "code_vitam";
    private static final AlertService alertService = new AlertServiceImpl();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final InputStream bodyInputStream = requestContext.getEntityStream();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        StreamUtils.copy(bodyInputStream, bout);
        try {
            SanityChecker.checkJsonAll(JsonHandler.getFromBytes(bout.toByteArray()));
            requestContext.setEntityStream(new ByteArrayInputStream(bout.toByteArray()));
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            requestContext.abortWith(
                Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorEntity(Status.PRECONDITION_FAILED, exc.getMessage())).build());
        } catch (InvalidParseOperationException exc) {
            LOGGER.error(exc);
            alertService.createAlert("Json invalid: " + exc.getMessage());
            requestContext.abortWith(
                Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorEntity(Status.PRECONDITION_FAILED, exc.getMessage())).build());
        }
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(CHECK_SANITY)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

}
