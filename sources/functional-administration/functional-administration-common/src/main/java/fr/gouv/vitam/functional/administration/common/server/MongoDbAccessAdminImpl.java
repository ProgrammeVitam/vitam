/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.functional.administration.common.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoClient;
import com.mongodb.MongoWriteException;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Insert;
import fr.gouv.vitam.common.database.parser.request.single.DeleteParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestHelper;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.database.server.DocumentValidator;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import org.bson.Document;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * MongoDbAccess Implement for Admin
 */
public class MongoDbAccessAdminImpl extends MongoDbAccess implements MongoDbAccessReferential {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MongoDbAccessAdminImpl.class);
    
    private final ElasticsearchFunctionalAdminIndexManager indexManager;
    private final OntologyLoader ontologyLoader;

    /**
     * @param mongoClient client of mongo
     * @param dbname name of database
     * @param recreate true if recreate type
     * @param indexManager
     * @param ontologyLoader
     */
    protected MongoDbAccessAdminImpl(MongoClient mongoClient, String dbname, boolean recreate,
        ElasticsearchFunctionalAdminIndexManager indexManager,
        OntologyLoader ontologyLoader) {
        super(mongoClient, dbname, recreate);
        this.indexManager = indexManager;
        for (final FunctionalAdminCollections collection : FunctionalAdminCollections.values()) {
            collection.initialize(super.getMongoDatabase(), recreate);
        }
        this.ontologyLoader = ontologyLoader;
    }

    @Override
    public DbRequestResult insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection)
        throws ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        return insertDocuments(arrayNode, collection, 0);
    }

    @Override
    public DbRequestResult insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection, Integer version)
        throws DocumentAlreadyExistsException, ReferentialException, SchemaValidationException {
        try {
            final DbRequestSingle dbrequest = new DbRequestSingle(
                collection.getVitamCollection(), this.ontologyLoader,
                indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(null));
            final Insert insertquery = new Insert();
            insertquery.setData(arrayNode);

            DocumentValidator documentValidator = ReferentialDocumentValidators.getValidator(collection);
            return dbrequest.execute(insertquery, version, documentValidator);
        } catch (MongoBulkWriteException | MongoWriteException | InvalidParseOperationException | BadRequestException | DatabaseException |
            InvalidCreateOperationException | VitamDBException e) {
            if (DbRequestHelper.isDuplicateKeyError(e)) {
                throw new DocumentAlreadyExistsException("Documents already exists: Duplicate Key", e);
            }
            throw new ReferentialException("Insert Documents Exception", e);
        }
    }

    // Not check, test feature !
    @Override
    @VisibleForTesting
    public DbRequestResult deleteCollectionForTesting(FunctionalAdminCollections collection, Delete delete)
        throws DatabaseException {
        long count;
        if (collection.isMultitenant()) {
            final Document filter =
                new Document().append(VitamDocument.TENANT_ID, ParameterHelper.getTenantParameter());
            count = collection.getCollection().countDocuments(filter);
        } else {
            count = collection.getCollection().countDocuments();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(collection.getName() + " count before: " + count);
        }
        if (count > 0) {

            final DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection(), this.ontologyLoader,
                this.indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(null));
            try (DbRequestResult result = dbrequest.execute(delete)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(collection.getName() + " result.result.getDeletedCount(): " + result.getCount());
                }

                return result;
            } catch (InvalidParseOperationException | BadRequestException | InvalidCreateOperationException |
                VitamDBException | SchemaValidationException e) {
                throw new DatabaseException("Delete document exception", e);
            }
        }
        return new DbRequestResult();
    }

    @Override
    @VisibleForTesting
    public DbRequestResult deleteCollectionForTesting(FunctionalAdminCollections collection)
        throws DatabaseException, SchemaValidationException {

        long count;
        if (collection.isMultitenant()) {
            final Document filter =
                new Document().append(VitamDocument.TENANT_ID, ParameterHelper.getTenantParameter());
            count = collection.getCollection().countDocuments(filter);
        } else {
            count = collection.getCollection().countDocuments();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(collection.getName() + " count before: " + count);
        }
        if (count > 0) {
            Delete delete = new Delete();
            final DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection(), this.ontologyLoader,
                indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(null));
            try (DbRequestResult result = dbrequest.execute(delete)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(collection.getName() + " result.result.getDeletedCount(): " + result.getCount());
                }
                if (result.getCount() != count) {
                    throw new DatabaseException(
                        String.format("%s: Delete %s from %s elements", collection.getName(),
                            result.getCount(), count));
                }
                return result;
            } catch (InvalidParseOperationException | BadRequestException | InvalidCreateOperationException |
                VitamDBException e) {
                throw new DatabaseException("Delete document exception", e);
            }
        }
        return new DbRequestResult();
    }

    @VisibleForTesting
    @Override
    public VitamDocument<?> getDocumentById(String id, FunctionalAdminCollections collection) {
        return (VitamDocument<?>) collection.getCollection().find(eq(VitamDocument.ID, id)).first();
    }

    @Override
    public VitamDocument<?> getDocumentByUniqueId(String id, FunctionalAdminCollections collection, String field) {
        if (collection.isMultitenant()) {
            Integer tenantId = ParameterHelper.getTenantParameter();
            return (VitamDocument<?>) collection.getCollection().find(and(eq(field, id),
                eq(VitamDocument.TENANT_ID, tenantId))).first();
        }
        return (VitamDocument<?>) collection.getCollection().find(eq(field, id)).first();
    }

    @Override
    public DbRequestResult findDocuments(JsonNode select, FunctionalAdminCollections collection)
        throws ReferentialException {
        try {
            final SelectParserSingle parser = new SelectParserSingle(collection.getVarNameAdapater());
            parser.parse(select);
            final DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection(), this.ontologyLoader,
                indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(null));
            return dbrequest.execute(parser.getRequest());
        } catch (final DatabaseException | BadRequestException | InvalidParseOperationException |
            InvalidCreateOperationException | VitamDBException | SchemaValidationException e) {
            throw new ReferentialException("find Document Exception", e);
        }
    }

    @Override
    public DbRequestResult deleteDocument(JsonNode delete, FunctionalAdminCollections collection)
        throws ReferentialException, BadRequestException, SchemaValidationException {
        try {
            final DeleteParserSingle parser = new DeleteParserSingle(collection.getVarNameAdapater());
            parser.parse(delete);
            final DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection(), this.ontologyLoader,
                indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(null));
            return dbrequest.execute(parser.getRequest());
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new BadRequestException(e);
        } catch (DatabaseException | VitamDBException e) {
            throw new ReferentialException("delete Document Exception", e);
        }
    }

    @Override
    public DbRequestResult updateData(JsonNode update, FunctionalAdminCollections collection, Integer version)
        throws ReferentialException, SchemaValidationException, BadRequestException {
        try {
            final UpdateParserSingle parser = new UpdateParserSingle(collection.getVarNameAdapater());
            parser.parse(update);
            final DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection(), this.ontologyLoader,
                indexManager.getElasticsearchIndexAliasResolver(collection).resolveIndexName(null));
            DocumentValidator documentValidator = ReferentialDocumentValidators.getValidator(collection);
            final DbRequestResult result = dbrequest.execute(parser.getRequest(), documentValidator);
            if (result.getDiffs().size() == 0) {
                throw new BadRequestException("Document was not updated as there is no changes");
            }
            return result;
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new BadRequestException(e);
        } catch (final DatabaseException | VitamDBException e) {
            throw new ReferentialException("find Document Exception", e);
        }
    }

    @Override
    public void replaceDocument(JsonNode document, String identifierValue, String identifierKey,
        FunctionalAdminCollections vitamCollection) throws DatabaseException {
        final DbRequestSingle dbRequest =
            new DbRequestSingle(vitamCollection.getVitamCollection(), this.ontologyLoader,
                indexManager.getElasticsearchIndexAliasResolver(vitamCollection).resolveIndexName(null));

        dbRequest.replaceDocument(document, identifierValue, identifierKey, vitamCollection.getVitamCollection());
    }

    @Override
    public DbRequestResult updateData(JsonNode update, FunctionalAdminCollections collection)
        throws ReferentialException, SchemaValidationException, BadRequestException {
        return updateData(update, collection, 0);
    }

    @Override
    public DbRequestResult insertDocument(JsonNode json, FunctionalAdminCollections collection)
        throws ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        return insertDocuments(JsonHandler.createArrayNode().add(json), collection);
    }

}
