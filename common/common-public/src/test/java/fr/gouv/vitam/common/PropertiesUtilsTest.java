/**
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
 */
package fr.gouv.vitam.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.Test;

public class PropertiesUtilsTest {

    @Test(expected = FileNotFoundException.class)
    public void testReadResourcesPropertiesFileNotFound() throws IOException {
        PropertiesUtils.readResourcesProperties("vesR[l}EQ2v6");
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetResourcesFileNotFound() throws FileNotFoundException {
        PropertiesUtils.getResourcesFile("Y?DFe@=JZEwEbf~c");
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testReadPropertiesFileNotFound() throws IOException {
        final File file0 = new File("");
        PropertiesUtils.readProperties(file0);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testReadPropertiesFileNotFoundNull() throws IOException {
        PropertiesUtils.readProperties((File) null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testReadResourcesPropertiesFileNotFoundNull() throws IOException {
        PropertiesUtils.readResourcesProperties(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetResourcesFileNotFoundNull() throws FileNotFoundException {
        PropertiesUtils.getResourcesFile(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test
    public void testGetResourcesFile() throws FileNotFoundException {
        File file = PropertiesUtils.getResourcesFile(
            ResourcesPublicUtilTest.GUID_TEST_PROPERTIES);
        assertTrue(file.exists());
        Path path = PropertiesUtils.getResourcesPath(ResourcesPublicUtilTest.GUID_TEST_PROPERTIES);
        assertEquals(file.getAbsolutePath(), path.toString());
        try {
            assertFalse(PropertiesUtils.readResourcesProperties(
                ResourcesPublicUtilTest.GUID_TEST_PROPERTIES).isEmpty());
        } catch (final IOException e) { // NOSONAR
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
    }

    private static class ConfigurationTest {
        private String test;
        private int number;
        protected ConfigurationTest(){
            // empty
        }
        protected final String getTest() {
            return test;
        }
        protected final void setTest(String test) {
            this.test = test;
        }
        protected final int getNumber() {
            return number;
        }
        protected final void setNumber(int number) {
            this.number = number;
        }
        
    }
    @Test
    public void testGetYamlFile() throws FileNotFoundException {
        try {
            ConfigurationTest test = PropertiesUtils.readResourcesYaml(
                ResourcesPublicUtilTest.YAML_TEST_CONF, ConfigurationTest.class);
            assertEquals("test", test.getTest());
            assertEquals(12346, test.getNumber());
        } catch (IOException e1) {
            e1.printStackTrace();
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
    }
}
