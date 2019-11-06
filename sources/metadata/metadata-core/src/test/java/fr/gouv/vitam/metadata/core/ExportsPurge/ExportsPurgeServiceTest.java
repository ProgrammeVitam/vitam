package fr.gouv.vitam.metadata.core.ExportsPurge;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ExportsPurgeServiceTest {

    private static final String DIP_CONTAINER = "DIP";
    @Test
    public void purgeExpiredDipFilesTest() throws ContentAddressableStorageServerException {

        // Given
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();
        ExportsPurgeService exportsPurgeService = new ExportsPurgeService(workspaceClientFactory, null, 10);

        // When
        exportsPurgeService.purgeExpiredFiles(DIP_CONTAINER);

        // Then
        verify(workspaceClient).purgeOldFilesInContainer(DIP_CONTAINER, new TimeToLive(10, ChronoUnit.MINUTES));
    }

    @Test
    public void migrationPurgeDipFilesFromOffersTest() throws StorageServerClientException {

        // Given
        StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
        StorageClient storageClient = mock(StorageClient.class);
        doReturn(storageClient).when(storageClientFactory).getClient();

        List<OfferLog> offerLogs = Arrays.asList(
            new OfferLog(1L, LocalDateUtil.now(), "container", "file1", OfferLogAction.WRITE),
            new OfferLog(2L, LocalDateUtil.now(), "container", "file2", OfferLogAction.WRITE),
            new OfferLog(3L, LocalDateUtil.now(), "container", "file2", OfferLogAction.DELETE)
        );

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(offerLogs))
            .when(storageClient).getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.DIP,
                null, VitamConfiguration.getChunkSize(), Order.ASC);

        ExportsPurgeService exportsPurgeService = new ExportsPurgeService(null, storageClientFactory, 10);

        // When
        exportsPurgeService.migrationPurgeDipFilesFromOffers();

        // Then
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.DIP, "file1");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.DIP, "file2");
    }
}
