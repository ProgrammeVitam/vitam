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
package fr.gouv.vitam.metadata.core.ExportsPurge;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.metadata.core.config.TimeToLiveConfiguration;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static fr.gouv.vitam.common.model.WorkspaceConstants.FREESPACE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExportsPurgeServiceTest {

    private static final String DIP_CONTAINER = "DIP";

    private static final int MIN_TIME_TO_LIVE = 1;
    private static final int MAX_TIME_TO_LIVE = 10;

    private WorkspaceClient workspaceClient;
    private ExportsPurgeService exportsPurgeService;
    private StorageClient storageClient;

    @Before
    public void setUp() throws Exception {
        workspaceClient = mock(WorkspaceClient.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();

        StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
        storageClient = mock(StorageClient.class);
        doReturn(storageClient).when(storageClientFactory).getClient();

        exportsPurgeService = new ExportsPurgeService(
            workspaceClientFactory,
            storageClientFactory,
            new TimeToLiveConfiguration(MAX_TIME_TO_LIVE, MIN_TIME_TO_LIVE, MAX_TIME_TO_LIVE)
        );
    }

    @Test
    public void migrationPurgeDipFilesFromOffersTest() throws Exception {
        // Given
        List<OfferLog> offerLogs = Arrays.asList(
            new OfferLog(1L, LocalDateUtil.now(), "container", "file1", OfferLogAction.WRITE),
            new OfferLog(2L, LocalDateUtil.now(), "container", "file2", OfferLogAction.WRITE),
            new OfferLog(3L, LocalDateUtil.now(), "container", "file2", OfferLogAction.DELETE)
        );

        doReturn(new RequestResponseOK<OfferLog>().addAllResults(offerLogs))
            .when(storageClient)
            .getOfferLogs(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.DIP,
                null,
                VitamConfiguration.getChunkSize(),
                Order.ASC
            );

        when(workspaceClient.getFreespacePercent()).thenReturn((JsonHandler.createObjectNode().put(FREESPACE, 50)));

        // When
        exportsPurgeService.migrationPurgeDipFilesFromOffers();

        // Then
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.DIP, "file1");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.DIP, "file2");
    }

    @Test
    public void purgeExpiredDipAndUseCriticalTimeToLiveWhenFreespaceLowerThanThreshold() throws Exception {
        // Given
        when(workspaceClient.getFreespacePercent()).thenReturn(
            (JsonHandler.createObjectNode().put(FREESPACE, VitamConfiguration.getWorkspaceFreespaceThreshold() - 1))
        );

        // When
        exportsPurgeService.purgeExpiredFiles(DIP_CONTAINER);

        // Then
        verify(workspaceClient).purgeOldFilesInContainer(
            DIP_CONTAINER,
            new TimeToLive(MIN_TIME_TO_LIVE, ChronoUnit.MINUTES)
        );
    }

    @Test
    public void purgeExpiredDipAndUseUsualTimeToLiveWhenFreespaceGreaterThanThreshold() throws Exception {
        // Given
        when(workspaceClient.getFreespacePercent()).thenReturn(
            (JsonHandler.createObjectNode().put(FREESPACE, VitamConfiguration.getWorkspaceFreespaceThreshold() + 1))
        );

        // When
        exportsPurgeService.purgeExpiredFiles(DIP_CONTAINER);

        // Then
        verify(workspaceClient).purgeOldFilesInContainer(
            DIP_CONTAINER,
            new TimeToLive(MAX_TIME_TO_LIVE, ChronoUnit.MINUTES)
        );
    }
}
