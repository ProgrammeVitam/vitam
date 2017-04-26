/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.server;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

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
    public static void putVitamIdFromHeaderInSession(MultivaluedMap<String, String> requestHeaders, Context ctx) {
        try {

            // TODO: find a better check ; we should detect and act accordingly with multiple incoming headers
            String requestId = requestHeaders.getFirst(GlobalDataRest.X_REQUEST_ID);
            String headerTenantId = requestHeaders.getFirst(GlobalDataRest.X_TENANT_ID);
            Integer tenantId = null;
            String contractId = requestHeaders.getFirst(GlobalDataRest.X_ACCESS_CONTRAT_ID);

            /*
             * Note : jetty seems NOT to correctly unserialize multiple headers declaration in only one header, with
             * values separated by commas Example : X-REQUEST-ID: header-1,header-2-should-not-be-take Note : Cf. the
             * last paragraph in https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2 TODO: Jetty bug ?
             */
            // KWA TODO: beurk.
            if (requestId != null) {
                requestId = requestId.split(",")[0];
            }
            
            if (headerTenantId != null) {
                headerTenantId = headerTenantId.split(",")[0];
                tenantId = Integer.parseInt(headerTenantId);
            }
            
            if (contractId != null) {
                contractId = contractId.split(",")[0];
            }
            // KWA TODO: end beurk.
            final VitamSession vitamSession = VitamThreadUtils.getVitamSession();

            if (vitamSession.getRequestId() != null && !vitamSession.getRequestId().equals(requestId)) {
                LOGGER.info(
                    "Note : the requestId stored in session was not empty and different from the received " +
                        "requestId before {} handling ! Some cleanup must have failed... " +
                        "Old requestId will be discarded in session.",
                    ctx);
            }
            
            if (vitamSession.getTenantId() != null && !vitamSession.getTenantId().equals(tenantId)) {
                LOGGER.info(
                    "Note : the tenantId stored in session was not empty and different from the received " +
                        "tenantId before {} handling ! Some cleanup must have failed... " +
                        "Old tenantId will be discarded in session.",
                    ctx);
            }
            
            if (vitamSession.getContractId() != null && !vitamSession.getContractId().equals(contractId)) {
                LOGGER.info(
                    "Note : the contratId stored in session was not empty and different from the received " +
                        "contratId before {} handling ! Some cleanup must have failed... " +
                        "Old contratId will be discarded in session.",
                    ctx);
            }

            vitamSession.setRequestId(requestId);
            vitamSession.setTenantId(tenantId);
            vitamSession.setContractId(contractId);

            if (requestId != null) {
                LOGGER.debug("Got requestId {} from {} headers ; setting it in the current VitamSession", requestId,
                    ctx);
            } else {
                LOGGER.debug("No requestId found in {} ; setting it as empty in the current VitamSession", ctx);
            }
            
            if (tenantId != null) {
                LOGGER.debug("Got tenantId {} from {} headers ; setting it in the current VitamSession", tenantId,
                    ctx);
            } else {
                LOGGER.debug("No tenantId found in {} ; setting it as empty in the current VitamSession", ctx);
            }
            
            if (contractId != null) {
                LOGGER.debug("Got contractId {} from {} headers ; setting it in the current VitamSession", contractId,
                    ctx);
            } else {
                LOGGER.debug("No contractId found in {} ; setting it as empty in the current VitamSession", ctx);
            }
        } catch (final VitamThreadAccessException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            LOGGER.warn(
                "Got an exception while trying to set the headers in the current session ; exception was : {}",
                requestHeaders, e.getMessage());
            // the processing should not be interrupted by this exception
        }
    }

    /**
     * Retrieves the vitam id from the VitamSession and add a X-TENANT-ID header
     *
     * @param headers List of target HTTP headers ; required header will be added to this list.
     * @param ctx Context, or rather http message type (request or response)
     */
    public static void putVitamIdFromSessionInHeader(MultivaluedMap<String, Object> headers, Context ctx, int statusCode) {
        try {
            final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
            final Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            final String contractId = VitamThreadUtils.getVitamSession().getContractId();
            
            if (requestId != null) {
                if (headers.containsKey(GlobalDataRest.X_REQUEST_ID)) {
                    LOGGER.warn("{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_REQUEST_ID, ctx);
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_REQUEST_ID, requestId);
                    LOGGER.debug("RequestId {} found in session and set in the {} header.", requestId, ctx);
                }
            } else {
                LOGGER.warn("No RequestId found in session (somebody should have set it) ! ");
                
                if (ctx.equals(Context.RESPONSE) && statusCode >= Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                    String newRequestId = GUIDFactory.newGUID().toString();

                    if (headers.containsKey(GlobalDataRest.X_REQUEST_ID)) {
                        LOGGER.warn("X_REQUEST_ID  header was already present in the headers");
                    } else {
                        headers.add(GlobalDataRest.X_REQUEST_ID, newRequestId);
                    }
                }
            }
            
            if (tenantId != null) {
                if (headers.containsKey(GlobalDataRest.X_TENANT_ID)) {
                    LOGGER.warn("{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_TENANT_ID, ctx);
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
                    LOGGER.debug("tenantId {} found in session and set in the {} header.", tenantId, ctx);
                }
            } else {
                LOGGER.warn(
                    "No tenantId found in session (somebody should have set it) ! " +
                        "{} header will not be set in the http {}.",
                    GlobalDataRest.X_TENANT_ID, ctx);
            }
            
            if (contractId != null) {
                if (headers.containsKey(GlobalDataRest.X_ACCESS_CONTRAT_ID)) {
                    LOGGER.warn("{} header was already present in the headers of the {} ; this header will be kept.",
                        GlobalDataRest.X_ACCESS_CONTRAT_ID, ctx);
                    // TODO: is it really the best way to react to this situation ?
                } else {
                    headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractId);
                    LOGGER.debug("contractId {} found in session and set in the {} header.", contractId, ctx);
                }
            } else {
                LOGGER.warn(
                    "No contract id found in session (somebody should have set it) ! " +
                        "{} header will not be set in the http {}.",
                    GlobalDataRest.X_ACCESS_CONTRAT_ID, ctx);
            }
        } catch (final VitamThreadAccessException e) {
            LOGGER.warn(
                "Got an exception while trying to get the headers from the current session ; exception was : {}",
                e.getMessage());
            // the processing should not be interrupted by this exception
        }
    }
}
