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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.batch.report.model.EliminationActionUnitModel;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * ReportCommonRepository
 */
public abstract class ReportCommonRepository {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReportCommonRepository.class);

    /**
     * Insert List of document in the given collection
     *
     * @param reports report
     */
    protected void bulkAppendReport(List<Document> reports, MongoCollection<Document> collection) {
        if (reports.isEmpty()) {
            return;
        }
        final List<WriteModel<Document>> updates = new ArrayList<>();
        for (Document document : reports) {
            Document metadata = (Document) document.get("_metadata");
            updates.add(
                new UpdateOneModel<>(
                    and(
                        eq("_metadata.id", metadata.get("id")),
                        eq("processId", document.get("processId")),
                        eq("_tenant", document.get("_tenant"))
                    ),
                    new Document("$set", document).append(
                        "$setOnInsert",
                        new Document("_id", GUIDFactory.newGUID().toString())
                    ),
                    new UpdateOptions().upsert(true)
                )
            );
        }
        collection.bulkWrite(updates);
    }

    /**
     * delete Report By Id and Tenant
     *
     * @param processId processId
     * @param tenantId tenantId
     */
    protected void deleteReportByIdAndTenant(String processId, int tenantId, MongoCollection<Document> collection) {
        DeleteResult deleteResult = collection.deleteMany(
            and(eq(EliminationActionUnitModel.PROCESS_ID, processId), eq(EliminationActionUnitModel.TENANT, tenantId))
        );
        LOGGER.info("Deleted document count: " + deleteResult.getDeletedCount() + " for process " + processId);
    }

    static Document pojoToDocument(Object object) {
        try {
            String json = JsonHandler.writeAsString(object);
            return Document.parse(json);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
