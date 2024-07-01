/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

package fr.gouv.vitam.metadata.audit.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoClient;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.server.OplogReader;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.server.application.configuration.DataConsistencyAuditConfig;
import fr.gouv.vitam.common.server.application.configuration.DbConfiguration;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.server.application.configuration.MongoDbShard;
import fr.gouv.vitam.common.server.application.configuration.MongoDbShardConf;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.BsonTimestamp;
import org.bson.Document;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.CharsetUtils.UTF_8;
import static fr.gouv.vitam.common.database.server.OplogReader.COLLECTION_NAME;
import static fr.gouv.vitam.common.database.server.OplogReader.FIELD_ID;
import static fr.gouv.vitam.common.database.server.OplogReader.FIELD_INCREMENT;
import static fr.gouv.vitam.common.database.server.OplogReader.FIELD_TIME;
import static fr.gouv.vitam.common.database.server.OplogReader.OPERATION_TIME;
import static fr.gouv.vitam.common.model.VitamConstants.JSON_EXTENSION;
import static fr.gouv.vitam.common.model.WorkspaceConstants.TMP_FILE_NAME_FOR_SHARDS_CONFIG;
import static java.util.Map.Entry;

public class MetadataAuditService {

    private static final String CONTEXT_METADATA_AUDIT = "METADATA_AUDIT";
    private static final String DATABASE_METADATA_AUDIT = "METADATA_AUDIT_DATABASE";
    private static final String LOGBOOK_METADATA_AUDIT = "METADATA_AUDIT_LOGBOOK";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataAuditService.class);
    private static final String AUDIT_CONTAINER_NAME = "dataConsistencyAuditContainer";
    private static final String CODE_VITAM = "code_vitam";
    private static final String AUDIT_DATA_CONSISTENCY_EVT = "DATA_CONSISTENCY_AUDIT";
    private static final String DB_NAME = "admin";

    private final WorkspaceClientFactory workspaceClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final VitamRepositoryProvider vitamRepositoryProvider;
    private final ElasticsearchMetadataIndexManager indexManager;
    private static final AlertService alertService = new AlertServiceImpl();
    private VitamElasticsearchRepository elasticsearchUnitsRepository;
    private VitamElasticsearchRepository elasticsearchGotRepository;
    private VitamMongoRepository mongoRepositoryForUnits;
    private VitamMongoRepository mongoRepositoryForGot;

    private static DataConsistencyAuditConfig dataConsistencyAuditConfig = new DataConsistencyAuditConfig();
    private GUID eip;

    @VisibleForTesting
    public MetadataAuditService(
        WorkspaceClientFactory workspaceClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        VitamRepositoryProvider vitamRepositoryProvider,
        ElasticsearchMetadataIndexManager indexManager,
        Boolean isDataConsistencyAuditRunnable,
        Integer dataConsistencyAuditOplogMaxSize,
        MongoDbShardConf mongodShardsConf,
        boolean dbAuthentication
    ) {
        this.workspaceClientFactory = workspaceClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.indexManager = indexManager;
        dataConsistencyAuditConfig = new DataConsistencyAuditConfig(
            isDataConsistencyAuditRunnable,
            dataConsistencyAuditOplogMaxSize,
            mongodShardsConf,
            dbAuthentication
        );
    }

    public Response auditDataConsistencyMongoEs()
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        Status responseStatus = Response.Status.OK;
        Object entityResponse;
        LogbookOperationsClient logbookClient = logbookOperationsClientFactory.getClient();
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (isAuditAlreadyRunning(logbookClient)) {
                responseStatus = Status.BAD_REQUEST;
                entityResponse = new VitamError(responseStatus.name())
                    .setHttpCode(responseStatus.getStatusCode())
                    .setContext(CONTEXT_METADATA_AUDIT)
                    .setState(CODE_VITAM)
                    .setMessage(responseStatus.getReasonPhrase())
                    .setDescription("A data consistency audit is already running.");
                return Response.status(responseStatus).entity(entityResponse).build();
            }

            if (
                !dataConsistencyAuditConfig.getIsDataConsistencyAuditRunnable() ||
                dataConsistencyAuditConfig.getDataConsistencyAuditOplogMaxSize() == null ||
                dataConsistencyAuditConfig.getDataConsistencyAuditOplogMaxSize() == 0
            ) {
                responseStatus = Status.BAD_REQUEST;
                entityResponse = new VitamError(responseStatus.name())
                    .setHttpCode(responseStatus.getStatusCode())
                    .setContext(CONTEXT_METADATA_AUDIT)
                    .setState(CODE_VITAM)
                    .setMessage(responseStatus.getReasonPhrase())
                    .setDescription("The data consistency audit params are not correct.");
                return Response.status(responseStatus).entity(entityResponse).build();
            }

            if (
                dataConsistencyAuditConfig.getMongodShardsConf() == null ||
                dataConsistencyAuditConfig.getMongodShardsConf().getMongoDbShards() == null ||
                dataConsistencyAuditConfig.getMongodShardsConf().getMongoDbShards().isEmpty()
            ) {
                responseStatus = Response.Status.BAD_REQUEST;
                entityResponse = new VitamError(responseStatus.name())
                    .setHttpCode(responseStatus.getStatusCode())
                    .setContext(CONTEXT_METADATA_AUDIT)
                    .setState(CODE_VITAM)
                    .setMessage(responseStatus.getReasonPhrase())
                    .setDescription("At least one shard is required for MongoDB configuration.");
                return Response.status(responseStatus).entity(entityResponse).build();
            }

            eip = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
            LOGGER.debug("Audit data consistency : Start Reading Config");
            Map<String, BsonTimestamp> shardsTimeStampConfigMap = new HashMap<>();
            startLogbookForAudit(logbookClient);
            getShardsConfig(shardsTimeStampConfigMap, workspaceClient);

            LOGGER.debug("Audit data consistency : Start Reading Oplog");
            mongoRepositoryForUnits = vitamRepositoryProvider.getVitamMongoRepository(
                MetadataCollections.UNIT.getVitamCollection()
            );
            mongoRepositoryForGot = vitamRepositoryProvider.getVitamMongoRepository(
                MetadataCollections.OBJECTGROUP.getVitamCollection()
            );
            Map<String, ? extends MetadataDocument<?>> metadataOplogFromMongo = getOplogDocumentsFromShards(
                shardsTimeStampConfigMap,
                workspaceClient
            );

            LOGGER.debug("Audit data consistency : Start Reading indexes from ES");
            elasticsearchUnitsRepository = vitamRepositoryProvider.getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                indexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
            );
            elasticsearchGotRepository = vitamRepositoryProvider.getVitamESRepository(
                MetadataCollections.OBJECTGROUP.getVitamCollection(),
                indexManager.getElasticsearchIndexAliasResolver(MetadataCollections.OBJECTGROUP)
            );
            Map<String, ? extends MetadataDocument<?>> metadataOpLogFromEs = metadataOplogFromMongo
                .entrySet()
                .stream()
                .map(elmt -> new SimpleEntry<>(elmt.getKey(), getDocumentFromEs(elmt)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            Map<String, String> incoherantData = generateIncoherantDocuments(
                metadataOplogFromMongo,
                metadataOpLogFromEs
            );

            LOGGER.debug("Audit data consistency : Update Logbook with {} resulted data", incoherantData.size());
            if (incoherantData.isEmpty()) {
                successLogbookForAudit(logbookClient);
            } else {
                rectifyDataInElasticsearch(logbookClient, metadataOplogFromMongo, metadataOpLogFromEs, incoherantData);
            }

            Map<String, Object> responseResults = Map.of(
                "requestId",
                eip.getId(),
                "IncoherantDataSize",
                incoherantData.size()
            );
            entityResponse = JsonHandler.toJsonNode(responseResults);
            LOGGER.debug("Audit data consistency : End of audit");
        } catch (
            ContentAddressableStorageNotFoundException
            | IOException
            | InvalidParseOperationException
            | ContentAddressableStorageServerException
            | InvalidGuidOperationException e
        ) {
            LOGGER.error(e);
            errorLogbookForAudit(logbookClient, e.getMessage());
            responseStatus = Response.Status.BAD_REQUEST;
            entityResponse = new VitamError(responseStatus.name())
                .setHttpCode(responseStatus.getStatusCode())
                .setContext(CONTEXT_METADATA_AUDIT)
                .setState(CODE_VITAM)
                .setMessage(responseStatus.getReasonPhrase())
                .setDescription(e.getMessage());
        } catch (
            LogbookClientBadRequestException
            | LogbookClientAlreadyExistsException
            | LogbookClientServerException
            | LogbookClientNotFoundException e
        ) {
            errorLogbookForAudit(logbookClient, e.getMessage());
            LOGGER.error(e);
            responseStatus = Response.Status.BAD_REQUEST;
            entityResponse = new VitamError(responseStatus.name())
                .setHttpCode(responseStatus.getStatusCode())
                .setContext(LOGBOOK_METADATA_AUDIT)
                .setState(CODE_VITAM)
                .setMessage(responseStatus.getReasonPhrase())
                .setDescription(e.getMessage());
        } catch (DatabaseException e) {
            errorLogbookForAudit(logbookClient, e.getMessage());
            LOGGER.error(e);
            responseStatus = Response.Status.BAD_REQUEST;
            entityResponse = new VitamError(responseStatus.name())
                .setHttpCode(responseStatus.getStatusCode())
                .setContext(DATABASE_METADATA_AUDIT)
                .setState(CODE_VITAM)
                .setMessage(responseStatus.getReasonPhrase())
                .setDescription(e.getMessage());
        }
        return Response.status(responseStatus).entity(entityResponse).build();
    }

    private void rectifyDataInElasticsearch(
        LogbookOperationsClient logbookClient,
        Map<String, ? extends MetadataDocument<?>> metadataOplogFromMongo,
        Map<String, ? extends MetadataDocument<?>> metadataOpLogFromEs,
        Map<String, String> incoherantData
    )
        throws DatabaseException, InvalidParseOperationException, LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        LOGGER.debug("Audit data consistency : Start updating incoherants documents in ES");
        alertService.createAlert(
            incoherantData.size() + " incoherant documents detected when running data consistency audit"
        );
        updateIncoherantDataFromEs(metadataOplogFromMongo, incoherantData);
        deleteIncoherantDataFromEs(metadataOplogFromMongo, metadataOpLogFromEs, incoherantData);

        Map<String, ? extends MetadataDocument<?>> metadataOpLogFromEsAfterUpdate = metadataOplogFromMongo
            .entrySet()
            .stream()
            .map(elmt -> new SimpleEntry<>(elmt.getKey(), getDocumentFromEs(elmt)))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        Map<String, String> incoherantDataAfterUpdate = generateIncoherantDocuments(
            metadataOplogFromMongo,
            metadataOpLogFromEsAfterUpdate
        );
        if (incoherantDataAfterUpdate.isEmpty()) {
            final String msgWarnToDisplay = String.format(
                "Audit data consistency : %s document(s) have been updated in ES",
                incoherantData.size()
            );
            LOGGER.warn(msgWarnToDisplay);
            warningLogbookForAudit(logbookClient, msgWarnToDisplay);
        } else {
            final String msgErrorToDisplay = "Audit data consistency : A problem occured when updating documents in ES";
            LOGGER.error(msgErrorToDisplay);
            errorLogbookForAudit(logbookClient, msgErrorToDisplay);
        }
    }

    private void deleteIncoherantDataFromEs(
        Map<String, ? extends MetadataDocument<?>> metadataOplogFromMongo,
        Map<String, ? extends MetadataDocument<?>> metadataOpLogFromEs,
        Map<String, String> incoherantData
    ) {
        Set<String> documentIdsToDeleteInES = incoherantData
            .keySet()
            .stream()
            .filter(elmt -> metadataOplogFromMongo.get(elmt).isEmpty())
            .collect(Collectors.toSet());
        if (!documentIdsToDeleteInES.isEmpty()) {
            List<? extends MetadataDocument> documentsToDelete = metadataOpLogFromEs
                .values()
                .stream()
                .filter(elmt -> documentIdsToDeleteInES.contains(elmt.getId()))
                .collect(Collectors.toList());
            Map<Pair<Integer, Boolean>, List<String>> documentsToDeleteByTenant = documentsToDelete
                .stream()
                .collect(
                    Collectors.toMap(
                        elmt -> Pair.of(elmt.getTenantId(), elmt instanceof Unit),
                        elmt -> new ArrayList<>(Collections.singleton(elmt.getId())),
                        (oldElmt, newElmt) -> {
                            oldElmt.addAll(newElmt);
                            return oldElmt;
                        }
                    )
                );
            documentsToDeleteByTenant
                .keySet()
                .forEach(key -> {
                    try {
                        if (key.getRight()) {
                            elasticsearchUnitsRepository.delete(documentsToDeleteByTenant.get(key), key.getLeft());
                        } else {
                            elasticsearchGotRepository.delete(documentsToDeleteByTenant.get(key), key.getLeft());
                        }
                    } catch (DatabaseException e) {
                        LOGGER.error(e.getMessage());
                    }
                });
        }
    }

    private void updateIncoherantDataFromEs(
        Map<String, ? extends MetadataDocument<?>> metadataOplogFromMongo,
        Map<String, String> incoherantData
    ) throws DatabaseException {
        List<Document> documentsToUpdateInES = metadataOplogFromMongo
            .values()
            .stream()
            .filter(elmt -> incoherantData.containsKey(elmt.get(FIELD_ID)))
            .collect(Collectors.toList());
        elasticsearchUnitsRepository.save(
            documentsToUpdateInES.stream().filter(elmt -> elmt instanceof Unit).collect(Collectors.toList())
        );
        elasticsearchGotRepository.save(
            documentsToUpdateInES.stream().filter(elmt -> elmt instanceof ObjectGroup).collect(Collectors.toList())
        );
    }

    private boolean isAuditAlreadyRunning(LogbookOperationsClient logbookClient)
        throws LogbookClientServerException, InvalidParseOperationException {
        RequestResponse<JsonNode> lastOperationByType = logbookClient.getLastOperationByType(
            AUDIT_DATA_CONSISTENCY_EVT
        );
        if (
            lastOperationByType instanceof RequestResponseOK &&
            !((RequestResponseOK<JsonNode>) lastOperationByType).getResults().isEmpty()
        ) {
            LogbookOperation lastLogbook = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) lastOperationByType).getResults().get(0),
                LogbookOperation.class
            );
            return lastLogbook.getEvents().isEmpty();
        }
        return false;
    }

    private MetadataDocument<?> getDocumentFromEs(Entry<String, ? extends MetadataDocument<?>> entrySet) {
        return entrySet.getValue() instanceof Unit
            ? new Unit(getIndexByDocument(elasticsearchUnitsRepository, entrySet))
            : new ObjectGroup(getIndexByDocument(elasticsearchGotRepository, entrySet));
    }

    private Map<String, String> generateIncoherantDocuments(
        Map<String, ? extends MetadataDocument<?>> metadataOplogFromMongo,
        Map<String, ? extends MetadataDocument<?>> metadataOpLogFromEs
    ) throws InvalidParseOperationException {
        Map<String, String> incoherantData = new HashMap<>();
        for (String documentId : metadataOplogFromMongo.keySet()) {
            if (metadataOpLogFromEs.containsKey(documentId)) {
                JsonNode documentFromMongoJsonNode = JsonHandler.toJsonNode(metadataOplogFromMongo.get(documentId));
                JsonNode documentFromEsJsonNode = JsonHandler.toJsonNode(metadataOpLogFromEs.get(documentId));
                JsonNode patch = JsonDiff.asJson(documentFromMongoJsonNode, documentFromEsJsonNode);
                if (!patch.isEmpty()) {
                    incoherantData.put(documentId, "Incoherant Data detected : " + patch.toString());
                }
            } else {
                incoherantData.put(documentId, "The key dont exist in ES");
            }
        }
        return incoherantData;
    }

    private Document getIndexByDocument(
        VitamElasticsearchRepository elasticsearchRepository,
        Map.Entry<String, ? extends MetadataDocument<?>> elmt
    ) {
        try {
            Document result = elasticsearchRepository.getDocumentById(elmt.getKey()).orElse(new Document());
            if (!result.isEmpty()) {
                result.put(FIELD_ID, elmt.getKey());
            }
            return result;
        } catch (DatabaseException e) {
            LOGGER.info("{} does not exist in ES", elmt.getKey());
            return new Document();
        }
    }

    private void getShardsConfig(Map<String, BsonTimestamp> shardsTimeStampConfigMap, WorkspaceClient workspaceClient)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException, InvalidParseOperationException, IOException {
        if (workspaceClient.isExistingContainer(AUDIT_CONTAINER_NAME)) {
            Map<String, Object> mapFromObject;
            final InputStream stream = (InputStream) workspaceClient
                .getObject(AUDIT_CONTAINER_NAME, TMP_FILE_NAME_FOR_SHARDS_CONFIG + JSON_EXTENSION)
                .getEntity();
            mapFromObject = JsonHandler.getMapFromString(IOUtils.toString(stream, UTF_8));
            shardsTimeStampConfigMap.putAll(
                mapFromObject
                    .entrySet()
                    .stream()
                    .map(e -> new SimpleEntry<>(e.getKey(), convertToBsonTimestamp(e.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        } else {
            workspaceClient.createContainer(AUDIT_CONTAINER_NAME);
            for (MongoDbShard mongoDbShard : dataConsistencyAuditConfig.getMongodShardsConf().getMongoDbShards()) {
                mongoDbShard
                    .getMongoDbNodes()
                    .forEach(
                        dbNode ->
                            shardsTimeStampConfigMap.put(
                                mongoDbShard.getShardName() + ":" + dbNode.getDbHost() + ":" + dbNode.getDbPort(),
                                null
                            )
                    );
            }
            storeDbShardsConfig(shardsTimeStampConfigMap, workspaceClient);
        }
    }

    private Map<String, ? extends MetadataDocument<?>> getOplogDocumentsFromShards(
        Map<String, BsonTimestamp> shardsTimeStampConfigMap,
        WorkspaceClient workspaceClient
    ) {
        boolean isShardsTimeStampConfigMapTouched = false;
        final String dbUserName = dataConsistencyAuditConfig.getMongodShardsConf().getDbUserName();
        final String dbUserPassword = dataConsistencyAuditConfig.getMongodShardsConf().getDbPassword();
        final List<String> collectionsToScan = Arrays.stream(MetadataCollections.values())
            .map(collection -> collection.getCollection().getNamespace().getFullName())
            .collect(Collectors.toList());
        Map<String, MetadataDocument<?>> metadataOplogDocuments = new HashMap<>();
        for (MongoDbShard mongoDbShard : dataConsistencyAuditConfig.getMongodShardsConf().getMongoDbShards()) {
            for (MongoDbNode dbNode : mongoDbShard.getMongoDbNodes()) {
                // Create OplogInstance
                OplogReader oplogReader = createOplogReaderInstance(
                    dbUserName,
                    dbUserPassword,
                    dbNode,
                    dataConsistencyAuditConfig.isDbAuthentication(),
                    dataConsistencyAuditConfig.getDataConsistencyAuditOplogMaxSize()
                );
                // Read Oplog from node
                final String shardNameForMapConfig =
                    mongoDbShard.getShardName() + ":" + dbNode.getDbHost() + ":" + dbNode.getDbPort();
                Map<String, Document> oplogByShard = oplogReader.readDocumentsFromOplogByShardAndCollections(
                    collectionsToScan,
                    shardsTimeStampConfigMap.get(shardNameForMapConfig)
                );
                if (!oplogByShard.isEmpty()) {
                    if (metadataOplogDocuments.isEmpty()) {
                        LOGGER.info(
                            oplogByShard.size() +
                            " operations will be added for data consistency audit from " +
                            shardNameForMapConfig
                        );
                        metadataOplogDocuments.putAll(getDocumentsFromMongo(oplogByShard));
                    } else {
                        Map<String, Document> oplogByShardToAdd = oplogByShard
                            .entrySet()
                            .stream()
                            .filter(elmt -> !metadataOplogDocuments.keySet().contains(elmt.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        LOGGER.info(
                            oplogByShardToAdd.size() +
                            " operations will be added for data consistency audit from " +
                            shardNameForMapConfig
                        );
                        metadataOplogDocuments.putAll(getDocumentsFromMongo(oplogByShardToAdd));
                    }
                    BsonTimestamp maxTimeStampScanned = oplogByShard
                        .values()
                        .stream()
                        .map(elmt -> ((BsonTimestamp) elmt.get(OPERATION_TIME)))
                        .max(BsonTimestamp::compareTo)
                        .get();
                    if (
                        shardsTimeStampConfigMap.get(shardNameForMapConfig) == null ||
                        maxTimeStampScanned.compareTo(shardsTimeStampConfigMap.get(shardNameForMapConfig)) > 0
                    ) {
                        isShardsTimeStampConfigMapTouched = true;
                        shardsTimeStampConfigMap.put(shardNameForMapConfig, maxTimeStampScanned);
                    }
                }
            }
        }
        // Store map in file in workspace if there is changes
        if (isShardsTimeStampConfigMapTouched) {
            storeDbShardsConfig(shardsTimeStampConfigMap, workspaceClient);
        }
        return metadataOplogDocuments;
    }

    private OplogReader createOplogReaderInstance(
        String dbUserName,
        String dbUserPassword,
        MongoDbNode dbNode,
        Boolean dbAuthentication,
        Integer dataConsistencyAuditOplogMaxSize
    ) {
        LOGGER.info("Connecting to MongoDB Shard Node");

        DbConfiguration dbConfiguration = new DbConfigurationImpl(
            List.of(dbNode),
            DB_NAME,
            dbAuthentication,
            dbUserName,
            dbUserPassword
        );
        MongoClient mongoClient = MongoDbAccess.createMongoClient(dbConfiguration);

        return new OplogReader(mongoClient, dataConsistencyAuditOplogMaxSize);
    }

    private Map<String, MetadataDocument<?>> getDocumentsFromMongo(Map<String, Document> documentsToScan) {
        return documentsToScan
            .entrySet()
            .stream()
            .map(
                elmt ->
                    new SimpleEntry<>(
                        elmt.getKey(),
                        elmt
                                .getValue()
                                .get(COLLECTION_NAME)
                                .toString()
                                .equals(MetadataCollections.UNIT.getCollection().getNamespace().getFullName())
                            ? getDocumentFromMongo(Unit.class, elmt)
                            : getDocumentFromMongo(ObjectGroup.class, elmt)
                    )
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private MetadataDocument<?> getDocumentFromMongo(
        Class<? extends MetadataDocument> clasz,
        Entry<String, Document> elmt
    ) {
        try {
            if (clasz == Unit.class) {
                Document unitDoc = mongoRepositoryForUnits.getByID(elmt.getKey(), null).orElse(new Document());
                if (!unitDoc.isEmpty()) {
                    return (Unit) unitDoc;
                } else {
                    return new Unit();
                }
            } else {
                Document gotDoc = mongoRepositoryForGot.getByID(elmt.getKey(), null).orElse(new Document());
                if (!gotDoc.isEmpty()) {
                    return (ObjectGroup) gotDoc;
                } else {
                    return new ObjectGroup();
                }
            }
        } catch (DatabaseException e) {
            LOGGER.info("{} does not exist in Mongo", elmt.getKey());
            return null;
        }
    }

    private void startLogbookForAudit(LogbookOperationsClient logbookClient)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationParameters logbookParametersForAuditStart =
            LogbookParameterHelper.newLogbookOperationParameters(
                eip,
                AUDIT_DATA_CONSISTENCY_EVT,
                eip,
                LogbookTypeProcess.DATA_CONSISTENCY_AUDIT,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(AUDIT_DATA_CONSISTENCY_EVT, StatusCode.STARTED),
                eip
            );
        logbookClient.create(logbookParametersForAuditStart);
    }

    private void successLogbookForAudit(LogbookOperationsClient logbookClient)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(VitamConfiguration.getAdminTenant());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            AUDIT_DATA_CONSISTENCY_EVT,
            eip,
            LogbookTypeProcess.DATA_CONSISTENCY_AUDIT,
            StatusCode.OK,
            VitamLogbookMessages.getCodeOp(AUDIT_DATA_CONSISTENCY_EVT, StatusCode.OK),
            eip
        );
        logbookClient.update(logbookParameters);
    }

    private void warningLogbookForAudit(LogbookOperationsClient logbookClient, String message)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(VitamConfiguration.getAdminTenant());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            AUDIT_DATA_CONSISTENCY_EVT,
            eip,
            LogbookTypeProcess.DATA_CONSISTENCY_AUDIT,
            StatusCode.WARNING,
            VitamLogbookMessages.getCodeOp(AUDIT_DATA_CONSISTENCY_EVT, StatusCode.WARNING),
            eip
        );
        final ObjectNode msgJson = JsonHandler.createObjectNode();
        msgJson.put("AuditWarnCheck", message);
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, msgJson.toString());
        logbookClient.update(logbookParameters);
    }

    private void errorLogbookForAudit(LogbookOperationsClient logbookClient, String message)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(VitamConfiguration.getAdminTenant());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            AUDIT_DATA_CONSISTENCY_EVT,
            eip,
            LogbookTypeProcess.DATA_CONSISTENCY_AUDIT,
            StatusCode.KO,
            VitamLogbookMessages.getCodeOp(AUDIT_DATA_CONSISTENCY_EVT, StatusCode.KO),
            eip
        );
        final ObjectNode msgJson = JsonHandler.createObjectNode();
        msgJson.put("AuditErrorCheck", message);
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, msgJson.toString());
        logbookClient.update(logbookParameters);
    }

    private BsonTimestamp convertToBsonTimestamp(Object value) {
        try {
            JsonNode valueToJsonNode = JsonHandler.toJsonNode(value);
            if (
                !valueToJsonNode.isEmpty() &&
                valueToJsonNode.get(FIELD_TIME) != null &&
                valueToJsonNode.get(FIELD_INCREMENT) != null
            ) {
                return new BsonTimestamp(
                    valueToJsonNode.get(FIELD_TIME).asInt(),
                    valueToJsonNode.get(FIELD_INCREMENT).asInt()
                );
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    private void storeDbShardsConfig(
        Map<String, BsonTimestamp> shardsTimeStampConfigMap,
        WorkspaceClient workspaceClient
    ) {
        try {
            final File firstMapTmpFile = File.createTempFile(TMP_FILE_NAME_FOR_SHARDS_CONFIG, JSON_EXTENSION);
            JsonHandler.writeAsFile(JsonHandler.toJsonNode(shardsTimeStampConfigMap), firstMapTmpFile);
            workspaceClient.putObject(
                AUDIT_CONTAINER_NAME,
                TMP_FILE_NAME_FOR_SHARDS_CONFIG + JSON_EXTENSION,
                firstMapTmpFile
            );
        } catch (IOException | InvalidParseOperationException | ContentAddressableStorageServerException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
