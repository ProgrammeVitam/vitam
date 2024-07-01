/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PropertiesUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        File tempFolder = temporaryFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", tempFolder.getAbsolutePath());
    }

    @Test
    public void testBuildPath() {
        final String fictivePath = "/mydir/myfile.txt";
        File file = PropertiesUtils.fileFromDataFolder(fictivePath);
        assertTrue(file.getAbsolutePath().startsWith(VitamConfiguration.getVitamDataFolder()));
        assertTrue(file.getAbsolutePath().endsWith(fictivePath));

        file = PropertiesUtils.fileFromConfigFolder(fictivePath);
        assertTrue(file.getAbsolutePath().startsWith(VitamConfiguration.getVitamConfigFolder()));
        assertTrue(file.getAbsolutePath().endsWith(fictivePath));

        file = PropertiesUtils.fileFromLogFolder(fictivePath);
        assertTrue(file.getAbsolutePath().startsWith(VitamConfiguration.getVitamLogFolder()));
        assertTrue(file.getAbsolutePath().endsWith(fictivePath));

        file = PropertiesUtils.fileFromTmpFolder(fictivePath);
        assertTrue(file.getAbsolutePath().startsWith(VitamConfiguration.getVitamTmpFolder()));
        assertTrue(file.getAbsolutePath().endsWith(fictivePath));

        try (InputStream stream = PropertiesUtils.getResourceAsStream("json-test.json")) {
            assertTrue(stream != null);
        } catch (final IOException e) {
            fail("Should not raized an exception");
        }
    }

    @Test
    public void should_validate_file() throws Exception {
        // When / Then
        assertThatThrownBy(() -> PropertiesUtils.fileFromTmpFolder("../test"))
            .isInstanceOf(VitamRuntimeException.class)
            .hasMessageContaining("invalid path");
        assertThatCode(() -> PropertiesUtils.fileFromTmpFolder("./test")).doesNotThrowAnyException();
        assertThatCode(() -> PropertiesUtils.fileFromTmpFolder("/test")).doesNotThrowAnyException();
        assertThatCode(() -> PropertiesUtils.fileFromTmpFolder("test")).doesNotThrowAnyException();
    }

    @Test(expected = FileNotFoundException.class)
    public void testReadResourcesPathFileNotFound() throws IOException {
        PropertiesUtils.getResourcePath("vesR[l}EQ2v6");
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetResourcesFileNotFound() throws FileNotFoundException {
        PropertiesUtils.getResourceFile("Y?DFe@=JZEwEbf~c");
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetFileNotFound() throws FileNotFoundException {
        PropertiesUtils.findFile("notfoundfilename");
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
        PropertiesUtils.readProperties(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetResourcesFileNotFoundNull() throws FileNotFoundException {
        PropertiesUtils.getResourceFile(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetResourcesStreamNotFoundNull() throws FileNotFoundException {
        PropertiesUtils.getResourceAsStream(null);
        fail(ResourcesPublicUtilTest.EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION);
    }

    @Test
    public void testGetResourcesFile() throws FileNotFoundException {
        final File file = PropertiesUtils.getResourceFile(ResourcesPublicUtilTest.GUID_TEST_PROPERTIES);
        assertTrue(file.exists());
        final Path path = PropertiesUtils.getResourcePath(ResourcesPublicUtilTest.GUID_TEST_PROPERTIES);
        assertEquals(file.getAbsolutePath(), path.toString());
        try {
            assertFalse(PropertiesUtils.readProperties(path.toFile()).isEmpty());
        } catch (final IOException e) { // NOSONAR
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try (
            InputStream inputStream = PropertiesUtils.getConfigAsStream(ResourcesPublicUtilTest.GUID_TEST_PROPERTIES)
        ) {
            //
        } catch (final FileNotFoundException e) {
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        } catch (final IOException e) {
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
    }

    static class ConfigurationTest {

        private String test;
        private int number;

        public ConfigurationTest() {
            // empty
        }

        public final String getTest() {
            return test;
        }

        public final void setTest(String test) {
            this.test = test;
        }

        public final int getNumber() {
            return number;
        }

        public final void setNumber(int number) {
            this.number = number;
        }
    }

    @Test
    public void testGetYamlFile() throws FileNotFoundException {
        try {
            final ConfigurationTest test = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(ResourcesPublicUtilTest.YAML_TEST_CONF),
                ConfigurationTest.class
            );
            assertEquals("test", test.getTest());
            assertEquals(12346, test.getNumber());
        } catch (final IOException e1) {
            e1.printStackTrace();
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try {
            final Map<String, Object> testMap = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(ResourcesPublicUtilTest.YAML_TEST_CONF),
                new TypeReference<Map<String, Object>>() {}
            );
            assertEquals("test", testMap.get("test"));
            assertEquals(12346, (int) testMap.get("number"));
        } catch (final IOException e1) {
            e1.printStackTrace();
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try (
            InputStream inputStream = new FileInputStream(
                PropertiesUtils.findFile(ResourcesPublicUtilTest.YAML_TEST_CONF)
            )
        ) {
            final ConfigurationTest test = PropertiesUtils.readYaml(inputStream, ConfigurationTest.class);
            assertEquals("test", test.getTest());
            assertEquals(12346, test.getNumber());
        } catch (final IOException e1) {
            e1.printStackTrace();
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try {
            final ConfigurationTest test = PropertiesUtils.readYaml(
                PropertiesUtils.getResourceFile(ResourcesPublicUtilTest.YAML_TEST_CONF),
                ConfigurationTest.class
            );
            assertEquals("test", test.getTest());
            assertEquals(12346, test.getNumber());
        } catch (final IOException e1) {
            e1.printStackTrace();
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try {
            final ConfigurationTest test = PropertiesUtils.readYaml(
                PropertiesUtils.getResourcePath(ResourcesPublicUtilTest.YAML_TEST_CONF),
                ConfigurationTest.class
            );
            assertEquals("test", test.getTest());
            assertEquals(12346, test.getNumber());
        } catch (final IOException e1) {
            e1.printStackTrace();
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
        try {
            final ConfigurationTest test = PropertiesUtils.readYaml(
                new File("inexistantFile"),
                ConfigurationTest.class
            );
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IOException e1) {
            // ignore
        }
        try {
            final ConfigurationTest test = PropertiesUtils.readYaml((File) null, ConfigurationTest.class);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IOException e1) {
            // ignore
        }
        try {
            final Map<String, Object> testMap = PropertiesUtils.readYaml(
                (File) null,
                new TypeReference<Map<String, Object>>() {}
            );
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IOException e1) {
            // ignore
        }
        try {
            final ConfigurationTest test = PropertiesUtils.readYaml((InputStream) null, ConfigurationTest.class);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IOException e1) {
            // ignore
        }
        try {
            final ConfigurationTest test = PropertiesUtils.readYaml((Path) null, ConfigurationTest.class);
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IOException e1) {
            // ignore
        }
        try {
            final ConfigurationTest test = PropertiesUtils.readYaml(
                PropertiesUtils.getResourcePath(ResourcesPublicUtilTest.YAML_TEST_CONF),
                null
            );
            fail(ResourcesPublicUtilTest.SHOULD_HAVE_AN_EXCEPTION);
        } catch (final IOException e1) {
            // ignore
        }
    }

    @Test
    public void testWriteYamlFile() throws FileNotFoundException {
        try {
            final File original = PropertiesUtils.findFile(ResourcesPublicUtilTest.YAML_TEST_CONF);
            final ConfigurationTest test = PropertiesUtils.readYaml(original, ConfigurationTest.class);
            assertEquals("test", test.getTest());
            assertEquals(12346, test.getNumber());
            final File destination = File.createTempFile(
                "test",
                ResourcesPublicUtilTest.YAML_TEST_CONF,
                original.getParentFile()
            );
            PropertiesUtils.writeYaml(destination, test);
        } catch (final IOException e1) {
            e1.printStackTrace();
            fail(ResourcesPublicUtilTest.SHOULD_NOT_HAVE_AN_EXCEPTION);
        }
    }
}
