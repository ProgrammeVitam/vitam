package fr.gouv.vitam.core.utils;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.core.database.collections.Result;
import fr.gouv.vitam.core.database.collections.ResultDefault;
import fr.gouv.vitam.parser.request.parser.SelectParser;

public class UnitsJsonUtilsTest {

    private static Result buildResult(int nbrResult) {
        Result result = new ResultDefault(FILTERARGS.UNITS, new ArrayList<String>());
        result.setNbResult(nbrResult);
        return result;
    }

    @Test
    public void given_resultwith_nbreresult_0_thenReturn_JsonNode() throws Exception {
        JsonNode jsonNode = UnitsJsonUtils.populateJSONObjectResponse(buildResult(0), new SelectParser());
        assertNotNull(jsonNode);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_resultwith_nbreresult_2_thenthrow_InvalidParseOperationException() throws Exception {
        UnitsJsonUtils.populateJSONObjectResponse(buildResult(2), new SelectParser());
    }
}
