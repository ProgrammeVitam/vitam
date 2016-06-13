package fr.gouv.vitam.core.database.collections;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;

public class MongoDbVarNameAdapterTest {

    private static MongoDbVarNameAdapter mongoVarNameAdapter = new MongoDbVarNameAdapter();

    @Test
    public void givenMongoDbVarNameAdapterWhengetVariableNameThenReturnCorrect() throws InvalidParseOperationException {
        assertEquals(null, mongoVarNameAdapter.getVariableName("notValid"));
        assertEquals(VitamDocument.ID, mongoVarNameAdapter.getVariableName("#id"));
        assertEquals(Unit.APPRAISALRULES, mongoVarNameAdapter.getVariableName("#dua"));
        assertEquals(Unit.NBCHILD, mongoVarNameAdapter.getVariableName("#nbunits"));
        assertEquals(VitamDocument.TYPE, mongoVarNameAdapter.getVariableName("#type"));
        assertEquals(ObjectGroup.OBJECTSIZE, mongoVarNameAdapter.getVariableName("#size"));
        assertEquals(ObjectGroup.OBJECTFORMAT, mongoVarNameAdapter.getVariableName("#format"));
    }

}
