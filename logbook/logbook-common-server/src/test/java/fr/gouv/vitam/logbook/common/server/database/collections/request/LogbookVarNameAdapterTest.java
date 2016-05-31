package fr.gouv.vitam.logbook.common.server.database.collections.request;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;

public class LogbookVarNameAdapterTest {

    private static LogbookVarNameAdapter logbookNameAdapter = new LogbookVarNameAdapter();

    @Test
    public void givenLogbookVarNameAdapterWhenGetVariableNameThenReturnCorrect() throws InvalidParseOperationException {
        assertEquals(null, logbookNameAdapter.getVariableName("notValid"));
        assertEquals(null, logbookNameAdapter.getVariableName("#id"));
    }

}
