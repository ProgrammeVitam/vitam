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
package fr.gouv.vitam.functional.administration.common.server;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.List;

// FIXME refactor with metadata


/**
 * ElasticSearch model with MongoDB as main database
 */
public class ElasticsearchAccessFunctionalAdmin extends ElasticsearchAccess {

    private final ElasticsearchFunctionalAdminIndexManager indexManager;

    /**
     * @param clusterName
     * @param nodes
     * @param indexManager
     * @throws VitamException
     */
    public ElasticsearchAccessFunctionalAdmin(final String clusterName, List<ElasticsearchNode> nodes,
        ElasticsearchFunctionalAdminIndexManager indexManager)
        throws VitamException {
        super(clusterName, nodes);
        this.indexManager = indexManager;
    }

    /**
     * Add a type to an index
     *
     * @param collection
     * @return key aliasName value indexName or empty
     */
    public final void addIndex(final FunctionalAdminCollections collection) throws ReferentialException {
        try {
            super
                .createIndexAndAliasIfAliasNotExists(
                    indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(null),
                    indexManager.getElasticsearchIndexSettings(collection));
        } catch (final Exception e) {
            throw new ReferentialException(e);
        }
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
        throws ReferentialException, BadRequestException {

        try {
            return super
                .search(
                    indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(null), query, filter,
                    VitamDocument.ES_FILTER_OUT,
                    null,
                    0,
                    GlobalDatas.LIMIT_LOAD, null, null, null, false);
        } catch (DatabaseException e) {
            throw new ReferentialException(e);
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
