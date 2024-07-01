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

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import fr.gouv.vitam.storage.engine.common.model.OfferSequence;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.storage.engine.common.model.OfferSequence.COUNTER_FIELD;

public class OfferSequenceDatabaseService {

    public static final String BACKUP_LOG_SEQUENCE_ID = "Backup_Log_Sequence";

    private MongoCollection<Document> mongoCollection;

    public OfferSequenceDatabaseService(MongoCollection<Document> mongoCollection) {
        this.mongoCollection = mongoCollection;
    }

    public long getNextSequence(String sequenceId) throws ContentAddressableStorageDatabaseException {
        return getNextSequence(sequenceId, 1L);
    }

    public long getNextSequence(String sequenceId, long inc) throws ContentAddressableStorageDatabaseException {
        try {
            final BasicDBObject incQuery = new BasicDBObject();
            incQuery.append("$inc", new BasicDBObject(COUNTER_FIELD, inc));
            Bson query = eq(OfferSequence.ID_FIELD, sequenceId);
            FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();
            findOneAndUpdateOptions.returnDocument(ReturnDocument.AFTER);
            findOneAndUpdateOptions.upsert(true);

            Document sequence = mongoCollection.findOneAndUpdate(query, incQuery, findOneAndUpdateOptions);
            if (sequence != null) {
                return sequence.getLong(COUNTER_FIELD) + 1L - inc;
            } else {
                throw new ContentAddressableStorageDatabaseException(
                    String.format("Database Error sequence %s not found", sequenceId)
                );
            }
        } catch (MongoException e) {
            throw new ContentAddressableStorageDatabaseException(
                String.format("Database Error while getting next sequence value for %s", sequenceId),
                e
            );
        }
    }
}
