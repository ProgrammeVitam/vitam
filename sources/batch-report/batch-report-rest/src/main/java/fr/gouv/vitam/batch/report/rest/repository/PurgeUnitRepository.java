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
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.batch.report.model.PurgeObjectGroupModel;
import fr.gouv.vitam.batch.report.model.PurgeUnitModel;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import org.bson.Document;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.OPI;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.ORIGINATING_AGENCY;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_UNITS;

/**
 * ReportRepository
 */
public class PurgeUnitRepository extends ReportCommonRepository {

    public static final String PURGE_UNIT = "PurgeUnit";
    public static final String METADATA_OBJECT_GROUP_ID = "_metadata.objectGroupId";
    private final MongoCollection<Document> purgeUnitReportCollection;

    @VisibleForTesting
    public PurgeUnitRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        this.purgeUnitReportCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public PurgeUnitRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, PURGE_UNIT);
    }

    public void bulkAppendReport(List<PurgeUnitModel> reports) {
        Set<PurgeUnitModel> reportsWithoutDuplicate = new HashSet<>(reports);
        List<Document> eliminationUnitDocument =
            reportsWithoutDuplicate.stream()
                .map(ReportCommonRepository::pojoToDocument)
                .collect(Collectors.toList());
        super.bulkAppendReport(eliminationUnitDocument, purgeUnitReportCollection);
    }

    public MongoCursor<Document> findCollectionByProcessIdTenant(String processId, int tenantId) {

        return purgeUnitReportCollection.aggregate(
                Arrays.asList(
                    Aggregates.match(and(
                        eq(PurgeUnitModel.PROCESS_ID, processId),
                        eq(PurgeUnitModel.TENANT, tenantId)
                    )),
                    Aggregates.project(Projections.fields(
                            new Document("_id", 0),
                            new Document("id", "$_metadata.id"),
                            new Document("distribGroup", null),
                            new Document("params.id", "$_metadata.id"),
                            new Document("params.type", new Document("$literal", "Unit")),
                            new Document("params.status", "$_metadata.status"),
                            new Document("params.opi", "$_metadata.opi"),
                            new Document("params.originatingAgency", "$_metadata.originatingAgency"),
                            new Document("params.objectGroupId", "$_metadata.objectGroupId")
                        )
                    ))
            )
            // Aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true)
            .iterator();
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        super.deleteReportByIdAndTenant(processId, tenantId, purgeUnitReportCollection);
    }

    /**
     * Aggregation on distinct objectGroupId, with status DELETED
     *
     * @param processId processId
     * @param tenantId tenantId
     * @return cursor over distinct objectGroupId
     */
    public MongoCursor<String> distinctObjectGroupOfDeletedUnits(String processId, int tenantId) {
        DistinctIterable<String> distinctObjectGroup =
            purgeUnitReportCollection
                .distinct(METADATA_OBJECT_GROUP_ID, and(eq(PurgeUnitModel.PROCESS_ID, processId),
                    eq("_metadata.status", "DELETED"), eq(PurgeUnitModel.TENANT, tenantId)), String.class);
        return distinctObjectGroup.iterator();
    }

    /**
     * Compute Own AccessionRegisterDetails
     */
    public MongoCursor<Document> computeOwnAccessionRegisterDetails(String processId, int tenantId) {
        return purgeUnitReportCollection.aggregate(
                Arrays.asList(
                    // Filter
                    Aggregates.match(and(
                        eq(PurgeObjectGroupModel.PROCESS_ID, processId),
                        eq(PurgeObjectGroupModel.TENANT, tenantId),
                        eq("_metadata.status", "DELETED")
                    )),
                    // Group By
                    Aggregates.group("$_metadata." + OPI,
                        Accumulators.first(ORIGINATING_AGENCY, "$_metadata." + ORIGINATING_AGENCY),
                        Accumulators.sum(TOTAL_UNITS, 1)
                    ),
                    // Projection
                    Aggregates.project(Projections.fields(
                            new Document("_id", 0),
                            new Document(OPI, "$_id"),
                            new Document(ORIGINATING_AGENCY, 1),
                            new Document(TOTAL_UNITS, 1)
                        )
                    ),
                    // Sort
                    Aggregates.sort(Sorts.descending("opi"))
                )
            )
            // Aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true)
            .iterator();
    }
}
