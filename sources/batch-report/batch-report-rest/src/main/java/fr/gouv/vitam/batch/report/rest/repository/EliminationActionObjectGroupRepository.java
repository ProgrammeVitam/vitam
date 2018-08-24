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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import fr.gouv.vitam.batch.report.model.EliminationActionObjectGroupModel;
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
 * EliminationObjectGroup
 */
public class EliminationActionObjectGroupRepository extends EliminationCommonRepository {

    public static final String ELIMINATION_ACTION_OBJECT_GROUP = "EliminationActionObjectGroup";

    private final MongoCollection<Document> objectGroupReportCollection;

    public EliminationActionObjectGroupRepository(MongoDbAccess mongoDbAccess) {
        objectGroupReportCollection =
            mongoDbAccess.getMongoDatabase().getCollection(ELIMINATION_ACTION_OBJECT_GROUP);
    }

    public void bulkAppendReport(List<EliminationActionObjectGroupModel> reports) {
        Set<EliminationActionObjectGroupModel> reportsWithoutDuplicate = new HashSet<>(reports);
        List<Document> eliminationObjectGroupDocument =
            reportsWithoutDuplicate.stream()
                .map(EliminationCommonRepository::pojoToDocument)
                .collect(Collectors.toList());
        super.bulkAppendReport(eliminationObjectGroupDocument, objectGroupReportCollection);
    }

    public MongoCursor<Document> findCollectionByProcessIdTenant(String processId, int tenantId) {

        return objectGroupReportCollection.aggregate(
            Arrays.asList(
                Aggregates.match(and(
                    eq(EliminationActionObjectGroupModel.PROCESS_ID, processId),
                    eq(EliminationActionObjectGroupModel.TENANT, tenantId)
                )),
                Aggregates.project(Projections.fields(
                    new Document("_id", 0),
                    new Document("id", "$_metadata.id"),
                    new Document("distribGroup", null),
                    new Document("params.id", "$_metadata.id"),
                    new Document("params.status", "$_metadata.status"),
                    new Document("params.opi", "$_metadata.opi"),
                    new Document("params.originatingAgency", "$_metadata.originatingAgency"),
                    new Document("params.deletedParentUnitIds", "$_metadata.deletedParentUnitIds"),
                    new Document("params.objectIds", "$_metadata.objectIds")
                    )
                ))
        ).iterator();
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        super.deleteReportByIdAndTenant(processId, tenantId, objectGroupReportCollection);
    }

    /**
     * TODO : JBP
     */
    public Optional<EliminationActionObjectGroupModel> computeRegisterDetails(String processId) {
        //TODO : JBP
        return Optional.empty();
    }
}
