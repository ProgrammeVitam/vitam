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
package fr.gouv.vitam.logbook.operations.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogbookOperationsDecoratorTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private LogbookOperationsImpl logbookOperationsImpl;
    private LogbookOperationParameters logbookParameters;
    LogbookOperationParameters[] operationArray;
    private String eventType = "STP_IMPORT_ACCESS_CONTRACT";
    private String outcome = "OK";
    private LogbookDbAccess mongoDbAccess;
    private WorkspaceClient workspaceClient;


    private static class TestClass extends LogbookOperationsDecorator {
        TestClass(LogbookOperations logbookOperations) {
            super(logbookOperations);
        }

        @Override
        public RequestResponse<LogbookOperation> selectOperations(JsonNode select) {
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        // Mock workspace and mongoDbAccess to avoid error on operation backup
        final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);

        mongoDbAccess = mock(LogbookDbAccess.class);
        StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
        when(storageClientFactory.getClient()).thenReturn(mock(StorageClient.class));
        logbookOperationsImpl = new LogbookOperationsImpl(mongoDbAccess, workspaceClientFactory, storageClientFactory, mock(IndexationHelper.class));
        logbookOperationsImpl = Mockito.spy(logbookOperationsImpl);
        logbookParameters = LogbookParametersFactory.newLogbookOperationParameters();
        logbookParameters.putParameterValue(LogbookParameterName.eventType, eventType);
        logbookParameters.putParameterValue(LogbookParameterName.outcome, outcome);
        logbookParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, GUIDFactory
            .newOperationLogbookGUID(0).getId());
        operationArray = new LogbookOperationParameters[] {logbookParameters};

        workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        doNothing().when(workspaceClient).createContainer(anyString());
        doNothing().when(workspaceClient).putObject(anyString(), anyString(), any());
        doNothing().when(workspaceClient).deleteObject(anyString(), anyString());
        when(mongoDbAccess.getLogbookOperation(anyString())).thenReturn(new LogbookOperation(logbookParameters));
    }

    @RunWithCustomExecutor
    @Test
    public final void testCreate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final TestClass tc = new TestClass(logbookOperationsImpl);
        tc.create(logbookParameters);
        verify(logbookOperationsImpl).create(logbookParameters);
        verify(mongoDbAccess).createLogbookOperation(logbookParameters);
    }

    @RunWithCustomExecutor
    @Test
    public final void testUpdate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final TestClass tc = new TestClass(logbookOperationsImpl);
        tc.update(logbookParameters);
        verify(logbookOperationsImpl).update(logbookParameters);
        verify(mongoDbAccess).updateLogbookOperation(logbookParameters);
    }

    @RunWithCustomExecutor
    @Test
    public final void testUpdateBulkLogbookOperation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final TestClass tc = new TestClass(logbookOperationsImpl);
        tc.updateBulkLogbookOperation(operationArray);
        verify(mongoDbAccess).updateBulkLogbookOperation(operationArray);
    }

    @RunWithCustomExecutor
    @Test
    public final void testCreateBulkLogbookOperation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final TestClass tc = new TestClass(logbookOperationsImpl);
        tc.createBulkLogbookOperation(operationArray);
        verify(mongoDbAccess).createBulkLogbookOperation(operationArray);
    }



}
