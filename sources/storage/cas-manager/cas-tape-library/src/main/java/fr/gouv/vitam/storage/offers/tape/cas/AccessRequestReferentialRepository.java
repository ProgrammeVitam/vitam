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
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.TapeAccessRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.offers.tape.exception.AccessRequestReferentialException;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class AccessRequestReferentialRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessRequestReferentialRepository.class);

    private final MongoCollection<Document> collection;
    private final int bulkSize;

    public AccessRequestReferentialRepository(MongoCollection<Document> collection) {
        this(collection, VitamConfiguration.getBatchSize());
    }

    @VisibleForTesting
    AccessRequestReferentialRepository(MongoCollection<Document> collection, int bulkSize) {
        this.collection = collection;
        this.bulkSize = bulkSize;
    }

    public void insert(TapeAccessRequestReferentialEntity accessRequestReferentialEntity)
        throws AccessRequestReferentialException {
        try {
            collection.insertOne(toBson(accessRequestReferentialEntity));
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException(
                "Could not insert access requests referential for id " + accessRequestReferentialEntity.getRequestId(),
                ex
            );
        }
    }

    public Optional<TapeAccessRequestReferentialEntity> findByRequestId(String requestId)
        throws AccessRequestReferentialException {
        try {
            TapeAccessRequestReferentialEntity accessRequestReferentialEntity = collection
                .find(Filters.eq(TapeAccessRequestReferentialEntity.ID, requestId))
                .map(this::toModel)
                .first();
            return Optional.ofNullable(accessRequestReferentialEntity);
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not find read request by id " + requestId, ex);
        }
    }

    public List<TapeAccessRequestReferentialEntity> findByRequestIds(Set<String> requestIds)
        throws AccessRequestReferentialException {
        try {
            if (requestIds.isEmpty()) {
                return emptyList();
            }

            List<TapeAccessRequestReferentialEntity> results = new ArrayList<>();
            for (List<String> requestIdsBulk : IteratorUtils.asIterable(
                Iterators.partition(requestIds.iterator(), bulkSize)
            )) {
                collection
                    .find(Filters.in(TapeAccessRequestReferentialEntity.ID, requestIdsBulk))
                    .map(this::toModel)
                    .forEach((Consumer<TapeAccessRequestReferentialEntity>) results::add);
            }
            return results;
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not find read request by ids", ex);
        }
    }

    public long countNonReadyAccessRequests() throws AccessRequestReferentialException {
        try {
            return collection.countDocuments(
                Filters.and(
                    Filters.exists(TapeAccessRequestReferentialEntity.UNAVAILABLE_ARCHIVE_IDS),
                    Filters.ne(TapeAccessRequestReferentialEntity.UNAVAILABLE_ARCHIVE_IDS, emptyList())
                )
            );
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not count non-ready access requests", ex);
        }
    }

    public long countReadyAccessRequests() throws AccessRequestReferentialException {
        try {
            String now = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

            return collection.countDocuments(Filters.gte(TapeAccessRequestReferentialEntity.EXPIRATION_DATE, now));
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not count ready access requests", ex);
        }
    }

    public long countExpiredAccessRequests() throws AccessRequestReferentialException {
        try {
            String now = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

            return collection.countDocuments(Filters.lt(TapeAccessRequestReferentialEntity.EXPIRATION_DATE, now));
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not count expired access requests", ex);
        }
    }

    public List<TapeAccessRequestReferentialEntity> findNonReadyAccessRequests()
        throws AccessRequestReferentialException {
        try {
            List<TapeAccessRequestReferentialEntity> results = new ArrayList<>();
            collection
                .find(
                    Filters.and(
                        Filters.exists(TapeAccessRequestReferentialEntity.UNAVAILABLE_ARCHIVE_IDS),
                        Filters.ne(TapeAccessRequestReferentialEntity.UNAVAILABLE_ARCHIVE_IDS, emptyList())
                    )
                )
                .map(this::toModel)
                .forEach((Consumer<TapeAccessRequestReferentialEntity>) results::add);
            return results;
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not find read request by ids", ex);
        }
    }

    public List<TapeAccessRequestReferentialEntity> findByUnavailableArchiveId(String archiveId)
        throws AccessRequestReferentialException {
        try {
            List<TapeAccessRequestReferentialEntity> results = new ArrayList<>();
            collection
                .find(Filters.in(TapeAccessRequestReferentialEntity.UNAVAILABLE_ARCHIVE_IDS, archiveId))
                .map(this::toModel)
                .forEach((Consumer<TapeAccessRequestReferentialEntity>) results::add);
            return results;
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not find read request by ids", ex);
        }
    }

    public boolean deleteAccessRequestById(String accessRequestId) throws AccessRequestReferentialException {
        try {
            DeleteResult deleteResult = collection.deleteOne(
                Filters.eq(TapeAccessRequestReferentialEntity.ID, accessRequestId)
            );

            if (deleteResult.getDeletedCount() == 0) {
                // LOG & continue (idempotency)
                LOGGER.warn("No such AccessRequest: " + accessRequestId + ". Concurrent delete?");
                return false;
            }

            LOGGER.warn("Access request deleted successfully: " + accessRequestId);
            return true;
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not delete access request for " + accessRequestId, ex);
        }
    }

    public List<TapeAccessRequestReferentialEntity> cleanupAndGetExpiredAccessRequests()
        throws AccessRequestReferentialException {
        try {
            String now = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

            List<TapeAccessRequestReferentialEntity> expiredAccessRequests = new ArrayList<>();
            try (
                MongoCursor<Document> expiredAccessRequestCursor = collection
                    .find(Filters.lt(TapeAccessRequestReferentialEntity.PURGE_DATE, now))
                    .cursor()
            ) {
                expiredAccessRequestCursor.forEachRemaining(document -> expiredAccessRequests.add(toModel(document)));
            }

            for (TapeAccessRequestReferentialEntity expiredAccessRequest : expiredAccessRequests) {
                LOGGER.warn(
                    "Deleting expired access request " +
                    expiredAccessRequest.getRequestId() +
                    ", lastUpdateDate: " +
                    expiredAccessRequest.getReadyDate()
                );
            }

            for (List<TapeAccessRequestReferentialEntity> accessRequestBulkToDelete : ListUtils.partition(
                expiredAccessRequests,
                VitamConfiguration.getBatchSize()
            )) {
                collection.deleteMany(
                    Filters.in(
                        TapeAccessRequestReferentialEntity.ID,
                        accessRequestBulkToDelete
                            .stream()
                            .map(TapeAccessRequestReferentialEntity::getRequestId)
                            .collect(Collectors.toList())
                    )
                );
            }

            return expiredAccessRequests;
        } catch (MongoException ex) {
            throw new AccessRequestReferentialException("Could not purge expired access requests.", ex);
        }
    }

    public Set<String> excludeArchiveIdsStillRequiredByAccessRequests(Set<String> archiveIdsToCheck) {
        if (archiveIdsToCheck.isEmpty()) {
            return Collections.emptySet();
        }

        // Prefill with all archive ids to check
        Set<String> inUseArchiveIds = new HashSet<>(archiveIdsToCheck);

        // Should we filter by bulk archiveIdsToCheck?
        for (List<String> archiveIdsBulk : IteratorUtils.asIterable(
            Iterators.partition(archiveIdsToCheck.iterator(), bulkSize)
        )) {
            DistinctIterable<String> distinct = collection.distinct(
                TapeAccessRequestReferentialEntity.UNAVAILABLE_ARCHIVE_IDS,
                Filters.in(TapeAccessRequestReferentialEntity.UNAVAILABLE_ARCHIVE_IDS, archiveIdsBulk),
                String.class
            );
            distinct.forEach((Consumer<? super String>) inUseArchiveIds::remove);
        }
        return inUseArchiveIds;
    }

    public boolean updateAccessRequest(
        TapeAccessRequestReferentialEntity updatedAccessRequestEntity,
        int expectedVersion
    ) throws AccessRequestReferentialException {
        try {
            UpdateResult updateResult = collection.replaceOne(
                Filters.and(
                    Filters.eq(TapeAccessRequestReferentialEntity.ID, updatedAccessRequestEntity.getRequestId()),
                    Filters.eq(TapeAccessRequestReferentialEntity.VERSION, expectedVersion)
                ),
                toBson(updatedAccessRequestEntity)
            );

            return (updateResult.getMatchedCount() != 0);
        } catch (MongoException e) {
            throw new AccessRequestReferentialException(
                "Could not update access request " + updatedAccessRequestEntity.getRequestId(),
                e
            );
        }
    }

    public CloseableIterator<TapeLibraryObjectReferentialId> listObjectIdsForActiveAccessRequests()
        throws AccessRequestReferentialException {
        try {
            // Select distinct containerName / objectName pairs of non-expired access requests
            String now = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

            List<Bson> aggregatePipeline = List.of(
                // Exclude expired access requests
                Aggregates.match(
                    Filters.or(
                        Filters.eq(TapeAccessRequestReferentialEntity.EXPIRATION_DATE, null),
                        Filters.gte(TapeAccessRequestReferentialEntity.EXPIRATION_DATE, now)
                    )
                ),
                // Only select objectNames & containerName
                Aggregates.project(
                    new Document()
                        .append(TapeAccessRequestReferentialEntity.ID, 0)
                        .append(TapeAccessRequestReferentialEntity.OBJECT_NAMES, 1)
                        .append(TapeAccessRequestReferentialEntity.CONTAINER_NAME, 1)
                ),
                // Unwind objectNames
                Aggregates.unwind("$" + TapeAccessRequestReferentialEntity.OBJECT_NAMES),
                // Deduplicate { "containerName": "<containerName>" , "objectName": "<objectName>" } pairs
                Aggregates.group(
                    new Document()
                        .append(
                            TapeLibraryObjectReferentialId.CONTAINER_NAME,
                            "$" + TapeAccessRequestReferentialEntity.CONTAINER_NAME
                        )
                        .append(
                            TapeLibraryObjectReferentialId.OBJECT_NAME,
                            "$" + TapeAccessRequestReferentialEntity.OBJECT_NAMES
                        )
                )
            );

            AggregateIterable<Document> aggregate = collection.aggregate(aggregatePipeline).allowDiskUse(true);
            MongoCursor<Document> iterator = aggregate.iterator();

            return new CloseableIterator<>() {
                @Override
                public void close() {
                    iterator.close();
                }

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public TapeLibraryObjectReferentialId next() {
                    Document docId = iterator.next().get("_id", Document.class);
                    return new TapeLibraryObjectReferentialId(
                        docId.getString(TapeLibraryObjectReferentialId.CONTAINER_NAME),
                        docId.getString(TapeLibraryObjectReferentialId.OBJECT_NAME)
                    );
                }
            };
        } catch (MongoException e) {
            throw new AccessRequestReferentialException(
                "Could not fetch archive ids required by active access requests",
                e
            );
        }
    }

    private Document toBson(TapeAccessRequestReferentialEntity object) {
        return Document.parse(JsonHandler.unprettyPrint(object));
    }

    private TapeAccessRequestReferentialEntity toModel(Document document) throws IllegalStateException {
        try {
            return BsonHelper.fromDocumentToObject(document, TapeAccessRequestReferentialEntity.class);
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException("Could not parse document from DB " + document.toJson(), e);
        }
    }
}
