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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
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
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PrepareObjectGroupLfcTraceabilityActionPluginTest {

    private static final TypeReference<LfcMetadataPair> LFC_METADATA_PAIR_TYPE_REFERENCE = new TypeReference<>() {};
    private HandlerIOImpl handlerIO;
    private GUID guid = GUIDFactory.newGUID();
    private List<IOParameter> out;
    private List<IOParameter> in;
    private static final Integer TENANT_ID = 0;

    private static final String HANDLER_ID = "PREPARE_OG_LFC_TRACEABILITY";

    private static final String LFC_OBJECTS_BIG_JSON =
        "PrepareObjectGroupLfcTraceabilityActionPlugin/lfc_objects_big.jsonl";
    private static final String MD_GOTS_BIG_1_JSON =
        "PrepareObjectGroupLfcTraceabilityActionPlugin/md_gots_big_part1.json";
    private static final String MD_GOTS_BIG_2_JSON =
        "PrepareObjectGroupLfcTraceabilityActionPlugin/md_gots_big_part2.json";

    private static final String LFC_OBJECTS_JSONL = "PrepareObjectGroupLfcTraceabilityActionPlugin/lfc_objects.jsonl";
    private static final String LAST_TRACEABILITY_JSON =
        "PrepareObjectGroupLfcTraceabilityActionPlugin/lastTraceability.json";
    private static final String GOT_METADATA_JSON = "PrepareObjectGroupLfcTraceabilityActionPlugin/md_gots.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private MetaDataClient metadataClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private MetaDataClientFactory metadataClientFactory;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private File storedFilesDirectory;

    public PrepareObjectGroupLfcTraceabilityActionPluginTest() throws FileNotFoundException {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();
        storedFilesDirectory = folder.newFolder("storedFiles");

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(metadataClientFactory.getClient()).thenReturn(metadataClient);

        doAnswer(invocation -> {
            String filename = invocation.getArgument(1);
            InputStream inputStream = invocation.getArgument(2);
            Path filePath = Paths.get(storedFilesDirectory.getAbsolutePath(), filename);
            java.nio.file.Files.copy(inputStream, filePath);
            return null;
        })
            .when(workspaceClient)
            .putObject(
                org.mockito.ArgumentMatchers.anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.any(InputStream.class)
            );

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        logbookOperationsClient = mock(LogbookOperationsClient.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        String objectId = "objectId";
        handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "PrepareObjectGroupLfcTraceabilityActionPluginTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
        // mock later ?
        in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "lastOperation.json")));
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "traceabilityInformation.json")));
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "lfcWithMetadata.jsonl")));
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(storedFilesDirectory);
    }

    @Test
    @RunWithCustomExecutor
    public void givenMultipleLifecyclesWhenExecuteAndNotMaxEntriesReachedThenReturnResponseOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertEquals(PrepareObjectGroupLfcTraceabilityActionPlugin.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final JsonNode lastTraceability = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON)
        );
        reset(logbookLifeCyclesClient);
        doReturn(PropertiesUtils.getResourceAsStream(LFC_OBJECTS_JSONL))
            .when(logbookLifeCyclesClient)
            .exportRawObjectGroupLifecyclesByLastPersistedDate(any(), any(), anyInt());
        reset(logbookOperationsClient);
        doReturn(Response.ok(JsonHandler.writeToInpustream(lastTraceability)).build())
            .when(workspaceClient)
            .getObject(anyString(), eq("lastOperation.json"));
        handlerIO.addInIOParameters(in);

        RequestResponseOK<JsonNode> metadataResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(GOT_METADATA_JSON),
            RequestResponseOK.class,
            JsonNode.class
        );

        when(
            metadataClient.getObjectGroupsByIdsRaw(
                eq(
                    new HashSet<>(
                        Arrays.asList("aebaaaaaaag457juaap7qaldnejfprqaaaaq", "aebaaaaaaag457juaap7qaldnejfpsiaaaaq")
                    )
                )
            )
        ).thenReturn(metadataResponse);

        int temporizationDelayInSeconds = 300;
        WorkerParameters params = createExecParams(temporizationDelayInSeconds, 100000);
        PrepareObjectGroupLfcTraceabilityActionPlugin handler = new PrepareObjectGroupLfcTraceabilityActionPlugin(
            metadataClientFactory,
            logbookLifeCyclesClientFactory,
            10
        );

        // When
        LocalDateTime snapshot1 = LocalDateUtil.now();
        final ItemStatus response = handler.execute(params, handlerIO);
        LocalDateTime snapshot2 = LocalDateUtil.now();

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        File savedLfcWithMetadataFile = getSavedWorkspaceObjectFile("lfcWithMetadata.jsonl");
        try (
            JsonLineGenericIterator<LfcMetadataPair> jsonLineIterator = new JsonLineGenericIterator<>(
                new FileInputStream(savedLfcWithMetadataFile),
                LFC_METADATA_PAIR_TYPE_REFERENCE
            )
        ) {
            List<LfcMetadataPair> entries = IteratorUtils.toList(jsonLineIterator);

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getLfc().get("_id").textValue()).isEqualTo(
                "aebaaaaaaag457juaap7qaldnejfprqaaaaq"
            );
            assertThat(entries.get(0).getMetadata().get("_id").textValue()).isEqualTo(
                "aebaaaaaaag457juaap7qaldnejfprqaaaaq"
            );
            assertThat(entries.get(0).getMetadata().get("dummy").intValue()).isEqualTo(1);
        }

        verify(workspaceClient).getObject(anyString(), eq("lastOperation.json"));
        JsonNode traceabilityInformation = getSavedJsonNodeWorkspaceObject("traceabilityInformation.json");
        assertNotNull(traceabilityInformation);
        assertThat(traceabilityInformation.get("nbEntries").asLong()).isEqualTo(2);
        assertThat(traceabilityInformation.get("startDate").asText()).isEqualTo("2018-05-16T13:09:22.407");
        assertThat(LocalDateUtil.parseMongoFormattedDate(traceabilityInformation.get("endDate").asText()))
            .isAfterOrEqualTo(snapshot1.minusSeconds(temporizationDelayInSeconds))
            .isBeforeOrEqualTo(snapshot2.minusSeconds(temporizationDelayInSeconds));
        assertThat(traceabilityInformation.get("maxEntriesReached").asBoolean()).isFalse();

        checkNumberOfSavedObjectInWorkspace(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenMultipleLifecyclesWhenExecuteAndMaxEntriesReachedThenReturnResponseOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertEquals(PrepareObjectGroupLfcTraceabilityActionPlugin.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final JsonNode lastTraceability = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON)
        );
        reset(logbookLifeCyclesClient);
        doReturn(PropertiesUtils.getResourceAsStream(LFC_OBJECTS_JSONL))
            .when(logbookLifeCyclesClient)
            .exportRawObjectGroupLifecyclesByLastPersistedDate(any(), any(), anyInt());
        reset(logbookOperationsClient);
        doReturn(Response.ok(JsonHandler.writeToInpustream(lastTraceability)).build())
            .when(workspaceClient)
            .getObject(anyString(), eq("lastOperation.json"));
        handlerIO.addInIOParameters(in);

        RequestResponseOK<JsonNode> metadataResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(GOT_METADATA_JSON),
            RequestResponseOK.class,
            JsonNode.class
        );

        when(
            metadataClient.getObjectGroupsByIdsRaw(
                eq(
                    new HashSet<>(
                        Arrays.asList("aebaaaaaaag457juaap7qaldnejfprqaaaaq", "aebaaaaaaag457juaap7qaldnejfpsiaaaaq")
                    )
                )
            )
        ).thenReturn(metadataResponse);

        int temporizationDelayInSeconds = 300;
        int maxEntries = 2;
        WorkerParameters params = createExecParams(temporizationDelayInSeconds, maxEntries);
        PrepareObjectGroupLfcTraceabilityActionPlugin handler = new PrepareObjectGroupLfcTraceabilityActionPlugin(
            metadataClientFactory,
            logbookLifeCyclesClientFactory,
            10
        );

        // When
        final ItemStatus response = handler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        File savedLfcWithMetadataFile = getSavedWorkspaceObjectFile("lfcWithMetadata.jsonl");
        try (
            JsonLineGenericIterator<LfcMetadataPair> jsonLineIterator = new JsonLineGenericIterator<>(
                new FileInputStream(savedLfcWithMetadataFile),
                LFC_METADATA_PAIR_TYPE_REFERENCE
            )
        ) {
            List<LfcMetadataPair> entries = IteratorUtils.toList(jsonLineIterator);

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getLfc().get("_id").textValue()).isEqualTo(
                "aebaaaaaaag457juaap7qaldnejfprqaaaaq"
            );
            assertThat(entries.get(0).getMetadata().get("_id").textValue()).isEqualTo(
                "aebaaaaaaag457juaap7qaldnejfprqaaaaq"
            );
            assertThat(entries.get(0).getMetadata().get("dummy").intValue()).isEqualTo(1);
        }

        verify(workspaceClient).getObject(anyString(), eq("lastOperation.json"));
        JsonNode traceabilityInformation = getSavedJsonNodeWorkspaceObject("traceabilityInformation.json");
        assertNotNull(traceabilityInformation);
        assertThat(traceabilityInformation.get("nbEntries").asLong()).isEqualTo(2);
        assertThat(traceabilityInformation.get("startDate").asText()).isEqualTo("2018-05-16T13:09:22.407");
        assertThat(traceabilityInformation.get("endDate").asText()).isEqualTo("2018-05-16T13:10:07.276");
        assertThat(traceabilityInformation.get("maxEntriesReached").asBoolean()).isTrue();

        checkNumberOfSavedObjectInWorkspace(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSelectLFCErrorWhenExecuteThenReturnResponseKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        handlerIO.addOutIOParameters(out);

        reset(logbookOperationsClient);
        final JsonNode lastTraceability = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON)
        );
        doReturn(Response.ok(JsonHandler.writeToInpustream(lastTraceability)).build())
            .when(workspaceClient)
            .getObject(anyString(), eq("lastOperation.json"));
        handlerIO.addInIOParameters(in);

        reset(logbookLifeCyclesClient);
        doThrow(new InvalidParseOperationException("InvalidParseOperationException"))
            .when(logbookLifeCyclesClient)
            .exportRawObjectGroupLifecyclesByLastPersistedDate(any(), any(), anyInt());

        WorkerParameters params = createExecParams(300, 100000);
        PrepareObjectGroupLfcTraceabilityActionPlugin handler = new PrepareObjectGroupLfcTraceabilityActionPlugin(
            metadataClientFactory,
            logbookLifeCyclesClientFactory,
            10
        );

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
        assertEquals(PrepareObjectGroupLfcTraceabilityActionPlugin.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final JsonNode lastTraceability = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON)
        );
        reset(logbookLifeCyclesClient);
        doReturn(PropertiesUtils.getResourceAsStream(LFC_OBJECTS_JSONL))
            .when(logbookLifeCyclesClient)
            .exportRawObjectGroupLifecyclesByLastPersistedDate(any(), any(), anyInt());
        reset(logbookOperationsClient);
        doReturn(Response.ok(JsonHandler.writeToInpustream(lastTraceability)).build())
            .when(workspaceClient)
            .getObject(anyString(), eq("lastOperation.json"));
        handlerIO.addInIOParameters(in);

        RequestResponseOK<JsonNode> metadataResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(GOT_METADATA_JSON),
            RequestResponseOK.class,
            JsonNode.class
        );

        when(
            metadataClient.getObjectGroupsByIdsRaw(
                eq(
                    new HashSet<>(
                        Arrays.asList("aebaaaaaaag457juaap7qaldnejfprqaaaaq", "aebaaaaaaag457juaap7qaldnejfpsiaaaaq")
                    )
                )
            )
        ).thenReturn(metadataResponse);

        doThrow(new ContentAddressableStorageServerException("ContentAddressableStorageServerException"))
            .when(workspaceClient)
            .putObject(anyString(), anyString(), any(InputStream.class));

        WorkerParameters params = createExecParams(300, 100000);
        PrepareObjectGroupLfcTraceabilityActionPlugin handler = new PrepareObjectGroupLfcTraceabilityActionPlugin(
            metadataClientFactory,
            logbookLifeCyclesClientFactory,
            10
        );

        // When
        final ItemStatus response = handler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenLogbookExceptionWhenExecuteThenReturnResponseFATAL() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        handlerIO.addOutIOParameters(out);
        reset(logbookLifeCyclesClient);
        doThrow(new LogbookClientException("LogbookClientException"))
            .when(logbookLifeCyclesClient)
            .exportRawObjectGroupLifecyclesByLastPersistedDate(any(), any(), anyInt());
        final JsonNode lastTraceability = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON)
        );
        reset(logbookOperationsClient);
        doReturn(Response.ok(JsonHandler.writeToInpustream(lastTraceability)).build())
            .when(workspaceClient)
            .getObject(anyString(), eq("lastOperation.json"));
        handlerIO.addInIOParameters(in);

        WorkerParameters params = createExecParams(300, 100000);
        PrepareObjectGroupLfcTraceabilityActionPlugin handler = new PrepareObjectGroupLfcTraceabilityActionPlugin(
            metadataClientFactory,
            logbookLifeCyclesClientFactory,
            10
        );

        // When
        final ItemStatus response = handler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenMetadataExceptionWhenExecuteThenReturnResponseFATAL() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertEquals(PrepareObjectGroupLfcTraceabilityActionPlugin.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final JsonNode lastTraceability = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON)
        );
        reset(logbookLifeCyclesClient);
        doReturn(PropertiesUtils.getResourceAsStream(LFC_OBJECTS_JSONL))
            .when(logbookLifeCyclesClient)
            .exportRawObjectGroupLifecyclesByLastPersistedDate(any(), any(), anyInt());
        reset(logbookOperationsClient);
        doReturn(Response.ok(JsonHandler.writeToInpustream(lastTraceability)).build())
            .when(workspaceClient)
            .getObject(anyString(), eq("lastOperation.json"));
        handlerIO.addInIOParameters(in);

        doThrow(new VitamClientException("prb")).when(metadataClient).getObjectGroupsByIdsRaw(any());

        WorkerParameters params = createExecParams(300, 100000);
        PrepareObjectGroupLfcTraceabilityActionPlugin handler = new PrepareObjectGroupLfcTraceabilityActionPlugin(
            metadataClientFactory,
            logbookLifeCyclesClientFactory,
            10
        );

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
        assertEquals(PrepareObjectGroupLfcTraceabilityActionPlugin.getId(), HANDLER_ID);
        handlerIO.addOutIOParameters(out);

        final JsonNode lastTraceability = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(LAST_TRACEABILITY_JSON)
        );
        reset(logbookLifeCyclesClient);

        doReturn(PropertiesUtils.getResourceAsStream(LFC_OBJECTS_BIG_JSON))
            .when(logbookLifeCyclesClient)
            .exportRawObjectGroupLifecyclesByLastPersistedDate(
                eq(LocalDateUtil.parseMongoFormattedDate("2018-05-16T13:09:22.407")),
                any(),
                anyInt()
            );
        reset(logbookOperationsClient);
        doReturn(Response.ok(JsonHandler.writeToInpustream(lastTraceability)).build())
            .when(workspaceClient)
            .getObject(anyString(), eq("lastOperation.json"));
        handlerIO.addInIOParameters(in);

        List<JsonNode> gotIds = new JsonLineGenericIterator<>(
            PropertiesUtils.getResourceAsStream(LFC_OBJECTS_BIG_JSON),
            new TypeReference<JsonNode>() {}
        )
            .stream()
            .collect(Collectors.toList());

        Set<String> gotIds1 = gotIds
            .stream()
            .map(entry -> entry.get("_id").asText())
            .limit(10)
            .collect(Collectors.toSet());
        Set<String> gotIds2 = gotIds
            .stream()
            .map(entry -> entry.get("_id").asText())
            .skip(10)
            .collect(Collectors.toSet());

        when(metadataClient.getObjectGroupsByIdsRaw(gotIds1)).thenReturn(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(MD_GOTS_BIG_1_JSON),
                RequestResponseOK.class,
                JsonNode.class
            )
        );
        when(metadataClient.getObjectGroupsByIdsRaw(gotIds2)).thenReturn(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(MD_GOTS_BIG_2_JSON),
                RequestResponseOK.class,
                JsonNode.class
            )
        );

        int temporizationDelayInSeconds = 300;
        WorkerParameters params = createExecParams(temporizationDelayInSeconds, 100000);
        PrepareObjectGroupLfcTraceabilityActionPlugin handler = new PrepareObjectGroupLfcTraceabilityActionPlugin(
            metadataClientFactory,
            logbookLifeCyclesClientFactory,
            10
        );

        // When
        LocalDateTime snapshot1 = LocalDateUtil.now();
        ItemStatus response = handler.execute(params, handlerIO);
        LocalDateTime snapshot2 = LocalDateUtil.now();

        // Than
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        File savedLfcWithMetadataFile = getSavedWorkspaceObjectFile("lfcWithMetadata.jsonl");
        try (
            JsonLineGenericIterator<LfcMetadataPair> jsonLineIterator = new JsonLineGenericIterator<>(
                new FileInputStream(savedLfcWithMetadataFile),
                LFC_METADATA_PAIR_TYPE_REFERENCE
            )
        ) {
            List<LfcMetadataPair> entries = IteratorUtils.toList(jsonLineIterator);

            assertThat(entries).hasSize(13);

            Integer[] expectedVals = IntStream.rangeClosed(1, 13).boxed().toArray(Integer[]::new);
            assertThat(
                entries.stream().map(entry -> entry.getMetadata().get("dummy").intValue()).collect(Collectors.toSet())
            ).containsExactlyInAnyOrder(expectedVals);
        }
        verify(workspaceClient).getObject(anyString(), eq("lastOperation.json"));
        JsonNode traceabilityInformation = getSavedJsonNodeWorkspaceObject("traceabilityInformation.json");
        assertNotNull(traceabilityInformation);
        assertThat(traceabilityInformation.get("nbEntries").asLong()).isEqualTo(13);
        assertThat(traceabilityInformation.get("startDate").asText()).isEqualTo("2018-05-16T13:09:22.407");
        assertThat(LocalDateUtil.parseMongoFormattedDate(traceabilityInformation.get("endDate").asText()))
            .isAfterOrEqualTo(snapshot1.minusSeconds(temporizationDelayInSeconds))
            .isBeforeOrEqualTo(snapshot2.minusSeconds(temporizationDelayInSeconds));
        assertThat(traceabilityInformation.get("maxEntriesReached").asBoolean()).isFalse();

        checkNumberOfSavedObjectInWorkspace(2);
    }

    private WorkerParameters createExecParams(int temporizationDelayInSeconds, int maxEntries) {
        return WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId())
            .setLogbookTypeProcess(LogbookTypeProcess.TRACEABILITY)
            .putParameterValue(
                WorkerParameterName.lifecycleTraceabilityTemporizationDelayInSeconds,
                Integer.toString(temporizationDelayInSeconds)
            )
            .putParameterValue(WorkerParameterName.lifecycleTraceabilityMaxEntries, Integer.toString(maxEntries));
    }

    private JsonNode getSavedJsonNodeWorkspaceObject(String filename) throws InvalidParseOperationException {
        File objectNameFile = getSavedWorkspaceObjectFile(filename);
        return JsonHandler.getFromFile(objectNameFile);
    }

    private File getSavedWorkspaceObjectFile(String filename) {
        return Paths.get(storedFilesDirectory.getAbsolutePath(), filename).toFile();
    }

    private void checkNumberOfSavedObjectInWorkspace(int files) throws ContentAddressableStorageServerException {
        verify(workspaceClient, times(files)).putObject(anyString(), anyString(), any(InputStream.class));
    }

    private LfcMetadataPair parse(JsonNode jsonNode) {
        try {
            return JsonHandler.getFromJsonNode(jsonNode, LfcMetadataPair.class);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
