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
package fr.gouv.vitam.worker.core.plugin;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexUnitActionPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private MetaDataClient metadataClient;

    IndexUnitActionPlugin plugin;
    private static final String ARCHIVE_UNIT = "indexUnitActionHandler/archiveUnit.json";
    private static final String ARCHIVE_UNIT_WITH_RULES =
        "indexUnitActionHandler/ARCHIVE_UNIT_TO_INDEX_WITH_RULES.json";
    private static final String ARCHIVE_UNIT_UPDATE_GUID_CHILD = "indexUnitActionHandler/GUID_ARCHIVE_UNIT_CHILD.json";
    private static final String ARCHIVE_UNIT_UPDATE_GUID_PARENT =
        "indexUnitActionHandler/GUID_ARCHIVE_UNIT_PARENT.json";
    private static final String ARCHIVE_UNIT_WITh_MGT_RULES = "indexUnitActionHandler/ARCHIVE_UNIT_WITH_MGT_RULES.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS = "globalSEDAParameters.json";

    private InputStream archiveUnit;
    private InputStream archiveUnitWithRules;
    private InputStream archiveUnitChild;
    private InputStream archiveUnitParent;
    private InputStream archiveUnitWithMgtRules;
    private GUID guid = GUIDFactory.newGUID();

    private HandlerIO handlerIO = mock(HandlerIO.class);
    private File globalSEDAParameter;

    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
        .setUrlWorkspace("http://localhost:8083")
        .setUrlMetadata("http://localhost:8083")
        .setObjectNameList(Lists.newArrayList("objectName.json"))
        .setObjectName("objectName.json")
        .setCurrentStep("currentStep")
        .setContainerName(guid.getId())
        .setLogbookTypeProcess(LogbookTypeProcess.INGEST);

    public IndexUnitActionPluginTest() {}

    @Before
    public void setUp() throws Exception {
        archiveUnit = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT);
        archiveUnitWithRules = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_WITH_RULES);
        archiveUnitChild = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_UPDATE_GUID_CHILD);
        archiveUnitParent = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_UPDATE_GUID_PARENT);
        archiveUnitWithMgtRules = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_WITh_MGT_RULES);
        when(metaDataClientFactory.getClient()).thenReturn(metadataClient);
        plugin = new IndexUnitActionPlugin(metaDataClientFactory);
        globalSEDAParameter = PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS);
        when(handlerIO.getInput(0)).thenReturn(globalSEDAParameter);
    }

    @After
    public void finish() {
        StreamUtils.closeSilently(archiveUnit);
        StreamUtils.closeSilently(archiveUnitWithRules);
        StreamUtils.closeSilently(archiveUnitChild);
        StreamUtils.closeSilently(archiveUnitParent);
        StreamUtils.closeSilently(archiveUnitWithMgtRules);
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL() throws Exception {
        final List<ItemStatus> response = plugin.executeList(params, handlerIO);
        assertEquals(response.get(0).getGlobalStatus(), StatusCode.FATAL);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        // Given
        when(metadataClient.insertUnitBulk(any())).thenReturn(JsonHandler.createObjectNode());
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(archiveUnit);
        // When
        final List<ItemStatus> response = plugin.executeList(params, handlerIO);
        // Then
        assertThat(response.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void testMetadataException() throws Exception {
        // Given
        when(metadataClient.insertUnitBulk(any())).thenThrow(new MetaDataExecutionException(""));
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(archiveUnit);
        // When
        final List<ItemStatus> response = plugin.executeList(params, handlerIO);
        // Then
        assertThat(response.get(0).getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    public void testWorkspaceException() throws Exception {
        // Given
        when(metadataClient.insertUnitBulk(any())).thenReturn(JsonHandler.createObjectNode());
        when(workspaceClient.getObject(any(), eq("Units/objectName.json"))).thenThrow(
            new ContentAddressableStorageNotFoundException("")
        );
        // When
        final List<ItemStatus> response = plugin.executeList(params, handlerIO);
        // Then
        assertThat(response.get(0).getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    public void testIndexUnitWithRulesOk() throws Exception {
        // Given
        when(metadataClient.insertUnitBulk(any())).thenReturn(JsonHandler.createObjectNode());
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(archiveUnitWithRules);
        // When
        final List<ItemStatus> response = plugin.executeList(params, handlerIO);
        // Then
        assertThat(response.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void testIndexUnitUpdateChildOk() throws Exception {
        // Given
        when(metadataClient.insertUnitBulk(any())).thenReturn(JsonHandler.createObjectNode());
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(archiveUnitChild);
        // When
        final List<ItemStatus> response = plugin.executeList(params, handlerIO);
        // Then
        assertThat(response.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void testIndexUnitUpdateParentOk() throws Exception {
        // Given
        when(metadataClient.insertUnitBulk(any())).thenReturn(JsonHandler.createObjectNode());
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(archiveUnitParent);
        // When
        final List<ItemStatus> response = plugin.executeList(params, handlerIO);
        // Then
        assertThat(response.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    public void testIndexWithRules() throws Exception {
        // Given
        when(metadataClient.insertUnitBulk(any())).thenReturn(JsonHandler.createObjectNode());
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(archiveUnitWithMgtRules);
        // When
        final List<ItemStatus> response = plugin.executeList(params, handlerIO);
        // Then
        assertThat(response.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
    }
}
