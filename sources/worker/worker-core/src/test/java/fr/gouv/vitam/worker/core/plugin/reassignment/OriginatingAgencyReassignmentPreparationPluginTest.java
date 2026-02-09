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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

    HandlerIO handlerIO = mock(HandlerIO.class);

    private OriginatingAgencyReassignmentPreparationPlugin originatingAgencyReassignmentPreparationPlugin;

    private static final TypeReference<JsonLineModel> jsonLineModelTypeReference = new TypeReference<>() {};

    private static final String UNITS_TO_UPDATE_FILE = "units_to_update.jsonl";

    @Before
    public void setUp() throws Exception {
        when(handlerIO.getMetaDataClient()).thenReturn(metaDataClient);
        when(handlerIO.getWorkspaceClient(any())).thenReturn(workspaceClient);
        when(handlerIO.getAdminManagementClient()).thenReturn(adminManagementClient);

        originatingAgencyReassignmentPreparationPlugin = new OriginatingAgencyReassignmentPreparationPlugin();
    }

    @Test
    public void should_generate_distribution_file_with_requested_ids() throws Exception {
        // Given
        File distributionFile = temporaryFolder.newFile();

        JsonNode queryNode = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/query.json")
        );

        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest =
            new OriginatingAgencyReassignmentRequest();

        originatingAgencyReassignmentRequest.setDslRequest(queryNode);
        originatingAgencyReassignmentRequest.setSourceOriginatingAgency("sourceOriginatingAgency");
        originatingAgencyReassignmentRequest.setTargetOriginatingAgency("targetOriginatingAgency");

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

        when(handlerIO.getNewLocalFile(any(), anyString())).thenReturn(distributionFile);
        File resultFile = temporaryFolder.newFile();
        doAnswer(invocation -> {
            File distributionFileCaptured = invocation.getArgument(1);

            try (FileOutputStream fileOutputStream = new FileOutputStream(resultFile)) {
                Files.copy(distributionFileCaptured.toPath(), fileOutputStream);
            }
            return null;
        })
            .when(handlerIO)
            .transferFileToWorkspace(
                ArgumentMatchers.eq(UNITS_TO_UPDATE_FILE),
                any(),
                ArgumentMatchers.eq(true),
                ArgumentMatchers.eq(false)
            );
        // When
        ItemStatus itemStatus = originatingAgencyReassignmentPreparationPlugin.execute(workerParameters, handlerIO);
        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.OK);
        JsonLineIterator<JsonLineModel> lines = new JsonLineIterator<>(
            new FileInputStream(resultFile),
            jsonLineModelTypeReference
        );
        List<JsonLineModel> units = lines.stream().toList();
        Set<String> unitIds = new HashSet<>();
        Set<String> unitAgencies = new HashSet<>();
        Set<Integer> unitGroups = new HashSet<>();
        for (JsonLineModel unitLine : units) {
            unitIds.add(unitLine.getId());
            unitAgencies.add(unitLine.getParams().get(VitamFieldsHelper.originatingAgency()).asText());
            unitGroups.add(unitLine.getDistribGroup());
        }
        assertThat(unitIds).hasSize(4);
        assertThat(unitGroups).hasSize(3);
        assertThat(unitAgencies).hasSize(1);
        assertThat(unitIds).contains("id_unit_1", "id_unit_2", "id_unit_3", "id_unit_5");
        assertThat(unitGroups).contains(1, 2, 3);
        assertThat(unitAgencies).contains("sourceOriginatingAgency");
    }

    @Test
    public void should_ko_when_calling_with__originating_agency_source_and_target_same() throws Exception {
        // Given

        JsonNode queryNode = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("reassignment/query.json")
        );

        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest =
            new OriginatingAgencyReassignmentRequest();

        originatingAgencyReassignmentRequest.setDslRequest(queryNode);
        originatingAgencyReassignmentRequest.setSourceOriginatingAgency("sameOriginatingAgency");
        originatingAgencyReassignmentRequest.setTargetOriginatingAgency("sameOriginatingAgency");

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
            new OriginatingAgencyReassignmentRequest();

        originatingAgencyReassignmentRequest.setDslRequest(queryNode);
        originatingAgencyReassignmentRequest.setSourceOriginatingAgency("sourceOriginatingAgency");
        originatingAgencyReassignmentRequest.setTargetOriginatingAgency("targetOriginatingAgency");

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
