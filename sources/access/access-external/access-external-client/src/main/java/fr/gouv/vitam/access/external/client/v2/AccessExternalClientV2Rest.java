package fr.gouv.vitam.access.external.client.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.dip.DipExportRequest;
import fr.gouv.vitam.common.model.export.dip.DipRequest;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Rest client implementation for Access External
 */
class AccessExternalClientV2Rest extends DefaultClient implements AccessExternalClientV2 {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalClientV2Rest.class);

    private static final String COULD_NOT_PARSE_SERVER_RESPONSE = "Could not parse server response";
    private static final String VITAM_CLIENT_INTERNAL_EXCEPTION = "VitamClientInternalException: ";

    AccessExternalClientV2Rest(AccessExternalClientV2Factory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(VitamContext vitamContext, DipRequest dipRequest)
        throws VitamClientException {
        Response response = null;

        try {
            response = performRequest(HttpMethod.POST, AccessExtAPI.DIP_API, vitamContext.getHeaders(),
                dipRequest, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error(COULD_NOT_PARSE_SERVER_RESPONSE, e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(VITAM_CLIENT_INTERNAL_EXCEPTION, e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(VitamContext vitamContext, DipExportRequest dipExportRequest)
        throws VitamClientException {
        return exportDIP(vitamContext, new DipRequest(dipExportRequest));
    }
}
