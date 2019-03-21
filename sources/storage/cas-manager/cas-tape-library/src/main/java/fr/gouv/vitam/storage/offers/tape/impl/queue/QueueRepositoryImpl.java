/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.offers.tape.impl.queue;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.Calendar;
import java.util.Optional;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 *
 */
public class QueueRepositoryImpl implements QueueRepository {

    protected final MongoCollection<Document> collection;

    public QueueRepositoryImpl(MongoCollection<Document> collection) {
        ParametersChecker.checkParameter("Collection param is required", collection);
        this.collection = collection;
    }

    @Override
    public void add(QueueMessageEntity queue) throws QueueException {
        try {
            Document doc = Document.parse(JsonHandler.unprettyPrint(queue));
            collection.insertOne(doc);
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public long remove(String queueId) throws QueueException {
        try {
            return collection.deleteOne(eq(VitamDocument.ID, queueId)).getDeletedCount();
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public long complete(String queueId) throws QueueException {
        try {
            return collection.updateOne(eq(VitamDocument.ID, queueId),
                Updates.set(QueueMessageEntity.STATE, QueueState.COMPLETED.getState())).getModifiedCount();
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public long markError(String queueMessageId) throws QueueException {
        try {
            return collection.updateOne(eq(VitamDocument.ID, queueMessageId),
                Updates.set(QueueMessageEntity.STATE, QueueState.ERROR.getState())).getModifiedCount();
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public long markReady(String queueMessageId) throws QueueException {
        try {
            return collection.updateOne(eq(VitamDocument.ID, queueMessageId),
                Updates.set(QueueMessageEntity.STATE, QueueState.READY.getState())).getModifiedCount();
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public <T> Optional<T> receive(QueueMessageType messageType) throws QueueException {
        return receive(messageType, true);
    }

    @Override
    public <T> Optional<T> receive(QueueMessageType messageType, boolean usePriority) throws QueueException {
        return receive(null, null, messageType, usePriority);
    }

    @Override
    public <T> Optional<T> receive(Bson inQuery, QueueMessageType messageType) throws QueueException {
        return receive(inQuery, messageType, true);
    }

    @Override
    public <T> Optional<T> receive(Bson inQuery, QueueMessageType messageType, boolean usePriority)
        throws QueueException {
        return receive(inQuery, null, messageType, usePriority);
    }

    @Override
    public <T> Optional<T> receive(Bson inQuery, Bson inUpdate, QueueMessageType messageType) throws QueueException {
        return receive(inQuery, inUpdate, messageType, true);
    }

    @Override
    public <T> Optional<T> receive(Bson inQuery, Bson inUpdate, QueueMessageType messageType, boolean usePriority)
        throws QueueException {

        Bson query = inQuery != null ?
            and(eq(QueueMessageEntity.STATE, QueueState.READY.getState()),
                eq(QueueMessageEntity.MESSAGE_TYPE, messageType.name()), inQuery)
            :
            and(eq(QueueMessageEntity.STATE, QueueState.READY.getState()),
                eq(QueueMessageEntity.MESSAGE_TYPE, messageType.name()));

        FindOneAndUpdateOptions option = new FindOneAndUpdateOptions();
        option.returnDocument(ReturnDocument.AFTER);
        if (usePriority) {
            option.sort(Sorts.ascending(QueueMessageEntity.PRIORITY, QueueMessageEntity.CREATED));
        } else {
            option.sort(Sorts.ascending(QueueMessageEntity.CREATED));
        }
        option.upsert(false);

        Bson update = inUpdate != null ?
            Updates.combine(
                Updates.set(QueueMessageEntity.STATE, QueueState.RUNNING.getState()),
                Updates.set(QueueMessageEntity.LAST_UPDATE, Calendar.getInstance().getTimeInMillis()),
                inUpdate)
            :
            Updates.combine(
                Updates.set(QueueMessageEntity.STATE, QueueState.RUNNING.getState()),
                Updates.set(QueueMessageEntity.LAST_UPDATE, Calendar.getInstance().getTimeInMillis()));

        Document sequence = collection.findOneAndUpdate(query, update, option);

        if (null == sequence) {
            return Optional.empty();
        }

        try {
            return Optional.of(JsonHandler.getFromString(JSON.serialize(sequence), (Class<T>) messageType.getClazz()));
        } catch (InvalidParseOperationException e) {
            throw new QueueException(e);
        }

    }

}
