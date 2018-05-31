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

package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ListUnitLifecycleTraceabilityActionHandlerTest {

    private HandlerIOImpl handlerIO;
    private GUID guid = GUIDFactory.newGUID();
    private List<IOParameter> out;
    private static final Integer TENANT_ID = 0;

    private static final String HANDLER_ID = "PREPARE_UNIT_LFC_TRACEABILITY";

    private static final String LFC_UNITS_BIG_1_JSON =
        "ListUnitLifecycleTraceabilityActionHandler/lfc_units_big_part1.json";
    private static final String LFC_UNITS_BIG_2_JSON =
        "ListUnitLifecycleTraceabilityActionHandler/lfc_units_big_part2.json";

    private static final String LFC_UNITS_JSON = "ListUnitLifecycleTraceabilityActionHandler/lfc_units.json";
    private static final String LAST_TRACEABILITY_JSON =
        "ListUnitLifecycleTraceabilityActionHandler/lastTraceability.json";

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private LogbookOperationsClient logbookOperationsClient;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    public ListUnitLifecycleTraceabilityActionHandlerTest() throws FileNotFoundException {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);

        when(logbookLifeCyclesClientFactory.getClient())
            .thenReturn(logbookLifeCyclesClient);

        logbookOperationsClient = mock(LogbookOperationsClient.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        when(logbookOperationsClientFactory.getClient())
            .thenReturn(logbookOperationsClient);

        handlerIO = new HandlerIOImpl(workspaceClient, "ListUnitLifecycleTraceabilityActionHandlerTest", "workerId");
        // mock later ?
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE,
            "Operations/lastOperation.json")));
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE,
            "Operations/traceabilityInformation.json")));
    }

    @Test
    @RunWithCustomExecutor
    public void givenMultipleLifecyclesWhenExecuteAndNotMaxEntriesReachedThenReturnResponseOK() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertEquals(ListUnitLifecycleTraceabilityActionHandler.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final List<JsonNode> unitsLFC = IteratorUtils.toList(JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LFC_UNITS_JSON)).elements());
        final JsonNode lastTraceability =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON));
        reset(logbookLifeCyclesClient);
        when(logbookLifeCyclesClient.getRawUnitLifecyclesByLastPersistedDate(any(), any(), anyInt()))
            .thenReturn(unitsLFC);
        reset(logbookOperationsClient);
        when(logbookOperationsClient.selectOperation(anyObject())).thenReturn(lastTraceability);

        saveWorkspacePutObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdtfqaaaaq.json");
        saveWorkspacePutObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdtgaaaaba.json");
        saveWorkspacePutObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdsvqaaabq.json");

        saveWorkspacePutObject("Operations/lastOperation.json");
        saveWorkspacePutObject("Operations/traceabilityInformation.json");

        int temporizationDelayInSeconds = 300;
        WorkerParameters params = createExecParams(temporizationDelayInSeconds, 100000);
        ListUnitLifecycleTraceabilityActionHandler handler = new ListUnitLifecycleTraceabilityActionHandler(
            logbookLifeCyclesClientFactory, logbookOperationsClientFactory, 10);

        // When
        LocalDateTime snapshot1 = LocalDateUtil.now();
        final ItemStatus response = handler.execute(params, handlerIO);
        LocalDateTime snapshot2 = LocalDateUtil.now();

        // Than
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        assertNotNull(getSavedWorkspaceObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdtfqaaaaq.json"));
        assertNotNull(getSavedWorkspaceObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdtgaaaaba.json"));
        assertNotNull(getSavedWorkspaceObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdsvqaaabq.json"));

        assertNotNull(getSavedWorkspaceObject("Operations/lastOperation.json"));
        JsonNode traceabilityInformation = getSavedWorkspaceObject("Operations/traceabilityInformation.json");
        assertNotNull(traceabilityInformation);
        assertThat(traceabilityInformation.get("nbEntries").asLong()).isEqualTo(3);
        assertThat(traceabilityInformation.get("startDate").asText()).isEqualTo("2018-05-16T12:32:30.051");
        assertThat(LocalDateUtil.parseMongoFormattedDate(traceabilityInformation.get("endDate").asText()))
            .isAfterOrEqualTo(snapshot1.minusSeconds(temporizationDelayInSeconds))
            .isBeforeOrEqualTo(snapshot2.minusSeconds(temporizationDelayInSeconds));
        assertThat(traceabilityInformation.get("maxEntriesReached").asBoolean()).isFalse();

        assertEquals(3, getNumberOfSavedObjectInWorkspace(UpdateWorkflowConstants.UNITS_FOLDER));
        assertEquals(2, getNumberOfSavedObjectInWorkspace("Operations"));
    }

    @Test
    @RunWithCustomExecutor
    public void givenMultipleLifecyclesWhenExecuteAndMaxEntriesReachedThenReturnResponseOK() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertEquals(ListUnitLifecycleTraceabilityActionHandler.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final List<JsonNode> unitsLFC = IteratorUtils.toList(JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LFC_UNITS_JSON)).elements());
        final JsonNode lastTraceability =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON));
        reset(logbookLifeCyclesClient);
        when(logbookLifeCyclesClient.getRawUnitLifecyclesByLastPersistedDate(any(), any(), anyInt()))
            .thenReturn(unitsLFC);
        reset(logbookOperationsClient);
        when(logbookOperationsClient.selectOperation(anyObject())).thenReturn(lastTraceability);

        saveWorkspacePutObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdtfqaaaaq.json");
        saveWorkspacePutObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdtgaaaaba.json");
        saveWorkspacePutObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdsvqaaabq.json");

        saveWorkspacePutObject("Operations/lastOperation.json");
        saveWorkspacePutObject("Operations/traceabilityInformation.json");

        int temporizationDelayInSeconds = 300;
        int maxEntries = 3;
        WorkerParameters params = createExecParams(temporizationDelayInSeconds, maxEntries);
        ListUnitLifecycleTraceabilityActionHandler handler = new ListUnitLifecycleTraceabilityActionHandler(
            logbookLifeCyclesClientFactory, logbookOperationsClientFactory, 10);

        // When
        final ItemStatus response = handler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        assertNotNull(getSavedWorkspaceObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdtfqaaaaq.json"));
        assertNotNull(getSavedWorkspaceObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdtgaaaaba.json"));
        assertNotNull(getSavedWorkspaceObject(
            UpdateWorkflowConstants.UNITS_FOLDER + "/aeaqaaaaaag457juaan3yaldndxdsvqaaabq.json"));

        assertNotNull(getSavedWorkspaceObject("Operations/lastOperation.json"));
        JsonNode traceabilityInformation = getSavedWorkspaceObject("Operations/traceabilityInformation.json");
        assertNotNull(traceabilityInformation);
        assertThat(traceabilityInformation.get("nbEntries").asLong()).isEqualTo(3);
        assertThat(traceabilityInformation.get("startDate").asText()).isEqualTo("2018-05-16T12:32:30.051");
        assertThat(traceabilityInformation.get("endDate").asText()).isEqualTo("2018-05-16T12:33:41.431");
        assertThat(traceabilityInformation.get("maxEntriesReached").asBoolean()).isTrue();

        assertEquals(3, getNumberOfSavedObjectInWorkspace(UpdateWorkflowConstants.UNITS_FOLDER));
        assertEquals(2, getNumberOfSavedObjectInWorkspace("Operations"));
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLFCErrorWhenExecuteThenReturnResponseKO() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        handlerIO.addOutIOParameters(out);

        reset(logbookOperationsClient);
        final JsonNode lastTraceability =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON));
        when(logbookOperationsClient.selectOperation(anyObject())).thenReturn(lastTraceability);

        reset(logbookLifeCyclesClient);
        when(logbookLifeCyclesClient.getRawUnitLifecyclesByLastPersistedDate(any(), any(), anyInt()))
            .thenThrow(new InvalidParseOperationException("InvalidParseOperationException"));

        WorkerParameters params = createExecParams(300, 100000);
        ListUnitLifecycleTraceabilityActionHandler handler = new ListUnitLifecycleTraceabilityActionHandler(
            logbookLifeCyclesClientFactory, logbookOperationsClientFactory, 10);

        // When
        final ItemStatus response = handler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenWorkspaceInErrorWhenExecuteThenReturnResponseKO() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        handlerIO.addOutIOParameters(out);

        final List<JsonNode> unitsLFC = IteratorUtils.toList(JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LFC_UNITS_JSON)).elements());
        final JsonNode lastTraceability =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON));
        reset(logbookLifeCyclesClient);
        when(logbookLifeCyclesClient.getRawUnitLifecyclesByLastPersistedDate(any(), any(), anyInt()))
            .thenReturn(unitsLFC);
        reset(logbookOperationsClient);
        when(logbookOperationsClient.selectOperation(anyObject())).thenReturn(lastTraceability);

        doThrow(new ContentAddressableStorageServerException("ContentAddressableStorageServerException"))
            .when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        WorkerParameters params = createExecParams(300, 100000);
        ListUnitLifecycleTraceabilityActionHandler handler = new ListUnitLifecycleTraceabilityActionHandler(
            logbookLifeCyclesClientFactory, logbookOperationsClientFactory, 10);

        // When
        final ItemStatus response = handler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenLogbookExceptionWhenExecuteThenReturnResponseFATAL() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        handlerIO.addOutIOParameters(out);
        reset(logbookLifeCyclesClient);
        when(logbookLifeCyclesClient.getRawUnitLifecyclesByLastPersistedDate(any(), any(), anyInt()))
            .thenThrow(new LogbookClientException("LogbookClientException"));
        final JsonNode lastTraceability =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON));
        reset(logbookOperationsClient);
        when(logbookOperationsClient.selectOperation(anyObject())).thenReturn(lastTraceability);

        WorkerParameters params = createExecParams(300, 100000);
        ListUnitLifecycleTraceabilityActionHandler handler = new ListUnitLifecycleTraceabilityActionHandler(
            logbookLifeCyclesClientFactory, logbookOperationsClientFactory, 10);

        // When
        final ItemStatus response = handler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenMultipleLifecyclesWhenExecuteWithMultiSelectQueriesThenReturnResponseOK() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertEquals(ListUnitLifecycleTraceabilityActionHandler.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final List<JsonNode> unitsLFC1 = IteratorUtils.toList(JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LFC_UNITS_BIG_1_JSON)).elements());
        final List<JsonNode> unitsLFC2 = IteratorUtils.toList(JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LFC_UNITS_BIG_2_JSON)).elements());

        final JsonNode lastTraceability =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON));
        reset(logbookLifeCyclesClient);

        when(logbookLifeCyclesClient.getRawUnitLifecyclesByLastPersistedDate(
            eq(LocalDateUtil.parseMongoFormattedDate("2018-05-16T12:32:30.051")), any(), eq(10)))
            .thenReturn(unitsLFC1);
        when(logbookLifeCyclesClient.getRawUnitLifecyclesByLastPersistedDate(
            eq(LocalDateUtil.parseMongoFormattedDate("2018-05-20T00:00:00.010")), any(), eq(10)))
            .thenReturn(unitsLFC2);
        reset(logbookOperationsClient);
        when(logbookOperationsClient.selectOperation(anyObject())).thenReturn(lastTraceability);

        for (JsonNode jsonNode : ListUtils.union(unitsLFC1, unitsLFC2)) {
            saveWorkspacePutObject(
                UpdateWorkflowConstants.UNITS_FOLDER + "/" + jsonNode.get("_id").asText() + ".json");
        }
        saveWorkspacePutObject("Operations/lastOperation.json");
        saveWorkspacePutObject("Operations/traceabilityInformation.json");

        int temporizationDelayInSeconds = 300;
        WorkerParameters params = createExecParams(temporizationDelayInSeconds, 100000);
        ListUnitLifecycleTraceabilityActionHandler handler = new ListUnitLifecycleTraceabilityActionHandler(
            logbookLifeCyclesClientFactory, logbookOperationsClientFactory, 10);

        // When
        LocalDateTime snapshot1 = LocalDateUtil.now();
        ItemStatus response = handler.execute(params, handlerIO);
        LocalDateTime snapshot2 = LocalDateUtil.now();

        // Than
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        for (JsonNode jsonNode : ListUtils.union(unitsLFC1, unitsLFC2)) {
            assertNotNull(getSavedWorkspaceObject(
                UpdateWorkflowConstants.UNITS_FOLDER + "/" + jsonNode.get("_id").asText() + ".json"));
        }
        assertNotNull(getSavedWorkspaceObject("Operations/lastOperation.json"));
        JsonNode traceabilityInformation = getSavedWorkspaceObject("Operations/traceabilityInformation.json");
        assertNotNull(traceabilityInformation);
        assertThat(traceabilityInformation.get("nbEntries").asLong()).isEqualTo(13);
        assertThat(traceabilityInformation.get("startDate").asText()).isEqualTo("2018-05-16T12:32:30.051");
        assertThat(LocalDateUtil.parseMongoFormattedDate(traceabilityInformation.get("endDate").asText()))
            .isAfterOrEqualTo(snapshot1.minusSeconds(temporizationDelayInSeconds))
            .isBeforeOrEqualTo(snapshot2.minusSeconds(temporizationDelayInSeconds));
        assertThat(traceabilityInformation.get("maxEntriesReached").asBoolean()).isFalse();
        assertEquals(13, getNumberOfSavedObjectInWorkspace(UpdateWorkflowConstants.UNITS_FOLDER));
        assertEquals(2, getNumberOfSavedObjectInWorkspace("Operations"));
    }

    private WorkerParameters createExecParams(int temporizationDelayInSeconds, int maxEntries) {
        return WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("objectName.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.TRACEABILITY)
            .putParameterValue(WorkerParameterName.lifecycleTraceabilityTemporizationDelayInSeconds,
                Integer.toString(temporizationDelayInSeconds))
            .putParameterValue(WorkerParameterName.lifecycleTraceabilityMaxEntries, Integer.toString(maxEntries));
    }

    private void saveWorkspacePutObject(String filename) throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgumentAt(2, InputStream.class);
            java.nio.file.Path file =
                java.nio.file.Paths
                    .get(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
                        handlerIO.getWorkerId() + "/" + filename.replaceAll("/", "_"));
            java.nio.file.Files.copy(inputStream, file);
            return null;
        }).when(workspaceClient).putObject(org.mockito.Matchers.anyString(),
            org.mockito.Matchers.eq(filename), org.mockito.Matchers.any(InputStream.class));
    }

    private JsonNode getSavedWorkspaceObject(String filename) throws InvalidParseOperationException {
        File objectNameFile =
            new File(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
                handlerIO.getWorkerId() + "/" + filename.replaceAll("/", "_"));
        return JsonHandler.getFromFile(objectNameFile);
    }

    private int getNumberOfSavedObjectInWorkspace(String filter) {
        return new File(System.getProperty("vitam.tmp.folder") + "/" + handlerIO.getContainerName() + "_" +
            handlerIO.getWorkerId()).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().contains(filter.toLowerCase()) && name.toLowerCase().endsWith(".json");
            }
        }).length;
    }

}
