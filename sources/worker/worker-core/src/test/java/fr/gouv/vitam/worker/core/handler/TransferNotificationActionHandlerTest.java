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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({WorkspaceClientFactory.class})
public class TransferNotificationActionHandlerTest {
    private static final String ARCHIVE_ID_TO_GUID_MAP = "ARCHIVE_ID_TO_GUID_MAP_obj.json";
    private static final String BINARY_DATA_OBJECT_ID_TO_GUID_MAP = "BINARY_DATA_OBJECT_ID_TO_GUID_MAP_obj.json";
    private static final String BDO_TO_OBJECT_GROUP_ID_MAP = "BDO_TO_OBJECT_GROUP_ID_MAP_obj.json";
    private static final String BDO_TO_VERSION_BDO_MAP = "BDO_TO_VERSION_BDO_MAP_obj.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS = "globalSEDAParameters.json";

    private static final String HANDLER_ID = "ATR_NOTIFICATION";
    private static final String LOGBOOK_OPERATION = "transferNotificationActionHandler/logbookOperation.json";
    private static final String LOGBOOK_LFC_AU = "transferNotificationActionHandler/logbookLifecycleAU.json";
    private static final String LOGBOOK_LFC_GOT = "transferNotificationActionHandler/logbookLifecycleGOT.json";

    private static LogbookDbAccess mongoDbAccess;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;    
    private HandlerIO action;
    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8080")
            .setUrlMetadata("http://localhost:8080").setObjectName("objectName.json").setCurrentStep("currentStep")
            .setContainerName("containerName").setProcessId("aeaaaaaaaaaaaaababz4aakxtykbybyaaaaq");

