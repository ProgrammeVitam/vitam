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

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.List;
import java.util.Optional;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.VitamRepository;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
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

/**
 * Implementation for Elasticsearch
 */
public class VitamElasticsearchRepository implements VitamRepository {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamElasticsearchRepository.class);

    private Client client;
    private String indexName;
    private boolean indexByTenant;


    public VitamElasticsearchRepository() {
    }

    /**
     * @param client
     * @param indexName
     * @param indexByTenant
     */
    public VitamElasticsearchRepository(Client client, String indexName, boolean indexByTenant) {
        this.client = client;
        this.indexName = indexName;
        this.indexByTenant = indexByTenant;
    }


    @Override
    public void save(Document document) throws DatabaseException {
        ParametersChecker.checkParameter("All params are required", document);

        String id = document.getString(VitamDocument.ID);
        Integer tenant = document.getInteger(VitamDocument.TENANT_ID);
        document.remove(VitamDocument.ID);
        document.remove(VitamDocument.SCORE);
        final String source = document.toJson(new JsonWriterSettings(JsonMode.STRICT));

        String index = indexName;
        if (indexByTenant) {
            index = index + "_" + tenant;
        }

        IndexResponse response = client.prepareIndex(index, VitamCollection.getTypeunique(), id)
            .setSource(source, XContentType.JSON).get();

        RestStatus status = response.status();

        switch (status) {
            case OK:
            case CREATED:
                break;
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
            String id = document.getString(VitamDocument.ID);
            Integer tenant = document.getInteger(VitamDocument.TENANT_ID);
            document.remove(VitamDocument.ID);
            document.remove(VitamDocument.SCORE);
            final String source = document.toJson(new JsonWriterSettings(JsonMode.STRICT));

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

        BulkResponse bulkResponse = bulkRequest.get();

        if (bulkResponse.hasFailures()) {
            LOGGER.error("Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
            throw new DatabaseException(String.format("DatabaseException when calling purge by bulk Request %s", bulkResponse.buildFailureMessage()));
        }

        return bulkResponse.getItems().length;
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

}
