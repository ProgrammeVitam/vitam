/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
/**
 *
 */
package fr.gouv.vitam.metadata.core.database.collections;

import com.amazonaws.util.CollectionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.DeleteMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.parser.query.PathQuery;
import fr.gouv.vitam.common.database.parser.query.helper.QueryDepthHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.MongoDbInMemory;
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
import fr.gouv.vitam.common.exception.ArchiveUnitOntologyValidationException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationStatus;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.metadata.core.graph.GraphLoader;
import fr.gouv.vitam.metadata.core.model.UpdatedDocument;
import fr.gouv.vitam.metadata.core.trigger.ChangesTrigger;
import fr.gouv.vitam.metadata.core.trigger.ChangesTriggerConfigFileException;
import org.apache.commons.lang.StringUtils;
import org.bson.conversions.Bson;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Accumulators.addToSet;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.push;

/**
 * DB Request using MongoDB only
 */
public class DbRequest {
    private static final String QUERY2 = "query: ";

    private static final String WHERE_PREVIOUS_RESULT_WAS = "where_previous_result_was: ";

    private static final String FROM2 = "from: ";

    private static final String NO_RESULT_AT_RANK2 = "no_result_at_rank: ";

    private static final String NO_RESULT_TRUE = "no_result: true";

    private static final String WHERE_PREVIOUS_IS = " \n\twhere previous is ";

    private static final String FROM = " from ";

    private static final String NO_RESULT_AT_RANK = "No result at rank: ";

