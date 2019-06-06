package fr.gouv.vitam.common.security.waf;

import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

/**
 * Common filter checker for header, uri and parameter
 */
@Priority(Priorities.AUTHENTICATION + 1)
public class SanityCheckerCommonFilter implements ContainerRequestFilter {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SanityCheckerCommonFilter.class);
    private static final String CHECK_SANITY = "CHECK_SANITY";
    private static final String CODE_VITAM = "code_vitam";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            // 1- Check Headers
            SanityChecker.checkHeadersMap(requestContext.getHeaders());

            // 2- Check Path Parameters
            SanityChecker.checkUriParametersMap(requestContext.getUriInfo().getPathParameters());

            // 3- Check Query Parameters
            SanityChecker.checkUriParametersMap(requestContext.getUriInfo().getQueryParameters());

        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
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
