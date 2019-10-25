/*
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
 */
package fr.gouv.vitam.batch.report.rest.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.batch.report.model.PurgeObjectGroupModel;
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
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_OBJECTS;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_OBJECT_GROUPS;
import static fr.gouv.vitam.batch.report.model.PurgeAccessionRegisterModel.TOTAL_SIZE;

/**
 * PurgeObjectGroup
 */
public class PurgeObjectGroupRepository extends ReportCommonRepository {

    public static final String PURGE_OBJECT_GROUP = "PurgeObjectGroup";
    public static final String OPI_GOT = "opi_got";

    private final MongoCollection<Document> objectGroupReportCollection;

    @VisibleForTesting
    public PurgeObjectGroupRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        objectGroupReportCollection =
            mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public PurgeObjectGroupRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, PURGE_OBJECT_GROUP);
    }

    public void bulkAppendReport(List<PurgeObjectGroupModel> reports) {
        Set<PurgeObjectGroupModel> reportsWithoutDuplicate = new HashSet<>(reports);
        List<Document> purgeObjectGroupDocument =
            reportsWithoutDuplicate.stream()
                .map(ReportCommonRepository::pojoToDocument)
                .collect(Collectors.toList());
        super.bulkAppendReport(purgeObjectGroupDocument, objectGroupReportCollection);
    }

    public MongoCursor<Document> findCollectionByProcessIdTenant(String processId, int tenantId) {

        return objectGroupReportCollection.aggregate(
            Arrays.asList(
                Aggregates.match(and(
                    eq(PurgeObjectGroupModel.PROCESS_ID, processId),
                    eq(PurgeObjectGroupModel.TENANT, tenantId)
                )),
                Aggregates.project(Projections.fields(
                    new Document("_id", 0),
                    new Document("id", "$_metadata.id"),
                    new Document("distribGroup", null),
                    new Document("params.id", "$_metadata.id"),
                    new Document("params.type", new Document("$literal", "ObjectGroup")),
                    new Document("params.status", "$_metadata.status"),
                    new Document("params.opi", "$_metadata.opi"),
                    new Document("params.originatingAgency", "$_metadata.originatingAgency"),
                    new Document("params.deletedParentUnitIds", "$_metadata.deletedParentUnitIds"),
                    new Document("params.objectIds", "$_metadata.objectIds")
                    )
                ))
        )
            // Aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true)
            .iterator();
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        super.deleteReportByIdAndTenant(processId, tenantId, objectGroupReportCollection);
    }

    /**
     * Compute Own AccessionRegisterDetails
     */
    public MongoCursor<Document> computeOwnAccessionRegisterDetails(String processId, int tenantId) {
        return objectGroupReportCollection.aggregate(
            Arrays.asList(
                // Filter
                Aggregates.match(and(
                    eq(PurgeObjectGroupModel.PROCESS_ID, processId),
                    eq(PurgeObjectGroupModel.TENANT, tenantId),
                    eq("_metadata.status", "DELETED")
                )),

                // Projection
                Aggregates.project(Projections.fields(
                    new Document("_id", 0),
                    new Document("id", "$_metadata.id"),
                    new Document(OPI, "$_metadata." + OPI),
                    new Document(ORIGINATING_AGENCY, "$_metadata." + ORIGINATING_AGENCY),
                    new Document("objectVersions", "$_metadata.objectVersions")

                    )
                ),

                // Create as many documents as there are items in the list objectVersions
                Aggregates.unwind("$objectVersions"),

                // Group BY
                Aggregates.group(new Document(OPI, "$objectVersions." + OPI)
                        .append(OPI_GOT, "$" + OPI),
                    Accumulators
                        .first(ORIGINATING_AGENCY, "$" + ORIGINATING_AGENCY),
                    Accumulators.sum(TOTAL_SIZE, "$objectVersions.size"),
                    Accumulators.sum(TOTAL_OBJECTS, 1),
                    Accumulators.addToSet("listGOT", "$id")
                ),

                // Projection
                Aggregates.project(Projections.fields(
                    new Document("_id", 0),
                    new Document(OPI, "$_id." + OPI),
                    new Document(OPI_GOT, "$_id." + OPI_GOT),
                    new Document(ORIGINATING_AGENCY, 1),
                    new Document(TOTAL_SIZE, 1),
                    new Document(TOTAL_OBJECTS, 1),
                    // if opi == opi_got then $size of $listGOT else 0. @see mongodb $cond.
                    // Do not count ObjectGroup for objects where there opi is different to opi of ObjectGroup himself
                    // The ObjectGroup is already counted with his principal opi
                    new Document(TOTAL_OBJECT_GROUPS, new Document("$cond", Lists
                        .newArrayList(new Document("$eq", Lists.newArrayList("$_id." + OPI, "$_id." + OPI_GOT)),
                            new Document("$size", "$listGOT"), 0)))
                    )
                ),

                // Group BY
                // At this point we will have multiple documents having the same opi => must be grouped by opi in the same document.
                Aggregates.group("$" + OPI,
                    Accumulators
                        .first(ORIGINATING_AGENCY, "$" + ORIGINATING_AGENCY),
                    Accumulators.sum(TOTAL_SIZE, "$" + TOTAL_SIZE),
                    Accumulators.sum(TOTAL_OBJECTS, "$" + TOTAL_OBJECTS),
                    Accumulators.sum(TOTAL_OBJECT_GROUPS, "$" + TOTAL_OBJECT_GROUPS)
                ),

                // Projection
                Aggregates.project(Projections.fields(
                    new Document("_id", 0),
                    new Document(OPI, "$_id"),
                    new Document(ORIGINATING_AGENCY, 1),
                    new Document(TOTAL_SIZE, 1),
                    new Document(TOTAL_OBJECTS, 1),
                    new Document(TOTAL_OBJECT_GROUPS, 1)
                    )
                ),

                // Sort
                Aggregates.sort(Sorts.descending(OPI))
            )
        )
            // Aggregation query requires more than 100MB to proceed.
            .allowDiskUse(true)
            .iterator();
    }
}
