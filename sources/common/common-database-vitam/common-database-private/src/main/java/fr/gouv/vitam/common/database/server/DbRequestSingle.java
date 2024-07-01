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
package fr.gouv.vitam.common.database.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.builder.query.NopQuery;
import fr.gouv.vitam.common.database.builder.query.PathQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Insert;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.mongodb.EmptyMongoCursor;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.elasticsearch.QueryToElasticsearch;
import fr.gouv.vitam.common.database.translators.elasticsearch.SelectToElasticsearch;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.database.translators.mongodb.SelectToMongodb;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.SetUtils;
import org.bson.conversions.Bson;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getConcernedDiffLines;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getUnifiedDiff;

/**
 * This class execute all request single in Vitam
 */
public class DbRequestSingle {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestSingle.class);

    private final VitamCollection<VitamDocument<?>> vitamCollection;
    private final VarNameAdapter vaNameAdapter;
    private final OntologyLoader ontologyLoader;
    private final ElasticsearchIndexAlias elasticsearchIndexAlias;
    private long count = 0;
    private long total = 0;
    private long offset = 0;
    private long limit = 0;

    /**
     * Constructor with VitamCollection
     */
    public DbRequestSingle(
        VitamCollection<VitamDocument<?>> collection,
        OntologyLoader ontologyLoader,
        ElasticsearchIndexAlias elasticsearchIndexAlias
    ) {
        this.vitamCollection = collection;
        this.ontologyLoader = ontologyLoader;
        this.elasticsearchIndexAlias = elasticsearchIndexAlias;
        this.vaNameAdapter = new SingleVarNameAdapter();
    }

    public DbRequestResult execute(Select request)
        throws InvalidParseOperationException, DatabaseException, BadRequestException, InvalidCreateOperationException, VitamDBException, SchemaValidationException {
        DynamicParserTokens parserTokens = new DynamicParserTokens(
            vitamCollection.getVitamDescriptionResolver(),
            ontologyLoader.loadOntologies()
        );
        return findDocuments(request.getFinalSelect(), parserTokens);
    }

    public DbRequestResult execute(Insert request, Integer version, DocumentValidator documentValidator)
        throws InvalidParseOperationException, DatabaseException, BadRequestException, InvalidCreateOperationException, VitamDBException, SchemaValidationException {
        ArrayNode data = request.getDatas();
        return insertDocuments(data, version, documentValidator);
    }

    public DbRequestResult execute(Delete request)
        throws InvalidParseOperationException, DatabaseException, BadRequestException, InvalidCreateOperationException, VitamDBException, SchemaValidationException {
        DynamicParserTokens parserTokens = new DynamicParserTokens(
            vitamCollection.getVitamDescriptionResolver(),
            ontologyLoader.loadOntologies()
        );
        return deleteDocuments(request.getFinalDelete(), parserTokens);
    }

    public DbRequestResult execute(Update request, DocumentValidator documentValidator)
        throws InvalidParseOperationException, DatabaseException, BadRequestException, InvalidCreateOperationException, VitamDBException, SchemaValidationException {
        DynamicParserTokens parserTokens = new DynamicParserTokens(
            vitamCollection.getVitamDescriptionResolver(),
            ontologyLoader.loadOntologies()
        );
        return updateDocuments(request.getFinalUpdate(), documentValidator, parserTokens);
    }

    /**
     * Main method for Multiple Insert
     *
     * @param arrayNode
     * @param version
     * @param documentValidator
     * @return DbRequestResult
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     */
    private DbRequestResult insertDocuments(ArrayNode arrayNode, Integer version, DocumentValidator documentValidator)
        throws InvalidParseOperationException, SchemaValidationException, DatabaseException {
        final List<VitamDocument<?>> vitamDocumentList = new ArrayList<>();
        for (final JsonNode objNode : arrayNode) {
            VitamDocument<?> obj = JsonHandler.getFromJsonNode(objNode, vitamCollection.getClasz());
            obj.remove(VitamDocument.SCORE);
            obj.append(VitamDocument.VERSION, version);
            if (vitamCollection.isMultiTenant()) {
                obj.append(VitamDocument.TENANT_ID, ParameterHelper.getTenantParameter());
            }

            if (!obj.containsKey(VitamDocument.ID)) {
                GUID uuid = GUIDFactory.newGUID();
                obj.put(VitamDocument.ID, uuid.toString());
            }

            //Validate the document against the collection's json schema
            documentValidator.validateDocument(JsonHandler.toJsonNode(obj));

            vitamDocumentList.add(obj);
        }
        MongoCollection<VitamDocument<?>> collection = vitamCollection.getCollection();
        try {
            collection.insertMany(vitamDocumentList);
        } catch (final MongoException e) {
            throw new DatabaseException(e);
        }
        insertToElasticsearch(vitamDocumentList);

        return new DbRequestResult().setCount(vitamDocumentList.size()).setTotal(vitamDocumentList.size());
    }

    /**
     * Private Elasticsearch insert method
     *
     * @param vitamDocumentList
     * @param vitamDocumentList
     * @throws DatabaseException
     * @throws DatabaseException if error occurs
     */
    private void insertToElasticsearch(List<VitamDocument<?>> vitamDocumentList) throws DatabaseException {
        if (vitamCollection.getEsClient() == null) {
            return;
        }
        this.vitamCollection.getEsClient().indexEntries(this.elasticsearchIndexAlias, vitamDocumentList);
    }

    /**
     * Main Select method
     *
     * @param select
     * @return DbRequestResult
     * @throws DatabaseException
     * @throws BadRequestException
     */
    private DbRequestResult findDocuments(JsonNode select, DynamicParserTokens parserTokens)
        throws DatabaseException, BadRequestException, VitamDBException {
        MongoCursor<VitamDocument<?>> cursor = search(select, parserTokens);
        return new DbRequestResult()
            .setCursor(cursor)
            .setTotal(total > 0 ? total : count)
            .setCount(count)
            .setLimit(limit)
            .setOffset(offset);
    }

    /**
     * Private method to select Elasticsearch or MongoDb
     *
     * @param select
     * @param parserTokens
     * @return MongoCursor<VitamDocument < ?>>
     * @throws DatabaseException
     * @throws BadRequestException
     */
    private MongoCursor<VitamDocument<?>> search(JsonNode select, DynamicParserTokens parserTokens)
        throws DatabaseException, BadRequestException, VitamDBException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(vaNameAdapter);
            parser.parse(select);
            if (vitamCollection.isMultiTenant()) {
                parser.addCondition(QueryHelper.eq(VitamFieldsHelper.tenant(), ParameterHelper.getTenantParameter()));
            }
            if (vitamCollection.getEsClient() != null) {
                return selectElasticsearchExecute(parser, parserTokens);
            } else {
                return selectMongoDbExecute(parser);
            }
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("find Document Exception", e);
            throw new DatabaseException(e);
        }
    }

    /**
     * Private method for select using ElasticsearchDbRequestSingle
     *
     * @param parser
     * @param parserTokens
     * @return MongoCursor<VitamDocument < ?>>
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     * @throws InvalidCreateOperationException
     * @throws DatabaseException
     * @throws BadRequestException
     */
    private MongoCursor<VitamDocument<?>> selectElasticsearchExecute(
        SelectParserSingle parser,
        DynamicParserTokens parserTokens
    )
        throws InvalidParseOperationException, InvalidCreateOperationException, DatabaseException, BadRequestException, VitamDBException {
        SelectToElasticsearch requestToEs = new SelectToElasticsearch(parser);
        QueryBuilder query = QueryToElasticsearch.getCommand(
            requestToEs.getNthQuery(0),
            parser.getAdapter(),
            parserTokens
        );
        List<SortBuilder<?>> sorts = requestToEs.getFinalOrderBy(vitamCollection.isUseScore(), parserTokens);
        offset = requestToEs.getFinalOffset();
        limit = requestToEs.getFinalLimit();
        SearchResponse elasticSearchResponse = search(
            query,
            null,
            sorts,
            requestToEs.getFinalOffset(),
            requestToEs.getFinalLimit()
        );
        if (elasticSearchResponse.status() != RestStatus.OK) {
            return new EmptyMongoCursor<>();
        }
        final SearchHits hits = elasticSearchResponse.getHits();
        if (hits.getTotalHits().value == 0) {
            return new EmptyMongoCursor<>();
        }
        total = hits.getTotalHits().value;
        final Iterator<SearchHit> iterator = hits.iterator();
        count = hits.getHits().length;
        final List<String> list = new ArrayList<>((int) count);
        final List<Float> listFloat = new ArrayList<>((int) count);
        while (iterator.hasNext()) {
            final SearchHit hit = iterator.next();
            list.add(hit.getId());
            listFloat.add(hit.getScore());
        }
        parser.getRequest().setQuery(new NopQuery());
        return selectMongoDbExecute(parser, list, listFloat);
    }

    /**
     * Private method for select using MongoDb from Elasticsearch result
     *
     * @param parser
     * @param list list of Ids
     * @return MongoCursor<VitamDocument < ?>>
     * @throws InvalidParseOperationException when query is not correct
     * @throws InvalidCreateOperationException
     */
    private MongoCursor<VitamDocument<?>> selectMongoDbExecute(
        SelectParserSingle parser,
        List<String> list,
        List<Float> score
    ) throws InvalidParseOperationException, VitamDBException {
        return DbRequestHelper.selectMongoDbExecuteThroughFakeMongoCursor(vitamCollection, parser, list, score);
    }

    /**
     * Private method for select using MongoDb
     *
     * @param parser
     * @return MongoCursor<VitamDocument < ?>>
     * @throws InvalidParseOperationException when query is not correctBson
     */
    private MongoCursor<VitamDocument<?>> selectMongoDbExecute(SelectParserSingle parser)
        throws InvalidParseOperationException {
        final SelectToMongodb selectToMongoDb = new SelectToMongodb(parser);
        Bson initialCondition = QueryToMongodb.getCommand(selectToMongoDb.getSingleSelect().getQuery());
        final Bson projection = selectToMongoDb.getFinalProjection();
        final Bson orderBy = selectToMongoDb.getFinalOrderBy();
        final int offset2 = selectToMongoDb.getFinalOffset();
        final int limit2 = selectToMongoDb.getFinalLimit();
        MongoCollection<VitamDocument<?>> collection = vitamCollection.getCollection();
        FindIterable<VitamDocument<?>> find = collection.find(initialCondition).skip(offset2);
        total = collection.countDocuments(initialCondition);

        if (projection != null) {
            find = find.projection(projection);
        }
        if (orderBy != null) {
            find = find.sort(orderBy);
        }
        if (limit2 > 0) {
            find = find.limit(limit2);
        }
        if (offset == 0) {
            offset = offset2;
        }
        if (limit == 0) {
            limit = limit2;
        }

        if (offset < total) {
            count = total - offset;
            if (count > limit) {
                count = limit;
            }
        }

        return find.iterator();
    }

    /**
     * Private sub method for Elasticsearch search
     *
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     * values" : [list of id] } }"
     * @param filter the filter
     * @param sorts the list of sort
     * @param offset the offset
     * @param limit the limit
     * @return a structure as ResultInterface
     * @throws DatabaseException
     * @throws BadRequestException
     */
    private SearchResponse search(
        final QueryBuilder query,
        final QueryBuilder filter,
        List<SortBuilder<?>> sorts,
        final int offset,
        final int limit
    ) throws DatabaseException, BadRequestException {
        return vitamCollection
            .getEsClient()
            .search(elasticsearchIndexAlias, query, filter, VitamDocument.ES_FILTER_OUT, sorts, offset, limit);
    }

    /**
     * Main Update method
     *
     * @param request
     * @return DbRequestResult
     * @throws InvalidParseOperationException
     * @throws DatabaseException
     * @throws BadRequestException
     * @throws InvalidCreateOperationException
     */
    private DbRequestResult updateDocuments(
        JsonNode request,
        DocumentValidator documentValidator,
        DynamicParserTokens parserTokens
    )
        throws InvalidParseOperationException, DatabaseException, BadRequestException, InvalidCreateOperationException, VitamDBException, SchemaValidationException {
        final UpdateParserSingle parser = new UpdateParserSingle(vaNameAdapter);
        parser.parse(request);
        if (vitamCollection.isMultiTenant()) {
            parser.addCondition(QueryHelper.eq(VitamFieldsHelper.tenant(), ParameterHelper.getTenantParameter()));
        }
        final Select selectQuery = new Select();
        selectQuery.setQuery(parser.getRequest().getQuery());

        MongoCursor<VitamDocument<?>> searchResult = search(selectQuery.getFinalSelect(), parserTokens);

        if (searchResult == null || !searchResult.hasNext()) {
            throw new DatabaseException("Document not found");
        }

        List<VitamDocument<?>> listDocuments = new ArrayList<>();

        while (searchResult.hasNext()) {
            listDocuments.add(searchResult.next());
        }
        searchResult.close();
        final Map<String, List<String>> diffs = new HashMap<>();
        List<VitamDocument<?>> listUpdatedDocuments = new ArrayList<>();
        MongoCollection<VitamDocument<?>> collection = vitamCollection.getCollection();

        for (VitamDocument<?> document : listDocuments) {
            document.remove(VitamDocument.SCORE);
            final String documentId = document.getId();
            String documentBeforeUpdate = JsonHandler.prettyPrint(document);

            VitamDocument<?> updatedDocument = null;
            int nbTry = 0;
            boolean modified = false;
            boolean updated = false;

            while (!updated && nbTry < VitamConfiguration.getOptimisticLockRetryNumber()) {
                if (nbTry > 0) {
                    document = collection.find(eq(VitamDocument.ID, documentId)).first();
                    documentBeforeUpdate = JsonHandler.prettyPrint(document);

                    if (null == document) {
                        throw new DatabaseException("[Optimistic_Lock]: Can not modify a deleted Document");
                    }
                }

                nbTry++;

                JsonNode jsonDocument = JsonHandler.toJsonNode(document);

                MongoDbInMemory mongoInMemory = new MongoDbInMemory(jsonDocument, parserTokens);
                ObjectNode updatedJsonDocument = (ObjectNode) mongoInMemory.getUpdateJson(
                    request,
                    false,
                    vaNameAdapter
                );

                documentValidator.validateDocument(updatedJsonDocument);

                updatedDocument = document.newInstance(updatedJsonDocument);
                if (!document.equals(updatedDocument)) {
                    modified = true;
                    updatedDocument.put(VitamDocument.VERSION, document.getVersion() + 1);
                    Bson condition = and(
                        eq(VitamDocument.ID, documentId),
                        eq(VitamDocument.VERSION, document.getVersion())
                    );
                    // Note: cannot do bulk since we need to check each and every update
                    try {
                        UpdateResult result = collection.replaceOne(condition, updatedDocument);
                        updated = result.getModifiedCount() == 1;
                    } catch (final MongoException e) {
                        LOGGER.warn("Update Document error : " + e.getMessage());
                    }

                    if (!updated) {
                        LOGGER.error(
                            "[Optimistic_Lock]: optimistic lock occurs while update document with id (" +
                            documentId +
                            ") of the collection " +
                            vitamCollection.getName() +
                            " retry number = " +
                            nbTry
                        );

                        try {
                            TimeUnit.MILLISECONDS.sleep(
                                ThreadLocalRandom.current().nextInt(VitamConfiguration.getOptimisticLockSleepTime())
                            );
                        } catch (InterruptedException e1) {
                            Thread.currentThread().interrupt();
                            SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
                            throw new DatabaseException(e1);
                        }
                    }
                } else {
                    break;
                }
            }

            if (modified && !updated) {
                // Throw Error after nb try
                throw new DatabaseException("[Optimistic_Lock]: Can not modify Document");
            }
            if (modified) {
                listUpdatedDocuments.add(updatedDocument);
                final String documentAfterUpdate = JsonHandler.prettyPrint(updatedDocument);
                diffs.put(documentId, getConcernedDiffLines(getUnifiedDiff(documentBeforeUpdate, documentAfterUpdate)));
            }
        }
        if (!listUpdatedDocuments.isEmpty()) {
            insertToElasticsearch(listUpdatedDocuments);
        }

        return new DbRequestResult()
            .setCount(listUpdatedDocuments.size())
            .setTotal(listUpdatedDocuments.size())
            .setDiffs(diffs);
    }

    /**
     * Main Delete method
     *
     * @param request
     * @return DbRequestResult
     * @throws DatabaseException
     * @throws BadRequestException
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     */
    private DbRequestResult deleteDocuments(JsonNode request, DynamicParserTokens parserTokens)
        throws DatabaseException, BadRequestException, InvalidCreateOperationException, InvalidParseOperationException, VitamDBException {
        final SelectParserSingle parser = new SelectParserSingle(vaNameAdapter);
        parser.parse(request);
        parser.addProjection(JsonHandler.createObjectNode(), JsonHandler.createObjectNode().put(VitamDocument.ID, 1));
        final MongoCursor<VitamDocument<?>> searchResult = search(parser.getRequest().getFinalSelect(), parserTokens);
        if (searchResult == null || !searchResult.hasNext()) {
            throw new DatabaseException("Document not found");
        }

        final List<String> ids = new ArrayList<>();
        while (searchResult.hasNext()) {
            final String documentId = searchResult.next().getId();
            ids.add(documentId);
        }
        PathQuery newQuery = QueryHelper.path(ids.toArray(new String[0]));
        searchResult.close();
        final Bson filter = QueryToMongodb.getCommand(newQuery);
        DeleteResult result;
        try {
            result = vitamCollection.getCollection().deleteMany(filter);
        } catch (final MongoException e) {
            LOGGER.warn(e);
            throw new DatabaseException(e);
        }
        deleteFromElasticSearch(ids);
        return new DbRequestResult().setCount(result.getDeletedCount()).setTotal(result.getDeletedCount());
    }

    /**
     * Private method for delete in Elasticsearch
     *
     * @param list list of Ids to delete
     * @return the number of deleted items
     * @throws DatabaseException if error occurs
     */
    private void deleteFromElasticSearch(List<String> list) throws DatabaseException {
        if (vitamCollection.getEsClient() == null || list.isEmpty()) {
            return;
        }

        vitamCollection.getEsClient().delete(elasticsearchIndexAlias, list);
    }

    public void replaceDocument(
        JsonNode document,
        String identifierValue,
        String identifierKey,
        VitamCollection<VitamDocument<?>> vitamCollection
    ) throws DatabaseException {
        replaceDocuments(Collections.singletonMap(identifierValue, document), identifierKey, vitamCollection);
    }

    public void replaceDocuments(
        Map<String, JsonNode> documentByIdentifiers,
        String identifierKey,
        VitamCollection<VitamDocument<?>> vitamCollection
    ) throws DatabaseException {
        MongoCollection<VitamDocument<?>> collection = vitamCollection.getCollection();

        if (documentByIdentifiers.isEmpty()) {
            return;
        }

        Bson documentCondition = getDocumentSelectionQuery(
            documentByIdentifiers.keySet(),
            identifierKey,
            vitamCollection
        );

        List<VitamDocument<?>> dbDocuments;
        Bson projection = Projections.include(
            Set.of(VitamDocument.ID, VitamDocument.VERSION, identifierKey).toArray(String[]::new)
        );
        try (
            MongoCursor<VitamDocument<?>> documentsInDataBase = vitamCollection
                .getCollection()
                .find(documentCondition)
                .projection(projection)
                .cursor()
        ) {
            dbDocuments = IteratorUtils.toList(documentsInDataBase);
        }

        Map<String, VitamDocument<?>> dbDocumentsByIdentifiers = dbDocuments
            .stream()
            .collect(Collectors.toMap(doc -> (String) doc.get(identifierKey), doc -> doc));

        Set<String> missingIdentifiers = SetUtils.difference(
            documentByIdentifiers.keySet(),
            dbDocumentsByIdentifiers.keySet()
        );

        if (!missingIdentifiers.isEmpty()) {
            throw new DatabaseException(
                "Documents not found with " + identifierKey + " in [" + missingIdentifiers + "]"
            );
        }

        List<ReplaceOneModel<VitamDocument<?>>> replaceOneModels = new ArrayList<>();
        List<VitamDocument<?>> updatedDocuments = new ArrayList<>();
        for (String identifier : dbDocumentsByIdentifiers.keySet()) {
            VitamDocument<?> dbDocument = dbDocumentsByIdentifiers.get(identifier);
            JsonNode document = documentByIdentifiers.get(identifier);

            ((ObjectNode) document).put(VitamDocument.ID, dbDocument.getId());
            VitamDocument<?> updatedDocument = dbDocument.newInstance(document);

            updatedDocument.put(VitamDocument.VERSION, dbDocument.getVersion() + 1);
            updatedDocuments.add(updatedDocument);

            Bson condition = and(
                eq(VitamDocument.ID, dbDocument.getId()),
                eq(VitamDocument.VERSION, dbDocument.getVersion())
            );
            replaceOneModels.add(new ReplaceOneModel<>(condition, updatedDocument));
        }

        try {
            BulkWriteResult bulkWriteResult = collection.bulkWrite(
                replaceOneModels,
                new BulkWriteOptions().ordered(false)
            );
            if (bulkWriteResult.getMatchedCount() != dbDocumentsByIdentifiers.size()) {
                throw new DatabaseException(
                    String.format(
                        "Error while bulk update document count : %s != size : %s :",
                        bulkWriteResult.getModifiedCount(),
                        dbDocumentsByIdentifiers.size()
                    )
                );
            }
        } catch (MongoException e) {
            throw new DatabaseException("Could not update documents in DB", e);
        }

        insertToElasticsearch(updatedDocuments);
    }

    private Bson getDocumentSelectionQuery(
        Collection<String> identifierValues,
        String identifierKey,
        VitamCollection<VitamDocument<?>> vitamCollection
    ) {
        if (vitamCollection.isMultiTenant()) {
            return and(
                in(identifierKey, identifierValues),
                eq(VitamDocument.TENANT_ID, ParameterHelper.getTenantParameter())
            );
        }

        return in(identifierKey, identifierValues);
    }
}
