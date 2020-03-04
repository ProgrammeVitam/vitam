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
package fr.gouv.vitam.storage.offers.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.collection.CloseableIterable;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog.CONTAINER;
import static fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog.SEQUENCE_END;
import static fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog.SEQUENCE_START;

public class OfferLogCompactionDatabaseService {
    private final MongoCollection<Document> offerLogCompactionCollection;

    public OfferLogCompactionDatabaseService(MongoCollection<Document> offerLogCompactionCollection) {
        this.offerLogCompactionCollection = offerLogCompactionCollection;
    }

    public CloseableIterable<CompactedOfferLog> getDescendingOfferLogCompactionBy(String containerName, Long offset) {
        Bson searchFilterCompaction = offset != null
            ? and(eq(CONTAINER, containerName), lte(SEQUENCE_END, offset))
            : eq(CONTAINER, containerName);

        Bson sequenceSort = Sorts.orderBy(Sorts.descending(SEQUENCE_END));
        return toCloseableIterable(
            offerLogCompactionCollection.find(searchFilterCompaction)
                .sort(sequenceSort)
                .map(this::transformDocumentToOfferLogCompaction)
        );
    }

    public CloseableIterable<CompactedOfferLog> getAscendingOfferLogCompactionBy(String containerName, Long offset) {
        Bson searchFilterCompaction = offset != null
            ? and(eq(CONTAINER, containerName), gte(SEQUENCE_START, offset))
            : eq(CONTAINER, containerName);

        Bson sequenceSort = Sorts.orderBy(Sorts.ascending(SEQUENCE_START));
        return toCloseableIterable(
            offerLogCompactionCollection.find(searchFilterCompaction)
            .sort(sequenceSort)
            .map(this::transformDocumentToOfferLogCompaction)
        );
    }

    private CompactedOfferLog transformDocumentToOfferLogCompaction(Document document) {
        try {
            return JsonHandler.getFromString(BsonHelper.stringify(document), CompactedOfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private CloseableIterable<CompactedOfferLog> toCloseableIterable(MongoIterable<CompactedOfferLog> mongoIterable) {
        return new CloseableIterable<>() {
            @Override
            public Iterator<CompactedOfferLog> iterator() {
                return mongoIterable.iterator();
            }

            @Override
            public void forEach(Consumer<? super CompactedOfferLog> action) {
                mongoIterable.forEach(action);
            }

            @Override
            public Spliterator<CompactedOfferLog> spliterator() {
                return mongoIterable.spliterator();
            }

            @Override
            public void close() {
                mongoIterable.cursor()
                    .close();
            }
        };
    }
}
