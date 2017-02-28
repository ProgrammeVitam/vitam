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
package fr.gouv.vitam.ingest.internal.client;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

/**
 * Mock client implementation for Ingest Internal
 */
public class IngestInternalClientMock extends AbstractMockClient implements IngestInternalClient {

    private static final String PARAMS_CANNOT_BE_NULL = "Params cannot be null";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalClientMock.class);
    public static final String MOCK_INGEST_INTERNAL_RESPONSE_STREAM = "VITAM-Ingest Internal Client Mock Response";
    public static final String ID = "identifier1";
    protected StatusCode globalStatus;

    @Override
    public Response upload(InputStream inputStream, MediaType archiveType, String contextId) throws VitamException {
        ParametersChecker.checkParameter(PARAMS_CANNOT_BE_NULL, inputStream, archiveType);
        StreamUtils.closeSilently(inputStream);

        LOGGER.debug("Post SIP");

        return new AbstractMockClient.FakeInboundResponse(Status.OK,
            IOUtils.toInputStream(MOCK_INGEST_INTERNAL_RESPONSE_STREAM),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public Response uploadInitialLogbook(Iterable<LogbookOperationParameters> logbookParametersList)
        throws VitamException {
        ParametersChecker.checkParameter(PARAMS_CANNOT_BE_NULL, logbookParametersList);
        return Response.status(Status.CREATED).build();
    }

    @Override
    public void uploadFinalLogbook(Iterable<LogbookOperationParameters> logbookParametersList)
        throws VitamClientException {
        ParametersChecker.checkParameter(PARAMS_CANNOT_BE_NULL, logbookParametersList);

    }

    @Override
    public Response downloadObjectAsync(String objectId, IngestCollection type) {
        return ClientMockResultHelper.getObjectStream();
    }

    @Override
    public Response storeATR(GUID guid, InputStream input) throws VitamClientException {
        return ClientMockResultHelper.getObjectStream();
    }

    public ItemStatus getOperationProcessStatus(String id) throws VitamClientException {
        return new ItemStatus(ID);
    }

    @Override
    public ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query) throws VitamClientException {
        return new ItemStatus(ID);
    }

    @Override
    public Response cancelOperationProcessExecution(String id) throws VitamClientException {
        // return new ItemStatus(ID);
        return Response.status(Status.OK).build();
    }

    @Override
    public Response updateOperationActionProcess(String actionId, String operationId) throws VitamClientException {
        return Response.status(Status.OK).build();
    }

    @Override
    public Response executeOperationProcess(String operationId, String workflow, String contextId, String actionId)
        throws VitamClientException {
        return Response.status(Status.OK).build();
    }

    @Override
    public Response initWorkFlow(String contextId) throws VitamClientException, VitamException {
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
    public Response listOperationsDetails() throws VitamClientInternalException {
        return Response.status(Status.OK).build();
    }


}
