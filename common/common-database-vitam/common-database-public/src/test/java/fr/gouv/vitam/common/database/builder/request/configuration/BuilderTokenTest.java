package fr.gouv.vitam.common.database.builder.request.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.MULTIFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERYARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.RANGEARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTIONARGS;

public class BuilderTokenTest {
    
    @Test
    public void testExactToken() {
        assertEquals(PROJECTIONARGS.ID.exactToken(), "#id");
        assertEquals(RANGEARGS.GT.exactToken(), "$gt");
        assertEquals(QUERYARGS.TYPE.exactToken(), "$type");
        assertEquals(PROJECTION.FIELDS.exactToken(), "$fields");
        assertEquals(FILTERARGS.CACHE.exactToken(), "cache");
        assertEquals(UPDATEACTION.SET.exactToken(), "$set");
        assertEquals(UPDATEACTIONARGS.EACH.exactToken(), "$each");
        assertEquals(MULTIFILTER.MULT.exactToken(), "$mult");
        assertEquals(QUERY.AND.exactToken(), "$and");
        assertEquals(SELECTFILTER.LIMIT.exactToken(), "$limit");
        assertEquals(GLOBAL.QUERY.exactToken(), "$query");
    }


}
