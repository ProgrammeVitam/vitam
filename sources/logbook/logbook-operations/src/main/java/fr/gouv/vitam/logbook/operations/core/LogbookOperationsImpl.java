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
package fr.gouv.vitam.logbook.operations.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.ExistsQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.ReindexationKO;
import fr.gouv.vitam.common.database.index.model.ReindexationOK;
import fr.gouv.vitam.common.database.index.model.ReindexationResult;
import fr.gouv.vitam.common.database.index.model.SwitchIndexResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.server.mongodb.VitamMongoCursor;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
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
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.not;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.outcomeDetail;
import static java.util.function.Predicate.not;

/**
 * Logbook Operations implementation base class
 */
public class LogbookOperationsImpl implements LogbookOperations {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationsImpl.class);

    private final LogbookDbAccess mongoDbAccess;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final IndexationHelper indexationHelper;
    private final ElasticsearchLogbookIndexManager indexManager;

    public LogbookOperationsImpl(LogbookDbAccess mongoDbAccess,
        ElasticsearchLogbookIndexManager indexManager) {
        this(mongoDbAccess, WorkspaceClientFactory.getInstance(), StorageClientFactory.getInstance(),
            IndexationHelper.getInstance(), indexManager);
    }

    @VisibleForTesting
    public LogbookOperationsImpl(LogbookDbAccess mongoDbAccess, WorkspaceClientFactory workspaceClientFactory,
        StorageClientFactory storageClientFactory, IndexationHelper indexationHelper,
        ElasticsearchLogbookIndexManager indexManager) {
        this.mongoDbAccess = mongoDbAccess;
        this.workspaceClientFactory = workspaceClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.indexationHelper = indexationHelper;
        this.indexManager = indexManager;
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
        return select(select, false);
    }

    @Override
    public RequestResponse<LogbookOperation> selectOperations(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, VitamDBException {
        return selectOperations(select, false);
    }

    @Override
    public RequestResponseOK<LogbookOperation> selectOperations(JsonNode select, boolean sliced)
            throws VitamDBException, LogbookNotFoundException, LogbookDatabaseException {
        VitamMongoCursor<LogbookOperation> cursor = mongoDbAccess.getLogbookOperations(select, sliced);
        List<LogbookOperation> operations = new ArrayList<>();
        while (cursor.hasNext()) {
            LogbookOperation doc = cursor.next();
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

        DatabaseCursor hits = (cursor.getScrollId() != null) ?
                new DatabaseCursor(cursor.getTotal(), offset, limit, operations.size(), cursor.getScrollId())
                :
                new DatabaseCursor(cursor.getTotal(), offset, limit, operations.size());
        return new RequestResponseOK<LogbookOperation>(select)
                .addAllResults(operations).setHits(hits);
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
        try {
            return mongoDbAccess.getLogbookOperations(select.getFinalSelect(), false).next();
        } catch (VitamDBException e) {
            throw new LogbookDatabaseException(e);
        }
    }

    @Override
    public LogbookOperation findLastLifecycleTraceabilityOperation(String eventType, boolean traceabilityWithZipOnly) throws VitamException {
        try {
            final Select query = new Select();
            final Query type = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
            final Query eventStatus = QueryHelper
                .in(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.outcomeDetail.getDbname()),
                    eventType + ".OK", eventType + ".WARNING");

            BooleanQuery add = and().add(type, eventStatus);
            if(traceabilityWithZipOnly) {
                ExistsQuery hasTraceabilityFile = exists("events.evDetData.FileName");
                add.add(hasTraceabilityFile);
            }
            query.setQuery(add);
            query.setLimitFilter(0, 1);

            query.addOrderByDescFilter("evDateTime");

            List<LogbookOperation> operations = select(query.getFinalSelect());
            if (operations.isEmpty()) {
                return null;
            }
            return operations.get(0);

        } catch (LogbookNotFoundException e) {
            LOGGER.debug("Logbook not found, this is the first Operation of this type");
            return null;
        } catch (InvalidCreateOperationException e) {
            throw new VitamException("Could not find last LFC traceability", e);
        }
    }

    @Override
    public ReindexationResult reindex(IndexParameters indexParameters) {
        LogbookCollections collection;
        try {
            collection = LogbookCollections.valueOf(indexParameters.getCollectionName().toUpperCase());
        } catch (IllegalArgumentException exc) {
            String message = "Invalid collection '" + indexParameters.getCollectionName() + "'";
            LOGGER.error(message, exc);
            return indexationHelper.getFullKOResult(indexParameters, message);
        }
        if (!LogbookCollections.OPERATION.equals(collection)) {
            String message = String.format("Try to reindex a non operation logbook collection '%s' with operation " +
                "logbook module", indexParameters.getCollectionName());
            LOGGER.error(message);
            return indexationHelper.getFullKOResult(indexParameters, message);
        }

        if (CollectionUtils.isEmpty(indexParameters.getTenants())) {
            String message = String.format("Missing tenants for %s collection reindexation",
                indexParameters.getCollectionName());
            LOGGER.error(message);
            return indexationHelper.getFullKOResult(indexParameters, message);
        }

        ReindexationResult reindexationResult = new ReindexationResult();
        reindexationResult.setCollectionName(indexParameters.getCollectionName());

        processDedicatedTenants(indexParameters, collection, reindexationResult);
        processGroupedTenants(indexParameters, collection, reindexationResult);

        return reindexationResult;
    }

    private void processDedicatedTenants(IndexParameters indexParameters, LogbookCollections collection,
        ReindexationResult reindexationResult) {

        ElasticsearchIndexAliasResolver indexAliasResolver =
            indexManager.getElasticsearchIndexAliasResolver(collection);

        List<Integer> dedicatedTenantToProcess = indexParameters.getTenants().stream()
            .filter(not(this.indexManager::isGroupedTenant))
            .collect(Collectors.toList());

        for (Integer tenantId : dedicatedTenantToProcess) {
            try {
                ReindexationOK reindexResult = this.indexationHelper.reindex(collection.getCollection(),
                    collection.getEsClient(), indexAliasResolver.resolveIndexName(tenantId),
                    this.indexManager.getElasticsearchIndexSettings(collection, tenantId),
                    collection.getElasticsearchCollection(), Collections.singletonList(tenantId), null);
                reindexationResult.addIndexOK(reindexResult);
            } catch (Exception exc) {
                String message =
                    "Cannot reindex collection " + collection.name() + " for tenant " + tenantId + ". Unexpected error";
                LOGGER.error(message, exc);
                reindexationResult.addIndexKO(new ReindexationKO(Collections.singletonList(tenantId), null, message));
            }
        }
    }

    private void processGroupedTenants(IndexParameters indexParameters, LogbookCollections collection,
        ReindexationResult reindexationResult) {
        ElasticsearchIndexAliasResolver indexAliasResolver =
            indexManager.getElasticsearchIndexAliasResolver(collection);

        SetValuedMap<String, Integer> tenantGroupTenantsMap = new HashSetValuedHashMap<>();
        indexParameters.getTenants().stream()
            .filter(this.indexManager::isGroupedTenant)
            .forEach(tenantId -> tenantGroupTenantsMap.put(this.indexManager.getTenantGroup(tenantId), tenantId));

        for (String tenantGroupName : tenantGroupTenantsMap.keySet()) {
            Collection<Integer> allTenantGroupTenants = this.indexManager.getTenantGroupTenants(tenantGroupName);
            if (allTenantGroupTenants.size() != tenantGroupTenantsMap.get(tenantGroupName).size()) {
                SetUtils.SetView<Integer> missingTenants = SetUtils.difference(
                    new HashSet<>(allTenantGroupTenants), tenantGroupTenantsMap.get(tenantGroupName));
                LOGGER.warn("Missing tenants " + missingTenants + " of tenant group " + tenantGroupName +
                    " will also be reindexed for collection " + collection);
            }
        }

        Collection<String> tenantGroupNamesToProcess = new TreeSet<>(tenantGroupTenantsMap.keySet());
        for (String tenantGroupName : tenantGroupNamesToProcess) {
            List<Integer> tenantIds = this.indexManager.getTenantGroupTenants(tenantGroupName);
            try {
                ReindexationOK reindexResult = this.indexationHelper.reindex(collection.getCollection(),
                    collection.getEsClient(), indexAliasResolver.resolveIndexName(tenantIds.get(0)),
                    this.indexManager.getElasticsearchIndexSettings(collection, tenantIds.get(0)),
                    collection.getElasticsearchCollection(), tenantIds, tenantGroupName);
                reindexationResult.addIndexOK(reindexResult);
            } catch (Exception exc) {
                String message = "Cannot reindex collection " + collection.name()
                    + " for tenant group " + tenantGroupName + ". Unexpected error";
                LOGGER.error(message, exc);
                reindexationResult.addIndexKO(new ReindexationKO(tenantIds, tenantGroupName, message));
            }
        }
    }

    @Override
    public SwitchIndexResult switchIndex(String alias, String newIndexName) throws DatabaseException {
        try {
            return indexationHelper.switchIndex(
                ElasticsearchIndexAlias.ofFullIndexName(alias),
                ElasticsearchIndexAlias.ofFullIndexName(newIndexName),
                LogbookCollections.OPERATION.getEsClient());
        } catch (DatabaseException exc) {
            LOGGER.error("Cannot switch alias {} to index {}", alias, newIndexName, exc);
            throw exc;
        }
    }

    @Override
    public boolean checkNewEligibleLogbookOperationsSinceLastTraceabilityOperation(
        LocalDateTime traceabilityStartDate, LocalDateTime traceabilityEndDate)
        throws LogbookDatabaseException {

        try {
            Select select = new Select();
            // Ignore scheduled background operations : TRACEABILITY, STORAGE_BACKUP...
            String[] ignoredBackgroundLogbookTypeProcesses =
                BackgroundLogbookTypeProcessHelper.getBackgroundLogbookTypeProcesses()
                .stream().map(Enum::name).toArray(String[]::new);
            select.setQuery(and()
                .add(gte(VitamFieldsHelper.lastPersistedDate(),
                    LocalDateUtil.getFormattedDateForMongo(traceabilityStartDate)))
                .add(lte(VitamFieldsHelper.lastPersistedDate(),
                    LocalDateUtil.getFormattedDateForMongo(traceabilityEndDate)))
                .add(not().add(in("evTypeProc", ignoredBackgroundLogbookTypeProcesses)))
            );

            // Limit to 1 result
            select.setLimitFilter(0, 1);

            List<LogbookOperation> logbookOperations = select(select.getFinalSelect(), false);
            return !logbookOperations.isEmpty();

        } catch (InvalidCreateOperationException | VitamDBException e) {
            throw new LogbookDatabaseException("Could not parse last traceability operation information", e);
        } catch (LogbookNotFoundException e) {
            LOGGER.debug("No new logbook operations since last traceability", e);
            return false;
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

                storageClient
                    .storeFileFromWorkspace(VitamConfiguration.getDefaultStrategy(), DataCategory.BACKUP_OPERATION,
                        operationGuid,
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
