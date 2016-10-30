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
package fr.gouv.vitam.metadata.core.database.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;

public class UnitTest {
    private static final String s1 = "{\"_id\":\"id1\", \"title\":\"title1\"}";
    private static final String s2 = "{\"_id\":\"id2\", \"title\":\"title2\", \"_up\":\"id1\"}";
    private static final String sub1 = "{\"_id\":\"id2\",\"description\":\"description1\"}";
    private static final String sub2 = "{\"_id\":\"id3\",\"champ\":\"champ1\"}";

    @Test
    public void testUnitInitialization() throws InvalidParseOperationException {
        final JsonNode json = JsonHandler.getFromString(s1);

        final Unit unit = new Unit();
        final Unit unit2 = new Unit(s1);
        final Unit unit3 = new Unit(json);
        assertTrue(unit.isEmpty());
        assertEquals("Document{{}}", unit.toStringDirect());
        assertEquals("Unit: null", unit.toStringDebug());
        assertEquals("Unit: Document{{_id=id1, title=title1}}", unit2.toString());
        assertEquals("Unit: Document{{_id=id1, title=title1}}", unit3.toString());
    }

    @Test
    public void testAddUnits() throws MetaDataExecutionException {
        final Unit unit = new Unit(s1);
        final Unit unit1 = new Unit(s2);
        final Unit subUnit1 = new Unit(sub1);
        final Unit subUnit2 = new Unit(sub2);
        unit.put("_min", 2);
        unit.put("_max", 5);

        final List<Unit> units = new ArrayList<Unit>();
        units.add(subUnit1);
        units.add(subUnit2);
        // TODO P1 REVIEW add multiple units at once seems in error
        // unit.addUnits(units);
    }


    @Test
    public void testloadDocument() {
        final Unit unit = new Unit(s1);
        unit.load("{\"_dom\":\"dom1\"}");
        final String s = unit.toString();
        assertEquals("Unit: Document{{_id=id1, title=title1, _dom=dom1}}", s);
    }

    @Test
    public void testSubDepth() {
        final Unit unit = new Unit(s1);
        assertEquals("[{ \"id1\" : 1}]", unit.getSubDepth().toString());

        final List<Document> list = new ArrayList<Document>();
        list.add(Document.parse("{\"UUID2\" : 3}"));
        list.add(Document.parse("{\"UUID1\" : 4}"));

        final Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("_uds", list);
        unit.putAll(map2);
        assertEquals("Unit: Document{{_id=id1, title=title1, _uds=[Document{{UUID2=3}}, Document{{UUID1=4}}]}}",
            unit.toString());
        assertEquals("[{ \"UUID2\" : 4}, { \"UUID1\" : 5}, { \"id1\" : 1}]", unit.getSubDepth().toString());
    }

    @Test
    public void testDomaineId() {
        final Unit unit = new Unit(s1);
        unit.put("_dom", 8888);
        assertEquals(8888, unit.getDomainId());
        final MetadataDocument<Unit> document = unit.checkId();
        assertEquals(8888, document.getDomainId());
    }

    @Test
    public void givenUnitWhenGetFathersUnitIdThenReturnAList() {
        final Unit unit = new Unit(s1);
        assertNotNull(unit.getFathersUnitIds(true));
        assertNotNull(unit.getFathersUnitIds(false));
    }

    @Test
    public void givenUnitWhenGetDepth() {
        final Unit unit = new Unit(s1);
        assertNotNull(unit.getDepths());
    }

    @Test
    public void givenUnitWhenCleanStructureThenItemCleaned() {
        final Unit unit = new Unit(s1);
        unit.cleanStructure(true);
    }

    @Test
    public void givenUnitWhenGetObjectGroupId() {
        final Unit unit = new Unit(s1);
        unit.getObjectGroupId(true);
        unit.getObjectGroupId(false);
    }
}
