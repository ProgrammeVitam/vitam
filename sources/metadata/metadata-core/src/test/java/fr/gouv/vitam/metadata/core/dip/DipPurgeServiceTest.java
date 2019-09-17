package fr.gouv.vitam.metadata.core.dip;

import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Test;

import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DipPurgeServiceTest {

    @Test
    public void purgeExpiredDipFilesTest() throws ContentAddressableStorageServerException {

        // Given
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();
        DipPurgeService dipPurgeService = new DipPurgeService(workspaceClientFactory, 10);

        // When
        dipPurgeService.purgeExpiredDipFiles();

        // Then
        verify(workspaceClient).purgeOldFilesInContainer("DIP", new TimeToLive(10, ChronoUnit.MINUTES));
    }
}
