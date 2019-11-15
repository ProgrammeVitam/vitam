package fr.gouv.vitam.metadata.client;

import java.io.FileNotFoundException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;

/**
 * Mock client implementation for metadata
 */
public class MetaDataClientMock extends AbstractMockClient implements MetaDataClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataClientMock.class);

    @Override
    public JsonNode insertUnitBulk(BulkUnitInsertRequest request) throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        ArrayNode arrayNode = JsonHandler.createArrayNode();
        JsonNode jsonNode = ClientMockResultHelper.getMetaDataResult().toJsonNode();
        arrayNode.add(jsonNode);
        return arrayNode;
    }

    @Override
    public JsonNode selectUnits(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        JsonNode res = null;
        try {
            res = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("result.json"));
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        }
        return res;
    }

    @Override
    public JsonNode selectUnitbyId(JsonNode selectQuery, String unitId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        JsonNode res = null;
        try {
            res = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("result.json"));
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        }
        return res;
    }

    @Override
    public JsonNode selectObjectGrouptbyId(JsonNode selectQuery, String objectGroupId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetadataInvalidSelectException, MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult().toJsonNode();
    }

    @Override
    public JsonNode updateUnitById(JsonNode updateQuery, String unitId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult().toJsonNode();
    }

    @Override
    public JsonNode insertObjectGroup(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult().toJsonNode();
    }

    @Override
    public JsonNode insertObjectGroups(List<JsonNode> insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult().toJsonNode();
    }

    @Override
    public List<UnitPerOriginatingAgency> selectAccessionRegisterOnUnitByOperationId(String operationId)
        throws MetaDataClientServerException {
        return new ArrayList<>();
    }

    @Override
    public List<ObjectGroupPerOriginatingAgency> selectAccessionRegisterOnObjectByOperationId(String operationId)
        throws MetaDataClientServerException {
        return new ArrayList<>();
    }

    @Override
    public JsonNode selectObjectGroups(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult().toJsonNode();
    }

    @Override
    public void updateObjectGroupById(JsonNode objectGroup, String objectGroupId) {
        // Empty
    }

    @Override
    public boolean refreshUnits() {
        return true;
    }

    @Override
    public boolean refreshObjectGroups() {
        return true;
    }

    @Override
    public JsonNode reindex(IndexParameters indexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        return ClientMockResultHelper.getReindexationInfo().toJsonNode();
    }

    @Override
    public JsonNode switchIndexes(SwitchIndexParameters switchIndexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        return ClientMockResultHelper.getSwitchIndexesInfo().toJsonNode();
    }

    @Override
    public RequestResponse<JsonNode> getUnitByIdRaw(String unitId) throws VitamClientException {
        try {
            return ClientMockResultHelper.getArchiveUnitRawResult();
        } catch (InvalidParseOperationException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> getObjectGroupByIdRaw(String objectGroupId) throws VitamClientException {
        try {
            return ClientMockResultHelper.getObjectGroupRawResult();
        } catch (InvalidParseOperationException e) {
            throw new VitamClientException(e);
        }
    }

    public RequestResponse<JsonNode> getUnitsByIdsRaw(Collection<String> unitIds) {
        throw new UnsupportedOperationException("No need for mocks");
    }

    @Override
    public RequestResponse<JsonNode> getObjectGroupsByIdsRaw(Collection<String> objectGroupIds) {
        throw new UnsupportedOperationException("No need for mocks");
    }

    @Override
    public GraphComputeResponse computeGraph(JsonNode queryDsl) throws VitamClientException {
        return new GraphComputeResponse(3, 3);
    }

    @Override
    public GraphComputeResponse computeGraph(GraphComputeResponse.GraphComputeAction action, Set<String> ids)
        throws VitamClientException {
        return new GraphComputeResponse(3, 3);
    }

    @Override 
    public RequestResponse<JsonNode> updateUnitBulk(JsonNode updateQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataDocumentSizeException, MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult();
    }

    @Override
    public RequestResponse<JsonNode> updateUnitsRulesBulk(List<String> unitsIds, RuleActions actions, Map<String, DurationData> rulesToDurationData) throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException, MetaDataDocumentSizeException, MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult();
    }

    @Override
    public void exportReclassificationChildNodes(Set<String> ids, String unitsToUpdateJsonLineFileName,
        String objectGroupsToUpdateJsonLineFileName) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public JsonNode selectUnitsWithInheritedRules(JsonNode selectQuery) {
        throw new UnsupportedOperationException("Stop using mocks in production");
    }

    @Override
    public JsonNode createAccessionRegisterSymbolic() {
        throw new RuntimeException("Do not use this");
    }

    @Override
    public void deleteUnitsBulk(Collection<String> listIds) {
        throw new IllegalStateException("Stop using mocks in production");
    }

    @Override
    public void deleteObjectGroupBulk(Collection<String> listIds) {
        throw new IllegalStateException("Stop using mocks in production");
    }
}
