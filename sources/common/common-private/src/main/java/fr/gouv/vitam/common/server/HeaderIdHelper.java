/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.server;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

/**
 * HeaderId Helper, check and put header values
 */
public class HeaderIdHelper {

    /**
     * Context of request
     */
    public enum Context {
        REQUEST,
        RESPONSE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HeaderIdHelper.class);

    /**
     * Helper class, so not instanciable.
     */
    private HeaderIdHelper() {
        throw new UnsupportedOperationException("Helper class");
    }

    /**
     * Extracts the vitam id from the headers to save it through the VitamSession
     *
     * @param requestHeaders Complete list of HTTP message headers ; will not be changed.
     * @param ctx Context, or rather http message type (request or response)
     */
    public static void putVitamIdFromExternalHeaderInSession(
        MultivaluedMap<String, String> requestHeaders,
        Context ctx
    ) {
        try {
            extractContractIdFromHeaders(requestHeaders, ctx);
            extractTenantIdFromHeaders(requestHeaders, ctx);
            extractApplicationSessionIdFromHeaders(requestHeaders, ctx);
        } catch (final VitamThreadAccessException e) {
            LOGGER.warn(
                "Got an exception while trying to set the headers in the current session {}; exception was : {}",
                requestHeaders,
                e.getMessage()
            );
            // the processing should not be interrupted by this exception
        }
    }

    /**
     * Extracts the vitam id from the headers to save it through the VitamSession
     *
     * @param requestHeaders Complete list of HTTP message headers ; will not be changed.
     * @param ctx Context, or rather http message type (request or response)
     */
    public static void putVitamIdFromHeaderInSession(MultivaluedMap<String, String> requestHeaders, Context ctx) {
        try {
            extractContractIdFromHeaders(requestHeaders, ctx);
            extractContextIdFromHeaders(requestHeaders, ctx);
            extractRequestIdFromHeaders(requestHeaders, ctx);
            extractTenantIdFromHeaders(requestHeaders, ctx);
            extractApplicationSessionIdFromHeaders(requestHeaders, ctx);
            extractPersonalCertificateFromHeaders(requestHeaders, ctx);
        } catch (final VitamThreadAccessException e) {
            LOGGER.debug(
                "Got an exception while trying to set the headers in the current session {}; exception was : {}",
                requestHeaders,
                e.getMessage()
            );
            // the processing should not be interrupted by this exception
        }
    }

    private static void extractRequestIdFromHeaders(MultivaluedMap<String, String> requestHeaders, Context ctx) {
        String requestId = getHeaderString(requestHeaders, GlobalDataRest.X_REQUEST_ID);

        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        if (vitamSession.getRequestId() != null && !vitamSession.getRequestId().equals(requestId)) {
            LOGGER.info(
                "Note : the requestId stored in session was not empty and different from the received " +
                "requestId before {} handling ! Some cleanup must have failed... " +
                "Old requestId will be discarded in session.",
                ctx
            );
        }

        vitamSession.setRequestId(requestId);

        if (requestId != null) {
            LOGGER.debug("Got requestId {} from {} headers ; setting it in the current VitamSession", requestId, ctx);
        } else {
            LOGGER.debug("No requestId found in {} ; setting it as empty in the current VitamSession", ctx);
        }
    }

    private static void extractContractIdFromHeaders(MultivaluedMap<String, String> requestHeaders, Context ctx) {
        String contractId = getHeaderString(requestHeaders, GlobalDataRest.X_ACCESS_CONTRAT_ID);

        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        if (vitamSession.getContractId() != null && !vitamSession.getContractId().equals(contractId)) {
            LOGGER.info(
                "Note : the contractId stored in session was not empty and different from the received " +
                "contractId before {} handling ! Some cleanup must have failed... " +
                "Old contractId will be discarded in session.",
                ctx
            );
        }

        vitamSession.setContractId(contractId);

        if (contractId != null) {
            LOGGER.debug("Got contractId {} from {} headers ; setting it in the current VitamSession", contractId, ctx);
        } else {
            LOGGER.debug("No contractId found in {} ; setting it as empty in the current VitamSession", ctx);
        }
    }

