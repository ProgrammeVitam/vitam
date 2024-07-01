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
import fr.gouv.vitam.batch.report.model.AuditObjectGroupModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditFullStatusCount;
import fr.gouv.vitam.batch.report.model.EvidenceAuditObjectModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditStatsModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditStatusCount;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

/**
 * EvidenceAuditReportRepository
 */
public class EvidenceAuditReportRepository extends ReportCommonRepository {

    public static final String EVIDENCE_AUDIT = "EvidenceAuditReport";
    private final MongoCollection<Document> evidenceAuditReportCollection;

    @VisibleForTesting
    public EvidenceAuditReportRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        this.evidenceAuditReportCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public EvidenceAuditReportRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, EVIDENCE_AUDIT);
    }

    /**
     * Appends report items to database
     *
     * @param reports report items
     */
    public void bulkAppendReport(List<EvidenceAuditObjectModel> reports) {
        Set<EvidenceAuditObjectModel> reportsWithoutDuplicate = new HashSet<>(reports);
        List<Document> evidenceAuditObjectGroupDocument = reportsWithoutDuplicate
            .stream()
            .map(ReportCommonRepository::pojoToDocument)
            .collect(Collectors.toList());
        super.bulkAppendReport(evidenceAuditObjectGroupDocument, evidenceAuditReportCollection);
    }

    /**
     * delete the report at finalization Step
     *
     * @param processId the process id
     * @param tenantId the tenantId id
     */
    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        super.deleteReportByIdAndTenant(processId, tenantId, evidenceAuditReportCollection);
    }

    /**
     * Compute the number of OK, WARNING, and KO
     *
     * @param processId the process id
     * @param tenantId the tenantId id
     */
    public ReportResults computeVitamResults(String processId, Integer tenantId) {
        ReportResults reportResult = new ReportResults();

        Bson eqProcessId = eq(EvidenceAuditObjectModel.PROCESS_ID, processId);
        Bson eqTenant = eq(EvidenceAuditObjectModel.TENANT, tenantId);

        EvidenceAuditFullStatusCount globalResults = new EvidenceAuditFullStatusCount(
            new EvidenceAuditStatusCount(),
            new EvidenceAuditStatusCount(),
            new EvidenceAuditStatusCount()
        );

        getNbObjects(match(and(eqProcessId, eqTenant)), globalResults);
        getMetadataTypeStats(globalResults, eqProcessId, eqTenant);

        reportResult.setNbOk(
            globalResults.getObjectGroupsCount().getNbOK() +
            globalResults.getUnitsCount().getNbOK() +
            globalResults.getObjectsCount().getNbOK()
        );
        reportResult.setNbKo(
            globalResults.getObjectGroupsCount().getNbKO() +
            globalResults.getUnitsCount().getNbKO() +
            globalResults.getObjectsCount().getNbKO()
        );
        reportResult.setNbWarning(
            globalResults.getObjectGroupsCount().getNbWARNING() +
            globalResults.getUnitsCount().getNbWARNING() +
            globalResults.getObjectsCount().getNbWARNING()
        );

        return reportResult;
    }

    /**
     * Retrieve all report mongo document for a report
     *
     * @param processId processId
     * @param tenantId tenantId
     * @return mongo cursor of report documents
     */
    public MongoCursor<Document> findCollectionByProcessIdTenant(String processId, int tenantId) {
        Bson eqProcessId = eq(AuditObjectGroupModel.PROCESS_ID, processId);
        Bson eqTenant = eq(AuditObjectGroupModel.TENANT, tenantId);

        return evidenceAuditReportCollection
            .aggregate(
                Arrays.asList(
                    Aggregates.match(and(eqProcessId, eqTenant)),
                    Aggregates.project(evidenceReportProjection())
                )
            )
            .allowDiskUse(true)
            .iterator();
    }

    /**
     * Retrieve report mongo document for a report filtered by defined statuses
     *
     * @param processId processId
     * @param tenantId tenantId
     * @param status statuses
     * @return mongo cursor of report documents
     */
    public MongoCursor<Document> findCollectionByProcessIdTenantAndStatus(
        String processId,
        int tenantId,
        String... status
    ) {
        Bson eqProcessId = eq(EvidenceAuditObjectModel.PROCESS_ID, processId);
        Bson eqTenant = eq(EvidenceAuditObjectModel.TENANT, tenantId);
        Bson inStatus = in(EvidenceAuditObjectModel.METADATA + ".status", status);

        return evidenceAuditReportCollection
            .aggregate(
                Arrays.asList(
                    Aggregates.match(and(eqProcessId, eqTenant, inStatus)),
                    Aggregates.project(evidenceReportProjection())
                )
            )
            // Aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true)
            .iterator();
    }

    /**
     * Generate statistics of evidence audit report
     *
     * @param processId processId
     * @param tenantId tenantId
     * @return statistics of evidence audit report
     */
    public EvidenceAuditStatsModel stats(String processId, int tenantId) {
        Bson eqProcessId = eq(EvidenceAuditObjectModel.PROCESS_ID, processId);
        Bson eqTenant = eq(EvidenceAuditObjectModel.TENANT, tenantId);

        EvidenceAuditFullStatusCount globalResults = new EvidenceAuditFullStatusCount(
            new EvidenceAuditStatusCount(),
            new EvidenceAuditStatusCount(),
            new EvidenceAuditStatusCount()
        );

        getNbObjects(match(and(eqProcessId, eqTenant)), globalResults);
        getMetadataTypeStats(globalResults, eqProcessId, eqTenant);
        return new EvidenceAuditStatsModel(
            globalResults.getObjectGroupsCount().getTotal(),
            globalResults.getUnitsCount().getTotal(),
            globalResults.getObjectsCount().getTotal(),
            globalResults
        );
    }

    /**
     * Append number of objects status to global stats results
     *
     * @param matchAgg filter
     * @param results results
     */
    private void getNbObjects(Bson matchAgg, EvidenceAuditFullStatusCount results) {
        Bson group = group("$_metadata.objectsReports.status", sum("result", 1));
        MongoCursor<Document> objectGroupsStatusCountResult = getListStats(
            matchAgg,
            Aggregates.unwind("$_metadata.objectsReports"),
            group
        );
        while (objectGroupsStatusCountResult.hasNext()) {
            Document result = objectGroupsStatusCountResult.next();
            String status = result.getString("_id");
            Integer count = result.getInteger("result");
            results.getObjectsCount().addOneStatus(status, count);
        }
    }

    /**
     * Archive Unit and GOT statistics
     *
     * @param globalResults globalResults
     * @param eqProcessId eqProcessId
     * @param eqTenant eqTenant
     * @return AU and GOT statistics
     */
    private void getMetadataTypeStats(EvidenceAuditFullStatusCount globalResults, Bson eqProcessId, Bson eqTenant) {
        MongoCursor<Document> findIterable = getListStats(
            match(and(eqProcessId, eqTenant)),
            group(
                new Document(
                    "_id",
                    new Document("restType", "$_metadata.objectType").append("resStatus", "$_metadata.status")
                ),
                sum("result", 1)
            )
        );
        while (findIterable.hasNext()) {
            Document current = findIterable.next();
            Document objectType = ((Document) current.get("_id"));
            String dataType = ((Document) objectType.get("_id")).get("restType").toString();
            String dataStatus = ((Document) objectType.get("_id")).getString("resStatus");
            Integer count = current.getInteger("result");
            switch (dataType) {
                case "OBJECTGROUP":
                    globalResults.getObjectGroupsCount().addOneStatus(dataStatus, count);
                    break;
                case "UNIT":
                    globalResults.getUnitsCount().addOneStatus(dataStatus, count);
                    break;
                default:
                    break;
            }
        }
    }

    private Bson evidenceReportProjection() {
        return Projections.fields(
            new Document("_id", 0),
            new Document("identifier", "$_metadata.identifier"),
            new Document("status", "$_metadata.status"),
            new Document("objectType", "$_metadata.objectType"),
            new Document("message", "$_metadata.message"),
            new Document("strategyId", "$_metadata.strategyId"),
            new Document("offersHashes", "$_metadata.offersHashes")
        );
    }

    private MongoCursor<Document> getListStats(Bson... aggregations) {
        return evidenceAuditReportCollection.aggregate(Arrays.asList(aggregations)).allowDiskUse(true).iterator();
    }
}
