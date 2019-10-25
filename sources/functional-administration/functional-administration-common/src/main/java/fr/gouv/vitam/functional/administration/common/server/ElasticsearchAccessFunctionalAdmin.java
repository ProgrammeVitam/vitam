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
package fr.gouv.vitam.functional.administration.common.server;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil.transferJsonToMapping;

// FIXME refactor with metadata


/**
 * ElasticSearch model with MongoDB as main database
 */
public class ElasticsearchAccessFunctionalAdmin extends ElasticsearchAccess {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccessFunctionalAdmin.class);

    public static final String MAPPING_FORMAT_FILE = "/format-es-mapping.json";
    public static final String MAPPING_RULE_FILE = "/rule-es-mapping.json";
    public static final String MAPPING_INGESTCONTRACT_FILE = "/ingestcontract-es-mapping.json";
    public static final String MAPPING_ACCESSCONTRACT_FILE = "/accesscontract-es-mapping.json";
    public static final String MAPPING_MANAGEMENTCONTRACT_FILE = "/managementcontract-es-mapping.json";
    public static final String MAPPING_AGENCIES_FILE = "/agencies-es-mapping.json";
    public static final String MAPPING_PROFILE_FILE = "/profile-es-mapping.json";
    public static final String MAPPING_CONTEXT_FILE = "/context-es-mapping.json";
    public static final String MAPPING_SECURITY_PROFILE_FILE = "/securityprofile-es-mapping.json";
    public static final String MAPPING_ARCHIVE_UNIT_PROFILE_FILE = "/archiveunitprofile-es-mapping.json";
    public static final String MAPPING_ONTOLOGY_FILE = "/ontology-es-mapping.json";
    public static final String MAPPING_ACCESSION_REGISTER_SYMBOLICS_FILE = "/accessionregistersymbolic-es-mapping.json";
    public static final String MAPPING_ACCESSION_REGISTER_SUMMARY_FILE = "/accessionregistersummary-es-mapping.json";
    public static final String MAPPING_ACCESSION_REGISTER_DETAIL_FILE = "/accessionregisterdetail-es-mapping.json";
    public static final String MAPPING_GRIFFIN_FILE = "/griffin-es-mapping.json";
    public static final String MAPPING_PRESERVATION_SCENARIO_FILE = "/preservationscenario-es-mapping.json";

    /**
     * @param clusterName
     * @param nodes
     * @throws VitamException
     */
    public ElasticsearchAccessFunctionalAdmin(final String clusterName, List<ElasticsearchNode> nodes)
        throws VitamException, IOException {
        super(clusterName, nodes);
    }

    /**
     * Delete one index
     *
     * @param collection
     */
    public final boolean deleteIndex(final FunctionalAdminCollections collection) {
        try {
            if (getClient().admin().indices().prepareExists(collection.getName().toLowerCase()).get().isExists()) {
                String indexName =
                    getClient().admin().indices().prepareGetAliases(collection.getName().toLowerCase()).get().getAliases()
                        .iterator().next().key;
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);

                if (!getClient().admin().indices().delete(deleteIndexRequest).get().isAcknowledged()) {
                    // TODO: 30/01/19 return false?
                    LOGGER.error("Error on index delete");
                }
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("Error while deleting index", e);
            return false;
        }
    }

    /**
     * Add a type to an index
     *
     * @param collection
     * @return key aliasName value indexName or empty
     */
    public final Map<String, String> addIndex(final FunctionalAdminCollections collection) {
        try {
            return super.createIndexAndAliasIfAliasNotExists(collection.getName().toLowerCase(), getMapping(collection),
                VitamCollection.getTypeunique(), null);
        } catch (final Exception e) {
            LOGGER.error("Error while set Mapping", e);
            return new HashMap<>();
        }
    }

    /**
     * refresh an index
     *
     * @param collection
     */
    public final void refreshIndex(final FunctionalAdminCollections collection) {
        LOGGER.debug("refreshIndex: " + collection.getName().toLowerCase());
        getClient().admin().indices().prepareRefresh(collection.getName().toLowerCase())
            .execute().actionGet();

    }

    /**
     * @param collection
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     * values" : [list of id] } }"
     * @param filter
     * @return a structure as ResultInterface
     * @throws ReferentialException
     */
    protected final SearchResponse search(final FunctionalAdminCollections collection, final QueryBuilder query,
        final QueryBuilder filter)
        throws ReferentialException {
        final String type = collection.getType();
        final SearchRequestBuilder request =
            getClient().prepareSearch(collection.getName().toLowerCase()).setSearchType(SearchType.DEFAULT)
                .setTypes(type).setExplain(false).setSize(GlobalDatas.LIMIT_LOAD);
        if (filter != null) {
            request.setQuery(query).setPostFilter(filter);
        } else {
            request.setQuery(query);
        }
        try {
            return request.get();
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
            throw new ReferentialException(e);
        }
    }

    private String getMapping(FunctionalAdminCollections collection) throws IOException {
        switch (collection) {
            case ACCESSION_REGISTER_SUMMARY:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_ACCESSION_REGISTER_SUMMARY_FILE));
            case ACCESSION_REGISTER_DETAIL:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_ACCESSION_REGISTER_DETAIL_FILE));
            case ARCHIVE_UNIT_PROFILE:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_ARCHIVE_UNIT_PROFILE_FILE));
            case ONTOLOGY:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_ONTOLOGY_FILE));
            case ACCESSION_REGISTER_SYMBOLIC:
                return transferJsonToMapping(
                    Object.class.getResourceAsStream(MAPPING_ACCESSION_REGISTER_SYMBOLICS_FILE));
            case FORMATS:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_FORMAT_FILE));
            case RULES:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_RULE_FILE));
            case INGEST_CONTRACT:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_INGESTCONTRACT_FILE));
            case MANAGEMENT_CONTRACT:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_MANAGEMENTCONTRACT_FILE));
            case ACCESS_CONTRACT:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_ACCESSCONTRACT_FILE));
            case PROFILE:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_PROFILE_FILE));
            case CONTEXT:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_CONTEXT_FILE));
            case AGENCIES:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_AGENCIES_FILE));
            case SECURITY_PROFILE:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_SECURITY_PROFILE_FILE));
            case GRIFFIN:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_GRIFFIN_FILE));
            case PRESERVATION_SCENARIO:
                return transferJsonToMapping(Object.class.getResourceAsStream(MAPPING_PRESERVATION_SCENARIO_FILE));
            case VITAM_SEQUENCE:
            default:
                LOGGER.warn(String.format("Trying to get mapping for collection '%s', but no mapping are configured.",
                    collection.getName()));
                return "";
        }
    }


    private static final BasicDBObject[] accessionRegisterSummaryIndexes = {
        new BasicDBObject(AccessionRegisterSummary.ORIGINATING_AGENCY, 1).append(AccessionRegisterSummary.TENANT_ID, 1)
    };

    private static final BasicDBObject[] accessionRegisterDetailIndexes = {
        new BasicDBObject(AccessionRegisterDetail.ORIGINATING_AGENCY, 1).append(AccessionRegisterDetail.OPI, 1).append(
            AccessionRegisterDetail.TENANT_ID, 1)
    };

    /**
     * Methods adding Indexes
     */

    public static void ensureIndex() {
        FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().dropIndexes();
        for (final BasicDBObject index : accessionRegisterSummaryIndexes) {
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().createIndex(index,
                new IndexOptions().unique(true));
        }

        FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().dropIndexes();
        for (final BasicDBObject index : accessionRegisterDetailIndexes) {
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().createIndex(index,
                new IndexOptions().unique(true));
        }
    }
}
