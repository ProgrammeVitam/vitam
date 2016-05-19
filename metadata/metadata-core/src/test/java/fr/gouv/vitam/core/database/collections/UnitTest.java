package fr.gouv.vitam.core.database.collections;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class UnitTest {
    private static final String s1 = "{\"_id\":\"id1\", \"title\":\"title1\"}";
    private static final String s2 = "{\"_id\":\"id2\", \"title\":\"title2\", \"_up\":\"id1\"}";
    private static final String s3 = "{\"_id\":\"id3\", \"title\":\"title3\", \"_up\":\"id1\"}";
    private static final String sub1 = "{\"_id\":\"id2\",\"description\":\"description1\"}";
    private static final String sub2 = "{\"_id\":\"id3\",\"champ\":\"champ1\"}";
    
    
    @Test
    public void testUnitInitialization() throws InvalidParseOperationException{
        JsonNode json = JsonHandler.getFromString(s1);
        
        Unit unit = new Unit();
        Unit unit2 = new Unit(s1);
        Unit unit3 = new Unit(json);
        assertTrue(unit.isEmpty());
        assertEquals("Document{{}}", unit.toStringDirect());
        assertEquals("Unit: null", unit.toStringDebug());
        assertEquals("Unit: Document{{_id=id1, title=title1}}", unit2.toString());
        assertEquals("Unit: Document{{_id=id1, title=title1}}", unit3.toString());
    }
    
    @Test
    public void testAddUnits(){
        Unit unit = new Unit(s1);       
        Unit subUnit1 = new Unit(sub1);
        Unit subUnit2 = new Unit(sub2);
        unit.put("_min", 2);
        unit.put("_max", 5);
        
        List<Unit> units = new ArrayList<Unit>();
        units.add(subUnit1);
        units.add(subUnit2);
        // TODO REVIEW add multiple units at once seems in error
        //unit.addUnit(units);
    }
    
    @Test
    public void testloadDocument(){
        Unit unit = new Unit(s1);
        unit.load("{\"_dom\":\"dom1\"}");
        assertEquals("Unit: Document{{_id=id1, title=title1, _dom=dom1}}", unit.toString());
    }
    
    @Test
    public void testSubDepth(){
        Unit unit = new Unit(s1);
        assertEquals("[{ \"id1\" : 1}]", unit.getSubDepth().toString());
        
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("UUID1", 3);
        map.put("UUID2", 4);
        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("_uds", map);
        unit.putAll(map2);
        assertEquals("Unit: Document{{_id=id1, title=title1, _uds={UUID2=4, UUID1=3}}}", unit.toString());
        assertEquals("[{ \"UUID2\" : 5}, { \"UUID1\" : 4}, { \"id1\" : 1}]", unit.getSubDepth().toString());
    }
    
    @Test
    public void testDomaineId(){
        Unit unit = new Unit(s1);
        unit.put("_dom", 8888);
        assertEquals(8888, unit.getDomainId());
        VitamDocument<Unit> document = unit.checkId();
        assertEquals(8888, document.getDomainId());
    }    
}
