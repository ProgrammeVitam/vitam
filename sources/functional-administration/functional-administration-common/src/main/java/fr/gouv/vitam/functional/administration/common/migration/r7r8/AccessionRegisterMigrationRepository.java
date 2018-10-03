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
package fr.gouv.vitam.functional.administration.common.migration.r7r8;

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * Repository for mongo data migration
 */
public class AccessionRegisterMigrationRepository {


    private static final String NAME = "name";
    private static final String ACCESSION_REGISTER_DETAIL_INDEX_TO_DELETE = "OriginatingAgency_1_Identifier_1__tenant_1";

    public void migrateIndexes() {

        MongoCollection collection = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection();

        ListIndexesIterable listIndexes = collection.listIndexes();
        if (null != listIndexes) {
            MongoCursor<Document> indexes = listIndexes.iterator();
            while (indexes.hasNext()) {
                Document index = indexes.next();
                System.err.println(JSON.serialize(index));
                String indexName = index.getString(NAME);
                if (ACCESSION_REGISTER_DETAIL_INDEX_TO_DELETE.equals(indexName)) {
                    collection.dropIndex(ACCESSION_REGISTER_DETAIL_INDEX_TO_DELETE);
                }

            }
        }

        collection.createIndex(Indexes.ascending("OriginatingAgency", "Opi", "_tenant"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("_tenant"));
        collection.createIndex(Indexes.ascending("OriginatingAgency"));
        collection.createIndex(Indexes.ascending("SubmissionAgency"));
        collection.createIndex(Indexes.ascending("Opc"));
        collection.createIndex(Indexes.ascending("Opi"));

    }

    /**
     * Returns all AccessionRegister to migrate, by chunks of (at most) BULK_SIZE.
     */
    public CloseableIterator<List<Document>> selectAccessionRegistesBulk(FunctionalAdminCollections collection) {

        MongoCursor<Document> cursor = collection.getCollection().find().iterator();

        PeekingIterator<Document> peekingIterator = new PeekingIterator<>(cursor);

        return new CloseableIterator<List<Document>>() {

            @Override
            public void close() {
                cursor.close();
            }

            @Override
            public boolean hasNext() {
                return peekingIterator.hasNext();
            }

            @Override
            public List<Document> next() {

                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                List<Document> bulkAccessionRegisters = new ArrayList<>();

                // Bulk processing
                while (cursor.hasNext()) {

                    bulkAccessionRegisters.add(peekingIterator.next());

                    boolean shouldSplitByBulkSize = bulkAccessionRegisters.size() == VitamConfiguration.getBatchSize();
                    if (shouldSplitByBulkSize) {
                        break;
                    }
                }

                return bulkAccessionRegisters;
            }
        };
    }

    /**
     * Replace all accession register (Detail or summary)
     */
    public void bulkReplaceOrUpdateAccessionRegisters(List<Document> updatedDocuments, FunctionalAdminCollections collection) {

        List<WriteModel<Document>> updates = updatedDocuments
                .stream()
                .map(o -> new ReplaceOneModel<>(eq(AccessionRegisterDetail.ID, o.get(AccessionRegisterDetail.ID)), o,
                        new UpdateOptions().upsert(false)))
                .collect(Collectors.toList());

        collection.getCollection().bulkWrite(updates, new BulkWriteOptions().ordered(false));

    }
}
