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
package fr.gouv.vitam.storage.offers.tape.cas;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ArchiveReferentialRepository {

    private final MongoCollection<Document> collection;
    private final int bulkSize;

    public ArchiveReferentialRepository(MongoCollection<Document> collection) {
        this(collection, VitamConfiguration.getBatchSize());
    }

    @VisibleForTesting
    ArchiveReferentialRepository(MongoCollection<Document> collection, int bulkSize) {
        this.collection = collection;
        this.bulkSize = bulkSize;
    }

    public void insert(TapeArchiveReferentialEntity tapeArchiveReferentialEntity) throws ArchiveReferentialException {
        try {
            collection.insertOne(toBson(tapeArchiveReferentialEntity));
        } catch (MongoException ex) {
            throw new ArchiveReferentialException(
                "Could not insert or update archive referential for id " + tapeArchiveReferentialEntity.getArchiveId(),
                ex
            );
        }
    }

    public Optional<TapeArchiveReferentialEntity> find(String archiveId) throws ArchiveReferentialException {
        Document document;

        try {
            document = collection.find(Filters.eq(TapeArchiveReferentialEntity.ID, archiveId)).first();
        } catch (MongoException ex) {
            throw new ArchiveReferentialException("Could not find storage location by id " + archiveId, ex);
        }

        if (document == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(fromBson(document, TapeArchiveReferentialEntity.class));
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException("Could not parse document from DB " + BsonHelper.stringify(document), e);
        }
    }

    public List<TapeArchiveReferentialEntity> bulkFind(Set<String> archiveIds) throws ArchiveReferentialException {
        if (archiveIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<TapeArchiveReferentialEntity> result = new ArrayList<>();

        // Process in bulk
        UnmodifiableIterator<List<String>> archiveIdBulkIterator = Iterators.partition(archiveIds.iterator(), bulkSize);
        while (archiveIdBulkIterator.hasNext()) {
            try (
                MongoCursor<Document> documents = collection
                    .find(Filters.in(TapeArchiveReferentialEntity.ID, archiveIdBulkIterator.next()))
                    .cursor()
            ) {
                documents.forEachRemaining(document -> {
                    try {
                        result.add(fromBson(document, TapeArchiveReferentialEntity.class));
                    } catch (InvalidParseOperationException e) {
                        throw new IllegalStateException(
                            "Could not parse document from DB " + BsonHelper.stringify(document),
                            e
                        );
                    }
                });
            } catch (MongoException ex) {
                throw new ArchiveReferentialException("Could not find storage location by ids", ex);
            }
        }
        return result;
    }

    public void updateLocationToReadyOnDisk(String archiveId, long size, String digest)
        throws ArchiveReferentialException {
        try {
            UpdateResult updateResult = collection.updateOne(
                Filters.eq(TapeArchiveReferentialEntity.ID, archiveId),
                Updates.combine(
                    Updates.set(
                        TapeArchiveReferentialEntity.LOCATION,
                        toBson(new TapeLibraryReadyOnDiskArchiveStorageLocation())
                    ),
                    Updates.set(TapeArchiveReferentialEntity.SIZE, size),
                    Updates.set(TapeArchiveReferentialEntity.DIGEST, digest),
                    Updates.set(TapeArchiveReferentialEntity.LAST_UPDATE_DATE, LocalDateUtil.now().toString())
                ),
                new UpdateOptions().upsert(false)
            );

            if (updateResult.getMatchedCount() != 1) {
                throw new ArchiveReferentialException(
                    "Could not update storage location for " + archiveId + ". No such archiveId"
                );
            }
        } catch (MongoException ex) {
            throw new ArchiveReferentialException("Could not update storage location for " + archiveId, ex);
        }
    }

    public void updateLocationToOnTape(
        String archiveId,
        TapeLibraryOnTapeArchiveStorageLocation onTapeTarStorageLocation
    ) throws ArchiveReferentialException {
        try {
            UpdateResult updateResult = collection.updateOne(
                Filters.eq(TapeArchiveReferentialEntity.ID, archiveId),
                Updates.combine(
                    Updates.set(TapeArchiveReferentialEntity.LOCATION, toBson(onTapeTarStorageLocation)),
                    Updates.set(TapeArchiveReferentialEntity.LAST_UPDATE_DATE, LocalDateUtil.now().toString())
                ),
                new UpdateOptions().upsert(false)
            );

            if (updateResult.getMatchedCount() != 1) {
                throw new ArchiveReferentialException(
                    "Could not update storage location for " + archiveId + ". No such archiveId"
                );
            }
        } catch (MongoException ex) {
            throw new ArchiveReferentialException("Could not update storage location for " + archiveId, ex);
        }
    }

    private Document toBson(Object object) {
        return Document.parse(JsonHandler.unprettyPrint(object));
    }

    private <T> T fromBson(Document document, Class<T> clazz) throws InvalidParseOperationException {
        return BsonHelper.fromDocumentToObject(document, clazz);
    }
}
