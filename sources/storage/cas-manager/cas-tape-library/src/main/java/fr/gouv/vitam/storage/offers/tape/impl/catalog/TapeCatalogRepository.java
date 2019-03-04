/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.offers.tape.impl.catalog;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.offers.tape.model.TapeModel;
import org.bson.Document;

/**
 * repository for Tapes Catalog management in mongo.
 */
public class TapeCatalogRepository {

    public static final String TAPE_CATALOG_COLLECTION = "TapeCatalog";

    private static final String ALL_PARAMS_REQUIRED = "All params are required";

    private final MongoCollection<Document> tapeCollection;

    String $_SET = "$set";
    String $_INC = "$inc";

    @VisibleForTesting
    public TapeCatalogRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        tapeCollection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public TapeCatalogRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, TAPE_CATALOG_COLLECTION);
    }

    /**
     * create a tape model
     *
     * @param tapeModel
     * @throws InvalidParseOperationException
     */
    public void createTape(TapeModel tapeModel) throws InvalidParseOperationException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tapeModel);
        tapeModel.setVersion(0);
        String json = JsonHandler.writeAsString(tapeModel);
        tapeCollection.insertOne(Document.parse(json));
    }

    /**
     * replace a tape model
     *
     * @param tapeModel
     * @throws InvalidParseOperationException
     */
    public boolean replaceTape(TapeModel tapeModel) throws InvalidParseOperationException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tapeModel);
        tapeModel.setVersion(tapeModel.getVersion() + 1);
        String json = JsonHandler.writeAsString(tapeModel);
        final UpdateResult result = tapeCollection.replaceOne(eq(TapeModel.ID, tapeModel.getId()), Document.parse(json));

        return result.getMatchedCount() == 1;
    }

    /**
     * apply fields changes for tape tapeId
     *
     * @param tapeId
     * @param fields
     * @return true if changes have been applied otherwise false
     */
    public boolean updateTape(String tapeId, Map<String, String> fields) {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tapeId, fields);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException(ALL_PARAMS_REQUIRED);
        }

        Document update = new Document();
        fields.forEach((key, value) -> update.append(key, value));

        Document data = new Document($_SET, update)
                .append($_INC, new Document(TapeModel.VERSION, 1));

        UpdateResult result = tapeCollection.updateOne(eq(TapeModel.ID, tapeId), data);

        return result.getMatchedCount() == 1;
    }

    /**
     * return tape models according to given fields
     *
     * @param fields
     * @return
     */
    public List<Document> findTapeByFields(Map<String, String> fields) {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, fields);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException(ALL_PARAMS_REQUIRED);
        }
        BasicDBObject filter = new BasicDBObject();
        fields.forEach((key, value) -> filter.append(key, value));
        return tapeCollection.find(filter).into(new ArrayList<Document>());
    }

    /**
     * return tape model according to given ID
     *
     * @param tapeId
     * @return
     */
    public TapeModel findTapeById(String tapeId) throws InvalidParseOperationException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tapeId);
        FindIterable<Document> models =
            tapeCollection.find(eq(TapeModel.ID, tapeId));
        Document first = models.first();
        if (first == null) {
            return null;
        }
        return JsonHandler.getFromString(JSON.serialize(first), TapeModel.class);
    }

}
