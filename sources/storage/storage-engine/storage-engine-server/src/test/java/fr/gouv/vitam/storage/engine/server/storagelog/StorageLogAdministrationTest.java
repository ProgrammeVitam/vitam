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

package fr.gouv.vitam.storage.engine.server.storagelog;

import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.driver.model.StorageLogBackupResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StorageLogAdministrationTest {

    private static final String STRATEGY_ID = "strategyId";

    private static final int TENANT_ID = 0;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    final StorageConfiguration configuration = new StorageConfiguration();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private StorageLog storageService;
    @Mock
    private WorkspaceClient workspaceClient;
    @Mock
    private StorageClient storageClient;
    @Mock
    private LogbookOperationsClient logbookOperationsClient;


    private StorageLogAdministration storageLogAdministration;

    @Before
    public void setUp() {
        final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        final StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        final LogbookOperationsClientFactory logbookOperationsClientFactory =
            mock(LogbookOperationsClientFactory.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        storageLogAdministration = new StorageLogAdministration(storageService,
            configuration, workspaceClientFactory, storageClientFactory, logbookOperationsClientFactory);
    }



    @Test
    public void should_execute_storagelog_backup() throws Exception {

        when(storageService.rotateLogFile(eq(TENANT_ID), eq(true))).thenReturn(
            List.of(new LogInformation(tempFolder.newFile().toPath(), LocalDateTime.now(), LocalDateTime.now()))
        );

        final List<StorageLogBackupResult> storageLogBackupResults =
            storageLogAdministration.backupStorageLog(STRATEGY_ID, true, List.of(TENANT_ID));


        assertThat(storageLogBackupResults).isNotEmpty();
        assertThat(storageLogBackupResults).extracting(StorageLogBackupResult::getTenantId).contains(TENANT_ID);

        verify(storageClient).storeFileFromWorkspace(eq(STRATEGY_ID), eq(DataCategory.STORAGELOG), anyString(),
            any(ObjectDescription.class));
        verify(workspaceClient).deleteContainer(anyString(), anyBoolean());
        verify(logbookOperationsClient).bulkCreate(anyString(), any());
    }

    @Test
    public void should_execute_storageaccesslog_backup() throws Exception {
        when(storageService.rotateLogFile(eq(TENANT_ID), eq(false))).thenReturn(
            List.of(new LogInformation(tempFolder.newFile().toPath(), LocalDateTime.now(), LocalDateTime.now()))
        );

        final List<StorageLogBackupResult> storageLogBackupResults =
            storageLogAdministration.backupStorageLog(STRATEGY_ID, false, List.of(TENANT_ID));

        assertThat(storageLogBackupResults).isNotEmpty();
        assertThat(storageLogBackupResults).extracting(StorageLogBackupResult::getTenantId).contains(TENANT_ID);

        verify(storageClient).storeFileFromWorkspace(eq(STRATEGY_ID), eq(DataCategory.STORAGEACCESSLOG), anyString(),
            any(ObjectDescription.class));
        verify(workspaceClient).deleteContainer(anyString(), anyBoolean());
        verify(logbookOperationsClient).bulkCreate(anyString(), any());
    }

    @Test
    public void should_delete_container_when_exception_raised() throws Exception {
        when(storageService.rotateLogFile(eq(TENANT_ID), eq(false))).thenReturn(
            List.of(new LogInformation(tempFolder.newFile().toPath(), LocalDateTime.now(), LocalDateTime.now())));

        when(storageClient.storeFileFromWorkspace(eq(STRATEGY_ID), eq(DataCategory.STORAGEACCESSLOG), anyString(),
            any())).thenThrow(
            StorageServerClientException.class
        );

        assertThatCode(
            () -> storageLogAdministration.backupStorageLog(STRATEGY_ID, false, List.of(TENANT_ID))).isInstanceOf(
            StorageLogException.class).hasMessageContaining("One or more StorageAccessLog operations failed");

        // ensure workspace clean
        verify(workspaceClient).deleteContainer(anyString(), anyBoolean());
        // ensure logbook creation
        verify(logbookOperationsClient).bulkCreate(anyString(), any());
    }
}