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

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.functional.administration.agencies.api.AgenciesService.AGENCIES_IMPORT_EVENT;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventType;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventTypeProcess;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

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
    public void logStarted() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final LogbookOperationParameters[] result = new LogbookOperationParameters[1];
        doAnswer(invocationOnMock ->
        {
            LogbookOperationParameters parameters = invocationOnMock.getArgumentAt(0, LogbookOperationParameters.class);
            result[0] = LogbookOperationsClientHelper.copy(parameters);
            return null;

        }).when(
            logbookOperationsClient).create(any()
        );
        AgenciesManager manager =
            new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(TENANT_ID), false);
        manager.logStarted(AGENCIES_IMPORT_EVENT);
        LogbookOperationParameters log = result[0];

        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("STARTED");

    }

    @Test
    @RunWithCustomExecutor
    public void logFinish() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final LogbookOperationParameters[] result = new LogbookOperationParameters[1];
        doAnswer(invocationOnMock ->
        {
            LogbookOperationParameters parameters = invocationOnMock.getArgumentAt(0, LogbookOperationParameters.class);
            result[0] = LogbookOperationsClientHelper.copy(parameters);
            return null;

        }).when(
            logbookOperationsClient).update(any()
        );
        AgenciesManager manager = new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(0), false);

        manager.logFinish();
        LogbookOperationParameters log = result[0];

        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("OK");


        manager = new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(0), true);
        manager.logFinish();
        log = result[0];


        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("WARNING");


    }


    @Test
    @RunWithCustomExecutor
    public void logError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final LogbookOperationParameters[] result = new LogbookOperationParameters[1];
        doAnswer(invocationOnMock ->
        {
            LogbookOperationParameters parameters = invocationOnMock.getArgumentAt(0, LogbookOperationParameters.class);
            result[0] = LogbookOperationsClientHelper.copy(parameters);
            return null;

        }).when(
            logbookOperationsClient).update(any()
        );
        AgenciesManager manager = new AgenciesManager(logbookOperationsClient, newOperationLogbookGUID(0));

        manager.logError("ErorMessage");
        LogbookOperationParameters log = result[0];

        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("KO");
    }


}
