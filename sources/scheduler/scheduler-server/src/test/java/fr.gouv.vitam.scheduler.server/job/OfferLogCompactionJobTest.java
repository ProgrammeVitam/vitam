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

package fr.gouv.vitam.scheduler.server.job;

import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class OfferLogCompactionJobTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    JobDetail jobDetail;

    @Mock
    JobExecutionContext context;

    @Mock
    private StorageClient storageClient;

    private OfferLogCompactionJob offerLogCompactionJob;

    @Before
    public void setup() throws StorageException {
        MockitoAnnotations.initMocks(this);
        Mockito.reset(storageClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        offerLogCompactionJob = new OfferLogCompactionJob(storageClientFactory);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("offer_storage_id", "default");
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(context.getJobDetail().getJobDataMap()).thenReturn(jobDataMap);
    }

    @Test
    public void testLaunchOfferLogCompactionSuccess() throws Exception {
        // Given
        when(storageClient.launchOfferLogCompaction(any(), any())).thenReturn(null);
        // When
        offerLogCompactionJob.execute(context);
        // Then
        verify(storageClient, times(1)).launchOfferLogCompaction(any(), any());
        verify(storageClient).close();
        verifyNoMoreInteractions(storageClient);
    }
}
