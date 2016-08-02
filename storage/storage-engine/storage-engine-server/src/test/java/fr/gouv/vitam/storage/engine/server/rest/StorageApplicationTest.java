/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.storage.engine.server.rest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServerFactory;

/**
 * 
 */
public class StorageApplicationTest {
    
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";
    
    private static final String STORAGE_CONF = "storage-engine.conf";

    private static int serverPort;
    private static int oldPort;
    private static JunitHelper junitHelper;
    private static File storage;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = new JunitHelper();
        storage = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration realStorage = PropertiesUtils.readYaml(storage, StorageConfiguration.class);
        
        try (FileOutputStream outputStream = new FileOutputStream(storage)) {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.writeValue(outputStream, realStorage);
        }

        serverPort = junitHelper.findAvailablePort();
        oldPort = VitamServerFactory.getDefaultPort();
        VitamServerFactory.setDefaultPort(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        junitHelper.releasePort(serverPort);
        VitamServerFactory.setDefaultPort(oldPort);
    }

    @Test
    public final void testFictiveLaunch() {
        try {
            ((BasicVitamServer) StorageApplication.startApplication(new String[] {
                storage.getAbsolutePath(), "-1"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) StorageApplication.startApplication(new String[] {
                storage.getAbsolutePath(), "-1xx"
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) StorageApplication.startApplication(new String[] {
                storage.getAbsolutePath(), Integer.toString(serverPort)
            })).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        try {
            ((BasicVitamServer) StorageApplication.startApplication(new String[0])).stop();
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (final VitamApplicationServerException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

    }
}
