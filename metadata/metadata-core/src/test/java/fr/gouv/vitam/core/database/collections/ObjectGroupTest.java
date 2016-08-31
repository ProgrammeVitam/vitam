package fr.gouv.vitam.core.database.collections;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;

import static org.junit.Assert.*;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class ObjectGroupTest {

    private final int IntTest = 12345;
    String groupGUID  = GUIDFactory.newObjectGUID(IntTest).toString();
    private final String go = "{\"_id\":\""  + groupGUID + "\", \"_qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"title\":\"title1\"}";    
            
    @Test
    public void testGOInitialization() throws InvalidParseOperationException {
        final JsonNode jsonGO = JsonHandler.getFromString(go);

        final ObjectGroup go1 = new ObjectGroup();
        final ObjectGroup go2 = new ObjectGroup(go);
        final ObjectGroup go3 = new ObjectGroup(jsonGO);
        
        assertTrue(go1.isEmpty());
        assertFalse(go2.isEmpty());
        assertEquals("Document{{}}", go1.toStringDirect());
        assertNotNull(go3);
    }

    @Test
    public void testloadDocument() {               
        final ObjectGroup group = new ObjectGroup();               
        group.load(go);                       
        assertNotNull(group);        
    }

    @Test
    public void givenObjectGroupWhenGetGuid(){
        final ObjectGroup group = new ObjectGroup(go);
        assertNotNull(group.newObjectGuid());
        assertEquals(2 ,ObjectGroup.getGUIDObjectTypeId());
    }
    
    @Test
    public void givenObjectGroupWhenGetFathersUnitIdThenReturnAList(){
        final ObjectGroup group = new ObjectGroup();
        assertNotNull(group.getFathersUnitIds(true));
        assertNotNull(group.getFathersUnitIds(false));
    }
    
    @Test
    public void givenObjectGroupWhenCleanStructureThenItemCleaned(){
        final ObjectGroup group = new ObjectGroup();
        group.cleanStructure(true);
    }
    
    @Test
    public void givenObjectGroupWhenGetCollection(){
        final ObjectGroup group = new ObjectGroup();
        group.getMetadataCollections();
    }
    
    @Test
    public void givenObjectGroupWhenIsNotImmediateParentThenReturnFalse(){
        final ObjectGroup group = new ObjectGroup(go);
        assertFalse(group.isImmediateParent(groupGUID));
    }        
}
