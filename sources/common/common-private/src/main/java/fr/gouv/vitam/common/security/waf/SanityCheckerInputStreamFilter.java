package fr.gouv.vitam.common.security.waf;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.error.VitamError;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

/**
 * Filter checker for body inputstream
 */
@Priority(Priorities.AUTHORIZATION)
public class SanityCheckerInputStreamFilter implements ContainerRequestFilter {
    private static final String CHECK_SANITY = "CHECK_SANITY";
    private static final String CODE_VITAM = "code_vitam";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // Check if Transfer-Encoding header exists : this header indicates that the request was sent with chunked mode
        // transfer
        if (!requestContext.getHeaders().containsKey(GlobalDataRest.TRANSFER_ENCODING_HEADER)) {
            requestContext.abortWith(Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED)).build());
        }
    }

    private VitamError getErrorEntity(Response.Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(CHECK_SANITY)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase())
            .setDescription("Send of Stream must be in Chunked Mode");
    }
}
