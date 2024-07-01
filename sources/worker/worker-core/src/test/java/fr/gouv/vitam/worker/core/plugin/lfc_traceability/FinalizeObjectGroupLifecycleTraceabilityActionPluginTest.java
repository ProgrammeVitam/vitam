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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;

import static fr.gouv.vitam.worker.core.plugin.lfc_traceability.FinalizeLifecycleTraceabilityActionPlugin.TRACEABILITY_EVENT_FILE_NAME;
import static fr.gouv.vitam.worker.core.plugin.lfc_traceability.FinalizeLifecycleTraceabilityActionPlugin.TRACEABILITY_ZIP_FILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FinalizeObjectGroupLifecycleTraceabilityActionPluginTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private HandlerIOImpl handlerIO;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private WorkerParameters params;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Before
    public void setUp() throws Exception {
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        String objectId = "objectId";
        handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "FinalizeObjectGroupLifecycleTraceabilityActionPluginTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTraceabilityZipInWorkspaceThenCopyFileToOffers() throws Exception {
        // Given
        doReturn(false).when(workspaceClient).isExistingObject(anyString(), eq(TRACEABILITY_EVENT_FILE_NAME));

        FinalizeObjectGroupLifecycleTraceabilityActionPlugin instance =
            new FinalizeObjectGroupLifecycleTraceabilityActionPlugin(storageClientFactory);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(storageClient, never()).storeFileFromWorkspace(anyString(), any(), anyString(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenNoTraceabilityZipInWorkspaceThenNothingToDo() throws Exception {
        // Given
        doReturn(true).when(workspaceClient).isExistingObject(anyString(), eq(TRACEABILITY_EVENT_FILE_NAME));
        doReturn(
            Response.status(Response.Status.OK)
                .entity(
                    PropertiesUtils.getResourceAsStream(
                        "FinalizeObjectGroupLifecycleTraceabilityActionPlugin/traceabilityEvent.json"
                    )
                )
                .build()
        )
            .when(workspaceClient)
            .getObject(any(), eq(TRACEABILITY_EVENT_FILE_NAME));

        FinalizeObjectGroupLifecycleTraceabilityActionPlugin instance =
            new FinalizeObjectGroupLifecycleTraceabilityActionPlugin(storageClientFactory);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatus.getMasterData()).containsOnlyKeys(LogbookParameterName.eventDetailData.name());
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromString(
                (String) itemStatus.getMasterData().get(LogbookParameterName.eventDetailData.name())
            ),
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(
                    "FinalizeObjectGroupLifecycleTraceabilityActionPlugin/traceabilityEvent.json"
                )
            )
        );
        ArgumentCaptor<ObjectDescription> objectDescriptionArgumentCaptor = ArgumentCaptor.forClass(
            ObjectDescription.class
        );
        verify(storageClient).storeFileFromWorkspace(
            eq(VitamConfiguration.getDefaultStrategy()),
            eq(DataCategory.LOGBOOK),
            eq("0_LogbookObjectGroupLifecycles_20191218_075549.zip"),
            objectDescriptionArgumentCaptor.capture()
        );
        assertThat(objectDescriptionArgumentCaptor.getValue().getWorkspaceObjectURI()).isEqualTo(
            TRACEABILITY_ZIP_FILE_NAME
        );
    }
}
