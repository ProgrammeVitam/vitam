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
package fr.gouv.vitam.storage.offers.tape.cas;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.tape.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.offers.tape.exception.ReadRequestReferentialException;
import org.bson.Document;

import java.util.Optional;

public class ReadRequestReferentialRepository {

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
                "Could not insert or update read requests referential for id " + tapeReadRequestReferentialEntity.getRequestId(), ex);
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

    public void updateReadRequestInProgress(String requestId)
        throws ReadRequestReferentialException {

        try {
            UpdateResult updateResult = collection.updateOne(
                Filters.eq(TapeReadRequestReferentialEntity.ID, requestId),
                Updates.inc(TapeReadRequestReferentialEntity.CURRENT_READ_TARS_COUNT, 1),
                new UpdateOptions().upsert(false)
            );

            if (updateResult.getMatchedCount() != 1) {
                throw new ReadRequestReferentialException("Could not update read request for " + requestId + ". No such read request");
            }
        } catch (MongoException ex) {
            throw new ReadRequestReferentialException("Could not update read request for " + requestId, ex);
        }
    }

    private Document toBson(Object object) {
        return Document.parse(JsonHandler.unprettyPrint(object));
    }

    private <T> T fromBson(Document document, Class<T> clazz)
        throws InvalidParseOperationException {
        return JsonHandler.getFromString(JSON.serialize(document), clazz);
    }
}
