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
import com.mongodb.client.model.Projections;
import fr.gouv.vitam.batch.report.model.AuditObjectGroupModel;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.TraceabilityObjectModel;
import fr.gouv.vitam.batch.report.model.TraceabilityStatsModel;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.fields;
import static fr.gouv.vitam.batch.report.model.TraceabilityObjectModel.METADATA;
import static fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry.OPERATION_TYPE;
import static fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry.STATUS;

public class TraceabilityReportRepository extends ReportCommonRepository {

    public static final String TRACEABILITY_REPORT = "TraceabilityReport";

    private final MongoCollection<Document> traceabilityReportCollection;
    private final String RESULT = "result";
    private final String OPERATION_PROJECTION = "$%s.%s";

    public TraceabilityReportRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, TRACEABILITY_REPORT);
    }

    @VisibleForTesting
    TraceabilityReportRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        this.traceabilityReportCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public MongoCursor<Document> findCollection(String processId, int tenantId) {
        Bson eqProcessId = eq(TraceabilityObjectModel.PROCESS_ID, processId);
        Bson eqTenant = eq(TraceabilityObjectModel.TENANT, tenantId);
        Bson aggregation = match(and(eqProcessId, eqTenant));

        return traceabilityReportCollection
            .aggregate(Collections.singletonList(aggregation))
            // aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true)
            .iterator();
    }

    public TraceabilityStatsModel stats(String processId, Integer tenantId) {
        TraceabilityStatsModel traceabilityStatsModel = new TraceabilityStatsModel(0, 0, 0, 0);

        Bson eqProcessId = eq(AuditObjectGroupModel.PROCESS_ID, processId);
        Bson eqTenant = eq(AuditObjectGroupModel.TENANT, tenantId);

        Bson aggregation = match(and(eqProcessId, eqTenant));
        Bson groupBy = group(String.format(OPERATION_PROJECTION, METADATA, OPERATION_TYPE), sum(RESULT, 1));
        Bson projections = project(fields(Projections.include(METADATA)));

        Iterator<Document> iterator = traceabilityReportCollection
            .aggregate(Arrays.asList(aggregation, projections, groupBy))
            .allowDiskUse(true)
            .iterator();

        iterator.forEachRemaining(result -> {
            String operationType = result.getString(VitamDocument.ID);
            Integer count = result.getInteger(RESULT);
            traceabilityStatsModel.addOneModel(operationType, count);
        });
        return traceabilityStatsModel;
    }

    public ReportResults computeVitamResults(String processId, Integer tenantId) {
        ReportResults reportResult = new ReportResults();
        Bson eqProcessId = eq(AuditObjectGroupModel.PROCESS_ID, processId);
        Bson eqTenant = eq(AuditObjectGroupModel.TENANT, tenantId);

        Bson aggregation = match(and(eqProcessId, eqTenant));
        Bson groupBy = group(String.format(OPERATION_PROJECTION, METADATA, STATUS), sum(RESULT, 1));
        Bson projections = project(fields(Projections.include(METADATA)));

        Iterator<Document> iterator = traceabilityReportCollection
            .aggregate(Arrays.asList(aggregation, projections, groupBy))
            .allowDiskUse(true)
            .iterator();

        iterator.forEachRemaining(result -> {
            String status = result.getString(VitamDocument.ID);
            Integer count = result.getInteger(RESULT);
            reportResult.addOneStatus(status, count);
        });

        return reportResult;
    }

    public void bulkAppendReport(List<TraceabilityObjectModel> reports) {
        Set<TraceabilityObjectModel> reportsWithoutDuplicate = new HashSet<>(reports);
        List<Document> traceabilityObjectDocument = reportsWithoutDuplicate
            .stream()
            .map(ReportCommonRepository::pojoToDocument)
            .collect(Collectors.toList());
        super.bulkAppendReport(traceabilityObjectDocument, traceabilityReportCollection);
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        super.deleteReportByIdAndTenant(processId, tenantId, traceabilityReportCollection);
    }
}
