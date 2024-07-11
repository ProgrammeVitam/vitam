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
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ObjectReferentialRepository {

    private final MongoCollection<Document> collection;
    private final int bulkSize;

    public ObjectReferentialRepository(MongoCollection<Document> collection) {
        this(collection, VitamConfiguration.getBatchSize());
    }

    @VisibleForTesting
    ObjectReferentialRepository(MongoCollection<Document> collection, int bulkSize) {
        this.collection = collection;
        this.bulkSize = bulkSize;
    }

    public void insertOrUpdate(TapeObjectReferentialEntity tapeObjectReferentialEntity)
        throws ObjectReferentialException {
        try {
            collection.findOneAndReplace(
                Filters.eq(TapeObjectReferentialEntity.ID, toBson(tapeObjectReferentialEntity.getId())),
                toBson(tapeObjectReferentialEntity),
                new FindOneAndReplaceOptions().upsert(true)
            );
        } catch (MongoException ex) {
            throw new ObjectReferentialException(
                "Could not insert or update tar referential for id " +
                tapeObjectReferentialEntity.getId().getContainerName() +
                "/" +
                tapeObjectReferentialEntity.getId().getObjectName(),
                ex
            );
        }
    }

    public Optional<TapeObjectReferentialEntity> find(String containerName, String objectName)
        throws ObjectReferentialException {
        Document document;
        try {
            document = collection
                .find(
                    Filters.eq(
                        TapeObjectReferentialEntity.ID,
                        toBson(new TapeLibraryObjectReferentialId(containerName, objectName))
                    )
                )
                .first();
        } catch (MongoException ex) {
            throw new ObjectReferentialException(
                "Could not find storage location by id " + containerName + "/" + objectName,
                ex
            );
        }

        if (document == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(fromBson(document));
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException("Could not parse document from DB " + BsonHelper.stringify(document), e);
        }
    }

    public List<TapeObjectReferentialEntity> bulkFind(String containerName, Set<String> objectNames)
        throws ObjectReferentialException {
        if (objectNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Process in bulks
        Iterator<List<String>> objectNameBulks = Iterators.partition(objectNames.iterator(), this.bulkSize);

        List<TapeObjectReferentialEntity> result = new ArrayList<>();
        while (objectNameBulks.hasNext()) {
            List<Document> objectReferentialIds = objectNameBulks
                .next()
                .stream()
                .map(objectName -> toBson(new TapeLibraryObjectReferentialId(containerName, objectName)))
                .collect(Collectors.toList());

            try (
                MongoCursor<Document> iterator = collection
                    .find(Filters.in(TapeObjectReferentialEntity.ID, objectReferentialIds))
                    .iterator()
            ) {
                while (iterator.hasNext()) {
                    Document document = iterator.next();
                    try {
                        result.add(fromBson(document));
                    } catch (InvalidParseOperationException e) {
                        throw new IllegalStateException(
                            "Could not parse documents from DB " + BsonHelper.stringify(document),
                            e
                        );
                    }
                }
            } catch (MongoException ex) {
                throw new ObjectReferentialException(
                    "Could not find storage location by ids " + objectNames + " in container " + containerName,
                    ex
                );
            }
        }
        return result;
    }

    public void updateStorageLocation(
        String containerName,
        String objectName,
        String storageId,
        TapeLibraryTarObjectStorageLocation tapeLibraryTarStorageLocation
    ) throws ObjectReferentialException {
        try {
            collection.updateOne(
                Filters.and(
                    Filters.eq(
                        TapeObjectReferentialEntity.ID,
                        toBson(new TapeLibraryObjectReferentialId(containerName, objectName))
                    ),
                    Filters.eq(TapeObjectReferentialEntity.STORAGE_ID, storageId)
                ),
                Updates.combine(
                    Updates.set(TapeObjectReferentialEntity.LOCATION, toBson(tapeLibraryTarStorageLocation)),
                    Updates.set(TapeObjectReferentialEntity.LAST_UPDATE_DATE, LocalDateUtil.now().toString())
                ),
                new UpdateOptions().upsert(false)
            );
        } catch (MongoException ex) {
            throw new ObjectReferentialException(
                "Could not update storage location for " + containerName + "/" + objectName,
                ex
            );
        }
    }

    public boolean delete(TapeLibraryObjectReferentialId tapeLibraryObjectReferentialId)
        throws ObjectReferentialException {
        try {
            DeleteResult deleteResult = collection.deleteOne(
                Filters.eq(TapeObjectReferentialEntity.ID, toBson(tapeLibraryObjectReferentialId))
            );

            return (deleteResult.getDeletedCount() > 0L);
        } catch (MongoException ex) {
            throw new ObjectReferentialException(
                "Could not delete tar referential for id " +
                tapeLibraryObjectReferentialId.getContainerName() +
                "/" +
                tapeLibraryObjectReferentialId.getObjectName(),
                ex
            );
        }
    }

    public Set<String> selectArchiveIdsByObjectIds(Iterator<TapeLibraryObjectReferentialId> objectIdIterator)
        throws ObjectReferentialException {
        Iterator<List<TapeLibraryObjectReferentialId>> objectIdBulkIterator = Iterators.partition(
            objectIdIterator,
            bulkSize
        );
        Set<String> archiveIds = new HashSet<>();
        while (objectIdBulkIterator.hasNext()) {
            List<Document> objectReferentialIds = objectIdBulkIterator
                .next()
                .stream()
                .map(this::toBson)
                .collect(Collectors.toList());

            List<Bson> pipeline = List.of(
                Aggregates.match(
                    Filters.and(
                        Filters.in(TapeObjectReferentialEntity.ID, objectReferentialIds),
                        Filters.eq(
                            TapeObjectReferentialEntity.LOCATION + "." + TapeLibraryObjectStorageLocation.TYPE,
                            TapeLibraryObjectStorageLocation.TAR
                        )
                    )
                ),
                Aggregates.project(
                    new Document()
                        .append("_id", 0)
                        .append(
                            TapeObjectReferentialEntity.LOCATION +
                            "." +
                            TapeLibraryTarObjectStorageLocation.TAR_ENTRIES +
                            "." +
                            TarEntryDescription.TAR_FILE_ID,
                            1
                        )
                ),
                Aggregates.unwind(
                    "$" + TapeObjectReferentialEntity.LOCATION + "." + TapeLibraryTarObjectStorageLocation.TAR_ENTRIES
                ),
                Aggregates.unwind(
                    "$" +
                    TapeObjectReferentialEntity.LOCATION +
                    "." +
                    TapeLibraryTarObjectStorageLocation.TAR_ENTRIES +
                    "." +
                    TarEntryDescription.TAR_FILE_ID
                ),
                // Deduplicate tarIds
                Aggregates.group(
                    "$" +
                    TapeObjectReferentialEntity.LOCATION +
                    "." +
                    TapeLibraryTarObjectStorageLocation.TAR_ENTRIES +
                    "." +
                    TarEntryDescription.TAR_FILE_ID
                )
            );

            try (MongoCursor<Document> iterator = collection.aggregate(pipeline).allowDiskUse(true).iterator()) {
                while (iterator.hasNext()) {
                    Document document = iterator.next();
                    archiveIds.add(document.getString("_id"));
                }
            } catch (MongoException ex) {
                throw new ObjectReferentialException("Could not find archiveIds by objectIds", ex);
            }
        }
        return archiveIds;
    }

    private Document toBson(Object object) {
        return Document.parse(JsonHandler.unprettyPrint(object));
    }

    private TapeObjectReferentialEntity fromBson(Document document) throws InvalidParseOperationException {
        return BsonHelper.fromDocumentToObject(document, TapeObjectReferentialEntity.class);
    }

    public CloseableIterator<ObjectEntry> listContainerObjectEntries(String containerName)
        throws ObjectReferentialException {
        try {
            MongoCursor<Document> mongoCursor = collection
                .find(
                    Filters.eq(
                        TapeObjectReferentialEntity.ID + "." + TapeLibraryObjectReferentialId.CONTAINER_NAME,
                        containerName
                    )
                )
                .projection(
                    Projections.include(
                        TapeObjectReferentialEntity.ID + "." + TapeLibraryObjectReferentialId.OBJECT_NAME,
                        TapeObjectReferentialEntity.SIZE
                    )
                )
                .batchSize(VitamConfiguration.getBatchSize())
                .iterator();

            return new CloseableIterator<>() {
                @Override
                public void close() {
                    mongoCursor.close();
                }

                @Override
                public boolean hasNext() {
                    return mongoCursor.hasNext();
                }

                @Override
                public ObjectEntry next() {
                    Document document = mongoCursor.next();
                    Document documentId = (Document) document.get(TapeObjectReferentialEntity.ID);
                    return new ObjectEntry(
                        documentId.getString(TapeLibraryObjectReferentialId.OBJECT_NAME),
                        // Handle both Integer & Long values
                        document.get(TapeObjectReferentialEntity.SIZE, Number.class).longValue()
                    );
                }
            };
        } catch (MongoException ex) {
            throw new ObjectReferentialException("Could not list objects of container " + containerName, ex);
        }
    }
}
