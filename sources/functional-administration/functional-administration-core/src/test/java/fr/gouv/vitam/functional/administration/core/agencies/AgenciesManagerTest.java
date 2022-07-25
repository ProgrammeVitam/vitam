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
package fr.gouv.vitam.functional.administration.core.agencies;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static fr.gouv.vitam.functional.administration.core.agencies.AgenciesService.AGENCIES_IMPORT_EVENT;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventDetailData;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventType;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventTypeProcess;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcome;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcomeDetail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgenciesManagerTest {

    private static final Integer TENANT_ID = 0;
    private static final String TEST_FILENAME = "test.json";
    private static LogbookOperationsClientFactory logbookOperationsClientFactory;
    private static LogbookOperationsClient logbookOperationsClient;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Before
    public void setUp() {
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        logbookOperationsClient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
    }

    @Test
    @RunWithCustomExecutor
    public void should_logbookStarted() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);


        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);

        LogbookAgenciesImportManager manager = new LogbookAgenciesImportManager(logbookOperationsClientFactory);

        // When
        manager.logStarted(GUIDFactory.newOperationLogbookGUID(TENANT_ID), AGENCIES_IMPORT_EVENT);

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

        LogbookAgenciesImportManager manager = new LogbookAgenciesImportManager(logbookOperationsClientFactory);

        // When
        manager.logFinishSuccess(GUIDFactory.newOperationLogbookGUID(TENANT_ID), TEST_FILENAME, StatusCode.OK);

        //THEN
        verify(logbookOperationsClient, times(1)).update(captor.capture());
        LogbookOperationParameters log = captor.getValue();

        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("OK");

        manager = new LogbookAgenciesImportManager(logbookOperationsClientFactory);

        manager.logFinishSuccess(GUIDFactory.newOperationLogbookGUID(TENANT_ID), TEST_FILENAME, StatusCode.WARNING);

        verify(logbookOperationsClient, times(2)).update(captor.capture());

        log = captor.getValue();

        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("WARNING");
        assertThat(log.getParameterValue(eventDetailData)).isEqualTo("{\"FileName\":\"test.json\"}");

    }


    @Test
    @RunWithCustomExecutor
    public void logError() throws Exception {

        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);
        LogbookAgenciesImportManager
            manager = new LogbookAgenciesImportManager(logbookOperationsClientFactory);

        //When
        manager.logError(GUIDFactory.newOperationLogbookGUID(TENANT_ID), "ErorMessage", null);
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
        LogbookAgenciesImportManager
            manager = new LogbookAgenciesImportManager(logbookOperationsClientFactory);

        //When
        manager.logError(GUIDFactory.newOperationLogbookGUID(TENANT_ID), "ErorMessage", "DELETION");
        verify(logbookOperationsClient).update(captor.capture());
        LogbookOperationParameters log = captor.getValue();
        //Then
        assertThat(log.getParameterValue(eventType)).isEqualTo(AGENCIES_IMPORT_EVENT);
        assertThat(log.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(log.getParameterValue(outcome)).isEqualTo("KO");
        assertThat(log.getParameterValue(outcomeDetail)).isEqualTo(AGENCIES_IMPORT_EVENT + ".DELETION.KO");
    }
}
