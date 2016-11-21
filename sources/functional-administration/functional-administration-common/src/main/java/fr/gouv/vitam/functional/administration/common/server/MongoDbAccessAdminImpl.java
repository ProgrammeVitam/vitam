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
package fr.gouv.vitam.functional.administration.common.server;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.SelectToMongoDb;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.translators.mongodb.QueryToMongodb;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * MongoDbAccess Implement for Admin
 */
public class MongoDbAccessAdminImpl extends MongoDbAccess
implements MongoDbAccessReferential {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoDbAccessAdminImpl.class);


    /**
     * @param mongoClient client of mongo
     * @param dbname name of database
     * @param recreate true if recreate type
     */
    protected MongoDbAccessAdminImpl(MongoClient mongoClient, String dbname, boolean recreate) {
        super(mongoClient, dbname, recreate);
        for (final FunctionalAdminCollections collection : FunctionalAdminCollections.values()) {
            collection.initialize(super.getMongoDatabase(), recreate);
        }
    }

    @Override
    public void insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection)
        throws ReferentialException {
        final List<VitamDocument> vitamDocumentList = new ArrayList<>();
        try {
            for (final JsonNode objNode : arrayNode) {
                VitamDocument obj;

                obj = (VitamDocument) JsonHandler.getFromJsonNode(objNode, collection.getClasz());

                vitamDocumentList.add(obj);
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Insert Documents Exception", e);
            throw new ReferentialException(e);
        }
        collection.getCollection().insertMany(vitamDocumentList);
    }

    // Not check, test feature !
    @Override
    public void deleteCollection(FunctionalAdminCollections collection) throws DatabaseException {
        long count = collection.getCollection().count();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(collection.getName() + " count before: " + count);
        }
        if (count > 0) {
            DeleteResult result = collection.getCollection().deleteMany(new Document());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(collection.getName() + " result.result.getDeletedCount(): " + result.getDeletedCount());
            }
            if (result.getDeletedCount() != count) {
                throw new DatabaseException(String.format("%s: Delete %s from %s elements", collection.getName(), result
                    .getDeletedCount(), count));
            }

        }
    }

    @Override
    public VitamDocument<?> getDocumentById(String id, FunctionalAdminCollections collection)
        throws ReferentialException {
        return (VitamDocument<?>) collection.getCollection().find(eq(VitamDocument.ID, id)).first();
    }

    @Override
    public MongoCursor<?> select(JsonNode select, FunctionalAdminCollections collection)
        throws ReferentialException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
            parser.parse(select);
            return selectExecute(collection, parser);
        } catch (final InvalidParseOperationException e) {
            throw new ReferentialException(e);
        }
    }

    /**
     * @param collection
     * @param parser
     * @return the Closeable MongoCursor on the find request based on the given collection
     * @throws InvalidParseOperationException when query is not correct
     */
    private MongoCursor selectExecute(final FunctionalAdminCollections collection, SelectParserSingle parser)
        throws InvalidParseOperationException {
        final SelectToMongoDb selectToMongoDb = new SelectToMongoDb(parser);
        final Bson condition = QueryToMongodb.getCommand(selectToMongoDb.getSelect().getQuery());
        final Bson projection = selectToMongoDb.getFinalProjection();
        final Bson orderBy = selectToMongoDb.getFinalOrderBy();
        final int offset = selectToMongoDb.getFinalOffset();
        final int limit = selectToMongoDb.getFinalLimit();
        FindIterable<?> find = collection.getCollection().find(condition).skip(offset);
        if (projection != null) {
            find = find.projection(projection);
        }
        if (orderBy != null) {
            find = find.sort(orderBy);
        }
        if (limit > 0) {
            find = find.limit(limit);
        }
        return find.iterator();
    }

    @Override
    public void updateDocumentByMap(Map<String, Object> map, JsonNode objNode, 
        FunctionalAdminCollections collection, UPDATEACTION operator)
        throws ReferentialException {
        BasicDBObject incQuery = new BasicDBObject();
        BasicDBObject updateFields = new BasicDBObject();
        for (Entry<String, Object> entry : map.entrySet()) {
            updateFields.append(entry.getKey(), (Number) entry.getValue());
        }
        incQuery.append(operator.exactToken(), updateFields);
        UpdateResult result = collection.getCollection().updateOne(eq(AccessionRegisterSummary.ORIGINATING_AGENCY, objNode.get(AccessionRegisterSummary.ORIGINATING_AGENCY).textValue()), incQuery);
        if (result.getModifiedCount() == 0 && result.getMatchedCount() == 0) {
            throw new ReferentialException("Document is not updated");
        }
    }

    @Override
    public void insertDocument(JsonNode json, FunctionalAdminCollections collection) throws ReferentialException {
        try {
            VitamDocument obj = (VitamDocument) JsonHandler.getFromJsonNode(json, collection.getClasz());
            collection.getCollection().insertOne(obj);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Documents not conformed Exception", e);
            throw new ReferentialException(e);
        }
    }

}
