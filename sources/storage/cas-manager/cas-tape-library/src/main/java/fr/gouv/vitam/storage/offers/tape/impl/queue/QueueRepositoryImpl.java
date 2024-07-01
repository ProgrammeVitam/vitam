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
package fr.gouv.vitam.storage.offers.tape.impl.queue;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.utils.QueryCriteriaUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 *
 */
public class QueueRepositoryImpl implements QueueRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(QueueRepositoryImpl.class);

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
    public void addIfAbsent(List<QueryCriteria> criteria, QueueMessageEntity queueMessageEntity) throws QueueException {
        try {
            List<Bson> filters = QueryCriteriaUtils.criteriaToMongoFilters(criteria);

            if (collection.find(Filters.and(filters)).iterator().hasNext()) {
                LOGGER.warn("Message already in queue " + JsonHandler.unprettyPrint(queueMessageEntity));
            } else {
                Document doc = Document.parse(JsonHandler.unprettyPrint(queueMessageEntity));
                collection.insertOne(doc);
            }
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public void tryCancelIfNotStarted(List<QueryCriteria> criteria) throws QueueException {
        try {
            List<Bson> filters = QueryCriteriaUtils.criteriaToMongoFilters(criteria);
            filters.add(Filters.ne(QueueMessageEntity.STATE, QueueState.RUNNING.getState()));

            DeleteResult deleteResult = collection.deleteOne(and(filters));
            if (deleteResult.getDeletedCount() != 1) {
                LOGGER.warn("Message could not be cancelled. Already running?");
            }
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
            return collection
                .updateOne(
                    eq(VitamDocument.ID, queueId),
                    Updates.set(QueueMessageEntity.STATE, QueueState.COMPLETED.getState())
                )
                .getModifiedCount();
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public long markError(String queueMessageId) throws QueueException {
        try {
            return collection
                .updateOne(
                    eq(VitamDocument.ID, queueMessageId),
                    Updates.set(QueueMessageEntity.STATE, QueueState.ERROR.getState())
                )
                .getModifiedCount();
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public long markReady(String queueMessageId) throws QueueException {
        try {
            return collection
                .updateOne(
                    eq(VitamDocument.ID, queueMessageId),
                    Updates.set(QueueMessageEntity.STATE, QueueState.READY.getState())
                )
                .getModifiedCount();
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }

    @Override
    public long initializeOnBootstrap() {
        try {
            return collection
                .updateMany(
                    eq(QueueMessageEntity.STATE, QueueState.RUNNING.getState()),
                    Updates.set(QueueMessageEntity.STATE, QueueState.READY.getState())
                )
                .getModifiedCount();
        } catch (Exception e) {
            throw new VitamRuntimeException(e);
        }
    }

    @Override
    public <T> Optional<T> receive(QueueMessageType messageType) throws QueueException {
        return receive(messageType, true);
    }

    @Override
    public <T> Optional<T> receive(QueueMessageType messageType, boolean usePriority) throws QueueException {
        return receive(null, messageType, usePriority);
    }

    @Override
    public <T> Optional<T> receive(Bson inQuery, QueueMessageType messageType) throws QueueException {
        return receive(inQuery, messageType, true);
    }

    @Override
    public <T> Optional<T> receive(Bson inQuery, QueueMessageType messageType, boolean usePriority)
        throws QueueException {
        Bson query = inQuery != null
            ? and(
                eq(QueueMessageEntity.STATE, QueueState.READY.getState()),
                eq(QueueMessageEntity.MESSAGE_TYPE, messageType.name()),
                inQuery
            )
            : and(
                eq(QueueMessageEntity.STATE, QueueState.READY.getState()),
                eq(QueueMessageEntity.MESSAGE_TYPE, messageType.name())
            );

        FindOneAndUpdateOptions option = new FindOneAndUpdateOptions();
        option.returnDocument(ReturnDocument.AFTER);
        if (usePriority) {
            option.sort(Sorts.ascending(QueueMessageEntity.PRIORITY, QueueMessageEntity.TAG_CREATION_DATE));
        } else {
            option.sort(Sorts.ascending(QueueMessageEntity.TAG_CREATION_DATE));
        }
        option.upsert(false);

        Bson update = Updates.combine(
            Updates.set(QueueMessageEntity.STATE, QueueState.RUNNING.getState()),
            Updates.set(QueueMessageEntity.TAG_LAST_UPDATE, Calendar.getInstance().getTimeInMillis())
        );

        Document sequence = collection.findOneAndUpdate(query, update, option);

        if (null == sequence) {
            return Optional.empty();
        }

        try {
            return Optional.of(BsonHelper.fromDocumentToObject(sequence, (Class<T>) messageType.getClazz()));
        } catch (InvalidParseOperationException e) {
            throw new QueueException(e);
        }
    }

    /**
     * count queue entries grouped by state & message type
     *
     * @return number of queue entries by state & message type
     */
    public Map<Pair<QueueState, QueueMessageType>, Integer> countByStateAndType() throws QueueException {
        try {
            AggregateIterable<Document> aggregate = collection.aggregate(
                List.of(
                    Aggregates.group(
                        new Document()
                            .append("state", "$" + QueueMessageEntity.STATE)
                            .append("type", "$" + QueueMessageEntity.MESSAGE_TYPE),
                        Accumulators.sum("count", 1)
                    )
                )
            );

            Map<Pair<QueueState, QueueMessageType>, Integer> results = new HashMap<>();
            try (MongoCursor<Document> iterator = aggregate.iterator()) {
                while (iterator.hasNext()) {
                    Document doc = iterator.next();
                    Document _id = (Document) doc.get("_id");

                    String stateStr = _id.getString("state");
                    QueueState queueState = QueueState.valueOf(stateStr);

                    String typeStr = _id.getString("type");
                    QueueMessageType type = QueueMessageType.valueOf(typeStr);

                    int count = doc.getInteger("count");

                    results.put(new ImmutablePair<>(queueState, type), count);
                }
            }

            return results;
        } catch (Exception e) {
            throw new QueueException(e);
        }
    }
}
