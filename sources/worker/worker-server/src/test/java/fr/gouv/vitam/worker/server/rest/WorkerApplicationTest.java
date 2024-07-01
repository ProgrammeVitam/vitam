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
package fr.gouv.vitam.worker.server.rest;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.fail;

/**
 *
 */
public class WorkerApplicationTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerApplicationTest.class);
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String WORKER_CONF = "worker-test.conf";
    private static int serverPort;
    private static int oldPort;
    private static JunitHelper junitHelper;
    private static File worker;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        worker = PropertiesUtils.findFile(WORKER_CONF);
        serverPort = junitHelper.findAvailablePort();
        // TODO verifier la compatibilité avec les tests parallèles sur jenkins

        final WorkerConfiguration realWorker = PropertiesUtils.readYaml(worker, WorkerConfiguration.class);
        realWorker
            .setRegisterServerPort(serverPort)
            .setRegisterServerHost("localhost")
            .setRegisterDelay(1)
            .setRegisterRetry(1)
            .setProcessingUrl("http://localhost:8888")
            .setUrlMetadata("http://localhost:8888")
            .setUrlWorkspace("http://localhost:8888");

        final File newWorkerConf = File.createTempFile("test", WORKER_CONF, worker.getParentFile());
        PropertiesUtils.writeYaml(newWorkerConf, realWorker);
        worker = newWorkerConf;

        oldPort = VitamServerFactory.getDefaultPort();
        VitamServerFactory.setDefaultPort(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        junitHelper.releasePort(serverPort);
        VitamServerFactory.setDefaultPort(oldPort);
        VitamClientFactory.resetConnections();
    }

    @Test
    public final void testFictiveLaunch() throws Exception {
        try {
            WorkerMain application = new WorkerMain(worker.getAbsolutePath());
            Assert.assertFalse(application.getVitamStarter().isStarted());
            application.start();
            Assert.assertTrue(application.getVitamStarter().isStarted());
            application.stop();
            Assert.assertFalse(application.getVitamStarter().isStarted());
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }
}
