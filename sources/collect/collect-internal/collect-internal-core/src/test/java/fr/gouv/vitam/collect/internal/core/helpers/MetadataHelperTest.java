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
package fr.gouv.vitam.collect.internal.core.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper.DYNAMIC_ATTACHEMENT;

public class MetadataHelperTest {
    private static final String UNIT_UP = "UNIT_UP";
    private static final String UNIT_GUID = "UNIT_GUID";
    private static final String COMPLEX_PATH = "Keyword.KeywordContent";
    private static final String SIMPLE_PATH = "Title";
    private static final String SIMPLE_PATH_ARRAY_VALUE = "OriginatingSystemId";

    @Test
    public void findUnitParentWithComplexPath() throws Exception {
        try (InputStream is = PropertiesUtils.getResourceAsStream("collect_unit.json")) {
            ObjectNode unit = (ObjectNode) JsonHandler.getFromInputStream(is);
            unit.put(VitamFieldsHelper.id(), "ID");
            Map.Entry<String, String> unitParent = MetadataHelper.findUnitParent(unit,
                List.of(new MetadataUnitUp(UNIT_UP, COMPLEX_PATH, "Aisne (departement)")),
                Map.of(DYNAMIC_ATTACHEMENT + "_" + UNIT_UP, UNIT_GUID));

            Assert.assertEquals(UNIT_GUID, unitParent.getValue());
        }
    }

    @Test
    public void findUnitParentWithSimplePath() throws Exception {
        try (InputStream is = PropertiesUtils.getResourceAsStream("collect_unit.json")) {
            ObjectNode unit = (ObjectNode) JsonHandler.getFromInputStream(is);
            unit.put(VitamFieldsHelper.id(), "ID");
            Map.Entry<String, String> unitParent =
                MetadataHelper.findUnitParent(unit, List.of(new MetadataUnitUp(UNIT_UP, SIMPLE_PATH, "My title")),
                    Map.of(DYNAMIC_ATTACHEMENT + "_" + UNIT_UP, UNIT_GUID));

            Assert.assertEquals(UNIT_GUID, unitParent.getValue());
        }
    }


    @Test
    public void findUnitParentWithSimplePathButValueIsArray() throws Exception {
        try (InputStream is = PropertiesUtils.getResourceAsStream("collect_unit.json")) {
            ObjectNode unit = (ObjectNode) JsonHandler.getFromInputStream(is);
            unit.put(VitamFieldsHelper.id(), "ID");
            Map.Entry<String, String> unitParent = MetadataHelper.findUnitParent(unit,
                List.of(new MetadataUnitUp(UNIT_UP, SIMPLE_PATH_ARRAY_VALUE, "ID01")),
                Map.of(DYNAMIC_ATTACHEMENT + "_" + UNIT_UP, UNIT_GUID));

            Assert.assertEquals(UNIT_GUID, unitParent.getValue());
        }
    }
}