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
package fr.gouv.vitam.worker.server.registration;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkerRegistrationListenerTest {

    private static final WorkerBean WORKER_DESCRIPTION;

    static {
        try {
            WORKER_DESCRIPTION = JsonHandler.getFromString(
                "{ \"name\" : \"workername\", \"workerId\":\"workerId\", \"family\" : \"DefaultWorker1\", \"capacity\" : 2, \"storage\" : 100, \"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }",
                WorkerBean.class
            );
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TempFolderRule tempFolderRule = new TempFolderRule();

    @Mock
    private ProcessingManagementClientFactory processingManagementClientFactory;

    @Mock
    private ProcessingManagementClient processingManagementClient;

    @Before
    public void setup() throws Exception {
        reset(processingManagementClientFactory);
        reset(processingManagementClient);

        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);
    }

    @Test
    public void test_on_contextInitialized_schedule_registration_on_contextDestroyed_unregister_ok() throws Exception {
        WorkerConfiguration workerConfiguration = new WorkerConfiguration();
        workerConfiguration.setCapacity(1);
        workerConfiguration.setRegisterDelay(1);
        workerConfiguration.setWorkerFamily("family_id");
        workerConfiguration.setUrlMetadata("localhost");
        workerConfiguration.setUrlWorkspace("localhost");
        workerConfiguration.setProcessingUrl("localhost");
        workerConfiguration.setRegisterServerHost("localhost");
        workerConfiguration.setRegisterServerPort(80);

        WorkerRegistrationListener workerRegistrationListener = new WorkerRegistrationListener(
            workerConfiguration,
            processingManagementClientFactory
        );

        workerRegistrationListener.contextInitialized(null);

        TimeUnit.MILLISECONDS.sleep(2500);

        // because of setRegisterDelay(2) then 2100 milliseconds > 2 seconds so at least 2 invocation
        verify(processingManagementClient, VerificationModeFactory.atLeast(2)).registerWorker(
            anyString(),
            anyString(),
            any()
        );

        workerRegistrationListener.contextDestroyed(null);

        TimeUnit.MILLISECONDS.sleep(500);

        verify(processingManagementClient, VerificationModeFactory.atLeastOnce()).unregisterWorker(
            anyString(),
            anyString()
        );
    }
}
