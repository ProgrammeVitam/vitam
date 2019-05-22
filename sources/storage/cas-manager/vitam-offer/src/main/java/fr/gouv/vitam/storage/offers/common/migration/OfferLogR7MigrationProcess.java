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
package fr.gouv.vitam.storage.offers.common.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.iterables.BulkIterator;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.OfferLogFormatVersion;
import fr.gouv.vitam.storage.offers.common.core.DefaultOfferService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME;

public class OfferLogR7MigrationProcess {

    private static final String SEQUENCE = "Sequence";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferLogR7MigrationProcess.class);
    public static final int NB_THREADS = 16;

    private final DefaultOfferService defaultOfferService;
    private OfferMigrationStatus offerMigrationStatus;
    private MongoCollection<Document> mongoCollection;


    public OfferLogR7MigrationProcess(DefaultOfferService defaultOfferService,
        MongoDatabase mongoDatabase) {

        this.defaultOfferService = defaultOfferService;
        this.offerMigrationStatus = new OfferMigrationStatus(
            VitamThreadUtils.getVitamSession().getRequestId(), StatusCode.UNKNOWN, null, null,
            null, null);
        this.mongoCollection = mongoDatabase.getCollection(OFFER_LOG_COLLECTION_NAME);
    }

    public void run(Long startOffset) {
        try {

            ExecutorService executorService =
                Executors.newFixedThreadPool(NB_THREADS, VitamThreadFactory.getInstance());

            LOGGER.info("Starting migration...");
            this.offerMigrationStatus = new OfferMigrationStatus(
                VitamThreadUtils.getVitamSession().getRequestId(), StatusCode.UNKNOWN, getCurrentDate(), null,
                startOffset, null);

            runMigration(executorService, startOffset);

            this.offerMigrationStatus.setStatusCode(StatusCode.OK);
            LOGGER.info("Migration succeeded...");

        } catch (Exception ex) {
            LOGGER.error("An error occurred during migration", ex);
            this.offerMigrationStatus.setStatusCode(StatusCode.KO);
        } finally {
            this.offerMigrationStatus.setEndDate(getCurrentDate());
        }
    }

    private void runMigration(ExecutorService executorService, Long offset) {

        Iterator<List<Document>> bulkIterator = listOfferLogs(offset);

        while (bulkIterator.hasNext()) {
            List<Document> offerLogList = bulkIterator.next();
            if (!offerLogList.isEmpty()) {
                migrateBulk(executorService, offerLogList);
                long lastOffset = ((Number) offerLogList.get(offerLogList.size() - 1).get("Sequence")).longValue();
                LOGGER.warn("OfferLog migration offset snapshot: " + lastOffset);
                this.offerMigrationStatus.setCurrentOffset(lastOffset);
            }
        }
    }

    private Iterator<List<Document>> listOfferLogs(Long startOffset) {
        Bson sequenceSort = Sorts.orderBy(Sorts.ascending(SEQUENCE));

        Bson offsetQuery = Filters.or(
            Filters.eq("_FormatVersion", OfferLogFormatVersion.V1.name()),
            // Defaults to V1
            Filters.exists("_FormatVersion", false)
        );
        if (startOffset != null) {
            offsetQuery = and(offsetQuery, gte(SEQUENCE, startOffset));
        }

        Bson searchFilter = offsetQuery;
        MongoCursor<Document> dbIterator = mongoCollection.find(searchFilter).sort(sequenceSort).iterator();

        return new BulkIterator<>(dbIterator, VitamConfiguration.getBatchSize());
    }

    private void migrateBulk(ExecutorService executorService, List<Document> offerLogList) {

        List<CompletableFuture<ObjectId>> futures = new ArrayList<>();
        for (Document offerLog : offerLogList) {
            CompletableFuture<ObjectId> completableFuture = CompletableFuture.supplyAsync(() -> {

                String container = offerLog.getString("Container");
                String fileName = offerLog.getString("FileName");

                boolean objectExists;
                try {
                    objectExists =
                        this.defaultOfferService.isObjectExist(container, fileName);
                } catch (ContentAddressableStorageServerException e) {
                    throw new RuntimeException(e);
                }

                if (!objectExists) {
                    LOGGER.warn("File not found in offer " + container + "/" + fileName
                        + ". Will be DELETED");
                    return offerLog.getObjectId("_id");
                }

                LOGGER.info("File successfully found in offer " + container + "/" + fileName);
                return null;
            }, executorService);
            futures.add(completableFuture);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<ObjectId> offerLogDocumentIdsToDelete = futures.stream()
            .map(offerLogCompletableFuture -> {
                try {
                    return offerLogCompletableFuture.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        mongoCollection.deleteMany(
            Filters.in("_id", offerLogDocumentIdsToDelete)
        );
    }

    public boolean isRunning() {
        return this.offerMigrationStatus.getEndDate() == null;
    }

    public OfferMigrationStatus getMigrationStatus() {
        return this.offerMigrationStatus;
    }

    private String getCurrentDate() {
        return LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
    }
}
