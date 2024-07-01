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
package fr.gouv.vitam.functional.administration.core.backup;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;

import static fr.gouv.vitam.storage.engine.common.model.DataCategory.REPORT;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BackupServiceTest {

    private static final String URI = "testFileName";
    private static BackupService backupService = new BackupService();
    private static WorkspaceClient workspaceClient;
    private static StorageClient storageClient;

    private final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
    private final StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
    private final String FILE_TO_SAVE = "accession-register.json";
    InputStream inputStream;

    @Before
    public void setUp() throws Exception {
        backupService = new BackupService(workspaceClientFactory, storageClientFactory);
        workspaceClient = mock(WorkspaceClient.class);
        storageClient = mock(StorageClient.class);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);
        inputStream = PropertiesUtils.getResourceAsStream(FILE_TO_SAVE);
    }

    @Test
    public void should_store_file() throws Exception {
        //Given
        final String uri = URI;
        ArgumentCaptor<ObjectDescription> objectDescriptionArgumentCaptor = ArgumentCaptor.forClass(
            ObjectDescription.class
        );
        //When
        backupService.backup(inputStream, REPORT, uri);

        //Then
        ArgumentCaptor<String> containerArgCaptor = ArgumentCaptor.forClass(String.class);
        verify(workspaceClient).putObject(containerArgCaptor.capture(), eq(uri), eq(inputStream));

        verify(storageClient).storeFileFromWorkspace(
            eq(VitamConfiguration.getDefaultStrategy()),
            eq(REPORT),
            eq(uri),
            objectDescriptionArgumentCaptor.capture()
        );
        ObjectDescription description = objectDescriptionArgumentCaptor.getValue();
        assertThat(description.getWorkspaceContainerGUID()).isEqualTo(containerArgCaptor.getValue());
        assertThat(description.getWorkspaceObjectURI()).isEqualTo(uri);

        verify(workspaceClient).deleteContainer(containerArgCaptor.getValue(), true);
    }

    @Test
    public void should_store_file_strategy() throws Exception {
        //Given
        final String uri = URI;
        ArgumentCaptor<ObjectDescription> objectDescriptionArgumentCaptor = ArgumentCaptor.forClass(
            ObjectDescription.class
        );
        //When
        backupService.backup(inputStream, UNIT, uri, "other_strategy");

        //Then
        ArgumentCaptor<String> containerArgCaptor = ArgumentCaptor.forClass(String.class);
        verify(workspaceClient).putObject(containerArgCaptor.capture(), eq(uri), eq(inputStream));

        verify(storageClient).storeFileFromWorkspace(
            eq("other_strategy"),
            eq(UNIT),
            eq(uri),
            objectDescriptionArgumentCaptor.capture()
        );
        ObjectDescription description = objectDescriptionArgumentCaptor.getValue();
        assertThat(description.getWorkspaceContainerGUID()).isEqualTo(containerArgCaptor.getValue());
        assertThat(description.getWorkspaceObjectURI()).isEqualTo(uri);

        verify(workspaceClient).deleteContainer(containerArgCaptor.getValue(), true);
    }

    @Test
    public void should_fail_when_storing_in_workSpace() throws Exception {
        //Given
        willThrow(ContentAddressableStorageServerException.class).given(workspaceClient).createContainer(any());
        //When
        final String description = "Unable to store file in workSpace";
        assertThatThrownBy(() -> backupService.backup(inputStream, REPORT, URI))
            .isInstanceOf(BackupServiceException.class)
            .hasMessageContaining(description);

        willThrow(ContentAddressableStorageServerException.class).given(workspaceClient).putObject(any(), any(), any());
        //When
        assertThatThrownBy(() -> backupService.backup(inputStream, REPORT, URI))
            .isInstanceOf(BackupServiceException.class)
            .hasMessageContaining(description);
    }

    @Test
    public void should_fail_when_storing_from_workSpace() throws Exception {
        //Given
        final String message = "Unable to store file from workSpace";

        willThrow(StorageAlreadyExistsClientException.class)
            .given(storageClient)
            .storeFileFromWorkspace(any(), any(), any(), any());
        //When
        assertThatThrownBy(() -> backupService.backup(inputStream, REPORT, URI))
            .isInstanceOf(BackupServiceException.class)
            .hasMessageContaining(message);
        //Given
        willThrow(StorageNotFoundClientException.class)
            .given(storageClient)
            .storeFileFromWorkspace(any(), any(), any(), any());
        //When
        assertThatThrownBy(() -> backupService.backup(inputStream, REPORT, URI))
            .isInstanceOf(BackupServiceException.class)
            .hasMessageContaining(message);

        //Given
        willThrow(StorageServerClientException.class)
            .given(storageClient)
            .storeFileFromWorkspace(any(), any(), any(), any());
        //When
        assertThatThrownBy(() -> backupService.backup(inputStream, REPORT, URI))
            .isInstanceOf(BackupServiceException.class)
            .hasMessageContaining(message);
    }

    @Test
    public void should_not_fail_clean_file_workSpace() throws Exception {
        //Given
        doThrow(ContentAddressableStorageNotFoundException.class)
            .when(workspaceClient)
            .deleteContainer(any(), anyBoolean());

        //When
        backupService.backup(inputStream, REPORT, URI);
        verify(workspaceClient).deleteContainer(any(), anyBoolean());
    }
}
