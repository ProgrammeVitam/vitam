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

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.collection.CloseableIterable;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.IteratorUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static fr.gouv.vitam.storage.engine.common.model.OfferLog.ACTION;
import static fr.gouv.vitam.storage.engine.common.model.OfferLog.CONTAINER;
import static fr.gouv.vitam.storage.engine.common.model.OfferLog.FILENAME;
import static fr.gouv.vitam.storage.engine.common.model.OfferLog.SEQUENCE;
import static fr.gouv.vitam.storage.engine.common.model.OfferLog.TIME;

public class OfferLogDatabaseService {

    private final MongoCollection<Document> mongoCollection;

    public OfferLogDatabaseService(MongoCollection<Document> mongoCollection) {
        this.mongoCollection = mongoCollection;
    }

    public void save(String containerName, String fileName, OfferLogAction action, long sequence)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        try {
            OfferLog offerLog = new OfferLog(sequence, LocalDateUtil.now(), containerName, fileName, action);
            mongoCollection.insertOne(Document.parse(JsonHandler.writeAsString(offerLog)));
        } catch (MongoException e) {
            throw new ContentAddressableStorageDatabaseException(
                String.format("Database Error while saving %s in OfferLog collection", fileName),
                e
            );
        } catch (InvalidParseOperationException e) {
            throw new ContentAddressableStorageServerException("Cannot parse storage log", e);
        }
    }

    public void bulkSave(String containerName, List<String> fileNames, OfferLogAction action, long sequence)
        throws ContentAddressableStorageServerException, ContentAddressableStorageDatabaseException {
        try {
            List<Document> documents = new ArrayList<>();
            for (String fileName : fileNames) {
                OfferLog offerLog = new OfferLog(sequence, LocalDateUtil.now(), containerName, fileName, action);
                documents.add(Document.parse(JsonHandler.writeAsString(offerLog)));
                sequence++;
            }
            mongoCollection.insertMany(documents, new InsertManyOptions().ordered(false));
        } catch (MongoException e) {
            throw new ContentAddressableStorageDatabaseException(
                String.format("Database Error while saving %s in OfferLog collection", fileNames),
                e
            );
        } catch (InvalidParseOperationException exc) {
            throw new ContentAddressableStorageServerException("Cannot parse storage log", exc);
        }
    }

    public List<OfferLog> getDescendingOfferLogsBy(String containerName, Long offset, int limit) {
        Bson searchFilter = offset != null
            ? and(eq(CONTAINER, containerName), lte(SEQUENCE, offset))
            : eq(CONTAINER, containerName);

        try (
            MongoCursor<OfferLog> cursor = mongoCollection
                .find(searchFilter)
                .sort(Sorts.orderBy(Sorts.descending(SEQUENCE)))
                .limit(limit)
                .map(this::transformDocumentToOfferLog)
                .cursor()
        ) {
            return IteratorUtils.toList(cursor);
        }
    }

    public List<OfferLog> getAscendingOfferLogsBy(String containerName, Long offset, int limit) {
        Bson searchFilter = offset != null
            ? and(eq(CONTAINER, containerName), gte(SEQUENCE, offset))
            : eq(CONTAINER, containerName);

        try (
            MongoCursor<OfferLog> cursor = mongoCollection
                .find(searchFilter)
                .sort(Sorts.orderBy(Sorts.ascending(SEQUENCE)))
                .limit(limit)
                .map(this::transformDocumentToOfferLog)
                .cursor()
        ) {
            return IteratorUtils.toList(cursor);
        }
    }

    public CloseableIterable<OfferLog> getExpiredOfferLogByContainer(long expirationValue, ChronoUnit expirationUnit) {
        LocalDateTime expirationDate = LocalDateUtil.now().minus(expirationValue, expirationUnit);

        return toCloseableIterable(
            mongoCollection
                .find(lte(TIME, LocalDateUtil.getFormattedDateForMongo(expirationDate)))
                .sort(Sorts.ascending(CONTAINER, SEQUENCE))
                .map(
                    d ->
                        new OfferLog(
                            ((Number) d.get(SEQUENCE)).longValue(),
                            LocalDateUtil.parseMongoFormattedDate(
                                LocalDateUtil.getFormattedDateForMongo(d.getString(TIME))
                            ),
                            d.getString(CONTAINER),
                            d.getString(FILENAME),
                            OfferLogAction.valueOf(d.getString(ACTION).toUpperCase())
                        )
                )
        );
    }

    private OfferLog transformDocumentToOfferLog(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, OfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private CloseableIterable<OfferLog> toCloseableIterable(MongoIterable<OfferLog> mongoIterable) {
        return new CloseableIterable<>() {
            @Override
            public Iterator<OfferLog> iterator() {
                return mongoIterable.iterator();
            }

            @Override
            public void forEach(Consumer<? super OfferLog> action) {
                mongoIterable.forEach(action);
            }

            @Override
            public Spliterator<OfferLog> spliterator() {
                return mongoIterable.spliterator();
            }

            @Override
            public void close() {
                mongoIterable.cursor().close();
            }
        };
    }
}
