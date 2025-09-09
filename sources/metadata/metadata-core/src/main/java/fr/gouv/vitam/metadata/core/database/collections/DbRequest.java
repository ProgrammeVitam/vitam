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
package fr.gouv.vitam.metadata.core.database.collections;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.DeleteMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.parser.query.PathQuery;
import fr.gouv.vitam.common.database.parser.query.helper.QueryDepthHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.MongoDbInMemory;
import fr.gouv.vitam.common.database.server.RuleUpdateException;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.RequestToAbstract;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.DeleteToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.InsertToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.MongoDbHelper;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.RequestToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamCommonMetrics;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.config.VirtualPathsManager;
import fr.gouv.vitam.metadata.core.graph.GraphLoader;
import fr.gouv.vitam.metadata.core.model.RequestById;
import fr.gouv.vitam.metadata.core.model.UpdatedDocument;
import fr.gouv.vitam.metadata.core.trigger.FieldHistoryManager;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationErrorCode;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.metadata.core.validation.OntologyValidator;
import fr.gouv.vitam.metadata.core.validation.UnitValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Accumulators.addToSet;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.push;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.boolMust;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.rangeInclusiveQuery;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.termsQuery;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.TENANT_ID;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.OPS;

/**
 * DB Request using MongoDB only
 */
