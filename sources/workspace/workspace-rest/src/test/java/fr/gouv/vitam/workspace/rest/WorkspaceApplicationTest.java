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
package fr.gouv.vitam.workspace.rest;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.workspace.core.StorageConfiguration;

public class WorkspaceApplicationTest {
    private static final String CONFIG_FILE_NAME = "workspace-test.conf";

    private WorkspaceApplication application;
    private static JunitHelper junitHelper;
    private static int port;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins
    }

    @AfterClass
    public static void shutdownAfterClass() {
        junitHelper.releasePort(port);
    }

    @Test(expected = IllegalStateException.class)
    public void givenEmptyArgsWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application = new WorkspaceApplication((String) null);
    }

    @Test(expected = Exception.class)
    public void givenFileNotFoundWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application = new WorkspaceApplication("notFound.conf");
    }

    @Test
    public void givenFileAlreadyExistsWhenConfigureApplicationOKThenRunServer() throws Exception {
        application = new WorkspaceApplication(CONFIG_FILE_NAME);
        application.stop();
    }

    @Test
    public void givenConfigFileWhenGetConfigThenReturnCorrectConfig() {
        application = new WorkspaceApplication(CONFIG_FILE_NAME);
        assertEquals("workspace-test.conf", application.getConfigFilename());
    }

    @Test(expected = Exception.class)
    public void givenNullConfigWhenRunAppThenThrowException() throws Exception {
        final StorageConfiguration config = new StorageConfiguration();
        application = new WorkspaceApplication(config);
    }

    @Test(expected = Exception.class)
    public void givenNullConfigWhenStartAppThenThrowException() throws Exception {
        application = new WorkspaceApplication((StorageConfiguration) null);
    }


}
