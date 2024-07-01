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
package fr.gouv.vitam.ingest.external.rest;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class IngestExternalApplicationTest {

    private IngestExternalMain application;
    private final JunitHelper junitHelper = JunitHelper.getInstance();
    private int portAvailable;
    private static IngestExternalConfiguration realIngest;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String configurationFile;

    @Before
    public void setUpBeforeMethod() throws Exception {
        portAvailable = junitHelper.findAvailablePort();
        File file = temporaryFolder.newFile();
        configurationFile = file.getAbsolutePath();
        PropertiesUtils.writeYaml(file, realIngest);
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (application != null) {
                application.stop();
            }
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
        junitHelper.releasePort(portAvailable);
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithEmptyArgs() throws Exception {
        application = new IngestExternalMain((String) null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithFileNotFound() throws Exception {
        application = new IngestExternalMain("notFound.conf");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithWrongConfigurationFile() throws Exception {
        application = new IngestExternalMain("ingest-external-err1.conf");
    }

    @Test
    public void shouldRunServerWhenConfigureApplicationWithFileExists() throws Exception {
        application = new IngestExternalMain("ingest-external-test.conf");
        Assert.assertFalse(application.getVitamStarter().isStarted());
        application.start();
        Assert.assertTrue(application.getVitamStarter().isStarted());
        application.stop();
        Assert.assertFalse(application.getVitamStarter().isStarted());
    }

    @Test
    public void shouldRunServerWhenConfigureApplicationWithSslConfiguration() throws Exception {
        application = new IngestExternalMain("ingest-external-ssl1-test.conf");
        Assert.assertFalse(application.getVitamStarter().isStarted());
        application.start();
        Assert.assertTrue(application.getVitamStarter().isStarted());
        application.stop();
        Assert.assertFalse(application.getVitamStarter().isStarted());
    }
}
