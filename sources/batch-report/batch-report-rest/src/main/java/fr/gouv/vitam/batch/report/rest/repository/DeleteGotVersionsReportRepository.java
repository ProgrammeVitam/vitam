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
package fr.gouv.vitam.batch.report.rest.repository;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.OPC;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_OBJECTS;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_SIZE;
import static fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry.OBJECT_GROUP_GLOBAL;
import static fr.gouv.vitam.batch.report.model.entry.ObjectGroupToDeleteReportEntry.DELETED_VERSIONS;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.DETAIL_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.PROCESS_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.STATUS;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.TENANT;
import static fr.gouv.vitam.common.model.StatusCode.OK;

public class DeleteGotVersionsReportRepository {

    private final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DeleteGotVersionsReportRepository.class);

    public static final String DELETE_GOT_VERSIONS_REPORT = "DeleteGotVersionsReport";
    public static final String ID = "_id";
    private final MongoCollection<Document> collection;

    @VisibleForTesting
    public DeleteGotVersionsReportRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        this.collection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public DeleteGotVersionsReportRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, DELETE_GOT_VERSIONS_REPORT);
    }

    public void bulkAppendReport(List<DeleteGotVersionsReportEntry> reports) {
        List<WriteModel<Document>> deleteGotVersionsModel = reports
            .stream()
            .distinct()
            .map(DeleteGotVersionsReportRepository::modelToWriteDocument)
            .collect(Collectors.toList());

        collection.bulkWrite(deleteGotVersionsModel);
    }

    private static WriteModel<Document> modelToWriteDocument(DeleteGotVersionsReportEntry model) {
        return new UpdateOneModel<>(
            and(eq(PROCESS_ID, model.getProcessId()), eq(DETAIL_ID, model.getDetailId())),
            new Document("$set", Document.parse(JsonHandler.unprettyPrint(model))).append(
                "$setOnInsert",
                new Document(ID, GUIDFactory.newGUID().toString())
            ),
            new UpdateOptions().upsert(true)
        );
    }

    public MongoCursor<Document> findCollectionByProcessIdTenant(String processId, int tenantId) {
        return collection
            .aggregate(Arrays.asList(match(and(eq(PROCESS_ID, processId), eq(TENANT, tenantId)))))
            .allowDiskUse(true)
            .iterator();
    }

    public MongoCursor<Document> computeDeleteGotVersionsEntriesByProcessIdTenant(String processId, int tenantId) {
        return collection
            .aggregate(
                Arrays.asList(
                    match(and(eq(PROCESS_ID, processId), eq(TENANT, tenantId))),
                    Aggregates.project(Projections.fields(new Document(ID, 0), new Document(OBJECT_GROUP_GLOBAL, 1))),
                    Aggregates.unwind("$" + OBJECT_GROUP_GLOBAL),
                    match(and(eq(OBJECT_GROUP_GLOBAL + "." + STATUS, OK.name()))),
                    Aggregates.project(
                        Projections.fields(
                            new Document(ID, 0),
                            new Document(DELETED_VERSIONS, "$objectGroupGlobal.deletedVersions")
                        )
                    ),
                    Aggregates.unwind("$" + DELETED_VERSIONS),
                    Aggregates.project(
                        Projections.fields(
                            new Document(ID, 0),
                            new Document(OPC, "$deletedVersions.opc"),
                            new Document("Size", "$deletedVersions.Size")
                        )
                    ),
                    Aggregates.group(
                        "$" + OPC,
                        Accumulators.sum(TOTAL_SIZE, "$Size"),
                        Accumulators.sum(TOTAL_OBJECTS, 1)
                    )
                )
            )
            .allowDiskUse(true)
            .iterator();
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        DeleteResult deleteResult = collection.deleteMany(and(eq(PROCESS_ID, processId), eq(TENANT, tenantId)));
        LOGGER.info("Deleted document count: " + deleteResult.getDeletedCount() + " for process " + processId);
    }

    public ReportResults computeVitamResults(String processId, Integer tenantId) {
        ReportResults reportResult = new ReportResults();

        Iterator<Document> iterator = collection
            .aggregate(
                Arrays.asList(
                    match(and(eq(PROCESS_ID, processId), eq(TENANT, tenantId))),
                    Aggregates.project(Projections.fields(new Document(ID, 0), new Document(OBJECT_GROUP_GLOBAL, 1))),
                    Aggregates.unwind("$" + OBJECT_GROUP_GLOBAL),
                    Aggregates.project(
                        Projections.fields(
                            new Document(ID, 0),
                            new Document(STATUS, "$objectGroupGlobal.status"),
                            new Document(DELETED_VERSIONS, "$objectGroupGlobal.deletedVersions")
                        )
                    )
                )
            )
            .allowDiskUse(true)
            .iterator();

        iterator.forEachRemaining(result -> {
            String status = result.getString(STATUS);
            // Nbr of Ok results refers to the number of elements in "deletedVersions" when status is OK
            if (status.equals(OK.name())) {
                reportResult.incrementStatus(status, result.get(DELETED_VERSIONS, List.class).size());
            } else {
                reportResult.incrementStatus(status, 1);
            }
        });
        return reportResult;
    }
}
