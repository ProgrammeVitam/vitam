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

package fr.gouv.vitam.common.database.server;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OplogReader {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OplogReader.class);
    public static final String OPERATION_TYPE = "op";
    public static final String COLLECTION_NAME = "ns";
    public static final String OPERATION_TIME = "ts";
    public static final String FIELD_TIME = "Time";
    public static final String FIELD_INCREMENT = "Inc";
    public static final String FIELD_O_2 = "o2";
    public static final String FIELD_O = "o";
    public static final String FIELD_ID = "_id";
    public static final String INSERT_OPERATION = "i";
    public static final String UPDATE_OPERATION = "u";
    public static final String DELETE_OPERATION = "d";
    public static final String PATH = "path";

    private static final String LOCALDB = "local";
    private static final String OPLOG = "oplog.rs";
    public static final String NATURAL = "$natural";
    private final MongoClient mongoClient;
    private final Integer dataMaxSize;

    public OplogReader(MongoClient mongoClient, Integer dataMaxSize) {
        this.mongoClient = mongoClient;
        this.dataMaxSize = dataMaxSize;
    }

    public Map<String, Document> readDocumentsFromOplogByShardAndCollections(
        List<String> collectionsToReadOplog,
        BsonTimestamp maxTimeStamp
    ) {
        Map<String, Document> recentlyTouchedDocuments = readOplog(collectionsToReadOplog, maxTimeStamp);
        mongoClient.close();
        return recentlyTouchedDocuments;
    }

    private Map<String, Document> readOplog(List<String> collectionsToReadOplog, BsonTimestamp maxTimeStamp) {
        LOGGER.info("Start reading Oplog");
        Map<String, Document> touchedDocumentsByRecentTime = new HashMap<>();
        List<Document> opLogList = new ArrayList<>();
        List<Bson> bsonsFilters = new ArrayList<>();
        bsonsFilters.add(
            Filters.in(OPERATION_TYPE, Arrays.asList(INSERT_OPERATION, UPDATE_OPERATION, DELETE_OPERATION))
        );
        bsonsFilters.add(Filters.in(COLLECTION_NAME, collectionsToReadOplog));
        if (maxTimeStamp != null) {
            bsonsFilters.add(Filters.gt(OPERATION_TIME, maxTimeStamp));
        }
        Document sort = new Document(NATURAL, 1);
        Bson filter = Filters.and(bsonsFilters);
        MongoCollection oplogCollection = mongoClient.getDatabase(LOCALDB).getCollection(OPLOG);
        MongoCursor<Document> cursor = oplogCollection
            .find(filter)
            .sort(sort)
            .limit(dataMaxSize) // use limit(0) to have no limit
            .iterator();
        while (cursor.hasNext()) {
            populateOplogList(opLogList, cursor.next());
        }

        touchedDocumentsByRecentTime.putAll(
            opLogList.stream().collect(Collectors.toMap(OplogReader::extractFieldId, elmt -> elmt))
        );
        return touchedDocumentsByRecentTime;
    }

    private static void populateOplogList(List<Document> opLogList, Document document) {
        Optional<Document> isAlreadyScannedDoc = opLogList
            .stream()
            .filter(elmt -> extractFieldId(elmt).equals(extractFieldId(document)))
            .findFirst();
        if (isAlreadyScannedDoc.isPresent()) {
            if ((extractFieldTimeStamp(document)).compareTo((extractFieldTimeStamp(isAlreadyScannedDoc.get()))) > 0) {
                // REPLACE WITH RECENT
                opLogList.remove(isAlreadyScannedDoc.get());
                opLogList.add(document);
            }
        } else {
            opLogList.add(document);
        }
    }

    private static String extractFieldId(Document elmt) {
        if (elmt.get(OPERATION_TYPE).equals(UPDATE_OPERATION)) {
            return ((Document) elmt.get(FIELD_O_2)).get(FIELD_ID).toString();
        }
        return ((Document) elmt.get(FIELD_O)).get(FIELD_ID).toString();
    }

    private static BsonTimestamp extractFieldTimeStamp(Document elmt) {
        return ((BsonTimestamp) elmt.get(OPERATION_TIME));
    }
}
