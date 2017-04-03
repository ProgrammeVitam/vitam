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
package fr.gouv.vitam.ingest.external.client;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;

/**
 * Mock client implementation for IngestExternal
 */
class IngestExternalClientMock extends AbstractMockClient implements IngestExternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientMock.class);
    private static final String FAKE_X_REQUEST_ID = GUIDFactory.newRequestIdGUID(0).getId();
    public static final String MOCK_INGEST_EXTERNAL_RESPONSE_STREAM = "VITAM-Ingest External Client Mock Response";
    final int TENANT_ID = 0;
    public static final String ID = "identifier1";
    protected StatusCode globalStatus;

    @Override
    public Response upload(InputStream stream, Integer tenantId, String contextId, String action)
        throws IngestExternalException {
        if (stream == null) {
            throw new IngestExternalException("stream is null");
        }
        StreamUtils.closeSilently(stream);

        return new AbstractMockClient.FakeInboundResponse(Status.OK,
            IOUtils.toInputStream(MOCK_INGEST_EXTERNAL_RESPONSE_STREAM),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, getDefaultHeaders(FAKE_X_REQUEST_ID, tenantId));
    }

    /**
     * Generate the default header map
     *
     * @param requestId fake x-request-id
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String requestId, Integer tenantId) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, requestId);
    	headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        return headers;
    }

    @Override
    public Response downloadObjectAsync(String objectId, IngestCollection type, Integer tenantId) throws IngestExternalException {
        return ClientMockResultHelper.getObjectStream();
    }

    @Override
    public ItemStatus getOperationProcessStatus(String id)
        throws VitamClientException, InternalServerException, BadRequestException {
        ItemStatus pwork = null;
        try {
            pwork = ClientMockResultHelper.getItemStatus(id);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new BadRequestException(e.getMessage(), e);
        }
        return pwork;
    }

    @Override
    public ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query)
        throws VitamClientException, InternalServerException, BadRequestException {
        return new ItemStatus(ID);
    }

    @Override
    public Response cancelOperationProcessExecution(String id)
        throws InternalServerException, BadRequestException, VitamClientException {
        // return new ItemStatus(ID);
        return Response.status(Status.OK).build();
    }

    @Override
    public Response updateOperationActionProcess(String actionId, String id)
        throws InternalServerException, BadRequestException, VitamClientException {
        return Response.status(Status.OK).build();
    }

    @Override
    public Response executeOperationProcess(String operationId, String workflow, String contextId, String actionId)
        throws InternalServerException, BadRequestException, VitamClientException {
        return Response.status(Status.OK).build();
    }

    @Override
    public Response initWorkFlow(String contextId) throws VitamException {
        return Response.status(Status.OK).build();
    }

    @Override
    public ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow)
        throws InternalServerException, BadRequestException, VitamClientException {
        return new ItemStatus(ID);
    }

    @Override
    public Response initVitamProcess(String contextId, String container, String workflow)
        throws InternalServerException, VitamClientException, BadRequestException {
        return Response.status(Status.OK).build();
    }

    @Override
    public Response listOperationsDetails() throws VitamClientException {
        return Response.status(Status.OK).build();
    }


}
