package fr.gouv.vitam.core.database.collections.translator.mongodb;

import static org.junit.Assert.assertEquals;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.junit.Test;

public class MongoDbHelperTest {
    private static final String test = "{ \"data\" : 1 }";

    @Test
    public void testBsonToStringFn() {
        final Bson bson = BsonDocument.parse(test);
        assertEquals(test, MongoDbHelper.bsonToString(bson, false));
        assertEquals("{\n  \"data\" : 1\n}", MongoDbHelper.bsonToString(bson, true));
    }
}
