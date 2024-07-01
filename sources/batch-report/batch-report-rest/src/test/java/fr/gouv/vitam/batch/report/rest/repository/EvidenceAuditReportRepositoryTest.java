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
import fr.gouv.vitam.batch.report.model.EvidenceAuditObjectModel;
import fr.gouv.vitam.batch.report.model.EvidenceAuditReportObject;
import fr.gouv.vitam.batch.report.model.EvidenceAuditStatsModel;
import fr.gouv.vitam.batch.report.model.EvidenceStatus;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.entry.EvidenceAuditReportEntry;
import fr.gouv.vitam.common.LocalDateUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class EvidenceAuditReportRepositoryTest {

    public static final String EVIDENCE_AUDIT_REPORT = "EvidenceAuditReport" + GUIDFactory.newGUID().getId();
    private static final int TENANT_ID = 0;

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), EVIDENCE_AUDIT_REPORT);

    private EvidenceAuditReportRepository evidenceAuditRepository;

    private MongoCollection<Document> evidenceAuditReportCollection;
    private EvidenceAuditObjectModel evidenceAuditReportEntryKO;
    private EvidenceAuditObjectModel evidenceAuditReportEntryOK;
    private EvidenceAuditObjectModel evidenceAuditReportEntryWARN;
    private String processId;
    private String DEFAULT_STRATEGY = "default";

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        evidenceAuditRepository = new EvidenceAuditReportRepository(mongoDbAccess, EVIDENCE_AUDIT_REPORT);
        evidenceAuditReportCollection = mongoRule.getMongoCollection(EVIDENCE_AUDIT_REPORT);
        processId = "aeeaaaaaagfpuagsaauscallv6nhzlyaaaaq";

        // TODO  get from JSON File
        Map<String, String> offerHashesObject = new HashMap<>();
        offerHashesObject.put(
            "offer-fs-1.service.consul",
            "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b"
        );
        ArrayList<EvidenceAuditReportObject> listEvidenceAuditReportObject = new ArrayList<>();
        EvidenceAuditReportObject evidenceAuditReportObject = new EvidenceAuditReportObject(
            "aebaaaaaaeg7sn7vabtioallotfxytyaaaaq",
            EvidenceStatus.OK.name(),
            "audit OK for ObjectGroup",
            "OBJECTGROUP",
            "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
            DEFAULT_STRATEGY,
            offerHashesObject
        );
        listEvidenceAuditReportObject.add(evidenceAuditReportObject);
        Map<String, String> offerHashesUnit = new HashMap<>();
        offerHashesObject.put(
            "offer-fs-1.service.consul",
            "cc9221173b12e75f7a8f629d49632ffbb87538b13d82ab7cc02e8b3486131fb609c6303bb7b5f67962e45b068e74cb40aa8c8906bcbb9db9fd6ebbe89929d655"
        );
        EvidenceAuditReportEntry evidenceAuditReportEntry1 = new EvidenceAuditReportEntry(
            "Id1",
            EvidenceStatus.OK.name(),
            "audit OK for aebaaaaaaeg7sn7vabtioallotfxytyaaaaq",
            "OBJECTGROUP",
            listEvidenceAuditReportObject,
            "cc9221173b12e75f7a8f629d49632ffbb87538b13d82ab7cc02e8b3486131fb609c6303bb7b5f67962e45b068e74cb40aa8c8906bcbb9db9fd6ebbe89929d655",
            DEFAULT_STRATEGY,
            offerHashesUnit,
            EvidenceStatus.OK.name()
        );
        evidenceAuditReportEntryOK = new EvidenceAuditObjectModel(
            processId,
            TENANT_ID,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            evidenceAuditReportEntry1
        );

        Map<String, String> offerHashesObject2 = new HashMap<>();
        offerHashesObject2.put(
            "offer-fs-1.service.consul",
            "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b"
        );
        ArrayList<EvidenceAuditReportObject> listEvidenceAuditReportObject2 = new ArrayList<>();
        EvidenceAuditReportObject evidenceAuditReportObject2 = new EvidenceAuditReportObject(
            "aebaaaaaaeg7sn7vabtioallotfxytyaaaaq",
            EvidenceStatus.KO.name(),
            "audit OK for ObjectGroup",
            "OBJECTGROUP",
            "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
            DEFAULT_STRATEGY,
            offerHashesObject2
        );
        listEvidenceAuditReportObject2.add(evidenceAuditReportObject2);
        Map<String, String> offerHashesUnit2 = new HashMap<>();
        offerHashesObject.put(
            "offer-fs-1.service.consul",
            "cc9221173b12e75f7a8f629d49632ffbb87538b13d82ab7cc02e8b3486131fb609c6303bb7b5f67962e45b068e74cb40aa8c8906bcbb9db9fd6ebbe89929d655"
        );
        EvidenceAuditReportEntry evidenceAuditReportEntry2 = new EvidenceAuditReportEntry(
            "Id2",
            EvidenceStatus.KO.name(),
            "audit OK for aebaaaaaaeg7sn7vabtioallotfxytyaaaaq",
            "OBJECTGROUP",
            listEvidenceAuditReportObject2,
            "cc9221173b12e75f7a8f629d49632ffbb87538b13d82ab7cc02e8b3486131fb609c6303bb7b5f67962e45b068e74cb40aa8c8906bcbb9db9fd6ebbe89929d655",
            DEFAULT_STRATEGY,
            offerHashesUnit2,
            EvidenceStatus.KO.name()
        );
        evidenceAuditReportEntryKO = new EvidenceAuditObjectModel(
            processId,
            TENANT_ID,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            evidenceAuditReportEntry2
        );

        Map<String, String> offerHashesObject3 = new HashMap<>();
        offerHashesObject3.put(
            "offer-fs-1.service.consul",
            "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b"
        );
        ArrayList<EvidenceAuditReportObject> listEvidenceAuditReportObject3 = new ArrayList<>();
        EvidenceAuditReportObject evidenceAuditReportObject3 = new EvidenceAuditReportObject(
            "aebaaaaaaeg7sn7vabtioallotfxytyaaaaq",
            EvidenceStatus.WARN.name(),
            "audit OK for ObjectGroup",
            "OBJECTGROUP",
            "664ac614a819df2a97d2a5df57dcad91d6ec38b0fffc793e80c56b4553a14ac7a5f0bce3bb71af419b0bb8f151ad3d512867454eeb818e01818a31989c13319b",
            DEFAULT_STRATEGY,
            offerHashesObject3
        );
        listEvidenceAuditReportObject3.add(evidenceAuditReportObject3);

        Map<String, String> offerHashesUnit3 = new HashMap<>();
        offerHashesObject.put(
            "offer-fs-1.service.consul",
            "cc9221173b12e75f7a8f629d49632ffbb87538b13d82ab7cc02e8b3486131fb609c6303bb7b5f67962e45b068e74cb40aa8c8906bcbb9db9fd6ebbe89929d655"
        );

        EvidenceAuditReportEntry evidenceAuditReportEntry3 = new EvidenceAuditReportEntry(
            "Id3",
            EvidenceStatus.WARN.name(),
            "audit OK for aebaaaaaaeg7sn7vabtioallotfxytyaaaaq",
            "OBJECTGROUP",
            listEvidenceAuditReportObject3,
            "cc9221173b12e75f7a8f629d49632ffbb87538b13d82ab7cc02e8b3486131fb609c6303bb7b5f67962e45b068e74cb40aa8c8906bcbb9db9fd6ebbe89929d655",
            DEFAULT_STRATEGY,
            offerHashesUnit3,
            EvidenceStatus.WARN.name()
        );

        evidenceAuditReportEntryWARN = new EvidenceAuditObjectModel(
            processId,
            TENANT_ID,
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            evidenceAuditReportEntry3
        );
    }

    @Test
    public void should_append_report() throws InvalidParseOperationException {
        // Given
        populateDatabase(evidenceAuditReportEntryKO);

        // When
        Document report = evidenceAuditReportCollection
            .find(and(eq(EvidenceAuditObjectModel.PROCESS_ID, processId), eq(EvidenceAuditObjectModel.TENANT, 0)))
            .first();

        // Then
        Object metadata = report.get("_metadata");
        JsonNode metadataNode = JsonHandler.toJsonNode(metadata);
        assertThat(report.get(EvidenceAuditObjectModel.PROCESS_ID)).isEqualTo(processId);
        assertThat(metadataNode.get("id").asText()).isEqualTo(evidenceAuditReportEntryKO.getMetadata().getDetailId());
        assertThat(metadataNode.get("status").asText()).isEqualTo(
            evidenceAuditReportEntryKO.getMetadata().getEvidenceStatus()
        );
    }

    @Test
    public void should_find_collection_by_processid_tenant() {
        // Given
        populateDatabase(evidenceAuditReportEntryKO, evidenceAuditReportEntryOK, evidenceAuditReportEntryWARN);
        // When
        MongoCursor<Document> iterator = evidenceAuditRepository.findCollectionByProcessIdTenant(processId, TENANT_ID);

        // Then
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            Document reportModel = iterator.next();
            documents.add(reportModel);
        }
        assertThat(documents.size()).isEqualTo(3);
    }

    @Test
    public void should_generate_vitamResults() throws InvalidParseOperationException {
        // Given
        populateDatabase(evidenceAuditReportEntryKO, evidenceAuditReportEntryOK, evidenceAuditReportEntryWARN);

        // When
        ReportResults reportResult = evidenceAuditRepository.computeVitamResults(processId, TENANT_ID);

        // Then
        assertThat(reportResult.getNbOk()).isEqualTo(2);
        assertThat(reportResult.getNbWarning()).isEqualTo(2);
        assertThat(reportResult.getNbKo()).isEqualTo(2);
        assertThat(reportResult.getTotal()).isEqualTo(6);
    }

    @Test
    public void should_generate_statistic() {
        // Given
        populateDatabase(evidenceAuditReportEntryKO);

        // When
        EvidenceAuditStatsModel stats = evidenceAuditRepository.stats(processId, TENANT_ID);

        // Then
        assertThat(stats.getNbObjectGroups()).isEqualTo(1);
    }

    @Test
    public void should_generate_statistic_for_three_objects() {
        // Given
        populateDatabase(evidenceAuditReportEntryKO, evidenceAuditReportEntryOK, evidenceAuditReportEntryWARN);

        // When
        EvidenceAuditStatsModel stats = evidenceAuditRepository.stats(processId, TENANT_ID);

        // Then
        assertThat(stats.getNbObjectGroups()).isEqualTo(3);
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbOK()).isEqualTo(1);
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbWARNING()).isEqualTo(1);
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbKO()).isEqualTo(1);
    }

    @Test
    public void should_delete_report_by_id_and_tenant() {
        // Given
        populateDatabase(evidenceAuditReportEntryKO, evidenceAuditReportEntryOK, evidenceAuditReportEntryWARN);
        // When
        evidenceAuditRepository.deleteReportByIdAndTenant(processId, TENANT_ID);
        // Then
        FindIterable<Document> iterable = evidenceAuditReportCollection.find(
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

    private void populateDatabase(EvidenceAuditObjectModel... entries) {
        List<EvidenceAuditObjectModel> reports = new ArrayList<>();
        reports.addAll(Arrays.asList(entries));
        evidenceAuditRepository.bulkAppendReport(reports);
    }
}
