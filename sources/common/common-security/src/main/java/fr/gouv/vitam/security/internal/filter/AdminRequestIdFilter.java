package fr.gouv.vitam.security.internal.filter;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class AdminRequestIdFilter implements ContainerRequestFilter {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminRequestIdFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String xrequestIdInHeader = requestContext.getHeaders().getFirst(GlobalDataRest.X_REQUEST_ID);
        if (xrequestIdInHeader != null) {
            LOGGER.error("RequestId found in admin API");
            // FIXME: throw what kind of exception ?
            // TODO: Add .entity()
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).build());
            return;
        }
        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        vitamSession.setRequestId(GUIDFactory.newGUID());
    }
}
