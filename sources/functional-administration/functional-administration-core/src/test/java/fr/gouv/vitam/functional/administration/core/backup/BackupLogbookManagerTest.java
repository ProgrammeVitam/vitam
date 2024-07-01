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

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.security.SanityChecker;
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

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventDetailData;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventType;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventTypeProcess;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class BackupLogbookManagerTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    public static Integer TENANT_ID = 0;
    private LogbookOperationsClient logbookOperationsClient;

    @Before
    public void setUp() throws Exception {
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        logbookOperationsClient = mock(LogbookOperationsClient.class);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
    }

    @Test
    @RunWithCustomExecutor
    public void should_log_Event_success() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        //Given
        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);
        BackupLogbookManager manager = new BackupLogbookManager(logbookOperationsClientFactory);

        Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
        digest.update("toto".getBytes());
        ObjectNode evdetData = JsonHandler.createObjectNode();
        evdetData.put(BackupLogbookManager.FILE_NAME, "toto.json");
        evdetData.put(BackupLogbookManager.DIGEST, digest.digestHex());
        evdetData.put(BackupLogbookManager.DIGESTTYPE, VitamConfiguration.getDefaultDigestType().getName());

        // When
        manager.logEventSuccess(newOperationLogbookGUID(0), "STP_TEST", digest.digestHex(), "toto.json", null);

        // Then
        verify(logbookOperationsClient).update(captor.capture());
        LogbookOperationParameters parameter = captor.getValue();

        assertThat(parameter.getParameterValue(eventType)).isEqualTo("STP_TEST");
        assertThat(parameter.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(parameter.getParameterValue(outcome)).isEqualTo("OK");

        assertThat(parameter.getParameterValue(eventDetailData)).isEqualTo(evdetData.toString());
    }

    @Test
    @RunWithCustomExecutor
    public void should_log_error() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        //Given
        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);
        BackupLogbookManager manager = new BackupLogbookManager(logbookOperationsClientFactory);
        ObjectNode evdetData = JsonHandler.createObjectNode();
        evdetData.put(BackupLogbookManager.ERROR_MESSAGE, "errorMessage");
        // When
        manager.logError(newOperationLogbookGUID(0), "STP_TEST", "errorMessage");

        // Then
        verify(logbookOperationsClient).update(captor.capture());
        LogbookOperationParameters parameter = captor.getValue();

        assertThat(parameter.getParameterValue(eventType)).isEqualTo("STP_TEST");
        assertThat(parameter.getParameterValue(eventTypeProcess)).isEqualTo("MASTERDATA");
        assertThat(parameter.getParameterValue(outcome)).isEqualTo("KO");
        assertThat(parameter.getParameterValue(eventDetailData)).isEqualTo(SanityChecker.sanitizeJson(evdetData));
    }
}
