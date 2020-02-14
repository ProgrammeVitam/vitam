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

import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ElasticSearch model with MongoDB as main database with management of index and index entries
 */
public class LogbookElasticsearchAccess extends ElasticsearchAccess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookElasticsearchAccess.class);
    public static final String MAPPING_LOGBOOK_OPERATION_FILE = "/logbook-es-mapping.json";

    /**
     * @param clusterName cluster name
     * @param nodes elasticsearch node
     * @throws VitamException if elasticsearch nodes list is empty/null
     */
    public LogbookElasticsearchAccess(final String clusterName, List<ElasticsearchNode> nodes)
        throws VitamException, IOException {
        super(clusterName, nodes);
    }

    /**
     * Add a type to an index
     *
     * @param collection collection of index
     * @param tenantId tenant Id
     * @return key aliasName value indexName or empty
     */
    public final Map<String, String> addIndex(final LogbookCollections collection, final Integer tenantId) {
        try {
            return super
                .createIndexAndAliasIfAliasNotExists(collection.getName().toLowerCase(), getMapping(), tenantId);
        } catch (final Exception e) {
            LOGGER.error("Error while set Mapping", e);
            return new HashMap<>();
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
    final void updateFullDocument(final LogbookCollections collection, final Integer tenantId,
        final String id, final VitamDocument<?> logbookDocument) throws LogbookExecutionException {
        try {
            super.updateEntry(collection.getName().toLowerCase(), tenantId, id, logbookDocument);
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
            int size = GlobalDatas.LIMIT_LOAD < limit ? GlobalDatas.LIMIT_LOAD : limit;
            return super
                .search(collection.getName().toLowerCase(), tenantId, query, filter, VitamDocument.ES_FILTER_OUT,
                    sorts,
                    offset,
                    size, null, null, null);
        } catch (DatabaseException | BadRequestException e) {
            throw new LogbookExecutionException(e);
        }
    }

    private String getMapping() throws IOException {
        return ElasticsearchUtil
            .transferJsonToMapping(LogbookOperation.class.getResourceAsStream(MAPPING_LOGBOOK_OPERATION_FILE));
    }
}
