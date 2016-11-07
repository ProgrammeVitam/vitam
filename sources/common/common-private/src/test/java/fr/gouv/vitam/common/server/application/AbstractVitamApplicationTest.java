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
package fr.gouv.vitam.common.server.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;

public class AbstractVitamApplicationTest {

    private static final String TEST_CONF_CONF = "test-conf.conf";

    private static class TestVitamApplication
        extends AbstractVitamApplication<TestVitamApplication, DbConfigurationImpl> {

        String conf = TEST_CONF_CONF;

        protected TestVitamApplication(Class<TestVitamApplication> applicationType,
            Class<DbConfigurationImpl> configurationType) {
            super(applicationType, configurationType);
        }

        @Override
        protected Handler buildApplicationHandler() {
            return null;
        }

        @Override
        protected String getConfigFilename() {
            return conf;
        }

    }

    @Test
    public final void testBuild() throws VitamApplicationServerException, FileNotFoundException {
        final TestVitamApplication testVitamApplication =
            new TestVitamApplication(TestVitamApplication.class, DbConfigurationImpl.class);
        final String filename1 = testVitamApplication.getConfigFilename();
        assertTrue(filename1.equals(TEST_CONF_CONF));
        final Path path0 = testVitamApplication.computeConfigurationPathFromInputArguments();
        final Path path1 = testVitamApplication.computeConfigurationPathFromInputArguments(
            PropertiesUtils.getResourceFile(filename1).getAbsolutePath());
        System.out.println(path0);
        System.out.println(path1);
        assertTrue(path0.equals(path1));
        final TestVitamApplication testVitamApplication2 = testVitamApplication.configure(path0);
        assertNotNull(testVitamApplication2);
        assertNull(testVitamApplication.getApplicationHandler());
        final DbConfigurationImpl configuration = testVitamApplication.getConfiguration();
        assertNotNull(configuration);
        
        List<MongoDbNode> nodes = configuration.getMongoDbNodes();
        assertEquals("localhost", nodes.get(0).getDbHost());
        assertEquals(45678, nodes.get(0).getDbPort());
        assertEquals("127.0.0.2", nodes.get(1).getDbHost());
        assertEquals(12345, nodes.get(1).getDbPort());
        
        assertEquals("Vitam-test", configuration.getDbName());
        testVitamApplication.conf = "fake-file-not-exist.conf";
        try {
            testVitamApplication.computeConfigurationPathFromInputArguments();
            fail("Should raized an exception");
        } catch (final VitamApplicationServerException e) {
            // ignore
        }
    }

}
