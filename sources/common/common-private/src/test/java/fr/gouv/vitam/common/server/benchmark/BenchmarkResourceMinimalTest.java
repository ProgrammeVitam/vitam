/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.server.benchmark;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

public class BenchmarkResourceMinimalTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BenchmarkResourceMinimalTest.class);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static final String BENCHMARK_CONF = "benchmark-test.conf";
    private static BenchmarkApplication application;
    private static int serverPort = 8889;
    private static final int NB_CLIENT = VitamConfiguration.getMaxClientPerHost();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MinimalTestVitamApplicationFactory<BenchmarkApplication> testFactory =
            new MinimalTestVitamApplicationFactory<BenchmarkApplication>() {

                @Override
                public StartApplicationResponse<BenchmarkApplication> startVitamApplication(int reservedPort)
                    throws IllegalStateException {
                    final BenchmarkApplication application = new BenchmarkApplication(BENCHMARK_CONF);
                    return startAndReturn(application);
                }

            };
        final StartApplicationResponse<BenchmarkApplication> response =
            testFactory.findAvailablePortSetToApplication();
        serverPort = response.getServerPort();
        application = response.getApplication();
        BenchmarkClientFactory.setConfiguration(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        JunitHelper.getInstance().releasePort(serverPort);
    }

    @Test
    public void testBadMinimalTestInitialization() {
        final MinimalTestVitamApplicationFactory<BenchmarkApplication> testFactory =
            new MinimalTestVitamApplicationFactory<BenchmarkApplication>() {

                @Override
                public StartApplicationResponse<BenchmarkApplication> startVitamApplication(int reservedPort)
                    throws IllegalStateException {
                    throw new IllegalStateException("Test should failed");
                }

            };
        try {
            testFactory.findAvailablePortSetToApplication();
            fail("Should raized an exception");
        } catch (final IllegalStateException e) {
            // nothing to do
        }
    }

    @Test
    public final void testStatus() {
        try (BenchmarkClientRest client = BenchmarkClientFactory.getInstance().getClient()) {
            client.checkStatus();
        } catch (final VitamApplicationServerException e) {
            fail("Cannot connect to server");
        }
        assertNotNull(BenchmarkClientFactory.getInstance().getDefaultConfigCient());
    }

    @RunWithCustomExecutor
    @Test
    public void multipleClientTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        VitamThreadUtils.getVitamSession().setContractId("contractId");
        assertNotNull(BenchmarkClientFactory.getInstance().getDefaultConfigCient());
        try (BenchmarkClientRest client = BenchmarkClientFactory.getInstance().getClient()) {
            for (int i = 0; i < NB_CLIENT; i++) {
                client.checkStatus();
            }
        }
        for (int j = 0; j < NB_CLIENT; j++) {
            try (BenchmarkClientRest client = BenchmarkClientFactory.getInstance().getClient()) {
                for (int i = 0; i < 10; i++) {
                    client.checkStatus();
                }
            }
        }
    }
}
