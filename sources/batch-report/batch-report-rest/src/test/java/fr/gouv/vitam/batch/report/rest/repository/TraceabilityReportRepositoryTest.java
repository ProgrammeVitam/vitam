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

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.batch.report.model.AuditObjectGroupModel;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.TraceabilityObjectModel;
import fr.gouv.vitam.batch.report.model.TraceabilityStatsModel;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
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
import static org.assertj.core.api.Assertions.assertThat;

public class TraceabilityReportRepositoryTest {

    private static final int TENANT_ID = 0;
    public static final String TRACEABILITY_REPORT = "TraceabilityReport" + GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), TRACEABILITY_REPORT);

    private TraceabilityReportRepository repository;

    private MongoCollection<Document> traceabilityReportCollection;

    private TraceabilityObjectModel traceabilityObjectModelWARNING;
    private TraceabilityObjectModel traceabilityObjectModelKO;
    private TraceabilityObjectModel traceabilityObjectModelOK;
    private String processId;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new TraceabilityReportRepository(mongoDbAccess, TRACEABILITY_REPORT);
        traceabilityReportCollection = mongoRule.getMongoCollection(TRACEABILITY_REPORT);
        processId = "aeeaaaaaacgw45nxaaopkalhchougsiaaaaq";

        traceabilityObjectModelOK = new TraceabilityObjectModel(
            processId,
            generateTraceabilityReportEntry("OK", "OPERATION"),
            TENANT_ID
        );
        traceabilityObjectModelKO = new TraceabilityObjectModel(
            processId,
            generateTraceabilityReportEntry("KO", "STORAGE"),
            TENANT_ID
        );
        traceabilityObjectModelWARNING = new TraceabilityObjectModel(
            processId,
            generateTraceabilityReportEntry("WARNING", "UNIT_LIFECYCLE"),
            TENANT_ID
        );
    }

    @Test
    public void should_append_report() throws InvalidParseOperationException {
        // Given
        populateDatabase(traceabilityObjectModelOK);

        // When
        Document report = traceabilityReportCollection
            .find(and(eq(AuditObjectGroupModel.PROCESS_ID, processId), eq(AuditObjectGroupModel.TENANT, 0)))
            .first();

        // Then
        Object metadata = report.get("_metadata");
        JsonNode metadataNode = JsonHandler.toJsonNode(metadata);
        assertThat(report.get(AuditObjectGroupModel.PROCESS_ID)).isEqualTo(processId);
        assertThat(metadataNode.get("id").asText()).isEqualTo(traceabilityObjectModelOK.getMetadata().getOperationId());
        assertThat(metadataNode.get("status").asText()).isEqualTo(traceabilityObjectModelOK.getMetadata().getStatus());
    }

    @Test
    public void should_find_collection_by_processid_tenant() {
        // Given
        populateDatabase(traceabilityObjectModelOK);
        // When
        MongoCursor<Document> iterator = repository.findCollection(processId, TENANT_ID);

        // Then
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            Document reportModel = iterator.next();
            documents.add(reportModel);
        }
        assertThat(documents.size()).isEqualTo(1);
    }

    @Test
    public void should_generate_vitamResults() {
        // Given
        populateDatabase(traceabilityObjectModelOK, traceabilityObjectModelKO, traceabilityObjectModelWARNING);

        // When
        ReportResults reportResult = repository.computeVitamResults(processId, TENANT_ID);

        // Then
        assertThat(reportResult.getNbOk()).isEqualTo(1);
        assertThat(reportResult.getNbWarning()).isEqualTo(1);
        assertThat(reportResult.getNbKo()).isEqualTo(1);
    }

    @Test
    public void should_generate_statistic() {
        // Given
        populateDatabase(traceabilityObjectModelOK);

        // When
        TraceabilityStatsModel stats = repository.stats(processId, TENANT_ID);

        // Then
        assertThat(stats.getNbOperations()).isEqualTo(1);
        assertThat(stats.getNbStorage()).isEqualTo(0);
        assertThat(stats.getNbUnitLFC()).isEqualTo(0);
        assertThat(stats.getNbGotLFC()).isEqualTo(0);
    }

    @Test
    public void should_generate_statistic_for_three_objects() {
        // Given
        populateDatabase(traceabilityObjectModelOK, traceabilityObjectModelKO, traceabilityObjectModelWARNING);

        // When
        TraceabilityStatsModel stats = repository.stats(processId, TENANT_ID);

        // Then
        assertThat(stats.getNbOperations()).isEqualTo(1);
        assertThat(stats.getNbStorage()).isEqualTo(1);
        assertThat(stats.getNbUnitLFC()).isEqualTo(1);
        assertThat(stats.getNbGotLFC()).isEqualTo(0);
    }

    @Test
    public void should_delete_report_by_id_and_tenant() {
        // Given
        populateDatabase(traceabilityObjectModelOK, traceabilityObjectModelKO, traceabilityObjectModelWARNING);
        // When
        repository.deleteReportByIdAndTenant(processId, TENANT_ID);
        // Then
        FindIterable<Document> iterable = traceabilityReportCollection.find(
            and(eq("processId", processId), eq("tenantId", TENANT_ID))
        );
        MongoCursor<Document> iterator = iterable.iterator();
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            documents.add(iterator.next());
        }
        assertThat(documents).isEmpty();
        assertThat(documents.size()).isEqualTo(0);
    }

    private TraceabilityReportEntry generateTraceabilityReportEntry(String status, String operationType) {
        return new TraceabilityReportEntry(
            "FAKE_OP_ID_" + status,
            operationType,
            status,
            "Operation is " + status,
            null,
            null,
            null,
            null,
            null
        );
    }

    private void populateDatabase(TraceabilityObjectModel... entries) {
        List<TraceabilityObjectModel> reports = new ArrayList<>(Arrays.asList(entries));
        repository.bulkAppendReport(reports);
    }
}
