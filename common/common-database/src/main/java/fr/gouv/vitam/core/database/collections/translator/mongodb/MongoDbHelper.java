/**
 *
 */
package fr.gouv.vitam.core.database.collections.translator.mongodb;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

import com.mongodb.MongoClient;

/**
 * MongoDb Helper
 */
public class MongoDbHelper {
    private static final JsonWriterSettings jws = new JsonWriterSettings(true);

    /**
     * 
     * @param bson
     * @param indent if True, output will be indented.
     * @return the String representation of the Bson
     */
    public static String bsonToString(Bson bson, boolean indent) {
        if (bson == null) {
            return "";
        }
        if (indent) {
            return bson.toBsonDocument(BsonDocument.class,
                MongoClient.getDefaultCodecRegistry()).toJson(MongoDbHelper.jws);
        } else {
            return bson.toBsonDocument(BsonDocument.class,
                MongoClient.getDefaultCodecRegistry()).toJson();
        }
    }

}
