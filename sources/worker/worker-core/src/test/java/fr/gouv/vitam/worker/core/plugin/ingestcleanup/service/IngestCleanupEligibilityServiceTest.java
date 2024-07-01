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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.CleanupReportManager;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IngestCleanupEligibilityServiceTest {

    private static final String INGEST_OPERATION_ID = "aeeaaaaaacesicexaah6kalo7e62mmqaaaaq";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @InjectMocks
    private IngestCleanupEligibilityService instance;

    @Before
    public void setUp() throws Exception {
        doReturn(metaDataClient).when(metaDataClientFactory).getClient();
        doReturn(logbookOperationsClient).when(logbookOperationsClientFactory).getClient();
    }

    @Test
    public void givenNoChildUnitsThenCheckChildUnitsFromOtherIngestsReturnsOK() throws Exception {
        // Given
        JsonNode unitIds = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Arrays.asList(
                    JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), "unit1"),
                    JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), "unit2"),
                    JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), "unit3")
                )
            )
            .toJsonNode();
        JsonNode childUnits = new RequestResponseOK<JsonNode>().toJsonNode();

        when(metaDataClient.selectUnits(any())).thenReturn(unitIds).thenReturn(childUnits);

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkChildUnitsFromOtherIngests(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.OK);
        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(2)).selectUnits(queryCaptor.capture());
        checkQueryEquals(queryCaptor.getAllValues().get(0), "IngestCleanup/Eligibility/selectUnitIdsQuery.json");
        checkQueryEquals(queryCaptor.getAllValues().get(1), "IngestCleanup/Eligibility/selectChildUnitsQuery.json");
    }

    @Test
    public void givenChildUnitsThenCheckChildUnitsFromOtherIngestsReturnsKO() throws Exception {
        // Given
        JsonNode unitIds = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Arrays.asList(
                    JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), "unit1"),
                    JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), "unit2"),
                    JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), "unit3")
                )
            )
            .toJsonNode();
        JsonNode childUnits1 = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Collections.singletonList(
                    JsonHandler.createObjectNode()
                        .set(VitamFieldsHelper.unitups(), JsonHandler.createArrayNode().add("unit2"))
                )
            )
            .toJsonNode();
        JsonNode childUnits2 = new RequestResponseOK<JsonNode>().toJsonNode();

        when(metaDataClient.selectUnits(any())).thenReturn(unitIds).thenReturn(childUnits1).thenReturn(childUnits2);

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkChildUnitsFromOtherIngests(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(cleanupReportManager.getCleanupReport().getUnits()).containsOnlyKeys("unit2");
        assertThat(cleanupReportManager.getCleanupReport().getUnits().get("unit2").getErrors()).isNotEmpty();
        assertThat(cleanupReportManager.getCleanupReport().getUnits().get("unit2").getWarnings()).isNull();

        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(3)).selectUnits(queryCaptor.capture());
        checkQueryEquals(queryCaptor.getAllValues().get(0), "IngestCleanup/Eligibility/selectUnitIdsQuery.json");
        checkQueryEquals(queryCaptor.getAllValues().get(1), "IngestCleanup/Eligibility/selectChildUnitsQuery.json");
        checkQueryEquals(
            queryCaptor.getAllValues().get(2),
            "IngestCleanup/Eligibility/selectChildUnitsQueryRemaining.json"
        );
    }

    @Test
    public void givenNoUnitUpdatesThenCheckUnitUpdatesFromOtherOperationsOK() throws Exception {
        // Given
        JsonNode unitIds = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Arrays.asList(
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "unit1")
                        .set(VitamFieldsHelper.operations(), JsonHandler.createArrayNode().add(INGEST_OPERATION_ID)),
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "unit2")
                        .set(VitamFieldsHelper.operations(), JsonHandler.createArrayNode().add(INGEST_OPERATION_ID))
                )
            )
            .toJsonNode();
        when(metaDataClient.selectUnits(any())).thenReturn(unitIds);

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkUnitUpdatesFromOtherOperations(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.OK);
        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(1)).selectUnits(queryCaptor.capture());
        checkQueryEquals(queryCaptor.getAllValues().get(0), "IngestCleanup/Eligibility/selectUnitOperationsQuery.json");
    }

    @Test
    public void givenUnitUpdatesThenCheckUnitUpdatesFromOtherOperationsWarning() throws Exception {
        // Given
        String anotherOperationId = "aecaaaaaachipxsgaamxmalhi4ovibiaaaar";
        JsonNode unitIds = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Arrays.asList(
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "unit1")
                        .set(VitamFieldsHelper.operations(), JsonHandler.createArrayNode().add(INGEST_OPERATION_ID)),
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "unit2")
                        .set(
                            VitamFieldsHelper.operations(),
                            JsonHandler.createArrayNode().add(INGEST_OPERATION_ID).add(anotherOperationId)
                        )
                )
            )
            .toJsonNode();
        when(metaDataClient.selectUnits(any())).thenReturn(unitIds);

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkUnitUpdatesFromOtherOperations(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(cleanupReportManager.getCleanupReport().getUnits()).containsOnlyKeys("unit2");
        assertThat(cleanupReportManager.getCleanupReport().getUnits().get("unit2").getWarnings()).isNotEmpty();
        assertThat(cleanupReportManager.getCleanupReport().getUnits().get("unit2").getErrors()).isNull();
        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(1)).selectUnits(queryCaptor.capture());
        checkQueryEquals(queryCaptor.getAllValues().get(0), "IngestCleanup/Eligibility/selectUnitOperationsQuery.json");
    }

    @Test
    public void givenNoObjectGroupUpdatesThenCheckObjectGroupUpdatesFromOtherOperationsOK() throws Exception {
        // Given
        JsonNode objectGroups = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Arrays.asList(
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "og1")
                        .set(VitamFieldsHelper.operations(), JsonHandler.createArrayNode().add(INGEST_OPERATION_ID)),
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "og1")
                        .set(VitamFieldsHelper.operations(), JsonHandler.createArrayNode().add(INGEST_OPERATION_ID))
                )
            )
            .toJsonNode();
        when(metaDataClient.selectObjectGroups(any())).thenReturn(objectGroups);

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkObjectGroupUpdatesFromOtherOperations(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.OK);
        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(1)).selectObjectGroups(queryCaptor.capture());
        checkQueryEquals(
            queryCaptor.getAllValues().get(0),
            "IngestCleanup/Eligibility/selectObjectGroupOperationsQuery.json"
        );
        verifyZeroInteractions(logbookOperationsClient);
    }

    @Test
    public void givenIngestObjectGroupUpdatesThenCheckObjectGroupUpdatesFromOtherOperationsKO() throws Exception {
        // Given
        String anotherOperationId = "aecaaaaaachipxsgaamxmalhi4ovibiaaaar";
        JsonNode objectGroups = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Arrays.asList(
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "og1")
                        .set(VitamFieldsHelper.operations(), JsonHandler.createArrayNode().add(INGEST_OPERATION_ID)),
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "og1")
                        .set(
                            VitamFieldsHelper.operations(),
                            JsonHandler.createArrayNode().add(INGEST_OPERATION_ID).add(anotherOperationId)
                        )
                )
            )
            .toJsonNode();
        when(metaDataClient.selectObjectGroups(any())).thenReturn(objectGroups);

        doReturn(
            new RequestResponseOK<JsonNode>()
                .addAllResults(
                    Collections.singletonList(
                        JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), anotherOperationId)
                    )
                )
                .toJsonNode()
        )
            .when(logbookOperationsClient)
            .selectOperation(any());

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkObjectGroupUpdatesFromOtherOperations(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(cleanupReportManager.getCleanupReport().getObjectGroups()).containsOnlyKeys("og1");
        assertThat(cleanupReportManager.getCleanupReport().getObjectGroups().get("og1").getErrors()).isNotEmpty();
        assertThat(cleanupReportManager.getCleanupReport().getObjectGroups().get("og1").getWarnings()).isNull();

        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(1)).selectObjectGroups(queryCaptor.capture());
        checkQueryEquals(
            queryCaptor.getAllValues().get(0),
            "IngestCleanup/Eligibility/selectObjectGroupOperationsQuery.json"
        );

        ArgumentCaptor<JsonNode> logbookQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(logbookOperationsClient).selectOperation(logbookQueryCaptor.capture());
        checkQueryEquals(
            logbookQueryCaptor.getAllValues().get(0),
            "IngestCleanup/Eligibility/selectIngestLogbookOperationsQuery.json"
        );
    }

    @Test
    public void givenNonIngestObjectGroupUpdatesThenCheckObjectGroupUpdatesFromOtherOperationsWarning()
        throws Exception {
        // Given
        String anotherOperationId = "aecaaaaaachipxsgaamxmalhi4ovibiaaaar";
        JsonNode objectGroups = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Arrays.asList(
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "og1")
                        .set(VitamFieldsHelper.operations(), JsonHandler.createArrayNode().add(INGEST_OPERATION_ID)),
                    JsonHandler.createObjectNode()
                        .put(VitamFieldsHelper.id(), "og1")
                        .set(
                            VitamFieldsHelper.operations(),
                            JsonHandler.createArrayNode().add(INGEST_OPERATION_ID).add(anotherOperationId)
                        )
                )
            )
            .toJsonNode();
        when(metaDataClient.selectObjectGroups(any())).thenReturn(objectGroups);

        doReturn(new RequestResponseOK<JsonNode>().toJsonNode()).when(logbookOperationsClient).selectOperation(any());

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkObjectGroupUpdatesFromOtherOperations(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(cleanupReportManager.getCleanupReport().getObjectGroups()).containsOnlyKeys("og1");
        assertThat(cleanupReportManager.getCleanupReport().getObjectGroups().get("og1").getErrors()).isNull();
        assertThat(cleanupReportManager.getCleanupReport().getObjectGroups().get("og1").getWarnings()).isNotEmpty();

        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(1)).selectObjectGroups(queryCaptor.capture());
        checkQueryEquals(
            queryCaptor.getAllValues().get(0),
            "IngestCleanup/Eligibility/selectObjectGroupOperationsQuery.json"
        );

        ArgumentCaptor<JsonNode> logbookQueryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(logbookOperationsClient).selectOperation(logbookQueryCaptor.capture());
        checkQueryEquals(
            logbookQueryCaptor.getAllValues().get(0),
            "IngestCleanup/Eligibility/selectIngestLogbookOperationsQuery.json"
        );
    }

    @Test
    public void givenNoAttachmentsThenCheckObjectAttachmentsToExistingObjectGroupsOK() throws Exception {
        // Given
        JsonNode objectGroups = new RequestResponseOK<JsonNode>().toJsonNode();
        when(metaDataClient.selectObjectGroups(any())).thenReturn(objectGroups);

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkObjectAttachmentsToExistingObjectGroups(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.OK);
        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(1)).selectObjectGroups(queryCaptor.capture());
        checkQueryEquals(
            queryCaptor.getAllValues().get(0),
            "IngestCleanup/Eligibility/selectAttachmentToObjectGroupsQuery.json"
        );
    }

    @Test
    public void givenAttachmentsThenCheckObjectAttachmentsToExistingObjectGroupsKO() throws Exception {
        // Given
        JsonNode objectGroups = new RequestResponseOK<JsonNode>()
            .addAllResults(
                Collections.singletonList(JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), "og_updated_1"))
            )
            .toJsonNode();
        when(metaDataClient.selectObjectGroups(any())).thenReturn(objectGroups);

        CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(INGEST_OPERATION_ID);

        // When
        instance.checkObjectAttachmentsToExistingObjectGroups(INGEST_OPERATION_ID, cleanupReportManager);

        // Then
        assertThat(cleanupReportManager.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(cleanupReportManager.getCleanupReport().getObjectGroups()).containsOnlyKeys("og_updated_1");
        assertThat(
            cleanupReportManager.getCleanupReport().getObjectGroups().get("og_updated_1").getErrors()
        ).isNotEmpty();
        assertThat(
            cleanupReportManager.getCleanupReport().getObjectGroups().get("og_updated_1").getWarnings()
        ).isNull();

        ArgumentCaptor<JsonNode> queryCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metaDataClient, times(1)).selectObjectGroups(queryCaptor.capture());
        checkQueryEquals(
            queryCaptor.getAllValues().get(0),
            "IngestCleanup/Eligibility/selectAttachmentToObjectGroupsQuery.json"
        );
    }

    private void checkQueryEquals(JsonNode expectedJsonNode, String resourceFile)
        throws InvalidParseOperationException, FileNotFoundException {
        JsonAssert.assertJsonEquals(
            expectedJsonNode,
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(resourceFile))
        );
    }
}
