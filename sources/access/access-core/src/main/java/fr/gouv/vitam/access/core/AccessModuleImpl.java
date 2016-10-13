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
package fr.gouv.vitam.access.core;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.ProcessingException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.api.AccessModule;
import fr.gouv.vitam.access.api.DataCategory;
import fr.gouv.vitam.access.common.exception.AccessExecutionException;
import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.client.MetaDataClient;
import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCycleClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

/**
 * AccessModuleImpl implements AccessModule
 */
// TODO: fully externalize logbook part if possible (like helper)
public class AccessModuleImpl implements AccessModule {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessModuleImpl.class);
    private LogbookLifeCycleClient logbookLifeCycleClient;
    private LogbookClient logbookOperationClient;
    private final StorageClient storageClient;

    private final AccessConfiguration accessConfiguration;

    private MetaDataClient metaDataClient;
    private static final String DEFAULT_STORAGE_STRATEGY = "default";

    private static final String ID_CHECK_FAILED = "the unit_id should be filled";
    private String eventType = "Update_archive_unit_unitary";

    //TODO setting in other place
    private Integer tenantId = 0;

    /**
     * AccessModuleImpl constructor
     *
     * @param configuration of mongoDB access
     */
    // constructor
    public AccessModuleImpl(AccessConfiguration configuration) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        accessConfiguration = configuration;
        storageClient = StorageClientFactory.getInstance().getStorageClient();
    }

    /**
     * For testing purpose only
     * @param metaDataClientFactory {@link MetaDataClientFactory} the metadata client factory
     * @param configuration {@link AccessConfiguration} access configuration
     * @param storageClient a StorageClient instance
     */
    AccessModuleImpl(AccessConfiguration configuration, StorageClient
        storageClient) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        this.accessConfiguration = configuration;
        this.storageClient = storageClient;
    }

    /**
     * AccessModuleImpl constructor <br>
     * with metaDataClientFactory, configuration and logbook operation client and lifecycle
     *
     * @param metaDataClientFactory {@link MetaDataClientFactory} the metadata client factory
     * @param configuration {@link AccessConfiguration} access configuration
     * @param pLogbookOperationClient logbook operation client
     * @param pLogbookLifeCycleClient logbook lifecycle client
     */
    public AccessModuleImpl(AccessConfiguration configuration,
                            LogbookClient pLogbookOperationClient, LogbookLifeCycleClient pLogbookLifeCycleClient) {
        this(configuration);
        logbookOperationClient = pLogbookOperationClient==null ?
                LogbookClientFactory.getInstance().getLogbookOperationClient() : pLogbookOperationClient;
        logbookLifeCycleClient = pLogbookLifeCycleClient==null ?
                LogbookLifeCyclesClientFactory.getInstance().getLogbookLifeCyclesClient() : pLogbookLifeCycleClient;
    }

    /**
     * select Unit
     *
     * @param jsonQuery as String { $query : query}
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessExecutionException Throw if error occurs when send Unit to database
     */
    @Override
    public JsonNode selectUnit(JsonNode jsonQuery)
        throws IllegalArgumentException, InvalidParseOperationException, AccessExecutionException {

        JsonNode jsonNode = null;

        try {
            metaDataClient = MetaDataClientFactory.create(accessConfiguration.getUrlMetaData());

            jsonNode = metaDataClient.selectUnits(jsonQuery.toString());

        } catch (final InvalidParseOperationException e) {
            LOGGER.error("parsing error", e);
            throw e;
        } catch (IllegalArgumentException e) {
            LOGGER.error("illegal argument", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("exeption thrown", e);
            throw new AccessExecutionException(e);
        }
        return jsonNode;
    }

    /**
     * select Unit by Id
     *
     * @param jsonQuery as String { $query : query}
     * @param idUnit as String
     * @throws IllegalArgumentException Throw if json format is not correct
     * @throws AccessExecutionException Throw if error occurs when send Unit to database
     */


    @Override
    public JsonNode selectUnitbyId(JsonNode jsonQuery, String idUnit)
        throws IllegalArgumentException, InvalidParseOperationException, AccessExecutionException {
        return selectMetadataDocumentById(jsonQuery, idUnit, DataCategory.UNIT);
    }

    private JsonNode selectMetadataDocumentById(JsonNode jsonQuery, String idDocument, DataCategory dataCategory)
        throws InvalidParseOperationException, AccessExecutionException {
        JsonNode jsonNode;
        ParametersChecker.checkParameter("Data category ", dataCategory);
        if (StringUtils.isBlank(idDocument)) {
            throw new IllegalArgumentException();
        }

        try {
            metaDataClient = MetaDataClientFactory.create(accessConfiguration.getUrlMetaData());

            switch (dataCategory) {
                case UNIT:
                    jsonNode = metaDataClient.selectUnitbyId(jsonQuery.toString(), idDocument);
                    break;
                case OBJECT_GROUP:
                    // TODO: metadata should return NotFound if the objectGroup is not found
                    jsonNode = metaDataClient.selectObjectGrouptbyId(jsonQuery.toString(), idDocument);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported category " + dataCategory);
            }
        // TODO: ProcessingException should probably be handled by clients ?
        } catch (MetadataInvalidSelectException | MetaDataDocumentSizeException | MetaDataExecutionException |
            ProcessingException e) {
            throw new AccessExecutionException(e);
        }
        return jsonNode;
    }

    @Override
    public JsonNode selectObjectGroupById(JsonNode jsonQuery, String idObjectGroup)
        throws InvalidParseOperationException, AccessExecutionException {
        return selectMetadataDocumentById(jsonQuery, idObjectGroup, DataCategory.OBJECT_GROUP);
    }

    @Override
    public InputStream getOneObjectFromObjectGroup(String idObjectGroup, JsonNode queryJson, String qualifier,
        int version, String tenantId) throws MetaDataNotFoundException, StorageNotFoundException, AccessExecutionException,
        InvalidParseOperationException {
        ParametersChecker.checkParameter("ObjectGroup id should be filled", idObjectGroup);
        ParametersChecker.checkParameter("You must specify a valid object qualifier", qualifier);
        ParametersChecker.checkParameter("You must specify a valid tenant", tenantId);
        ParametersChecker.checkValue("version", version, 0);
        // TODO : add 'space' and 'tab' handling in checkParameter
        ParametersChecker.checkParameter("ObjectGroup id should be filled", idObjectGroup.trim());
        ParametersChecker.checkParameter("You must specify a valid object qualifier", qualifier.trim());
        ParametersChecker.checkParameter("You must specify a valid tenant", tenantId.trim());

        SelectParserMultiple selectRequest = new SelectParserMultiple();
        selectRequest.parse(queryJson);
        Select request = selectRequest.getRequest();
        request.reset().addRoots(idObjectGroup);
        // TODO : create helper to build this kind of projection
        // TODO : it would be nice to be able to handle $slice in projection via builder
        request.parseProjection("{\"$fields\":{\"_qualifiers."+ qualifier.trim() + ".versions\": { $slice: [" + version + "," +
            "1]},\"_id\":0," + "\"_qualifiers."+ qualifier.trim() + ".versions._id\":1}}");
        JsonNode jsonResponse = selectObjectGroupById(request.getFinalSelect(), idObjectGroup);
        if (jsonResponse == null) {
            throw new AccessExecutionException("Null json response node from metadata");
        }
        List<String> valuesAsText = jsonResponse.get("$result").findValuesAsText("_id");
        if (valuesAsText.size() > 1) {
            String ids = valuesAsText.stream().reduce((s, s2) -> s + ", " + s2).get();
            throw new AccessExecutionException("More than one object founds. Ids are : " + ids);
        }
        String objectId = valuesAsText.get(0);
        try {
            return storageClient.getContainer(tenantId, DEFAULT_STORAGE_STRATEGY, objectId, StorageCollectionType.OBJECTS);
        } catch (StorageServerClientException e) {
            throw new AccessExecutionException(e);
        }
    }

    /**
     * update Unit by id
     *
     * @param queryJson json update query
     * @param idUnit   as String
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessExecutionException       Throw if error occurs when send Unit to database
     * @throws IllegalArgumentException       Throw if error occurs when checking argument
     */
    @Override
    public JsonNode updateUnitbyId(JsonNode queryJson, String idUnit) throws IllegalArgumentException, InvalidParseOperationException, AccessExecutionException {
        LogbookOperationParameters logbookOpParamStart, logbookOpParamEnd;
        LogbookLifeCycleUnitParameters logbookLCParamStart, logbookLCParamEnd;
        if (StringUtils.isEmpty(idUnit)) {
            throw new IllegalArgumentException(ID_CHECK_FAILED);
        }

        // eventidentifierprocess for lifecycle
        final GUID updateOpGuidStart = GUIDFactory.newOperationIdGUID(tenantId);

        try {

            metaDataClient = MetaDataClientFactory.create(accessConfiguration.getUrlMetaData());

            logbookOperationClient = logbookOperationClient == null
                ? LogbookClientFactory.getInstance().getLogbookOperationClient() : logbookOperationClient;

            logbookLifeCycleClient = logbookLifeCycleClient == null
                ? LogbookLifeCyclesClientFactory.getInstance().getLogbookLifeCyclesClient() : logbookLifeCycleClient;

            // Create logbook operation
                // TODO: interest of this private method ?
            logbookOpParamStart = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                LogbookOutcome.STARTED, "update archiveunit:" + idUnit, idUnit);
            logbookOperationClient.create(logbookOpParamStart);

            // update logbook lifecycle
            // TODO: interest of this private method ?
            logbookLCParamStart = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, LogbookOutcome.STARTED,
                queryJson.toString(), queryJson.toString(), idUnit);
            logbookLifeCycleClient.update(logbookLCParamStart);

            //call update
            JsonNode jsonNode = metaDataClient.updateUnitbyId(queryJson.toString(), idUnit);

            // TODO: interest of this private method ?
            logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                LogbookOutcome.OK, "update archiveunit:" + idUnit, idUnit);
            logbookOperationClient.update(logbookOpParamEnd);

            // update logbook lifecycle
            // TODO: interest of this private method ?
            logbookLCParamEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, LogbookOutcome.OK,
                queryJson.toString(), queryJson.toString(), idUnit);
            logbookLCParamEnd.putParameterValue(LogbookParameterName.eventDetailData, getDiffMessageFor(jsonNode, idUnit));
            logbookLifeCycleClient.update(logbookLCParamEnd);

            // commit logbook lifecycle
            logbookLifeCycleClient.commit(logbookLCParamEnd);
            
            return jsonNode;

        } catch (final InvalidParseOperationException ipoe) {
            rollBackLogbook(updateOpGuidStart, queryJson, idUnit);
            LOGGER.error("parsing error", ipoe);
            throw ipoe;
        } catch (IllegalArgumentException iae) {
            rollBackLogbook(updateOpGuidStart, queryJson, idUnit);
            LOGGER.error("illegal argument", iae);
            throw iae;
        } catch (MetaDataDocumentSizeException mddse) {
            rollBackLogbook(updateOpGuidStart, queryJson, idUnit);
            LOGGER.error("metadata document size error", mddse);
            throw  new AccessExecutionException(mddse);
        } catch (LogbookClientServerException lcse) {
            rollBackLogbook(updateOpGuidStart, queryJson, idUnit);
            LOGGER.error("document client server error", lcse);
            throw new AccessExecutionException(lcse);
        } catch (MetaDataExecutionException mdee) {
            rollBackLogbook(updateOpGuidStart, queryJson, idUnit);
            LOGGER.error("metadata execution execution error", mdee);
            throw new AccessExecutionException(mdee);
        } catch (LogbookClientNotFoundException lcnfe) {
            rollBackLogbook(updateOpGuidStart, queryJson, idUnit);
            LOGGER.error("logbook client not found error", lcnfe);
            throw new AccessExecutionException(lcnfe);
        } catch (LogbookClientBadRequestException lcbre) {
            rollBackLogbook(updateOpGuidStart, queryJson, idUnit);
            LOGGER.error("logbook client bad request error", lcbre);
            throw new AccessExecutionException(lcbre);
        } catch (LogbookClientAlreadyExistsException e) {
            LOGGER.error("logbook operation already exists", e);
            throw new AccessExecutionException(e);
        }        
    }

    private void rollBackLogbook(GUID updateOpGuidStart, JsonNode queryJson, String objectIdentifier) {
        try {
            // TODO: interest of this private method ?
            LogbookOperationParameters logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    LogbookOutcome.ERROR, "Echec de l'écriture de la mise à jour des métadonnées", objectIdentifier);
            logbookOperationClient.update(logbookOpParamEnd);
            // TODO: interest of this private method ?
            LogbookLifeCycleUnitParameters logbookParametersEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, LogbookOutcome.ERROR,
                    queryJson.toString(), queryJson.toString(), objectIdentifier);
            logbookLifeCycleClient.rollback(logbookParametersEnd);
        } catch (LogbookClientBadRequestException lcbre) {
            LOGGER.error("bad request", lcbre);
        } catch (LogbookClientNotFoundException lcbre) {
            LOGGER.error("client not found", lcbre);
        } catch (LogbookClientServerException lcse) {
            LOGGER.error("client server error", lcse);
        }
    }

    private LogbookLifeCycleUnitParameters getLogbookLifeCycleUpdateUnitParameters(GUID eventIdentifierProcess, LogbookOutcome logbookOutcome, String outcomeDetail,
                                                                                   String outcomeDetailMessage, String objectIdentifier) {
        LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newUnitGUID(tenantId); //eventidentifier
        return LogbookParametersFactory.newLogbookLifeCycleUnitParameters
            (updateGuid, eventType, eventIdentifierProcess, eventTypeProcess, logbookOutcome, "update archive unit",
                "update unit " + objectIdentifier, objectIdentifier);
    }

    private LogbookOperationParameters getLogbookOperationUpdateUnitParameters(GUID eventIdentifier, GUID eventIdentifierProcess, LogbookOutcome logbookOutcome,
                                                                               String outcomeDetailMessage, String eventIdentifierRequest) {
        LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters
            (eventIdentifier,eventType,eventIdentifierProcess,eventTypeProcess,logbookOutcome,outcomeDetailMessage,
                eventIdentifierRequest);
        parameters.putParameterValue(LogbookParameterName.objectIdentifier, eventIdentifierRequest);
        return parameters;
    }

    private String getDiffMessageFor(JsonNode diff, String unitId) throws InvalidParseOperationException {
        JsonNode arrayNode = diff.has("$diff") ? diff.get("$diff") : diff.get("$result");
        for (JsonNode diffNode : arrayNode) {
            if (diffNode.get("_id") != null && unitId.equals(diffNode.get("_id").textValue())) {
                return JsonHandler.writeAsString(diffNode.get("_diff"));
            }
        }
        // TODO : empty string or error because no diff for this id ?
        return "";
    }
}
