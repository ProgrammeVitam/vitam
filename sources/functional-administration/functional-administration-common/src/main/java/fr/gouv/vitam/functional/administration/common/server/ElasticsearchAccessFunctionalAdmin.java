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
package fr.gouv.vitam.functional.administration.common.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.gouv.vitam.functional.administration.common.Ontology;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.SecurityProfile;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

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
    public static final String MAPPING_AGENCIES_FILE = "/agencies-es-mapping.json";

    public static final String MAPPING_PROFILE_FILE = "/profile-es-mapping.json";
    public static final String MAPPING_CONTEXT_FILE = "/context-es-mapping.json";
    public static final String MAPPING_SECURITY_PROFILE_FILE = "/securityprofile-es-mapping.json";
    public static final String MAPPING_ARCHIVE_UNIT_PROFILE_FILE = "/archiveunitprofile-es-mapping.json";
    public static final String MAPPING_ONTOLOGY_FILE = "/ontology-es-mapping.json";

    public static final String MAPPING_ACCESSION_REGISTER_SUMMARY_FILE = "/accessionregistersummary-es-mapping.json";
    public static final String MAPPING_ACCESSION_REGISTER_DETAIL_FILE = "/accessionregisterdetail-es-mapping.json";

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
     * @throws ReferentialException
     */
    public final void deleteIndex(final FunctionalAdminCollections collection) throws ReferentialException {
        try {
            if (client.admin().indices().prepareExists(collection.getName().toLowerCase()).get().isExists()) {
                if (!client.admin().indices().prepareDelete(collection.getName().toLowerCase()).get()
                    .isAcknowledged()) {
                    LOGGER.error("Error on index delete");
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Error while deleting index", e);
            throw new ReferentialException(e);
        }
    }

    /**
     * Add a type to an index
     *
     * @param collection
     * @return True if ok
     */
    public final boolean addIndex(final FunctionalAdminCollections collection) {
        try {
            super.createIndexAndAliasIfAliasNotExists(collection.getName().toLowerCase(), getMapping(collection),
                VitamCollection.getTypeunique(), null);
        } catch (final Exception e) {
            LOGGER.error("Error while set Mapping", e);
            return false;
        }
        return true;
    }

    /**
     * refresh an index
     *
     * @param collection
     */
    public final void refreshIndex(final FunctionalAdminCollections collection) {
        LOGGER.debug("refreshIndex: " + collection.getName().toLowerCase());
        client.admin().indices().prepareRefresh(collection.getName().toLowerCase())
            .execute().actionGet();

    }

    /**
     * Add a set of entries in the ElasticSearch index. <br>
     * Used in reload from scratch.
     *
     * @param collection
     * @param mapIdJson
     * @return the listener on bulk insert
     */
    final BulkResponse addEntryIndexes(final FunctionalAdminCollections collection,
        final Map<String, String> mapIdJson) {
        final BulkRequestBuilder bulkRequest = client.prepareBulk();

        // either use client#prepare, or use Requests# to directly build index/delete requests
        final String type = collection.getType();
        for (final Entry<String, String> val : mapIdJson.entrySet()) {
            bulkRequest.setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .add(client.prepareIndex(collection.getName().toLowerCase(), type,
                    val.getKey()).setSource(val.getValue(), XContentType.JSON));

        }
        return bulkRequest.execute().actionGet();
    }


    /**
     * @param collection
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     *        values" : [list of id] } }"
     * @param filter
     * @return a structure as ResultInterface
     * @throws ReferentialException
     */
    protected final SearchResponse search(final FunctionalAdminCollections collection, final QueryBuilder query,
        final QueryBuilder filter)
        throws ReferentialException {
        final String type = collection.getType();
        final SearchRequestBuilder request =
            client.prepareSearch(collection.getName().toLowerCase()).setSearchType(SearchType.DEFAULT)
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
        if (collection.equals(FunctionalAdminCollections.FORMATS)) {
            return ElasticsearchUtil.transferJsonToMapping(FileFormat.class.getResourceAsStream(MAPPING_FORMAT_FILE));
        } else if (collection.equals(FunctionalAdminCollections.RULES)) {
            return ElasticsearchUtil.transferJsonToMapping(FileRules.class.getResourceAsStream(MAPPING_RULE_FILE));
        } else if (collection.equals(FunctionalAdminCollections.INGEST_CONTRACT)) {
            return ElasticsearchUtil
                .transferJsonToMapping(FileRules.class.getResourceAsStream(MAPPING_INGESTCONTRACT_FILE));
        } else if (collection.equals(FunctionalAdminCollections.ACCESS_CONTRACT)) {
            return ElasticsearchUtil
                .transferJsonToMapping(FileRules.class.getResourceAsStream(MAPPING_ACCESSCONTRACT_FILE));
        } else if (collection.equals(FunctionalAdminCollections.PROFILE)) {
            return ElasticsearchUtil.transferJsonToMapping(FileRules.class.getResourceAsStream(MAPPING_PROFILE_FILE));
        } else if (collection.equals(FunctionalAdminCollections.CONTEXT)) {
            return ElasticsearchUtil.transferJsonToMapping(FileRules.class.getResourceAsStream(MAPPING_CONTEXT_FILE));
        } else if (collection.equals(FunctionalAdminCollections.AGENCIES)) {
            return ElasticsearchUtil.transferJsonToMapping(Agencies.class.getResourceAsStream(MAPPING_AGENCIES_FILE));
        } else if (collection.equals(FunctionalAdminCollections.SECURITY_PROFILE)) {
            return ElasticsearchUtil
                .transferJsonToMapping(SecurityProfile.class.getResourceAsStream(MAPPING_SECURITY_PROFILE_FILE));
        } else if (FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.equals(collection)) {
            return ElasticsearchUtil.transferJsonToMapping(
                AccessionRegisterSummary.class.getResourceAsStream(MAPPING_ACCESSION_REGISTER_SUMMARY_FILE));
        } else if (FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.equals(collection)) {
            return ElasticsearchUtil.transferJsonToMapping(
                AccessionRegisterDetail.class.getResourceAsStream(MAPPING_ACCESSION_REGISTER_DETAIL_FILE));
        } else if(FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.equals(collection)) {
            return ElasticsearchUtil.transferJsonToMapping(
                ArchiveUnitProfile.class.getResourceAsStream(MAPPING_ARCHIVE_UNIT_PROFILE_FILE));
        } else if (FunctionalAdminCollections.ONTOLOGY.equals(collection)) {
            return ElasticsearchUtil.transferJsonToMapping(
                Ontology.class.getResourceAsStream(MAPPING_ONTOLOGY_FILE));
        }
        return "";
    }
}
