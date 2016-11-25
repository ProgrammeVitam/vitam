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
package fr.gouv.vitam.access.internal.core;

import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.access.internal.api.AccessBinaryData;
import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.api.DataCategory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server2.application.AsyncInputStreamHelper;
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
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
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
public class AccessInternalModuleImpl implements AccessInternalModule {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalModuleImpl.class);
    private final LogbookLifeCyclesClient logbookLifeCycleClientMock;
    private final LogbookOperationsClient logbookOperationClientMock;
    private final StorageClient storageClientMock;

    private static final String DEFAULT_STORAGE_STRATEGY = "default";

    private static final String ID_CHECK_FAILED = "the unit_id should be filled";

    private static final String STP_UPDATE_UNIT = "STP_UPDATE_UNIT";

    // TODO P1 setting in other place
    private final Integer tenantId = 0;

    /**
     * AccessModuleImpl constructor
     *
     * @param configuration of mongoDB access
     */
    // constructor
    public AccessInternalModuleImpl(AccessInternalConfiguration configuration) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        storageClientMock = null;
        logbookLifeCycleClientMock = null;
        logbookOperationClientMock = null;
    }

    /**
     * AccessModuleImpl constructor <br>
     * with metaDataClientFactory, configuration and logbook operation client and lifecycle
     * 
     * @param storageClient a StorageClient instance
     * @param pLogbookOperationClient logbook operation client
     * @param pLogbookLifeCycleClient logbook lifecycle client
     */
    AccessInternalModuleImpl(StorageClient storageClient, LogbookOperationsClient pLogbookOperationClient,
        LogbookLifeCyclesClient pLogbookLifeCycleClient) {
        this.storageClientMock = storageClient;
        logbookOperationClientMock = pLogbookOperationClient;
        logbookLifeCycleClientMock = pLogbookLifeCycleClient;
    }

    /**
     * select Unit
     *
     * @param jsonQuery as String { $query : query}
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     */
    @Override
    public JsonNode selectUnit(JsonNode jsonQuery)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException {

        JsonNode jsonNode = null;

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(jsonQuery);
            // Check correctness of request
            RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
            parser.getRequest().reset();
            if (!(parser instanceof SelectParserMultiple)) {
                throw new InvalidParseOperationException("Not a Select operation");
            }
            jsonNode = metaDataClient.selectUnits(jsonQuery);

        } catch (final InvalidParseOperationException e) {
            LOGGER.error("parsing error", e);
            throw e;
        } catch (final IllegalArgumentException e) {
            LOGGER.error("illegal argument", e);
            throw e;
        } catch (final Exception e) {
            LOGGER.error("exeption thrown", e);
            throw new AccessInternalExecutionException(e);
        }
        return jsonNode;
    }

    /**
     * select Unit by Id
     *
     * @param jsonQuery as String { $query : query}
     * @param idUnit as String
     * @throws IllegalArgumentException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     */


    @Override
    public JsonNode selectUnitbyId(JsonNode jsonQuery, String idUnit)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException {
        // Check correctness of request
        RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
        parser.getRequest().reset();
        if (!(parser instanceof SelectParserMultiple)) {
            throw new InvalidParseOperationException("Not a Select operation");
        }
        return selectMetadataDocumentById(jsonQuery, idUnit, DataCategory.UNIT);
    }

    private JsonNode selectMetadataDocumentById(JsonNode jsonQuery, String idDocument, DataCategory dataCategory)
        throws InvalidParseOperationException, AccessInternalExecutionException {
        JsonNode jsonNode;
        ParametersChecker.checkParameter("Data category ", dataCategory);
        ParametersChecker.checkParameter("idDocument is empty", idDocument);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            switch (dataCategory) {
                case UNIT:
                    jsonNode = metaDataClient.selectUnitbyId(jsonQuery, idDocument);
                    break;
                case OBJECT_GROUP:
                    // FIXME P1: metadata should return NotFound if the objectGroup is not found
                    jsonNode = metaDataClient.selectObjectGrouptbyId(jsonQuery, idDocument);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported category " + dataCategory);
            }
            // TODO P1 : ProcessingException should probably be handled by clients ?
        } catch (MetadataInvalidSelectException | MetaDataDocumentSizeException | MetaDataExecutionException |
            ProcessingException | MetaDataClientServerException e) {
            throw new AccessInternalExecutionException(e);
        }
        return jsonNode;
    }

    @Override
    public JsonNode selectObjectGroupById(JsonNode jsonQuery, String idObjectGroup)
        throws InvalidParseOperationException, AccessInternalExecutionException {
        // Check correctness of request
        RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
        parser.getRequest().reset();
        if (!(parser instanceof SelectParserMultiple)) {
            throw new InvalidParseOperationException("Not a Select operation");
        }
        return selectMetadataDocumentById(jsonQuery, idObjectGroup, DataCategory.OBJECT_GROUP);
    }

    @Override
    public AccessBinaryData getOneObjectFromObjectGroup(AsyncResponse asyncResponse, String idObjectGroup,
        JsonNode queryJson, String qualifier, int version, String tenantId)
        throws MetaDataNotFoundException, StorageNotFoundException, AccessInternalExecutionException,
        InvalidParseOperationException {
        ParametersChecker.checkParameter("ObjectGroup id should be filled", idObjectGroup);
        ParametersChecker.checkParameter("You must specify a valid object qualifier", qualifier);
        ParametersChecker.checkParameter("You must specify a valid tenant", tenantId);
        ParametersChecker.checkValue("version", version, 0);

        final SelectParserMultiple selectRequest = new SelectParserMultiple();
        selectRequest.parse(queryJson);
        final Select request = selectRequest.getRequest();
        request.reset().addRoots(idObjectGroup);
        // FIXME P1: we should find a better way to do that than use json, like a POJO.
        request.setProjectionSliceOnQualifier(qualifier, version, "FormatIdentification", "FileInfo");

        final JsonNode jsonResponse = selectObjectGroupById(request.getFinalSelect(), idObjectGroup);
        if (jsonResponse == null) {
            throw new AccessInternalExecutionException("Null json response node from metadata");
        }

        // FIXME P1: do not use direct access but POJO
        final List<String> valuesAsText = jsonResponse.get("$results").findValuesAsText("_id");
        if (valuesAsText.size() > 1) {
            final String ids = valuesAsText.stream().reduce((s, s2) -> s + ", " + s2).get();
            throw new AccessInternalExecutionException("More than one object founds. Ids are : " + ids);
        }
        LOGGER.debug(JsonHandler.prettyPrint(jsonResponse.get("$results").get(0)));
        String mimetype = null;
        String filename = null;


        final List<String> mimeTypesAsText = jsonResponse.get("$results").findValuesAsText("MimeType");
        if (mimeTypesAsText.size() > 1) {
            final String multipleMimetype = mimeTypesAsText.stream().reduce((s, s2) -> s + ", " + s2).get();
            LOGGER.warn("Multiple mimetypes found {}, using the first.", multipleMimetype);
        }
        if (!mimeTypesAsText.isEmpty()) {
            mimetype = mimeTypesAsText.get(0);
        }

        final List<String> fileNamesAsText = jsonResponse.get("$results").findValuesAsText("Filename");
        if (fileNamesAsText.size() > 1) {
            final String multipleFilenames = fileNamesAsText.stream().reduce((s, s2) -> s + ", " + s2).get();
            LOGGER.warn("Multiple filenames found {}, using the first", multipleFilenames);
        }
        if (!fileNamesAsText.isEmpty()) {
            filename = fileNamesAsText.get(0);
        }

        if (Strings.isNullOrEmpty(mimetype)) {
            mimetype = MediaType.APPLICATION_OCTET_STREAM;
        }
        final String objectId = valuesAsText.get(0);
        if (Strings.isNullOrEmpty(filename)) {
            filename = objectId;
        }
        StorageClient storageClient =
            storageClientMock == null ? StorageClientFactory.getInstance().getClient() : storageClientMock;
        try {
            Response response = storageClient.getContainerAsync(tenantId, DEFAULT_STORAGE_STRATEGY, objectId,
                StorageCollectionType.OBJECTS);
            AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            ResponseBuilder responseBuilder = Response.status(Status.OK).header(GlobalDataRest.X_QUALIFIER, qualifier)
                .header(GlobalDataRest.X_VERSION, version)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .type(mimetype);
            helper.writeResponse(responseBuilder);
            return new AccessBinaryData(filename, mimetype, response);
        } catch (final StorageServerClientException e) {
            throw new AccessInternalExecutionException(e);
        } finally {
            if (storageClientMock == null && storageClient != null) {
                storageClient.close();
            }
        }
    }

    /**
     * update Unit by id
     *
     * @param queryJson json update query
     * @param idUnit as String
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     * @throws IllegalArgumentException Throw if error occurs when checking argument
     */
    @Override
    public JsonNode updateUnitbyId(JsonNode queryJson, String idUnit)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException {
        LogbookOperationParameters logbookOpParamStart, logbookOpParamEnd;
        LogbookLifeCycleUnitParameters logbookLCParamStart, logbookLCParamEnd;
        ParametersChecker.checkParameter(ID_CHECK_FAILED, idUnit);
        int tenant = tenantId;
        final GUID idGUID;
        try {
            idGUID = GUIDReader.getGUID(idUnit);
            tenant = idGUID.getTenantId();
        } catch (InvalidGuidOperationException e) {
            throw new IllegalArgumentException("idUnit is not a valid GUID", e);
        }
        // Check Request is really an Update
        RequestParserMultiple parser = RequestParserHelper.getParser(queryJson);
        if (!(parser instanceof UpdateParserMultiple)) {
            parser.getRequest().reset();
            throw new IllegalArgumentException("Request is not an update operation");
        }
        // eventidentifierprocess for lifecycle
        final GUID updateOpGuidStart = GUIDFactory.newOperationLogbookGUID(tenant);
        JsonNode newQuery = queryJson;
        try {
            newQuery = ((UpdateParserMultiple) parser).getRequest()
                .addActions(UpdateActionHelper.push(VitamFieldsHelper.operations(), updateOpGuidStart.toString()))
                .getFinalUpdate();
        } catch (InvalidCreateOperationException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        LogbookOperationsClient logbookOperationClient = logbookOperationClientMock;
        LogbookLifeCyclesClient logbookLifeCycleClient = logbookLifeCycleClientMock;
        if (logbookOperationClient == null) {
            logbookOperationClient = LogbookOperationsClientFactory.getInstance().getClient();
        }
        if (logbookLifeCycleClient == null) {
            logbookLifeCycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        }
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            // Create logbook operation
            logbookOpParamStart = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                StatusCode.STARTED, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.STARTED), idGUID);
            logbookOperationClient.create(logbookOpParamStart);

            // update logbook lifecycle
            logbookLCParamStart = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.STARTED,
                idGUID);
            logbookLifeCycleClient.update(logbookLCParamStart);

            // call update
            final JsonNode jsonNode = metaDataClient.updateUnitbyId(newQuery, idUnit);

            logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                StatusCode.OK, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.OK), idGUID);
            logbookOperationClient.update(logbookOpParamEnd);

            // update logbook lifecycle
            logbookLCParamEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.OK,
                idGUID);
            logbookLCParamEnd.putParameterValue(LogbookParameterName.eventDetailData,
                getDiffMessageFor(jsonNode, idUnit));
            logbookLifeCycleClient.update(logbookLCParamEnd);

            // commit logbook lifecycle
            logbookLifeCycleClient.commit(logbookLCParamEnd);

            return jsonNode;

        } catch (final InvalidParseOperationException ipoe) {
            rollBackLogbook(logbookLifeCycleClient, logbookOperationClient, updateOpGuidStart, newQuery, idGUID);
            LOGGER.error("parsing error", ipoe);
            throw ipoe;
        } catch (final IllegalArgumentException iae) {
            rollBackLogbook(logbookLifeCycleClient, logbookOperationClient, updateOpGuidStart, newQuery, idGUID);
            LOGGER.error("illegal argument", iae);
            throw iae;
        } catch (final MetaDataDocumentSizeException mddse) {
            rollBackLogbook(logbookLifeCycleClient, logbookOperationClient, updateOpGuidStart, newQuery, idGUID);
            LOGGER.error("metadata document size error", mddse);
            throw new AccessInternalExecutionException(mddse);
        } catch (final LogbookClientServerException lcse) {
            rollBackLogbook(logbookLifeCycleClient, logbookOperationClient, updateOpGuidStart, newQuery, idGUID);
            LOGGER.error("document client server error", lcse);
            throw new AccessInternalExecutionException(lcse);
        } catch (final MetaDataExecutionException mdee) {
            rollBackLogbook(logbookLifeCycleClient, logbookOperationClient, updateOpGuidStart, newQuery, idGUID);
            LOGGER.error("metadata execution execution error", mdee);
            throw new AccessInternalExecutionException(mdee);
        } catch (final LogbookClientNotFoundException lcnfe) {
            rollBackLogbook(logbookLifeCycleClient, logbookOperationClient, updateOpGuidStart, newQuery, idGUID);
            LOGGER.error("logbook client not found error", lcnfe);
            throw new AccessInternalExecutionException(lcnfe);
        } catch (final LogbookClientBadRequestException lcbre) {
            rollBackLogbook(logbookLifeCycleClient, logbookOperationClient, updateOpGuidStart, newQuery, idGUID);
            LOGGER.error("logbook client bad request error", lcbre);
            throw new AccessInternalExecutionException(lcbre);
        } catch (final LogbookClientAlreadyExistsException e) {
            LOGGER.error("logbook operation already exists", e);
            throw new AccessInternalExecutionException(e);
        } catch (MetaDataClientServerException e) {
            LOGGER.error("Metadata internal server error", e);
            rollBackLogbook(logbookLifeCycleClient, logbookOperationClient, updateOpGuidStart, newQuery, idGUID);
            throw new AccessInternalExecutionException(e);
        } finally {
            if (logbookLifeCycleClientMock == null && logbookLifeCycleClient != null) {
                logbookLifeCycleClient.close();
            }
            if (logbookOperationClientMock == null && logbookOperationClient != null) {
                logbookOperationClient.close();
            }
        }
    }

    private void rollBackLogbook(LogbookLifeCyclesClient logbookLifeCycleClient,
        LogbookOperationsClient logbookOperationClient, GUID updateOpGuidStart, JsonNode queryJson,
        GUID objectIdentifier) {
        try {
            final LogbookOperationParameters logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.KO, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.KO), objectIdentifier);
            logbookOperationClient.update(logbookOpParamEnd);
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
        StatusCode logbookOutcome, GUID objectIdentifier) {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newUnitGUID(tenantId); // eventidentifier
        return LogbookParametersFactory.newLogbookLifeCycleUnitParameters(updateGuid, STP_UPDATE_UNIT,
            eventIdentifierProcess,
            eventTypeProcess, logbookOutcome, "update archive unit",
            "update unit " + objectIdentifier, objectIdentifier);
    }

    private LogbookOperationParameters getLogbookOperationUpdateUnitParameters(GUID eventIdentifier,
        GUID eventIdentifierProcess, StatusCode logbookOutcome,
        String outcomeDetailMessage, GUID eventIdentifierRequest) {
        final LogbookOperationParameters parameters =
            LogbookParametersFactory.newLogbookOperationParameters(eventIdentifier,
                STP_UPDATE_UNIT, eventIdentifierProcess, LogbookTypeProcess.UPDATE, logbookOutcome,
                outcomeDetailMessage,
                eventIdentifierRequest);
        parameters.putParameterValue(LogbookParameterName.objectIdentifier, eventIdentifierRequest.getId());
        return parameters;
    }

    private String getDiffMessageFor(JsonNode diff, String unitId) throws InvalidParseOperationException {
        if (diff == null) {
            return "";
        }
        final JsonNode arrayNode = diff.has("$diff") ? diff.get("$diff") : diff.get("$results");
        if (arrayNode == null) {
            return "";
        }
        for (final JsonNode diffNode : arrayNode) {
            if (diffNode.get("#id") != null && unitId.equals(diffNode.get("#id").textValue())) {
                return JsonHandler.writeAsString(diffNode.get("#diff"));
            }
        }
        // TODO P1 : empty string or error because no diff for this id ?
        return "";
    }
}
