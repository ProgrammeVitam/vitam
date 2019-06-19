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
package fr.gouv.vitam.common.database.translators.mongodb;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import net.javacrumbs.jsonunit.JsonAssert;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.junit.Test;

public class MongoUpgradeTest {
    private static final String test = "{\"data\": 1}";

    @Test
    public void testBsonToStringFn() throws ParseException, IOException, InvalidParseOperationException {
        final Bson bson = BsonDocument.parse(test);
        assertEquals(test, MongoDbHelper.bsonToString(bson, false));
        assertEquals("{\n  \"data\": 1\n}", MongoDbHelper.bsonToString(bson, true));


        String mongo_3_4_4 =
            JsonHandler
                .getFromInputStream(PropertiesUtils.getResourceAsStream("upgrade-mongo/serialize-mongo.3.4.3.json"))
                .toString();
        String mongo_3_8_0 =
            JsonHandler
                .getFromInputStream(PropertiesUtils.getResourceAsStream("upgrade-mongo/serialize-mongo.3.8.0.json"))
                .toString();

        FakeObjet fakeObjet = new FakeObjet();

        JsonWriterSettings writerSettingsRelaxed = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
        JsonWriterSettings writerSettingsExtended = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
        JsonWriterSettings writerSettingsStrict = JsonWriterSettings.builder().outputMode(JsonMode.STRICT).build();

        // Problem Float and Date (Not used in Vitam) fixed with JSON.serialize(
        final String mongoJson = fakeObjet.toJson(writerSettingsRelaxed);
        final String serialize = JSON.serialize(fakeObjet);

        // JsonAssert.assertJsonEquals(serialize, mongo_3_4_4);
        //JsonAssert.assertJsonEquals(serialize, mongo_3_8_0);



        final String relaxed = fakeObjet.toJson(writerSettingsRelaxed);
        final String extended = fakeObjet.toJson(writerSettingsExtended);
        final String strict = fakeObjet.toJson(writerSettingsStrict);
        //JsonAssert.assertJsonEquals(serialize, relaxed);
        //JsonAssert.assertJsonEquals(extended, relaxed);

        JsonNode relaxedJson = JsonHandler.getFromString(relaxed);
        InputStream in = CanonicalJsonFormatter.serialize(relaxedJson);
        String str = StringUtils.getStringFromInputStream(in);
        System.err.println("Relaxed : " + str);

        JsonNode strictJson = JsonHandler.getFromString(strict);
        in = CanonicalJsonFormatter.serialize(strictJson);
        str = StringUtils.getStringFromInputStream(in);
        System.err.println("Strict : " + str);


        JsonNode extendedJson = JsonHandler.getFromString(extended);
        in = CanonicalJsonFormatter.serialize(extendedJson);
        str = StringUtils.getStringFromInputStream(in);
        System.err.println("Extended : " + str);


        JsonNode legacyJson = JsonHandler.getFromString(serialize);
        in = CanonicalJsonFormatter.serialize(legacyJson);
        str = StringUtils.getStringFromInputStream(in);
        System.err.println("Legacy : " + str);


        in = CanonicalJsonFormatter.serialize(JsonHandler.toJsonNode(fakeObjet));
        str = StringUtils.getStringFromInputStream(in);
        System.err.println("Jackson : " + str);



    }



    class FakeObjet extends Document {
        public FakeObjet() throws ParseException {
            append("floatVal", 2.2f);
            append("longVal", 2l);
            append("intVal", -2);
            append("doubleVal", -2.2);
            append("strVal", "2");
            append("dateVal", LocalDateUtil.getDate("1982-10-20"));
            append("dateStrVal", LocalDateUtil.getFormattedDate(LocalDateUtil.getDate("1982-10-20")));
        }
    }

}

