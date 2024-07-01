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
package fr.gouv.vitam.ihmdemo.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * JsonTransformerTest junit test
 */
public class JsonTransformerTest {

    private static final String VALID_ALL_PARENTS =
        "[{#id:'ID029',Title:'ID029',#unitups:['ID028', 'ID030'],_tenant:0}, " +
        "{#id:'ID028',Title:'ID028',#unitups:['ID027'],_tenant:0}," +
        "{#id:'ID030',Title:'ID030',#unitups:['ID027'],_tenant:0}," +
        "{#id:'ID027',Title:'ID027',#unitups:['ID026', 'ID025'],_tenant:0}," +
        "{#id:'ID026',Title:'ID026',#unitups:[],_tenant:0}," +
        "{#id:'ID025',Title:'ID025',#unitups:[],_tenant:0}]";

    private static final String INVALID_ALL_PARENTS_WITH_MISSING_ID =
        "[{Title:'ID029',#unitups:['ID028', 'ID030'],_tenant:0}, " +
        "{#id:'ID028',Title:'ID028',#unitups:['ID027'],_tenant:0}," +
        "{#id:'ID030',Title:'ID030',#unitups:['ID027'],_tenant:0}," +
        "{#id:'ID027',Title:'ID027',#unitups:['ID026', 'ID025'],_tenant:0}," +
        "{#id:'ID026',Title:'ID026',#unitups:[],_tenant:0}," +
        "{#id:'ID025',Title:'ID025',#unitups:[],_tenant:0}]";

    private static final String INVALID_ALL_PARENTS_WITH_MISSING_UP =
        "[{#id:'ID029',Title:'ID029',#unitups:['ID028', 'ID030'],_tenant:0}, " +
        "{#id:'ID028',Title:'ID028',_tenant:0}," +
        "{#id:'ID030',Title:'ID030',#unitups:['ID027'],_tenant:0}," +
        "{#id:'ID027',Title:'ID027',#unitups:['ID026', 'ID025'],_tenant:0}," +
        "{#id:'ID026',Title:'ID026',#unitups:[],_tenant:0}," +
        "{#id:'ID025',Title:'ID025',#unitups:[],_tenant:0}]";

    private static final String INVALID_ALL_PARENTS_WITH_INVALID_UP =
        "[{#id:'ID029',Title:'ID029',#unitups:['ID028', 'ID030'],_tenant:0}, " +
        "{#id:'ID028',Title:'ID028',#unitups:['ID027'],_tenant:0}," +
        "{#id:'ID030',Title:'ID030',#unitups:['ID027'],_tenant:0}," +
        "{#id:'ID027',Title:'ID027',#unitups:'WRONG_up',_tenant:0}," +
        "{#id:'ID026',Title:'ID026',#unitups:[],_tenant:0}," +
        "{#id:'ID025',Title:'ID025',#unitups:[],_tenant:0}]";

    private static JsonNode validParents;
    private static JsonNode invalidParentsWithMissingId;
    private static JsonNode invalidParentsWithMissingUp;
    private static JsonNode invalidParentsWithInvalidUp;
    private static final String SAMPLE_LOGBOOKOPERATION_FILENAME = "logbookoperation_sample.json";
    private static JsonNode sampleLogbookOperation;

    @BeforeClass
    public static void setup() throws Exception {
        validParents = JsonHandler.getFromString(VALID_ALL_PARENTS);
        invalidParentsWithMissingId = JsonHandler.getFromString(INVALID_ALL_PARENTS_WITH_MISSING_ID);
        invalidParentsWithMissingUp = JsonHandler.getFromString(INVALID_ALL_PARENTS_WITH_MISSING_UP);
        invalidParentsWithInvalidUp = JsonHandler.getFromString(INVALID_ALL_PARENTS_WITH_INVALID_UP);
        sampleLogbookOperation = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_LOGBOOKOPERATION_FILENAME));
    }

    @Test
    public void testTransformerObjectGroupSuccess() throws Exception {
        final JsonNode sampleObjectGroup = JsonHandler.getFromFile(
            PropertiesUtils.findFile("sample_objectGroup_document.json")
        );
        assertNotNull(JsonTransformer.transformResultObjects(sampleObjectGroup));
    }

    @Test
    public void testTransformerObjectGroupPhysicalSuccess() throws Exception {
        final JsonNode sampleObjectGroup = JsonHandler.getFromFile(
            PropertiesUtils.findFile("sample_objectGroup_physical.json")
        );
        assertNotNull(JsonTransformer.transformResultObjects(sampleObjectGroup));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransformerNullThrowsException() throws Exception {
        JsonTransformer.transformResultObjects(null);
    }

    @Test
    public void testBuildAllParentsRefOk() throws VitamException {
        assertNotNull(JsonTransformer.buildAllParentsRef("ID029", validParents));
    }

    @Test(expected = VitamException.class)
    public void testBuildAllParentsRefWithMissingIdThrowsVitamException() throws Exception {
        JsonTransformer.buildAllParentsRef("ID029", invalidParentsWithMissingId);
    }

    @Test(expected = VitamException.class)
    public void testBuildAllParentsRefWithMissingUpThrowsVitamException() throws Exception {
        JsonTransformer.buildAllParentsRef("ID029", invalidParentsWithMissingUp);
    }

    @Test(expected = VitamException.class)
    public void testBuildAllParentsRefWithInvalidUpThrowsVitamException() throws Exception {
        JsonTransformer.buildAllParentsRef("ID029", invalidParentsWithInvalidUp);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildAllParentsRefWithNullParentsList() throws Exception {
        JsonTransformer.buildAllParentsRef("ID029", null);
    }

    @Test(expected = VitamException.class)
    public void testBuildAllParentsRefWithMissingUnitIdThrowsVitamException() throws Exception {
        JsonTransformer.buildAllParentsRef("ID020", validParents);
    }

    @Test
    public void testBuildLogbookStatCsvFile() throws VitamException, IOException {
        final ByteArrayOutputStream report = JsonTransformer.buildLogbookStatCsvFile(sampleLogbookOperation);
        // TODO P1 : validate the created report
    }
}
