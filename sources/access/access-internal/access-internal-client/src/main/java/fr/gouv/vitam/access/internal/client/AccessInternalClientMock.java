/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.access.internal.client;


import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.massupdate.MassUpdateUnitRuleRequest;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Mock client implementation for access
 */
class AccessInternalClientMock extends AbstractMockClient implements AccessInternalClient {

    static final String MOCK_GET_FILE_CONTENT = "Vitam test";

    @Override
    public RequestResponse<JsonNode> selectUnits(JsonNode selectQuery)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"Title\" : \"Archive1\" }}, $projection: {}, $filter: {}}, $result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse<JsonNode> selectUnitbyId(JsonNode sqlQuery, String id)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse<JsonNode> updateUnitbyId(JsonNode updateQuery, String unitId)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"ArchiveUnit1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    @Override
    public RequestResponse<JsonNode> updateUnits(JsonNode updateQuery)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"ArchiveUnit1\" }}, $projection: {}, $filter: {}},$result:[{'#id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}"));
    }

    /**
     * Mass update of archive units rules.
     *
     * @param massUpdateUnitRuleRequest the request to be used to update archive units rules
     * @return null
     */
    @Override
    public RequestResponse<JsonNode> updateUnitsRules(MassUpdateUnitRuleRequest massUpdateUnitRuleRequest) {
        return null;
    }
    
    @Override
    public RequestResponse<JsonNode> bulkAtomicUpdateUnits(JsonNode updateQueries) throws InvalidParseOperationException {
        throw new IllegalStateException("Stop using mocks in production");
    }
    

    @Override
    public RequestResponse<JsonNode> selectObjectbyId(JsonNode selectObjectQuery, String objectId)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(JsonHandler.getFromString(
            "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:" +
                "[{'#id': '1', 'name': 'abcdef', 'creation_date': '2015-07-14T17:07:14Z', 'fmt': 'ftm/123', 'numerical_information': '55.3'}]}"));
    }

    @Override
    public Response getObject(String objectGroupId, String usage, int version, String unitId) {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, StreamUtils.toInputStream(MOCK_GET_FILE_CONTENT),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse<JsonNode> selectOperation(JsonNode select, boolean isSliced, boolean isCrossTenant)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookResults());
    }

    @Override
    public RequestResponse<JsonNode> selectOperationById(String processId, JsonNode queryDsl, boolean isSliced,
        boolean isCrossTenant)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation());
    }

    @Override
    public RequestResponse<JsonNode> selectOperationById(String processId)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation());
    }

    @Override
    public RequestResponse<JsonNode> selectUnitLifeCycleById(String idUnit, JsonNode queryDsl)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation());
    }

    @Override
    public RequestResponse<JsonNode> selectObjectGroupLifeCycleById(String idObject, JsonNode queryDsl)
        throws InvalidParseOperationException {
        return new RequestResponseOK().addResult(ClientMockResultHelper.getLogbookOperation());
    }

    @Override
    public RequestResponse<JsonNode> linkedCheckTraceability(JsonNode query) throws InvalidParseOperationException {
        return ClientMockResultHelper.checkOperationTraceability();
    }

    @Override
    public Response downloadTraceabilityFile(String operationId) {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, StreamUtils.toInputStream(MOCK_GET_FILE_CONTENT),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public Response downloadAccessLogFile(JsonNode params) throws AccessInternalClientServerException {
        //TODO make accesslog.log + InputStream resourceAsStream = getClass().getResourceAsStream("/accesslog.log")
        try (InputStream resourceAsStream = getClass().getResourceAsStream("/objectGroup.xml")) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(resourceAsStream, byteArrayOutputStream);
            return Response.ok().entity(byteArrayOutputStream.toByteArray()).build();
        } catch (IOException e) {
            throw new AccessInternalClientServerException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(JsonNode dslRequest) {
        return new RequestResponseOK<>();
    }

    @Override
    public RequestResponse<JsonNode> exportByUsageFilter(ExportRequest exportRequest) {
        return new RequestResponseOK<>();
    }

    @Override
    public Response findExportByID(String id) {
        return Response.ok().build();
    }

    @Override
    public Response findTransferSIPByID(String id) {
        return Response.ok().build();
    }

    @Override
    public RequestResponse<JsonNode> reclassification(JsonNode reclassificationRequest) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> selectObjects(JsonNode selectQuery)
        throws InvalidParseOperationException {

        JsonNode res;
        try {
            res = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("resultGot.json"));
        } catch (FileNotFoundException e) {
            throw new VitamRuntimeException(e);
        }
        return new RequestResponseOK().addResult(res);
    }

    @Override
    public RequestResponse<JsonNode> selectUnitsWithInheritedRules(JsonNode selectQuery) {
        throw new UnsupportedOperationException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> startEliminationAnalysis(EliminationRequestBody eliminationRequestBody) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> startEliminationAction(EliminationRequestBody eliminationRequestBody) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> startPreservation(PreservationRequest preservationRequest) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> startComputeInheritedRules(JsonNode dslQuery) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> deleteComputeInheritedRules(JsonNode dslQuery) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> startTransferReplyWorkflow(InputStream transferReply) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public RequestResponse<JsonNode> revertUnits(JsonNode queryJson) {
        throw new IllegalStateException("Stop using mocks in production");
    }
}
