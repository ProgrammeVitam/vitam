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
package fr.gouv.vitam.logbook.operations.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.server.mongodb.VitamMongoCursor;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.outcomeDetail;

/**
 * Logbook Operations implementation base class
 */
public class LogbookOperationsImpl implements LogbookOperations {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationsImpl.class);

    private final LogbookDbAccess mongoDbAccess;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final IndexationHelper indexationHelper;

    /**
     * Constructor
     *
     * @param mongoDbAccess of logbook
     */
    public LogbookOperationsImpl(LogbookDbAccess mongoDbAccess) {
        this(mongoDbAccess, WorkspaceClientFactory.getInstance(), StorageClientFactory.getInstance(),
            IndexationHelper.getInstance());
    }

    @VisibleForTesting
    public LogbookOperationsImpl(LogbookDbAccess mongoDbAccess, WorkspaceClientFactory workspaceClientFactory,
        StorageClientFactory storageClientFactory, IndexationHelper indexationHelper) {
        this.mongoDbAccess = mongoDbAccess;
        this.workspaceClientFactory = workspaceClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.indexationHelper = indexationHelper;
    }

    @Override
    public void create(LogbookOperationParameters parameters)
        throws LogbookAlreadyExistsException,
        LogbookDatabaseException {
        mongoDbAccess.createLogbookOperation(parameters);
        backupOperation(parameters);
    }

    @Override
    public void update(LogbookOperationParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException {
        mongoDbAccess.updateLogbookOperation(parameters);
        backupOperation(parameters);
    }

    @Override
    public List<LogbookOperation> select(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException, VitamDBException {
        // TODO: why true by default ? this is a queryDSL, all the request options are in, so why ?
        List<LogbookOperation> operations = new ArrayList<>();
        operations = select(select, true);
        return operations;
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperations(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, VitamDBException {
        VitamMongoCursor<LogbookOperation> cursor = mongoDbAccess.getLogbookOperations(select, true);
        List<LogbookOperation> operations = new ArrayList<>();
        while (cursor.hasNext()) {
            LogbookOperation doc = (LogbookOperation) cursor.next();
            filterFinalResponse(doc);
            operations.add(doc);
        }
        long offset = 0;
        long limit = 0;
        if (select.get("$filter") != null) {
            if (select.get("$filter").get("$offset") != null) {
                offset = select.get("$filter").get("$offset").asLong();
            }
            if (select.get("$filter").get("$limit") != null) {
                limit = select.get("$filter").get("$limit").asLong();
            }
        }

        DatabaseCursor hitss = (cursor.getScrollId() != null) ?
            new DatabaseCursor(cursor.getTotal(), offset, limit, operations.size(), cursor.getScrollId())
            :
            new DatabaseCursor(cursor.getTotal(), offset, limit, operations.size());
        return new RequestResponseOK<LogbookOperation>(select)
            .addAllResults(operations).setHits(hitss);
    }

    private void filterFinalResponse(VitamDocument<?> document) {
        for (final ParserTokens.PROJECTIONARGS projection : ParserTokens.PROJECTIONARGS.values()) {
            switch (projection) {
                case ID:
                    replace(document, VitamDocument.ID, VitamFieldsHelper.id());
                    break;
                case TENANT:
                    replace(document, VitamDocument.TENANT_ID, VitamFieldsHelper.tenant());

                    break;
                // FIXE ME check if _v is necessary
                case VERSION:
                    replace(document, VitamDocument.VERSION, VitamFieldsHelper.version());
                    break;
                case LAST_PERSISTED_DATE:
                    replace(document, LogbookDocument.LAST_PERSISTED_DATE, VitamFieldsHelper.lastPersistedDate());
                    break;
                case SEDAVERSION:
                    replace(document, VitamDocument.SEDAVERSION, VitamFieldsHelper.sedaVersion());
                    break;
                case IMPLEMENTATIONVERSION:
                    replace(document, VitamDocument.IMPLEMENTATIONVERSION, VitamFieldsHelper.implementationVersion());
                    break;
                default:
                    break;

            }
        }
    }

    private void replace(VitamDocument<?> document, String originalFieldName, String targetFieldName) {
        final Object value = document.remove(originalFieldName);
        if (value != null) {
            document.append(targetFieldName, value);
        }
    }

    @Override
    public List<LogbookOperation> select(JsonNode select, boolean sliced)
        throws LogbookNotFoundException, LogbookDatabaseException, VitamDBException {
        final List<LogbookOperation> result = new ArrayList<>();
        try (final VitamMongoCursor<LogbookOperation> logbook = mongoDbAccess.getLogbookOperations(select, sliced)) {
            if (logbook == null || !logbook.hasNext()) {
                // TODO: seriously, thrown a not found exception here ??? Not found only if I search a specific
                // operation with an ID, not if the logbook is empty ! But fix this and evreything may be broken
                // Should return an empty list i think.
                throw new LogbookNotFoundException("Logbook entry not found");
            }
            while (logbook.hasNext()) {
                LogbookOperation doc = logbook.next();
                filterFinalResponse(doc);
                result.add(doc);
            }
        }
        return result;
    }

    @Override
    public LogbookOperation getById(String idProcess) throws LogbookDatabaseException, LogbookNotFoundException {
        return mongoDbAccess.getLogbookOperation(idProcess);
    }

    @Override
    public final void createBulkLogbookOperation(final LogbookOperationParameters[] operationArray)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        mongoDbAccess.createBulkLogbookOperation(operationArray);
        backupBulkOperation(operationArray);
    }

    @Override
    public final void updateBulkLogbookOperation(final LogbookOperationParameters[] operationArray)
        throws LogbookDatabaseException, LogbookNotFoundException {
        mongoDbAccess.updateBulkLogbookOperation(operationArray);
        backupBulkOperation(operationArray);
    }

    @Override
    public MongoCursor<LogbookOperation> selectOperationsByLastPersistenceDateInterval(LocalDateTime startDate,
        LocalDateTime endDate)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidCreateOperationException,
        InvalidParseOperationException {

        Select select = new Select();
        select.setQuery(QueryHelper.and()
            .add(QueryHelper
                .gte(VitamFieldsHelper.lastPersistedDate(), LocalDateUtil.getFormattedDateForMongo(startDate)))
            .add(QueryHelper
                .lte(VitamFieldsHelper.lastPersistedDate(), LocalDateUtil.getFormattedDateForMongo(endDate))));
        select.addOrderByAscFilter(VitamFieldsHelper.lastPersistedDate());

        MongoCursor<LogbookOperation> cursor = null;
        try {
            cursor =
                mongoDbAccess.getLogbookOperations(select.getFinalSelect(), false);
        } catch (VitamDBException e) {
            LOGGER.error(e);
        }
        return cursor;
    }

    @Override
    public LogbookOperation findFirstTraceabilityOperationOKAfterDate(final LocalDateTime date)
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException {
        final Select select = new Select();
        final Query query = QueryHelper.gt("evDateTime", date.toString());
        final Query type = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
        final Query status =
            QueryHelper.eq(LogbookDocument.EVENTS + "." + outcomeDetail.getDbname(), "STP_OP_SECURISATION.OK");
        select.setQuery(QueryHelper.and().add(query, type, status));
        select.setLimitFilter(0, 1);
        LogbookOperation logbookOperation = null;
        try {
            logbookOperation =
                mongoDbAccess.getLogbookOperations(select.getFinalSelect(), false).next();
        } catch (VitamDBException e) {
            LOGGER.error(e);
        }
        return logbookOperation;
    }

    @Override
    public LogbookOperation findLastTraceabilityOperationOK()
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException,
        InvalidParseOperationException {
        final Select select = new Select();
        final Query type = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
        final Query findEvent = QueryHelper
            .eq(String.format("%s.%s", LogbookDocument.EVENTS, outcomeDetail.getDbname()), "STP_OP_SECURISATION.OK");
        select.setLimitFilter(0, 1);
        select.setQuery(QueryHelper.and().add(type, findEvent));
        select.addOrderByDescFilter("evDateTime");
        LogbookOperation logbookOperation = null;
        try {
            logbookOperation =
                mongoDbAccess.getLogbookOperations(select.getFinalSelect(), false).next();
        } catch (VitamDBException e) {
            LOGGER.error(e);
        }
        return logbookOperation;
    }

    @Override
    public IndexationResult reindex(IndexParameters indexParameters) {
        LogbookCollections collection;
        try {
            collection = LogbookCollections.valueOf(indexParameters.getCollectionName().toUpperCase());
        } catch (IllegalArgumentException exc) {
            String message = String.format("Try to reindex a non operation logbook collection '%s' with operation " +
                    "logbook module",
                indexParameters
                    .getCollectionName());
            LOGGER.error(message);
            return indexationHelper.getFullKOResult(indexParameters, message);
        }
        if (!LogbookCollections.OPERATION.equals(collection)) {
            String message = String.format("Try to reindex a non operation logbook collection '%s' with operation " +
                    "logbook module",
                indexParameters
                    .getCollectionName());
            LOGGER.error(message);
            return indexationHelper.getFullKOResult(indexParameters, message);
        } else {
            MongoCollection<Document> mongoCollection = collection.getCollection();
            try (InputStream mappingStream = ElasticsearchCollections
                .valueOf(indexParameters.getCollectionName().toUpperCase())
                .getMappingAsInputStream()) {
                return indexationHelper.reindex(mongoCollection, collection.getName(), collection.getEsClient(),
                    indexParameters.getTenants(), mappingStream);
            } catch (IOException exc) {
                LOGGER.error("Cannot get '{}' elastic search mapping for tenants {}", collection.name(),
                    indexParameters.getTenants().stream().map(Object::toString).collect(Collectors.joining(", ")));
                return indexationHelper.getFullKOResult(indexParameters, exc.getMessage());
            }
        }

    }

    @Override
    public void switchIndex(String alias, String newIndexName) throws DatabaseException {
        try {
            indexationHelper.switchIndex(alias, newIndexName, LogbookCollections.OPERATION.getEsClient());
        } catch (DatabaseException exc) {
            LOGGER.error("Cannot switch alias {} to index {}", alias, newIndexName, exc);
            throw exc;
        }
    }

    private void backupOperation(LogbookOperationParameters parameters) throws LogbookDatabaseException {
        String operationGuid = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        LogbookOperation logbookOperation;
        try {
            logbookOperation = mongoDbAccess.getLogbookOperation(operationGuid);
        } catch (LogbookNotFoundException e) {
            throw new LogbookDatabaseException("Cannot find operation with GUID " + operationGuid + ", cannot backup " +
                "it", e);
        }
        Integer tenantId = ParameterHelper.getTenantParameter();
        // tenant_backup_operation
        String containerName = tenantId + "_" + DataCategory.BACKUP_OPERATION.getFolder();
        // Ugly hack to mock workspaceFactoryClient
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            workspaceClient.createContainer(containerName);
            workspaceClient.putObject(containerName, operationGuid, JsonHandler.writeToInpustream
                (logbookOperation));
            try (StorageClient storageClient = storageClientFactory.getClient()) {
                ObjectDescription objectDescription = new ObjectDescription();
                objectDescription.setWorkspaceContainerGUID(containerName);
                objectDescription.setObjectName(operationGuid);
                objectDescription.setType(DataCategory.BACKUP_OPERATION);
                objectDescription.setWorkspaceObjectURI(operationGuid);
                storageClient.storeFileFromWorkspace(VitamConfiguration.getDefaultStrategy(), DataCategory.BACKUP_OPERATION, operationGuid,
                    objectDescription);
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e) {
                throw new LogbookDatabaseException("Cannot backup operation with GUID " + operationGuid, e);
            }
            workspaceClient.deleteObject(containerName, operationGuid);
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            LOGGER.warn("Cannot delete temporary backup operation file with GUID {}", operationGuid, e);
        } catch (InvalidParseOperationException e) {
            throw new LogbookDatabaseException("Cannot backup operation with GUID " + operationGuid, e);
        }
    }

    private void backupBulkOperation(LogbookOperationParameters[] parametersArray) throws LogbookDatabaseException {
        for (LogbookOperationParameters parameters : parametersArray) {
            // TODO: better exception management ?
            backupOperation(parameters);
        }
    }
}
