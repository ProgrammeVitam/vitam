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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClientMock;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CheckStorageAvailabilityActionHandlerTest {

    private static final String HANDLER_ID = "STORAGE_AVAILABILITY_CHECK";
    private GUID guid;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
    private static final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory = mock(
        LogbookLifeCyclesClientFactory.class
    );
    private static final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
    private static final StorageClient storageClient = mock(StorageClient.class);
    private static final LogbookLifeCyclesClient logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
    private static final WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
    private static final HandlerIO handlerIO = mock(HandlerIO.class);

    private static final SedaUtilsFactory sedaUtilsFactory = mock(SedaUtilsFactory.class);
    private static final SedaUtils sedaUtils = mock(SedaUtils.class);

    private CheckStorageAvailabilityActionHandler handler = new CheckStorageAvailabilityActionHandler(
        storageClientFactory,
        sedaUtilsFactory
    );

    @Before
    public void setUp() throws FileNotFoundException {
        reset(storageClient);
        reset(workspaceClient);
        reset(logbookLifeCyclesClient);
        reset(sedaUtils);
        reset(handlerIO);

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(sedaUtilsFactory.createSedaUtils(handlerIO)).thenReturn(sedaUtils);

        guid = GUIDFactory.newGUID();
    }

    @Test
    public void givenSedaNotExistWhenCheckStorageThenReturnResponseFatal() throws Exception {
        when(sedaUtils.computeTotalSizeOfObjectsInManifest(any())).thenThrow(new ProcessingException(""));

        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, handlerIO);

        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void givenSedaExistWhenCheckStorageExecuteThenReturnResponseKO() throws Exception {
        File input = PropertiesUtils.getResourceFile(
            "CheckStorageAvailabilityActionHandler/ingestContractWithDetail_no_MC.json"
        );
        when(handlerIO.getInput(eq(0))).thenReturn(input);
        when(sedaUtils.computeTotalSizeOfObjectsInManifest(any())).thenReturn(new Long(838860800));

        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());

        final String MOCK_INFOS_RESULT_ARRAY =
            "{\"capacities\": [{\"offerId\": \"offer1\",\"usableSpace\": " +
            "838860700, \"nbc\": 2}," +
            "{\"offerId\": " +
            "\"offer2\",\"usableSpace\": 838860800,  \"nbc\": 2}]}";

        when(storageClient.getStorageInformation(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            JsonHandler.getFromString(MOCK_INFOS_RESULT_ARRAY)
        );

        final ItemStatus response = handler.execute(params, handlerIO);

        assertEquals(StatusCode.KO, response.getGlobalStatus());
        JsonNode evDetData = JsonHandler.getFromString(response.getEvDetailData());
        assertEquals(evDetData.get("offer1").textValue(), "KO");
        assertEquals(evDetData.get("offer1_usableSpace").intValue(), 838860700);
        assertEquals(evDetData.get("offer1_totalSizeToBeStored").intValue(), 838860800);
        assertEquals(evDetData.get("offer2").textValue(), "OK");
    }

    @Test
    public void givenSedaExistWhenCheckStorageExecuteThenReturnResponseOK() throws Exception {
        File input = PropertiesUtils.getResourceFile(
            "CheckStorageAvailabilityActionHandler/ingestContractWithDetail_no_MC.json"
        );
        when(handlerIO.getInput(eq(0))).thenReturn(input);
        when(sedaUtils.computeTotalSizeOfObjectsInManifest(any())).thenReturn(new Long(1024));

        when(storageClient.getStorageInformation(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            new StorageClientMock().getStorageInformation("str")
        );
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, handlerIO);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
        JsonNode evDetData = JsonHandler.getFromString(response.getEvDetailData());
        assertEquals(evDetData.get("offer1").textValue(), "OK");
        assertNull(evDetData.get("offer1_usableSpace"));
        assertNull(evDetData.get("offer1_totalSizeToBeStored"));
        assertEquals(evDetData.get("offer2").textValue(), "OK");
        assertNull(evDetData.get("offer2_usableSpace"));
        assertNull(evDetData.get("offer2_totalSizeToBeStored"));
    }

    // This test checks that if a null is returned by the getStorageInformation, then we get an OK status, and a WARN
    // log is displayed
    @RunWithCustomExecutor
    @Test
    public void givenProblemWithOfferCheckStorageExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(-1);
        File input = PropertiesUtils.getResourceFile(
            "CheckStorageAvailabilityActionHandler/ingestContractWithDetail_no_MC.json"
        );
        when(handlerIO.getInput(eq(0))).thenReturn(input);
        when(sedaUtils.computeTotalSizeOfObjectsInManifest(any())).thenReturn(new Long(1024));

        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, handlerIO);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    // This test checks that if empty informations is returned by the getStorageInformation (sthg like {\"capacities\":
    // []}), then we get an OK status, and a WARN log is displayed
    @RunWithCustomExecutor
    @Test
    public void givenProblemWithOffersCheckStorageExecuteThenReturnResponseOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(-2);
        File input = PropertiesUtils.getResourceFile(
            "CheckStorageAvailabilityActionHandler/ingestContractWithDetail_no_MC.json"
        );
        when(handlerIO.getInput(eq(0))).thenReturn(input);
        when(sedaUtils.computeTotalSizeOfObjectsInManifest(any())).thenReturn(new Long(1024));

        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void givenMcNoObjectStorageWhenCheckStorageExecuteThenReturnResponseOK() throws Exception {
        File input = PropertiesUtils.getResourceFile(
            "CheckStorageAvailabilityActionHandler/ingestContractWithDetail_MC_noObjectStorage.json"
        );
        when(handlerIO.getInput(eq(0))).thenReturn(input);
        when(sedaUtils.computeTotalSizeOfObjectsInManifest(any())).thenReturn(new Long(1024));

        when(storageClient.getStorageInformation(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            new StorageClientMock().getStorageInformation(VitamConfiguration.getDefaultStrategy())
        );
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, handlerIO);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
        JsonNode evDetData = JsonHandler.getFromString(response.getEvDetailData());
        assertEquals(evDetData.get("offer1").textValue(), "OK");
        assertEquals(evDetData.get("offer2").textValue(), "OK");
    }

    @Test
    public void givenMcObjectStorageWhenCheckStorageExecuteThenReturnResponseOK() throws Exception {
        File input = PropertiesUtils.getResourceFile(
            "CheckStorageAvailabilityActionHandler/ingestContractWithDetail_MC_ObjectStorage.json"
        );
        when(handlerIO.getInput(eq(0))).thenReturn(input);
        when(sedaUtils.computeTotalSizeOfObjectsInManifest(any())).thenReturn(new Long(1024));

        when(storageClient.getStorageInformation(eq("test"))).thenReturn(
            new StorageClientMock().getStorageInformation(VitamConfiguration.getDefaultStrategy())
        );
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, handlerIO);

        verify(storageClient, never()).getStorageInformation(eq(VitamConfiguration.getDefaultStrategy()));
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        JsonNode evDetData = JsonHandler.getFromString(response.getEvDetailData());
        assertEquals(evDetData.get("offer1").textValue(), "OK");
        assertEquals(evDetData.get("offer2").textValue(), "OK");
    }
}
