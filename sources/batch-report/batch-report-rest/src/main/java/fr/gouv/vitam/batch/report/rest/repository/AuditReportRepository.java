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
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import fr.gouv.vitam.batch.report.model.AuditFullStatusCount;
import fr.gouv.vitam.batch.report.model.AuditObjectGroupModel;
import fr.gouv.vitam.batch.report.model.AuditStatsModel;
import fr.gouv.vitam.batch.report.model.AuditStatusCount;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.or;

/**
 * AuditReportRepository
 */
public class AuditReportRepository extends ReportCommonRepository {

    public static final String AUDIT_OBJECT_GROUP = "AuditObjectGroup";
    private final MongoCollection<Document> objectGroupReportCollection;

    @VisibleForTesting
    public AuditReportRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        this.objectGroupReportCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public AuditReportRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, AUDIT_OBJECT_GROUP);
    }

    /**
     * Appends report items to database
     *
     * @param reports report items
     */
    public void bulkAppendReport(List<AuditObjectGroupModel> reports) {
        Set<AuditObjectGroupModel> reportsWithoutDuplicate = new HashSet<>(reports);
        List<Document> auditObjectGroupDocument = reportsWithoutDuplicate
            .stream()
            .map(ReportCommonRepository::pojoToDocument)
            .collect(Collectors.toList());
        super.bulkAppendReport(auditObjectGroupDocument, objectGroupReportCollection);
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        super.deleteReportByIdAndTenant(processId, tenantId, objectGroupReportCollection);
    }

    public ReportResults computeVitamResults(String processId, Integer tenantId) {
        ReportResults reportResult = new ReportResults(0, 0, 0);

        Bson eqProcessId = eq(AuditObjectGroupModel.PROCESS_ID, processId);
        Bson eqTenant = eq(AuditObjectGroupModel.TENANT, tenantId);
        Bson groupOnSp = group("$_metadata.status", sum("result", 1));
        MongoCursor<Document> objectGroupsStatusCountResult = getListStats(
            Aggregates.match(and(eqProcessId, eqTenant)),
            groupOnSp
        );
        while (objectGroupsStatusCountResult.hasNext()) {
            Document result = objectGroupsStatusCountResult.next();
            String status = result.getString("_id");
            Integer count = result.getInteger("result");
            switch (status) {
                case "OK":
                    reportResult.setNbOk(count);
                    break;
                case "WARNING":
                    reportResult.setNbWarning(count);
                    break;
                case "KO":
                    reportResult.setNbKo(count);
                    break;
                default:
                    break;
            }
        }
        return reportResult;
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
        Bson eqProcessId = eq(AuditObjectGroupModel.PROCESS_ID, processId);
        Bson eqTenant = eq(AuditObjectGroupModel.TENANT, tenantId);
        Bson inStatus = or(
            in(AuditObjectGroupModel.METADATA + ".status", status),
            in(AuditObjectGroupModel.METADATA + ".objectVersions.status", status)
        );

        return objectGroupReportCollection
            .aggregate(
                Arrays.asList(
                    Aggregates.match(and(eqProcessId, eqTenant, inStatus)),
                    Aggregates.project(reportProjection())
                )
            )
            // Aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true)
            .iterator();
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

        return objectGroupReportCollection
            .aggregate(
                Arrays.asList(Aggregates.match(and(eqProcessId, eqTenant)), Aggregates.project(reportProjection()))
            )
            // Aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true)
            .iterator();
    }

    /**
     * Generate statistics of report
     *
     * @param processId processId
     * @param tenantId tenantId
     * @return statistics of report
     */
    public AuditStatsModel stats(String processId, int tenantId) {
        Bson eqProcessId = eq(AuditObjectGroupModel.PROCESS_ID, processId);
        Bson eqTenant = eq(AuditObjectGroupModel.TENANT, tenantId);

        Bson filterAgg = getMatchAggregation(and(eqProcessId, eqTenant));

        int nbObjectGroups = getNbObjectGroups(filterAgg);
        int nbObjects = getNbObjects(filterAgg);
        Set<String> opis = getObjectsOpi(filterAgg);

        AuditFullStatusCount globalResults = new AuditFullStatusCount(new AuditStatusCount(), new AuditStatusCount());
        getNbGotsStatus(filterAgg, globalResults);
        getNbObjectsStatus(filterAgg, globalResults);

        Map<String, AuditFullStatusCount> originatingAgencyResults = new HashMap<String, AuditFullStatusCount>();
        getNbGotsStatusBySp(filterAgg, originatingAgencyResults);
        getNbObjectsStatusBySp(filterAgg, originatingAgencyResults);

        return new AuditStatsModel(nbObjectGroups, nbObjects, opis, globalResults, originatingAgencyResults);
    }

    /**
     * Append number of objects groups
     *
     * @param matchAgg filter
     * @return number of objectGroups
     */
    private int getNbObjectGroups(Bson matchAgg) {
        Document nbObjectGroupsResult = getStats(matchAgg, getGroupAggregation("nbObjectGroups"));
        if (nbObjectGroupsResult != null) {
            return nbObjectGroupsResult.getInteger("result");
        } else {
            return 0;
        }
    }

    /**
     * Append number of objects
     *
     * @param matchAgg filter
     * @return number of objects
     */
    private int getNbObjects(Bson matchAgg) {
        Document nbObjectsResult = getStats(
            matchAgg,
            getUnwindAggregation("_metadata.objectVersions"),
            getGroupAggregation("nbObjects")
        );
        if (nbObjectsResult != null) {
            return nbObjectsResult.getInteger("result");
        } else {
            return 0;
        }
    }

    /**
     * Return the list of all opis treated
     *
     * @return opis
     */
    private Set<String> getObjectsOpi(Bson matchAgg) {
        Bson unwindVersions = unwind("$_metadata.objectVersions");
        Bson groupObjectOpi = group("$_metadata.objectVersions.opi");
        Bson projectObjectOpi = project(new Document("_id", "$_id"));

        Set<String> opis = new HashSet<String>();

        MongoCursor<Document> opisResult = getListStats(matchAgg, unwindVersions, groupObjectOpi, projectObjectOpi);

        while (opisResult.hasNext()) {
            Document opi = opisResult.next();
            opis.add(opi.getString("_id"));
        }
        return opis;
    }

    /**
     * Append number of objects group status to global stats results
     *
     * @param matchAgg filter
     * @param results results
     */
    private void getNbGotsStatus(Bson matchAgg, AuditFullStatusCount results) {
        Bson groupOnSp = group("$_metadata.status", sum("result", 1));
        MongoCursor<Document> objectGroupsStatusCountResult = getListStats(matchAgg, groupOnSp);
        while (objectGroupsStatusCountResult.hasNext()) {
            Document result = objectGroupsStatusCountResult.next();
            String status = result.getString("_id");
            Integer count = result.getInteger("result");
            results.getObjectGroupsCount().addOneStatus(status, count);
        }
    }

    /**
     * Append number of objects status to global stats results
     *
     * @param matchAgg filter
     * @param results results
     */
    private void getNbObjectsStatus(Bson matchAgg, AuditFullStatusCount results) {
        Bson unwindVersions = unwind("$_metadata.objectVersions");
        Bson groupOnSp = group("$_metadata.objectVersions.status", sum("result", 1));
        MongoCursor<Document> objectGroupsStatusCountResult = getListStats(matchAgg, unwindVersions, groupOnSp);
        while (objectGroupsStatusCountResult.hasNext()) {
            Document result = objectGroupsStatusCountResult.next();
            String status = result.getString("_id");
            Integer count = result.getInteger("result");
            results.getObjectsCount().addOneStatus(status, count);
        }
    }

    /**
     * Append number of object groups status by originating agency to global stats
     * results
     *
     * @param matchAgg filter
     * @param results results
     */
    private void getNbGotsStatusBySp(Bson matchAgg, Map<String, AuditFullStatusCount> results) {
        Map<String, Object> groupId = new HashMap<String, Object>();
        groupId.put("originatingAgency", "$_metadata.originatingAgency");
        groupId.put("status", "$_metadata.status");
        DBObject groupFields = new BasicDBObject(groupId);
        Bson groupOnSp = group(groupFields, sum("result", 1));

        MongoCursor<Document> objectGroupsStatusCountResult = getListStats(matchAgg, groupOnSp);
        while (objectGroupsStatusCountResult.hasNext()) {
            Document result = objectGroupsStatusCountResult.next();
            Document id = result.get("_id", Document.class);
            String originatingAgency = id.getString("originatingAgency");
            String status = id.getString("status");
            Integer count = result.getInteger("result");
            if (!results.containsKey(originatingAgency)) {
                results.put(
                    originatingAgency,
                    new AuditFullStatusCount(new AuditStatusCount(), new AuditStatusCount())
                );
            }
            results.get(originatingAgency).getObjectGroupsCount().addOneStatus(status, count);
        }
    }

    /**
     * Append number of objects status by originating agency to global stats results
     *
     * @param matchAgg filter
     * @param results results
     */
    private void getNbObjectsStatusBySp(Bson matchAgg, Map<String, AuditFullStatusCount> results) {
        Bson unwindVersions = unwind("$_metadata.objectVersions");
        Map<String, Object> groupId = new HashMap<String, Object>();
        groupId.put("originatingAgency", "$_metadata.originatingAgency");
        groupId.put("status", "$_metadata.objectVersions.status");
        DBObject groupFields = new BasicDBObject(groupId);
        Bson groupOnSp = group(groupFields, sum("result", 1));

        MongoCursor<Document> objectGroupsStatusCountResult = getListStats(matchAgg, unwindVersions, groupOnSp);
        while (objectGroupsStatusCountResult.hasNext()) {
            Document result = objectGroupsStatusCountResult.next();
            Document id = result.get("_id", Document.class);
            String originatingAgency = id.getString("originatingAgency");
            String status = id.getString("status");
            Integer count = result.getInteger("result");
            if (!results.containsKey(originatingAgency)) {
                results.put(
                    originatingAgency,
                    new AuditFullStatusCount(new AuditStatusCount(), new AuditStatusCount())
                );
            }
            results.get(originatingAgency).getObjectsCount().addOneStatus(status, count);
        }
    }

    private Bson reportProjection() {
        return Projections.fields(
            new Document("_id", 0),
            new Document("outcome", "$_metadata.outcome"),
            new Document("detailType", "$_metadata.detailType"),
            new Document("detailId", "$_metadata.detailId"),
            new Document("params.id", "$_metadata.id"),
            new Document("params.status", "$_metadata.status"),
            new Document("params.opi", "$_metadata.opi"),
            new Document("params.originatingAgency", "$_metadata.originatingAgency"),
            new Document("params.parentUnitIds", "$_metadata.parentUnitIds"),
            new Document("params.objectVersions", "$_metadata.objectVersions")
        );
    }

    private Bson getMatchAggregation(Bson filter) {
        return match(filter);
    }

    private Bson getUnwindAggregation(String field) {
        return unwind(String.format("$%s", field));
    }

    private Bson getGroupAggregation(String name) {
        return group(String.format("$%s", name), sum("result", 1));
    }

    private MongoCursor<Document> getListStats(Bson... aggregations) {
        return objectGroupReportCollection.aggregate(Arrays.asList(aggregations)).iterator();
    }

    private Document getStats(Bson... aggregations) {
        return objectGroupReportCollection.aggregate(Arrays.asList(aggregations)).first();
    }
}
