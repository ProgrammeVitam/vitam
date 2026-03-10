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
package fr.gouv.vitam.worker.core.plugin.reassignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.io.TempWorkspace;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OriginatingAgencyReassignmentUnitsChildrenPreparationPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private BatchReportClient batchReportClient;

    HandlerIO handlerIO = mock(HandlerIO.class);

    private TempWorkspace tempWorkspace;

    private OriginatingAgencyReassignmentUnitsChildrenPreparationPlugin originatingAgencyReassignmentUnitsChildrenPreparationPlugin;

    private static final String UNITS_TO_UPDATE_FILE_NAME = "units_to_update.jsonl";
    private static final String UNITS_CHILDREN_FILE_NAME = "unitsChildrenToUpdateSps.jsonl";
    private static final String INTERMEDIATE_UNITS_IDS_FILE_NAME = "intermediate_units_ids.jsonl";

    @Before
    public void setUp() throws Exception {
        when(handlerIO.getMetaDataClient()).thenReturn(metaDataClient);
        when(handlerIO.getWorkspaceClient(any())).thenReturn(workspaceClient);
        when(handlerIO.getAdminManagementClient()).thenReturn(adminManagementClient);
        when(handlerIO.getBatchReportClient()).thenReturn(batchReportClient);

        originatingAgencyReassignmentUnitsChildrenPreparationPlugin =
            new OriginatingAgencyReassignmentUnitsChildrenPreparationPlugin();

        tempWorkspace = new TempWorkspace();
    }

    @After
    public void cleanup() {
        // Restore default batch size
        VitamConfiguration.setBatchSize(1000);
    }

    @Test
    public void should_generate_children_distribution_file() throws Exception {
        // Given

        File main_units_distributionFile = PropertiesUtils.getResourceFile(
            "reassignment/main_units_distribution.jsonl"
        );

        doReturn(main_units_distributionFile)
            .when(handlerIO)
            .getFileFromWorkspace(any(), eq(UNITS_TO_UPDATE_FILE_NAME));

        JsonNode childrenUnitsResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/children_units.json")
        );

        JsonNode objectGroupResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/objectGroups.json")
        );

        ArrayNode results = (ArrayNode) childrenUnitsResponse.get("$results");
        List<String> unitToUpdateSps = new ArrayList<>();

        for (JsonNode unitNode : results) {
            unitToUpdateSps.add(unitNode.get(VitamFieldsHelper.id()).asText());
        }

        when(metaDataClient.selectUnits(any(JsonNode.class))).thenReturn(childrenUnitsResponse);
        when(metaDataClient.selectObjectGroups(any())).thenReturn(objectGroupResponse);

        when(workspaceClient.isExistingObject(anyString(), eq(UNITS_CHILDREN_FILE_NAME))).thenReturn(false);

        when(handlerIO.getContainerName()).thenReturn("processId");
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        when(workerParameters.getExecutionContext()).thenReturn(WorkFlowExecutionContext.VITAM);

        ArgumentCaptor<ReportBody> reportBodyArgumentCaptor = ArgumentCaptor.forClass(ReportBody.class);

        doNothing().when(batchReportClient).appendReportEntries(any());

        // Create 2 separate local files
        File unitsDistributionFileTempFile = temporaryFolder.newFile("units_to_update.jsonl");
        File gotsDistributionFileTempFile = temporaryFolder.newFile("intermediate_gots_ids.jsonl");

        when(handlerIO.getNewLocalFile(any(), anyString()))
            .thenReturn(unitsDistributionFileTempFile) // 1st call
            .thenReturn(gotsDistributionFileTempFile); // 2nd call

        // When
        originatingAgencyReassignmentUnitsChildrenPreparationPlugin.execute(workerParameters, handlerIO);

        // Then
        verify(batchReportClient, times(2)).appendReportEntries(reportBodyArgumentCaptor.capture());

        List<ReportBody> reportBodies = reportBodyArgumentCaptor.getAllValues();

        ReportBody firstReportBody = reportBodies.get(0);
        ReportBody secondReportBody = reportBodies.get(1);

        assertThat(firstReportBody.getProcessId()).isEqualTo("processId");
        assertThat(firstReportBody.getReportType()).isEqualTo(
            ReportType.REASSIGNMENT_UNITS_ORIGINATING_AGENCIES_COMPUTE
        );

        assertThat(secondReportBody.getProcessId()).isEqualTo("processId");
        assertThat(secondReportBody.getReportType()).isEqualTo(
            ReportType.REASSIGNMENT_OBJECT_GROUPS_ORIGINATING_AGENCIES_COMPUTE
        );

        assertThat(firstReportBody.getEntries()).extracting("unitId").containsAll(unitToUpdateSps);
        verify((batchReportClient)).exportUnitsToComputeOriginatingAgencies(anyString(), any(), any());
    }
}
