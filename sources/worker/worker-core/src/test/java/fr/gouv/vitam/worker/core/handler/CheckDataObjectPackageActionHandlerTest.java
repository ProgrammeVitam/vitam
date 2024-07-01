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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.ExtractUriResponse;
import fr.gouv.vitam.worker.common.utils.SedaIngestParams;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static fr.gouv.vitam.worker.common.utils.SedaUtilsFactory.SEDA_INGEST_PARAMS_FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckDataObjectPackageActionHandlerTest {

    private static final String SIP_ARBORESCENCE = "SIP_Arborescence.xml";
    private static final String STORAGE_INFO_JSON = "CheckDataObjectPackageActionHandler/storageInfo.json";
    private static final String INGEST_CONTRACT = "CheckDataObjectPackageActionHandler/ingestContractWithDetails.json";
    private static final String SEDA_INGEST_PARAMS = "CheckDataObjectPackageActionHandler/SedaParams.json";

    private static final String CHECK_NO_OBJECTS_ACTION_HANDLER_ITEM_STATUS_FILE =
        "CheckDataObjectPackageActionHandler/checkNoObjectsActionHandler.json";
    private static final String CHECK_OBJECTS_NUMBER_ACTION_HANDLER_ITEM_STATUS_FILE =
        "CheckDataObjectPackageActionHandler/checkObjectsNumberActionHandler.json";
    private static final String EXTRACT_SEDA_ACTION_HANDLER_ITEM_STATUS_FILE =
        "CheckDataObjectPackageActionHandler/extractSedaActionHandler.json";
    private AdminManagementClient adminManagementClient;

    private WorkspaceClient workspaceClient;
    private SedaUtilsFactory sedaUtilsFactory;

    private SedaUtils sedaUtils;
    private ExtractUriResponse extractUriResponseOK;
    private HandlerIOImpl action;
    private List<IOParameter> out;
    private List<IOParameter> in;
    private static final Integer TENANT_ID = 0;
    private final Set<URI> uriSetWorkspaceOK = new HashSet<>();
    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
        .setUrlWorkspace("http://localhost:8083")
        .setUrlMetadata("http://localhost:8083")
        .setObjectNameList(Lists.newArrayList("objectName.json"))
        .setObjectName("objectName.json")
        .setCurrentStep("currentStep")
        .setLogbookTypeProcess(LogbookTypeProcess.INGEST)
        .setContainerName("ExtractSedaActionHandlerTest");

    private CheckDataObjectPackageActionHandler handler;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TempFolderRule temporaryFolder = new TempFolderRule();

    @Before
    public void setUp() throws URISyntaxException, IOException, InvalidParseOperationException {
        LogbookLifeCyclesClient logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        sedaUtilsFactory = mock(SedaUtilsFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        adminManagementClient = mock(AdminManagementClient.class);
        AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        MetaDataClient metadataClient = mock(MetaDataClient.class);
        MetaDataClientFactory metadataClientFactory = mock(MetaDataClientFactory.class);
        when(metadataClientFactory.getClient()).thenReturn(metadataClient);

        sedaUtils = mock(SedaUtils.class);

        CheckNoObjectsActionHandler checkNoObjectsActionHandler = mock(CheckNoObjectsActionHandler.class);
        try (
            InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                CHECK_NO_OBJECTS_ACTION_HANDLER_ITEM_STATUS_FILE
            )
        ) {
            ItemStatus itemStatus = JsonHandler.getFromInputStream(resourceAsStream, ItemStatus.class);
            when(checkNoObjectsActionHandler.execute(any(), any())).thenReturn(itemStatus);
        }

        CheckObjectsNumberActionHandler checkObjectsNumberActionHandler = mock(CheckObjectsNumberActionHandler.class);
        try (
            InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                CHECK_OBJECTS_NUMBER_ACTION_HANDLER_ITEM_STATUS_FILE
            )
        ) {
            ItemStatus itemStatus = JsonHandler.getFromInputStream(resourceAsStream, ItemStatus.class);
            when(checkObjectsNumberActionHandler.execute(any(), any())).thenReturn(itemStatus);
        }

        ExtractSedaActionHandler extractSedaActionHandler = mock(ExtractSedaActionHandler.class);
        try (
            InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(
                EXTRACT_SEDA_ACTION_HANDLER_ITEM_STATUS_FILE
            )
        ) {
            ItemStatus itemStatus = JsonHandler.getFromInputStream(resourceAsStream, ItemStatus.class);
            when(extractSedaActionHandler.execute(any(), any())).thenReturn(itemStatus);
        }

        handler = new CheckDataObjectPackageActionHandler(
            metadataClientFactory,
            adminManagementClientFactory,
            logbookLifeCyclesClientFactory,
            sedaUtilsFactory,
            checkNoObjectsActionHandler,
            checkObjectsNumberActionHandler,
            extractSedaActionHandler,
            mock(CheckObjectUnitConsistencyActionHandler.class)
        );

        String objectId = "objectId";
        action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "ExtractSedaActionHandlerTest",
            "workerId",
            newArrayList(objectId)
        );
        action.setCurrentObjectId(objectId);

        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "UnitsLevel/ingestLevelStack.json")));
        out.add(
            new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP.json"))
        );
        out.add(
            new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_ID_TO_GUID_MAP.json"))
        );
        out.add(
            new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json"))
        );
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "MapsMemory/OG_TO_ARCHIVE_ID_MAP.json")));
        out.add(
            new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP.json"))
        );
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "ATR/globalSEDAParameters.json")));
        out.add(
            new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "MapsMemory/OBJECT_GROUP_ID_TO_GUID_MAP.json"))
        );
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/GUID_TO_ARCHIVE_ID_MAP.json")));

        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Ontology/ontology.json")));

        out.add(
            new IOParameter()
                .setUri(
                    new ProcessingUri(UriPrefix.WORKSPACE, "Maps/EXISTING_GOT_TO_NEW_GOT_GUID_FOR_ATTACHMENT_MAP.json")
                )
        );

        out.add(
            new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/EXISTING_UNITS_GUID_FOR_ATTACHMENT_MAP.json"))
        );

        out.add(
            new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/EXISTING_GOTS_GUID_FOR_ATTACHMENT_MAP.json"))
        );

        in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "true")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "INGEST")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "StorageInfo/storageInfo.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "referential/contracts.json")));
        out.add(
            new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.WORKSPACE, "UpdateObjectGroup/existing_object_group.json"))
        );

        uriSetWorkspaceOK.add(new URI("content/file1.pdf"));
        uriSetWorkspaceOK.add(new URI("content/file2.pdf"));
        uriSetWorkspaceOK.add(new URI("manifest.xml"));
        extractUriResponseOK = new ExtractUriResponse();
        extractUriResponseOK.setUriSetManifest(uriSetWorkspaceOK);
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void testHandlerWorking() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertNotNull(CheckDataObjectPackageActionHandler.getId());
        final InputStream seda_arborescence = PropertiesUtils.getResourceAsStream(SIP_ARBORESCENCE);
        final InputStream storageInfo = PropertiesUtils.getResourceAsStream(STORAGE_INFO_JSON);
        final InputStream ingestContract = PropertiesUtils.getResourceAsStream(INGEST_CONTRACT);
        SedaIngestParams sedaIngestParams = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(SEDA_INGEST_PARAMS),
            SedaIngestParams.class
        );
        when(sedaUtils.getSedaIngestParams()).thenReturn(sedaIngestParams);
        when(sedaUtilsFactory.createSedaUtilsWithSedaIngestParams(any())).thenReturn(sedaUtils);
        when(sedaUtilsFactory.createSedaUtils(any())).thenReturn(sedaUtils);

        when(workspaceClient.isExistingObject(any(), eq(SEDA_INGEST_PARAMS_FILE))).thenReturn(true);
        when(workspaceClient.getObject(any(), eq(SEDA_INGEST_PARAMS_FILE))).thenReturn(
            Response.status(Status.OK).entity(sedaIngestParams).build()
        );

        when(sedaUtils.getAllDigitalObjectUriFromManifest()).thenReturn(extractUriResponseOK);
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml"))).thenReturn(
            Response.status(Status.OK).entity(seda_arborescence).build()
        );
        when(workspaceClient.getListUriDigitalObjectFromFolder(any(), any())).thenReturn(
            new RequestResponseOK<List<URI>>().addResult(List.copyOf(uriSetWorkspaceOK))
        );
        when(workspaceClient.getObject(any(), eq("StorageInfo/storageInfo.json"))).thenReturn(
            Response.status(Status.OK).entity(storageInfo).build()
        );
        when(workspaceClient.getObject(any(), eq("referential/contracts.json"))).thenReturn(
            Response.status(Status.OK).entity(ingestContract).build()
        );
        when(workspaceClient.getObject(any(), eq("Maps/sedaParams.json"))).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream(SEDA_INGEST_PARAMS)).build()
        );
        when(adminManagementClient.findIngestContractsByID(anyString())).thenReturn(
            ClientMockResultHelper.getIngestContracts()
        );
        when(adminManagementClient.findIngestContracts(any())).thenReturn(ClientMockResultHelper.getIngestContracts());
        action.addOutIOParameters(out);
        action.addInIOParameters(in);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());

        in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "false")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "INGEST")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "StorageInfo/storageInfo.json")));
        action.reset();
        action.addOutIOParameters(out);
        action.addInIOParameters(in);

        Map<String, Map<String, String>> versionMap = new HashMap<>();
        final Map<String, String> invalidVersionMap = new HashMap<>();
        final Map<String, String> validVersionMap = new HashMap<>();
        versionMap.put(SedaUtils.INVALID_DATAOBJECT_VERSION, invalidVersionMap);
        versionMap.put(SedaUtils.VALID_DATAOBJECT_VERSION, validVersionMap);

        Mockito.doReturn(versionMap).when(sedaUtils).checkSupportedDataObjectVersion(any());
        final ItemStatus response2 = handler.execute(params, action);
        assertEquals(StatusCode.KO, response2.getGlobalStatus());
    }
}