    private static void extractContextIdFromHeaders(MultivaluedMap<String, String> requestHeaders, Context ctx) {
        String contextId = getHeaderString(requestHeaders, GlobalDataRest.X_SECURITY_CONTEXT_ID);

        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        if (vitamSession.getContextId() != null && !vitamSession.getContextId().equals(contextId)) {
            LOGGER.info(
                "Note : the contextId stored in session was not empty and different from the received " +
                "contextId before {} handling ! Some cleanup must have failed... " +
                "Old contextId will be discarded in session.",
                ctx
            );
        }

        vitamSession.setContextId(contextId);

        if (contextId != null) {
            LOGGER.debug("Got contextId {} from {} headers ; setting it in the current VitamSession", contextId, ctx);
        } else {
            LOGGER.debug("No contextId found in {} ; setting it as empty in the current VitamSession", ctx);
        }
    }

    private static void extractPersonalCertificateFromHeaders(
        MultivaluedMap<String, String> requestHeaders,
        Context ctx
    ) {
        String personalCertificate = getHeaderString(requestHeaders, GlobalDataRest.X_PERSONAL_CERTIFICATE);

        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        if (
            vitamSession.getPersonalCertificate() != null &&
            !vitamSession.getPersonalCertificate().equals(personalCertificate)
        ) {
            LOGGER.info(
                "Note : the personalCertificate stored in session was not empty and different from the received " +
                "personalCertificate before {} handling ! Some cleanup must have failed... " +
                "Old personalCertificate will be discarded in session.",
                ctx
            );
        }

        vitamSession.setPersonalCertificate(personalCertificate);

        if (personalCertificate != null) {
            LOGGER.debug(
                "Got personalCertificate {} from {} headers ; setting it in the current VitamSession",
                personalCertificate,
                ctx
            );
        } else {
            LOGGER.debug("No personalCertificate found in {} ; setting it as empty in the current VitamSession", ctx);
        }
    }

    private static void extractTenantIdFromHeaders(MultivaluedMap<String, String> requestHeaders, Context ctx) {
        // TODO: find a better check ; we should detect and act accordingly with multiple incoming headers
        String headerTenantId = getHeaderString(requestHeaders, GlobalDataRest.X_TENANT_ID);
        Integer tenantId = null;
        if (headerTenantId != null) {
            tenantId = Integer.parseInt(headerTenantId);
        }

        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        if (vitamSession.getTenantId() != null && !vitamSession.getTenantId().equals(tenantId)) {
            LOGGER.info(
                "Note : the tenantId stored in session was not empty and different from the received " +
                "tenantId before {} handling ! Some cleanup must have failed... " +
                "Old tenantId will be discarded in session.",
                ctx
            );
        }

        vitamSession.setTenantId(tenantId);

        if (tenantId != null) {
            LOGGER.debug("Got tenantId {} from {} headers ; setting it in the current VitamSession", tenantId, ctx);
        } else {
            LOGGER.debug("No tenantId found in {} ; setting it as empty in the current VitamSession", ctx);
        }
    }

    private static void extractApplicationSessionIdFromHeaders(
        MultivaluedMap<String, String> requestHeaders,
        Context ctx
    ) {
        String applicationSessionId = getHeaderString(requestHeaders, GlobalDataRest.X_APPLICATION_ID);

        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        if (
            vitamSession.getApplicationSessionId() != null &&
            !vitamSession.getApplicationSessionId().equals(applicationSessionId)
        ) {
            LOGGER.info(
                "Note : the applicationSessionId stored in session was not empty and different from the received " +
                "applicationSessionId before {} handling ! Some cleanup must have failed... " +
                "Old applicationSessionId will be discarded in session.",
                ctx
            );
        }

        vitamSession.setApplicationSessionId(applicationSessionId);

        if (applicationSessionId != null) {
            LOGGER.debug(
                "Got applicationSessionId {} from {} headers ; setting it in the current VitamSession",
                applicationSessionId,
                ctx
            );
        } else {
            LOGGER.debug("No applicationSessionId found in {} ; setting it as empty in the current VitamSession", ctx);
        }
    }

