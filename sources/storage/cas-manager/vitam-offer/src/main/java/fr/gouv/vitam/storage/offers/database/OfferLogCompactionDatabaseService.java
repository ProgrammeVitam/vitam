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
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;

public class OfferLogCompactionDatabaseService {

    private static final int COMPACTED_OFFER_LOG_BULK = 10;

    private final MongoCollection<Document> offerLogCompactionCollection;

    public OfferLogCompactionDatabaseService(MongoCollection<Document> offerLogCompactionCollection) {
        this.offerLogCompactionCollection = offerLogCompactionCollection;
    }

    public List<OfferLog> getDescendingOfferLogCompactionBy(String containerName, Long offset, int limit) {
        List<OfferLog> results = new ArrayList<>();

        int nextLimit = limit;
        Long nextOffset = offset;
        while (nextLimit > 0) {
            if (!loadNextOfferLogsDescending(containerName, nextOffset, nextLimit, results)) {
                // No more data
                break;
            }

            nextLimit = limit - results.size();
            nextOffset = results.get(results.size() - 1).getSequence() - 1;
        }

        return results;
    }

    private boolean loadNextOfferLogsDescending(String containerName, Long offset, int limit, List<OfferLog> results) {
        int lastSize = results.size();
        try (
            MongoCursor<OfferLog> offerLogCursor = offerLogCompactionCollection
                .aggregate(
                    Arrays.asList(
                        Aggregates.match(
                            offset != null
                                ? and(
                                    eq(CompactedOfferLog.CONTAINER, containerName),
                                    lte(CompactedOfferLog.SEQUENCE_START, offset)
                                )
                                : eq(CompactedOfferLog.CONTAINER, containerName)
                        ),
                        Aggregates.sort(Sorts.orderBy(Sorts.descending(CompactedOfferLog.SEQUENCE_START))),
                        Aggregates.limit(COMPACTED_OFFER_LOG_BULK),
                        Aggregates.project(
                            Projections.fields(
                                new Document("_id", 0),
                                new Document(
                                    CompactedOfferLog.LOGS,
                                    new Document("$reverseArray", "$" + CompactedOfferLog.LOGS)
                                )
                            )
                        ),
                        Aggregates.unwind("$" + CompactedOfferLog.LOGS),
                        Aggregates.match(
                            offset != null
                                ? lte(CompactedOfferLog.LOGS + "." + OfferLog.SEQUENCE, offset)
                                : new BsonDocument()
                        ),
                        Aggregates.limit(limit),
                        Aggregates.replaceWith("$" + CompactedOfferLog.LOGS)
                    )
                )
                .map(this::transformDocumentToOfferLog)
                .cursor()
        ) {
            offerLogCursor.forEachRemaining(results::add);
        }
        return results.size() != lastSize;
    }

    public List<OfferLog> getAscendingOfferLogCompactionBy(String containerName, Long offset, int limit) {
        List<OfferLog> results = new ArrayList<>();

        int nextLimit = limit;
        Long nextOffset = offset;
        while (nextLimit > 0) {
            if (!loadNextOfferLogsAscending(containerName, nextOffset, nextLimit, results)) {
                // No more data
                break;
            }

            nextLimit = limit - results.size();
            nextOffset = results.get(results.size() - 1).getSequence() + 1;
        }

        return results;
    }

    public boolean loadNextOfferLogsAscending(String containerName, Long offset, int limit, List<OfferLog> results) {
        int lastSize = results.size();
        try (
            MongoCursor<OfferLog> offerLogCursor = offerLogCompactionCollection
                .aggregate(
                    Arrays.asList(
                        Aggregates.match(
                            offset != null
                                ? and(
                                    eq(CompactedOfferLog.CONTAINER, containerName),
                                    gte(CompactedOfferLog.SEQUENCE_END, offset)
                                )
                                : eq(CompactedOfferLog.CONTAINER, containerName)
                        ),
                        Aggregates.sort(Sorts.orderBy(Sorts.ascending(CompactedOfferLog.SEQUENCE_END))),
                        Aggregates.limit(COMPACTED_OFFER_LOG_BULK),
                        Aggregates.unwind("$" + CompactedOfferLog.LOGS),
                        Aggregates.match(
                            offset != null
                                ? gte(CompactedOfferLog.LOGS + "." + OfferLog.SEQUENCE, offset)
                                : new BsonDocument()
                        ),
                        Aggregates.limit(limit),
                        Aggregates.replaceWith("$" + CompactedOfferLog.LOGS)
                    )
                )
                .map(this::transformDocumentToOfferLog)
                .cursor()
        ) {
            offerLogCursor.forEachRemaining(results::add);
        }
        return results.size() != lastSize;
    }

    private OfferLog transformDocumentToOfferLog(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, OfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
