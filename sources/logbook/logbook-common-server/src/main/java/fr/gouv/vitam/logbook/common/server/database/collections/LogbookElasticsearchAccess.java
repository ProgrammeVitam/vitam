/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.logbook.common.server.database.collections;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.util.Collection;
import java.util.List;

import static fr.gouv.vitam.logbook.common.parameters.Contexts.IMPORT_ONTOLOGY;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.REFERENTIAL_FORMAT_IMPORT;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.REFRENTIAL_FORMAT_DELETE;

/**
 * ElasticSearch model with MongoDB as main database with management of index and index entries
 */
public class LogbookElasticsearchAccess extends ElasticsearchAccess {

    public static final String MAPPING_LOGBOOK_OPERATION_FILE = "/logbook-es-mapping.json";

    private static final String[] MULTI_TENANT_EV_TYPES = {
            IMPORT_ONTOLOGY.getEventType(),
            REFERENTIAL_FORMAT_IMPORT.getEventType(),
            REFRENTIAL_FORMAT_DELETE.getEventType()
    };

    private final ElasticsearchLogbookIndexManager indexManager;

    /**
     * @param clusterName cluster name
     * @param nodes elasticsearch node
     * @param indexManager
     * @throws VitamException if elasticsearch nodes list is empty/null
     */
    public LogbookElasticsearchAccess(final String clusterName, List<ElasticsearchNode> nodes,
        ElasticsearchLogbookIndexManager indexManager)
        throws VitamException {
        super(clusterName, nodes);
        this.indexManager = indexManager;
    }

    public void createIndexesAndAliases() {

        try {

            createIndexesAndAliasesForDedicatedTenants();
            createIndexesAndAliasesForTenantGroups();

        } catch (final Exception e) {
            throw new RuntimeException("Could not create indexes and aliases", e);
        }
    }

    private void createIndexesAndAliasesForDedicatedTenants() throws LogbookExecutionException {
        Collection<Integer> dedicatedTenants = this.indexManager.getDedicatedTenants();

        for (int tenantId : dedicatedTenants) {
            createIndexAndAliasIfAliasNotExists(LogbookCollections.OPERATION, tenantId);
        }
    }

    private void createIndexesAndAliasesForTenantGroups() throws LogbookExecutionException {
        Collection<String> tenantGroups = this.indexManager.getTenantGroups();
        for (String tenantGroup : tenantGroups) {

            Collection<Integer> tenantGroupTenants = this.indexManager.getTenantGroupTenants(tenantGroup);
            if (tenantGroupTenants.isEmpty()) {
                continue;
            }

            int tenantId = tenantGroupTenants.iterator().next();
            createIndexAndAliasIfAliasNotExists(LogbookCollections.OPERATION, tenantId);
        }
    }

    public final void createIndexAndAliasIfAliasNotExists(final LogbookCollections collection, final Integer tenantId)
        throws LogbookExecutionException {
        try {
            ElasticsearchIndexAlias indexAlias =
                this.indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenantId);
            ElasticsearchIndexSettings indexSettings =
                this.indexManager.getElasticsearchIndexSettings(collection, tenantId);
            super.createIndexAndAliasIfAliasNotExists(indexAlias, indexSettings);
        } catch (final Exception e) {
            throw new LogbookExecutionException("Error while set Mapping", e);
        }
    }

    /**
     * Update an entry in the ElasticSearch index
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     * @param id the id of the entry
     * @param logbookDocument the entry document
     */
    final <T extends VitamDocument> void updateFullDocument(final LogbookCollections collection, final Integer tenantId,
        final String id, final T logbookDocument) throws LogbookExecutionException {
        try {
            ElasticsearchIndexAlias indexAlias =
                this.indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenantId);
            super.updateEntry(indexAlias, id, logbookDocument);
        } catch (DatabaseException e) {
            throw new LogbookExecutionException(e);
        }
    }

    /**
     * Search entries in the ElasticSearch index.
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     * @param query as in DSL mode "{ "fieldname" : "value" }" "{ "match" : { "fieldname" : "value" } }" "{ "ids" : { "
     * values" : [list of id] } }"
     * @param filter the filter
     * @param sorts the list of sort
     * @param offset the offset
     * @param limit the limit
     * @return a structure as SearchResponse
     * @throws LogbookException thrown of an error occurred while executing the request
     */
    public final SearchResponse search(final LogbookCollections collection, final Integer tenantId,
        final QueryBuilder query,
        final QueryBuilder filter, final List<SortBuilder> sorts, final int offset, final int limit)
        throws LogbookException {
        try {
            int size = Math.min(GlobalDatas.LIMIT_LOAD, limit);
            ElasticsearchIndexAlias indexAlias =
                this.indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenantId);

            BoolQueryBuilder finalQuery = new BoolQueryBuilder().must(query);
            TermQueryBuilder currentTenantQuery = QueryBuilders.termQuery(LogbookDocument.TENANT_ID, tenantId);

            if (ParameterHelper.getTenantParameter().equals(VitamConfiguration.getAdminTenant())) {
                finalQuery.must(currentTenantQuery);
                return super
                        .search(indexAlias, finalQuery, filter, VitamDocument.ES_FILTER_OUT,
                                sorts,
                                offset,
                                size, null, null, null);
            } else {
                ElasticsearchIndexAlias adminTenantIndex = this.indexManager.getElasticsearchIndexAliasResolver(collection)
                        .resolveIndexName(VitamConfiguration.getAdminTenant());
                finalQuery.must(new BoolQueryBuilder()
                        .should(currentTenantQuery)
                        .should(new BoolQueryBuilder()
                                .must(QueryBuilders.termsQuery(LogbookEvent.EV_TYPE, MULTI_TENANT_EV_TYPES))
                                .must(QueryBuilders.termQuery(LogbookDocument.TENANT_ID, VitamConfiguration.getAdminTenant()))
                        )
                );
                return super
                        .searchCrossIndices(ImmutableSet.of(indexAlias, adminTenantIndex),
                                finalQuery, filter, VitamDocument.ES_FILTER_OUT,
                                sorts, offset, size, null, null, null);
            }
        } catch (DatabaseException | BadRequestException e) {
            throw new LogbookExecutionException(e);
        }
    }

    @VisibleForTesting
    public void deleteIndexByAliasForTesting(LogbookCollections collection, int tenantId)
        throws LogbookExecutionException {
        try {
            super.deleteIndexByAliasForTesting(this.indexManager
                .getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenantId));
        } catch (DatabaseException e) {
            throw new LogbookExecutionException(e);
        }
    }

    public void indexEntry(LogbookCollections collection, Integer tenantId, String id, VitamDocument vitamDocument)
        throws LogbookExecutionException {
        try {
            super.indexEntry(this.indexManager
                .getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenantId), id, vitamDocument);
        } catch (DatabaseException e) {
            throw new LogbookExecutionException(e);
        }
    }

    public void refreshIndex(LogbookCollections collection, int tenantId) throws LogbookExecutionException {
        try {
            super.refreshIndex(this.indexManager
                .getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenantId));
        } catch (DatabaseException e) {
            throw new LogbookExecutionException(e);
        }
    }

    @VisibleForTesting
    public void purgeIndexForTesting(LogbookCollections collection, Integer tenantId) throws LogbookExecutionException {
        try {
            ElasticsearchIndexAlias indexAlias =
                this.indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(tenantId);
            super.purgeIndexForTesting(indexAlias, tenantId);
        } catch (DatabaseException e) {
            throw new LogbookExecutionException(e);
        }
    }
}
