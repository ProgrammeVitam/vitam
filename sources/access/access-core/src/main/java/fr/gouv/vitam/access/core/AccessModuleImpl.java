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

import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.access.api.AccessBinaryData;
import fr.gouv.vitam.access.api.AccessModule;
import fr.gouv.vitam.access.api.DataCategory;
import fr.gouv.vitam.access.common.exception.AccessExecutionException;
import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
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
    private LogbookLifeCyclesClient logbookLifeCycleClient;
    private LogbookOperationsClient logbookOperationClient;
    private final StorageClient storageClient;

    private final AccessConfiguration accessConfiguration;

    private MetaDataClient metaDataClient;
    private static final String DEFAULT_STORAGE_STRATEGY = "default";

    private static final String ID_CHECK_FAILED = "the unit_id should be filled";
    private static final String EVENT_TYPE = "Update_archive_unit_unitary";

    // TODO setting in other place
    private final Integer tenantId = 0;
    private boolean mock;

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
        mock = false;
    }

    /**
     * For testing purpose only
     *
     * @param metaDataClientFactory {@link MetaDataClientFactory} the metadata client factory
     * @param configuration {@link AccessConfiguration} access configuration
     * @param storageClient a StorageClient instance
     */
    AccessModuleImpl(AccessConfiguration configuration, StorageClient storageClient) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        accessConfiguration = configuration;
        this.storageClient = storageClient;
        mock = false;
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
        LogbookOperationsClient pLogbookOperationClient, LogbookLifeCyclesClient pLogbookLifeCycleClient) {
        this(configuration);
        logbookOperationClient = pLogbookOperationClient == null
            ? LogbookOperationsClientFactory.getInstance().getClient() : pLogbookOperationClient;
        logbookLifeCycleClient = pLogbookLifeCycleClient == null
            ? LogbookLifeCyclesClientFactory.getInstance().getClient() : pLogbookLifeCycleClient;
        mock = pLogbookOperationClient == null;
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
        } catch (final IllegalArgumentException e) {
            LOGGER.error("illegal argument", e);
            throw e;
        } catch (final Exception e) {
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
        ParametersChecker.checkParameter("idDocument is empty", idDocument);

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
    public AccessBinaryData getOneObjectFromObjectGroup(String idObjectGroup,
        JsonNode queryJson, String qualifier, int version, String tenantId)
        throws MetaDataNotFoundException, StorageNotFoundException, AccessExecutionException,
        InvalidParseOperationException {
        ParametersChecker.checkParameter("ObjectGroup id should be filled", idObjectGroup);
        ParametersChecker.checkParameter("You must specify a valid object qualifier", qualifier);
        ParametersChecker.checkParameter("You must specify a valid tenant", tenantId);
        ParametersChecker.checkValue("version", version, 0);

        final SelectParserMultiple selectRequest = new SelectParserMultiple();
        selectRequest.parse(queryJson);
        final Select request = selectRequest.getRequest();
        request.reset().addRoots(idObjectGroup);
        // TODO : create helper to build this kind of projection
        // TODO : it would be nice to be able to handle $slice in projection via builder
        request.parseProjection(
            "{\"$fields\":{\"_qualifiers." + qualifier.trim() + ".versions\": { $slice: [" + version + "," +
                "1]},\"_id\":0," + "\"_qualifiers." + qualifier.trim() + ".versions._id\":1}}");
        final JsonNode jsonResponse = selectObjectGroupById(request.getFinalSelect(), idObjectGroup);
        if (jsonResponse == null) {
            throw new AccessExecutionException("Null json response node from metadata");
        }
        final List<String> valuesAsText = jsonResponse.get("$result").findValuesAsText("_id");
        if (valuesAsText.size() > 1) {
            final String ids = valuesAsText.stream().reduce((s, s2) -> s + ", " + s2).get();
            throw new AccessExecutionException("More than one object founds. Ids are : " + ids);
        }
        String mimetype = null;
        String filename = null;
        JsonNode node = jsonResponse.get("$result").get("FormatIdentification");
        if (node != null) {
            node = node.get("MimeType");
            if (node != null) {
                mimetype = node.asText();
            }
        }
        node = jsonResponse.get("$result").get("FileInfo");
        if (node != null) {
            node = node.get("Filename");
            if (node != null) {
                filename = node.asText();
            }
        }
        if (Strings.isNullOrEmpty(mimetype)) {
            mimetype = MediaType.APPLICATION_OCTET_STREAM;
        }
        final String objectId = valuesAsText.get(0);
        if (Strings.isNullOrEmpty(filename)) {
            filename = objectId;
        }
        try {
            Response response = storageClient.getContainerAsync(tenantId, DEFAULT_STORAGE_STRATEGY, objectId,
                StorageCollectionType.OBJECTS);
            return new AccessBinaryData(filename, mimetype, response);
        } catch (final StorageServerClientException e) {
            throw new AccessExecutionException(e);
        }
    }

    /**
     * update Unit by id
     *
     * @param queryJson json update query
     * @param idUnit as String
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessExecutionException Throw if error occurs when send Unit to database
     * @throws IllegalArgumentException Throw if error occurs when checking argument
     */
    @Override
    public JsonNode updateUnitbyId(JsonNode queryJson, String idUnit)
        throws IllegalArgumentException, InvalidParseOperationException, AccessExecutionException {
        LogbookOperationParameters logbookOpParamStart, logbookOpParamEnd;
        LogbookLifeCycleUnitParameters logbookLCParamStart, logbookLCParamEnd;
        ParametersChecker.checkParameter(ID_CHECK_FAILED, idUnit);
        
        // Check Request is really an Update
        RequestParserMultiple parser = RequestParserHelper.getParser(queryJson);
        if (!(parser instanceof UpdateParserMultiple)) {
            throw new IllegalArgumentException("Request is not an update operation");
        }
        // eventidentifierprocess for lifecycle
        final GUID updateOpGuidStart = GUIDFactory.newEventGUID(tenantId);
        JsonNode newQuery = queryJson;
        try {
            newQuery = ((UpdateParserMultiple) parser).getRequest()
                .addActions(UpdateActionHelper.push(VitamFieldsHelper.operations(), updateOpGuidStart.toString()))
                .getFinalUpdate();
        } catch (InvalidCreateOperationException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {

            metaDataClient = MetaDataClientFactory.create(accessConfiguration.getUrlMetaData());

            if (!mock) {
                logbookOperationClient = LogbookOperationsClientFactory.getInstance().getClient();
                logbookLifeCycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
            }
            // Create logbook operation
            // TODO: interest of this private method ?
            logbookOpParamStart = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                StatusCode.STARTED, "update archiveunit:" + idUnit, idUnit);
            logbookOperationClient.create(logbookOpParamStart);

            // update logbook lifecycle
            logbookLCParamStart = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.STARTED,
                idUnit);
            logbookLifeCycleClient.update(logbookLCParamStart);

            // call update
            final JsonNode jsonNode = metaDataClient.updateUnitbyId(newQuery.toString(), idUnit);

            logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                StatusCode.OK, "update archiveunit:" + idUnit, idUnit);
            logbookOperationClient.update(logbookOpParamEnd);

            // update logbook lifecycle
            logbookLCParamEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.OK,
                idUnit);
            logbookLCParamEnd.putParameterValue(LogbookParameterName.eventDetailData,
                getDiffMessageFor(jsonNode, idUnit));
            logbookLifeCycleClient.update(logbookLCParamEnd);

            // commit logbook lifecycle
            logbookLifeCycleClient.commit(logbookLCParamEnd);

            return jsonNode;

        } catch (final InvalidParseOperationException ipoe) {
            rollBackLogbook(updateOpGuidStart, newQuery, idUnit);
            LOGGER.error("parsing error", ipoe);
            throw ipoe;
        } catch (final IllegalArgumentException iae) {
            rollBackLogbook(updateOpGuidStart, newQuery, idUnit);
            LOGGER.error("illegal argument", iae);
            throw iae;
        } catch (final MetaDataDocumentSizeException mddse) {
            rollBackLogbook(updateOpGuidStart, newQuery, idUnit);
            LOGGER.error("metadata document size error", mddse);
            throw new AccessExecutionException(mddse);
        } catch (final LogbookClientServerException lcse) {
            rollBackLogbook(updateOpGuidStart, newQuery, idUnit);
            LOGGER.error("document client server error", lcse);
            throw new AccessExecutionException(lcse);
        } catch (final MetaDataExecutionException mdee) {
            rollBackLogbook(updateOpGuidStart, newQuery, idUnit);
            LOGGER.error("metadata execution execution error", mdee);
            throw new AccessExecutionException(mdee);
        } catch (final LogbookClientNotFoundException lcnfe) {
            rollBackLogbook(updateOpGuidStart, newQuery, idUnit);
            LOGGER.error("logbook client not found error", lcnfe);
            throw new AccessExecutionException(lcnfe);
        } catch (final LogbookClientBadRequestException lcbre) {
            rollBackLogbook(updateOpGuidStart, newQuery, idUnit);
            LOGGER.error("logbook client bad request error", lcbre);
            throw new AccessExecutionException(lcbre);
        } catch (final LogbookClientAlreadyExistsException e) {
            LOGGER.error("logbook operation already exists", e);
            throw new AccessExecutionException(e);
        } finally {
            if (!mock) {
                logbookOperationClient.close();
                logbookOperationClient = null;
                logbookLifeCycleClient.close();
                logbookLifeCycleClient = null;
            }
        }
    }

    private void rollBackLogbook(GUID updateOpGuidStart, JsonNode queryJson, String objectIdentifier) {
        try {
            // TODO: interest of this private method ?
            final LogbookOperationParameters logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.KO, "Echec de l'écriture de la mise à jour des métadonnées", objectIdentifier);
            logbookOperationClient.update(logbookOpParamEnd);
            // TODO: interest of this private method ?
            final LogbookLifeCycleUnitParameters logbookParametersEnd =
                getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.KO,
                    objectIdentifier);
            logbookLifeCycleClient.rollback(logbookParametersEnd);
        } catch (final LogbookClientBadRequestException lcbre) {
            LOGGER.error("bad request", lcbre);
        } catch (final LogbookClientNotFoundException lcbre) {
            LOGGER.error("client not found", lcbre);
        } catch (final LogbookClientServerException lcse) {
            LOGGER.error("client server error", lcse);
        }
    }

    private LogbookLifeCycleUnitParameters getLogbookLifeCycleUpdateUnitParameters(GUID eventIdentifierProcess,
        StatusCode logbookOutcome, String objectIdentifier) {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newUnitGUID(tenantId); // eventidentifier
        return LogbookParametersFactory.newLogbookLifeCycleUnitParameters(updateGuid, EVENT_TYPE, eventIdentifierProcess,
            eventTypeProcess, logbookOutcome, "update archive unit",
            "update unit " + objectIdentifier, objectIdentifier);
    }

    private LogbookOperationParameters getLogbookOperationUpdateUnitParameters(GUID eventIdentifier,
        GUID eventIdentifierProcess, StatusCode logbookOutcome,
        String outcomeDetailMessage, String eventIdentifierRequest) {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final LogbookOperationParameters parameters =
            LogbookParametersFactory.newLogbookOperationParameters(eventIdentifier,
                EVENT_TYPE, eventIdentifierProcess, eventTypeProcess, logbookOutcome, outcomeDetailMessage,
                eventIdentifierRequest);
        parameters.putParameterValue(LogbookParameterName.objectIdentifier, eventIdentifierRequest);
        return parameters;
    }

    private String getDiffMessageFor(JsonNode diff, String unitId) throws InvalidParseOperationException {
        if (diff == null) {
            return "";
        }
        final JsonNode arrayNode = diff.has("$diff") ? diff.get("$diff") : diff.get("$result");
        if (arrayNode == null) {
            return "";
        }
        for (final JsonNode diffNode : arrayNode) {
            if (diffNode.get("_id") != null && unitId.equals(diffNode.get("_id").textValue())) {
                return JsonHandler.writeAsString(diffNode.get("_diff"));
            }
        }
        // TODO : empty string or error because no diff for this id ?
        return "";
    }
}
