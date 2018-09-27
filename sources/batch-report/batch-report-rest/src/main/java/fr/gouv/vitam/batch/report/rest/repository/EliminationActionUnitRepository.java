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

import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import fr.gouv.vitam.batch.report.model.EliminationActionUnitModel;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import org.bson.Document;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * ReportRepository
 */
public class EliminationActionUnitRepository extends EliminationCommonRepository {

    public static final String ELIMINATION_ACTION_UNIT = "EliminationActionUnit";
    public static final String METADATA_OBJECT_GROUP_ID = "_metadata.objectGroupId";
    private final MongoCollection<Document> unitReportCollection;

    public EliminationActionUnitRepository(MongoDbAccess mongoDbAccess) {
        this.unitReportCollection = mongoDbAccess.getMongoDatabase().getCollection(ELIMINATION_ACTION_UNIT);
    }

    public void bulkAppendReport(List<EliminationActionUnitModel> reports) {
        Set<EliminationActionUnitModel> reportsWithoutDuplicate = new HashSet<>(reports);
        List<Document> eliminationUnitDocument =
            reportsWithoutDuplicate.stream()
                .map(EliminationCommonRepository::pojoToDocument)
                .collect(Collectors.toList());
        super.bulkAppendReport(eliminationUnitDocument, unitReportCollection);
    }

    public MongoCursor<Document> findCollectionByProcessIdTenant(String processId, int tenantId) {

        return unitReportCollection.aggregate(
            Arrays.asList(
                Aggregates.match(and(
                    eq(EliminationActionUnitModel.PROCESS_ID, processId),
                    eq(EliminationActionUnitModel.TENANT, tenantId)
                )),
                Aggregates.project(Projections.fields(
                    new Document("_id", 0),
                    new Document("id", "$_metadata.id"),
                    new Document("distribGroup", null),
                    new Document("params.id", "$_metadata.id"),
                    new Document("params.status", "$_metadata.status"),
                    new Document("params.opi", "$_metadata.opi"),
                    new Document("params.originatingAgency", "$_metadata.originatingAgency"),
                    new Document("params.objectGroupId", "$_metadata.objectGroupId")
                    )
                ))
        ).iterator();
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        super.deleteReportByIdAndTenant(processId, tenantId, unitReportCollection);
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
            unitReportCollection.distinct(METADATA_OBJECT_GROUP_ID, and(eq(EliminationActionUnitModel.PROCESS_ID, processId),
                eq("_metadata.status", "DELETED"), eq(EliminationActionUnitModel.TENANT, tenantId)), String.class);
        return distinctObjectGroup.iterator();
    }

    /**
     * TODO JBP
     */
    public Optional<EliminationActionUnitModel> computeRegisterDetails(String processId) {
        //TODO : JBP
        return Optional.empty();
    }
}
