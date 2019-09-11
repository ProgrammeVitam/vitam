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
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import fr.gouv.vitam.batch.report.model.AuditObjectGroupModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditFullStatusCount;
import fr.gouv.vitam.batch.report.model.EvidenceAuditObjectModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditStatsModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditStatusCount;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.model.MetadataType;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
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
        List<Document> evidenceAuditObjectGroupDocument = reportsWithoutDuplicate.stream()
            .map(ReportCommonRepository::pojoToDocument).collect(Collectors.toList());
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
        ReportResults reportResult = new ReportResults(0, 0, 0, 0);

        Bson eqProcessId = eq(EvidenceAuditObjectModel.PROCESS_ID, processId);
        Bson eqTenant = eq(EvidenceAuditObjectModel.TENANT, tenantId);
        Bson groupOnSp = group("$_metadata.status", sum("result", 1));
        MongoCursor<Document> objectGroupsStatusCountResult = getListStats(Aggregates.match(and(eqProcessId, eqTenant)),
            groupOnSp);
        while (objectGroupsStatusCountResult.hasNext()) {
            Document result = objectGroupsStatusCountResult.next();
            String status = result.getString("_id");
            Integer count = result.getInteger("result");
            switch (status) {
                case "OK":
                    reportResult.setNbOk(count);
                    break;
                case "WARN":
                    reportResult.setNbWarning(count);
                    break;
                case "KO":
                    reportResult.setNbKo(count);
                    break;
                default:
                    break;
            }
        }
        reportResult.setTotal(reportResult.getNbKo() + reportResult.getNbOk() + reportResult.getNbWarning());
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
            .aggregate(Arrays.asList(Aggregates.match(and(eqProcessId, eqTenant)),
                Aggregates.project(evidenceReportProjection())))
            .allowDiskUse(true).iterator();
    }

    /**
     * Retrieve report mongo document for a report filtered by defined statuses
     *
     * @param processId processId
     * @param tenantId tenantId
     * @param status statuses
     * @return mongo cursor of report documents
     */
    public MongoCursor<Document> findCollectionByProcessIdTenantAndStatus(String processId, int tenantId,
        String... status) {
        Bson eqProcessId = eq(EvidenceAuditObjectModel.PROCESS_ID, processId);
        Bson eqTenant = eq(EvidenceAuditObjectModel.TENANT, tenantId);
        Bson inStatus = in(EvidenceAuditObjectModel.METADATA + ".status", status);

        return evidenceAuditReportCollection
            .aggregate(Arrays.asList(Aggregates.match(and(eqProcessId, eqTenant, inStatus)),
                Aggregates.project(evidenceReportProjection())))
            // Aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true).iterator();
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

        EvidenceAuditFullStatusCount globalResults =
            new EvidenceAuditFullStatusCount(new EvidenceAuditStatusCount(), new EvidenceAuditStatusCount(),
                new EvidenceAuditStatusCount());

        int nbObjects = getObjectsStats(globalResults, eqProcessId, eqTenant, MetadataType.OBJECTGROUP.name());
        int nbObjectsGroups =
            getMetadataTypeStats(globalResults, eqProcessId, eqTenant, MetadataType.OBJECTGROUP.name());
        int nbUnits = getMetadataTypeStats(globalResults, eqProcessId, eqTenant, MetadataType.UNIT.name());

        return
            new EvidenceAuditStatsModel(nbObjectsGroups, nbUnits, nbObjects, globalResults);

    }

    /**
     * Object statistics
     *
     * @param globalResults globalResults
     * @param eqProcessId eqProcessId
     * @param eqTenant eqTenant
     * @param type type
     * @return the number of objects
     */
    private Integer getObjectsStats(EvidenceAuditFullStatusCount globalResults, Bson eqProcessId, Bson eqTenant,
        String type) {
        FindIterable<Document> findIterable =
            evidenceAuditReportCollection.find(and(eq("_metadata.objectType", type), eqProcessId, eqTenant, Filters
                .size("_metadata.objectsReports", 1)));
        MongoCursor<Document> result = findIterable.iterator();
        int cpt = 1;
        while (result.hasNext()) {
            Document current = result.next();
            String status = ((Document) current.get("_metadata")).getString("status");
            globalResults.getObjectsCount().addOneStatus(status, cpt++);

        }
        return cpt - 1;
    }

    /**
     * Archive Unit ang GOT statistics
     *
     * @param globalResults globalResults
     * @param eqProcessId eqProcessId
     * @param eqTenant eqTenant
     * @param type type
     * @return AU and GOT statistics
     */
    private Integer getMetadataTypeStats(EvidenceAuditFullStatusCount globalResults, Bson eqProcessId, Bson eqTenant,
        String type) {
        FindIterable<Document> findIterable =
            evidenceAuditReportCollection.find(and(eq("_metadata.objectType", type), eqProcessId, eqTenant));
        MongoCursor<Document> result = findIterable.iterator();
        int cpt = 1;
        while (result.hasNext()) {
            Document current = result.next();
            String status = ((Document) current.get("_metadata")).getString("status");
            switch (type) {
                case "OBJECTGROUP":
                    globalResults.getObjectGroupsCount().addOneStatus(status, cpt++);
                    break;
                case "UNIT":
                    globalResults.getUnitsCount().addOneStatus(status, cpt++);
                    break;
                default:
                    break;
            }
        }
        return cpt - 1;
    }

    private Bson evidenceReportProjection() {
        return Projections.fields(new Document("_id", 0), new Document("identifier", "$_metadata.identifier"),
            new Document("status", "$_metadata.status"), new Document("objectType", "$_metadata.objectType"),
            new Document("message", "$_metadata.message"),
            new Document("strategyId", "$_metadata.strategyId"),
            new Document("offersHashes", "$_metadata.offersHashes"));
    }

    private MongoCursor<Document> getListStats(Bson... aggregations) {
        return evidenceAuditReportCollection.aggregate(Arrays.asList(aggregations)).iterator();
    }

}