    private static final String DEPTH_ARRAY = "deptharray";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequest.class);
    private static final String
        CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S =
        "[Consistency Error] : The document guid=%s in ES is not in MongoDB anymore, tenant : %s, requestId : %s";

    private final MongoDbMetadataRepository<Unit> mongoDbUnitRepository;
    private final MongoDbMetadataRepository<ObjectGroup> mongoDbObjectGroupRepository;
    private final AdminManagementClientFactory adminManagementClientFactory;

    private ChangesTrigger changesTrigger = null;

    @VisibleForTesting
    DbRequest(MongoDbMetadataRepository<Unit> mongoDbUnitRepository,
        MongoDbMetadataRepository<ObjectGroup> mongoDbObjectGroupRepository,
        AdminManagementClientFactory adminManagementClientFactory) {
        this.mongoDbUnitRepository = mongoDbUnitRepository;
        this.mongoDbObjectGroupRepository = mongoDbObjectGroupRepository;
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    /**
     * Constructor
     */
    // TODO JE finish to refactor
    public DbRequest() {
        this(
            new MongoDbMetadataRepository<Unit>(() -> MetadataCollections.UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> MetadataCollections.OBJECTGROUP.getCollection()),
            AdminManagementClientFactory.getInstance());
    }

    /**
     * Constructor
     */
    public DbRequest(String fileNameTriggersConfig) throws ChangesTriggerConfigFileException {
        this(new MongoDbMetadataRepository<Unit>(() -> MetadataCollections.UNIT.getCollection()),
            new MongoDbMetadataRepository<ObjectGroup>(() -> MetadataCollections.OBJECTGROUP.getCollection()),
            AdminManagementClientFactory.getInstance());
        this.changesTrigger = new ChangesTrigger(fileNameTriggersConfig);
    }

    /**
     * Execute rule action on unit
     *
     * @param documentId the unitId
     * @param ruleActions the list of ruleAction (by category)
     * @param ontologyModels
     */
    public UpdatedDocument execRuleRequest(final String documentId, final RuleActions ruleActions,
        Map<String, DurationData> bindRuleToDuration,
        List<OntologyModel> ontologyModels)
        throws InvalidParseOperationException, MetaDataExecutionException, SchemaValidationException,
        ArchiveUnitOntologyValidationException,
        InvalidCreateOperationException, MetaDataNotFoundException {

        final Integer tenantId = ParameterHelper.getTenantParameter();


        SchemaValidationUtils validator;
        try {
            validator = new SchemaValidationUtils();
        } catch (FileNotFoundException | ProcessingException e) {
            LOGGER.debug("Unable to initialize Json Validator");
            throw new MetaDataExecutionException(e);
        }

        int tries = 0;

        while (tries < 3) {

            MongoCollection<MetadataDocument<?>> collection = MetadataCollections.UNIT.getCollection();
            MetadataDocument<?> document = collection.find(and(
                eq(MetadataDocument.ID, documentId),
                eq(MetadataDocument.TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            )).first();
            if (document == null) {
                throw new MetaDataNotFoundException("Document not found by id " + documentId);
            }

            final JsonNode jsonDocument = JsonHandler.toJsonNode(document);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("DEBUG update {} to update to {}", jsonDocument,
                    JsonHandler.prettyPrint(ruleActions));
            }
            final MongoDbInMemory mongoInMemory = new MongoDbInMemory(jsonDocument);

            // Add operationId to #operations
            UpdateMultiQuery updateQuery = new UpdateMultiQuery();
            updateQuery
                .addActions(push(VitamFieldsHelper.operations(), VitamThreadUtils.getVitamSession().getRequestId()));

            final RequestParserMultiple updateRequest = new UpdateParserMultiple(new MongoDbVarNameAdapter());
            updateRequest.parse(updateQuery.getFinalUpdateById());
            mongoInMemory.getUpdateJson(updateRequest);

            // Update rules
            final ObjectNode updatedJsonDocument =
                (ObjectNode) mongoInMemory.getUpdateJsonForRule(ruleActions, bindRuleToDuration);

            Integer documentVersion = document.getVersion();
            int newDocumentVersion = documentVersion + 1;
            Integer atomicVersion = document.getAtomicVersion();
            int newAtomicVersion = atomicVersion == null ? newDocumentVersion : atomicVersion + 1;

            updatedJsonDocument.put(VitamDocument.VERSION, newDocumentVersion);
            updatedJsonDocument.put(MetadataDocument.ATOMIC_VERSION, newAtomicVersion);

            Unit updatedDocument = new Unit(updatedJsonDocument);

            JsonNode aupSchemaIdNode =
                updatedJsonDocument.remove(SchemaValidationUtils.TAG_SCHEMA_VALIDATION);
            if (!ontologyModels.isEmpty()) {
                validateAndUpdateOntology(updatedJsonDocument, validator, ontologyModels);
            }
            SchemaValidationStatus status = validator.validateInsertOrUpdateUnit(updatedJsonDocument.deepCopy());

            if (!SchemaValidationStatusEnum.VALID.equals(status.getValidationStatus())) {
                throw new SchemaValidationException("Unable to validate updated Unit " + status.getValidationMessage());
            }

            if (aupSchemaIdNode != null) {
                String aupSchemaId = aupSchemaIdNode.isArray() ?
                    (aupSchemaIdNode.get(0) != null ? aupSchemaIdNode.get(0).asText() : null)
                    : aupSchemaIdNode.asText();

                if (aupSchemaId != null && !aupSchemaId.isEmpty()) {
                    // Call AdminClient to get AUP info
                    JsonNode aupSchema = extractAUPSchema(aupSchemaId);
                    validateOtherExternalSchema(updatedJsonDocument, aupSchema);
                    updatedDocument.remove(SchemaValidationUtils.TAG_SCHEMA_VALIDATION);
                }
            }

            // Make Update
            final Bson condition;
            if (atomicVersion == null) {
                condition = and(
                    eq(MetadataDocument.ID, documentId),
                    eq(MetadataDocument.TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId()),
                    exists(MetadataDocument.ATOMIC_VERSION, false));
            } else {
                condition = and(
                    eq(MetadataDocument.ID, documentId),
                    eq(MetadataDocument.TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId()),
                    eq(MetadataDocument.ATOMIC_VERSION, atomicVersion));
            }

            LOGGER.debug("DEBUG update {}", updatedJsonDocument);
            UpdateResult result = collection.replaceOne(condition, updatedDocument);
            if (result.getModifiedCount() == 1) {
                indexFieldsUpdated(updatedDocument, tenantId);
                return new UpdatedDocument(documentId, jsonDocument, JsonHandler.toJsonNode(updatedDocument));
            }
            tries++;
        }

        throw new MetaDataExecutionException("Can not modify document " + documentId);
    }

    private JsonNode extractAUPSchema(String archiveUnitProfileIdentifier)
        throws MetaDataExecutionException {

        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(ArchiveUnitProfile.IDENTIFIER, archiveUnitProfileIdentifier));
            RequestResponse<ArchiveUnitProfileModel> response =
                adminClient.findArchiveUnitProfiles(select.getFinalSelect());

            List<ArchiveUnitProfileModel> results =
                ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults();

            if (!response.isOk() || results.isEmpty()) {
                throw new MetaDataExecutionException("Archive unit profile could not be found");
            }

            ArchiveUnitProfileModel archiveUnitProfile = results.get(0);
            return JsonHandler.getFromString(archiveUnitProfile.getControlSchema());

        } catch (AdminManagementClientServerException | InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new MetaDataExecutionException(e);
        }

    }


    /**
     * The request should be already analyzed.
     *
     * @param requestParser the RequestParserMultiple to execute
     *
     * @return the Result
     *
     * @throws MetaDataExecutionException     when select/update/delete on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws BadRequestException
     */
    public Result execRequest(final RequestParserMultiple requestParser)
        throws MetaDataExecutionException,
        InvalidParseOperationException, BadRequestException,
        VitamDBException {
        final RequestMultiple request = requestParser.getRequest();
        final RequestToAbstract requestToMongodb = RequestToMongodb.getRequestToMongoDb(requestParser);
        final int maxQuery = request.getNbQueries();
        boolean checkConsistency = false;
        Result<MetadataDocument<?>> roots;
        if (requestParser.model() == FILTERARGS.UNITS) {
            roots = checkUnitStartupRoots(requestParser);
        } else {
            // OBJECTGROUPS:
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("OBJECTGROUPS DbRequest: %s", requestParser.toString()));
            }
            roots = checkObjectGroupStartupRoots(requestParser);
        }

        Result<MetadataDocument<?>> result = roots;
        int rank = 0;
        // if roots is empty, check if first query gives a non empty roots (empty query allowed for insert)
        if (result.getCurrentIds().isEmpty() && maxQuery > 0) {
            final Result<MetadataDocument<?>> newResult = executeQuery(requestParser, requestToMongodb, rank, result);

            if (newResult != null && !newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
                checkConsistency = true;
            } else {
                LOGGER.debug(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(requestParser.model())
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result).setTotal(newResult != null ? newResult.total : 0);
                return result;
            }
            LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
            rank++;
        }
        // Stops if no result (empty)
        for (; !result.getCurrentIds().isEmpty() && rank < maxQuery; rank++) {
            final Result<MetadataDocument<?>> newResult = executeQuery(requestParser, requestToMongodb, rank, result);
            if (newResult == null) {
                LOGGER.debug(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(result.type)
                    .addError(result.getCurrentIds().toString())
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            if (!newResult.getCurrentIds().isEmpty() && !newResult.isError()) {
                result = newResult;
            } else {
                LOGGER.debug(
                    NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
                // XXX TODO P1 should be adapted to have a correct error feedback
                result = new ResultError(newResult.type)
                    .addError(newResult != null ? newResult.getCurrentIds().toString() : NO_RESULT_TRUE)
                    .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                    .addError(WHERE_PREVIOUS_RESULT_WAS + result);
                return result;
            }
            LOGGER.debug("Query: {}\n\tResult: {}", requestParser, result);
        }
        // others do not allow empty result
        if (result.getCurrentIds().isEmpty()) {
            LOGGER.debug(NO_RESULT_AT_RANK + rank + FROM + requestParser + WHERE_PREVIOUS_IS + result);
            // XXX TODO P1 should be adapted to have a correct error feedback
            result = new ResultError(result.type)
                .addError(result != null ? result.getCurrentIds().toString() : NO_RESULT_TRUE)
                .addError(NO_RESULT_AT_RANK2 + rank).addError(FROM2 + requestParser)
                .addError(WHERE_PREVIOUS_RESULT_WAS + result);
            return result;
        }
        if (request instanceof DeleteMultiQuery) {
            final Result<MetadataDocument<?>> newResult =
                lastDeleteFilterProjection((DeleteToMongodb) requestToMongodb, result);
            if (newResult != null) {
                result = newResult;
            }
        } else {
            // Select part
            // get facets
            // get result
            final Result<MetadataDocument<?>> newResult =
                lastSelectFilterProjection((SelectToMongodb) requestToMongodb, result, checkConsistency);

            if (newResult != null) {
                result = newResult;
            }
        }
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Results: {}", result);
        }
        return result;
    }

    public UpdatedDocument execUpdateRequest(final RequestParserMultiple requestParser, String documentId,
        List<OntologyModel> ontologyModels, MetadataCollections metadataCollection)
        throws MetaDataExecutionException, ArchiveUnitOntologyValidationException,
        InvalidParseOperationException, MetaDataNotFoundException {

        final UpdatedDocument result =
            updateDocumentWithRetries(documentId, requestParser, ontologyModels, metadataCollection);
        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Results: {}", result);
        }
        return result;
    }

    /**
     * Check Unit at startup against Roots
     *
     * @param request
     *
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
     *
     * @return the valid root ids
     */
    private Result<MetadataDocument<?>> checkObjectGroupStartupRoots(final RequestParserMultiple request) {
        // TODO P1 add unit tests
        final Set<String> roots = request.getRequest().getRoots();
        return MongoDbMetadataHelper.createOneResult(FILTERARGS.OBJECTGROUPS, roots);
    }

    /**
     * Check Unit parents against Roots
     *
     * @param current         set of result id
     * @param defaultStartSet
     *
     * @return the valid root ids set
     */
    private Set<String> checkUnitAgainstRoots(final Set<String> current,
        final Result<MetadataDocument<?>> defaultStartSet) {
        // roots
        if (defaultStartSet == null || defaultStartSet.getCurrentIds().isEmpty()) {
            // no limitation: using roots
            return current;
        }
        // TODO P1 add unit tests
        @SuppressWarnings("unchecked")
        final FindIterable<Unit> iterable =
            (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.UNIT,
                MongoDbMetadataHelper.queryForAncestorsOrSame(current, defaultStartSet.getCurrentIds()),
                MongoDbMetadataHelper.ID_PROJECTION);
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
     * @param rank             current rank query
     * @param previous         previous Result from previous level (except in level == 0 where it is the subset of valid roots)
     *
     * @return the new Result from this request
     *
     * @throws MetaDataExecutionException
     * @throws InvalidParseOperationException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> executeQuery(final RequestParserMultiple requestParser,
        final RequestToAbstract requestToMongodb, final int rank,
        final Result<MetadataDocument<?>> previous)
        throws MetaDataExecutionException, InvalidParseOperationException, BadRequestException {
        final Query realQuery = requestToMongodb.getNthQuery(rank);
        final boolean isLastQuery = requestToMongodb.getNbQueries() == rank + 1;
        List<SortBuilder> sorts = null;
        List<AggregationBuilder> facets = null;
        int limit = -1;
        int offset = -1;
        String scrollId = requestParser.getFinalScrollId();
        Integer scrollTimeout = requestParser.getFinalScrollTimeout();
        final Integer tenantId = ParameterHelper.getTenantParameter();
        final FILTERARGS collectionType = requestToMongodb.model();
        if (requestToMongodb instanceof SelectToMongodb && isLastQuery) {
            VitamCollection.setMatch(false);
            sorts =
                QueryToElasticsearch.getSorts(requestParser, realQuery.isFullText() || VitamCollection.containMatch(),
                    collectionType.equals(FILTERARGS.UNITS) ? MetadataCollections.UNIT.useScore()
                        : MetadataCollections.OBJECTGROUP.useScore());
            if (FILTERARGS.UNITS.equals(collectionType) || FILTERARGS.OBJECTGROUPS.equals(collectionType)) {
                facets = QueryToElasticsearch.getFacets(requestParser);
            }
            VitamCollection.setMatch(false);
            limit = requestToMongodb.getFinalLimit();
            offset = requestToMongodb.getFinalOffset();
        }

        if (GlobalDatasDb.PRINT_REQUEST && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Rank: " + rank + "\n\tPrevious: " + previous + "\n\tRequest: " + realQuery.getCurrentQuery());
        }
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
        final int relativeDepth = QueryDepthHelper.HELPER.getRelativeDepth(realQuery);
        Result result;
        try {
            if (collectionType == FILTERARGS.UNITS) {
                if (exactDepth > 0) {
                    // Exact Depth request (descending)
                    LOGGER.debug("Unit Exact Depth request (descending)");
                    result = exactDepthUnitQuery(realQuery, previous, exactDepth, tenantId, sorts,
                        offset, limit, facets, scrollId, scrollTimeout);
                } else if (relativeDepth != 0) {
                    // Relative Depth request (ascending or descending)
                    LOGGER.debug("Unit Relative Depth request (ascending or descending)");
                    result =
                        relativeDepthUnitQuery(realQuery, previous, relativeDepth, tenantId, sorts,
                            offset, limit, facets, scrollId, scrollTimeout);
                } else {
                    // Current sub level request
                    LOGGER.debug("Unit Current sub level request");
                    result = sameDepthUnitQuery(realQuery, previous, tenantId, sorts, offset,
                        limit, facets, scrollId, scrollTimeout);
                }
            } else {
                // OBJECTGROUPS
                // No depth at all
                // FIXME later on see if we should support depth
                LOGGER.debug("ObjectGroup No depth at all");
                result = objectGroupQuery(realQuery, previous, tenantId, sorts, offset,
                    limit, scrollId, scrollTimeout, facets);
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
     *
     * @return the associated Result
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> exactDepthUnitQuery(Query realQuery, Result<MetadataDocument<?>> previous,
        int exactDepth, Integer tenantId, final List<SortBuilder> sorts, final int offset, final int limit,
        final List<AggregationBuilder> facets, final String scrollId, final Integer scrollTimeout)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES only
        final BoolQueryBuilder roots =
            new BoolQueryBuilder().must(QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(exactDepth).gte(0))
                .must(QueryBuilders.rangeQuery(Unit.MINDEPTH).lte(exactDepth).gte(0));
        if (!previous.getCurrentIds().isEmpty()) {
            roots.must(QueryToElasticsearch.getRoots(MetadataDocument.UP, previous.getCurrentIds()));
        }

        // lets add the query on the tenant
        BoolQueryBuilder query = new BoolQueryBuilder()
            .must(QueryToElasticsearch.getCommand(realQuery, new MongoDbVarNameAdapter()))
            .filter(QueryBuilders.termQuery(MetadataDocument.TENANT_ID, tenantId))
            .filter(QueryBuilders.termQuery(Unit.UNITUPS + "." + exactDepth, previous.getCurrentIds()));

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Req1LevelMD: {}", query);
        }

        final Result<MetadataDocument<?>> result =
            MetadataCollections.UNIT.getEsClient().search(MetadataCollections.UNIT, tenantId,
                VitamCollection.getTypeunique(), query, sorts, offset, limit, facets, scrollId, scrollTimeout);

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.warn("UnitExact: {}", result);
        }
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
     *
     * @return the associated Result
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> relativeDepthUnitQuery(Query realQuery, Result<MetadataDocument<?>> previous,
        int relativeDepth, Integer tenantId, final List<SortBuilder> sorts, final int offset,
        final int limit, final List<AggregationBuilder> facets, final String scrollId, final Integer scrollTimeout)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES only
        QueryBuilder roots;

        // lets add the query on the tenant
        BoolQueryBuilder query = new BoolQueryBuilder()
            .must(QueryToElasticsearch.getCommand(realQuery, new MongoDbVarNameAdapter()));

        if (previous.getCurrentIds().isEmpty()) {
            if (relativeDepth < 1) {
                query.filter(QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(1).gte(0));
            } else {
                query.filter(QueryBuilders.rangeQuery(Unit.MAXDEPTH).lte(relativeDepth + 1).gte(0));
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
                roots = QueryToElasticsearch.getRoots(MetadataDocument.ID, fathers);
                query.filter(roots);
            }
        }

        query.filter(QueryBuilders.termQuery(MetadataDocument.TENANT_ID, tenantId));

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("Req1LevelMD: {}", query);
        }

        final Result<MetadataDocument<?>> result =
            MetadataCollections.UNIT.getEsClient().search(MetadataCollections.UNIT, tenantId,
                VitamCollection.getTypeunique(), query, sorts, offset, limit, facets, scrollId, scrollTimeout);

        if (GlobalDatasDb.PRINT_REQUEST) {
            LOGGER.debug("UnitRelative: {}", result);
        }

        return result;
    }

    /**
     * Aggregate Unit Depths according to parent relative Depth
     *
     * @param ids
     * @param relativeDepth
     *
     * @return the aggregate set of multi level parents for this relativeDepth
     */
    protected Set<String> aggregateUnitDepths(Collection<String> ids, int relativeDepth) {
        // TODO P1 add unit tests
        // Select all items from ids
        final Bson match = match(in(MetadataDocument.ID, ids));
        // aggregate all UNITDEPTH in one (ignoring depth value)
        final Bson group = group(new BasicDBObject(MetadataDocument.ID, "all"),
            addToSet(DEPTH_ARRAY, BuilderToken.DEFAULT_PREFIX + Unit.UNITDEPTHS));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Depth: {} {}", MongoDbHelper.bsonToString(match, false),
                MongoDbHelper.bsonToString(group, false));
        }
        final List<Bson> pipeline = Arrays.asList(match, group);
        @SuppressWarnings("unchecked")
        final AggregateIterable<Unit> aggregateIterable =
            MetadataCollections.UNIT.getCollection().aggregate(pipeline);
        final Unit aggregate = aggregateIterable.first();
        final Set<String> set = new HashSet<>();
        if (aggregate != null) {
            @SuppressWarnings("unchecked")
            final List<Map<String, List<String>>> array =
                (List<Map<String, List<String>>>) aggregate.get(DEPTH_ARRAY);
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
     *
     * @return the associated Result
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> sameDepthUnitQuery(Query realQuery, Result<MetadataDocument<?>> previous,
        Integer tenantId, final List<SortBuilder> sorts, final int offset, final int limit,
        final List<AggregationBuilder> facets, final String scrollId, final Integer scrollTimeout)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES
        final QueryBuilder query = QueryToElasticsearch.getCommand(realQuery, new MongoDbVarNameAdapter());
        QueryBuilder finalQuery;
        LOGGER.debug("DEBUG prev {} RealQuery {}", previous.getCurrentIds(), realQuery);
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final QueryBuilder roots = QueryToElasticsearch.getRoots(MetadataDocument.ID, previous.getCurrentIds());
            finalQuery = QueryBuilders.boolQuery().must(query).must(roots);
        }
        if (tenantId != null) {
            // lets add the query on the tenant
            finalQuery = new BoolQueryBuilder().must(finalQuery)
                .must(QueryBuilders.termQuery(MetadataDocument.TENANT_ID, tenantId));
        }

        LOGGER.debug(QUERY2 + "{}", finalQuery);
        return MetadataCollections.UNIT.getEsClient().search(MetadataCollections.UNIT, tenantId,
            VitamCollection.getTypeunique(), finalQuery, sorts, offset, limit, facets, scrollId, scrollTimeout);
    }

    /**
     * Execute one relative Depth ObjectGroup Query
     *
     * @param realQuery
     * @param previous  units, Note: only immediate Unit parents are allowed
     * @param tenantId
     * @param sorts
     * @param offset
     * @param limit
     *
     * @return the associated Result
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws BadRequestException
     */
    protected Result<MetadataDocument<?>> objectGroupQuery(Query realQuery, Result<MetadataDocument<?>> previous,
        Integer tenantId, final List<SortBuilder> sorts, final int offset, final int limit,
        final String scrollId, final Integer scrollTimeout, final List<AggregationBuilder> facets)
        throws InvalidParseOperationException, MetaDataExecutionException, BadRequestException {
        // ES
        final QueryBuilder query = QueryToElasticsearch.getCommand(realQuery, new MongoDbVarNameAdapter());
        QueryBuilder finalQuery;
        if (previous.getCurrentIds().isEmpty()) {
            finalQuery = query;
        } else {
            final QueryBuilder roots;
            if (FILTERARGS.UNITS.equals(previous.getType())) {
                roots = QueryToElasticsearch.getRoots(MetadataDocument.UP, previous.getCurrentIds());
            } else {
                roots = QueryToElasticsearch.getRoots(MetadataDocument.ID, previous.getCurrentIds());
            }
            finalQuery = QueryBuilders.boolQuery().must(query).must(roots);
        }
        if (tenantId != null) {
            // lets add the query on the tenant
            finalQuery = new BoolQueryBuilder().must(finalQuery)
                .must(QueryBuilders.termQuery(MetadataDocument.TENANT_ID, tenantId));
        }

        LOGGER.debug(QUERY2 + "{}", finalQuery);
        return MetadataCollections.OBJECTGROUP.getEsClient().search(MetadataCollections.OBJECTGROUP, tenantId,
            VitamCollection.getTypeunique(), finalQuery, sorts, offset, limit, facets, scrollId, scrollTimeout);
    }

    /**
     * Finalize the queries with last True Select
     *
     * @param requestToMongodb
     * @param last
     *
     * @return the final Result
     *
     * @throws InvalidParseOperationException
     */
    protected Result<MetadataDocument<?>> lastSelectFilterProjection(SelectToMongodb requestToMongodb,
        Result<MetadataDocument<?>> last, boolean checkConsistency)
        throws InvalidParseOperationException, VitamDBException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        final Bson projection = requestToMongodb.getFinalProjection();
        final boolean isIdIncluded = requestToMongodb.idWasInProjection();
        final FILTERARGS model = requestToMongodb.model();
        final List<String> desynchronizedResults = new ArrayList<>();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("To Select: " + MongoDbHelper.bsonToString(roots, false) + " " +
                (projection != null ? MongoDbHelper.bsonToString(projection, false) : ""));
        }
        if (model == FILTERARGS.UNITS) {
            final Map<String, Unit> units = new HashMap<>();
            @SuppressWarnings("unchecked")
            final FindIterable<Unit> iterable =
                (FindIterable<Unit>) MongoDbMetadataHelper.select(MetadataCollections.UNIT,
                    roots, projection, null, -1, -1);
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
                    if (VitamConfiguration.isExportScore() && MetadataCollections.UNIT.useScore() &&
                        requestToMongodb.isScoreIncluded()) {
                        Float score = 1F;
                        try {
                            score = last.scores.get(i);
                            if (score.isNaN()) {
                                score = 1F;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            SysErrLogger.FAKE_LOGGER.ignoreLog(e);

                        }
                        unit.append(VitamDocument.SCORE, score);
                    }
                    if (!isIdIncluded) {
                        unit.remove(VitamDocument.ID);
                    }
                    last.addFinal(unit);
                } else if (checkConsistency) {
                    // check the consistency between elasticSearch and MongoDB
                    desynchronizedResults.add(id);
                    // desynchronization logs
                    LOGGER.error(String.format(
                        CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S,
                        id, ParameterHelper.getTenantParameter(), VitamThreadUtils.getVitamSession().getRequestId()));
                }
            }

            // As soon as we detect a synchronization error MongoDB / ES, we return an error.
            if (!desynchronizedResults.isEmpty()) {
                throw new VitamDBException(
                    "[Consistency ERROR] : An internal data consistency error has been detected !");
            }

            return last;
        }
        // OBJECTGROUPS:
        final Map<String, ObjectGroup> obMap = new HashMap<>();
        @SuppressWarnings("unchecked")
        final FindIterable<ObjectGroup> iterable =
            (FindIterable<ObjectGroup>) MongoDbMetadataHelper.select(
                MetadataCollections.OBJECTGROUP,
                roots, projection, null, -1, -1);
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
                if (VitamConfiguration.isExportScore() && MetadataCollections.OBJECTGROUP.useScore() &&
                    requestToMongodb.isScoreIncluded()) {
                    Float score = 1F;
                    try {
                        score = last.scores.get(i);
                        if (score.isNaN()) {
                            score = 1F;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                    }
                    og.append(VitamDocument.SCORE, score);
                }
                if (!isIdIncluded) {
                    og.remove(VitamDocument.ID);
                }
                last.addFinal(og);
            } else if (checkConsistency) {
                // check the consistency between elasticSearch and MongoDB
                desynchronizedResults.add(id);
                // desynchronization logs
                LOGGER.error(String.format(
                    CONSISTENCY_ERROR_THE_DOCUMENT_GUID_S_IN_ES_IS_NOT_IN_MONGO_DB_ANYMORE_TENANT_S_REQUEST_ID_S,
                    id, ParameterHelper.getTenantParameter(), VitamThreadUtils.getVitamSession().getRequestId()));
            }
        }

        // As soon as we detect a synchronization error MongoDB / ES, we return an error.
        if (!desynchronizedResults.isEmpty()) {
            throw new VitamDBException(
                "[Consistency ERROR] : An internal data consistency error has been detected !");
        }

        return last;
    }

    private UpdatedDocument updateDocumentWithRetries(String documentId,
        RequestParserMultiple requestParser, List<OntologyModel> ontologyModels, MetadataCollections metadataCollection)
        throws InvalidParseOperationException, MetaDataExecutionException, ArchiveUnitOntologyValidationException,
        MetaDataNotFoundException {
        final Integer tenantId = ParameterHelper.getTenantParameter();

        SchemaValidationUtils validator;
        try {
            validator = new SchemaValidationUtils();
        } catch (FileNotFoundException | ProcessingException e) {
            throw new MetaDataExecutionException("Unable to initialize Json Validator", e);
        }

        int tries = 0;
        while (tries < 3) {

            MongoCollection<MetadataDocument<?>> collection = metadataCollection.getCollection();

            MetadataDocument<?> document = collection.find(and(
                eq(MetadataDocument.ID, documentId),
                eq(MetadataDocument.TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            )).first();

            if (document == null) {
                throw new MetaDataNotFoundException("Document not found by id " + documentId);
            }

            final Integer documentVersion = document.getVersion();

            final JsonNode jsonDocument = JsonHandler.toJsonNode(document);
            final MongoDbInMemory mongoInMemory = new MongoDbInMemory(jsonDocument);
            final ObjectNode updatedJsonDocument = (ObjectNode) mongoInMemory.getUpdateJson(requestParser);

            if (metadataCollection == MetadataCollections.UNIT && changesTrigger != null) {
                changesTrigger.trigger(jsonDocument, updatedJsonDocument);
            }

            int newDocumentVersion = incrementDocumentVersionIfRequired(metadataCollection, mongoInMemory, documentVersion);
            updatedJsonDocument.put(VitamDocument.VERSION, newDocumentVersion);

            Integer atomicVersion = document.getAtomicVersion();
            int newAtomicVersion = atomicVersion == null ? newDocumentVersion : atomicVersion + 1;
            updatedJsonDocument.put(MetadataDocument.ATOMIC_VERSION, newAtomicVersion);

            if (metadataCollection == MetadataCollections.UNIT) {
                JsonNode externalSchema =
                    updatedJsonDocument.remove(SchemaValidationUtils.TAG_SCHEMA_VALIDATION);
                if (!ontologyModels.isEmpty()) {
                    validateAndUpdateOntology(updatedJsonDocument, validator, ontologyModels);
                }
                SchemaValidationStatus status = validator.validateInsertOrUpdateUnit(updatedJsonDocument.deepCopy());
                if (!SchemaValidationStatusEnum.VALID.equals(status.getValidationStatus())) {
                    throw new MetaDataExecutionException(
                        "Unable to validate updated Unit " + status.getValidationMessage());
                }
                if (externalSchema != null && externalSchema.size() > 0) {
                    try {
                        validateOtherExternalSchema(updatedJsonDocument, externalSchema);
                    } catch (SchemaValidationException e) {
                        throw new MetaDataExecutionException(e);
                    }
                }
            }

            // Make Update
            final Bson condition;
            if (atomicVersion == null) {
                condition = and(eq(MetadataDocument.ID, documentId),
                    exists(MetadataDocument.ATOMIC_VERSION, false));
            } else {
                condition = and(eq(MetadataDocument.ID, documentId),
                    eq(MetadataDocument.ATOMIC_VERSION, atomicVersion));
            }
            LOGGER.debug("DEBUG update {}", updatedJsonDocument);
            MetadataDocument<?> finalDocument = (MetadataDocument<?>) document.newInstance(updatedJsonDocument);

            UpdateResult result = collection.replaceOne(condition, finalDocument);
            if (result.getModifiedCount() == 1) {

                if (metadataCollection == MetadataCollections.UNIT) {
                    indexFieldsUpdated(finalDocument, tenantId);
                } else {
                    indexFieldsOGUpdated(finalDocument, tenantId);
                }

                return new UpdatedDocument(documentId, jsonDocument, updatedJsonDocument);

            }
            tries++;
        }

        throw new MetaDataExecutionException("Can not modify document " + documentId);
    }

    private int incrementDocumentVersionIfRequired(MetadataCollections metadataCollection, MongoDbInMemory mongoInMemory,
        int documentVersion) {

        // FIXME : To avoid potential update loss for computed fields, we should make version field "non persisted"
        //  and always increment document version (DbRequest, and any other document update like graph computation,
        //  indexation, reconstruction...)

        Set<String> updatedFields = mongoInMemory.getUpdatedFields();

        Set<String> computedFields = (metadataCollection == MetadataCollections.UNIT) ?
            MetadataDocumentHelper.getComputedUnitFields() :
            MetadataDocumentHelper.getComputedObjectGroupFields();

        if (computedFields.containsAll(updatedFields)) {
            return documentVersion;
        } else {
            return documentVersion + 1;
        }
    }


    private void validateOtherExternalSchema(ObjectNode updatedJsonDocument, JsonNode schema)
        throws InvalidParseOperationException, MetaDataExecutionException, SchemaValidationException {
        try {
            SchemaValidationUtils validatorSecond =
                new SchemaValidationUtils(
                    schema.isArray()
                        ? schema.get(0).asText()
                        : schema.toString(),
                    true);
            updatedJsonDocument.remove(SchemaValidationUtils.TAG_SCHEMA_VALIDATION);
            SchemaValidationStatus status =
                validatorSecond.validateInsertOrUpdateUnit(updatedJsonDocument.deepCopy());
            if (!SchemaValidationStatusEnum.VALID.equals(status.getValidationStatus())) {
                throw new SchemaValidationException(
                    "Unable to validate updated Unit " + status.getValidationMessage());
            }
        } catch (FileNotFoundException | ProcessingException e) {
            LOGGER.debug("Unable to initialize External Json Validator");
            throw new MetaDataExecutionException(e);
        }
    }

    private void validateAndUpdateOntology(ObjectNode updatedJsonDocument,
        SchemaValidationUtils validator,
        List<OntologyModel> ontologies)
        throws ArchiveUnitOntologyValidationException {


        Map<String, OntologyModel> ontologiesByIdentifier;

            ontologiesByIdentifier =
                ontologies.stream().collect(Collectors.toMap(OntologyModel::getIdentifier, oM -> oM));

        List<String> errors = new ArrayList<>();
        // that means a transformation could be done so we need to process the full json
        validator.verifyAndReplaceFields(updatedJsonDocument, ontologiesByIdentifier, errors);

        if (!errors.isEmpty()) {
            // archive unit could not be transformed, so the error would be thrown later by the schema
            // validation verification
            String error = "Archive unit contains fields declared in ontology with a wrong format : " +
                CollectionUtils.join(errors, ",");
            throw new ArchiveUnitOntologyValidationException(error);
        }
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
     * indexFieldsOGUpdated : Update index OG related to Fields updated
     *
     * @param updatedDocument : contains the document to be indexed
     * @throws Exception
     */
    private void indexFieldsOGUpdated(MetadataDocument<?> updatedDocument, Integer tenantId)
        throws MetaDataExecutionException {

        MetadataCollections.OBJECTGROUP.getEsClient()
            .updateFullDocument(MetadataCollections.OBJECTGROUP, tenantId, updatedDocument.getId(), updatedDocument);
    }

    /**
     * removeOGIndexFields : remove index related to Fields deleted
     *
     * @param last : contains the Result to be removed
     *
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
     *
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
     *
     * @throws MetaDataExecutionException     when insert on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws MetaDataAlreadyExistException  when insert metadata exception
     * @throws MetaDataNotFoundException      when metadata not found exception
     */
    public void execInsertUnitRequest(InsertParserMultiple requestParser)
        throws MetaDataExecutionException, MetaDataNotFoundException, InvalidParseOperationException,
        MetaDataAlreadyExistException {

        LOGGER.debug("Exec db insert unit request: %s", requestParser);
        execInsertUnitRequests(Lists.newArrayList(requestParser));
    }

    /**
     * Inserts an object group
     *
     * @param requestParsers the list of InsertParserMultiple to execute
     *
     * @throws MetaDataExecutionException     when insert on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws MetaDataAlreadyExistException  when insert metadata exception
     */
    public void execInsertObjectGroupRequests(List<InsertParserMultiple> requestParsers)
        throws MetaDataExecutionException, InvalidParseOperationException, MetaDataAlreadyExistException {

        LOGGER.debug("Exec db insert object group request: %s", requestParsers);

        Integer tenantId = ParameterHelper.getTenantParameter();
        List<ObjectGroup> objectGroups = new ArrayList<>();

        for (InsertParserMultiple insertParserMultiple : requestParsers) {

            InsertToMongodb requestToMongodb =
                (InsertToMongodb) RequestToMongodb.getRequestToMongoDb(insertParserMultiple);
            objectGroups.add(new ObjectGroup(requestToMongodb.getFinalData()));

        }

        try {
            Stopwatch mongoWatch = Stopwatch.createStarted();
            mongoDbObjectGroupRepository.insert(objectGroups);
            PerformanceLogger.getInstance().log("STP_OBJ_STORING", "OG_METADATA_INDEXATION", "storeMongo",
                mongoWatch.elapsed(TimeUnit.MILLISECONDS));
        } catch (MetaDataAlreadyExistException e) {
            // Even ObjectGroup already exists in MongoDB, reindex in elastic search
            LOGGER.warn(e);
            persistInElasticSearch(MetadataCollections.OBJECTGROUP, tenantId, objectGroups, "STP_OBJ_STORING",
                "OG_METADATA_INDEXATION");
            throw e;
        }

        persistInElasticSearch(MetadataCollections.OBJECTGROUP, tenantId, objectGroups, "STP_OBJ_STORING",
            "OG_METADATA_INDEXATION");
    }

    private void persistInElasticSearch(MetadataCollections collection, Integer tenantId,
        List<? extends MetadataDocument> documents, String logKey, String logAction)
        throws MetaDataExecutionException {
        Stopwatch stopWatch = Stopwatch.createStarted();

        collection.getEsClient().insertFullDocuments(collection, tenantId, documents);

        PerformanceLogger.getInstance()
            .log(logKey, logAction, "storeElastic", stopWatch.elapsed(TimeUnit.MILLISECONDS));
    }

    /**
     * Finalize the queries with last True Delete
     *
     * @param requestToMongodb
     * @param last
     *
     * @return the final Result
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     */
    protected Result<MetadataDocument<?>> lastDeleteFilterProjection(DeleteToMongodb requestToMongodb,
        Result<MetadataDocument<?>> last)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final Bson roots = QueryToMongodb.getRoots(MetadataDocument.ID, last.getCurrentIds());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("To Delete: " + MongoDbHelper.bsonToString(roots, false));
        }
        if (!requestToMongodb.isMultiple() && last.getNbResult() > 1) {
            throw new MetaDataExecutionException(
                "Delete Request is not multiple but found multiples entities to delete");
        }
        final FILTERARGS model = requestToMongodb.model();
        try {
            if (model == FILTERARGS.UNITS) {
                final DeleteResult result = MongoDbMetadataHelper.delete(MetadataCollections.UNIT,
                    roots, last.getCurrentIds().size());
                if (result.getDeletedCount() != last.getNbResult()) {
                    LOGGER.warn("Deleted items different than specified");
                }
                removeUnitIndexFields(last);
                return last;
            }
            // TODO P1 add unit tests
            // OBJECTGROUPS:
            final DeleteResult result =
                MongoDbMetadataHelper.delete(MetadataCollections.OBJECTGROUP,
                    roots, last.getCurrentIds().size());
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
     * @param requestParsers list of InsertParserMultiple to execute
     *
     * @throws MetaDataExecutionException     when insert on metadata collection exception occurred
     * @throws InvalidParseOperationException when json data exception occurred
     * @throws MetaDataAlreadyExistException  when insert metadata exception
     * @throws MetaDataNotFoundException      when metadata not found exception
     */
    public void execInsertUnitRequests(Collection<InsertParserMultiple> requestParsers)
        throws MetaDataExecutionException, MetaDataNotFoundException, InvalidParseOperationException,
        MetaDataAlreadyExistException {

        LOGGER.debug("Exec db insert unit request: %s", requestParsers);

        List<Unit> unitToSave = Lists.newArrayList();
        Map<String, ObjectGroupGraphUpdates> objectGroupGraphUpdatesMap = new HashMap<>();
        final Integer tenantId = ParameterHelper.getTenantParameter();

        try (GraphLoader graphLoader = new GraphLoader(mongoDbUnitRepository)) {

            List<String> allRoots = new ArrayList<>();
            for (InsertParserMultiple requestParser : requestParsers) {
                allRoots.addAll(requestParser.getRequest().getRoots());
            }

            Stopwatch loadParentAU = Stopwatch.createStarted();
            Map<String, UnitGraphModel> parentGraphs = graphLoader.loadGraphInfo(allRoots);
            PerformanceLogger.getInstance().log("STP_UNIT_METADATA", "UNIT_METADATA_INDEXATION", "loadParentAU",
                loadParentAU.elapsed(TimeUnit.MILLISECONDS));

            Stopwatch computeAU = Stopwatch.createStarted();

            for (InsertParserMultiple requestParser : requestParsers) {
                final InsertToMongodb requestToMongodb = new InsertToMongodb(requestParser);
                final Unit unit = new Unit(requestToMongodb.getFinalData());

                Set<String> roots = requestParser.getRequest().getRoots();

                UnitGraphModel unitGraphModel = new UnitGraphModel(unit);
                roots.forEach(parentId -> {
                    UnitGraphModel parentGraphModel = parentGraphs.get(parentId);
                    unitGraphModel.addParent(parentGraphModel);
                });

                unit.mergeWith(unitGraphModel);

                // save mongo
                unitToSave.add(unit);

                String ogId = unit.getString(MetadataDocument.OG);
                if (StringUtils.isNotEmpty(ogId)) {
                    ObjectGroupGraphUpdates objectGroupGraphUpdates = objectGroupGraphUpdatesMap
                        .computeIfAbsent(ogId, id -> new ObjectGroupGraphUpdates());
                    objectGroupGraphUpdates.buildParentGraph(unit);
                }
            }
            PerformanceLogger.getInstance().log("STP_UNIT_METADATA", "UNIT_METADATA_INDEXATION", "computeAU",
                computeAU.elapsed(TimeUnit.MILLISECONDS));


            MetaDataAlreadyExistException metaDataAlreadyExistException = null;
            if (!unitToSave.isEmpty()) {

                try {
                    Stopwatch saveAU = Stopwatch.createStarted();
                    mongoDbUnitRepository.insert(unitToSave);
                    PerformanceLogger.getInstance()
                        .log("STP_UNIT_METADATA", "UNIT_METADATA_INDEXATION", "saveUnitInMongo",
                            saveAU.elapsed(TimeUnit.MILLISECONDS));

                } catch (MetaDataAlreadyExistException e) {
                    // Even Unit already exists in MongoDB, reindex in elastic search
                    metaDataAlreadyExistException = e;
                }

                persistInElasticSearch(MetadataCollections.UNIT, tenantId, unitToSave, "STP_UNIT_METADATA",
                    "UNIT_METADATA_INDEXATION");
            }

            if (!objectGroupGraphUpdatesMap.isEmpty()) {
                Stopwatch saveGOT = Stopwatch.createStarted();
                Map<String, Bson> updates = objectGroupGraphUpdatesMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, item -> item.getValue().toBsonUpdate()));
                mongoDbObjectGroupRepository.update(updates);
                PerformanceLogger.getInstance().log("STP_UNIT_METADATA", "UNIT_METADATA_INDEXATION", "saveGOTInMongo",
                    saveGOT.elapsed(TimeUnit.MILLISECONDS));

                saveGOT = Stopwatch.createStarted();
                Collection<ObjectGroup> objectGroups = mongoDbObjectGroupRepository.selectByIds(updates.keySet(), null);
                MetadataCollections.OBJECTGROUP.getEsClient()
                    .insertFullDocuments(MetadataCollections.OBJECTGROUP, tenantId, objectGroups);
                PerformanceLogger.getInstance().log("STP_UNIT_METADATA", "UNIT_METADATA_INDEXATION", "saveGOTInElastic",
                    saveGOT.elapsed(TimeUnit.MILLISECONDS));
            }

            // In case of MetaDataAlreadyExistException, we assume that documents are re-indexed in elastic search and ObjectGroup are updated
            // Then we Re-throw MetaDataAlreadyExistException
            if (null != metaDataAlreadyExistException) {
                LOGGER.warn(metaDataAlreadyExistException);
                throw metaDataAlreadyExistException;

            }

        } catch (final MongoException e) {
            throw new MetaDataExecutionException("Insert concern", e);
        }
    }

    /**
     * Delete units
     */
    public void deleteUnits(List<String> documentsToDelete)
        throws MetaDataExecutionException {

        if (documentsToDelete.isEmpty()) {
            return;
        }

        Integer tenantId = ParameterHelper.getTenantParameter();

        MetadataCollections.UNIT.getEsClient().deleteBulkUnitsEntriesIndexes(documentsToDelete, tenantId);

        List<Unit> documents = new ArrayList<>();
        for (String id : documentsToDelete) {
            documents
                .add((Unit) new Unit().append(MetadataDocument.ID, id).append(MetadataDocument.TENANT_ID, tenantId));
        }
        mongoDbUnitRepository.delete(documents);

    }

    /**
     * Delete object groups
     */
    public void deleteObjectGroups(List<String> documentsToDelete)
        throws MetaDataExecutionException {

        if (documentsToDelete.isEmpty()) {
            return;
        }

        Integer tenantId = ParameterHelper.getTenantParameter();

        MetadataCollections.OBJECTGROUP.getEsClient().deleteBulkOGEntriesIndexes(documentsToDelete, tenantId);

        List<ObjectGroup> documents = new ArrayList<>();
        for (String id : documentsToDelete) {
            documents.add((ObjectGroup) new ObjectGroup().append(MetadataDocument.ID, id)
                .append(MetadataDocument.TENANT_ID, tenantId));
        }

        mongoDbObjectGroupRepository.delete(documents);

    }
}