public class DbRequest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequest.class);

    private static final JsonPointer JSON_POINTER_TO_OPS = JsonPointer.compile("/$push/_ops/0");
    private static final String PATH_SEPARATOR = "/";
    /**
     * Quick projection for ID Only
     */
    private static final BasicDBObject ID_PROJECTION = new BasicDBObject(ID, 1);
    private static final String HISTORY_TRIGGER_NAME = "history-triggers.json";
    private static final String QUERY2 = "query: ";
    private static final String WHERE_PREVIOUS_RESULT_WAS = "where_previous_result_was: ";
    private static final String FROM2 = "from: ";
    private static final String NO_RESULT_AT_RANK2 = "no_result_at_rank: ";
    private static final String NO_RESULT_TRUE = "no_result: true";
    private static final String WHERE_PREVIOUS_IS = " \n\twhere previous is ";
    private static final String FROM = " from ";
    private static final String NO_RESULT_AT_RANK = "No result at rank: ";
    private static final String DEPTH_ARRAY = "deptharray";
    private static final String CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S =
        "[Consistency Error] : The document guid=%s in ES is not in MongoDB anymore, tenant : %s, requestId : %s";
    private static final long DEFAULT_WAITING_TIME_RETRY_MS = 10_000L;
    private static final int DEFAULT_RETRIES_COUNT = 3;

    private final MongoDbMetadataRepository<Unit> mongoDbUnitRepository;
    private final MongoDbMetadataRepository<ObjectGroup> mongoDbObjectGroupRepository;
    private final FieldHistoryManager fieldHistoryManager;
    private final VirtualPathsManager virtualPathsManager;

    @VisibleForTesting
    DbRequest(
        MongoDbMetadataRepository<Unit> mongoDbUnitRepository,
        MongoDbMetadataRepository<ObjectGroup> mongoDbObjectGroupRepository,
        FieldHistoryManager fieldHistoryManager,
        VirtualPathsManager virtualPathsManager
    ) {
        this.mongoDbUnitRepository = mongoDbUnitRepository;
        this.mongoDbObjectGroupRepository = mongoDbObjectGroupRepository;
        this.fieldHistoryManager = fieldHistoryManager;
        this.virtualPathsManager = virtualPathsManager;
    }

    public DbRequest(MetaDataConfiguration metaDataConfiguration) {
        this(
            new MongoDbMetadataRepository<>(MetadataCollections.UNIT::getCollection),
            new MongoDbMetadataRepository<>(MetadataCollections.OBJECTGROUP::getCollection),
            new FieldHistoryManager(HISTORY_TRIGGER_NAME),
            new VirtualPathsManager(metaDataConfiguration)
        );
    }

    /**
     * Execute rule action on unit
     *
     * @param ontologyModels
     * @param documentId the unitId
     * @param ruleActions the list of ruleAction (by category)
     */
    public UpdatedDocument execRuleRequest(
        final String documentId,
        final RuleActions ruleActions,
        Map<String, DurationData> bindRuleToDuration,
        OntologyValidator ontologyValidator,
        UnitValidator unitValidator,
        List<OntologyModel> ontologyModels
    )
        throws InvalidParseOperationException, MetaDataExecutionException, InvalidCreateOperationException, MetaDataNotFoundException, MetadataValidationException {
        final Integer tenantId = ParameterHelper.getTenantParameter();

        int tries = 0;

        while (tries < 3) {
            MetadataCollections metadataCollections = MetadataCollections.UNIT;

            MongoCollection<MetadataDocument<?>> collection = metadataCollections.getCollection();
            MetadataDocument<?> document = collection
                .find(and(eq(ID, documentId), eq(TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())))
                .first();
            if (document == null) {
                throw new MetaDataNotFoundException("Document not found by id " + documentId);
            }

            final JsonNode jsonDocument = JsonHandler.toJsonNode(document);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("DEBUG update {} to update to {}", jsonDocument, JsonHandler.prettyPrint(ruleActions));
            }
            DynamicParserTokens parserTokens = new DynamicParserTokens(
                metadataCollections.getVitamDescriptionResolver(),
                ontologyModels
            );
            final MongoDbInMemory mongoInMemory = new MongoDbInMemory(jsonDocument, parserTokens);

            // Add operationId to #operations
            UpdateMultiQuery updateQuery = new UpdateMultiQuery();
            updateQuery.addActions(
                push(VitamFieldsHelper.operations(), VitamThreadUtils.getVitamSession().getRequestId())
            );

            final RequestParserMultiple updateRequest = new UpdateParserMultiple(new MongoDbVarNameAdapter());
            updateRequest.parse(updateQuery.getFinalUpdateById());
            mongoInMemory.getUpdateJson(updateRequest);

            // Update rules
            final ObjectNode updatedJsonDocument;
            try {
                updatedJsonDocument = (ObjectNode) mongoInMemory.getUpdateJsonForRule(ruleActions, bindRuleToDuration);
            } catch (RuleUpdateException e) {
                throw new MetadataValidationException(toMetadataValidationErrorCode(e), e.getMessage(), e);
            }

            fieldHistoryManager.trigger(jsonDocument, updatedJsonDocument);

            Integer documentVersion = document.getVersion();
            int newDocumentVersion = documentVersion + 1;
            Integer atomicVersion = document.getAtomicVersion();
            int newAtomicVersion = atomicVersion == null ? newDocumentVersion : atomicVersion + 1;

            updatedJsonDocument.put(VitamDocument.VERSION, newDocumentVersion);
            updatedJsonDocument.put(MetadataDocument.ATOMIC_VERSION, newAtomicVersion);

            Unit updatedDocument = new Unit(updatedJsonDocument);

            // Ontology checks & format transformation
            final ObjectNode transformedUpdatedDocument = ontologyValidator.verifyAndReplaceFields(updatedJsonDocument);

            // Unit validation
            unitValidator.validateUnit(transformedUpdatedDocument);

            List<String> virtualFields = virtualPathsManager.getVirtualPathsFields();
            if (CollectionUtils.isNotEmpty(virtualFields)) {
                fillVirtualPaths(updatedJsonDocument, virtualFields);
            }
            // Make Update
            final Bson condition;
            if (atomicVersion == null) {
                condition = and(
                    eq(ID, documentId),
                    eq(TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId()),
                    exists(MetadataDocument.ATOMIC_VERSION, false)
                );
            } else {
                condition = and(
                    eq(ID, documentId),
                    eq(TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId()),
                    eq(MetadataDocument.ATOMIC_VERSION, atomicVersion)
                );
            }

            updatedDocument.setApproximateUpdateDate(LocalDateUtil.now());

            LOGGER.debug("DEBUG update {}", transformedUpdatedDocument);
            UpdateResult result = collection.replaceOne(condition, updatedDocument);
            if (result.getModifiedCount() == 1) {
                indexFieldsUpdated(updatedDocument, tenantId);
                return new UpdatedDocument(documentId, jsonDocument, JsonHandler.toJsonNode(updatedDocument), true);
            }
            tries++;
        }

        throw new MetaDataExecutionException("Can not modify document " + documentId);
    }

    private MetadataValidationErrorCode toMetadataValidationErrorCode(RuleUpdateException e) {
        switch (e.getRuleUpdateErrorCode()) {
            case HOLD_END_DATE_BEFORE_START_DATE:
                return MetadataValidationErrorCode.RULE_UPDATE_HOLD_END_DATE_BEFORE_START_DATE;
            case HOLD_END_DATE_ONLY_ALLOWED_FOR_HOLD_RULE_WITH_UNDEFINED_DURATION:
                return MetadataValidationErrorCode.RULE_UPDATE_UNEXPECTED_HOLD_END_DATE;
            default:
                throw new IllegalStateException("Unexpected value: " + e.getRuleUpdateErrorCode());
        }
    }

    /**
     * The request should be already analyzed.
     *
     * @param requestParser the RequestParserMultiple to execute
     * @return the Result
     * @throws MetaDataExecutionException when select/update/delete on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws BadRequestException
     */
    public Result<MetadataDocument<?>> execRequest(
        final RequestParserMultiple requestParser,
        List<OntologyModel> ontologies
    ) throws MetaDataExecutionException, InvalidParseOperationException, BadRequestException, VitamDBException {
        final RequestMultiple request = requestParser.getRequest();
        final RequestToAbstract requestToMongodb = RequestToMongodb.getRequestToMongoDb(requestParser);
        final int maxQuery = request.getNbQueries();
        boolean checkConsistency = false;
        Result<MetadataDocument<?>> roots;

        MetadataCollections metadataCollections;
        if (requestParser.model() == FILTERARGS.UNITS) {
            roots = checkUnitStartupRoots(requestParser);
            metadataCollections = MetadataCollections.UNIT;
        } else {
            metadataCollections = MetadataCollections.OBJECTGROUP;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("OBJECTGROUPS DbRequest: %s", requestParser.toString()));
            }
            roots = checkObjectGroupStartupRoots(requestParser);
        }

        DynamicParserTokens parserTokens = new DynamicParserTokens(
            metadataCollections.getVitamDescriptionResolver(),
            ontologies
        );

        Result<MetadataDocument<?>> result = roots;
        int rank = 0;
        // if roots is empty, check if first query gives a non empty roots (empty query allowed for insert)
        if (result.getCurrentIds().isEmpty() && maxQuery > 0) {
            final Result<MetadataDocument<?>> newResult = executeQuery(
                requestParser,
                requestToMongodb,
                rank,
                result,
                parserTokens
            );

            if (newResult != null && !newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
                checkConsistency = true;
            } else {
                LOGGER.debug(NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                result = new ResultError(requestParser.model())
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank)
                    .addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result)
                    .setTotal(newResult != null ? newResult.total : 0);
                return result;
            }
            LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
            rank++;
        }
        // Stops if no result (empty)
        for (; !result.getCurrentIds().isEmpty() && rank < maxQuery; rank++) {
            final Result<MetadataDocument<?>> newResult = executeQuery(
                requestParser,
                requestToMongodb,
                rank,
                result,
                parserTokens
            );
            if (newResult == null) {
                LOGGER.debug(NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                result = new ResultError(result.type)
                    .addError(result.getCurrentIds().toString())
                    .addError(NO_RESULT_AT_RANK2 + rank)
                    .addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            if (!newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
            } else {
                LOGGER.debug(NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                result = new ResultError(newResult.type)
                    .addError(newResult.getCurrentIds().toString())
                    .addError(NO_RESULT_AT_RANK2 + rank)
                    .addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
        }
        // others do not allow empty result
        if (result.getCurrentIds().isEmpty()) {
            LOGGER.debug(NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
            result = new ResultError(result.type)
                .addError(result.getCurrentIds().toString())
                .addError(NO_RESULT_AT_RANK2 + rank)
                .addError(FROM2 + requestParser)
                .addError(WHERE_PREVIOUS_RESULT_WAS + result);
            return result;
        }
        if (request instanceof DeleteMultiQuery) {
            final Result<MetadataDocument<?>> newResult = lastDeleteFilterProjection(
                (DeleteToMongodb) requestToMongodb,
                result
            );
            if (newResult != null) {
                result = newResult;
            }
        } else {
            // Select part
            // get facets
            // get result
            final Result<MetadataDocument<?>> newResult = lastSelectFilterProjection(
                (SelectToMongodb) requestToMongodb,
                result,
                checkConsistency
            );

            if (newResult != null) {
                result = newResult;
            }
        }
        LOGGER.debug("Results: {}", result);
        return result;
    }

    public List<UpdatedDocument> execUpdateRequest(
        List<RequestById> bulkRequests,
        MetadataCollections metadataCollection,
        OntologyValidator ontologyValidator,
        UnitValidator unitValidator,
        List<OntologyModel> ontologyModels,
        boolean forceUpdate,
        boolean refreshElasticIndexPostBulkIndexing
    ) throws MetaDataExecutionException, InvalidParseOperationException, MetaDataNotFoundException {
        if (CollectionUtils.isEmpty(bulkRequests)) {
            return Collections.emptyList();
        }
        AtomicInteger index = new AtomicInteger(0);
        bulkRequests.forEach(requestById -> requestById.setIndex(index.getAndIncrement()));

        //Prepare the sorted list with the right size to prevent shifting elements cost
        List<UpdatedDocument> sortedResults = new ArrayList<>(Collections.nCopies(bulkRequests.size(), null));

        List<RequestById> remainElementsList = new ArrayList<>(bulkRequests);
        List<RequestById> bulkRequestsFiltered;

        do {
            List<RequestById> duplicateElementsList = new ArrayList<>();
            //A map to preserve the index of each query by document Id, we are sure not have duplicate document id here
            Map<String, Integer> queriesIndexByDocumentId = new HashMap<>();
            Set<String> seenIdentifiers = new HashSet<>();
            bulkRequestsFiltered = new ArrayList<>();

            for (RequestById requestById : remainElementsList) {
                if (seenIdentifiers.add(requestById.getDocumentId())) {
                    bulkRequestsFiltered.add(requestById);
                    queriesIndexByDocumentId.put(requestById.getDocumentId(), requestById.getIndex());
                } else {
                    duplicateElementsList.add(requestById);
                }
            }
            LOGGER.debug(
                "handling a list of elements containings : " +
                String.join(",", bulkRequestsFiltered.stream().map(elt -> elt.getDocumentId()).toList())
            );
            final List<UpdatedDocument> partialResults = updateDocumentWithRetries(
                bulkRequestsFiltered,
                metadataCollection,
                ontologyValidator,
                unitValidator,
                ontologyModels,
                forceUpdate,
                refreshElasticIndexPostBulkIndexing
            );

            for (UpdatedDocument resultElt : partialResults) {
                if (queriesIndexByDocumentId.containsKey(resultElt.getDocumentId())) {
                    sortedResults.set(queriesIndexByDocumentId.get(resultElt.getDocumentId()), resultElt);
                }
            }
            remainElementsList = duplicateElementsList;
        } while (CollectionUtils.isNotEmpty(remainElementsList));
        LOGGER.info(
            "handling " + String.join(",", bulkRequestsFiltered.stream().map(elt -> elt.getDocumentId()).toList())
        );

        return sortedResults;
    }

    /**
     * Check Unit at startup against Roots
     *
     * @param request
     * @return the valid root ids
     */
    private Result<MetadataDocument<?>> checkUnitStartupRoots(final RequestParserMultiple request) {
        final Set<String> roots = request.getRequest().getRoots();
        if (roots.isEmpty()) {
            return MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS);
        }
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.UNITS, roots);
    }

    /**
     * Check ObjectGroup at startup against Roots
     *
     * @param request
     * @return the valid root ids
     */
    private Result<MetadataDocument<?>> checkObjectGroupStartupRoots(final RequestParserMultiple request) {
        final Set<String> roots = request.getRequest().getRoots();
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS, roots);
    }

    /**
     * Check Unit parents against Roots
     *
     * @param current set of result id
     * @param defaultStartSet
     * @return the valid root ids set
     */
    private Set<String> checkUnitAgainstRoots(
        final Set<String> current,
        final Result<MetadataDocument<?>> defaultStartSet
    ) {
        // roots
        if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
            // no limitation: using roots
            return current;
        }
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper.select(
            MetadataCollections.UNIT,
            MongoDbMetadataHelper.queryForAncestorsOrSame(current, defaultStartSet.getCurrentIds()),
            MongoDbMetadataHelper.ID_PROJECTION
        );
        final Set<String> newRoots = new HashSet<>();
        try (final MongoCursor<Unit> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final Unit unit = cursor.next();
                newRoots.add(unit.getId());
            }
        }
        return newRoots;
    }

    /**
     * Execute one request
     *
     * @param requestToMongodb
     * @param rank current rank query
     * @param previous previous Result from previous level (except in level == 0 where it is the subset of valid roots)
     * @return the new Result from this request
     * @throws MetaDataExecutionException
     * @throws InvalidParseOperationException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> executeQuery(
        final RequestParserMultiple requestParser,
        final RequestToAbstract requestToMongodb,
        final int rank,
        final Result<MetadataDocument<?>> previous,
        DynamicParserTokens parserTokens
    ) throws MetaDataExecutionException, InvalidParseOperationException, BadRequestException {
        final Query realQuery = requestToMongodb.getNthQuery(rank);
        final boolean isLastQuery = requestToMongodb.getNbQueries() == rank + 1;
        List<SortOptions> sorts = null;
        Map<String, Aggregation> facets = null;
        int limit = -1;
        int offset = -1;
        String scrollId = requestParser.getFinalScrollId();
        Integer scrollTimeout = requestParser.getFinalScrollTimeout();
        final Integer tenantId = ParameterHelper.getTenantParameter();
        final FILTERARGS collectionType = requestToMongodb.model();
        if (requestToMongodb instanceof SelectToMongodb && isLastQuery) {
            sorts = QueryToElasticsearch.getSorts(
                requestParser,
                collectionType.equals(FILTERARGS.UNITS)
                    ? MetadataCollections.UNIT.useScore()
                    : MetadataCollections.OBJECTGROUP.useScore(),
                parserTokens
            );
            if (FILTERARGS.UNITS.equals(collectionType) || FILTERARGS.OBJECTGROUPS.equals(collectionType)) {
                facets = QueryToElasticsearch.getFacets(requestParser, parserTokens);
            }
            limit = requestToMongodb.getFinalLimit();
            offset = requestToMongodb.getFinalOffset();
        }

        LOGGER.debug("Rank: " + rank + "\n\tPrevious: " + previous + "\n\tRequest: " + realQuery.getCurrentQuery());

        final QUERY type = realQuery.getQUERY();
        if (type == QUERY.PATH) {
            // Check if path is compatible with previous
            if (previous.getCurrentIds().isEmpty()) {
                previous.clear();
                return MongoDbMetadataHelper.createOneResult(collectionType, ((PathQuery) realQuery).getPaths());
            }
            if (collectionType.equals(FILTERARGS.UNITS)) {
                final Set<String> newRoots = checkUnitAgainstRoots(((PathQuery) realQuery).getPaths(), previous);
                previous.clear();
                if (newRoots.isEmpty()) {
                    return MongoDbMetadataHelper.createOneResult(collectionType);
                }
                return MongoDbMetadataHelper.createOneResult(collectionType, newRoots);
            } else {
                // FIXME TODO check against _up of OG
                return previous;
            }
        }
        // Not PATH
        int exactDepth = QueryDepthHelper.HELPER.getExactDepth(realQuery);
        if (exactDepth < 0) {
            exactDepth = GlobalDatas.MAXDEPTH;
        }

        boolean trackTotalHits = false;
        if (isLastQuery) {
            trackTotalHits = requestParser.trackTotalHits();
        }

        final int relativeDepth = QueryDepthHelper.HELPER.getRelativeDepth(realQuery);
        Result<MetadataDocument<?>> result;
        try {
            if (collectionType == FILTERARGS.UNITS) {
                if (exactDepth > 0) {
                    // Exact Depth request (descending)
                    LOGGER.debug("Unit Exact Depth request (descending)");
                    result = exactDepthUnitQuery(
                        realQuery,
                        previous,
                        exactDepth,
                        tenantId,
                        sorts,
                        offset,
                        limit,
                        facets,
                        scrollId,
                        scrollTimeout,
                        parserTokens,
                        trackTotalHits
                    );
                } else if (relativeDepth != 0) {
                    // Relative Depth request (ascending or descending)
                    LOGGER.debug("Unit Relative Depth request (ascending or descending)");
                    result = relativeDepthUnitQuery(
                        realQuery,
                        previous,
                        relativeDepth,
                        tenantId,
                        sorts,
                        offset,
                        limit,
                        facets,
                        scrollId,
                        scrollTimeout,
                        parserTokens,
                        trackTotalHits
                    );
                } else {
                    // Current sub level request
                    LOGGER.debug("Unit Current sub level request");
                    result = sameDepthUnitQuery(
                        realQuery,
                        previous,
                        tenantId,
                        sorts,
                        offset,
                        limit,
                        facets,
                        scrollId,
                        scrollTimeout,
                        parserTokens,
                        trackTotalHits
                    );
                }
            } else {
                // OBJECTGROUPS
                // No depth at all
                LOGGER.debug("ObjectGroup No depth at all");
                result = objectGroupQuery(
                    realQuery,
                    previous,
                    tenantId,
                    sorts,
                    offset,
                    limit,
                    scrollId,
                    scrollTimeout,
                    facets,
                    parserTokens,
                    trackTotalHits
                );
            }
        } finally {
            previous.clear();
        }
        return result;
    }

    /**
     * Execute one Unit Query using exact Depth
     *
     * @param realQuery
     * @param previous
     * @param exactDepth
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     * @param facets
     * @param trackTotalHits
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> exactDepthUnitQuery(
        Query realQuery,
        Result<MetadataDocument<?>> previous,
        int exactDepth,
        Integer tenantId,
        final List<SortOptions> sorts,
        final int offset,
        final int limit,
        final Map<String, Aggregation> facets,
        final String scrollId,
        final Integer scrollTimeout,
        DynamicParserTokens parserTokens,
        boolean trackTotalHits
    ) throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES only
        BoolQuery.Builder roots = QueryBuilders.bool()
            .must(rangeInclusiveQuery(Unit.MAXDEPTH, 0, exactDepth))
            .must(rangeInclusiveQuery(Unit.MINDEPTH, 0, exactDepth));
        if (!previous.getCurrentIds().isEmpty()) {
            roots.must(QueryToElasticsearch.getRoots(MetadataDocument.UP, previous.getCurrentIds()));
        }
        MetadataCollections metadataCollections = MetadataCollections.UNIT;

        // lets add the query on the tenant
        co.elastic.clients.elasticsearch._types.query_dsl.Query query = QueryBuilders.bool()
            .must(QueryToElasticsearch.getCommand(realQuery, new MongoDbVarNameAdapter(), parserTokens))
            .filter(termsQuery(Unit.UNITUPS + "." + exactDepth, previous.getCurrentIds()))
            .build()
            ._toQuery();

        LOGGER.debug("Req1LevelMD: {}", query);

        final Result<MetadataDocument<?>> result = metadataCollections
            .getEsClient()
            .search(
                metadataCollections,
                tenantId,
                query,
                sorts,
                offset,
                limit,
                facets,
                scrollId,
                scrollTimeout,
                trackTotalHits
            );

        LOGGER.warn("UnitExact: {}", result);

        return result;
    }

    /**
     * Execute one relative Depth Unit Query
     *
     * @param realQuery
     * @param previous
     * @param relativeDepth
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     * @param facets
     * @param trackTotalHits
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> relativeDepthUnitQuery(
        Query realQuery,
        Result<MetadataDocument<?>> previous,
        int relativeDepth,
        Integer tenantId,
        final List<SortOptions> sorts,
        final int offset,
        final int limit,
        final Map<String, Aggregation> facets,
        final String scrollId,
        final Integer scrollTimeout,
        DynamicParserTokens parserTokens,
        boolean trackTotalHits
    ) throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES only
        co.elastic.clients.elasticsearch._types.query_dsl.Query roots;

        // lets add the query on the tenant
        BoolQuery.Builder query = QueryBuilders.bool()
            .must(QueryToElasticsearch.getCommand(realQuery, new MongoDbVarNameAdapter(), parserTokens));

        if (previous.getCurrentIds().isEmpty()) {
            if (relativeDepth < 1) {
                query.filter(rangeInclusiveQuery(Unit.MAXDEPTH, 0, 1));
            } else {
                query.filter(rangeInclusiveQuery(Unit.MAXDEPTH, 0, relativeDepth + 1));
            }
        } else {
            if (relativeDepth == 1) {
                roots = QueryToElasticsearch.getRoots(MetadataDocument.UP, previous.getCurrentIds());
                query.filter(roots);
            } else if (relativeDepth > 1) {
                QueryToElasticsearch.addRoots(query, Unit.UNITDEPTHS, previous.getCurrentIds(), relativeDepth);
            } else {
                // Relative parent: previous has future result in their _up
                // so future result ids are in previous UNITDEPTHS
                final Set<String> fathers = aggregateUnitDepths(previous.getCurrentIds(), relativeDepth);
                roots = QueryToElasticsearch.getRoots(ID, fathers);
                query.filter(roots);
            }
        }

        LOGGER.debug("Req1LevelMD: {}", query);

        final Result<MetadataDocument<?>> result = MetadataCollections.UNIT.getEsClient()
            .search(
                MetadataCollections.UNIT,
                tenantId,
                query.build()._toQuery(),
                sorts,
                offset,
                limit,
                facets,
                scrollId,
                scrollTimeout,
                trackTotalHits
            );

        LOGGER.debug("UnitRelative: {}", result);

        return result;
    }

    /**
     * Aggregate Unit Depths according to parent relative Depth
     *
     * @param ids
     * @param relativeDepth
     * @return the aggregate set of multi level parents for this relativeDepth
     */
    protected Set<String> aggregateUnitDepths(Collection<String> ids, int relativeDepth) {
        // Select all items from ids
        final Bson match = match(in(ID, ids));
        // aggregate all UNITDEPTH in one (ignoring depth value)
        final Bson group = group(
            new BasicDBObject(ID, "all"),
            addToSet(DEPTH_ARRAY, BuilderToken.DEFAULT_PREFIX + Unit.UNITDEPTHS)
        );
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Depth: {} {}",
                MongoDbHelper.bsonToString(match, false),
                MongoDbHelper.bsonToString(group, false)
            );
        }
        final List<Bson> pipeline = Arrays.asList(match, group);
        final AggregateIterable<Unit> aggregateIterable = MetadataCollections.UNIT.<Unit>getCollection()
            .aggregate(pipeline);
        final Unit aggregate = aggregateIterable.first();
        final Set<String> set = new HashSet<>();
        if (aggregate != null) {
            @SuppressWarnings("unchecked")
            final List<Map<String, List<String>>> array = (List<Map<String, List<String>>>) aggregate.get(DEPTH_ARRAY);
            relativeDepth = Math.abs(relativeDepth);
            for (final Map<String, List<String>> map : array) {
                for (final String key : map.keySet()) {
                    int depth = Integer.parseInt(key);
                    if (depth <= relativeDepth) {
                        set.addAll(map.get(key));
                    }
                }
                map.clear();
            }
            array.clear();
        }
        return set;
    }

    /**
     * Execute one relative Depth Unit Query
     *
     * @param realQuery
     * @param previous
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     * @param facets
     * @param trackTotalHits
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> sameDepthUnitQuery(
        Query realQuery,
        Result<MetadataDocument<?>> previous,
        Integer tenantId,
        final List<SortOptions> sorts,
        final int offset,
        final int limit,
        final Map<String, Aggregation> facets,
        final String scrollId,
        final Integer scrollTimeout,
        DynamicParserTokens parserTokens,
        boolean trackTotalHits
    ) throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES
        final co.elastic.clients.elasticsearch._types.query_dsl.Query query = QueryToElasticsearch.getCommand(
            realQuery,
            new MongoDbVarNameAdapter(),
            parserTokens
        );
        co.elastic.clients.elasticsearch._types.query_dsl.Query finalQuery;
        LOGGER.debug("DEBUG prev {} RealQuery {}", previous.getCurrentIds(), realQuery);
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final co.elastic.clients.elasticsearch._types.query_dsl.Query roots = QueryToElasticsearch.getRoots(
                ID,
                previous.getCurrentIds()
            );
            finalQuery = boolMust(query, roots);
        }

        LOGGER.debug(QUERY2 + "{}", finalQuery);
        return MetadataCollections.UNIT.getEsClient()
            .search(
                MetadataCollections.UNIT,
                tenantId,
                finalQuery,
                sorts,
                offset,
                limit,
                facets,
                scrollId,
                scrollTimeout,
                trackTotalHits
            );
    }

    /**
     * Execute one relative Depth ObjectGroup Query
     *
     * @param realQuery
     * @param previous units, Note: only immediate Unit parents are allowed
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     * @param trackTotalHits
     * @return the associated Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> objectGroupQuery(
        Query realQuery,
        Result<MetadataDocument<?>> previous,
        Integer tenantId,
        final List<SortOptions> sorts,
        final int offset,
        final int limit,
        final String scrollId,
        final Integer scrollTimeout,
        final Map<String, Aggregation> facets,
        DynamicParserTokens parserTokens,
        boolean trackTotalHits
    ) throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES
        final co.elastic.clients.elasticsearch._types.query_dsl.Query query = QueryToElasticsearch.getCommand(
            realQuery,
            new MongoDbVarNameAdapter(),
            parserTokens
        );
        co.elastic.clients.elasticsearch._types.query_dsl.Query finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final co.elastic.clients.elasticsearch._types.query_dsl.Query roots;
            if (FILTERARGS.UNITS.equals(previous.getType())) {
                roots = QueryToElasticsearch.getRoots(MetadataDocument.UP, previous.getCurrentIds());
            } else {
                roots = QueryToElasticsearch.getRoots(ID, previous.getCurrentIds());
            }
            finalQuery = boolMust(query, roots);
        }

        LOGGER.debug(QUERY2 + "{}", finalQuery);
        return MetadataCollections.OBJECTGROUP.getEsClient()
            .search(
                MetadataCollections.OBJECTGROUP,
                tenantId,
                finalQuery,
                sorts,
                offset,
                limit,
                facets,
                scrollId,
                scrollTimeout,
                trackTotalHits
            );
    }

    /**
     * Finalize the queries with last True Select
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     */
    protected Result<MetadataDocument<?>> lastSelectFilterProjection(
        SelectToMongodb requestToMongodb,
        Result<MetadataDocument<?>> last,
        boolean checkConsistency
    ) throws InvalidParseOperationException, VitamDBException {
        final Bson roots = QueryToMongodb.getRoots(ID, last.getCurrentIds());
        final Bson projection = requestToMongodb.getFinalProjection();
        final boolean isIdIncluded = requestToMongodb.idWasInProjection();
        final FILTERARGS model = requestToMongodb.model();
        final List<String> desynchronizedResults = new ArrayList<>();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "To Select: " +
                MongoDbHelper.bsonToString(roots, false) +
                " " +
                (projection != null ? MongoDbHelper.bsonToString(projection, false) : "")
            );
        }
        if (model == FILTERARGS.UNITS) {
            final Map<String, Unit> units = new HashMap<>();
            final FindIterable<Unit> iterable = MetadataCollections.UNIT.<Unit>getCollection()
                .find(roots)
                .projection(projection);
            try (final MongoCursor<Unit> cursor = iterable.iterator()) {
                while (cursor.hasNext()) {
                    final Unit unit = cursor.next();
                    units.put(unit.getId(), unit);
                }
            }
            for (int i = 0; i < last.getCurrentIds().size(); i++) {
                final String id = last.getCurrentIds().get(i);
                Unit unit = units.get(id);
                if (unit != null) {
                    if (
                        VitamConfiguration.isExportScore() &&
                        MetadataCollections.UNIT.useScore() &&
                        requestToMongodb.isScoreIncluded()
                    ) {
                        Double score = 1d;
                        try {
                            score = last.scores.get(i);
                            if (score.isNaN()) {
                                score = 1d;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                        }
                        unit.append(VitamDocument.SCORE, score);
                    }
                    if (!isIdIncluded) {
                        unit.remove(ID);
                    }
                    last.addFinal(unit);
                } else if (checkConsistency) {
                    // check the consistency between elasticSearch and MongoDB
                    desynchronizedResults.add(id);
                    VitamCommonMetrics.CONSISTENCY_ERROR_COUNTER.labels(
                        String.valueOf(ParameterHelper.getTenantParameter()),
                        "DbRequest"
                    ).inc();
                    // desynchronization logs
                    LOGGER.error(
                        String.format(
                            CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S,
                            id,
                            ParameterHelper.getTenantParameter(),
                            VitamThreadUtils.getVitamSession().getRequestId()
                        )
                    );
                }
            }

            // As soon as we detect a synchronization error MongoDB / ES, we return an error.
            if (!desynchronizedResults.isEmpty()) {
                VitamCommonMetrics.CONSISTENCY_ERROR_COUNTER.labels(
                    String.valueOf(ParameterHelper.getTenantParameter()),
                    "DbRequest"
                ).inc();
                throw new VitamDBException(
                    "[Consistency ERROR] : An internal data consistency error has been detected !"
                );
            }

            return last;
        }
        // OBJECTGROUPS:
        final Map<String, ObjectGroup> obMap = new HashMap<>();
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable = (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
            MetadataCollections.OBJECTGROUP,
            roots,
            projection,
            null,
            -1,
            -1
        );
        try (final MongoCursor<ObjectGroup> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final ObjectGroup og = cursor.next();
                obMap.put(og.getId(), og);
            }
        }
        for (int i = 0; i < last.getCurrentIds().size(); i++) {
            final String id = last.getCurrentIds().get(i);
            ObjectGroup og = obMap.get(id);
            if (og != null) {
                if (
                    VitamConfiguration.isExportScore() &&
                    MetadataCollections.OBJECTGROUP.useScore() &&
                    requestToMongodb.isScoreIncluded()
                ) {
                    Double score = 1d;
                    try {
                        score = last.scores.get(i);
                        if (score.isNaN()) {
                            score = 1d;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                    }
                    og.append(VitamDocument.SCORE, score);
                }
                if (!isIdIncluded) {
                    og.remove(ID);
                }
                last.addFinal(og);
            } else if (checkConsistency) {
                // check the consistency between elasticSearch and MongoDB
                desynchronizedResults.add(id);
                VitamCommonMetrics.CONSISTENCY_ERROR_COUNTER.labels(
                    String.valueOf(ParameterHelper.getTenantParameter()),
                    "DbRequest"
                ).inc();
                // desynchronization logs
                LOGGER.error(
                    String.format(
                        CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S,
                        id,
                        ParameterHelper.getTenantParameter(),
                        VitamThreadUtils.getVitamSession().getRequestId()
                    )
                );
            }
        }

        // As soon as we detect a synchronization error MongoDB / ES, we return an error.
        if (!desynchronizedResults.isEmpty()) {
            VitamCommonMetrics.CONSISTENCY_ERROR_COUNTER.labels(
                String.valueOf(ParameterHelper.getTenantParameter()),
                "DbRequest"
            ).inc();
            throw new VitamDBException("[Consistency ERROR] : An internal data consistency error has been detected !");
        }

        return last;
    }

    private List<UpdatedDocument> updateDocumentWithRetries(
        final List<RequestById> bulkRequests,
        MetadataCollections metadataCollection,
        OntologyValidator ontologyValidator,
        UnitValidator unitValidator,
        List<OntologyModel> ontologyModels,
        boolean forceUpdate,
        boolean refreshElasticIndexPostBulkIndexing
    ) throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        if (CollectionUtils.isEmpty(bulkRequests)) {
            return Collections.emptyList();
        }
        DynamicParserTokens parserTokens = new DynamicParserTokens(
            metadataCollection.getVitamDescriptionResolver(),
            ontologyModels
        );

        Map<String, UpdatedDocument> updateDocumentResults = new HashMap<>();

        final Set<String> seen = new HashSet<>();
        final Set<String> duplicates = bulkRequests
            .stream()
            .map(RequestById::getDocumentId)
            .filter(val -> !seen.add(val))
            .collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(duplicates)) {
            throw new InvalidParseOperationException("Duplicate document Id found: " + String.join(",", duplicates));
        }

        Set<RequestById> remainRequests = new HashSet<>(bulkRequests);
        int tries = 0;
        boolean retryMore;

        Map<String, UpdatedDocument> failedUpdateDocuments = new HashMap<>();
        do {
            Set<String> documentIds = remainRequests
                .stream()
                .map(RequestById::getDocumentId)
                .collect(Collectors.toSet());

            List<MetadataDocument> dbDocuments = loadDocumentsFromDb(documentIds, metadataCollection);

            Map<String, MetadataDocument> dbDocumentsById = dbDocuments
                .stream()
                .collect(Collectors.toMap(MetadataDocument::getId, Function.identity()));

            Set<String> alreadyUpdatedDocumentIdsByCurrentProcess = retrieveDocumentIdsAlreadyUpdatedByCurrentProcess(
                remainRequests,
                dbDocumentsById,
                updateDocumentResults
            );

            Set<RequestById> requestsByIdToApplyOnMongoDb = remainRequests
                .stream()
                .filter(requestById -> !alreadyUpdatedDocumentIdsByCurrentProcess.contains(requestById.getDocumentId()))
                .collect(Collectors.toSet());

            Map<String, ObjectNode> transformedDocumentsToUpdateInMongo = transformDocumentsToUpdate(
                requestsByIdToApplyOnMongoDb,
                dbDocumentsById,
                parserTokens,
                ontologyValidator,
                metadataCollection,
                unitValidator,
                failedUpdateDocuments
            );

            Map<String, UpdatedDocument> documentsResultToIgnore = retrieveIgnoredDocumentForUpdate(
                metadataCollection,
                forceUpdate,
                remainRequests,
                dbDocumentsById,
                transformedDocumentsToUpdateInMongo,
                updateDocumentResults
            );

            //remove documents to ignore

            documentsResultToIgnore
                .keySet()
                .forEach(documentId -> {
                    transformedDocumentsToUpdateInMongo.remove(documentId);
                    requestsByIdToApplyOnMongoDb.removeIf(requestById -> requestById.getDocumentId() == documentId);
                });

            List<WriteModel<MetadataDocument<?>>> bulkOperations = prepareDbBulkOperations(
                transformedDocumentsToUpdateInMongo,
                dbDocumentsById
            );
            Set<String> successUpdatedDocumentsIds = handleBulkingUpdateDocumentsInMongo(
                metadataCollection,
                bulkOperations,
                requestsByIdToApplyOnMongoDb,
                failedUpdateDocuments,
                tenantId
            );

            updateDocumentResults.putAll(
                handleIndexingDocumentsInElasticSearch(
                    metadataCollection,
                    refreshElasticIndexPostBulkIndexing,
                    successUpdatedDocumentsIds,
                    alreadyUpdatedDocumentIdsByCurrentProcess,
                    transformedDocumentsToUpdateInMongo,
                    dbDocumentsById,
                    documentsResultToIgnore
                )
            );

            //Manage ignoredDocuments response
            updateDocumentResults.putAll(documentsResultToIgnore);

            boolean hasFailedUpdates = successUpdatedDocumentsIds.size() != bulkOperations.size();
            if (hasFailedUpdates) {
                remainRequests = retrieveFailedDocuments(remainRequests, successUpdatedDocumentsIds);
                LOGGER.debug("Failed updated found with documents size : " + remainRequests.size());
                waitSomeTimeBeforeRetryFailed();
                tries++;

                if (tries >= DEFAULT_RETRIES_COUNT && CollectionUtils.isEmpty(remainRequests)) {
                    LOGGER.error(
                        "After 3 retries, failed to documents with ids " +
                        "[" +
                        String.join(", ", remainRequests.stream().map(RequestById::getDocumentId).toList()) +
                        "]"
                    );
                    throw new IllegalStateException(
                        "After 3 retries, failed to documents with ids " +
                        "[" +
                        String.join(", ", remainRequests.stream().map(RequestById::getDocumentId).toList()) +
                        "]"
                    );
                }
                retryMore = tries < DEFAULT_RETRIES_COUNT && !remainRequests.isEmpty();
            } else {
                retryMore = false;
            }
        } while (retryMore);

        updateDocumentResults.putAll(failedUpdateDocuments);

        return updateDocumentResults.values().stream().toList();
    }

    private void waitSomeTimeBeforeRetryFailed() {
        try {
            long waitingMs = DEFAULT_WAITING_TIME_RETRY_MS * (new SecureRandom().nextInt(10) + 1);
            LOGGER.info("Waiting before retry for " + waitingMs);
            Thread.sleep(waitingMs);
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    private Set<String> handleBulkingUpdateDocumentsInMongo(
        MetadataCollections metadataCollection,
        List<WriteModel<MetadataDocument<?>>> bulkOperations,
        Set<RequestById> requestsByIdToApplyOnMongoDb,
        Map<String, UpdatedDocument> failedUpdateDocuments,
        Integer tenantId
    ) {
        Set<String> successUpdatedDocumentsIds = new HashSet<>();
        if (CollectionUtils.isNotEmpty(bulkOperations)) {
            BulkWriteResult bulkWriteMongoResult = metadataCollection
                .getCollection()
                .bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));

            Set<String> documentIdsRequestedToUpdate = requestsByIdToApplyOnMongoDb
                .stream()
                .map(RequestById::getDocumentId)
                .filter(documentId -> !failedUpdateDocuments.containsKey(documentId))
                .collect(Collectors.toSet());

            successUpdatedDocumentsIds = extractSuccessUpdatedDocumentIds(
                bulkWriteMongoResult,
                metadataCollection,
                bulkOperations.size(),
                documentIdsRequestedToUpdate,
                tenantId
            );
        }
        return successUpdatedDocumentsIds;
    }

    private Map<String, UpdatedDocument> handleIndexingDocumentsInElasticSearch(
        MetadataCollections metadataCollection,
        boolean refreshElasticIndexPostBulkIndexing,
        Set<String> successUpdatedDocumentsIds,
        Set<String> alreadyUpdatedDocumentIdsByCurrentProcess,
        Map<String, ObjectNode> transformedDocumentsToUpdateInMongo,
        Map<String, MetadataDocument> dbDocumentsById,
        Map<String, UpdatedDocument> documentsResultToIgnore
    ) throws InvalidParseOperationException, MetaDataExecutionException {
        Set<String> documentIdsToIndexInEs = new HashSet<>(successUpdatedDocumentsIds);

        //We index the update documents in mongo and those already updated in mongo before
        documentIdsToIndexInEs.addAll(alreadyUpdatedDocumentIdsByCurrentProcess);

        Map<String, ObjectNode> documentsToIndexInES = new HashMap<>(transformedDocumentsToUpdateInMongo);
        for (String documentId : alreadyUpdatedDocumentIdsByCurrentProcess) {
            documentsToIndexInES.put(documentId, (ObjectNode) JsonHandler.toJsonNode(dbDocumentsById.get(documentId)));
        }

        indexSuccessDocumentsInES(
            metadataCollection,
            documentIdsToIndexInEs,
            dbDocumentsById,
            documentsToIndexInES,
            refreshElasticIndexPostBulkIndexing
        );

        return prepareSuccessResponseResults(
            documentIdsToIndexInEs,
            documentsResultToIgnore,
            dbDocumentsById,
            transformedDocumentsToUpdateInMongo
        );
    }

    private Map<String, UpdatedDocument> prepareSuccessResponseResults(
        Set<String> documentIdsToIndexInEs,
        Map<String, UpdatedDocument> documentsResultToIgnore,
        Map<String, MetadataDocument> dbDocumentsById,
        Map<String, ObjectNode> transformedDocumentsToUpdateInMongo
    ) throws InvalidParseOperationException {
        Map<String, UpdatedDocument> successUpdateDocumentResults = new HashMap<>();
        for (String successDocId : documentIdsToIndexInEs) {
            final JsonNode afterUpdate = documentsResultToIgnore.containsKey(successDocId)
                ? JsonHandler.toJsonNode(dbDocumentsById.get(successDocId))
                : transformedDocumentsToUpdateInMongo.get(successDocId);
            successUpdateDocumentResults.put(
                successDocId,
                new UpdatedDocument(
                    successDocId,
                    JsonHandler.toJsonNode(dbDocumentsById.get(successDocId)),
                    afterUpdate,
                    true
                )
            );
        }
        return successUpdateDocumentResults;
    }

    private List<WriteModel<MetadataDocument<?>>> prepareDbBulkOperations(
        Map<String, ObjectNode> transformedUpdatedDocuments,
        Map<String, MetadataDocument> dbDocumentsById
    ) {
        List<WriteModel<MetadataDocument<?>>> bulkOperations = new ArrayList<>();
        for (Map.Entry<String, ObjectNode> entryTransformedDoc : transformedUpdatedDocuments.entrySet()) {
            //prepares operations to bulk
            MetadataDocument<?> currentDocument = dbDocumentsById.get(entryTransformedDoc.getKey());
            MetadataDocument<?> transformedDocument = (MetadataDocument<?>) currentDocument.newInstance(
                entryTransformedDoc.getValue()
            );
            final Bson filter = getUpdateDocumentFilter(
                currentDocument.getAtomicVersion(),
                entryTransformedDoc.getKey()
            );
            bulkOperations.add(new ReplaceOneModel<>(filter, transformedDocument));
        }
        return bulkOperations;
    }

    private Map<String, UpdatedDocument> retrieveIgnoredDocumentForUpdate(
        MetadataCollections metadataCollection,
        boolean forceUpdate,
        Set<RequestById> requestsToApply,
        Map<String, MetadataDocument> dbDocumentsById,
        Map<String, ObjectNode> transformedUpdatedDocuments,
        Map<String, UpdatedDocument> updateDocumentResults
    ) throws InvalidParseOperationException {
        if (metadataCollection == MetadataCollections.UNIT) {
            for (RequestById request : requestsToApply) {
                String documentId = request.getDocumentId();
                MetadataDocument currentDocument = dbDocumentsById.get(documentId);
                ObjectNode transformedUpdatedDocument = transformedUpdatedDocuments.get(documentId);
                final JsonNode currentJsonDocument = JsonHandler.toJsonNode(currentDocument);

                if (
                    !forceUpdate &&
                    !hasModificationOfUnitDescriptiveMetadata(currentJsonDocument, transformedUpdatedDocument)
                ) {
                    updateDocumentResults.put(
                        documentId,
                        new UpdatedDocument(documentId, currentJsonDocument, currentJsonDocument, false)
                    );
                }
            }
        }
        return updateDocumentResults;
    }

    private Set<String> retrieveDocumentIdsAlreadyUpdatedByCurrentProcess(
        Set<RequestById> remainRequests,
        Map<String, MetadataDocument> dbDocumentsById,
        Map<String, UpdatedDocument> updateDocumentResults
    ) throws InvalidParseOperationException {
        Set<String> alreadyUpdatedDocumentBefore = new HashSet<>();
        for (RequestById request : remainRequests) {
            RequestParserMultiple requestParser = request.getRequest();
            String documentId = request.getDocumentId();
            MetadataDocument currentDocument = dbDocumentsById.get(documentId);
            if (noChangesAndOpsAlreadyContainingOperation(requestParser, currentDocument)) {
                alreadyUpdatedDocumentBefore.add(documentId);
                final JsonNode currentJsonDocument = JsonHandler.toJsonNode(currentDocument);
                updateDocumentResults.put(
                    documentId,
                    new UpdatedDocument(documentId, currentJsonDocument, currentJsonDocument, true)
                );
            }
        }
        return alreadyUpdatedDocumentBefore;
    }

    private Map<String, ObjectNode> transformDocumentsToUpdate(
        Set<RequestById> requestsToHandle,
        Map<String, MetadataDocument> oldDocuments,
        DynamicParserTokens parserTokens,
        OntologyValidator ontologyValidator,
        MetadataCollections metadataCollection,
        UnitValidator unitValidator,
        Map<String, UpdatedDocument> failedUpdateDocuments
    ) throws InvalidParseOperationException {
        LOGGER.debug("Handling transformation for bulk update for a set of " + requestsToHandle.size());

        Map<String, ObjectNode> transformedDocumentsToUpdate = new HashMap<>();
        for (RequestById request : requestsToHandle) {
            String documentId = request.getDocumentId();
            RequestParserMultiple requestParser = request.getRequest();
            MetadataDocument currentDocument = oldDocuments.get(documentId);
            final JsonNode currentJsonDocument = JsonHandler.toJsonNode(currentDocument);
            try {
                final MongoDbInMemory mongoInMemory = new MongoDbInMemory(currentJsonDocument, parserTokens);
                final ObjectNode updatedJsonDocument = (ObjectNode) mongoInMemory.getUpdateJson(requestParser);
                if (metadataCollection == MetadataCollections.UNIT) {
                    fieldHistoryManager.trigger(currentJsonDocument, updatedJsonDocument);
                    List<String> virtualFields = virtualPathsManager.getVirtualPathsFields();
                    if (CollectionUtils.isNotEmpty(virtualFields)) {
                        boolean anyChangesOnVirtualFieldsSrc = false;
                        for (String virtualField : virtualFields) {
                            JsonNode currentVirtualPathNode = currentJsonDocument.get(virtualField);
                            JsonNode toUpdateVirtualPathNode = updatedJsonDocument.get(virtualField);
                            anyChangesOnVirtualFieldsSrc = !Objects.equals(
                                currentVirtualPathNode,
                                toUpdateVirtualPathNode
                            );
                            if (anyChangesOnVirtualFieldsSrc) break;
                        }
                        if (anyChangesOnVirtualFieldsSrc) {
                            fillVirtualPaths(updatedJsonDocument, virtualFields);
                        }
                    }
                }
                final Integer docVersion = currentDocument.getVersion();
                int newDocVersion = incrementDocumentVersionIfRequired(metadataCollection, mongoInMemory, docVersion);
                updatedJsonDocument.put(VitamDocument.VERSION, newDocVersion);
                Integer atomicVersion = currentDocument.getAtomicVersion();
                int newAtomicVersion = atomicVersion == null ? newDocVersion : atomicVersion + 1;
                updatedJsonDocument.put(MetadataDocument.ATOMIC_VERSION, newAtomicVersion);
                // Ontology checks & format transformation
                final ObjectNode transformedUpdatedDocument;

                transformedUpdatedDocument = ontologyValidator.verifyAndReplaceFields(updatedJsonDocument);

                if (newDocVersion != docVersion) {
                    transformedUpdatedDocument.put(
                        MetadataDocument.APPROXIMATE_UPDATE_DATE,
                        LocalDateUtil.nowFormatted()
                    );
                }

                if (metadataCollection == MetadataCollections.UNIT) {
                    // Unit validation
                    unitValidator.validateUnit(transformedUpdatedDocument);
                }

                LOGGER.debug("DEBUG update {}", transformedUpdatedDocument);
                transformedDocumentsToUpdate.put(documentId, transformedUpdatedDocument);
            } catch (MetadataValidationException e) {
                LOGGER.error("Failed to validate document with id:  " + documentId + " error" + e.getMessage());
                failedUpdateDocuments.put(
                    documentId,
                    new UpdatedDocument(
                        documentId,
                        currentJsonDocument,
                        currentJsonDocument,
                        false,
                        e.getErrorCode(),
                        e.getMessage()
                    )
                );
            }
        }
        return transformedDocumentsToUpdate;
    }

    private Set<RequestById> retrieveFailedDocuments(
        Set<RequestById> remainRequestsToHandle,
        Set<String> successUpdatedDocumentsIds
    ) {
        return remainRequestsToHandle
            .stream()
            .filter(requestById -> successUpdatedDocumentsIds.contains(requestById.getDocumentId()))
            .collect(Collectors.toSet());
    }

    private Bson getUpdateDocumentFilter(Integer atomicVersion, String documentId) {
        // Make Update
        final Bson condition;
        if (atomicVersion == null) {
            condition = and(eq(ID, documentId), exists(MetadataDocument.ATOMIC_VERSION, false));
        } else {
            condition = and(eq(ID, documentId), eq(MetadataDocument.ATOMIC_VERSION, atomicVersion));
        }
        return condition;
    }

    private void indexSuccessDocumentsInES(
        MetadataCollections metadataCollection,
        Set<String> documentIdsToIndex,
        Map<String, MetadataDocument> dbDocumentsById,
        Map<String, ObjectNode> updatedDocuments,
        boolean withRefreshIndex
    ) throws MetaDataExecutionException {
        if (CollectionUtils.isEmpty(documentIdsToIndex)) {
            return;
        }
        List<MetadataDocument<?>> documentsToIndex = new ArrayList<>();
        for (String successDocId : documentIdsToIndex) {
            if (updatedDocuments.containsKey(successDocId)) {
                MetadataDocument<?> currentDoc = dbDocumentsById.get(successDocId);
                documentsToIndex.add((MetadataDocument<?>) currentDoc.newInstance(updatedDocuments.get(successDocId)));
            }
        }
        indexUpdatedDocuments(metadataCollection, documentsToIndex, withRefreshIndex);
    }

    private Set<String> extractSuccessUpdatedDocumentIds(
        BulkWriteResult bulkWriteResult,
        MetadataCollections metadataCollection,
        int bulkedDocumentCount,
        Set<String> documentIdsRequestToUpdate,
        Integer tenantId
    ) {
        Set<String> successDocumentsId = new HashSet<>();

        if (bulkWriteResult.getModifiedCount() == bulkedDocumentCount) {
            successDocumentsId = documentIdsRequestToUpdate;
        } else {
            Bson filter = and(
                eq(TENANT_ID, tenantId),
                in(ID, documentIdsRequestToUpdate),
                eq(OPS, VitamThreadUtils.getVitamSession().getRequestId())
            );

            try (
                MongoCursor<?> cursor = metadataCollection
                    .getCollection()
                    .find(filter)
                    .projection(ID_PROJECTION)
                    .iterator()
            ) {
                while (cursor.hasNext()) {
                    MetadataDocument document = (MetadataDocument) cursor.next();
                    successDocumentsId.add(document.getId());
                }
            }
        }
        return successDocumentsId;
    }

    private List<MetadataDocument> loadDocumentsFromDb(Set<String> documentIds, MetadataCollections collection)
        throws MetaDataNotFoundException {
        LOGGER.debug("Loading documents from for documents id " + String.join(", ", documentIds));
        List<MetadataDocument> documents = new ArrayList<>();
        Bson filter = and(in(ID, documentIds), eq(TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId()));

        try (MongoCursor<?> cursor = collection.getCollection().find(filter).iterator()) {
            while (cursor.hasNext()) {
                MetadataDocument document = (MetadataDocument) cursor.next();
                documents.add(document);
            }
        }

        if (documents.size() != documentIds.size()) {
            throw new MetaDataNotFoundException(
                "Number of documents found is different to the requested document list, the document nb requested is:  " +
                documents.size() +
                "but document found is :  " +
                documentIds.size()
            );
        }

        return documents;
    }

    /**
     * Generates a diff between two version of a Unit Json document, exclude internal
     * modification and return true if there are other modification.<br>
     * The excluded internal modification are : _v, _av, _glpd, _ops, _history<br>
     *
     * @param oldDocument original document
     * @param newDocument modifies document
     * @return true if at least one modification has been made on a descriptive
     * metadata, false if not
     */
    private boolean hasModificationOfUnitDescriptiveMetadata(final JsonNode oldDocument, final JsonNode newDocument) {
        List<String> diffLines = VitamDocument.getConcernedDiffLines(
            VitamDocument.getUnifiedDiff(JsonHandler.prettyPrint(oldDocument), JsonHandler.prettyPrint(newDocument))
        );
        long metadataModifications = diffLines
            .stream()
            .filter(line -> !(line.contains("\"" + MetadataDocument.VERSION + "\"")))
            .filter(line -> !(line.contains("\"" + MetadataDocument.ATOMIC_VERSION + "\"")))
            .filter(line -> !(line.contains("\"" + MetadataDocument.GRAPH_LAST_PERSISTED_DATE + "\"")))
            .filter(line -> !(line.contains("\"" + OPS + "\"")))
            .filter(line -> !(line.contains("\"" + MetadataDocument.APPROXIMATE_CREATION_DATE + "\"")))
            .filter(line -> !(line.contains("\"" + MetadataDocument.APPROXIMATE_UPDATE_DATE + "\"")))
            .filter(line -> !(line.contains("\"_history\"")))
            .count();

        return metadataModifications > 0;
    }

    /**
     * Some technical operations do not update the OPS field, ex: Elimination analysis, reclassification of indirect nodes, etc
     * If OPS field update has been requested, no need to update document again in mongoDb, but we should to re-index it in ES to ensure idempotency
     * In this case, we can't return documents updates diff
     *
     * @param requestParser
     * @param document
     * @return
     */
    private boolean noChangesAndOpsAlreadyContainingOperation(
        RequestParserMultiple requestParser,
        MetadataDocument<?> document
    ) {
        Optional<Action> actionWithOp = requestParser
            .getRequest()
            .getActions()
            .stream()
            .filter(a -> !a.getCurrentAction().at(JSON_POINTER_TO_OPS).isMissingNode())
            .findFirst();
        if (actionWithOp.isEmpty()) {
            return false;
        }
        String opsToAdd = actionWithOp.get().getCurrentObject().at(JSON_POINTER_TO_OPS).asText();
        Collection<String> existingOps = document.getCollectionOrEmpty(SedaConstants.PREFIX_OPS);

        return existingOps.contains(opsToAdd);
    }

    private int incrementDocumentVersionIfRequired(
        MetadataCollections metadataCollection,
        MongoDbInMemory mongoInMemory,
        int documentVersion
    ) {
        // FIXME : To avoid potential update loss for computed fields, we should make version field "non persisted"
        //  and always increment document version (DbRequest, and any other document update like graph computation,
        //  indexation, reconstruction...)

        Set<String> updatedFields = mongoInMemory.getUpdatedFields();

        Set<String> computedFields = (metadataCollection == MetadataCollections.UNIT)
            ? MetadataDocumentHelper.getComputedUnitFields()
            : MetadataDocumentHelper.getComputedObjectGroupFields();

        if (computedFields.containsAll(updatedFields)) {
            return documentVersion;
        } else {
            return documentVersion + 1;
        }
    }

    private void indexUpdatedDocuments(
        MetadataCollections collection,
        Collection<MetadataDocument<?>> updatedDocuments,
        boolean withRefreshIndex
    ) throws MetaDataExecutionException {
        if (collection == MetadataCollections.UNIT) {
            indexUpdatedDocuments(updatedDocuments, ParameterHelper.getTenantParameter(), withRefreshIndex);
        } else {
            indexUpdatedOGDocuments(updatedDocuments, ParameterHelper.getTenantParameter(), withRefreshIndex);
        }
    }

    /**
     * indexUpdatedDocuments : Update documents updated
     *
     * @param tenantId : contains the document list to be indexed
     * @throws Exception
     */
    private void indexUpdatedDocuments(
        Collection<MetadataDocument<?>> updatedDocuments,
        Integer tenantId,
        boolean withRefreshIndex
    ) throws MetaDataExecutionException {
        MetadataCollections.UNIT.getEsClient()
            .insertFullDocumentsWithRefreshSettings(
                MetadataCollections.UNIT,
                tenantId,
                updatedDocuments,
                withRefreshIndex
            );
    }

    /**
     * indexFieldsUpdated : Update index related to Fields updated
     *
     * @param updatedDocument : contains the document to be indexed
     * @throws Exception
     */
    private void indexFieldsUpdated(MetadataDocument<?> updatedDocument, Integer tenantId)
        throws MetaDataExecutionException {
        MetadataCollections.UNIT.getEsClient()
            .updateFullDocument(MetadataCollections.UNIT, tenantId, updatedDocument.getId(), updatedDocument);
    }

    /**
     * indexUpdatedDocuments : Update documents updated
     *
     * @param tenantId : contains the document list to be indexed
     * @throws Exception
     */
    private void indexUpdatedOGDocuments(
        Collection<MetadataDocument<?>> updatedDocuments,
        Integer tenantId,
        boolean withRefreshIndex
    ) throws MetaDataExecutionException {
        MetadataCollections.OBJECTGROUP.getEsClient()
            .insertFullDocumentsWithRefreshSettings(
                MetadataCollections.OBJECTGROUP,
                tenantId,
                updatedDocuments,
                withRefreshIndex
            );
    }

    /**
     * removeOGIndexFields : remove index related to Fields deleted
     *
     * @param last : contains the Result to be removed
     * @throws Exception
     */
    private void removeOGIndexFields(Result<MetadataDocument<?>> last) throws Exception {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        if (last.getCurrentIds().isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            // no result to delete
            return;
        }
        MetadataCollections.OBJECTGROUP.getEsClient().deleteBulkOGEntriesIndexes(last.getCurrentIds(), tenantId);
    }

    /**
     * removeUnitIndexFields : remove index related to Fields deleted
     *
     * @param last : contains the Result to be removed
     * @throws Exception
     */
    private void removeUnitIndexFields(Result<MetadataDocument<?>> last) throws Exception {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        if (last.getCurrentIds().isEmpty()) {
            LOGGER.error("ES delete in error since no results to delete");
            // no result to delete
            return;
        }
        MetadataCollections.UNIT.getEsClient().deleteBulkUnitsEntriesIndexes(last.getCurrentIds(), tenantId);
    }

    /**
     * Inserts a unit
     *
     * @param requestParser the InsertParserMultiple to execute
     * @throws MetaDataExecutionException when insert on metadata collection exception occurred
     * @throws MetaDataNotFoundException when metadata not found exception
     */
    @Deprecated
    public void execInsertUnitRequest(InsertParserMultiple requestParser)
        throws MetaDataExecutionException, MetaDataNotFoundException {
        LOGGER.debug("Exec db insert unit request: %s", requestParser);
        execInsertUnitRequests(Collections.singletonList(requestParser));
    }

    /**
     * Inserts an object group
     *
     * @param requestParsers the list of InsertParserMultiple to execute
     * @throws MetaDataExecutionException when insert on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws MetaDataAlreadyExistException when insert metadata exception
     */
    public void execInsertObjectGroupRequests(List<InsertParserMultiple> requestParsers)
        throws MetaDataExecutionException, InvalidParseOperationException {
        LOGGER.debug("Exec db insert object group request: %s", requestParsers);
        boolean withRefreshIndex = true;
        Integer tenantId = ParameterHelper.getTenantParameter();
        List<ObjectGroup> objectGroups = new ArrayList<>();

        for (InsertParserMultiple insertParserMultiple : requestParsers) {
            InsertToMongodb requestToMongodb = (InsertToMongodb) RequestToMongodb.getRequestToMongoDb(
                insertParserMultiple
            );
            final ObjectGroup og = new ObjectGroup(requestToMongodb.getFinalData());

            setDateCreationAndModification(og);

            objectGroups.add(og);
        }

        Stopwatch mongoWatch = Stopwatch.createStarted();
        mongoDbObjectGroupRepository.insert(objectGroups);
        PerformanceLogger.getInstance()
            .log("STP_OBJ_STORING", "OG_METADATA_INDEXATION", "storeMongo", mongoWatch.elapsed(TimeUnit.MILLISECONDS));

        persistInElasticSearch(
            MetadataCollections.OBJECTGROUP,
            tenantId,
            objectGroups,
            "STP_OBJ_STORING",
            "OG_METADATA_INDEXATION",
            withRefreshIndex
        );
    }

    private void persistInElasticSearch(
        MetadataCollections collection,
        Integer tenantId,
        List<? extends MetadataDocument<?>> documents,
        String logKey,
        String logAction,
        boolean withRefreshIndex
    ) throws MetaDataExecutionException {
        Stopwatch stopWatch = Stopwatch.createStarted();

        collection
            .getEsClient()
            .insertFullDocumentsWithRefreshSettings(collection, tenantId, documents, withRefreshIndex);

        PerformanceLogger.getInstance()
            .log(logKey, logAction, "storeElastic", stopWatch.elapsed(TimeUnit.MILLISECONDS));
    }

    /**
     * Finalize the queries with last True Delete
     *
     * @param requestToMongodb
     * @param last
     * @return the final Result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result<MetadataDocument<?>> lastDeleteFilterProjection(
        DeleteToMongodb requestToMongodb,
        Result<MetadataDocument<?>> last
    ) throws MetaDataExecutionException {
        final Bson roots = QueryToMongodb.getRoots(ID, last.getCurrentIds());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("To Delete: " + MongoDbHelper.bsonToString(roots, false));
        }
        if (!requestToMongodb.isMultiple() && last.getNbResult() > 1) {
            throw new MetaDataExecutionException(
                "Delete Request is not multiple but found multiples entities to delete"
            );
        }
        final FILTERARGS model = requestToMongodb.model();
        try {
            if (model == FILTERARGS.UNITS) {
                final DeleteResult result = MongoDbMetadataHelper.delete(
                    MetadataCollections.UNIT,
                    roots,
                    last.getCurrentIds().size()
                );
                if (result.getDeletedCount() != last.getNbResult()) {
                    LOGGER.warn("Deleted items different than specified");
                }
                removeUnitIndexFields(last);
                return last;
            }
            // OBJECTGROUPS:
            final DeleteResult result = MongoDbMetadataHelper.delete(
                MetadataCollections.OBJECTGROUP,
                roots,
                last.getCurrentIds().size()
            );
            if (result.getDeletedCount() != last.getNbResult()) {
                LOGGER.warn("Deleted items different than specified");
            }
            removeOGIndexFields(last);
            last.setTotal(last.getNbResult());
            return last;
        } catch (final MetaDataExecutionException e) {
            throw e;
        } catch (final Exception e) {
            throw new MetaDataExecutionException("Delete concern", e);
        }
    }

    /**
     * Inserts a unit
     *
     * @param requests list of unit insert requests
     * @throws MetaDataExecutionException when insert on metadata collection exception occurred
     * @throws MetaDataNotFoundException when metadata not found exception
     */
    public void execInsertUnitRequests(List<InsertParserMultiple> requests)
        throws MetaDataExecutionException, MetaDataNotFoundException {
        LOGGER.debug("Exec db insert unit request: %s", requests);
        boolean withRefreshIndex = true;
        List<Unit> unitToSave = Lists.newArrayList();
        Map<String, ObjectGroupGraphUpdates> objectGroupGraphUpdatesMap = new HashMap<>();
        final Integer tenantId = ParameterHelper.getTenantParameter();

        try (GraphLoader graphLoader = new GraphLoader(mongoDbUnitRepository)) {
            Set<String> allRoots = new HashSet<>();
            for (InsertParserMultiple entry : requests) {
                allRoots.addAll(entry.getRequest().getRoots());
            }

            Stopwatch loadParentAU = Stopwatch.createStarted();
            Map<String, UnitGraphModel> parentGraphs = graphLoader.loadGraphInfo(allRoots);
            PerformanceLogger.getInstance()
                .log(
                    "STP_UNIT_METADATA",
                    "UNIT_METADATA_INDEXATION",
                    "loadParentAU",
                    loadParentAU.elapsed(TimeUnit.MILLISECONDS)
                );

            Stopwatch computeAU = Stopwatch.createStarted();

            for (InsertParserMultiple entry : requests) {
                final Unit unit = new Unit(entry.getRequest().getData());
                Set<String> roots = entry.getRequest().getRoots();

                UnitGraphModel unitGraphModel = new UnitGraphModel(unit);
                roots.forEach(parentId -> {
                    UnitGraphModel parentGraphModel = parentGraphs.get(parentId);
                    unitGraphModel.addParent(parentGraphModel);
                });

                unit.mergeWith(unitGraphModel);
                setDateCreationAndModification(unit);
                List<String> virtualFields = virtualPathsManager.getVirtualPathsFields();
                if (CollectionUtils.isNotEmpty(virtualFields)) {
                    fillVirtualPaths(unit, virtualFields);
                }
                // save mongo
                unitToSave.add(unit);

                String ogId = unit.getString(MetadataDocument.OG);
                if (StringUtils.isNotEmpty(ogId)) {
                    ObjectGroupGraphUpdates objectGroupGraphUpdates = objectGroupGraphUpdatesMap.computeIfAbsent(
                        ogId,
                        id -> new ObjectGroupGraphUpdates()
                    );
                    objectGroupGraphUpdates.buildParentGraph(unit);
                }
            }
            PerformanceLogger.getInstance()
                .log(
                    "STP_UNIT_METADATA",
                    "UNIT_METADATA_INDEXATION",
                    "computeAU",
                    computeAU.elapsed(TimeUnit.MILLISECONDS)
                );

            if (!unitToSave.isEmpty()) {
                Stopwatch saveAU = Stopwatch.createStarted();
                mongoDbUnitRepository.insert(unitToSave);
                PerformanceLogger.getInstance()
                    .log(
                        "STP_UNIT_METADATA",
                        "UNIT_METADATA_INDEXATION",
                        "saveUnitInMongo",
                        saveAU.elapsed(TimeUnit.MILLISECONDS)
                    );

                persistInElasticSearch(
                    MetadataCollections.UNIT,
                    tenantId,
                    unitToSave,
                    "STP_UNIT_METADATA",
                    "UNIT_METADATA_INDEXATION",
                    withRefreshIndex
                );
            }

            if (!objectGroupGraphUpdatesMap.isEmpty()) {
                Stopwatch saveGOT = Stopwatch.createStarted();
                Map<String, Bson> updates = objectGroupGraphUpdatesMap
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, item -> item.getValue().toBsonUpdate()));
                mongoDbObjectGroupRepository.update(updates);
                PerformanceLogger.getInstance()
                    .log(
                        "STP_UNIT_METADATA",
                        "UNIT_METADATA_INDEXATION",
                        "saveGOTInMongo",
                        saveGOT.elapsed(TimeUnit.MILLISECONDS)
                    );

                saveGOT = Stopwatch.createStarted();
                Collection<ObjectGroup> objectGroups = mongoDbObjectGroupRepository.selectByIds(updates.keySet(), null);
                MetadataCollections.OBJECTGROUP.getEsClient()
                    .insertFullDocuments(MetadataCollections.OBJECTGROUP, tenantId, objectGroups);
                PerformanceLogger.getInstance()
                    .log(
                        "STP_UNIT_METADATA",
                        "UNIT_METADATA_INDEXATION",
                        "saveGOTInElastic",
                        saveGOT.elapsed(TimeUnit.MILLISECONDS)
                    );
            }
        } catch (final MongoException e) {
            throw new MetaDataExecutionException("Insert concern", e);
        }
    }

    private void setDateCreationAndModification(MetadataDocument<? extends MetadataDocument<?>> metadataDocument) {
        final LocalDateTime now = LocalDateUtil.now();
        metadataDocument.setApproximateCreationDate(now);
        metadataDocument.setApproximateUpdateDate(now);
    }

    /**
     * Delete units
     */
    public void deleteUnits(List<String> documentsToDelete) throws MetaDataExecutionException {
        if (documentsToDelete.isEmpty()) {
            return;
        }

        Integer tenantId = ParameterHelper.getTenantParameter();

        MetadataCollections.UNIT.getEsClient().deleteBulkUnitsEntriesIndexes(documentsToDelete, tenantId);

        List<Unit> documents = new ArrayList<>();
        for (String id : documentsToDelete) {
            documents.add((Unit) new Unit().append(ID, id).append(TENANT_ID, tenantId));
        }
        mongoDbUnitRepository.delete(documents);
    }

    /**
     * Delete object groups
     */
    public void deleteObjectGroups(List<String> documentsToDelete) throws MetaDataExecutionException {
        if (documentsToDelete.isEmpty()) {
            return;
        }

        Integer tenantId = ParameterHelper.getTenantParameter();

        MetadataCollections.OBJECTGROUP.getEsClient().deleteBulkOGEntriesIndexes(documentsToDelete, tenantId);

        List<ObjectGroup> documents = new ArrayList<>();
        for (String id : documentsToDelete) {
            documents.add((ObjectGroup) new ObjectGroup().append(ID, id).append(TENANT_ID, tenantId));
        }

        mongoDbObjectGroupRepository.delete(documents);
    }

    public void clearEsScrollId(String scrollId) {
        try {
            MetadataCollections.UNIT.getEsClient().clearScroll(scrollId);
        } catch (DatabaseException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void fillVirtualPaths(Unit archiveUnit, List<String> virtualPathFields) {
        archiveUnit.remove(SedaConstants.VIRTUAL_UPS);

        for (String virtualField : virtualPathFields) {
            if (!archiveUnit.containsKey(virtualField)) {
                continue;
            }
            List<String> virtualPathsElts = extractFieldValues(archiveUnit, virtualField);
            if (CollectionUtils.isEmpty(virtualPathsElts)) {
                continue;
            }
            Set<String> virtualPathsSet = buildAllPaths(virtualPathsElts);
            archiveUnit.put(SedaConstants.VIRTUAL_UPS, virtualPathsSet);
        }
    }

    private Set<String> buildAllPaths(List<String> paths) {
        Set<String> virtualPathsSet = new HashSet<>();

        for (String path : paths) {
            if (StringUtils.isBlank(path)) {
                continue;
            }

            List<String> parts = Arrays.asList(path.split(PATH_SEPARATOR));
            StringBuilder currentPath = new StringBuilder();
            parts
                .stream()
                .filter(StringUtils::isNotBlank)
                .forEach(part -> {
                    currentPath.append(PATH_SEPARATOR).append(part.trim());
                    virtualPathsSet.add(String.valueOf(currentPath));
                });
        }
        return virtualPathsSet;
    }

    private List<String> extractFieldValues(Unit unitDoc, String fieldName) {
        Object val = unitDoc.get(fieldName);

        if (val instanceof String str) {
            return List.of(str);
        } else if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        } else if (val instanceof Map<?, ?> map) {
            return map.values().stream().map(Object::toString).toList();
        } else if (val != null) {
            return List.of(val.toString());
        } else throw new IllegalArgumentException("Could not extract field value for field " + fieldName);
    }

    private List<String> toStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || node.isNull()) {
            return list;
        }
        if (node.isArray()) {
            for (JsonNode element : node) {
                list.add(element.asText());
            }
        } else {
            list.add(node.asText());
        }
        return list;
    }

    private void fillVirtualPaths(JsonNode archiveUnit, List<String> virtualPathFields) {
        ObjectNode archiveUnitNode = (ObjectNode) archiveUnit;
        archiveUnitNode.remove(SedaConstants.VIRTUAL_UPS);
        for (String virtualField : virtualPathFields) {
            if (!archiveUnit.has(virtualField)) {
                continue;
            }
            JsonNode pathsNode = archiveUnitNode.get(virtualField);
            Set<String> virtualPathsSet = buildAllPaths(toStringList(pathsNode));

            ArrayNode pathsArray = JsonHandler.createArrayNode();
            for (String path : virtualPathsSet) {
                pathsArray.add(path);
            }
            archiveUnitNode.set(SedaConstants.VIRTUAL_UPS, pathsArray);
        }
    }
}
