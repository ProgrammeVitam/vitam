package fr.gouv.vitam.functional.administration.agencies.api;

import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.functional.administration.agencies.api.AgenciesService.AGENCIES_IMPORT_EVENT;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventType;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventTypeProcess;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcome;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcomeDetail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AgenciesManagerTest {

    private static final Integer TENANT_ID = 0;
    private static LogbookOperationsClient logbookOperationsClient;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Before
    public void setUp() throws Exception {
        logbookOperationsClient = mock(LogbookOperationsClient.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_logbookStarted() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);

        AgenciesManager manager =
            new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(TENANT_ID), false);

        // When
        manager.logStarted(AGENCIES_IMPORT_EVENT);

        // Then
        verify(logbookOperationsClient).create(captor.capture());
        LogbookOperationParameters log = captor.getValue();

        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("STARTED");

    }

    @Test
    @RunWithCustomExecutor
    public void should_event_logbook_ok_when_call_log_finish() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);

        AgenciesManager manager = new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(0), false);

        // When
        manager.logFinish();

        //THEN
        verify(logbookOperationsClient, times(1)).update(captor.capture());
        LogbookOperationParameters log = captor.getValue();

        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("OK");

        manager = new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(0), true);

        manager.logFinish();

        verify(logbookOperationsClient, times(2)).update(captor.capture());

        log = captor.getValue();

        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("WARNING");
    }


    @Test
    @RunWithCustomExecutor
    public void logError() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);
        AgenciesManager manager = new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(0));

        //When
        manager.logError("ErorMessage", null);
        verify(logbookOperationsClient).update(captor.capture());
        LogbookOperationParameters log = captor.getValue();
        //Then
        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("KO");
    }

    @Test
    @RunWithCustomExecutor
    public void logErrorWithDetails() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);
        AgenciesManager manager = new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(0));

        //When
        manager.logError("ErorMessage", "DELETION");
        verify(logbookOperationsClient).update(captor.capture());
        LogbookOperationParameters log = captor.getValue();
        //Then
        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("KO");
        assertThat(log.getParameterValue(outcomeDetail)).isEqualTo(AGENCIES_IMPORT_EVENT + ".DELETION.KO");
    }


}
