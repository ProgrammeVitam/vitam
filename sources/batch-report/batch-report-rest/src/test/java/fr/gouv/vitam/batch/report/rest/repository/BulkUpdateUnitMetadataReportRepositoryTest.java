/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.model.entry.BulkUpdateUnitMetadataReportEntry;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.batch.report.model.ReportType.BULK_UPDATE_UNIT;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;

public class BulkUpdateUnitMetadataReportRepositoryTest {

    private static final int TENANT_ID = 0;
    public static final String COLLECTION_NAME = "BulkUpdateUnitMetadataReport" + GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule = new MongoRule(getMongoClientOptions(), COLLECTION_NAME);

    private BulkUpdateUnitMetadataReportRepository repository;

    private MongoCollection<Document> bulkReportCollection;
    private BulkUpdateUnitMetadataReportEntry bulkUpdateUnitMetadataEntryKO;
    private BulkUpdateUnitMetadataReportEntry bulkUpdateUnitMetadataEntryOK;
    private BulkUpdateUnitMetadataReportEntry bulkUpdateUnitMetadataEntryWARNING;
    private String processId;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new BulkUpdateUnitMetadataReportRepository(mongoDbAccess, COLLECTION_NAME);
        bulkReportCollection = mongoRule.getMongoCollection(COLLECTION_NAME);
        processId = "aeeaaaaaacgw45nxaaopkalhchougsiaaaaq";

        String unPrettyQuery =
            "{\"$query\":[{\"$match\":{\"Title\":\"sous fonds\"}}],\"$action\":[{\"$set\":{\"Title\":\"update old title sous fonds émouvantี\"}}]}";
        bulkUpdateUnitMetadataEntryKO = new BulkUpdateUnitMetadataReportEntry(
            TENANT_ID,
            processId,
            "unit1",
            unPrettyQuery, null, "More than one Unit was found for the $query", StatusCode.KO,
            String.format("%s.%s", "PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST", StatusCode.KO),
            "More than one Unit was found for the $query");

        bulkUpdateUnitMetadataEntryWARNING = new BulkUpdateUnitMetadataReportEntry(
            TENANT_ID,
            processId,
            "unit2",
            unPrettyQuery, null, "No Unit was found for the $query", StatusCode.WARNING,
            String.format("%s.%s", "PREPARE_BULK_ATOMIC_UPDATE_UNIT_LIST", StatusCode.WARNING),
            "No Unit was found for the $query");

        bulkUpdateUnitMetadataEntryOK = new BulkUpdateUnitMetadataReportEntry(
            TENANT_ID,
            processId,
            "unit3",
            unPrettyQuery, GUIDFactory.newGUID().getId(), "Update done", StatusCode.OK,
            String.format("%s.%s", "BULK_ATOMIC_UPDATE_UNITS", StatusCode.OK), "All went good");
    }

    @Test
    public void should_append_report() throws InvalidParseOperationException {
        // Given
        populateDatabase(bulkUpdateUnitMetadataEntryKO);

        // When
        Document report = bulkReportCollection
            .find(and(eq(BulkUpdateUnitMetadataReportEntry.PROCESS_ID, processId),
                eq(BulkUpdateUnitMetadataReportEntry.TENANT_ID, 0)))
            .first();

        // Then
        assertThat(report.get(BulkUpdateUnitMetadataReportEntry.PROCESS_ID)).isEqualTo(processId);
        assertThat(report.get(BulkUpdateUnitMetadataReportEntry.TENANT_ID)).isEqualTo(0);
        assertThat(report.get(BulkUpdateUnitMetadataReportEntry.UNIT_ID)).isNull();
        assertThat(report.get(BulkUpdateUnitMetadataReportEntry.DETAIL_TYPE).toString())
            .isEqualTo(BULK_UPDATE_UNIT.name());
        assertThat(report.get(BulkUpdateUnitMetadataReportEntry.STATUS).toString())
            .isEqualTo(bulkUpdateUnitMetadataEntryKO.getStatus().name());
    }

    @Test
    public void should_find_collection_by_processid_tenant() {
        // Given
        populateDatabase(bulkUpdateUnitMetadataEntryKO);
        // When
        MongoCursor<Document> iterator = repository.findCollectionByProcessIdTenant(processId, TENANT_ID);

        // Then
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            Document reportModel = iterator.next();
            documents.add(reportModel);
        }
        assertThat(documents.size()).isEqualTo(1);
    }

    @Test
    public void should_find_collection_by_processid_tenant_status() {
        // Given
        populateDatabase(bulkUpdateUnitMetadataEntryKO, bulkUpdateUnitMetadataEntryWARNING,
            bulkUpdateUnitMetadataEntryOK);
        // When
        MongoCursor<Document> iterator = repository.findCollectionByProcessIdTenant(processId, TENANT_ID);

        // Then
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            Document reportModel = iterator.next();
            documents.add(reportModel);
        }
        assertThat(documents.size()).isEqualTo(3);
    }

    @Test
    public void should_delete_report_by_id_and_tenant() {
        // Given
        populateDatabase(bulkUpdateUnitMetadataEntryKO, bulkUpdateUnitMetadataEntryWARNING,
            bulkUpdateUnitMetadataEntryOK);
        // When
        repository.deleteReportByIdAndTenant(processId, TENANT_ID);
        // Then
        FindIterable<Document> iterable = bulkReportCollection
            .find(and(eq("processId", processId), eq("tenantId", TENANT_ID)));
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        assertThat(documents).isEmpty();
        assertThat(documents.size()).isEqualTo(0);
    }

    private void populateDatabase(BulkUpdateUnitMetadataReportEntry... entries) {
        List<BulkUpdateUnitMetadataReportEntry> reports = new ArrayList<>();
        reports.addAll(Arrays.asList(entries));
        repository.bulkAppendReport(reports);
    }

}