    @Before
    public void setUp() throws URISyntaxException, FileNotFoundException {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);        
        mongoDbAccess = mock(LogbookDbAccess.class);
        action = new HandlerIO("containerName");
        action.addInput(PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP));
        action.addInput(PropertiesUtils.getResourceFile(BINARY_DATA_OBJECT_ID_TO_GUID_MAP));
        action.addInput(PropertiesUtils.getResourceFile(BDO_TO_OBJECT_GROUP_ID_MAP));
        action.addInput(PropertiesUtils.getResourceFile(BDO_TO_VERSION_BDO_MAP));
        action.addInput(PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS));
    }


    @Test
    public void givenXMLCreationWhenValidThenResponseOK()
        throws Exception {
        TransferNotificationActionHandler handler = new TransferNotificationActionHandler(mongoDbAccess);

        MongoCursor<LogbookLifeCycleObjectGroup> lifecycleGOTCursor = mock(MongoCursor.class);
        Mockito.when(lifecycleGOTCursor.hasNext()).thenReturn(true).thenReturn(false);
        Mockito.when(lifecycleGOTCursor.next()).thenReturn(getLogbookLifecycleGOT());

        MongoCursor<LogbookLifeCycleUnit> lifecycleAUCursor = mock(MongoCursor.class);
        Mockito.when(lifecycleAUCursor.hasNext()).thenReturn(true).thenReturn(false);
        Mockito.when(lifecycleAUCursor.next()).thenReturn(getLogbookLifecycleAU());

        Mockito.doReturn(getLogbookOperation()).when(mongoDbAccess).getLogbookOperation(anyObject());
        Mockito.doReturn(lifecycleGOTCursor).when(mongoDbAccess).getLogbookLifeCycleObjectGroups(anyObject());
        Mockito.doReturn(lifecycleAUCursor).when(mongoDbAccess).getLogbookLifeCycleUnits(anyObject());

        assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenXMLKOCreationWhenProcessKOThenResponseKO()
        throws Exception {
        TransferNotificationActionHandler handler = new TransferNotificationActionHandler(mongoDbAccess);

        MongoCursor<LogbookLifeCycleObjectGroup> lifecycleGOTCursor = mock(MongoCursor.class);
        Mockito.when(lifecycleGOTCursor.hasNext()).thenReturn(true).thenReturn(false);
        Mockito.when(lifecycleGOTCursor.next()).thenReturn(getLogbookLifecycleGOT());

        MongoCursor<LogbookLifeCycleUnit> lifecycleAUCursor = mock(MongoCursor.class);
        Mockito.when(lifecycleAUCursor.hasNext()).thenReturn(true).thenReturn(false);
        Mockito.when(lifecycleAUCursor.next()).thenReturn(getLogbookLifecycleAU());

        Mockito.doReturn(getLogbookOperation()).when(mongoDbAccess).getLogbookOperation(anyObject());
        Mockito.doReturn(lifecycleGOTCursor).when(mongoDbAccess).getLogbookLifeCycleObjectGroups(anyObject());        
        Mockito.doReturn(lifecycleAUCursor).when(mongoDbAccess).getLogbookLifeCycleUnits(anyObject());

        assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
        final CompositeItemStatus response = handler
            .execute(params.putParameterValue(WorkerParameterName.workflowStatusKo, Boolean.TRUE.toString()), action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }


    @Test
    public void givenXMLKOCreationWhenProcessKOBeforeLifecycleThenResponseOK()
        throws Exception {
        TransferNotificationActionHandler handler = new TransferNotificationActionHandler(mongoDbAccess);

        Mockito.doReturn(getLogbookOperation()).when(mongoDbAccess).getLogbookOperation(anyObject());
        Mockito.doReturn(null).when(mongoDbAccess).getLogbookLifeCycleObjectGroups(anyObject());
        Mockito.doReturn(null).when(mongoDbAccess).getLogbookLifeCycleUnits(anyObject());

        assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
        final CompositeItemStatus response = handler
            .execute(params.putParameterValue(WorkerParameterName.workflowStatusKo, Boolean.TRUE.toString()), action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    
    @Test
    public void givenExceptionLogbookWhenProcessKOBeforeLifecycleThenResponseKO()
        throws Exception {
        TransferNotificationActionHandler handler = new TransferNotificationActionHandler(mongoDbAccess);

        Mockito.doThrow(new LogbookDatabaseException("")).when(mongoDbAccess).getLogbookOperation(anyObject());
        Mockito.doReturn(null).when(mongoDbAccess).getLogbookLifeCycleObjectGroups(anyObject());
        Mockito.doReturn(null).when(mongoDbAccess).getLogbookLifeCycleUnits(anyObject());

        assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
        final CompositeItemStatus response = handler
            .execute(params.putParameterValue(WorkerParameterName.workflowStatusKo, Boolean.TRUE.toString()), action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }
    
    @Test
    public void givenExceptionLogbookLCUnitWhenProcessOKThenResponseKO()
        throws Exception {
        TransferNotificationActionHandler handler = new TransferNotificationActionHandler(mongoDbAccess);

        Mockito.doReturn(getLogbookOperation()).when(mongoDbAccess).getLogbookOperation(anyObject());
        Mockito.doReturn(null).when(mongoDbAccess).getLogbookLifeCycleObjectGroups(anyObject());
        Mockito.doThrow(new LogbookDatabaseException("")).when(mongoDbAccess).getLogbookLifeCycleUnits(anyObject());

        assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
        final CompositeItemStatus response = handler
            .execute(params.putParameterValue(WorkerParameterName.workflowStatusKo, Boolean.TRUE.toString()), action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void givenExceptionLogbookLCObjectWhenProcessOKThenResponseKO()
        throws Exception {
        TransferNotificationActionHandler handler = new TransferNotificationActionHandler(mongoDbAccess);

        Mockito.doReturn(getLogbookOperation()).when(mongoDbAccess).getLogbookOperation(anyObject());
        Mockito.doThrow(new LogbookDatabaseException("")).when(mongoDbAccess).getLogbookLifeCycleObjectGroups(anyObject());
        Mockito.doReturn(null).when(mongoDbAccess).getLogbookLifeCycleUnits(anyObject());

        assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
        final CompositeItemStatus response = handler
            .execute(params.putParameterValue(WorkerParameterName.workflowStatusKo, Boolean.TRUE.toString()), action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }
    
    private static LogbookOperation getLogbookOperation() throws FileNotFoundException, IOException {
        return new LogbookOperation(IOUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_OPERATION)));
    }

    private static LogbookLifeCycleObjectGroup getLogbookLifecycleGOT() throws FileNotFoundException, IOException {
        return new LogbookLifeCycleObjectGroup(IOUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_GOT)));
    }

    private static LogbookLifeCycleUnit getLogbookLifecycleAU() throws FileNotFoundException, IOException {
        return new LogbookLifeCycleUnit(IOUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_AU)));
    }

}
