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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TraceabilityJobTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    JobExecutionContext context;

    @InjectMocks
    private TraceabilityJob callTraceability;

    @Before
    public void setup() {
        doReturn(logbookOperationsClient).when(logbookOperationsClientFactory).getClient();
        VitamConfiguration.setAdminTenant(1);
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageLogBackupOKThenSuccess() throws Exception {
        // Given
        AtomicInteger tenantId = new AtomicInteger();
        doAnswer(args -> {
            assertThat(Thread.currentThread()).isInstanceOf(VitamThreadFactory.VitamThread.class);
            tenantId.set(VitamThreadUtils.getVitamSession().getTenantId());
            return new RequestResponseOK<>();
        })
            .when(logbookOperationsClient)
            .traceability(any());

        // When
        callTraceability.execute(context);

        // Then
        verify(logbookOperationsClient).traceability(eq(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)));
        verify(logbookOperationsClient).close();
        verifyNoMoreInteractions(logbookOperationsClient);
        assertThat(tenantId).hasValue(1);
    }

    @Test
    @RunWithCustomExecutor
    public void testStorageLogBackupKOThenException() throws Exception {
        // Given
        doThrow(new LogbookClientServerException("prb")).when(logbookOperationsClient).traceability(any());

        // When / Then
        assertThatThrownBy(() -> callTraceability.execute(context)).isInstanceOf(JobExecutionException.class);
        verify(logbookOperationsClient).traceability(any());
    }
}
