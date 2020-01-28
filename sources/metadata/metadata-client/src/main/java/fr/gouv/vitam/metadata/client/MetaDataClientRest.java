/*
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
 */
package fr.gouv.vitam.metadata.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.BatchRulesUpdateInfo;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.api.model.ReclassificationChildNodeExportRequest;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static fr.gouv.vitam.metadata.client.ErrorMessage.INSERT_UNITS_QUERY_NULL;
import static fr.gouv.vitam.metadata.client.ErrorMessage.INVALID_METADATA_VALUE;
import static fr.gouv.vitam.metadata.client.ErrorMessage.INVALID_PARSE_OPERATION;
import static fr.gouv.vitam.metadata.client.ErrorMessage.NOT_FOUND;
import static fr.gouv.vitam.metadata.client.ErrorMessage.SELECT_OBJECT_GROUP_QUERY_NULL;
import static fr.gouv.vitam.metadata.client.ErrorMessage.SELECT_UNITS_QUERY_NULL;
import static fr.gouv.vitam.metadata.client.ErrorMessage.SIZE_TOO_LARGE;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

public class MetaDataClientRest extends DefaultClient implements MetaDataClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataClientRest.class);

    private static final String COMPUTE_GRAPH_URI = "/computegraph";
    private static final String REINDEX_URI = "/reindex";
    private static final String ALIASES_URI = "/alias";

    public MetaDataClientRest(VitamClientFactoryInterface factory) {
        super(factory);
    }

    @Override
    public JsonNode insertUnitBulk(BulkUnitInsertRequest request)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataDocumentSizeException, MetaDataClientServerException {
        try (Response response = make(
            post().withPath("/units/bulk").withBody(request, INSERT_UNITS_QUERY_NULL.getMessage()).withJson())) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode selectUnits(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        try (Response response = make(
            get().withPath("/units").withBody(selectQuery, SELECT_UNITS_QUERY_NULL.getMessage()).withJson())) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (MetaDataNotFoundException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode selectObjectGroups(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        try (Response response = make(
            get().withPath("/objectgroups").withBody(selectQuery, SELECT_OBJECT_GROUP_QUERY_NULL.getMessage())
                .withJson())) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (MetaDataNotFoundException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode selectUnitbyId(JsonNode selectQuery, String unitId)
        throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException {
        if (Strings.isNullOrEmpty(unitId)) {
            throw new InvalidParseOperationException("unitId MUST NOT be empty.");
        }
        try (Response response = make(
            get().withPath("/units/" + unitId).withBody(selectQuery, "selectQuery MUST NOT be empty.").withJson())) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (MetaDataNotFoundException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode selectObjectGrouptbyId(JsonNode selectQuery, String objectGroupId)
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataDocumentSizeException,
        InvalidParseOperationException, MetaDataClientServerException {
        if (Strings.isNullOrEmpty(objectGroupId)) {
            throw new InvalidParseOperationException("objectGroupId may not be empty");
        }
        try (Response response = make(get().withPath("/objectgroups/" + objectGroupId)
            .withBody(selectQuery, SELECT_OBJECT_GROUP_QUERY_NULL.getMessage()).withJson())) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode updateUnitById(JsonNode updateQuery, String unitId)
        throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException,
        MetaDataNotFoundException {
        if (Strings.isNullOrEmpty(unitId)) {
            throw new InvalidParseOperationException("unitId MUST NOT be empty.");
        }
        Response response = null;
        try {
            response = make(put().withJson().withPath("/units/" + unitId)
                .withBody(updateQuery, ErrorMessage.UPDATE_UNITS_QUERY_NULL.getMessage()));
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (InvalidParseOperationException e) {
            JsonNode resp = response.readEntity(JsonNode.class);
            if (resp != null && resp.get("description") != null) {
                throw new InvalidParseOperationException(JsonHandler.unprettyPrint(resp.get("description")));
            }
            throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public JsonNode insertObjectGroup(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataDocumentSizeException, MetaDataClientServerException {
        try (Response response = make(post().withJson().withPath("/objectgroups")
            .withBody(insertQuery, "Insert Request is a mandatory parameter"))) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode insertObjectGroups(List<JsonNode> insertQueries)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataDocumentSizeException, MetaDataClientServerException {
        try (Response response = make(post().withJson().withPath("/objectgroups/bulk")
            .withBody(insertQueries, "Insert Request is a mandatory parameter"))) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }


    @Override
    public void updateObjectGroupById(JsonNode queryUpdate, String objectGroupId)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataExecutionException {
        if (Strings.isNullOrEmpty(objectGroupId)) {
            throw new InvalidParseOperationException("objectGroupId may not be empty");
        }
        try (Response response = make(put().withJson().withPath("/objectgroups/" + objectGroupId)
            .withBody(queryUpdate, ErrorMessage.UPDATE_UNITS_QUERY_NULL.getMessage()))) {
            check(response);
        } catch (MetaDataDocumentSizeException | MetaDataNotFoundException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public List<UnitPerOriginatingAgency> selectAccessionRegisterOnUnitByOperationId(String operationId)
        throws MetaDataClientServerException {
        try (Response response = make(get().withJson().withPath("/accession-registers/units/" + operationId))) {
            check(response);
            RequestResponse<JsonNode> requestResponse = RequestResponse.parseFromResponse(response);
            if (requestResponse.isOk()) {
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;

                List<UnitPerOriginatingAgency> unitPerOriginatingAgencies = new ArrayList<>();
                for (JsonNode jsonNode : requestResponseOK.getResults()) {
                    unitPerOriginatingAgencies
                        .add(JsonHandler.getFromJsonNode(jsonNode, UnitPerOriginatingAgency.class));
                }

                return unitPerOriginatingAgencies;
            }
            VitamError vitamError = (VitamError) requestResponse;
            LOGGER
                .error("find accession register for unit failed, http code is {}, error is {}",
                    vitamError.getCode(),
                    vitamError.getErrors());
            throw new MetaDataClientServerException(vitamError.getDescription());
        } catch (InvalidParseOperationException | VitamClientInternalException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataNotFoundException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public List<ObjectGroupPerOriginatingAgency> selectAccessionRegisterOnObjectByOperationId(String operationId)
        throws MetaDataClientServerException {
        try (Response response = make(get().withJson().withPath("/accession-registers/objects/" + operationId))) {
            check(response);
            RequestResponse<ObjectGroupPerOriginatingAgency> requestResponse =
                RequestResponse.parseFromResponse(response, ObjectGroupPerOriginatingAgency.class);
            if (requestResponse.isOk()) {
                RequestResponseOK<ObjectGroupPerOriginatingAgency> requestResponseOK =
                    (RequestResponseOK<ObjectGroupPerOriginatingAgency>) requestResponse;
                return requestResponseOK.getResults();
            }
            VitamError vitamError = (VitamError) requestResponse;
            LOGGER
                .error("find accession register for object group failed, http code is {}, error is {}",
                    vitamError.getCode(),
                    vitamError.getErrors());
            throw new MetaDataClientServerException(vitamError.getDescription());
        } catch (InvalidParseOperationException | VitamClientInternalException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataNotFoundException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public boolean refreshUnits() throws MetaDataClientServerException {
        try (Response response = make(put().withJsonAccept().withPath("/units"))) {
            check(response);
            return response.getStatus() == OK.getStatusCode();
        } catch (InvalidParseOperationException | VitamClientInternalException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataNotFoundException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public boolean refreshObjectGroups() throws MetaDataClientServerException {
        try (Response response = make(put().withJsonAccept().withPath("/objectgroups"))) {
            check(response);
            return response.getStatus() == OK.getStatusCode();
        } catch (InvalidParseOperationException | VitamClientInternalException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataNotFoundException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode reindex(IndexParameters indexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        try (Response response = make(
            post().withJson().withPath(REINDEX_URI).withBody(indexParam, "The options are mandatory"))) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode switchIndexes(SwitchIndexParameters switchIndexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        try (Response response = make(
            post().withJson().withPath(ALIASES_URI).withBody(switchIndexParam, "The options are mandatory"))) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> getUnitByIdRaw(String unitId) throws VitamClientException {
        Response response = null;
        try {
            response = make(get().withJson().withPath("/raw/units/" + unitId));
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException | InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
            throw new VitamClientException(e);
        } catch (MetaDataNotFoundException e) {
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public RequestResponse<JsonNode> getUnitsByIdsRaw(Collection<String> unitIds) throws VitamClientException {
        try (Response response = make(
            get().withJson().withPath("/raw/units").withBody(unitIds, "The unit ids are mandatory"))) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> getObjectGroupByIdRaw(String objectGroupId) throws VitamClientException {
        Response response = null;
        try {
            response = make(get().withJson().withPath("/raw/objectgroups/" + objectGroupId));
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException | InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
            throw new VitamClientException(e);
        } catch (MetaDataNotFoundException e) {
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public RequestResponse<JsonNode> getObjectGroupsByIdsRaw(Collection<String> objectGroupId)
        throws VitamClientException {
        try (Response response = make(get().withJson().withPath("/raw/objectgroups")
            .withBody(objectGroupId, "The object group ids are mandatory"))) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public GraphComputeResponse computeGraph(JsonNode queryDsl) throws VitamClientException {
        try (Response response = make(
            post().withJson().withPath(COMPUTE_GRAPH_URI).withBody(queryDsl, "The queryDsl is mandatory"))) {
            check(response);
            return response.readEntity(GraphComputeResponse.class);
        } catch (InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public GraphComputeResponse computeGraph(GraphComputeResponse.GraphComputeAction action, Set<String> ids)
        throws VitamClientException {
        try (Response response = make(post().withJson().withPath(COMPUTE_GRAPH_URI + "/" + action.name())
            .withBody(ids, "All params are mandatory"))) {
            check(response);
            return response.readEntity(GraphComputeResponse.class);
        } catch (InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public void exportReclassificationChildNodes(Set<String> ids, String unitsToUpdateJsonLineFileName,
        String objectGroupsToUpdateJsonLineFileName)
        throws VitamClientException, MetaDataExecutionException {
        ReclassificationChildNodeExportRequest reclassificationChildNodeExportRequest =
            new ReclassificationChildNodeExportRequest(ids, unitsToUpdateJsonLineFileName,
                objectGroupsToUpdateJsonLineFileName);
        try (Response response = make(post().withJson().withPath("exportReclassificationChildNodes")
            .withBody(reclassificationChildNodeExportRequest, "All params are mandatory"))) {
            check(response);
        } catch (InvalidParseOperationException | MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataNotFoundException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    @Override
    public void deleteUnitsBulk(Collection<String> listIds)
        throws MetaDataExecutionException, MetaDataClientServerException {
        try (Response response = make(delete().withJson().withPath("/units/bulkDelete")
            .withBody(listIds, INSERT_UNITS_QUERY_NULL.getMessage()))) {
            check(response);
        } catch (InvalidParseOperationException | MetaDataDocumentSizeException | MetaDataNotFoundException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public void deleteObjectGroupBulk(Collection<String> listIds)
        throws MetaDataExecutionException, MetaDataClientServerException {
        try (Response response = make(delete().withJson().withPath("/objectGroups/bulkDelete")
            .withBody(listIds, INSERT_UNITS_QUERY_NULL.getMessage()))) {
            check(response);
        } catch (InvalidParseOperationException | MetaDataDocumentSizeException | MetaDataNotFoundException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    public RequestResponse<JsonNode> updateUnitBulk(JsonNode updateQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataDocumentSizeException, MetaDataClientServerException {
        try (Response response = make(post().withJson().withPath("/units/updatebulk")
            .withBody(updateQuery, INSERT_UNITS_QUERY_NULL.getMessage()))) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> updateUnitsRulesBulk(List<String> unitsIds, RuleActions actions,
        Map<String, DurationData> rulesToDurationData)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataDocumentSizeException, MetaDataClientServerException {
        BatchRulesUpdateInfo requestContext = new BatchRulesUpdateInfo(unitsIds, actions, rulesToDurationData);
        try (Response response = make(post().withJson().withPath("/units/updaterulesbulk")
            .withBody(JsonHandler.toJsonNode(requestContext), INSERT_UNITS_QUERY_NULL.getMessage()))) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode selectUnitsWithInheritedRules(JsonNode selectQuery)
        throws MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException, MetaDataExecutionException {
        try (Response response = make(get().withJson().withPath("/unitsWithInheritedRules")
            .withBody(selectQuery, SELECT_UNITS_QUERY_NULL.getMessage()))) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (MetaDataNotFoundException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    @Override
    public JsonNode createAccessionRegisterSymbolic()
        throws MetaDataClientServerException, MetaDataExecutionException {
        try (Response response = make(post().withJsonAccept().withPath("accession-registers/symbolic"))) {
            check(response);
            return response.readEntity(JsonNode.class);
        } catch (InvalidParseOperationException | MetaDataDocumentSizeException | MetaDataNotFoundException | VitamClientInternalException e) {
            throw new MetaDataClientServerException(e);
        }
    }

    private void check(Response response)
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataDocumentSizeException,
        InvalidParseOperationException,
        MetaDataClientServerException {
        Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
            return;
        }

        switch (status) {
            case INTERNAL_SERVER_ERROR:
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR.getReasonPhrase());
            case NOT_FOUND:
                throw new MetaDataNotFoundException(NOT_FOUND.getMessage());
            case REQUEST_ENTITY_TOO_LARGE:
                throw new MetaDataDocumentSizeException(SIZE_TOO_LARGE.getMessage());
            case BAD_REQUEST:
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION.getMessage());
            case PRECONDITION_FAILED:
                throw new IllegalArgumentException(INVALID_METADATA_VALUE.getMessage());
            default:
                throw new MetaDataClientServerException(status.toString());
        }
    }
}
