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
package fr.gouv.vitam.access.internal.client;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import fr.gouv.vitam.common.stream.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import org.apache.commons.io.IOUtils;


/**
 * Mock client implementation for access
 */
class AccessInternalClientMock extends AbstractMockClient implements AccessInternalClient {

    static final String MOCK_GET_FILE_CONTENT = "Vitam test";

    @Override
    public RequestResponse<JsonNode> selectUnits(JsonNode selectQuery)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"Title\" : \"Archive1\" }}, $projection: {}, $filter: {}}, $result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse<JsonNode> selectUnitbyId(JsonNode sqlQuery, String id)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse<JsonNode> updateUnitbyId(JsonNode updateQuery, String unitId)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, NoWritingPermissionException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"ArchiveUnit1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse<JsonNode> selectObjectbyId(JsonNode selectObjectQuery, String objectId)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:" +
                "[{'#id': '1', 'name': 'abcdef', 'creation_date': '2015-07-14T17:07:14Z', 'fmt': 'ftm/123', 'numerical_information': '55.3'}]}"));

    }

    @Override
    public Response getObject(String objectGroupId, String usage, int version)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, StreamUtils.toInputStream(MOCK_GET_FILE_CONTENT),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }


    @Override
    public RequestResponse<JsonNode> selectOperation(JsonNode select)
        throws LogbookClientException, InvalidParseOperationException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookResults());
    }

    @Override
    public RequestResponse<JsonNode> selectOperationById(String processId, JsonNode queryDsl)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation());
    }

    @Override
    public RequestResponse<JsonNode> selectUnitLifeCycleById(String idUnit, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation());
    }

    @Override
    public RequestResponse<JsonNode> selectUnitLifeCycle(JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation());
    }

    @Override
    public RequestResponse<JsonNode> selectObjectGroupLifeCycleById(String idObject, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation());
    }

    @SuppressWarnings("unchecked")
    @Override
    public RequestResponse<JsonNode> checkTraceabilityOperation(JsonNode query) throws InvalidParseOperationException {
        return ClientMockResultHelper.checkOperationTraceability();
    }

    @Override
    public Response downloadTraceabilityFile(String operationId)
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, StreamUtils.toInputStream(MOCK_GET_FILE_CONTENT),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(JsonNode queryJson) {
        return new RequestResponseOK<>();
    }

    @Override
    public Response findDIPByID(String id) throws AccessInternalClientServerException {
        return Response.ok().build();
    }
}
