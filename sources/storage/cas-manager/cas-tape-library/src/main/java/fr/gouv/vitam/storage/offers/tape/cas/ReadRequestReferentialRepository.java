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
package fr.gouv.vitam.storage.offers.tape.cas;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarLocation;
import fr.gouv.vitam.storage.offers.tape.exception.ReadRequestReferentialException;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadTask;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.Optional;

public class ReadRequestReferentialRepository implements ReadRequestReferentialCleaner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReadTask.class);

    private final MongoCollection<Document> collection;

    public ReadRequestReferentialRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public void insert(TapeReadRequestReferentialEntity tapeReadRequestReferentialEntity)
        throws ReadRequestReferentialException {

        try {
            collection.insertOne(toBson(tapeReadRequestReferentialEntity));
        } catch (MongoException ex) {
            throw new ReadRequestReferentialException(
                "Could not insert or update read requests referential for id " +
                    tapeReadRequestReferentialEntity.getRequestId(), ex);
        }
    }

    public Optional<TapeReadRequestReferentialEntity> find(String requestId)
        throws ReadRequestReferentialException {

        Document document;

        try {
            document = collection.find(
                Filters.eq(TapeReadRequestReferentialEntity.ID, requestId))
                .first();
        } catch (MongoException ex) {
            throw new ReadRequestReferentialException("Could not find read request by id " + requestId, ex);
        }

        if (document == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(fromBson(document, TapeReadRequestReferentialEntity.class));
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException("Could not parse document from DB " + JSON.serialize(document), e);
        }
    }

    /**
     * Update location of a given archive id in all read request where this archive id exists
     *
     * @param archiveId
     * @param tarLocation
     * @throws ReadRequestReferentialException
     */
    public void updateReadRequests(String archiveId, TarLocation tarLocation)
        throws ReadRequestReferentialException {

        try {
            UpdateResult updateResult = collection.updateMany(
                Filters.exists(TapeReadRequestReferentialEntity.TAR_LOCATIONS + "." + archiveId),
                Updates.set(TapeReadRequestReferentialEntity.TAR_LOCATIONS + "." + archiveId, tarLocation.name()),
                new UpdateOptions().upsert(false)
            );


            if (updateResult.getModifiedCount() == 0) {
                LOGGER.warn("No read request modified with archive :" + archiveId +
                    ", Matched read request: " +
                    updateResult.getMatchedCount());
            } else {
                LOGGER.debug(updateResult.getModifiedCount() + " read request updated with archive :" + archiveId +
                    " location: " + tarLocation.name());
            }
        } catch (MongoException ex) {
            throw new ReadRequestReferentialException("Could not update read request for " + archiveId, ex);
        }
    }

    private Document toBson(Object object) {
        return Document.parse(JsonHandler.unprettyPrint(object));
    }

    private <T> T fromBson(Document document, Class<T> clazz)
        throws InvalidParseOperationException {
        return JsonHandler.getFromString(JSON.serialize(document), clazz);
    }

    @Override
    public long cleanUp() throws ReadRequestReferentialException {
        try {
            DeleteResult request =
                collection.deleteMany(Filters.eq(TapeReadRequestReferentialEntity.IS_EXPIRED, true));
            return request.getDeletedCount();
        } catch (MongoException ex) {
            throw new ReadRequestReferentialException("Could not cleanup expired read request", ex);
        }
    }

    @Override
    public void invalidate(String readOrderRequestId) throws ReadRequestReferentialException {
        try {
            UpdateResult updateResult = collection.updateOne(
                Filters.eq(TapeReadRequestReferentialEntity.ID, readOrderRequestId),
                Updates.combine(Updates.set(TapeReadRequestReferentialEntity.IS_EXPIRED, true),
                    Updates.set(TapeReadRequestReferentialEntity.EXPIRE_DATE, LocalDateUtil
                        .getFormattedDateForMongo(LocalDateTime.now()))),
                new UpdateOptions().upsert(false)
            );

            if (updateResult.getModifiedCount() != 1) {
                throw new ReadRequestReferentialException(
                    "Could not update read request for " + readOrderRequestId + ". No such read request");
            }
        } catch (MongoException ex) {
            throw new ReadRequestReferentialException("Could not update read request for " + readOrderRequestId, ex);
        }
    }
}
