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

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.VitamRepository;
import fr.gouv.vitam.common.database.api.VitamRepositoryStatus;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.HeaderIdHelper;

/**
 * Implementation for Elasticsearch
 */
public class VitamElasticsearchRepository implements VitamRepository {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamElasticsearchRepository.class);
    public static final String IDENTIFIER = "Identifier";

    private Client client;
    private String indexName;
    private boolean indexByTenant;

    /**
     * VitamElasticsearchRepository Constructor
     *
     * @param client        the es client
     * @param indexName     the name of the index
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
        ParametersChecker.checkParameter("All params are required", document);
        Document internalDocument = new Document(document);
        String id = internalDocument.getString(VitamDocument.ID);
        Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
        internalDocument.remove(VitamDocument.ID);
        internalDocument.remove(VitamDocument.SCORE);
        final String source = internalDocument.toJson(new JsonWriterSettings(JsonMode.STRICT));

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
    }

    @Override
    public void save(List<Document> documents) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", documents);
        BulkRequestBuilder bulkRequest = client.prepareBulk();

        documents.forEach(document -> {
            Document internalDocument = new Document(document);
            String id = internalDocument.getString(VitamDocument.ID);
            Integer tenant = internalDocument.getInteger(VitamDocument.TENANT_ID);
            internalDocument.remove(VitamDocument.ID);
            internalDocument.remove(VitamDocument.SCORE);
            final String source = internalDocument.toJson(new JsonWriterSettings(JsonMode.STRICT));

            String index = indexName;
            if (indexByTenant) {
                index = index + "_" + tenant;
            }

            bulkRequest.add(client.prepareIndex(index, VitamCollection.getTypeunique(), id)
                .setSource(source, XContentType.JSON));
        });

        bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        BulkResponse bulkResponse = bulkRequest.get();

        if (bulkResponse.hasFailures()) {
            LOGGER.error("Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
            throw new DatabaseException(bulkResponse.buildFailureMessage());
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
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        documents.forEach(vitamDocument -> {


            Integer tenantId = HeaderIdHelper.getTenantId();
            LOGGER.debug("insertToElasticsearch");
            String id = vitamDocument.getString(VitamDocument.ID);
            vitamDocument.remove(VitamDocument.ID);
            vitamDocument.remove(VitamDocument.SCORE);
            vitamDocument.remove("_unused");
            String index = indexName;
            if (indexByTenant) {
                index = index + "_" + tenantId;
            }
            final String mongoJson = vitamDocument.toJson(new JsonWriterSettings(JsonMode.STRICT));
            final DBObject dbObject = (DBObject) com.mongodb.util.JSON.parse(mongoJson);
            vitamDocument.clear();
            final String esJson = dbObject.toString();

            bulkRequest.add(client.prepareIndex(index, VitamCollection.getTypeunique(), id)
                .setSource(esJson, XContentType.JSON));
        });

        bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        BulkResponse bulkResponse = bulkRequest.get();

        if (bulkResponse.hasFailures()) {
            LOGGER.error("Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
            throw new DatabaseException("Index Elasticsearch has errors");
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

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        documents.forEach(vitamDocument -> {
            Integer tenantId = HeaderIdHelper.getTenantId();
            LOGGER.debug("insertToElasticsearch");
            String id = vitamDocument.getString(VitamDocument.ID);
            vitamDocument.remove(VitamDocument.ID);
            vitamDocument.remove(VitamDocument.SCORE);

            String index = indexName;
            if (indexByTenant) {
                index = index + "_" + tenantId;
            }

            transformDataForElastic(vitamDocument);
            final String mongoJson = vitamDocument.toJson(new JsonWriterSettings(JsonMode.STRICT));
            vitamDocument.clear();
            final String esJson = ((DBObject) com.mongodb.util.JSON.parse(mongoJson)).toString();

            bulkRequest.add(client.prepareIndex(index, VitamCollection.getTypeunique(), id)
                .setSource(esJson, XContentType.JSON));

        });

        bulkRequest.request().setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        BulkResponse bulkResponse = bulkRequest.get();

        if (bulkResponse.hasFailures()) {
            LOGGER.error("Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
            throw new DatabaseException("Index Elasticsearch has errors");
        }
    }

    // TODO : very ugly here, should be generic
    private void transformDataForElastic(Document vitamDocument) {
        if (vitamDocument.get("evDetData") != null) {
            String evDetDataString = (String) vitamDocument.get("evDetData");
            LOGGER.debug(evDetDataString);
            try {
                JsonNode evDetData = JsonHandler.getFromString(evDetDataString);
                vitamDocument.remove("evDetData");
                vitamDocument.put("evDetData", evDetData);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("EvDetData is not a json compatible field", e);
            }
        }
        if (vitamDocument.get("agIdExt") != null) {
            String agidExt = (String) vitamDocument.get("agIdExt");
            LOGGER.debug(agidExt);
            try {
                JsonNode agidExtNode = JsonHandler.getFromString(agidExt);
                vitamDocument.remove("agIdExt");
                vitamDocument.put("agIdExt", agidExtNode);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("agidExtNode is not a json compatible field", e);
            }
        }
        if (vitamDocument.get("rightsStatementIdentifier") != null) {
            String rightsStatementIdentifier =
                (String) vitamDocument.get("rightsStatementIdentifier");
            LOGGER.debug(rightsStatementIdentifier);
            try {
                JsonNode rightsStatementIdentifierNode = JsonHandler.getFromString(rightsStatementIdentifier);
                vitamDocument.remove("rightsStatementIdentifier");
                vitamDocument.put("rightsStatementIdentifier",
                    rightsStatementIdentifierNode);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("rightsStatementIdentifier is not a json compatible field", e);
            }
        }
        List<Document> eventDocuments = (List<Document>) vitamDocument.get("events");
        if (eventDocuments != null) {
            for (Document eventDocument : eventDocuments) {
                if (eventDocument.getString("evDetData") != null) {
                    String eventEvDetDataString =
                        eventDocument.getString("evDetData");
                    Document eventEvDetDataDocument = Document.parse(eventEvDetDataString);
                    eventDocument.remove("evDetData");
                    eventDocument.put("evDetData", eventEvDetDataDocument);
                }
                if (eventDocument.getString("rightsStatementIdentifier") != null) {
                    String eventrightsStatementIdentifier =
                        eventDocument.getString("rightsStatementIdentifier");
                    Document eventEvDetDataDocument = Document.parse(eventrightsStatementIdentifier);
                    eventDocument.remove("rightsStatementIdentifier");
                    eventDocument.put("rightsStatementIdentifier", eventEvDetDataDocument);
                }
                if (eventDocument.getString("agIdExt") != null) {
                    String eventagIdExt =
                        eventDocument.getString("agIdExt");
                    Document eventEvDetDataDocument = Document.parse(eventagIdExt);
                    eventDocument.remove("agIdExt");
                    eventDocument.put("agIdExt", eventEvDetDataDocument);
                }
            }
        }
        vitamDocument.remove("events");
        vitamDocument.put("events", eventDocuments);

    }

    @Override
    public void saveOrUpdate(List<Document> documents) throws DatabaseException {
        save(documents);
    }


    @Override
    public void remove(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", id);

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
    public void removeByNameAndTenant(String name, Integer tenant) throws DatabaseException {
        throw new DatabaseException("removeByNameAndTenant not implemented for Elasticsearch repository");
    }

    @Override
    public long purge(Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", tenant);

        String index = indexName;
        if (indexByTenant) {
            index = index + "_" + tenant;
        }

        QueryBuilder qb = termQuery(VitamDocument.TENANT_ID, tenant);

        SearchResponse scrollResp = client.prepareSearch(index)
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

        if (bulkRequest.request().numberOfActions() != 0) {
            BulkResponse bulkResponse = bulkRequest.get();

            if (bulkResponse.hasFailures()) {
                LOGGER.error("Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
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

        if (bulkRequest.request().numberOfActions() != 0) {
            BulkResponse bulkResponse = bulkRequest.get();

            if (bulkResponse.hasFailures()) {
                LOGGER.error("Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
                throw new DatabaseException(String.format("DatabaseException when calling purge by bulk Request %s",
                    bulkResponse.buildFailureMessage()));
            }

            return bulkResponse.getItems().length;
        }

        return 0;
    }

    @Override
    public Optional<Document> getByID(String id, Integer tenant) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", id);

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
        ParametersChecker.checkParameter("All params are required", tenant);

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
        ParametersChecker.checkParameter("All params are required");

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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FindIterable<Document> findDocuments(int mongoBatchSize, Integer tenant) {
        // Not implement yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FindIterable<Document> findDocuments(int mongoBatchSize) {
        // Not implement yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FindIterable<Document> findDocuments(Bson query, int mongoBatchSize) {
        // Not implement yet
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
