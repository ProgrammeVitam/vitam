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
package fr.gouv.vitam.metadata.core;


import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.common.json.JsonHandler.toArrayList;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.OPS;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ORIGINATING_AGENCIES;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.QUALIFIERS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.MongoWriteException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.FacetResult;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.metadata.api.MetaData;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbVarNameAdapter;
import fr.gouv.vitam.metadata.core.database.collections.Result;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.utils.MetadataJsonResponseUtils;
import org.bson.Document;

/**
 * MetaDataImpl implements a MetaData interface
 */
public class MetaDataImpl implements MetaData {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MetaDataImpl.class);
    private static final String REQUEST_IS_NULL = "Request select is null or is empty";
    private static final MongoDbVarNameAdapter DEFAULT_VARNAME_ADAPTER = new MongoDbVarNameAdapter();
    public static final String ORIGINATING_AGENCY = "originatingAgency";
    public static final String OPI = "opi";
    public static final String QUALIFIER_VERSION_OPI = "qualifierVersionOpi";
    public static final String TOTAL_SIZE = "totalSize";
    public static final String TOTAL_OBJECT = "totalObject";
    public static final String LIST_GOT = "listGOT";
    public static final String TOTAL_GOT = "totalGOT";
    public static final String COUNT = "count";
    private final MongoDbAccessMetadataImpl mongoDbAccess;

    /**
     * @param mongoDbAccess
     */
    public MetaDataImpl(MongoDbAccessMetadataImpl mongoDbAccess) {
        this.mongoDbAccess = mongoDbAccess;
    }

    /**
     * @return the MongoDbAccessMetadataImpl
     */
    public MongoDbAccessMetadataImpl getMongoDbAccess() {
        return mongoDbAccess;
    }

    /**
     * Get a new MetaDataImpl instance
     *
     * @param mongoDbAccessMetadata
     * @return a new instance of MetaDataImpl
     */
    public static MetaData newMetadata(MongoDbAccessMetadataImpl mongoDbAccessMetadata) {
        return new MetaDataImpl(mongoDbAccessMetadata);
    }

    @Override
    public void insertUnit(JsonNode insertRequest)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataAlreadyExistException, MetaDataNotFoundException {
        try {
            final InsertParserMultiple insertParser = new InsertParserMultiple(DEFAULT_VARNAME_ADAPTER);
            insertParser.parse(insertRequest);
            DbRequestFactoryImpl.getInstance().create().execInsertUnitRequest(insertParser);
        } catch (final MongoWriteException e) {
            throw new MetaDataAlreadyExistException(e);
        }
    }

    @Override
    public void insertObjectGroup(JsonNode objectGroupRequest)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataAlreadyExistException {

        try {
            final InsertParserMultiple insertParser = new InsertParserMultiple(DEFAULT_VARNAME_ADAPTER);
            insertParser.parse(objectGroupRequest);
            insertParser.getRequest().addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
            DbRequestFactoryImpl.getInstance().create().execInsertObjectGroupRequest(insertParser);
        } catch (final MongoWriteException e) {
            throw new MetaDataAlreadyExistException(e);
        }
    }

    @Override
    public List<Document> selectAccessionRegisterOnUnitByOperationId(String operationId) {
        AggregateIterable<Document> aggregate = MetadataCollections.UNIT.getCollection().aggregate(Arrays.asList(
            new Document("$match", new Document("$and", Arrays.asList(new Document(MetadataDocument.OPI, operationId),
                new Document(Unit.UNIT_TYPE, new Document("$ne", UnitType.HOLDING_UNIT.name()))))),
            new Document("$unwind", "$" + ORIGINATING_AGENCIES),
            new Document("$group",
                new Document(ID, "$" + ORIGINATING_AGENCIES).append(COUNT, new Document("$sum", 1)))),
            Document.class);
        return Lists.newArrayList(aggregate.iterator());
    }



    @Override
    public List<Document> selectAccessionRegisterOnObjectGroupByOperationId(String operationId) {
        AggregateIterable<Document> aggregate =
            MetadataCollections.OBJECTGROUP.getCollection().aggregate(Arrays.asList(
                new Document("$match", new Document(OPS, operationId)),
                new Document("$unwind", "$" + QUALIFIERS),
                new Document("$unwind", "$" + QUALIFIERS + ".versions"),
                new Document("$unwind", "$" + ORIGINATING_AGENCIES),
                new Document("$group",
                    new Document(ID, new Document(ORIGINATING_AGENCY, "$" + ORIGINATING_AGENCIES)
                        .append(OPI, "$" + MetadataDocument.OPI)
                        .append(QUALIFIER_VERSION_OPI, "$" + QUALIFIERS + ".versions._opi"))
                        .append(TOTAL_SIZE, new Document("$sum", "$" + QUALIFIERS + ".versions.Size"))
                        .append(TOTAL_OBJECT, new Document("$sum", 1))
                        .append(LIST_GOT, new Document("$addToSet", "$_id"))),
                new Document("$project", new Document(ID, 1)
                    .append(TOTAL_SIZE, 1)
                    .append(TOTAL_OBJECT, 1)
                    .append(TOTAL_GOT, new Document("$size", "$listGOT"))
                    /*.append(TOTAL_GOT,
                        new Document("$cond", Arrays
                            .asList(new Document("$ne", new Document("$" + OPI, "$" + QUALIFIER_VERSION_OPI)), 0,
                                new Document("$size", "$listGOT"))))*/
                )
                ),
                Document.class);
        return Lists.newArrayList(aggregate.iterator());
    }

    @Override
    public RequestResponse<JsonNode> selectUnitsByQuery(JsonNode selectQuery)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {
        LOGGER.debug("SelectUnitsByQuery/ selectQuery: " + selectQuery);
        return selectMetadataObject(selectQuery, null, Collections.singletonList(BuilderToken.FILTERARGS.UNITS));

    }

    @Override
    public RequestResponse<JsonNode> selectObjectGroupsByQuery(JsonNode selectQuery)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {
        LOGGER.debug("selectObjectGroupsByQuery/ selectQuery: " + selectQuery);
        return selectMetadataObject(selectQuery, null, Collections.singletonList(BuilderToken.FILTERARGS.OBJECTGROUPS));

    }

    @Override
    public RequestResponse<JsonNode> selectUnitsById(JsonNode selectQuery, String unitId)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {
        LOGGER.debug("SelectUnitsById/ selectQuery: " + selectQuery);
        return selectMetadataObject(selectQuery, unitId, Collections.singletonList(BuilderToken.FILTERARGS.UNITS));
    }

    @Override
    public RequestResponse<JsonNode> selectObjectGroupById(JsonNode selectQuery, String objectGroupId)
        throws InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataExecutionException,
        MetaDataNotFoundException, BadRequestException, VitamDBException {
        LOGGER.debug("SelectObjectGroupById - objectGroupId : " + objectGroupId);
        LOGGER.debug("SelectObjectGroupById - selectQuery : " + selectQuery);
        return selectMetadataObject(selectQuery, objectGroupId,
            Collections.singletonList(BuilderToken.FILTERARGS.OBJECTGROUPS));
    }


    private RequestResponseOK<JsonNode> selectMetadataObject(JsonNode selectQuery, String unitOrObjectGroupId,
        List<BuilderToken.FILTERARGS> filters)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {

        Result result;
        ArrayNode arrayNodeResponse;
        if (selectQuery.isNull()) {
            throw new InvalidParseOperationException(REQUEST_IS_NULL);
        }

        final JsonNode queryCopy = selectQuery.deepCopy();
        long offset = 0;
        long limit = 0;
        if (selectQuery.get("$filter") != null) {
            if (selectQuery.get("$filter").get("$offset") != null) {
                offset = selectQuery.get("$filter").get("$offset").asLong();
            }
            if (selectQuery.get("$filter").get("$limit") != null) {
                limit = selectQuery.get("$filter").get("$limit").asLong();
            }
        }

        try {
            // parse Select request
            final RequestParserMultiple selectRequest = new SelectParserMultiple(DEFAULT_VARNAME_ADAPTER);
            selectRequest.parse(selectQuery);
            // Reset $roots (add or override id on roots)
            if (unitOrObjectGroupId != null && !unitOrObjectGroupId.isEmpty()) {
                final RequestMultiple request = selectRequest.getRequest();
                if (request != null) {
                    LOGGER.debug("Reset $roots id with :" + unitOrObjectGroupId);
                    request.resetRoots().addRoots(unitOrObjectGroupId);
                }
            }
            if (filters != null && !filters.isEmpty()) {
                final RequestMultiple request = selectRequest.getRequest();
                if (request != null) {
                    final String[] hints = filters.stream()
                        .map(BuilderToken.FILTERARGS::exactToken)
                        .toArray(String[]::new);
                    LOGGER.debug("Adding given $hint filters: " + Arrays.toString(hints));
                    request.addHintFilter(hints);
                }
            }

            boolean shouldComputeUnitRule = false;
            ObjectNode fieldsProjection =
                (ObjectNode) selectRequest.getRequest().getProjection().get(PROJECTION.FIELDS.exactToken());
            if (fieldsProjection != null && fieldsProjection.get(GLOBAL.RULES.exactToken()) != null) {
                shouldComputeUnitRule = true;
                fieldsProjection.removeAll();
            }
            result = DbRequestFactoryImpl.getInstance().create().execRequest(selectRequest);
            arrayNodeResponse = MetadataJsonResponseUtils.populateJSONObjectResponse(result, selectRequest);

            // Compute Rule for unit(only with search by Id)
            if (shouldComputeUnitRule && result.hasFinalResult()) {
                computeRuleForUnit(arrayNodeResponse);
            }
        } catch (final MetaDataNotFoundException | BadRequestException e) {
            LOGGER.error(e);
            throw e;
        }
        List res = toArrayList(arrayNodeResponse);
        List<FacetResult> facetResults = (result != null) ? result.getFacet() : new ArrayList<>();
        Long total = (result != null) ? result.getTotal() : res.size();
        String scrollId = (result != null) ? result.getScrollId() : null;
        DatabaseCursor hits = (scrollId != null) ? new DatabaseCursor(total, offset, limit, res.size(), scrollId)
            : new DatabaseCursor(total, offset, limit, res.size());
        return new RequestResponseOK<JsonNode>(queryCopy)
            .addAllResults(res).addAllFacetResults(facetResults).setHits(hits);
    }

    // TODO : handle version
    @Override
    public void updateObjectGroupId(JsonNode updateQuery, String objectId)
        throws InvalidParseOperationException, MetaDataExecutionException, VitamDBException {
        Result result;
        if (updateQuery.isNull()) {
            throw new InvalidParseOperationException(REQUEST_IS_NULL);
        }
        try {
            final RequestParserMultiple updateRequest = new UpdateParserMultiple(new MongoDbVarNameAdapter());
            updateRequest.parse(updateQuery);

            // Reset $roots (add or override unit_id on roots)
            if (objectId != null && !objectId.isEmpty()) {
                final RequestMultiple request = updateRequest.getRequest();
                request.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
                if (request != null) {
                    LOGGER.debug("Reset $roots objectId by :" + objectId);
                    request.resetRoots().addRoots(objectId);
                    LOGGER.debug("DEBUG: {}", request);
                }
            }

            // Execute DSL request
            result = DbRequestFactoryImpl.getInstance().create().execRequest(updateRequest);
            if (result.getNbResult() == 0) {
                throw new MetaDataNotFoundException("ObjectGroup not found: " + objectId);
            }
        } catch (final MetaDataExecutionException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw e;
        } catch (final BadRequestException |
            MetaDataNotFoundException e) {
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        }
    }

    // TODO : in order to deal with selection (update from the root) in the query, the code should be modified
    @Override
    public RequestResponse<JsonNode> updateUnitbyId(JsonNode updateQuery, String unitId)
        throws MetaDataNotFoundException, InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, VitamDBException {
        Result result;
        ArrayNode arrayNodeResponse;
        if (updateQuery.isNull()) {
            throw new InvalidParseOperationException(REQUEST_IS_NULL);
        }
        JsonNode queryCopy = updateQuery.deepCopy();

        try {
            // parse Update request
            final RequestParserMultiple updateRequest = new UpdateParserMultiple(DEFAULT_VARNAME_ADAPTER);
            updateRequest.parse(updateQuery);
            // Reset $roots (add or override unit_id on roots)
            if (unitId != null && !unitId.isEmpty()) {
                final RequestMultiple request = updateRequest.getRequest();
                if (request != null) {
                    LOGGER.debug("Reset $roots unit_id by :" + unitId);
                    request.resetRoots().addRoots(unitId);
                    LOGGER.debug("DEBUG: {}", request);
                }
            }

            final String unitBeforeUpdate = JsonHandler.prettyPrint(getUnitById(unitId));

            // Execute DSL request
            result = DbRequestFactoryImpl.getInstance().create().execRequest(updateRequest);

            final String unitAfterUpdate = JsonHandler.prettyPrint(getUnitById(unitId));

            final Map<String, List<String>> diffs = new HashMap<>();
            diffs.put(unitId,
                VitamDocument.getConcernedDiffLines(VitamDocument.getUnifiedDiff(unitBeforeUpdate, unitAfterUpdate)));

            arrayNodeResponse = MetadataJsonResponseUtils.populateJSONObjectResponse(result, updateRequest, diffs);
        } catch (final MetaDataExecutionException | InvalidParseOperationException | MetaDataNotFoundException e) {
            LOGGER.error(e);
            throw e;
        } catch (final BadRequestException e) {
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        }
        List res = toArrayList(arrayNodeResponse);
        Long total = result != null ? result.getTotal() : res.size();
        return new RequestResponseOK<JsonNode>(queryCopy)
            .addAllResults(toArrayList(arrayNodeResponse))
            .setTotal(total);
    }

    private RequestResponse getUnitById(String id)
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException,
        MetaDataNotFoundException, BadRequestException, VitamDBException {
        final SelectMultiQuery select = new SelectMultiQuery();
        return selectUnitsById(select.getFinalSelect(), id);
    }

    private SelectMultiQuery createSearchParentSelect(List<String> unitList) throws InvalidParseOperationException {
        SelectMultiQuery newSelectQuery = new SelectMultiQuery();
        String[] rootList = new String[unitList.size()];
        rootList = unitList.toArray(rootList);
        newSelectQuery.addRoots(rootList);
        newSelectQuery.addProjection(
            JsonHandler.createObjectNode().set(PROJECTION.FIELDS.exactToken(),
                JsonHandler.createObjectNode()
                    .put(PROJECTIONARGS.ID.exactToken(), 1)
                    .put(PROJECTIONARGS.UNITUPS.exactToken(), 1)
                    .put(PROJECTIONARGS.MANAGEMENT.exactToken(), 1)));
        return newSelectQuery;
    }

    private void computeRuleForUnit(ArrayNode arrayNodeResponse)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataDocumentSizeException,
        MetaDataNotFoundException, BadRequestException, VitamDBException {
        Map<String, UnitNode> allUnitNode = new HashMap<>();
        Set<String> rootList = new HashSet<>();
        List<String> unitParentIdList = new ArrayList<>();
        String unitId = "";
        for (JsonNode unitNode : arrayNodeResponse) {
            ArrayNode unitParentId = (ArrayNode) unitNode.get(PROJECTIONARGS.ALLUNITUPS.exactToken());
            for (JsonNode parentIdNode : unitParentId) {
                unitParentIdList.add(parentIdNode.asText());
            }
            String currentUnitId = unitNode.get(PROJECTIONARGS.ID.exactToken()).asText();
            if (unitId.isEmpty()) {
                unitId = currentUnitId;
            }
            unitParentIdList.add(currentUnitId);
        }
        SelectMultiQuery newSelectQuery = createSearchParentSelect(unitParentIdList);
        RequestResponseOK unitParents = selectMetadataObject(newSelectQuery.getFinalSelect(), null,
            Collections.singletonList(BuilderToken.FILTERARGS.UNITS));

        Map<String, UnitSimplified> unitMap = UnitSimplified.getUnitIdMap(unitParents.getResults());
        UnitRuleCompute unitNode = new UnitRuleCompute(unitMap.get(unitId));
        unitNode.buildAncestors(unitMap, allUnitNode, rootList);
        unitNode.computeRule();
        JsonNode rule = JsonHandler.toJsonNode(unitNode.getHeritedRules().getInheritedRule());
        ((ObjectNode) arrayNodeResponse.get(0)).set(UnitInheritedRule.INHERITED_RULE, rule);
    }

    @Override
    public void flushUnit() throws IllegalArgumentException, VitamThreadAccessException {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        mongoDbAccess.getEsClient().refreshIndex(MetadataCollections.UNIT, tenantId);
    }

    @Override
    public void flushObjectGroup() throws IllegalArgumentException, VitamThreadAccessException {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        mongoDbAccess.getEsClient().refreshIndex(MetadataCollections.OBJECTGROUP, tenantId);
    }

    @Override
    public IndexationResult reindex(IndexParameters indexParam) {
        MetadataCollections collection;
        try {
            collection = MetadataCollections.valueOf(indexParam.getCollectionName().toUpperCase());
        } catch (IllegalArgumentException exc) {
            String message = String.format("Try to reindex a non metadata collection '%s' with metadata module",
                indexParam.getCollectionName());
            LOGGER.error(message);
            return IndexationHelper.getFullKOResult(indexParam, message);
        }
        // mongo collection
        MongoCollection<Document> mongoCollection = collection.getCollection();
        try (InputStream mappingStream = ElasticsearchCollections.valueOf(indexParam.getCollectionName().toUpperCase())
            .getMappingAsInputStream()) {
            return IndexationHelper.reindex(mongoCollection, mongoDbAccess.getEsClient(),
                indexParam.getTenants(), mappingStream);
        } catch (IOException exc) {
            LOGGER.error("Cannot get '{}' elastic search mapping for tenants {}", collection.name(),
                indexParam.getTenants().stream().map(Object::toString).collect(Collectors.joining(", ")));
            return IndexationHelper.getFullKOResult(indexParam, exc.getMessage());
        }
    }

    @Override
    public void switchIndex(String alias, String newIndexName) throws DatabaseException {
        try {
            IndexationHelper.switchIndex(alias, newIndexName, mongoDbAccess.getEsClient());
        } catch (DatabaseException exc) {
            LOGGER.error("Cannot switch alias {} to index {}", alias, newIndexName);
            throw exc;
        }
    }
}
