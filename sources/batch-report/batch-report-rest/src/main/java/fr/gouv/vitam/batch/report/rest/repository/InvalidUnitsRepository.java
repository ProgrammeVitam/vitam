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
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class InvalidUnitsRepository extends ReportCommonRepository {

    public static final String INVALID_UNITS_COLLECTION_NAME = "InvalidUnits";
    public static final String PROCESS_ID = "processId";
    public static final String UNIT_ID = "unitId";
    public static final String TENANT_ID = "_tenant";

    private final MongoCollection<Document> collection;

    @VisibleForTesting
    InvalidUnitsRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        this.collection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public InvalidUnitsRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, INVALID_UNITS_COLLECTION_NAME);
    }

    public void bulkAppendUnits(List<String> unitsId, String processId) {
        List<WriteModel<Document>> updates = unitsId.stream()
            .map(id -> getWriteModel(id, processId))
            .collect(Collectors.toList());

        collection.bulkWrite(updates);
    }

    public void deleteUnitsAndProgeny(String processId) {
        collection.deleteMany(and(
            eq(PROCESS_ID, processId),
            eq(TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
        ));
    }

    public MongoCursor<Document> findUnitsByProcessId(String processId) {
        return collection.aggregate(Arrays.asList(
            match(and(
                eq(PROCESS_ID, processId),
                eq(TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            )),
            project(Projections.fields(
                new Document("_id", 0),
                new Document(UNIT_ID, "unitId")))
            ))
            .allowDiskUse(true)
            .iterator();
    }

    private WriteModel<Document> getWriteModel(String unitId, String operationId) {
        Document doc = new Document(UNIT_ID, unitId)
            .append(PROCESS_ID, operationId)
            .append(TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId());
        return new UpdateOneModel<>(
            doc,
            new Document("$set", doc)
                .append("$setOnInsert", new Document("_id", GUIDFactory.newGUID().toString())),
            new UpdateOptions().upsert(true)
        );
    }

}
