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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.ReassignmentObjectGroupReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
import fr.gouv.vitam.common.jsonl.JsonLineWriter;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.worker.core.plugin.reassignment.OriginatingAgencyReassignmentPreparationPlugin.OBJECT_GROUPS_TO_UPDATE_JSONL_FILE;
import static fr.gouv.vitam.worker.core.plugin.reassignment.OriginatingAgencyReassignmentPreparationPlugin.UNITS_TO_UPDATE_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OriginatingAgencyReassignmentPreparationPluginTest {

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

    private final HandlerIO handlerIO = mock(HandlerIO.class);

    private final Map<String, File> writtenFiles = new HashMap<>();

    private OriginatingAgencyReassignmentPreparationPlugin originatingAgencyReassignmentPreparationPlugin;

    private static final TypeReference<JsonLineModel> jsonLineModelTypeReference = new TypeReference<>() {};

    @Before
    public void setUp() throws Exception {
        when(handlerIO.getMetaDataClient()).thenReturn(metaDataClient);
        when(handlerIO.getWorkspaceClient(any())).thenReturn(workspaceClient);
        when(handlerIO.getAdminManagementClient()).thenReturn(adminManagementClient);
        when(handlerIO.getBatchReportClient()).thenReturn(batchReportClient);
        when(handlerIO.getContainerName()).thenReturn("processId");

        // Simulate & record files being copied to/from workspace
        doAnswer(args -> {
            String path = args.getArgument(0);
            File file = args.getArgument(1);

            File copy = temporaryFolder.newFile();
            FileUtils.copyFile(file, copy);
            writtenFiles.put(path, copy);

            return null;
        })
            .when(handlerIO)
            .transferFileToWorkspace(any(), any(), anyBoolean(), anyBoolean());

        when(handlerIO.getNewLocalFile(any(), anyString())).thenAnswer(args -> {
            String name = args.getArgument(1);
            return new File(temporaryFolder.getRoot(), name);
        });

        when(handlerIO.getFileFromWorkspace(any(), anyString())).thenAnswer(args -> {
            String path = args.getArgument(1);

            File copy = temporaryFolder.newFile();
            FileUtils.copyFile(writtenFiles.get(path), copy);
            return copy;
        });

        originatingAgencyReassignmentPreparationPlugin = new OriginatingAgencyReassignmentPreparationPlugin();
    }

    @Test
    public void should_generate_distribution_files_ok() throws Exception {
        // Given
        JsonNode queryNode = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/query.json")
        );

        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest =
            new OriginatingAgencyReassignmentRequest(
                queryNode,
                "sourceOriginatingAgency",
                "targetOriginatingAgency",
                true
            );

        WorkerParameters workerParameters = mock(WorkerParameters.class);

        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/units.json")
        );

        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse);

        JsonNode ogResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/objectGroups.json")
        );

        when(metaDataClient.selectObjectGroups(any())).thenReturn(ogResponse);

        AgenciesModel targetOriginatingAgenciesModel = new AgenciesModel();
        targetOriginatingAgenciesModel.setId(originatingAgencyReassignmentRequest.getTargetOriginatingAgency());
        targetOriginatingAgenciesModel.setIdentifier(originatingAgencyReassignmentRequest.getTargetOriginatingAgency());

        when(adminManagementClient.getAgencyById(anyString())).thenReturn(
            new RequestResponseOK<AgenciesModel>().addResult(targetOriginatingAgenciesModel)
        );

        doReturn(JsonHandler.writeToInpustream(originatingAgencyReassignmentRequest))
            .when(handlerIO)
            .getInputStreamFromWorkspace(any(), eq("request.json"));

        // Simulate batch report
        List<ReassignmentObjectGroupReportEntry> batchReportEntries = new ArrayList<>();

        doAnswer(args -> {
            ReportBody<ReassignmentObjectGroupReportEntry> reportBody = args.getArgument(0);
            assertThat(reportBody.getProcessId()).isEqualTo("processId");
            assertThat(reportBody.getReportType()).isEqualTo(ReportType.REASSIGNMENT_OBJECT_GROUPS);

            batchReportEntries.addAll(reportBody.getEntries());
            return null;
        })
            .when(batchReportClient)
            .appendReportEntries(any());

        doAnswer(args -> {
            File ogDistributionFile = temporaryFolder.newFile();
            try (
                JsonLineWriter<JsonLineModel> ogWriter = new JsonLineWriter<>(new FileOutputStream(ogDistributionFile))
            ) {
                for (ReassignmentObjectGroupReportEntry entry : batchReportEntries) {
                    ogWriter.addEntry(
                        new JsonLineModel(
                            entry.getObjectGroupId(),
                            null,
                            JsonHandler.createObjectNode().put("#opi", entry.getOpi())
                        )
                    );
                }
            }

            writtenFiles.put(OBJECT_GROUPS_TO_UPDATE_JSONL_FILE, ogDistributionFile);

            return null;
        })
            .when(batchReportClient)
            .exportReassignmentObjectGroups(any(), any(), any());

        // When
        ItemStatus itemStatus = originatingAgencyReassignmentPreparationPlugin.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        JsonLineIterator<JsonLineModel> unitDistributionLines = new JsonLineIterator<>(
            new FileInputStream(writtenFiles.get(UNITS_TO_UPDATE_FILE)),
            jsonLineModelTypeReference
        );

        assertThat(unitDistributionLines.stream())
            .extracting(
                JsonLineModel::getId,
                JsonLineModel::getDistribGroup,
                jsonLineModel -> jsonLineModel.getParams().get(VitamFieldsHelper.originatingAgency()).asText()
            )
            .containsExactlyInAnyOrder(
                tuple("id_unit_1", 1, "sourceOriginatingAgency"),
                tuple("id_unit_2", 1, "sourceOriginatingAgency"),
                tuple("id_unit_3", 2, "sourceOriginatingAgency"),
                tuple("id_unit_5", 3, "sourceOriginatingAgency")
            );

        JsonLineIterator<JsonLineModel> ogDistributionLines = new JsonLineIterator<>(
            new FileInputStream(writtenFiles.get(OBJECT_GROUPS_TO_UPDATE_JSONL_FILE)),
            jsonLineModelTypeReference
        );

        assertThat(ogDistributionLines.stream())
            .extracting(
                JsonLineModel::getId,
                JsonLineModel::getDistribGroup,
                jsonLineModel -> jsonLineModel.getParams().get(VitamFieldsHelper.initialOperation()).asText()
            )
            .containsExactlyInAnyOrder(tuple("id_og_1", null, "opi_1"), tuple("id_og_2", null, "opi_2"));
    }

    @Test
    public void should_generate_distribution_files_with_no_eligible_object_groups() throws Exception {
        // Given
        JsonNode queryNode = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/query.json")
        );

        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest =
            new OriginatingAgencyReassignmentRequest(
                queryNode,
                "sourceOriginatingAgency",
                "targetOriginatingAgency",
                true
            );

        WorkerParameters workerParameters = mock(WorkerParameters.class);

        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/units.json")
        );

        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse);

        JsonNode ogResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/objectGroups_empty.json")
        );

        when(metaDataClient.selectObjectGroups(any())).thenReturn(ogResponse);

        AgenciesModel targetOriginatingAgenciesModel = new AgenciesModel();
        targetOriginatingAgenciesModel.setId(originatingAgencyReassignmentRequest.getTargetOriginatingAgency());
        targetOriginatingAgenciesModel.setIdentifier(originatingAgencyReassignmentRequest.getTargetOriginatingAgency());

        when(adminManagementClient.getAgencyById(anyString())).thenReturn(
            new RequestResponseOK<AgenciesModel>().addResult(targetOriginatingAgenciesModel)
        );

        doReturn(JsonHandler.writeToInpustream(originatingAgencyReassignmentRequest))
            .when(handlerIO)
            .getInputStreamFromWorkspace(any(), eq("request.json"));

        // Simulate batch report
        List<ReassignmentObjectGroupReportEntry> batchReportEntries = new ArrayList<>();

        doAnswer(args -> {
            ReportBody<ReassignmentObjectGroupReportEntry> reportBody = args.getArgument(0);
            assertThat(reportBody.getProcessId()).isEqualTo("processId");
            assertThat(reportBody.getReportType()).isEqualTo(ReportType.REASSIGNMENT_OBJECT_GROUPS);

            batchReportEntries.addAll(reportBody.getEntries());
            return null;
        })
            .when(batchReportClient)
            .appendReportEntries(any());

        doAnswer(args -> {
            File ogDistributionFile = temporaryFolder.newFile();
            try (
                JsonLineWriter<JsonLineModel> ogWriter = new JsonLineWriter<>(new FileOutputStream(ogDistributionFile))
            ) {
                for (ReassignmentObjectGroupReportEntry entry : batchReportEntries) {
                    ogWriter.addEntry(
                        new JsonLineModel(
                            entry.getObjectGroupId(),
                            null,
                            JsonHandler.createObjectNode().put("#opi", entry.getOpi())
                        )
                    );
                }
            }

            writtenFiles.put(OBJECT_GROUPS_TO_UPDATE_JSONL_FILE, ogDistributionFile);

            return null;
        })
            .when(batchReportClient)
            .exportReassignmentObjectGroups(any(), any(), any());

        // When
        ItemStatus itemStatus = originatingAgencyReassignmentPreparationPlugin.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        JsonLineIterator<JsonLineModel> unitDistributionLines = new JsonLineIterator<>(
            new FileInputStream(writtenFiles.get(UNITS_TO_UPDATE_FILE)),
            jsonLineModelTypeReference
        );

        assertThat(unitDistributionLines.stream())
            .extracting(
                JsonLineModel::getId,
                JsonLineModel::getDistribGroup,
                jsonLineModel -> jsonLineModel.getParams().get(VitamFieldsHelper.originatingAgency()).asText()
            )
            .containsExactlyInAnyOrder(
                tuple("id_unit_1", 1, "sourceOriginatingAgency"),
                tuple("id_unit_2", 1, "sourceOriginatingAgency"),
                tuple("id_unit_3", 2, "sourceOriginatingAgency"),
                tuple("id_unit_5", 3, "sourceOriginatingAgency")
            );

        assertThat(writtenFiles.get(OBJECT_GROUPS_TO_UPDATE_JSONL_FILE)).isEmpty();
    }

    @Test
    public void should_generate_distribution_files_no_object_groups() throws Exception {
        // Given

        JsonNode queryNode = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/query.json")
        );

        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest =
            new OriginatingAgencyReassignmentRequest(
                queryNode,
                "sourceOriginatingAgency",
                "targetOriginatingAgency",
                true
            );

        WorkerParameters workerParameters = mock(WorkerParameters.class);

        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/units_no_object_groups.json")
        );

        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse);

        AgenciesModel targetOriginatingAgenciesModel = new AgenciesModel();
        targetOriginatingAgenciesModel.setId(originatingAgencyReassignmentRequest.getTargetOriginatingAgency());
        targetOriginatingAgenciesModel.setIdentifier(originatingAgencyReassignmentRequest.getTargetOriginatingAgency());

        when(adminManagementClient.getAgencyById(anyString())).thenReturn(
            new RequestResponseOK<AgenciesModel>().addResult(targetOriginatingAgenciesModel)
        );

        doReturn(JsonHandler.writeToInpustream(originatingAgencyReassignmentRequest))
            .when(handlerIO)
            .getInputStreamFromWorkspace(any(), eq("request.json"));

        // Simulate batch report
        List<ReassignmentObjectGroupReportEntry> batchReportEntries = new ArrayList<>();

        doAnswer(args -> {
            ReportBody<ReassignmentObjectGroupReportEntry> reportBody = args.getArgument(0);
            assertThat(reportBody.getProcessId()).isEqualTo("processId");
            assertThat(reportBody.getReportType()).isEqualTo(ReportType.REASSIGNMENT_OBJECT_GROUPS);

            batchReportEntries.addAll(reportBody.getEntries());
            return null;
        })
            .when(batchReportClient)
            .appendReportEntries(any());

        doAnswer(args -> {
            File ogDistributionFile = temporaryFolder.newFile();
            try (
                JsonLineWriter<JsonLineModel> ogWriter = new JsonLineWriter<>(new FileOutputStream(ogDistributionFile))
            ) {
                for (ReassignmentObjectGroupReportEntry entry : batchReportEntries) {
                    ogWriter.addEntry(
                        new JsonLineModel(
                            entry.getObjectGroupId(),
                            null,
                            JsonHandler.createObjectNode().put("#opi", entry.getOpi())
                        )
                    );
                }
            }

            writtenFiles.put(OBJECT_GROUPS_TO_UPDATE_JSONL_FILE, ogDistributionFile);

            return null;
        })
            .when(batchReportClient)
            .exportReassignmentObjectGroups(any(), any(), any());

        // When
        ItemStatus itemStatus = originatingAgencyReassignmentPreparationPlugin.execute(workerParameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        JsonLineIterator<JsonLineModel> unitDistributionLines = new JsonLineIterator<>(
            new FileInputStream(writtenFiles.get(UNITS_TO_UPDATE_FILE)),
            jsonLineModelTypeReference
        );

        assertThat(unitDistributionLines.stream())
            .extracting(
                JsonLineModel::getId,
                JsonLineModel::getDistribGroup,
                jsonLineModel -> jsonLineModel.getParams().get(VitamFieldsHelper.originatingAgency()).asText()
            )
            .containsExactlyInAnyOrder(
                tuple("id_unit_1", 1, "sourceOriginatingAgency"),
                tuple("id_unit_2", 1, "sourceOriginatingAgency"),
                tuple("id_unit_3", 2, "sourceOriginatingAgency"),
                tuple("id_unit_5", 3, "sourceOriginatingAgency")
            );

        assertThat(writtenFiles.get(OBJECT_GROUPS_TO_UPDATE_JSONL_FILE)).isEmpty();
        verify(metaDataClient, never()).selectObjectGroups(any());
    }

    @Test
    public void should_ko_when_calling_with__originating_agency_source_and_target_same() throws Exception {
        // Given

        JsonNode queryNode = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/query.json")
        );

        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest =
            new OriginatingAgencyReassignmentRequest(queryNode, "sameOriginatingAgency", "sameOriginatingAgency", true);

        WorkerParameters workerParameters = mock(WorkerParameters.class);
        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/units.json")
        );
        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse);

        AgenciesModel targetOriginatingAgenciesModel = new AgenciesModel();
        targetOriginatingAgenciesModel.setId(originatingAgencyReassignmentRequest.getTargetOriginatingAgency());
        targetOriginatingAgenciesModel.setIdentifier(originatingAgencyReassignmentRequest.getTargetOriginatingAgency());

        when(adminManagementClient.getAgencyById(anyString())).thenReturn(
            new RequestResponseOK<AgenciesModel>().addResult(targetOriginatingAgenciesModel)
        );

        doReturn(JsonHandler.writeToInpustream(originatingAgencyReassignmentRequest))
            .when(handlerIO)
            .getInputStreamFromWorkspace(any(), eq("request.json"));

        // When
        ItemStatus itemStatus = originatingAgencyReassignmentPreparationPlugin.execute(workerParameters, handlerIO);

        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.KO);
    }

    @Test
    public void should_ko_when_calling_with_not_found_target_originating_agency() throws Exception {
        // Given

        JsonNode queryNode = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/query.json")
        );

        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest =
            new OriginatingAgencyReassignmentRequest(
                queryNode,
                "sourceOriginatingAgency",
                "targetOriginatingAgency",
                true
            );

        WorkerParameters workerParameters = mock(WorkerParameters.class);
        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/units.json")
        );
        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse);

        when(adminManagementClient.getAgencyById(anyString())).thenThrow(
            new ReferentialNotFoundException("not found referential originating agency")
        );

        doReturn(JsonHandler.writeToInpustream(originatingAgencyReassignmentRequest))
            .when(handlerIO)
            .getInputStreamFromWorkspace(any(), eq("request.json"));

        // When
        ItemStatus itemStatus = originatingAgencyReassignmentPreparationPlugin.execute(workerParameters, handlerIO);
        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.KO);
    }
}
