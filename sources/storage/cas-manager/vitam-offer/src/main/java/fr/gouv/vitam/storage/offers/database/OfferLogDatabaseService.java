/**
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
 */

package fr.gouv.vitam.storage.offers.database;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;

/**
 * Database service for access to OfferLog collection.
 */
public class OfferLogDatabaseService {

    private static final String SEQUENCE = "Sequence";
    private static final String CONTAINER = "Container";

    private MongoCollection<Document> mongoCollection;

    private OfferSequenceDatabaseService offerSequenceDatabaseService;

    /**
     * Constructor
     *
     * @param offerSequenceDatabaseService offerSequenceService
     * @param mongoDatabase mongoDatabase
     */
    public OfferLogDatabaseService(OfferSequenceDatabaseService offerSequenceDatabaseService,
        MongoDatabase mongoDatabase) {
        this.mongoCollection = mongoDatabase.getCollection(OfferCollections.OFFER_LOG.getName());
        this.offerSequenceDatabaseService = offerSequenceDatabaseService;
    }

    /**
     * Save on offerLog.
     *
     * @param containerName name of the container
     * @param fileName file name
     * @param action action
     * @throws ContentAddressableStorageServerException parsing error
     * @throws ContentAddressableStorageDatabaseException database error
     */
    public void save(String containerName, String fileName, OfferLogAction action)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        try {
            OfferLog offerLog = new OfferLog(containerName, fileName, action);
            offerLog.setSequence(
                offerSequenceDatabaseService.getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID));
            String json;
            try {
                json = JsonHandler.writeAsString(offerLog);
            } catch (InvalidParseOperationException exc) {
                throw new ContentAddressableStorageServerException("Cannot parse storage log", exc);
            }
            mongoCollection.insertOne(Document.parse(json));
        } catch (MongoException e) {
            throw new ContentAddressableStorageDatabaseException(String.format(
                "Database Error while saving %s in OfferLog collection", fileName), e);
        }
    }

    /**
     * Save on offerLog.
     *
     * @param containerName name of the container
     * @param fileNames file names
     * @param action action
     * @throws ContentAddressableStorageServerException parsing error
     * @throws ContentAddressableStorageDatabaseException database error
     */
    public void bulkSave(String containerName, List<String> fileNames, OfferLogAction action)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        try {
            long nextSequence = offerSequenceDatabaseService
                .getNextSequence(OfferSequenceDatabaseService.BACKUP_LOG_SEQUENCE_ID, fileNames.size());

            List<Document> documents = new ArrayList<>();
            for (String fileName : fileNames) {
                OfferLog offerLog = new OfferLog(containerName, fileName, action);
                offerLog.setSequence(nextSequence);

                String json;
                try {
                    json = JsonHandler.writeAsString(offerLog);
                } catch (InvalidParseOperationException exc) {
                    throw new ContentAddressableStorageServerException("Cannot parse storage log", exc);
                }

                documents.add(Document.parse(json));
                nextSequence++;
            }

            mongoCollection.insertMany(documents, new InsertManyOptions().ordered(false));

        } catch (MongoException e) {
            throw new ContentAddressableStorageDatabaseException(String.format(
                "Database Error while saving %s in OfferLog collection", fileNames), e);
        }
    }

    /**
     * Search in offer log
     *
     * @param containerName container name
     * @param offset sequence offset
     * @param limit max number of result
     * @param order order
     * @return list of offer logs
     * @throws ContentAddressableStorageDatabaseException database error
     * @throws ContentAddressableStorageServerException parsing error
     */
    public List<OfferLog> searchOfferLog(String containerName, Long offset, int limit, Order order)
        throws ContentAddressableStorageDatabaseException, ContentAddressableStorageServerException {
        try {
            List<OfferLog> offerLog = new ArrayList<>();
            Bson containerQuery = eq(CONTAINER, containerName);
            Bson offsetQuery = null;

            Bson sequenceSort;
            if (Order.ASC.equals(order)) {
                sequenceSort = Sorts.orderBy(Sorts.ascending(SEQUENCE));
                if (offset != null) {
                    offsetQuery = gte(SEQUENCE, offset);
                }
            } else {
                sequenceSort = Sorts.orderBy(Sorts.descending(SEQUENCE));
                if (offset != null) {
                    offsetQuery = lte(SEQUENCE, offset);
                }
            }
            Bson searchFilter = containerQuery;
            if (offsetQuery != null) {
                searchFilter = and(containerQuery, offsetQuery);
            }
            FindIterable<Document> result = mongoCollection.find(searchFilter).limit(limit).sort(sequenceSort);
            for (Document document : result) {
                offerLog.add(JsonHandler.getFromString(JSON.serialize(document), OfferLog.class));
            }
            return offerLog;
        } catch (MongoException e) {
            throw new ContentAddressableStorageDatabaseException(String.format(
                "Database Error while getting OfferLog for container %s", containerName), e);
        } catch (InvalidParseOperationException e) {
            throw new ContentAddressableStorageServerException(
                String.format("Parsing Error while loading offerLog from database for container %s", containerName),
                e);
        }
    }
}
