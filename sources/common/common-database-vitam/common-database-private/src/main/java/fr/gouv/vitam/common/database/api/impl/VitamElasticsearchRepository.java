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
package fr.gouv.vitam.common.database.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepository;
import fr.gouv.vitam.common.database.api.VitamRepositoryStatus;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.iterables.BulkIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.HeaderIdHelper;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * Implementation for Elasticsearch
 */
public class VitamElasticsearchRepository implements VitamRepository {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamElasticsearchRepository.class);
    /**
     * Identifier
     */
    public static final String IDENTIFIER = "Identifier";
    private static final String ALL_PARAMS_REQUIRED = "All params are required";
    private static final String BULK_REQ_FAIL_WITH_ERROR = "Bulk Request failure with error: ";
    private static final String EV_DET_DATA = "evDetData";
    private static final String RIGHT_STATE_ID = "rightsStatementIdentifier";
    private static final String AG_ID_EXT = "agIdExt";
    private static final String EVENTS = "events";
    private static final String NOT_IMPLEMENTED_YET = "Not implemented yet";


    private Client client;
    private String indexName;
    private boolean indexByTenant;

    /**
     * VitamElasticsearchRepository Constructor
     *
     * @param client the es client
     * @param indexName the name of the index
     * @param indexByTenant specifies if the index is for a specific tenant or not
     */
    public VitamElasticsearchRepository(Client client, String indexName, boolean indexByTenant) {
        this.client = client;
        this.indexName = indexName;
        this.indexByTenant = indexByTenant;
    }

    @Override
    public void save(Document document) throws DatabaseException {
        saveOrUpdate(document);
    }

    @Override
    public VitamRepositoryStatus saveOrUpdate(Document document) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, document);
        Document internalDocument = new Document(document);
        Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
        String id = (String) internalDocument.remove(VitamDocument.ID);
        Object score = internalDocument.remove(VitamDocument.SCORE);
        try {
            final String source = JsonHandler.unprettyPrint(internalDocument);

            String index = indexName;
            if (indexByTenant) {
                index = index + "_" + tenant;
            }

            IndexResponse response = client.prepareIndex(index, VitamCollection.getTypeunique(), id)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .setSource(source, XContentType.JSON).get();

            RestStatus status = response.status();

            switch (status) {
                case OK:
                    return VitamRepositoryStatus.UPDATED;
                case CREATED:
                    return VitamRepositoryStatus.CREATED;
                default:
                    String result = response.getResult().getLowercase();
                    LOGGER.error(String.format("Insert Documents Exception caused by : %s", result));
                    throw new DatabaseException("Insert Document Exception: " + result);
            }
        } finally {
            internalDocument.put(VitamDocument.ID, id);
            if (Objects.nonNull(score)) {
                internalDocument.put(VitamDocument.SCORE, score);
            }
        }
    }

    @Override
    public void save(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, documents);
        BulkRequestBuilder bulkRequest = client.prepareBulk();

        documents.forEach(document -> {
            Document internalDocument = new Document(document);
            Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
            String id = (String) internalDocument.remove(VitamDocument.ID);
            internalDocument.remove(VitamDocument.SCORE);

            final String source = JsonHandler.unprettyPrint(internalDocument);

            String index = indexName;
            if (indexByTenant) {
                index = index + "_" + tenant;
            }

            bulkRequest.add(client.prepareIndex(index, VitamCollection.getTypeunique(), id)
                .setSource(source, XContentType.JSON));
        });

        if (bulkRequest.numberOfActions() != 0) {
            bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            BulkResponse bulkResponse = bulkRequest.get();

            if (bulkResponse.hasFailures()) {
                LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + bulkResponse.buildFailureMessage());
                throw new DatabaseException(bulkResponse.buildFailureMessage());
            }
        }

    }

    // TODO : This should be generic, but for now, we have a specific code to handle unit

    /**
     * Reindex Unit documents
     *
     * @param documents documents to be reindexed
     * @throws DatabaseException if the ES insert was in error
     */
    public void saveUnit(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, documents);
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        documents.forEach(vitamDocument -> {
            Integer tenantId = HeaderIdHelper.getTenantId();
            LOGGER.debug("insertToElasticsearch");
            String id = (String) vitamDocument.remove(VitamDocument.ID);
            vitamDocument.remove(VitamDocument.SCORE);
            vitamDocument.remove("_unused");

            String index = indexName;
            if (indexByTenant) {
                index = index + "_" + tenantId;
            }

            final String esJson = JsonHandler.unprettyPrint(vitamDocument);
            vitamDocument.clear();

            bulkRequest.add(client.prepareIndex(index, VitamCollection.getTypeunique(), id)
                .setSource(esJson, XContentType.JSON));
        });

        if (bulkRequest.numberOfActions() != 0) {
            bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            BulkResponse bulkResponse = bulkRequest.get();

            if (bulkResponse.hasFailures()) {
                LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + bulkResponse.buildFailureMessage());
                throw new DatabaseException("Index Elasticsearch has errors");
            }
        }
    }

    // TODO : This should be generic, but for now, we have a specific code to handle logbook

    /**
     * Reindex Logbook documents
     *
     * @param documents documents to be reindexed
     * @throws DatabaseException if the ES insert was in error
     */
    public void saveLogbook(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, documents);
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        documents.forEach(vitamDocument -> {
            Integer tenantId = HeaderIdHelper.getTenantId();
            LOGGER.debug("insertToElasticsearch");
            String id = (String) vitamDocument.remove(VitamDocument.ID);
            String index = indexName;
            if (indexByTenant) {
                index = index + "_" + tenantId;
            }

            transformDataForElastic(vitamDocument);

            final String esJson = JsonHandler.unprettyPrint(vitamDocument);
            vitamDocument.clear();

            bulkRequest.add(client.prepareIndex(index, VitamCollection.getTypeunique(), id)
                .setSource(esJson, XContentType.JSON));
        });

        if (bulkRequest.numberOfActions() != 0) {
            bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            BulkResponse bulkResponse = bulkRequest.get();

            if (bulkResponse.hasFailures()) {
                LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + bulkResponse.buildFailureMessage());
                throw new DatabaseException("Index Elasticsearch has errors");
            }
        }
    }

    // TODO : very ugly here, should be generic
    private void transformDataForElastic(Document vitamDocument) {
        if (vitamDocument.get(EV_DET_DATA) != null) {
            String evDetDataString = (String) vitamDocument.get(EV_DET_DATA);
            LOGGER.debug(evDetDataString);
            try {
                JsonNode evDetData = JsonHandler.getFromString(evDetDataString);
                vitamDocument.remove(EV_DET_DATA);
                vitamDocument.put(EV_DET_DATA, evDetData);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("EvDetData is not a json compatible field", e);
            }
        }
        if (vitamDocument.get(AG_ID_EXT) != null) {
            String agidExt = (String) vitamDocument.get(AG_ID_EXT);
            LOGGER.debug(agidExt);
            try {
                JsonNode agidExtNode = JsonHandler.getFromString(agidExt);
                vitamDocument.remove(AG_ID_EXT);
                vitamDocument.put(AG_ID_EXT, agidExtNode);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("agidExtNode is not a json compatible field", e);
            }
        }
        if (vitamDocument.get(RIGHT_STATE_ID) != null) {
            String rightsStatementIdentifier =
                (String) vitamDocument.get(RIGHT_STATE_ID);
            LOGGER.debug(rightsStatementIdentifier);
            try {
                JsonNode rightsStatementIdentifierNode = JsonHandler.getFromString(rightsStatementIdentifier);
                vitamDocument.remove(RIGHT_STATE_ID);
                vitamDocument.put(RIGHT_STATE_ID,
                    rightsStatementIdentifierNode);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("rightsStatementIdentifier is not a json compatible field", e);
            }
        }
        List<Document> eventDocuments = (List<Document>) vitamDocument.get(EVENTS);
        if (eventDocuments != null) {
            for (Document eventDocument : eventDocuments) {
                if (eventDocument.getString(EV_DET_DATA) != null) {
                    String eventEvDetDataString =
                        eventDocument.getString(EV_DET_DATA);
                    Document eventEvDetDataDocument = Document.parse(eventEvDetDataString);
                    eventDocument.remove(EV_DET_DATA);
                    eventDocument.put(EV_DET_DATA, eventEvDetDataDocument);
                }
                if (eventDocument.getString(RIGHT_STATE_ID) != null) {
                    String eventrightsStatementIdentifier =
                        eventDocument.getString(RIGHT_STATE_ID);
                    Document eventEvDetDataDocument = Document.parse(eventrightsStatementIdentifier);
                    eventDocument.remove(RIGHT_STATE_ID);
                    eventDocument.put(RIGHT_STATE_ID, eventEvDetDataDocument);
                }
                if (eventDocument.getString(AG_ID_EXT) != null) {
                    String eventagIdExt =
                        eventDocument.getString(AG_ID_EXT);
                    Document eventEvDetDataDocument = Document.parse(eventagIdExt);
                    eventDocument.remove(AG_ID_EXT);
                    eventDocument.put(AG_ID_EXT, eventEvDetDataDocument);
                }
            }
        }
        vitamDocument.remove(EVENTS);
        vitamDocument.put(EVENTS, eventDocuments);

    }

    @Override
    public void saveOrUpdate(List<Document> documents) throws DatabaseException {
        save(documents);
    }

    @Override
    public void update(List<WriteModel<Document>> updates) throws DatabaseException {
        // Not implement yet
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    public FindIterable<Document> findDocuments(Collection<String> ids, Bson projection) {
        // Not implement yet
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    public void remove(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);

        String index = indexName;
        if (indexByTenant) {
            index = index + "_" + tenant;
        }

        DeleteResponse response = client.prepareDelete(index, VitamCollection.getTypeunique(), id).get();
        switch (response.status()) {
            case OK:
                break;
            default:
                String result = response.getResult().getLowercase();
                LOGGER.error(String.format("Delete Documents Exception caused by : %s", result));
                throw new DatabaseException("Delete Document Exception: " + result);
        }

    }

    @Override
    public long remove(Bson query) throws DatabaseException {
        return 0;
    }

    @Override
    public void removeByNameAndTenant(String name, Integer tenant) throws DatabaseException {
        throw new DatabaseException("removeByNameAndTenant not implemented for Elasticsearch repository");
    }

    @Override
    public long purge(Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);

        String index = indexName;
        if (indexByTenant) {
            index = index + "_" + tenant;
        }

        QueryBuilder qb = termQuery(VitamDocument.TENANT_ID, tenant);

        SearchResponse scrollResp = client.prepareSearch(index)
            .setFetchSource(false)
            .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
            .setScroll(new TimeValue(60000))
            .setQuery(qb)
            .setSize(100).get();

        BulkRequestBuilder bulkRequest = client.prepareBulk();

        do {
            for (SearchHit hit : scrollResp.getHits().getHits()) {
                bulkRequest.add(client.prepareDelete(index, VitamCollection.getTypeunique(), hit.getId()));
            }

            scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute()
                .actionGet();
        } while (scrollResp.getHits().getHits().length != 0);

        bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        if (bulkRequest.numberOfActions() != 0) {
            BulkResponse bulkResponse = bulkRequest.get();

            if (bulkResponse.hasFailures()) {
                LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + bulkResponse.buildFailureMessage());
                throw new DatabaseException(String.format("DatabaseException when calling purge by bulk Request %s",
                    bulkResponse.buildFailureMessage()));
            }

            return bulkResponse.getItems().length;
        }

        return 0;
    }


    @Override
    public long purge() throws DatabaseException {
        String index = indexName;

        QueryBuilder qb = matchAllQuery();

        SearchResponse scrollResp = client.prepareSearch(index)
            .setFetchSource(false)
            .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
            .setScroll(new TimeValue(60000))
            .setQuery(qb)
            .setSize(100).get();

        BulkRequestBuilder bulkRequest = client.prepareBulk();

        do {
            for (SearchHit hit : scrollResp.getHits().getHits()) {
                bulkRequest.add(client.prepareDelete(index, VitamCollection.getTypeunique(), hit.getId()));
            }

            scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute()
                .actionGet();
        } while (scrollResp.getHits().getHits().length != 0);

        bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        if (bulkRequest.numberOfActions() != 0) {
            BulkResponse bulkResponse = bulkRequest.get();

            if (bulkResponse.hasFailures()) {
                LOGGER.error(BULK_REQ_FAIL_WITH_ERROR + bulkResponse.buildFailureMessage());
                throw new DatabaseException(String.format("DatabaseException when calling purge by bulk Request %s",
                    bulkResponse.buildFailureMessage()));
            }

            return bulkResponse.getItems().length;
        }

        return 0;
    }

    @Override
    public Optional<Document> getByID(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, id);

        String index = indexName;
        if (indexByTenant) {
            index = index + "_" + tenant;
        }


        GetResponse response = client.prepareGet(index, VitamCollection.getTypeunique(), id).get();

        if (response.isExists()) {
            try {
                return Optional.of(JsonHandler.getFromString(response.getSourceAsString(), Document.class));
            } catch (InvalidParseOperationException e) {
                throw new DatabaseException(e);
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Document> findByIdentifierAndTenant(String identifier, Integer tenant)
        throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tenant);

        String index = indexName;
        if (indexByTenant) {
            index = index + "_" + tenant;
        }
        QueryBuilder qb = boolQuery().must(termQuery(IDENTIFIER, identifier))
            .must(termQuery(VitamDocument.TENANT_ID, tenant));

        SearchResponse search = client.prepareSearch(index)
            .setQuery(qb).get();
        for (SearchHit hit : search.getHits().getHits()) {
            try {
                return Optional.of(JsonHandler.getFromString(hit.getSourceAsString(), Document.class));
            } catch (InvalidParseOperationException e) {
                throw new DatabaseException(e);
            }
        }

        return Optional.empty();

    }

    @Override
    public Optional<Document> findByIdentifier(String identifier)
        throws DatabaseException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED);

        String index = indexName;


        SearchResponse search = client.prepareSearch(index)
            .setQuery(termQuery(IDENTIFIER, identifier)).get();
        for (SearchHit hit : search.getHits().getHits()) {
            try {
                return Optional.of(JsonHandler.getFromString(hit.getSourceAsString(), Document.class));
            } catch (InvalidParseOperationException e) {
                throw new DatabaseException(e);
            }
        }

        return Optional.empty();
    }

    @Override
    public FindIterable<Document> findByFieldsDocuments(Map<String, String> fields, int mongoBatchSize,
        Integer tenant) {
        // Not implement yet
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    public FindIterable<Document> findDocuments(int mongoBatchSize, Integer tenant) {
        // Not implement yet
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    public FindIterable<Document> findDocuments(int mongoBatchSize) {
        // Not implement yet
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    public FindIterable<Document> findDocuments(Bson query, int mongoBatchSize) {
        // Not implement yet
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    public void delete(List<String> ids, int tenant) throws DatabaseException {

        String index = indexName;
        if (indexByTenant) {
            index = index + "_" + tenant;
        }

        Iterator<List<String>> idIterator =
            new BulkIterator<>(ids.iterator(), VitamConfiguration.getMaxElasticsearchBulk());

        while (idIterator.hasNext()) {

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (String id : idIterator.next()) {
                bulkRequest.add(client.prepareDelete(index,
                    VitamCollection.getTypeunique(), id));
            }

            WriteRequest.RefreshPolicy refreshPolicy = idIterator.hasNext() ?
                WriteRequest.RefreshPolicy.NONE :
                WriteRequest.RefreshPolicy.IMMEDIATE;

            if (bulkRequest.numberOfActions() != 0) {
                final BulkResponse bulkResponse =
                    bulkRequest.setRefreshPolicy(refreshPolicy).execute().actionGet();

                if (bulkResponse.hasFailures()) {
                    throw new DatabaseException("ES delete in error: " + bulkResponse.buildFailureMessage());
                }
            }
        }
    }
}
