/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ingest.upload.rest;

import static org.assertj.core.api.Assertions.assertThat;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.Spy;

import fr.gouv.vitam.common.junit.JunitHelper;

import java.io.IOException;

/**
 * Tests the class MainUpload
 */
public class MainUploadTest {

    private static JunitHelper junitHelper;

    @Spy
    private MainUpload mainUpload;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = new JunitHelper();
    }

    @AfterClass
    public static void shutdownAfterClass() {

    }

    @Before
    public void setUp() {

    }

    @Test(expected = Exception.class)
    public void givenEmptyPort_WhenConfigureApplication_ThenRaiseAnException() throws Exception {
        MainUpload.serverInitialisation((Integer) null);
    }

    @Test
    public void givenPort_WhenConfigureApplication_ThenServerStartedOK() throws Exception {
        // Server started
        int port = junitHelper.findAvailablePort();
        MainUpload.serverInitialisation(port);
        MainUpload.serverStart();
        assertThat(MainUpload.getServer()).isNotNull();
        assertThat(MainUpload.getServer().isStarted()).isTrue();

        // Server stopped
        MainUpload.stopServer();
        assertThat(MainUpload.getServer().isStopped()).isTrue();
        junitHelper.releasePort(port);
    }

    @Test
    public void givenMain_WhenConfiguredApplicaiotn_ThenStartApplication() throws Exception {
        String[] args = {"ingest-rest-test.properties"};
        MainUpload.serverInitialisation(args);
        int port = junitHelper.findAvailablePort();
        MainUpload.serverInitialisation(port);
        MainUpload.serverStart();

        // Server stopped
        MainUpload.stopServer();
        assertThat(MainUpload.getServer().isStopped()).isTrue();
        junitHelper.releasePort(port);
    }

    @Test(expected = VitamException.class)
    public void givenMain_WhenThrowIOException_ThenThrowVitamException() throws IOException, VitamException {
        String[] args = {"mainFake", "fake.properties"};
        MainUpload.serverInitialisation(args);
    }
}
