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
import fr.gouv.vitam.batch.report.model.AuditStatsModel;
import fr.gouv.vitam.batch.report.model.ReportItemStatus;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportStatus;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectGroupReportEntry;
import fr.gouv.vitam.batch.report.model.entry.AuditObjectVersion;
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
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class AuditReportRepositoryTest {

    private static final int TENANT_ID = 0;
    public static final String AUDIT_REPORT = "AuditReport" + GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(), AUDIT_REPORT);

    private AuditReportRepository repository;

    private MongoCollection<Document> auditReportCollection;
    private AuditObjectGroupModel auditReportEntryKO;
    private AuditObjectGroupModel auditReportEntryOK;
    private AuditObjectGroupModel auditReportEntryWARNING;
    private String processId;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        repository = new AuditReportRepository(mongoDbAccess, AUDIT_REPORT);
        auditReportCollection = mongoRule.getMongoCollection(AUDIT_REPORT);
        processId = "aeeaaaaaacgw45nxaaopkalhchougsiaaaaq";

        List<AuditObjectVersion> objectVersions1 = new ArrayList<AuditObjectVersion>();
        objectVersions1.add(
            generateVersion(
                "objectId1",
                "objectOpi1",
                "objectQualifier1",
                "objectVersion1",
                "strategyId1",
                ReportStatus.OK,
                ReportStatus.KO,
                ReportStatus.KO
            )
        );
        objectVersions1.add(
            generateVersion(
                "objectId2",
                "objectOpi2",
                "objectQualifier2",
                "objectVersion2",
                "strategyId2",
                ReportStatus.OK,
                ReportStatus.KO,
                ReportStatus.KO
            )
        );
        AuditObjectGroupReportEntry auditObjectGroupEntry1 = new AuditObjectGroupReportEntry(
            "objectGroupId1",
            Collections.singletonList("unitId"),
            "originatingAgency1",
            "opi",
            objectVersions1,
            ReportStatus.KO,
            "outcome"
        );
        auditReportEntryKO = new AuditObjectGroupModel(
            processId,
            LocalDateUtil.nowFormatted(),
            auditObjectGroupEntry1,
            TENANT_ID
        );

        List<AuditObjectVersion> objectVersions2 = new ArrayList<AuditObjectVersion>();
        objectVersions2.add(
            generateVersion(
                "objectId3",
                "objectOpi1",
                "objectQualifier1",
                "objectVersion1",
                "strategyId1",
                ReportStatus.OK,
                ReportStatus.OK,
                ReportStatus.OK
            )
        );
        objectVersions2.add(
            generateVersion(
                "objectId4",
                "objectOpi2",
                "objectQualifier2",
                "objectVersion2",
                "strategyId2",
                ReportStatus.OK,
                ReportStatus.OK,
                ReportStatus.OK
            )
        );
        AuditObjectGroupReportEntry auditObjectGroupEntry2 = new AuditObjectGroupReportEntry(
            "objectGroupId2",
            Collections.singletonList("unitId"),
            "originatingAgency1",
            "opi",
            objectVersions2,
            ReportStatus.OK,
            "outcome"
        );
        auditReportEntryOK = new AuditObjectGroupModel(
            processId,
            LocalDateUtil.nowFormatted(),
            auditObjectGroupEntry2,
            TENANT_ID
        );

        List<AuditObjectVersion> objectVersions3 = new ArrayList<AuditObjectVersion>();
        objectVersions3.add(
            generateVersion(
                "objectId5",
                "objectOpi1",
                "objectQualifier1",
                "objectVersion1",
                "strategyId1",
                ReportStatus.OK,
                ReportStatus.OK,
                ReportStatus.OK
            )
        );
        objectVersions3.add(
            generateVersion(
                "objectId6",
                "objectOpi3",
                "objectQualifier2",
                "objectVersion2",
                "strategyId2",
                ReportStatus.OK,
                ReportStatus.WARNING,
                ReportStatus.WARNING
            )
        );
        AuditObjectGroupReportEntry auditObjectGroupEntry3 = new AuditObjectGroupReportEntry(
            "objectGroupId3",
            Collections.singletonList("unitId"),
            "originatingAgency2",
            "opi",
            objectVersions3,
            ReportStatus.WARNING,
            "outcome"
        );
        auditReportEntryWARNING = new AuditObjectGroupModel(
            processId,
            LocalDateUtil.nowFormatted(),
            auditObjectGroupEntry3,
            TENANT_ID
        );
    }

    @Test
    public void should_append_report() throws InvalidParseOperationException {
        // Given
        populateDatabase(auditReportEntryKO);

        // When
        Document report = auditReportCollection
            .find(and(eq(AuditObjectGroupModel.PROCESS_ID, processId), eq(AuditObjectGroupModel.TENANT, 0)))
            .first();

        // Then
        Object metadata = report.get("_metadata");
        JsonNode metadataNode = JsonHandler.toJsonNode(metadata);
        assertThat(report.get(AuditObjectGroupModel.PROCESS_ID)).isEqualTo(processId);
        assertThat(metadataNode.get("id").asText()).isEqualTo(auditReportEntryKO.getMetadata().getDetailId());
        assertThat(metadataNode.get("status").asText()).isEqualTo(auditReportEntryKO.getMetadata().getStatus().name());
    }

    @Test
    public void should_find_collection_by_processid_tenant() {
        // Given
        populateDatabase(auditReportEntryKO);
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
        populateDatabase(auditReportEntryKO, auditReportEntryOK, auditReportEntryWARNING);
        // When
        MongoCursor<Document> iterator = repository.findCollectionByProcessIdTenantAndStatus(
            processId,
            TENANT_ID,
            "WARNING",
            "KO"
        );

        // Then
        List<Document> documents = new ArrayList<>();
        while (iterator.hasNext()) {
            Document reportModel = iterator.next();
            documents.add(reportModel);
        }
        assertThat(documents.size()).isEqualTo(2);
    }

    @Test
    public void should_generate_vitamResults() throws InvalidParseOperationException {
        // Given
        populateDatabase(auditReportEntryKO, auditReportEntryOK, auditReportEntryWARNING);

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
        populateDatabase(auditReportEntryKO);

        // When
        AuditStatsModel stats = repository.stats(processId, TENANT_ID);

        // Then
        assertThat(stats.getNbObjectGroups()).isEqualTo(1);
        assertThat(stats.getNbObjects()).isEqualTo(2);
        assertThat(stats.getOpis().size()).isEqualTo(2);
        assertThat(stats.getOpis()).contains("objectOpi1", "objectOpi2");
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbOK()).isEqualTo(0);
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbWARNING()).isEqualTo(0);
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbKO()).isEqualTo(1);
        assertThat(stats.getOriginatingAgencyResults().entrySet().size()).isEqualTo(1);
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectGroupsCount().getNbOK()
        ).isEqualTo(0);
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectGroupsCount().getNbWARNING()
        ).isEqualTo(0);
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectGroupsCount().getNbKO()
        ).isEqualTo(1);
        assertThat(stats.getOriginatingAgencyResults().get("FakeoriginatingAgency")).isNull();
    }

    @Test
    public void should_generate_statistic_for_three_objects() {
        // Given
        populateDatabase(auditReportEntryKO, auditReportEntryOK, auditReportEntryWARNING);

        // When
        AuditStatsModel stats = repository.stats(processId, TENANT_ID);

        // Then
        assertThat(stats.getNbObjectGroups()).isEqualTo(3);
        assertThat(stats.getNbObjects()).isEqualTo(6);
        assertThat(stats.getOpis().size()).isEqualTo(3);
        assertThat(stats.getOpis()).contains("objectOpi1", "objectOpi2", "objectOpi3");
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbOK()).isEqualTo(1);
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbWARNING()).isEqualTo(1);
        assertThat(stats.getGlobalResults().getObjectGroupsCount().getNbKO()).isEqualTo(1);
        assertThat(stats.getOriginatingAgencyResults().entrySet().size()).isEqualTo(2);
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectGroupsCount().getNbOK()
        ).isEqualTo(1);
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectGroupsCount().getNbWARNING()
        ).isEqualTo(0);
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectGroupsCount().getNbKO()
        ).isEqualTo(1);
        assertThat(stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectsCount().getNbOK()).isEqualTo(
            2
        );
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectsCount().getNbWARNING()
        ).isEqualTo(0);
        assertThat(stats.getOriginatingAgencyResults().get("originatingAgency1").getObjectsCount().getNbKO()).isEqualTo(
            2
        );
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency2").getObjectGroupsCount().getNbOK()
        ).isEqualTo(0);
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency2").getObjectGroupsCount().getNbWARNING()
        ).isEqualTo(1);
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency2").getObjectGroupsCount().getNbKO()
        ).isEqualTo(0);
        assertThat(stats.getOriginatingAgencyResults().get("originatingAgency2").getObjectsCount().getNbOK()).isEqualTo(
            1
        );
        assertThat(
            stats.getOriginatingAgencyResults().get("originatingAgency2").getObjectsCount().getNbWARNING()
        ).isEqualTo(1);
        assertThat(stats.getOriginatingAgencyResults().get("originatingAgency2").getObjectsCount().getNbKO()).isEqualTo(
            0
        );
        assertThat(stats.getOriginatingAgencyResults().get("FakeoriginatingAgency")).isNull();
    }

    @Test
    public void should_delete_report_by_id_and_tenant() {
        // Given
        populateDatabase(auditReportEntryKO, auditReportEntryOK, auditReportEntryWARNING);
        // When
        repository.deleteReportByIdAndTenant(processId, TENANT_ID);
        // Then
        FindIterable<Document> iterable = auditReportCollection.find(
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

    private void populateDatabase(AuditObjectGroupModel... entries) {
        List<AuditObjectGroupModel> reports = new ArrayList<>();
        reports.addAll(Arrays.asList(entries));
        repository.bulkAppendReport(reports);
    }

    private AuditObjectVersion generateVersion(
        String objectId,
        String objectOpi,
        String objectQualifier,
        String objectVersion,
        String objectStrategy,
        ReportStatus offer1Status,
        ReportStatus offer2Status,
        ReportStatus objectStatus
    ) {
        return new AuditObjectVersion(
            objectId,
            objectOpi,
            objectQualifier,
            objectVersion,
            objectStrategy,
            new ArrayList<ReportItemStatus>() {
                {
                    add(new ReportItemStatus("offerId1", offer1Status));
                    add(new ReportItemStatus("offerId2", offer2Status));
                }
            },
            objectStatus
        );
    }
}
