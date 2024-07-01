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
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry.MESSAGE;
import static fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry.PROCESS_ID;
import static fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry.QUERY;
import static fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry.RESULT_KEY;
import static fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry.STATUS;
import static fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry.STATUS_ID;
import static fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry.TENANT_ID;
import static fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry.UNIT_ID;
import static fr.gouv.vitam.batch.report.model.entry.ReportEntry.DETAIL_ID;
import static fr.gouv.vitam.batch.report.model.entry.ReportEntry.DETAIL_TYPE;
import static fr.gouv.vitam.batch.report.model.entry.ReportEntry.OUTCOME;

public class BulkUpdateUnitMetadataReportRepository extends ReportCommonRepository {

    private static final String COLLECTION_NAME = "BulkUpdateUnitMetadataReport";

    private final MongoCollection<Document> collection;

    private static final Bson PROJECTION = Aggregates.project(
        Projections.fields(
            new Document(RESULT_KEY, "$resultKey"),
            new Document(PROCESS_ID, "$processId"),
            new Document(TENANT_ID, "$_tenant"),
            new Document(STATUS, "$status"),
            new Document(QUERY, "$query"),
            new Document(UNIT_ID, "$unitId"),
            new Document(MESSAGE, "$message"),
            new Document(OUTCOME, "$outcome"),
            new Document(DETAIL_TYPE, "$detailType"),
            new Document(DETAIL_ID, "$id")
        )
    );

    @VisibleForTesting
    public BulkUpdateUnitMetadataReportRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        this.collection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public BulkUpdateUnitMetadataReportRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, COLLECTION_NAME);
    }

    private static WriteModel<Document> modelToWriteDocument(BulkUpdateUnitMetadataReportEntry model) {
        try {
            return new UpdateOneModel<>(
                and(
                    eq(PROCESS_ID, model.getProcessId()),
                    eq(TENANT_ID, model.getTenantId()),
                    eq(DETAIL_ID, model.getDetailId())
                ),
                new Document("$set", Document.parse(JsonHandler.writeAsString(model))).append(
                    "$setOnInsert",
                    new Document("_id", GUIDFactory.newGUID().toString())
                ),
                new UpdateOptions().upsert(true)
            );
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public void bulkAppendReport(List<BulkUpdateUnitMetadataReportEntry> reports) {
        List<WriteModel<Document>> preservationDocument = reports
            .stream()
            .distinct()
            .map(BulkUpdateUnitMetadataReportRepository::modelToWriteDocument)
            .collect(Collectors.toList());

        collection.bulkWrite(preservationDocument);
    }

    public MongoCursor<Document> findCollectionByProcessIdTenant(String processId, int tenantId) {
        return collection
            .aggregate(
                Arrays.asList(
                    match(and(eq(PROCESS_ID, processId), eq(TENANT_ID, tenantId))),
                    sort(Sorts.descending(STATUS_ID)),
                    PROJECTION
                )
            )
            .allowDiskUse(true)
            .iterator();
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        super.deleteReportByIdAndTenant(processId, tenantId, collection);
    }
}
