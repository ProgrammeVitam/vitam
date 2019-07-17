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

package fr.gouv.vitam.batch.report.rest.repository;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import org.bson.Document;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;

public class InvalidUnitsRepository extends ReportCommonRepository {

    private static final String INVALID_UNITS = "InvalidUnits";
    private static final String PROCESS_ID = "processId";
    private static final String ID = "_id";

    private final MongoCollection<Document> collection;

    @VisibleForTesting
    InvalidUnitsRepository(SimpleMongoDBAccess mongoDbAccess, String collectionName) {
        this.collection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public InvalidUnitsRepository(SimpleMongoDBAccess mongoDbAccess) {
        this(mongoDbAccess, INVALID_UNITS);
    }

    public void bulkAppendUnits(List<String> unitsId, String processId) {
        List<WriteModel<Document>> updates = unitsId.stream()
            .map(id -> getWriteModel(id, processId))
            .collect(Collectors.toList());

        collection.bulkWrite(updates);
    }

    public void deleteUnitsAndProgeny(String processId) {
        collection.deleteMany(eq(PROCESS_ID, processId));
    }

    public MongoCursor<Document> findUnitsByProcessId(String processId) {
        return collection.aggregate(Collections.singletonList(match(eq(PROCESS_ID, processId))))
            .allowDiskUse(true)
            .iterator();
    }

    private WriteModel<Document> getWriteModel(String unitId, String operationId) {
        Document doc = new Document(ID, unitId);
        doc.append(PROCESS_ID, operationId);
        return new UpdateOneModel<>(
            doc,
            new Document("$set", doc),
            new UpdateOptions().upsert(true)
        );
    }

}
