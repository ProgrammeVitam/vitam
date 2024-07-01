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
package fr.gouv.vitam.storage.offers.database;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog.SEQUENCE_END;
import static fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog.SEQUENCE_START;
import static fr.gouv.vitam.storage.engine.common.model.OfferLog.CONTAINER;
import static fr.gouv.vitam.storage.engine.common.model.OfferLog.SEQUENCE;

public class OfferLogAndCompactedOfferLogService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferLogAndCompactedOfferLogService.class);
    private static final AlertService alertService = new AlertServiceImpl();

    private final MongoCollection<Document> offerLogCollection;
    private final MongoCollection<Document> compactedOfferLogCollection;

    public OfferLogAndCompactedOfferLogService(
        MongoCollection<Document> offerLogCollection,
        MongoCollection<Document> compactedOfferLogCollection
    ) {
        this.offerLogCollection = offerLogCollection;
        this.compactedOfferLogCollection = compactedOfferLogCollection;
    }

    public void almostTransactionalSaveAndDelete(CompactedOfferLog toSave, List<OfferLog> toDelete) {
        try {
            save(toSave);
            delete(toDelete);
        } catch (Exception e) {
            // FIXME : This is a problem, it means we will have incoherent data between CompactedOfferLog and OfferLog collection.
            //  We MUST use transaction in order to have coherent data in those two collection, but 'no time' to finish this story.
            //  NB : Mongodb transactions requires a replica set deployment mode. Not testable right now on dev/build environments
            //  "com.mongodb.MongoClientException: Sessions are not supported by the MongoDB cluster to which this client is connected"

            LOGGER.error("An error occurred during offer log compaction. Possible CompactedOfferLog corruption", e);
            alertService.createAlert(
                "An error occurred during offer log compaction. Possible CompactedOfferLog corruption"
            );
            throw new VitamRuntimeException(e);
        }
    }

    private void save(CompactedOfferLog compactedOfferLog) {
        Bson filters = and(
            eq(SEQUENCE_START, compactedOfferLog.getSequenceStart()),
            eq(SEQUENCE_END, compactedOfferLog.getSequenceEnd()),
            eq(CompactedOfferLog.CONTAINER, compactedOfferLog.getContainer())
        );
        CompactedOfferLog alreadySavedOfferLogCompacted = compactedOfferLogCollection
            .find(filters)
            .map(this::transformDocumentToOfferLogCompaction)
            .first();
        if (alreadySavedOfferLogCompacted != null) {
            throw new VitamRuntimeException(
                String.format(
                    "Incoherent data between CompactedOfferLog and OfferLog collection, compaction offer logs '%s' already inserted.",
                    alreadySavedOfferLogCompacted
                )
            );
        }
        try {
            compactedOfferLogCollection.insertOne(Document.parse(JsonHandler.writeAsString(compactedOfferLog)));
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void delete(List<OfferLog> offerLogs) {
        OfferLog first = offerLogs.get(0);
        OfferLog end = offerLogs.get(offerLogs.size() - 1);

        offerLogCollection.deleteMany(
            and(
                eq(CONTAINER, first.getContainer()),
                lte(SEQUENCE, end.getSequence()),
                gte(SEQUENCE, first.getSequence())
            )
        );
    }

    private CompactedOfferLog transformDocumentToOfferLogCompaction(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, CompactedOfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
