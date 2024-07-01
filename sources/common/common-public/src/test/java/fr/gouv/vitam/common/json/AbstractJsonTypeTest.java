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
package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import fr.gouv.vitam.common.ResourcesPublicUtilTest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AbstractJsonTypeTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractJsonType.class);

    private static class TestClass extends AbstractJsonType {

        String a;

        @JsonGetter
        protected final String getA() {
            return a;
        }

        @JsonSetter
        protected final TestClass setA(String a) {
            this.a = a;
            return this;
        }
    }

    @Test
    public final void testGenerateJson() throws InvalidParseOperationException, FileNotFoundException {
        final TestClass tc = new TestClass().setA("val");
        tc.generateJson();
        final File file = ResourcesPublicUtilTest.getInstance().getJsonTest2JsonFile();
        if (file == null) {
            LOGGER.error(ResourcesPublicUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE);
        }
        Assume.assumeTrue(ResourcesPublicUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE, file != null);

        final TestClass tc2 = (TestClass) AbstractJsonType.readJsonFile(file);
        assertEquals(tc2.getA(), tc.getA());
        AbstractJsonType.readJsonString(tc.generateJsonString());
        File file2;
        try {
            file2 = File.createTempFile("test", "test", file.getParentFile());
            tc2.writeJsonToFile(file2);
            final TestClass tc3 = (TestClass) AbstractJsonType.readJsonFile(file2);
            assertEquals(tc2.getA(), tc3.getA());
        } catch (final IOException e) { // NOSONAR
            // ignore: write right access
        }
    }
}
