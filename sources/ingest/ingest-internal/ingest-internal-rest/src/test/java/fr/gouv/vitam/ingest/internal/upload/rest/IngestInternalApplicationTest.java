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
package fr.gouv.vitam.ingest.internal.upload.rest;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;

public class IngestInternalApplicationTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raise an exception";

    private static final String INGEST_INTERNAL_CONF = "ingest-internal.conf";

    private static int serverPort;
    private static JunitHelper junitHelper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();
        VitamServerFactory.setDefaultPort(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        junitHelper.releasePort(serverPort);
    }

    @Test
    public final void givenNegativeNumberWhenStartApplicationThenNotRaiseException() throws FileNotFoundException {
        try {
            ((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(INGEST_INTERNAL_CONF).getAbsolutePath(), "-1"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test
    public final void givenIncorrectPortNumberWhenStartApplicationThenNotRaiseException() throws FileNotFoundException {
        try {
            ((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(INGEST_INTERNAL_CONF).getAbsolutePath(), "-1xx"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

    }

    @Test
    public final void givenFileNotFoundWhenStartApplicationThenStartOnDefaultPort() throws FileNotFoundException {
        try {
            ((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
                "src/test/resources/notFound.conf"})).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test
    public final void givenCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() throws FileNotFoundException {
        try {
            ((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
                "src/test/resources/ingest-internal.conf"})).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }


    @Test(expected = IllegalStateException.class)
    public final void givenInCorrrectConfigFileWhenStartApplicationThenStartOnDefaultPort() {
        try {
            ((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
                "ingest-internal-err1.conf"})).stop();
        } catch (final VitamApplicationServerException e) {
            fail("Exception");
        }
    }

    @Test(expected = IllegalStateException.class)
    public final void givenNotConfigFileWhenStartApplicationThenStartOnDefaultPort() {
        try {
            ((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
                "ingest-internal-err2.conf"})).stop();
        } catch (final VitamApplicationServerException e) {
            fail("Exception");
        }
    }


    @Test
    public final void givenCorrectPortNumberWhenStartApplicationThenNotRaiseException() throws FileNotFoundException {
        try {
            ((BasicVitamServer) IngestInternalApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(INGEST_INTERNAL_CONF).getAbsolutePath(), Integer.toString(serverPort)
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public final void givenNullParamWhenStartApplicationThenRaiseException() throws VitamApplicationServerException {
        ((BasicVitamServer) IngestInternalApplication.startApplication(new String[0])).stop();
    }
}
