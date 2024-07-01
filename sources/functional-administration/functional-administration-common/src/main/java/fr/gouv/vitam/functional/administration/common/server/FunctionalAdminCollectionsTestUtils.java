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

package fr.gouv.vitam.functional.administration.common.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Updates;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.config.FunctionalAdminIndexationConfiguration;
import org.bson.Document;

import java.util.Collection;
import java.util.Collections;

@VisibleForTesting
public final class FunctionalAdminCollectionsTestUtils {

    private FunctionalAdminCollectionsTestUtils() {
        // Private constructor for static class
    }

    @VisibleForTesting
    public static void beforeTestClass(
        final MongoDatabase db,
        String prefix,
        final ElasticsearchAccessFunctionalAdmin esClient
    ) {
        beforeTestClass(db, prefix, esClient, Lists.newArrayList(FunctionalAdminCollections.values()));
    }

    @VisibleForTesting
    public static void beforeTestClass(
        final MongoDatabase db,
        String prefix,
        final ElasticsearchAccessFunctionalAdmin esClient,
        Collection<FunctionalAdminCollections> functionalAdminCollections
    ) {
        for (FunctionalAdminCollections collection : functionalAdminCollections) {
            if (collection != FunctionalAdminCollections.VITAM_SEQUENCE) {
                collection.getVitamCollection().setName(prefix + collection.getClasz().getSimpleName());
                collection.initialize(db, false);
                if (collection.getEsClient() == null) {
                    collection.initialize(esClient);
                } else {
                    collection.initialize(collection.getEsClient());
                }
            }
        }
        if (functionalAdminCollections.contains(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)) {
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection()
                .createIndex(
                    new Document("OriginatingAgency", 1).append("Opi", 1).append("_tenant", 1),
                    new IndexOptions().unique(true)
                );
        }

        if (functionalAdminCollections.contains(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY)) {
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                .createIndex(
                    new Document("_tenant", 1).append("OriginatingAgency", 1),
                    new IndexOptions().unique(true)
                );
        }
        // TODO: 30/01/19 Add indexes of other collections

    }

    @VisibleForTesting
    public static void afterTestClass(boolean deleteEsIndex) {
        afterTestClass(Lists.newArrayList(FunctionalAdminCollections.values()), deleteEsIndex);
    }

    @VisibleForTesting
    public static void resetVitamSequenceCounter() {
        FunctionalAdminCollections.VITAM_SEQUENCE.getCollection()
            .updateMany(Filters.exists(VitamSequence.ID), Updates.set(VitamSequence.COUNTER, 0));
    }

    @VisibleForTesting
    public static void afterTestClass(
        Collection<FunctionalAdminCollections> functionalAdminCollections,
        boolean deleteEsIndex
    ) {
        ParametersChecker.checkParameter("functionalAdminCollections is required", functionalAdminCollections);
        try {
            for (FunctionalAdminCollections collection : functionalAdminCollections) {
                if (collection != FunctionalAdminCollections.VITAM_SEQUENCE) {
                    if (null != collection.getVitamCollection().getCollection()) {
                        collection.getVitamCollection().getCollection().deleteMany(new Document());
                    }

                    if (collection.getEsClient() != null) {
                        if (deleteEsIndex) {
                            collection
                                .getEsClient()
                                .deleteIndexByAliasForTesting(
                                    ElasticsearchIndexAlias.ofCrossTenantCollection(collection.getName())
                                );
                        } else {
                            collection
                                .getEsClient()
                                .purgeIndexForTesting(
                                    ElasticsearchIndexAlias.ofCrossTenantCollection(collection.getName())
                                );
                        }
                    }
                }
            }
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    public static void afterTest() {
        afterTestClass(false);
    }

    @VisibleForTesting
    public static void afterTest(Collection<FunctionalAdminCollections> functionalAdminCollections) {
        afterTestClass(functionalAdminCollections, false);
    }

    @VisibleForTesting
    public static ElasticsearchFunctionalAdminIndexManager createTestIndexManager() {
        return new ElasticsearchFunctionalAdminIndexManager(
            new AdminManagementConfiguration(Collections.emptyList(), null, null, null).setIndexationConfiguration(
                new FunctionalAdminIndexationConfiguration().setDefaultConfiguration(new CollectionConfiguration(1, 0))
            )
        );
    }
}
