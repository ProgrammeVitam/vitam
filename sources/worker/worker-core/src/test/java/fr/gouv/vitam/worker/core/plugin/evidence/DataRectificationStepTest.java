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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.StoreMetaDataObjectGroupActionPlugin;
import fr.gouv.vitam.worker.core.plugin.StoreMetaDataUnitActionPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Arrays;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.json.JsonHandler.writeAsFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataRectificationStepTest {

    public static final String OBJECTGROUP_REPORT_EVIDENCE_AUDIT =
        "{\n" +
        "  \"identifier\" : \"aebaaaaaaackemvrabfuealm66lqsjqaaaaq\",\n" +
        "  \"status\" : \"KO\",\n" +
        "  \"message\" : \"Traceability audit KO  Database check failure Errors are :  [ \\\"There is an  error on the audit of the  linked  object\\\" ]\",\n" +
        "  \"objectType\" : \"OBJECTGROUP\",\n" +
        "  \"objectsReports\" : [ {\n" +
        "    \"identifier\" : \"aeaaaaaaaackemvrabfuealm66lqsiqaaaaq\",\n" +
        "    \"status\" : \"KO\",\n" +
        "    \"message\" : \"[\\\"The digest '942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7' for the offer 'default-bis' is null \\\"]\",\n" +
        "    \"securedHash\" : \"942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7\",\n" +
        "    \"strategyId\" : \"default\",\n" +
        "    \"offersHashes\" : {\n" +
        "        \"default\" : \"942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7\",\n" +
        "        \"default-bis\" : \"null\"\n" +
        "    }\n" +
        "  } ],\n" +
        "  \"securedHash\" : \"6bb18b91044ef4c92ec47e558ebdfc822a02c8c9d768de6653e7f353f6e2f0b00cb3d53b05dde3080f66b9601b020c03ae2e48c7629e9f7ccf59c23b2d18ecb8\",\n" +
        "  \"strategyId\" : \"default\",\n" +
        "  \"offersHashes\" : {\n" +
        "    \"default\" : \"6bb18b91044ef4c92ec47e558ebdfc822a02c8c9d768de6653e7f353f6e2f0b00cb3d53b05dde3080f66b9601b020c03ae2e48c7629e9f7ccf59c23b2d18ecb8\",\n" +
        "    \"default-bis\" : \"6bb18b91044ef4c92ec47e558ebdfc822a02c8c9d768de6653e7f353f6e2f0b00cb3d53b05dde3080f66b9601b020c03ae2e48c7629e9f7ccf59c23b2d18ecb8\"\n" +
        "  }\n" +
        "}";
    public static final String UNIT_REPORT_EVIDENCE_AUDIT =
        "{\n" +
        "  \"identifier\" : \"aebaaaaaaackemvrabfuealm66lqsjqaaaaq\",\n" +
        "  \"status\" : \"KO\",\n" +
        "  \"message\" : \"Traceability audit KO  Database check failure Errors are :  [ \\\"There is an  error on the audit of the  linked  object\\\" ]\",\n" +
        "  \"objectType\" : \"UNIT\",\n" +
        "  \"objectsReports\" : [],\n" +
        "  \"securedHash\" : \"6bb18b91044ef4c92ec47e558ebdfc822a02c8c9d768de6653e7f353f6e2f0b00cb3d53b05dde3080f66b9601b020c03ae2e48c7629e9f7ccf59c23b2d18ecb8\",\n" +
        "  \"strategyId\" : \"default\",\n" +
        "  \"offersHashes\" : {\n" +
        "    \"default\" : \"6bb18b91044ef4c92ec47e558ebdfc822a02c8c9d768de6653e7f353f6e2f0b00cb3d53b05dde3080f66b9601b020c03ae2e48c7629e9f7ccf59c23b2d18ecb8\",\n" +
        "    \"default-bis\" : \"z6bb18b91044ef4c92ec47e558ebdfc822a02c8c9d768de6653e7f353f6e2f0b00cb3d53b05dde3080f66b9601b020c03ae2e48c7629e9f7ccf59c23b2d18ecb8\"\n" +
        "  }\n" +
        "}";
    private static final String FAKE_REQUEST_ID = "FakeRequestId";
    private static final String FAKE_CONTEXT_ID = "FakeContextId";
    private static final Integer TENANT = 0;
    private static final String GUID_OG1 = "rectification/aebaaaaaaackemvrabfuealm66lqsjqaaaaq.json";
    private static final String CONTAINER_NAME = "aecaaaaaacecatmyabbmealcfeqzwjwaaaaq";
    private static final String OBJECT_NAME = "test";
    private static final String ALTER = "alter/test";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    public HandlerIO handlerIO;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    WorkerParameters defaultWorkerParameters;

    @Mock
    private DataRectificationService dataRectificationService;

    private DataRectificationStep dataRectificationStep;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    private JsonNode rawObjectGroup1;

    private MetaDataClient metaDataClient;
    private MetaDataClientFactory metaDataClientFactory;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private StorageClient storageClient;
    private StoreMetaDataObjectGroupActionPlugin storeMetaDataObjectGroupActionPlugin;
    private StoreMetaDataUnitActionPlugin storeMetaDataUnitActionPlugin;

    @Before
    public void setUp() throws Exception {
        metaDataClient = mock(MetaDataClient.class);
        storageClient = mock(StorageClient.class);
        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        storageClientFactory = mock(StorageClientFactory.class);
        metaDataClientFactory = mock(MetaDataClientFactory.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        storeMetaDataObjectGroupActionPlugin = mock(StoreMetaDataObjectGroupActionPlugin.class);
        storeMetaDataUnitActionPlugin = mock(StoreMetaDataUnitActionPlugin.class);

        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        dataRectificationService = new DataRectificationService(storageClientFactory, logbookLifeCyclesClientFactory);
        dataRectificationStep = new DataRectificationStep(
            dataRectificationService,
            storeMetaDataObjectGroupActionPlugin,
            storeMetaDataUnitActionPlugin
        );

        if (Thread.currentThread() instanceof VitamThreadFactory.VitamThread) {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT);
            VitamThreadUtils.getVitamSession().setRequestId(FAKE_REQUEST_ID);
            VitamThreadUtils.getVitamSession().setContextId(FAKE_CONTEXT_ID);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_rectificate_objectGroup_step() throws Exception {
        // Given
        when(defaultWorkerParameters.getObjectName()).thenReturn(OBJECT_NAME);
        when(defaultWorkerParameters.getContainerName()).thenReturn(CONTAINER_NAME);

        File file = tempFolder.newFile();
        File file2 = tempFolder.newFile();
        writeAsFile(getFromString(OBJECTGROUP_REPORT_EVIDENCE_AUDIT), file);

        when(handlerIO.getFileFromWorkspace(ALTER)).thenReturn(file);
        when(handlerIO.getNewLocalFile(OBJECT_NAME)).thenReturn(file2);

        rawObjectGroup1 = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(GUID_OG1));

        doReturn(new RequestResponseOK<JsonNode>().addResult(rawObjectGroup1))
            .when(metaDataClient)
            .getObjectGroupsByIdsRaw(eq(Arrays.asList(GUID_OG1)));
        // When
        ItemStatus execute = dataRectificationStep.execute(defaultWorkerParameters, handlerIO);
        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(handlerIO.getNewLocalFile("test")).isFile().exists();
        JsonNode result = JsonHandler.getFromFile(handlerIO.getNewLocalFile("test"));
        assertThat(result.get(0).get("Id").asText()).isEqualTo("aebaaaaaaackemvrabfuealm66lqsjqaaaaq");
        assertThat(result.get(0).get("Type").asText()).isEqualTo("OBJECT");
    }

    @Test
    @RunWithCustomExecutor
    public void should_rectificate_unit_step() throws Exception {
        // Given
        when(defaultWorkerParameters.getObjectName()).thenReturn(OBJECT_NAME);
        when(defaultWorkerParameters.getContainerName()).thenReturn(CONTAINER_NAME);

        File file = tempFolder.newFile();
        File file2 = tempFolder.newFile();
        writeAsFile(getFromString(UNIT_REPORT_EVIDENCE_AUDIT), file);

        when(handlerIO.getFileFromWorkspace(ALTER)).thenReturn(file);
        when(handlerIO.getNewLocalFile(OBJECT_NAME)).thenReturn(file2);

        rawObjectGroup1 = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(GUID_OG1));

        doReturn(new RequestResponseOK<JsonNode>().addResult(rawObjectGroup1))
            .when(metaDataClient)
            .getObjectGroupsByIdsRaw(eq(Arrays.asList(GUID_OG1)));

        // When
        ItemStatus execute = dataRectificationStep.execute(defaultWorkerParameters, handlerIO);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
        JsonNode result = JsonHandler.getFromFile(handlerIO.getNewLocalFile("test"));
        assertThat(result.get(0).get("Id").asText()).isEqualTo("aebaaaaaaackemvrabfuealm66lqsjqaaaaq");
        assertThat(result.get(0).get("Type").asText()).isEqualTo("UNIT");
    }
}
