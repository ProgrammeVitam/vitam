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
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TraceabilityLFCJobTest {

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

    @Mock
    JobDetail jobDetail;

    @InjectMocks
    private TraceabilityLFCJob callTraceabilityLFC;

    @Before
    public void setup() {
        doReturn(logbookOperationsClient).when(logbookOperationsClientFactory).getClient();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("item", MetadataType.UNIT.getName());
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(context.getJobDetail().getJobDataMap()).thenReturn(jobDataMap);
        VitamConfiguration.setAdminTenant(1);
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    @RunWithCustomExecutor
    public void testTraceabilityLFCOKThenSuccess() throws Exception {
        // Given
        doReturn(new RequestResponseOK<String>().addAllResults(List.of("opId")))
            .when(logbookOperationsClient)
            .traceabilityLfcUnit();

        doReturn(new LifecycleTraceabilityStatus(true, false, "", false))
            .when(logbookOperationsClient)
            .checkLifecycleTraceabilityWorkflowStatus(anyString());

        // When
        callTraceabilityLFC.execute(context);

        // Then
        verify(logbookOperationsClient, times(10)).traceabilityLfcUnit();
        verify(logbookOperationsClient, times(10)).close();
        verify(logbookOperationsClient, atLeast(10)).checkLifecycleTraceabilityWorkflowStatus(anyString());
        verifyNoMoreInteractions(logbookOperationsClient);
    }

    @Test
    @RunWithCustomExecutor
    public void testTraceabilityLFCFatalThenOK() throws Exception {
        // Given
        doReturn(new RequestResponseOK<String>().addAllResults(List.of("opId")))
            .when(logbookOperationsClient)
            .traceabilityLfcUnit();

        doAnswer(args -> {
            assertThat(Thread.currentThread()).isInstanceOf(VitamThreadFactory.VitamThread.class);
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            if (tenantId == 2) {
                return new LifecycleTraceabilityStatus(false, true, "PAUSE.FATAL", false);
            }
            return new LifecycleTraceabilityStatus(true, false, "", false);
        })
            .when(logbookOperationsClient)
            .checkLifecycleTraceabilityWorkflowStatus(anyString());

        // When
        callTraceabilityLFC.execute(context);

        // Then
        verify(logbookOperationsClient, times(10)).traceabilityLfcUnit();
        verify(logbookOperationsClient, times(10)).close();
        verify(logbookOperationsClient, atLeast(10)).checkLifecycleTraceabilityWorkflowStatus(anyString());
        verifyNoMoreInteractions(logbookOperationsClient);
    }
}