    public static String getHeaderString(MultivaluedMap<String, String> requestHeaders, String headerName) {
        /*
         * Note : jetty seems NOT to correctly unserialize multiple headers declaration in only one header, with values
         * separated by commas Example : X-Request-Id: header-1,header-2-should-not-be-take Note : Cf. the last
         * paragraph in https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2 TODO: Jetty bug ?
         */
        // KWA TODO: beurk.

        String headerValue = requestHeaders.getFirst(headerName);
        if (headerValue != null) {
            headerValue = headerValue.split(",")[0];
        }
        return headerValue;
    }

    /**
     * Retrieves the vitam id from the VitamSession and add a X-TENANT-ID header
     *
     * @param headers List of target HTTP headers ; required header will be added to this list.
     * @param ctx Context, or rather http message type (request or response)
     * @param statusCode the status code
     */
    public static void putVitamIdFromSessionInExternalHeader(
        MultivaluedMap<String, Object> headers,
        Context ctx,
        int statusCode
    ) {
        try {
            final String requestId = VitamThreadUtils.getVitamSession().getRequestId();

            if (requestId != null) {
                if (headers.containsKey(GlobalDataRest.X_REQUEST_ID)) {
                    LOGGER.info(
                        "{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_REQUEST_ID,
                        ctx
                    );
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_REQUEST_ID, requestId);
                    LOGGER.debug("RequestId {} found in session and set in the {} header.", requestId, ctx);
                }
            } else {
                // TODO this should be improved : warn log and consul calls to be handled in a better way
                LOGGER.info("No RequestId found in session (somebody should have set it) ! ");
                if (ctx.equals(Context.RESPONSE) && statusCode >= Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                    String newRequestId = GUIDFactory.newGUID().toString();

                    if (headers.containsKey(GlobalDataRest.X_REQUEST_ID)) {
                        LOGGER.info("X_REQUEST_ID  header was already present in the headers");
                    } else {
                        headers.add(GlobalDataRest.X_REQUEST_ID, newRequestId);
                    }
                }
            }
        } catch (final VitamThreadAccessException e) {
            LOGGER.warn(
                "Got an exception while trying to get the headers from the current session ; exception was : {}",
                e.getMessage()
            );
            // the processing should not be interrupted by this exception
        }
    }

    /**
     * Retrieves the vitam id from the VitamSession and add a X-TENANT-ID header
     *
     * @param headers List of target HTTP headers ; required header will be added to this list.
     * @param ctx Context, or rather http message type (request or response)
     * @param statusCode status code
     */
    public static void putVitamIdFromSessionInHeader(
        MultivaluedMap<String, Object> headers,
        Context ctx,
        int statusCode
    ) {
        try {
            final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
            final Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            final String contractId = VitamThreadUtils.getVitamSession().getContractId();
            final String contextId = VitamThreadUtils.getVitamSession().getContextId();
            final String personalCertificate = VitamThreadUtils.getVitamSession().getPersonalCertificate();
            final String applicationSessionId = VitamThreadUtils.getVitamSession().getApplicationSessionId();

            if (requestId != null) {
                if (headers.containsKey(GlobalDataRest.X_REQUEST_ID)) {
                    LOGGER.info(
                        "{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_REQUEST_ID,
                        ctx
                    );
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_REQUEST_ID, requestId);
                    LOGGER.debug("RequestId {} found in session and set in the {} header.", requestId, ctx);
                }
            } else {
                // TODO this should be improved : warn log and consul calls to be handled in a better way
                LOGGER.info("No RequestId found in session (somebody should have set it) ! ");
                if (ctx.equals(Context.RESPONSE) && statusCode >= Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                    String newRequestId = GUIDFactory.newGUID().toString();

                    if (headers.containsKey(GlobalDataRest.X_REQUEST_ID)) {
                        LOGGER.info("X_REQUEST_ID  header was already present in the headers");
                    } else {
                        headers.add(GlobalDataRest.X_REQUEST_ID, newRequestId);
                    }
                }
            }

            if (tenantId != null) {
                if (headers.containsKey(GlobalDataRest.X_TENANT_ID)) {
                    LOGGER.info(
                        "{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_TENANT_ID,
                        ctx
                    );
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
                    LOGGER.debug("tenantId {} found in session and set in the {} header.", tenantId, ctx);
                }
            } else {
                LOGGER.debug(
                    "No tenantId found in session (somebody should have set it) ! " +
                    "{} header will not be set in the http {}.",
                    GlobalDataRest.X_TENANT_ID,
                    ctx
                );
            }

            if (contractId != null) {
                if (headers.containsKey(GlobalDataRest.X_ACCESS_CONTRAT_ID)) {
                    LOGGER.info(
                        "{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_ACCESS_CONTRAT_ID,
                        ctx
                    );
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractId);
                    LOGGER.debug("contractId {} found in session and set in the {} header.", contractId, ctx);
                }
            } else {
                // Not everywhere useful
                LOGGER.debug(
                    "No contract id found in session (somebody should have set it) ! " +
                    "{} header will not be set in the http {}.",
                    GlobalDataRest.X_ACCESS_CONTRAT_ID,
                    ctx
                );
            }

            if (contextId != null) {
                if (headers.containsKey(GlobalDataRest.X_SECURITY_CONTEXT_ID)) {
                    LOGGER.info(
                        "{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_SECURITY_CONTEXT_ID,
                        ctx
                    );
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_SECURITY_CONTEXT_ID, contextId);
                    LOGGER.debug("contextId {} found in session and set in the {} header.", contextId, ctx);
                }
            } else {
                // Not everywhere useful
                LOGGER.debug(
                    "No contextId found in session (somebody should have set it) ! " +
                    "{} header will not be set in the http {}.",
                    GlobalDataRest.X_SECURITY_CONTEXT_ID,
                    ctx
                );
            }

            if (personalCertificate != null) {
                if (headers.containsKey(GlobalDataRest.X_PERSONAL_CERTIFICATE)) {
                    LOGGER.info(
                        "{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_PERSONAL_CERTIFICATE,
                        ctx
                    );
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_PERSONAL_CERTIFICATE, personalCertificate);
                    LOGGER.debug(
                        "personalCertificate {} found in session and set in the {} header.",
                        personalCertificate,
                        ctx
                    );
                }
            } else {
                // Not everywhere useful
                LOGGER.debug(
                    "No contextId found in session (somebody should have set it) ! " +
                    "{} header will not be set in the http {}.",
                    GlobalDataRest.X_SECURITY_CONTEXT_ID,
                    ctx
                );
            }

            if (applicationSessionId != null) {
                if (headers.containsKey(GlobalDataRest.X_APPLICATION_ID)) {
                    LOGGER.info(
                        "{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_APPLICATION_ID,
                        ctx
                    );
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_APPLICATION_ID, applicationSessionId);
                    LOGGER.debug(
                        "applicationSessionId {} found in session and set in the {} header.",
                        applicationSessionId,
                        ctx
                    );
                }
            } else {
                // Not everywhere useful
                LOGGER.debug(
                    "No applicationSessionId found in session (somebody should have set it) ! " +
                    "{} header will not be set in the http {}.",
                    GlobalDataRest.X_APPLICATION_ID,
                    ctx
                );
            }
        } catch (final VitamThreadAccessException e) {
            LOGGER.warn(
                "Got an exception while trying to get the headers from the current session ; exception was : {}",
                e.getMessage()
            );
            // the processing should not be interrupted by this exception
        }
    }
}
