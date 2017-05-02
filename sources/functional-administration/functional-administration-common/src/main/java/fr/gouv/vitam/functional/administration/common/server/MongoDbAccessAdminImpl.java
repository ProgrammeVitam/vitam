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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Insert;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * MongoDbAccess Implement for Admin
 */
public class MongoDbAccessAdminImpl extends MongoDbAccess implements MongoDbAccessReferential {

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
        try {
            DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection());
            Insert insertquery = new Insert();
            insertquery.setData(arrayNode);
            dbrequest.execute(insertquery);
        } catch (InvalidParseOperationException | DatabaseException | InvalidCreateOperationException e) {
            LOGGER.error("Insert Documents Exception", e);
            throw new ReferentialException(e);
        }
    }

    // Not check, test feature !
    @Override
    public void deleteCollection(FunctionalAdminCollections collection) throws DatabaseException, ReferentialException {
        Document filter = new Document().append(VitamDocument.TENANT_ID, ParameterHelper.getTenantParameter());
        long count = 0;
        if (FunctionalAdminCollections.FORMATS.equals(collection)) {
            count = collection.getCollection().count();
        } else {
            count = collection.getCollection().count(filter);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(collection.getName() + " count before: " + count);
        }
        if (count > 0) {
            final Delete delete = new Delete();
            DbRequestResult result = null;
            DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection());
            try {
                delete.setQuery(QueryHelper.exists(VitamFieldsHelper.id()));
                result = dbrequest.execute(delete);
            } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
                throw new DatabaseException("Delete document exception");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(collection.getName() + " result.result.getDeletedCount(): " + result.getCount());
            }
            if (result.getCount() != count) {
                throw new DatabaseException(String.format("%s: Delete %s from %s elements", collection.getName(),
                    result.getCount(), count));
            }
        }
    }

    @VisibleForTesting
    @Override
    public VitamDocument<?> getDocumentById(String id, FunctionalAdminCollections collection)
        throws ReferentialException {
        return (VitamDocument<?>) collection.getCollection().find(eq(VitamDocument.ID, id)).first();
    }

    @Override
    public MongoCursor<VitamDocument<?>> findDocuments(JsonNode select, FunctionalAdminCollections collection)
        throws ReferentialException {
        try {
            SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
            parser.parse(select);
            DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection());
            return dbrequest.execute(parser.getRequest()).getCursor();
        } catch (final DatabaseException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("find Document Exception", e);
            throw new ReferentialException(e);
        }
    }

    @Override
    public void updateData(JsonNode update, FunctionalAdminCollections collection)
        throws ReferentialException {
        try { 
            UpdateParserSingle parser = new UpdateParserSingle(new VarNameAdapter());
            parser.parse(update);
            DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection());
            DbRequestResult result = (DbRequestResult) dbrequest.execute(parser.getRequest());
            if (result.getDiffs().size() == 0 ) {
                throw new ReferentialException("Document is not updated");
            }
        } catch (final DatabaseException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("find Document Exception", e);
            throw new ReferentialException(e);
        }
    }

    @Override
    public void updateDocumentByMap(Map<String, Object> map, JsonNode objNode,
        FunctionalAdminCollections collection, UPDATEACTION operator)
        throws ReferentialException {
        final BasicDBObject incQuery = new BasicDBObject();
        final BasicDBObject updateFields = new BasicDBObject();
        for (final Entry<String, Object> entry : map.entrySet()) {
            updateFields.append(entry.getKey(), entry.getValue());
        }
        incQuery.append(operator.exactToken(), updateFields);
        Bson query = and(
            eq(AccessionRegisterSummary.ORIGINATING_AGENCY,
                objNode.get(AccessionRegisterSummary.ORIGINATING_AGENCY).textValue()),
            eq(VitamDocument.TENANT_ID, ParameterHelper.getTenantParameter()));

        final UpdateResult result = collection.getCollection().updateOne(query, incQuery);
        if (result.getModifiedCount() == 0 && result.getMatchedCount() == 0) {
            throw new ReferentialException("Document is not updated");
        }
    }

    @Override
    public void insertDocument(JsonNode json, FunctionalAdminCollections collection) throws ReferentialException {
        insertDocuments(JsonHandler.createArrayNode().add(json), collection);
    }

}
