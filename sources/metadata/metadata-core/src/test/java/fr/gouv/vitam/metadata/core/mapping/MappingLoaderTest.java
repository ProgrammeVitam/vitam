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
package fr.gouv.vitam.metadata.core.mapping;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.metadata.core.utils.MappingLoaderTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class MappingLoaderTest {

    MappingLoader fakeMappingLoader;
    MappingLoader mappingLoader;
    private static final String UNIT = "Unit";
    private static final String OBJECTGROUP = "ObjectGroup";
    private static final String FAKE_COLLECTION = "fake";
    private static final String BAD_UNIT_MAPPING_PATH = "some/path/unit-mapping.json";
    private static final String BAD_OBJECTGROUP_MAPPING_PATH = "some/path/og-mapping.json";

    @Before
    public void setup() throws Exception {
        mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();
    }

    @Test
    public void testUnitLoadMappingExistFiles() throws IOException {
        InputStream is = mappingLoader.loadMapping(UNIT.toUpperCase());
        assertNotNull(is);
    }

    @Test
    public void testObjectGroupLoadMappingExistFiles() throws IOException {
        InputStream is = mappingLoader.loadMapping(OBJECTGROUP.toUpperCase());
        assertNotNull(is);
    }

    @Test
    public void testFakeLoadMappingThenNull() throws IOException {
        try {
            mappingLoader.loadMapping(FAKE_COLLECTION);
            fail("Should raise an exception");
        } catch (final VitamRuntimeException e) {}
    }

    @Test
    public void testUnitMappingNotExistThenFail() throws Exception {
        fakeMappingLoader = MappingLoaderTestUtils.getTestMappingLoader();
        fakeMappingLoader.getElasticsearchExternalMappings().get(0).setMappingFile(BAD_UNIT_MAPPING_PATH);
        try {
            fakeMappingLoader.loadMapping(UNIT.toUpperCase());
            fail("should raise an exception");
        } catch (final FileNotFoundException e) {}
    }

    @Test
    public void testObjectGroupMappingNotExistThenFail() throws Exception {
        fakeMappingLoader = MappingLoaderTestUtils.getTestMappingLoader();
        fakeMappingLoader.getElasticsearchExternalMappings().get(1).setMappingFile(BAD_OBJECTGROUP_MAPPING_PATH);
        try {
            fakeMappingLoader.loadMapping(OBJECTGROUP.toUpperCase());
            fail("should raise an exception");
        } catch (final FileNotFoundException e) {}
    }
}
