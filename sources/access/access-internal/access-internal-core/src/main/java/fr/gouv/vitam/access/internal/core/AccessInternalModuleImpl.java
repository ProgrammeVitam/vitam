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

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalRuleExecutionException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.exception.ArchiveUnitProfileEmptyControlSchemaException;
import fr.gouv.vitam.common.exception.ArchiveUnitProfileInactiveException;
import fr.gouv.vitam.common.exception.ArchiveUnitProfileNotFoundException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.VitamConstants.AppraisalRuleFinalAction;
import fr.gouv.vitam.common.model.VitamConstants.StorageRuleFinalAction;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.utils.ClassificationLevelUtil;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
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
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import static fr.gouv.vitam.access.internal.core.DslParserHelper.getValueForUpdateDsl;

/**
 * AccessModuleImpl implements AccessModule
 */
public class AccessInternalModuleImpl implements AccessInternalModule {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalModuleImpl.class);
    /**
     * Access contract
     */
    public static final String ACCESS_CONTRACT = "AccessContract";

    private final LogbookLifeCyclesClient logbookLifeCycleClientMock;
    private final LogbookOperationsClient logbookOperationClientMock;
    private final StorageClient storageClientMock;
    private final WorkspaceClient workspaceClientMock;

    private static final String DEFAULT_STORAGE_STRATEGY = "default";
    private static final String ID_CHECK_FAILED = "the unit_id should be filled";
    private static final String STP_UPDATE_UNIT = "STP_UPDATE_UNIT";
    private static final String UNIT_METADATA_UPDATE = "UNIT_METADATA_UPDATE";
    private static final String UNIT_CHECK_RULES = "UNIT_METADATA_UPDATE_CHECK_RULES";
    private static final String UNIT_CHECK_DT = "UNIT_METADATA_UPDATE_CHECK_DT";
    private static final String UNIT_METADATA_STORAGE = "UNIT_METADATA_STORAGE";
    private static final String COMMIT_LIFE_CYCLE_UNIT = "COMMIT_LIFE_CYCLE_UNIT";
    private static final String DOLLAR_DIFF = "$diff";
    private static final String RESULTS = "$results";
    private static final String METADATA_INTERNAL_SERVER_ERROR = "Metadata internal server error";
    private static final String LOGBOOK_OPERATION_ALREADY_EXISTS = "logbook operation already exists";
    private static final String LOGBOOK_CLIENT_BAD_REQUEST_ERROR = "logbook client bad request error";
    private static final String LOGBOOK_CLIENT_NOT_FOUND_ERROR = "logbook client not found error";
    private static final String METADATA_EXECUTION_EXECUTION_ERROR = "metadata execution execution error";
    private static final String METADATA_NOT_FOUND_ERROR = "metadata not found execution error";
    private static final String DOCUMENT_CLIENT_SERVER_ERROR = "document client server error";
    private static final String METADATA_DOCUMENT_SIZE_ERROR = "metadata document size error";
    private static final String ILLEGAL_ARGUMENT = "illegal argument";
    private static final String PARSING_ERROR = "parsing error";
    private static final String CLIENT_SERVER_ERROR = "client server error";
    private static final String CLIENT_NOT_FOUND = "client not found";
    private static final String BAD_REQUEST = "bad request";
    private static final String DIFF = "#diff";
    private static final String ID = "#id";
    private static final String DEFAULT_STRATEGY = "default";


    private static final String WORKSPACE_SERVER_EXCEPTION = "workspace server exception";
    private static final String STORAGE_SERVER_EXCEPTION = "Storage server exception";
    private static final String JSON = ".json";
    private static final String ARCHIVE_UNIT_NOT_FOUND = "Archive unit not found";
    private static final String ERROR_ADD_CONDITION = "Error during adding condition of Operations";
    private static final String ERROR_CHECK_RULES = "Error during checking updated rules";
    private static final String ERROR_VALIDATE_DT = "Error during validation of the document type";
    private static final String DT_NO_EXTISTING = "Archive unit profile could not be found";
    private static final String ERROR_UPDATE_RULE = "Can't Update Rule: ";
    private static final String ERROR_CREATE_RULE = "Can't Create Rule: ";
    private static final String ERROR_DELETE_RULE = "Can't Delete Rule: ";
    private static final String ERROR_PREVENT_INHERITANCE = " contains an inheritance prevention";
    private static final String MANAGEMENT_KEY = "#management";
    private static final String RULES_KEY = "Rules";
    private static final String FINAL_ACTION_KEY = "FinalAction";
    private static final String INHERITANCE_KEY = "Inheritance";
    private static final String PREVENT_INHERITANCE_KEY = "PreventInheritance";
    private static final String PREVENT_RULES_ID_KEY = "PreventRulesId";
    private static final String MANAGEMENT_PREFIX = MANAGEMENT_KEY + '.';
    private static final String RULES_PREFIX = '.' + RULES_KEY;
    private static final String FINAL_ACTION_PREFIX = '.' + FINAL_ACTION_KEY;
    private static final String PREVENT_INHERITANCE_PREFIX = '.' + INHERITANCE_KEY + '.' + PREVENT_INHERITANCE_KEY;
    private static final String NOT_A_SELECT_OPERATION = "Not a Select operation";
    private static final String DATA_CATEGORY = "Data category ";
    private static final String ID_DOC_EMPTY = "idDocument is empty";
    private static final String UNSUPPORTED_CATEGORY = "Unsupported category ";
    private static final String ERROR_CODE = "errorCode";
    private static final String RULE_TYPE = "RuleType";

    /**
     * AccessModuleImpl constructor
     */
    // constructor
    public AccessInternalModuleImpl() {
        storageClientMock = null;
        logbookLifeCycleClientMock = null;
        logbookOperationClientMock = null;
        workspaceClientMock = null;
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
        LogbookLifeCyclesClient pLogbookLifeCycleClient, WorkspaceClient workspaceClient) {
        storageClientMock = storageClient;
        logbookOperationClientMock = pLogbookOperationClient;
        logbookLifeCycleClientMock = pLogbookLifeCycleClient;
        workspaceClientMock = workspaceClient;
    }

    /**
     * select Unit
     *
     * @param jsonQuery as String { $query : query}
     * @throws InvalidParseOperationException   Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     */
    @Override
    public JsonNode selectUnit(JsonNode jsonQuery)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException {

        JsonNode jsonNode = null;
        LOGGER.debug("DEBUG: start selectUnits {}", jsonQuery);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(jsonQuery);
            // Check correctness of request
            LOGGER.debug("{}", jsonNode);
            final RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
            parser.getRequest().reset();
            if (!(parser instanceof SelectParserMultiple)) {
                throw new InvalidParseOperationException(NOT_A_SELECT_OPERATION);
            }
            jsonNode =
                metaDataClient.selectUnits(jsonQuery);
            LOGGER.debug("DEBUG {}", jsonNode);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PARSING_ERROR, e);
            throw e;
        } catch (final IllegalArgumentException e) {
            LOGGER.error(ILLEGAL_ARGUMENT, e);
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
     * @throws IllegalArgumentException         Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     */


    @Override
    public JsonNode selectUnitbyId(JsonNode jsonQuery, String idUnit)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException {
        // Check correctness of request
        final RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
        parser.getRequest().reset();
        if (!(parser instanceof SelectParserMultiple)) {
            throw new InvalidParseOperationException(NOT_A_SELECT_OPERATION);
        }
        return selectMetadataDocumentById(jsonQuery, idUnit, DataCategory.UNIT);
    }

    private JsonNode selectMetadataDocumentById(JsonNode jsonQuery, String idDocument, DataCategory dataCategory)
        throws InvalidParseOperationException, AccessInternalExecutionException {
        JsonNode jsonNode;

        ParametersChecker.checkParameter(DATA_CATEGORY, dataCategory);
        ParametersChecker.checkParameter(ID_DOC_EMPTY, idDocument);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            switch (dataCategory) {
                case UNIT:
                    jsonNode = metaDataClient.selectUnitbyId(jsonQuery, idDocument);
                    break;
                case OBJECTGROUP:
                    jsonNode = metaDataClient.selectObjectGrouptbyId(jsonQuery, idDocument);
                    break;
                default:
                    throw new IllegalArgumentException(UNSUPPORTED_CATEGORY + dataCategory);
            }
            // TODO P1 : ProcessingException should probably be handled by clients ?
        } catch (MetadataInvalidSelectException | MetaDataDocumentSizeException | MetaDataExecutionException |
            ProcessingException | MetaDataClientServerException e) {
            throw new AccessInternalExecutionException(e);
        }
        return jsonNode;
    }

    private JsonNode selectMetadataRawDocumentById(String idDocument, DataCategory dataCategory)
        throws AccessInternalExecutionException {

        RequestResponse<JsonNode> requestResponse;
        JsonNode jsonResponse;

        ParametersChecker.checkParameter(DATA_CATEGORY, dataCategory);
        ParametersChecker.checkParameter(ID_DOC_EMPTY, idDocument);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            switch (dataCategory) {
                case UNIT:
                    requestResponse = metaDataClient.getUnitByIdRaw(idDocument);
                    break;
                case OBJECTGROUP:
                    requestResponse = metaDataClient.getObjectGroupByIdRaw(idDocument);
                    break;
                default:
                    throw new IllegalArgumentException(UNSUPPORTED_CATEGORY + dataCategory);
            }
            if (requestResponse.isOk()) {
                jsonResponse = requestResponse.toJsonNode();
            } else {
                throw new ProcessingException("Document not found");
            }
        } catch (VitamClientException e) {
            LOGGER.error(e);
            throw new AccessInternalExecutionException(e);
        }
        return jsonResponse;
    }

    @Override
    public JsonNode selectObjectGroupById(JsonNode jsonQuery, String idObjectGroup)
        throws InvalidParseOperationException, AccessInternalExecutionException {
        // Check correctness of request
        final RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
        parser.getRequest().reset();
        if (!(parser instanceof SelectParserMultiple)) {
            throw new InvalidParseOperationException(NOT_A_SELECT_OPERATION);
        }
        return selectMetadataDocumentById(jsonQuery, idObjectGroup, DataCategory.OBJECTGROUP);
    }

    @Override
    public Response getOneObjectFromObjectGroup(String idObjectGroup, String qualifier, int version)
        throws MetaDataNotFoundException, StorageNotFoundException, AccessInternalExecutionException,
        InvalidParseOperationException {
        ParametersChecker.checkParameter("ObjectGroup id should be filled", idObjectGroup);
        ParametersChecker.checkParameter("You must specify a valid object qualifier", qualifier);
        Integer tenantId = ParameterHelper.getTenantParameter();

        ParametersChecker.checkParameter("You must specify a valid tenant", tenantId);
        ParametersChecker.checkValue("version", version, 0);


        final SelectMultiQuery request = new SelectMultiQuery();
        request.addRoots(idObjectGroup);
        // FIXME P1: we should find a better way to do that than use json, like a POJO.
        request.setProjectionSliceOnQualifier("FormatIdentification", "FileInfo");

        final JsonNode jsonResponse = selectObjectGroupById(request.getFinalSelect(), idObjectGroup);
        if (jsonResponse == null) {
            throw new AccessInternalExecutionException("Null json response node from metadata");
        }

        ObjectGroupResponse objectGroupResponse =
            JsonHandler.getFromJsonNode(jsonResponse.get(RESULTS), ObjectGroupResponse.class);


        VersionsModel finalversionsResponse = null;
        // FIXME P1: do not use direct access but POJO
        // #2604 : Filter the given result for not having false positif in the request result
        // && objectGroupResponse.getQualifiers().size() > 1
        if (objectGroupResponse.getQualifiers() != null) {
            final String dataObjectVersion = qualifier + "_" + version;
            for (QualifiersModel qualifiersResponse : objectGroupResponse.getQualifiers()) {
                // FIXME very ugly fix, qualifier with underscore should be handled
                if (qualifiersResponse.getQualifier() != null && qualifiersResponse.getQualifier().contains("_")) {
                    qualifiersResponse.setQualifier(qualifiersResponse
                        .getQualifier().split("_")[0]);
                }
                if (qualifier.equals(qualifiersResponse.getQualifier())) {
                    for (VersionsModel versionResponse : qualifiersResponse.getVersions()) {
                        if (dataObjectVersion.equals(versionResponse.getDataObjectVersion())) {
                            finalversionsResponse = versionResponse;
                            break;
                        }
                    }
                }
            }
        }
        String mimetype = null;
        String filename = null;
        String objectId = null;
        if (finalversionsResponse != null) {
            if (finalversionsResponse.getFormatIdentification() != null &&
                !finalversionsResponse.getFormatIdentification().getMimeType().isEmpty()) {
                mimetype = finalversionsResponse.getFormatIdentification().getMimeType();

            }
            if (finalversionsResponse.getFileInfoModel() != null &&
                !finalversionsResponse.getFileInfoModel().getFilename().isEmpty()) {
                filename = finalversionsResponse.getFileInfoModel().getFilename();
            }
            objectId = finalversionsResponse.getId();
        }

        if (Strings.isNullOrEmpty(mimetype)) {
            mimetype = MediaType.APPLICATION_OCTET_STREAM;
        }

        if (Strings.isNullOrEmpty(filename)) {
            filename = objectId;
        }

        final StorageClient storageClient =
            storageClientMock == null ? StorageClientFactory.getInstance().getClient() : storageClientMock;
        try {
            final Response response = storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, objectId,
                DataCategory.OBJECT);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, mimetype);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.put(GlobalDataRest.X_QUALIFIER, qualifier);
            headers.put(GlobalDataRest.X_VERSION, Integer.toString(version));
            return new VitamAsyncInputStreamResponse(response, Status.OK, headers);
        } catch (final StorageServerClientException e) {
            throw new AccessInternalExecutionException(e);
        } finally {
            if (storageClientMock == null && storageClient != null) {
                storageClient.close();
            }
        }
    }

    @Override
    public JsonNode updateUnitbyId(JsonNode queryJson, String idUnit, String requestId)
        throws MetaDataNotFoundException, IllegalArgumentException, InvalidParseOperationException,
        AccessInternalExecutionException,
        AccessInternalRuleExecutionException {
        LogbookOperationParameters logbookOpStpParamStart;
        LogbookLifeCycleUnitParameters logbookLCParamEnd;
        ParametersChecker.checkParameter(ID_CHECK_FAILED, idUnit);
        JsonNode jsonNode = JsonHandler.createObjectNode();
        boolean globalStep = true;
        boolean stepCheckRules = true;
        boolean stepDTValidation = true;
        boolean stepMetadataUpdate = true;
        boolean stepStorageUpdate = true;
        boolean stepLFCCommit = true;
        final GUID idGUID;
        final GUID idRequest;
        try {
            idGUID = GUIDReader.getGUID(idUnit);
            idRequest = GUIDReader.getGUID(requestId);
        } catch (final InvalidGuidOperationException e) {
            LOGGER.error("idUnit is not a valid GUID - " + METADATA_NOT_FOUND_ERROR, e);
            throw new MetaDataNotFoundException("idUnit is not a valid GUID - " + METADATA_NOT_FOUND_ERROR, e);
        }
        // Check Request is really an Update
        final RequestParserMultiple parser = RequestParserHelper.getParser(queryJson);
        if (!(parser instanceof UpdateParserMultiple)) {
            parser.getRequest().reset();
            throw new IllegalArgumentException("Request is not an update operation");
        }

        // eventidentifierprocess for lifecycle
        final GUID updateOpGuidStart = idRequest;
        LogbookOperationsClient logbookOperationClient = logbookOperationClientMock;
        LogbookLifeCyclesClient logbookLifeCycleClient = logbookLifeCycleClientMock;
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            if (logbookOperationClient == null) {
                logbookOperationClient = LogbookOperationsClientFactory.getInstance().getClient();
            }
            if (logbookLifeCycleClient == null) {
                logbookLifeCycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
            }

            // create logbook
            logbookOpStpParamStart = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                StatusCode.STARTED, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.STARTED), idRequest,
                STP_UPDATE_UNIT, true);
            logbookOpStpParamStart.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
            logbookOpStpParamStart.putParameterValue(LogbookParameterName.outcomeDetail, STP_UPDATE_UNIT + "." +
                StatusCode.STARTED);
            logbookOperationClient.create(logbookOpStpParamStart);
            globalStep = false;

            /** Update: Check Rules task **/
            // Call method
            stepCheckRules = false;
            checkAndUpdateRuleQuery((UpdateParserMultiple) parser);
            stepCheckRules = true;
            try {
                stepDTValidation = false;
                checkArchiveUnitProfileQuery((UpdateParserMultiple) parser, idUnit);
            } catch (AccessInternalExecutionException e) {
                LOGGER.error("Exception while getting archive unit", e);
                throw new MetaDataNotFoundException(e);
            } catch (AdminManagementClientServerException e) {
                throw new AccessInternalExecutionException("Error during checking archive unit profile", e);
            } catch (InvalidCreateOperationException e) {
                LOGGER.error(e);
                throw new AccessInternalExecutionException(ERROR_ADD_CONDITION, e);
            }
            try {
                addOntologyFieldsToBeUpdated((UpdateParserMultiple) parser);
            } catch (AdminManagementClientServerException | InvalidCreateOperationException |
                InvalidParseOperationException e) {
                LOGGER.error(e);
                throw new AccessInternalExecutionException("Error while adding ontology information", e);
            }

            try {
                queryJson = ((UpdateParserMultiple) parser).getRequest()
                    .addActions(UpdateActionHelper.push(VitamFieldsHelper.operations(), updateOpGuidStart.toString()))
                    .getFinalUpdate();
            } catch (final InvalidCreateOperationException e) {
                LOGGER.error(e);
                throw new AccessInternalExecutionException(ERROR_ADD_CONDITION, e);
            }
            stepDTValidation = true;
            /** Update: Indexation task **/
            stepMetadataUpdate = false;
            // call update
            jsonNode = metaDataClient.updateUnitbyId(queryJson, idUnit);


            // update logbook TASK INDEXATION
            stepMetadataUpdate = true;

            // update global logbook lifecycle TASK INDEXATION
            logbookLCParamEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.OK,
                idGUID, UNIT_METADATA_UPDATE);
            logbookLCParamEnd.putParameterValue(LogbookParameterName.eventDetailData,
                getDiffMessageFor(jsonNode, idUnit));

            stepLFCCommit = false;
            logbookLifeCycleClient.update(logbookLCParamEnd, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);
            stepLFCCommit = true;


            /** Update: Storage task **/
            // update stored Metadata
            stepStorageUpdate = false;
            replaceStoredUnitMetadata(idUnit, requestId);
            stepStorageUpdate = true;

            // full step OK
            globalStep = true;

            // write logbook operation at end, in case of exception it is written by rollBackLogbook
            finalizeStepOperation(logbookOperationClient, updateOpGuidStart, idRequest, idUnit, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation, null);
        } catch (final InvalidParseOperationException ipoe) {
            ObjectNode evDetData = JsonHandler.createObjectNode();
            evDetData.put(ERROR_CODE, ipoe.getMessage());
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                JsonHandler.unprettyPrint(evDetData));
            LOGGER.error(PARSING_ERROR, ipoe);
            throw ipoe;
        } catch (final IllegalArgumentException iae) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                null);
            LOGGER.error(ILLEGAL_ARGUMENT, iae);
            throw iae;
        } catch (final MetaDataNotFoundException mdnfe) {
            ObjectNode evDetData = JsonHandler.createObjectNode();
            evDetData.put(ERROR_CODE, DT_NO_EXTISTING);
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                JsonHandler.unprettyPrint(evDetData));
            LOGGER.error(METADATA_NOT_FOUND_ERROR, mdnfe);
            throw mdnfe;
        } catch (final MetaDataDocumentSizeException mddse) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                null);
            LOGGER.error(METADATA_DOCUMENT_SIZE_ERROR, mddse);
            throw new AccessInternalExecutionException(mddse);
        } catch (final LogbookClientServerException lcse) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                null);
            LOGGER.error(DOCUMENT_CLIENT_SERVER_ERROR, lcse);
            throw new AccessInternalExecutionException(lcse);
        } catch (final MetaDataExecutionException mdee) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                null);
            LOGGER.error(METADATA_EXECUTION_EXECUTION_ERROR, mdee);
            throw new AccessInternalExecutionException(mdee);
        } catch (final LogbookClientNotFoundException lcnfe) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                null);
            LOGGER.error(LOGBOOK_CLIENT_NOT_FOUND_ERROR, lcnfe);
            throw new AccessInternalExecutionException(lcnfe);
        } catch (final LogbookClientBadRequestException lcbre) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                null);
            LOGGER.error(LOGBOOK_CLIENT_BAD_REQUEST_ERROR, lcbre);
            throw new AccessInternalExecutionException(lcbre);
        } catch (final LogbookClientAlreadyExistsException e) {
            LOGGER.error(LOGBOOK_OPERATION_ALREADY_EXISTS, e);
            throw new AccessInternalExecutionException(e);
        } catch (final MetaDataClientServerException e) {
            LOGGER.error(METADATA_INTERNAL_SERVER_ERROR, e);
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                null);
            throw new AccessInternalExecutionException(e);
        } catch (StorageClientException e) {
            // NO since metadata is already updated: rollBackLogbook(logbookLifeCycleClient, logbookOperationClient,
            // updateOpGuidStart, newQuery, idGUID);
            try {
                finalizeStepOperation(logbookOperationClient, updateOpGuidStart, idRequest, idUnit, globalStep,
                    stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation, null);
            } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                LogbookClientServerException e1) {
                LOGGER.error(STORAGE_SERVER_EXCEPTION, e1);
            }
            LOGGER.error(STORAGE_SERVER_EXCEPTION, e);
            throw new AccessInternalExecutionException(STORAGE_SERVER_EXCEPTION, e);
        } catch (ContentAddressableStorageException e) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                null);
            LOGGER.error(WORKSPACE_SERVER_EXCEPTION, e);
        } catch (AccessInternalRuleExecutionException e) {
            ObjectNode evDetData = JsonHandler.createObjectNode();
            evDetData.put(ERROR_CODE, e.getMessage());
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                JsonHandler.unprettyPrint(evDetData));
            LOGGER.error(ERROR_CHECK_RULES, e);
            throw e;
        } catch (final ArchiveUnitProfileNotFoundException | ArchiveUnitProfileInactiveException |
            ArchiveUnitProfileEmptyControlSchemaException aupnfe) {
            ObjectNode evDetData = JsonHandler.createObjectNode();
            evDetData.put(ERROR_CODE, aupnfe.getMessage());
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, idRequest, idUnit,
                globalStep, stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation,
                JsonHandler.unprettyPrint(evDetData));
            LOGGER.error(ERROR_VALIDATE_DT, aupnfe);
            throw new IllegalArgumentException(aupnfe);
        } catch (AccessInternalException e) {
            LOGGER.error(ARCHIVE_UNIT_NOT_FOUND, e);
            throw new AccessInternalExecutionException(e);
        } finally {
            if (logbookLifeCycleClientMock == null && logbookLifeCycleClient != null) {
                logbookLifeCycleClient.close();
            }
            if (logbookOperationClientMock == null && logbookOperationClient != null) {
                logbookOperationClient.close();
            }
        }
        return jsonNode;
    }


    /**
     * @param idUnit
     * @param requestId
     * @throws StorageClientException
     * @throws AccessInternalException
     * @throws ContentAddressableStorageServerException
     */
    private void replaceStoredUnitMetadata(String idUnit, String requestId)
        throws ContentAddressableStorageException,
        StorageClientException, AccessInternalException {

        final WorkspaceClient workspaceClient =
            workspaceClientMock == null ? WorkspaceClientFactory.getInstance().getClient() : workspaceClientMock;
        try {
            final String fileName = idUnit + JSON;
            JsonNode unit = getUnitRawWithLfc(idUnit);
            workspaceClient.createContainer(requestId);
            InputStream inputStream = CanonicalJsonFormatter.serialize(unit);
            workspaceClient.putObject(requestId,
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + fileName,
                inputStream);
            // updates (replaces) stored object
            storeMetaDataUnit(new ObjectDescription(DataCategory.UNIT, requestId, fileName,
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + fileName));

        } finally {
            cleanWorkspace(workspaceClient, requestId);
            if (workspaceClientMock == null) {
                workspaceClient.close();
            }
        }
    }

    /**
     * getUnitWithLfc, create a jsonNode with the unit and it's lfc
     *
     * @param idUnit the unit id
     * @return a new JsonNode with unit and lfc inside
     * @throws AccessInternalException if unable to find the unit or it's lfc
     */
    private JsonNode getUnitRawWithLfc(String idUnit) throws AccessInternalException {
        // get metadata
        JsonNode jsonResponse = selectMetadataRawDocumentById(idUnit, DataCategory.UNIT);
        JsonNode unit = extractNodeFromResponse(jsonResponse, ARCHIVE_UNIT_NOT_FOUND);
        MetadataDocumentHelper.removeComputedGraphFieldsFromUnit(unit);

        // get lfc
        JsonNode lfc = getRawUnitLifeCycleById(idUnit);

        // get doc with lfc
        return MetadataStorageHelper.getUnitWithLFC(unit, lfc);
    }

    /**
     * retrieve raw unit LFC
     *
     * @param idDocument document uuid
     * @throws ProcessingException if no result found or error during parsing response from logbook client
     */
    private JsonNode getRawUnitLifeCycleById(String idDocument)
        throws AccessInternalExecutionException {
        ParametersChecker.checkParameter(ID_DOC_EMPTY, idDocument);

        try (LogbookLifeCyclesClient logbookClient = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            return logbookClient.getRawUnitLifeCycleById(idDocument);
        } catch (final InvalidParseOperationException | LogbookClientException e) {
            throw new AccessInternalExecutionException(e);
        }
    }

    /**
     * extractNodeFromResponse, check response and extract single result
     *
     * @param jsonResponse
     * @param error message to throw if response is null or no result could be found
     * @return a single result from response
     * @throws AccessInternalException if no result found
     */
    private JsonNode extractNodeFromResponse(JsonNode jsonResponse, final String error) throws AccessInternalException {

        JsonNode jsonNode;
        // check response
        if (jsonResponse == null) {
            LOGGER.error(error);
            throw new AccessInternalException(error);
        }
        jsonNode = jsonResponse.get(RESULTS);
        // if result = 0 then throw Exception
        if (jsonNode == null || jsonNode.size() == 0) {
            LOGGER.error(error);
            throw new AccessInternalException(error);
        }

        // return a single node
        return jsonNode.get(0);
    }


    /**
     * The function is used for retrieving ObjectGroup in workspace and storing metaData in storage offer
     *
     * @param description
     * @throws StorageServerClientException
     * @throws StorageNotFoundClientException
     * @throws StorageAlreadyExistsClientException
     * @throws ProcessingException                 when error in execution
     */
    private void storeMetaDataUnit(ObjectDescription description) throws StorageClientException {
        final StorageClient storageClient =
            storageClientMock == null ? StorageClientFactory.getInstance().getClient() : storageClientMock;
        try {
            // store binary data object
            storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY, description.getType(),
                description.getObjectName(),
                description);
        } finally {
            if (storageClient != null && storageClientMock == null) {
                storageClient.close();
            }
        }

    }

    private void finalizeStepOperation(LogbookOperationsClient logbookOperationClient, GUID updateOpGuidStart,
        GUID idRequest, String idUnit, boolean globalStep, boolean stepMetadataUpdate, boolean stepStorageUpdate,
        boolean stepCheckRules, boolean stepLFCCommit, boolean stepDTValidation, String evDetData)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {

        // Global step id
        GUID parentEventGuid = GUIDFactory.newEventGUID(updateOpGuidStart);

        // Tasks' events
        LogbookOperationParameters logbookOpParamEnd;
        if (!stepCheckRules) {
            // STEP UNIT_CHECK_RULES KO
            logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart), updateOpGuidStart,
                    StatusCode.KO, VitamLogbookMessages.getCodeOp(UNIT_CHECK_RULES, StatusCode.KO), idRequest,
                    UNIT_CHECK_RULES, false);
            logbookOpParamEnd.putParameterValue(LogbookParameterName.eventDetailData, evDetData);
            logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
            logbookOperationClient.update(logbookOpParamEnd);
        } else if (!stepDTValidation) {
            // STEP UNIT_CHECK_DT KO
            logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart), updateOpGuidStart,
                    StatusCode.KO, VitamLogbookMessages.getCodeOp(UNIT_CHECK_DT, StatusCode.KO), idRequest,
                    UNIT_CHECK_DT, false);
            logbookOpParamEnd.putParameterValue(LogbookParameterName.eventDetailData, evDetData);
            logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
            logbookOperationClient.update(logbookOpParamEnd);
        } else {
            // last step STEP UNIT_CHECK_RULES OK
            logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart), updateOpGuidStart,
                    StatusCode.OK, VitamLogbookMessages.getCodeOp(UNIT_CHECK_RULES, StatusCode.OK), idRequest,
                    UNIT_CHECK_RULES, false);
            logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
            logbookOperationClient.update(logbookOpParamEnd);

            // last step STEP UNIT_CHECK_DT OK
            logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart), updateOpGuidStart,
                    StatusCode.OK, VitamLogbookMessages.getCodeOp(UNIT_CHECK_DT, StatusCode.OK), idRequest,
                    UNIT_CHECK_DT, false);
            logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
            logbookOperationClient.update(logbookOpParamEnd);

            if (!stepMetadataUpdate) {
                // STEP UNIT_METADATA_UPDATE KO
                logbookOpParamEnd =
                    getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart),
                        updateOpGuidStart,
                        StatusCode.KO, VitamLogbookMessages.getCodeOp(UNIT_METADATA_UPDATE, StatusCode.KO), idRequest,
                        UNIT_METADATA_UPDATE, false);
                logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
            } else if (!stepDTValidation) {
                // STEP UNIT_CHECK_DT KO
                logbookOpParamEnd =
                    getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart),
                        updateOpGuidStart,
                        StatusCode.KO, VitamLogbookMessages.getCodeOp(UNIT_CHECK_DT, StatusCode.KO), idRequest,
                        UNIT_CHECK_DT, false);
                logbookOpParamEnd.putParameterValue(LogbookParameterName.eventDetailData, evDetData);
                logbookOperationClient.update(logbookOpParamEnd);
            } else {
                // last step UNIT_METADATA_UPDATE OK
                logbookOpParamEnd =
                    getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart),
                        updateOpGuidStart,
                        StatusCode.OK, VitamLogbookMessages.getCodeOp(UNIT_METADATA_UPDATE, StatusCode.OK), idRequest,
                        UNIT_METADATA_UPDATE, false);
                logbookOperationClient.update(logbookOpParamEnd);

                if (!stepLFCCommit) {
                    // STEP COMMIT_LIFE_CYCLE_UNIT KO
                    logbookOpParamEnd =
                        getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart),
                            updateOpGuidStart,
                            StatusCode.KO, VitamLogbookMessages.getCodeOp(COMMIT_LIFE_CYCLE_UNIT, StatusCode.KO),
                            idRequest,
                            COMMIT_LIFE_CYCLE_UNIT, false);
                    logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
                    logbookOperationClient.update(logbookOpParamEnd);
                } else {
                    // STEP UNIT_METADATA_STORAGE OK
                    logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(
                        GUIDFactory.newEventGUID(updateOpGuidStart), updateOpGuidStart,
                        StatusCode.OK, VitamLogbookMessages.getCodeOp(COMMIT_LIFE_CYCLE_UNIT, StatusCode.OK),
                        idRequest,
                        COMMIT_LIFE_CYCLE_UNIT, false);
                    logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
                    logbookOperationClient.update(logbookOpParamEnd);

                    if (!stepStorageUpdate) {
                        // STEP UNIT_METADATA_STORAGE KO
                        logbookOpParamEnd =
                            getLogbookOperationUpdateUnitParameters(GUIDFactory.newEventGUID(updateOpGuidStart),
                                updateOpGuidStart,
                                StatusCode.KO, VitamLogbookMessages.getCodeOp(UNIT_METADATA_STORAGE, StatusCode.KO),
                                idRequest,
                                UNIT_METADATA_STORAGE, false);
                        logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
                        logbookOperationClient.update(logbookOpParamEnd);
                    } else {
                        // STEP UNIT_METADATA_STORAGE OK
                        logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(
                            GUIDFactory.newEventGUID(updateOpGuidStart), updateOpGuidStart,
                            StatusCode.OK, VitamLogbookMessages.getCodeOp(UNIT_METADATA_STORAGE, StatusCode.OK),
                            idRequest,
                            UNIT_METADATA_STORAGE, false);
                        logbookOpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
                        logbookOperationClient.update(logbookOpParamEnd);

                    }
                }
            }
        }

        // Global step event
        LogbookOperationParameters logbookOpStpParamEnd;
        if (!globalStep) {
            // GLOBAL KO
            logbookOpStpParamEnd =
                getLogbookOperationUpdateUnitParameters(parentEventGuid, updateOpGuidStart,
                    StatusCode.KO, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.KO), idRequest,
                    STP_UPDATE_UNIT, false);

            logbookOpStpParamEnd.putParameterValue(LogbookParameterName.outcomeDetail, STP_UPDATE_UNIT + "." +
                StatusCode.KO);
        } else {
            // GLOBAL OK
            logbookOpStpParamEnd =
                getLogbookOperationUpdateUnitParameters(parentEventGuid, updateOpGuidStart,
                    StatusCode.OK, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.OK), idRequest,
                    STP_UPDATE_UNIT, false);
            logbookOpStpParamEnd.putParameterValue(LogbookParameterName.outcomeDetail, STP_UPDATE_UNIT + "." +
                StatusCode.OK);
        }
        logbookOpStpParamEnd.putParameterValue(LogbookParameterName.objectIdentifier, idUnit);
        logbookOperationClient.update(logbookOpStpParamEnd);
    }

    private void rollBackLogbook(LogbookOperationsClient logbookOperationClient,
        LogbookLifeCyclesClient logbookLifeCycleClient, GUID updateOpGuidStart, GUID idRequest, String idUnit,
        boolean globalStep, boolean stepMetadataUpdate, boolean stepStorageUpdate, boolean stepCheckRules,
        boolean stepLFCCommit, boolean stepDTValidation, String evDetData) {
        try {
            finalizeStepOperation(logbookOperationClient, updateOpGuidStart, idRequest, idUnit, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, stepLFCCommit, stepDTValidation, evDetData);
            // That means lifecycle could be found, so it could be roolbacked
            if (stepMetadataUpdate) {
                logbookLifeCycleClient.rollBackUnitsByOperation(updateOpGuidStart.toString());
            }

            if (!stepStorageUpdate) {
                LOGGER.error(String.format(
                    "[Consistency Error] : The Archive Unit with guid =%s is not saved in storage, tenant : %s, requestId : %s",
                    idUnit,
                    ParameterHelper.getTenantParameter(), idRequest.toString()));
            }
        } catch (final LogbookClientBadRequestException lcbre) {
            LOGGER.error(BAD_REQUEST, lcbre);
        } catch (final LogbookClientNotFoundException lcbre) {
            LOGGER.error(CLIENT_NOT_FOUND, lcbre);
        } catch (final LogbookClientServerException lcse) {
            LOGGER.error(CLIENT_SERVER_ERROR, lcse);
        }
    }

    private LogbookLifeCycleUnitParameters getLogbookLifeCycleUpdateUnitParameters(GUID eventIdentifierProcess,
        StatusCode logbookOutcome, GUID objectIdentifier, String action) {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());

        LogbookLifeCycleUnitParameters logbookLifeCycleUnitParameters =
            LogbookParametersFactory.newLogbookLifeCycleUnitParameters(updateGuid,
                VitamLogbookMessages.getEventTypeLfc(action),
                eventIdentifierProcess,
                eventTypeProcess, logbookOutcome,
                VitamLogbookMessages.getOutcomeDetailLfc(action, logbookOutcome),
                VitamLogbookMessages.getCodeLfc(action, logbookOutcome), objectIdentifier);
        return logbookLifeCycleUnitParameters;
    }

    private LogbookOperationParameters getLogbookOperationUpdateUnitParameters(GUID eventIdentifier,
        GUID eventIdentifierProcess, StatusCode logbookOutcome,
        String outcomeDetailMessage, GUID eventIdentifierRequest, String action, boolean addContract) {
        final LogbookOperationParameters parameters =
            LogbookParametersFactory.newLogbookOperationParameters(eventIdentifier,
                action, eventIdentifierProcess, LogbookTypeProcess.UPDATE, logbookOutcome,
                outcomeDetailMessage,
                eventIdentifierRequest);
        if (addContract) {
            ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
            rightsStatementIdentifier
                .put(ACCESS_CONTRACT, VitamThreadUtils.getVitamSession().getContract().getIdentifier());
            parameters.putParameterValue(LogbookParameterName.rightsStatementIdentifier,
                rightsStatementIdentifier.toString());
        }
        return parameters;
    }

    private String getDiffMessageFor(JsonNode diff, String unitId) throws InvalidParseOperationException {
        if (diff == null) {
            return "";
        }
        final JsonNode arrayNode = diff.has(DOLLAR_DIFF) ? diff.get(DOLLAR_DIFF) : diff.get(RESULTS);
        if (arrayNode == null) {
            return "";
        }
        for (final JsonNode diffNode : arrayNode) {
            if (diffNode.get(ID) != null && unitId.equals(diffNode.get(ID).textValue())) {
                ObjectNode diffObject = JsonHandler.createObjectNode();
                diffObject.set("diff", diffNode.get(DIFF));
                return JsonHandler.writeAsString(diffObject);
            }
        }
        // TODO P1 : empty string or error because no diff for this id ?
        return "";
    }

    private void cleanWorkspace(final WorkspaceClient workspaceClient, final String containerName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        // call workspace
        if (workspaceClient.isExistingContainer(containerName)) {
            workspaceClient.deleteContainer(containerName, true);
        }
    }

    public void checkAndUpdateRuleQuery(UpdateParserMultiple updateParser)
        throws AccessInternalRuleExecutionException, AccessInternalExecutionException {
        UpdateMultiQuery request = updateParser.getRequest();
        List<String> deletedCategoryRules = new LinkedList<>();
        Map<String, JsonNode> updatedCategoryRules = new HashMap<>();

        checkActionsOnRules(request, deletedCategoryRules, updatedCategoryRules);
        if (deletedCategoryRules.isEmpty() && updatedCategoryRules.isEmpty()) {
            return;
        }

        String unitId = updateParser.getRequest().getRoots().toArray(new String[1])[0];
        JsonNode management = getUnitManagement(unitId);

        Set<String> updatedCategories = updatedCategoryRules.keySet();
        for (String category : VitamConstants.getSupportedRules()) {
            ArrayNode rulesForCategory = null;
            JsonNode categoryNode = management.get(category);
            JsonNode fullupdatedInheritanceNode;
            JsonNode updatedPreventInheritanceNode;
            JsonNode updatedPreventRulesNode;
            if (categoryNode != null) {
                rulesForCategory = (ArrayNode) categoryNode.get(RULES_KEY);
            }

            if (deletedCategoryRules.contains(category)) {
                checkDeletedCategories(categoryNode, category);
            }

            if (rulesForCategory != null && rulesForCategory.size() != 0) {
                if (updatedCategories.contains(category)) {
                    ArrayNode rulesForUpdatedCategory = (ArrayNode) updatedCategoryRules.get(category).get(RULES_KEY);
                    JsonNode finalAction = updatedCategoryRules.get(category).get(FINAL_ACTION_KEY);
                    JsonNode existingFinalAction = categoryNode.get(FINAL_ACTION_KEY);
                    JsonNode classificationLevel =
                        updatedCategoryRules.get(category).get(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL);
                    JsonNode classificationOwner =
                        updatedCategoryRules.get(category).get(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER);
                    fullupdatedInheritanceNode = updatedCategoryRules.get(category).get(INHERITANCE_KEY);
                    updatedPreventInheritanceNode =
                        updatedCategoryRules.get(category).get(INHERITANCE_KEY + "." + PREVENT_INHERITANCE_KEY) != null
                            ? updatedCategoryRules.get(category).get(INHERITANCE_KEY + "." + PREVENT_INHERITANCE_KEY)
                            : updatedCategoryRules.get(category).get(PREVENT_INHERITANCE_KEY);

                    updatedPreventRulesNode =
                        updatedCategoryRules.get(category).get(INHERITANCE_KEY + "." + PREVENT_RULES_ID_KEY) != null
                            ? updatedCategoryRules.get(category).get(INHERITANCE_KEY + "." + PREVENT_RULES_ID_KEY)
                            : updatedCategoryRules.get(category).get(PREVENT_RULES_ID_KEY);

                    boolean deleteAllRules = false;
                    ArrayNode updatedRules;
                    ArrayNode createdRules;

                    if (rulesForUpdatedCategory == null || rulesForUpdatedCategory.size() == 0) {
                        deleteAllRules = true;
                        updatedRules = JsonHandler.createArrayNode();
                    } else {
                        updatedRules =
                            checkUpdatedRules(category, rulesForCategory, rulesForUpdatedCategory, finalAction);
                        createdRules =
                            checkAddedRules(category, rulesForCategory, rulesForUpdatedCategory, finalAction);

                        if (createdRules.size() != 0) {
                            updatedRules.addAll(createdRules);
                        }
                    }

                    boolean hasNewFinalAction = hasNewFinalAction(existingFinalAction, finalAction);

                    boolean hasNewRules = updatedRules.size() != 0
                        || fullupdatedInheritanceNode != null
                        || updatedPreventInheritanceNode != null
                        || updatedPreventRulesNode != null;

                    if (!hasNewRules && !hasNewFinalAction && !deleteAllRules) {
                        return;
                    }

                    Map<String, JsonNode> action = new HashMap<>();
                    if (updatedRules.size() != 0) {
                        action.put(MANAGEMENT_PREFIX + category + RULES_PREFIX, updatedRules);
                    } else if (deleteAllRules) {
                        action.put(MANAGEMENT_PREFIX + category + RULES_PREFIX, JsonHandler.createArrayNode());
                    }
                    if (hasNewFinalAction) {
                        action.put(MANAGEMENT_PREFIX + category + FINAL_ACTION_PREFIX, finalAction);
                    }
                    if (fullupdatedInheritanceNode != null) {
                        action.put(MANAGEMENT_PREFIX + category + "." + INHERITANCE_KEY,
                            fullupdatedInheritanceNode);
                    }
                    if (updatedPreventInheritanceNode != null) {
                        action.put(
                            MANAGEMENT_PREFIX + category + "." + INHERITANCE_KEY + "." + PREVENT_INHERITANCE_KEY,
                            updatedPreventInheritanceNode);
                    }
                    if (updatedPreventRulesNode != null) {
                        action.put(
                            MANAGEMENT_PREFIX + category + "." + INHERITANCE_KEY + "." + PREVENT_RULES_ID_KEY,
                            updatedPreventRulesNode);
                    }
                    if (classificationLevel != null) {
                        action.put(MANAGEMENT_PREFIX + category + "." + SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL,
                            classificationLevel);
                    }
                    if (classificationOwner != null) {
                        action.put(MANAGEMENT_PREFIX + category + "." + SedaConstants.TAG_RULE_CLASSIFICATION_OWNER,
                            classificationOwner);
                    }

                    try {
                        if (action.size() > 0) {
                            request.addActions(new SetAction(action));
                        }
                    } catch (InvalidCreateOperationException e) {
                        throw new AccessInternalExecutionException(ERROR_ADD_CONDITION, e);
                    }
                }
            } else if (updatedCategories.contains(category)) {
                // Check for new rules (rules only present in request)
                ArrayNode rulesForUpdatedCategory = (ArrayNode) updatedCategoryRules.get(category).get(RULES_KEY);
                JsonNode finalAction = updatedCategoryRules.get(category).get(FINAL_ACTION_KEY);
                JsonNode classificationLevel =
                    updatedCategoryRules.get(category).get(SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL);
                JsonNode classificationOwner =
                    updatedCategoryRules.get(category).get(SedaConstants.TAG_RULE_CLASSIFICATION_OWNER);
                fullupdatedInheritanceNode = updatedCategoryRules.get(category).get(INHERITANCE_KEY);

                updatedPreventInheritanceNode =
                    updatedCategoryRules.get(category).get(INHERITANCE_KEY + "." + PREVENT_INHERITANCE_KEY) != null
                        ? updatedCategoryRules.get(category).get(INHERITANCE_KEY + "." + PREVENT_INHERITANCE_KEY)
                        : updatedCategoryRules.get(category).get(PREVENT_INHERITANCE_KEY);

                updatedPreventRulesNode =
                    updatedCategoryRules.get(category).get(INHERITANCE_KEY + "." + PREVENT_RULES_ID_KEY) != null
                        ? updatedCategoryRules.get(category).get(INHERITANCE_KEY + "." + PREVENT_RULES_ID_KEY)
                        : updatedCategoryRules.get(category).get(PREVENT_RULES_ID_KEY);

                ArrayNode createdRules =
                    checkAddedRules(category, null, rulesForUpdatedCategory, finalAction);

                Map<String, JsonNode> action = new HashMap<>();
                if (createdRules.size() != 0) {
                    action.put(MANAGEMENT_PREFIX + category + RULES_PREFIX, createdRules);
                }

                if (finalAction != null) {
                    action.put(MANAGEMENT_PREFIX + category + FINAL_ACTION_PREFIX, finalAction);
                }

                if (fullupdatedInheritanceNode != null) {
                    action.put(MANAGEMENT_PREFIX + category + "." + INHERITANCE_KEY,
                        fullupdatedInheritanceNode);
                }
                if (updatedPreventInheritanceNode != null) {
                    action.put(MANAGEMENT_PREFIX + category + "." + INHERITANCE_KEY + "." + PREVENT_INHERITANCE_KEY,
                        updatedPreventInheritanceNode);
                }
                if (updatedPreventRulesNode != null) {
                    action.put(MANAGEMENT_PREFIX + category + "." + INHERITANCE_KEY + "." + PREVENT_RULES_ID_KEY,
                        updatedPreventRulesNode);
                }
                if (classificationLevel != null) {
                    action.put(MANAGEMENT_PREFIX + category + "." + SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL,
                        classificationLevel);
                }
                if (classificationOwner != null) {
                    action.put(MANAGEMENT_PREFIX + category + "." + SedaConstants.TAG_RULE_CLASSIFICATION_OWNER,
                        classificationOwner);
                }
                try {
                    request.addActions(new SetAction(action));
                } catch (InvalidCreateOperationException e) {
                    throw new AccessInternalExecutionException(ERROR_ADD_CONDITION, e);
                }
            }
        }
    }

    private boolean hasNewFinalAction(JsonNode existingFinalAction, JsonNode finalAction) {
        if (existingFinalAction == null && finalAction != null) {
            return true;
        }
        if (existingFinalAction != null && finalAction == null) {
            return false;
        }
        if (existingFinalAction == null && finalAction == null) {
            return false;
        }
        return !existingFinalAction.textValue().equalsIgnoreCase(finalAction.textValue());
    }

    /**
     * Check if there is update actions on rules. If not no updates/checks on the query. SetActions on rules are removed
     * for the request because they will be computed for endDate and reinserted later
     *
     * @param request The initial request
     * @param deletedCategoryRules The returned list of deleted Rules (Must be initialized)
     * @param updatedCategoryRules The returned list of updated Rules (Must be initialized)
     */
    private static void checkActionsOnRules(UpdateMultiQuery request, List<String> deletedCategoryRules,
        Map<String, JsonNode> updatedCategoryRules) {
        Iterator<Action> actionsIterator = request.getActions().iterator();
        while (actionsIterator.hasNext()) {
            Action action = actionsIterator.next();
            UPDATEACTION currentAction = action.getUPDATEACTION();
            if (UPDATEACTION.SET.equals(currentAction) || UPDATEACTION.UNSET.equals(currentAction)) {
                JsonNode object = action.getCurrentObject();

                if (UPDATEACTION.UNSET.equals(currentAction)) {
                    // Delete a field
                    ArrayNode values = (ArrayNode) object;
                    for (int i = 0, len = values.size(); i < len; i++) {
                        String unsetField = values.get(i).asText();
                        if (unsetField.startsWith(MANAGEMENT_PREFIX) && VitamConstants.getSupportedRules()
                            .contains(unsetField.substring(MANAGEMENT_PREFIX.length()))) {
                            // Delete a ruleCategory
                            deletedCategoryRules.add(unsetField.substring(MANAGEMENT_PREFIX.length()));
                        }
                    }
                } else {
                    // Set a field
                    Iterator<String> fields = object.fieldNames();
                    while (fields.hasNext()) {
                        ObjectNode objectToPut = null;
                        String field = fields.next();
                        if (field.startsWith(MANAGEMENT_PREFIX)) {
                            // 2 possibilities are now handled :
                            // - {"#management.AccessRule.Rules":[...]}
                            // - {"#management.AccessRule":{"Rules":[...]}}
                            String ruleToBeChecked = field.substring(MANAGEMENT_PREFIX.length());
                            if (ruleToBeChecked.contains(".")) {
                                String[] params = ruleToBeChecked.split("\\.");
                                String mainAtt = params[0];
                                String subAtt = params[params.length - 1];
                                objectToPut = JsonHandler.createObjectNode();
                                objectToPut.set(subAtt, object.get(field));
                                ruleToBeChecked = mainAtt;
                            }
                            if (VitamConstants.getSupportedRules().contains(ruleToBeChecked)) {
                                // Set a ruleCategory
                                updatedCategoryRules.put(ruleToBeChecked,
                                    objectToPut != null ? objectToPut : object.get(field));
                                actionsIterator.remove();
                            }
                        }
                    }
                }

            }
        }
    }

    private JsonNode getUnitArchiveUnitProfile(String unitId) throws AccessInternalExecutionException {
        JsonNode jsonUnit = null;
        try {
            Select selectAUPforUnit = new Select();
            selectAUPforUnit.setProjection(JsonHandler.getFromString("{\"$fields\": { \"ArchiveUnitProfile\": 1}}"));
            JsonNode response = selectUnitbyId(selectAUPforUnit.getFinalSelect(), unitId);
            if (response == null || response.get(RESULTS) == null) {
                throw new AccessInternalExecutionException("Can't get unit by ID: " + unitId);
            }
            JsonNode results = response.get(RESULTS);
            if (results.size() != 1) {
                throw new AccessInternalExecutionException("Can't get unique unit by ID: " + unitId);
            }
            jsonUnit = results.get(0);
        } catch (AccessInternalExecutionException | IllegalArgumentException | InvalidParseOperationException e) {
            throw new AccessInternalExecutionException(e);
        }

        return jsonUnit.get(SedaConstants.TAG_ARCHIVE_UNIT_PROFILE);
    }

    private JsonNode getUnitManagement(String unitId) throws AccessInternalExecutionException {
        JsonNode jsonUnit = null;
        try {
            // TODO Do it cleaner
            String emptyQuery = "{\"$queries\": [],\"$filter\": { },\"$projection\": {}}";
            JsonNode response = selectUnitbyId(JsonHandler.getFromString(emptyQuery), unitId);
            if (response == null || response.get(RESULTS) == null) {
                throw new AccessInternalExecutionException("Can't get unit by ID: " + unitId);
            }
            JsonNode results = response.get(RESULTS);
            if (results.size() != 1) {
                throw new AccessInternalExecutionException("Can't get unique unit by ID: " + unitId);
            }
            jsonUnit = results.get(0);
        } catch (AccessInternalExecutionException | IllegalArgumentException | InvalidParseOperationException e) {
            throw new AccessInternalExecutionException(e);
        }

        return jsonUnit.get(MANAGEMENT_KEY);
    }

    private void checkDeletedCategories(JsonNode categoryNode, String category)
        throws AccessInternalRuleExecutionException {
        if (checkInheritancePrevention(categoryNode)) {
            LOGGER.error(ERROR_DELETE_RULE + category + ERROR_PREVENT_INHERITANCE);
            throw new AccessInternalRuleExecutionException(
                VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_DELETE_CATEGORY_INHERITANCE.name());
        }
    }

    private ArrayNode checkUpdatedRules(String category, ArrayNode rulesForCategory, ArrayNode rulesForUpdatedCategory,
        JsonNode finalAction)
        throws AccessInternalRuleExecutionException, AccessInternalExecutionException {
        // Check for all rules in rulesForCategory and compare with rules in rulesForUpdatedCategory
        ArrayNode updatedRules = JsonHandler.createArrayNode();
        if (rulesForCategory != null && rulesForUpdatedCategory != null) {
            for (JsonNode unitRule : rulesForCategory) {
                boolean findIt = false;
                Iterator<JsonNode> updateRulesIterator = rulesForUpdatedCategory.iterator();
                // Try to find a matching rule in order to check update
                while (!findIt && updateRulesIterator.hasNext()) {
                    JsonNode updateRule = updateRulesIterator.next();
                    String updateRuleName = updateRule.get("Rule").asText();
                    if (unitRule.get("Rule") != null && unitRule.get("Rule").asText().equals(updateRuleName)) {
                        findIt = true;

                        if (checkEndDateInRule(updateRule)) {
                            LOGGER.error(ERROR_UPDATE_RULE + updateRule + " contains an endDate");
                            throw new AccessInternalRuleExecutionException(
                                VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_END_DATE.name());
                        }
                        if (!checkRuleFinalAction(category, finalAction)) {
                            LOGGER.error(ERROR_UPDATE_RULE + updateRule + "(FinalAction: " + finalAction +
                                ") contains wrong FinalAction");
                            throw new AccessInternalRuleExecutionException(
                                VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_FINAL_ACTION.name());
                        }
                        JsonNode ruleInReferential = checkExistingRule(updateRuleName);
                        if (ruleInReferential == null) {
                            LOGGER.error(ERROR_UPDATE_RULE + updateRule.get("Rule") + " is not in referential");
                            throw new AccessInternalRuleExecutionException(
                                VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_EXIST.name());
                        }
                        if (!category.equals(ruleInReferential.get(RULE_TYPE).asText())) {
                            LOGGER.error(ERROR_UPDATE_RULE + updateRule.get("Rule") + " is not a " + category);
                            throw new AccessInternalRuleExecutionException(
                                VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_CATEGORY.name());
                        }

                        try {
                            updateRule = computeEndDate((ObjectNode) updateRule, ruleInReferential);
                        } catch (AccessInternalRuleExecutionException e) {
                            throw new AccessInternalRuleExecutionException(
                                VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_START_DATE.name());
                        }
                        updatedRules.add(updateRule);
                    }
                }
            }
        }

        return updatedRules;
    }

    private ArrayNode checkAddedRules(String category, ArrayNode rulesForCategory, ArrayNode rulesForUpdatedCategory,
        JsonNode finalAction)
        throws AccessInternalRuleExecutionException, AccessInternalExecutionException {
        ArrayNode createdRules = JsonHandler.createArrayNode();
        if (rulesForUpdatedCategory != null) {
            for (JsonNode updateRule : rulesForUpdatedCategory) {
                if (updateRule.get("Rule") == null) {
                    throw new AccessInternalRuleExecutionException(
                        VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CHECK_RULES.name());
                }
                boolean findIt = false;
                if (rulesForCategory != null) {
                    for (JsonNode unitRule : rulesForCategory) {
                        if (unitRule.get("Rule") != null &&
                            unitRule.get("Rule").asText().equals(updateRule.get("Rule").asText())) {
                            // Stop loop over unitRule
                            findIt = true;
                        }
                    }
                }
                if (!findIt) {
                    // Created Rule
                    if (checkEndDateInRule(updateRule)) {
                        LOGGER.error(ERROR_CREATE_RULE + updateRule + " contains an endDate");
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_END_DATE.name());
                    }
                    if (!checkRuleFinalAction(category, finalAction)) {
                        LOGGER.error(ERROR_CREATE_RULE + updateRule + "(FinalAction: " + finalAction +
                            ") contains wrong FinalAction");
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_FINAL_ACTION.name());
                    }
                    JsonNode ruleInReferential = checkExistingRule(updateRule.get("Rule").asText());
                    if (ruleInReferential == null) {
                        LOGGER.error(ERROR_CREATE_RULE + updateRule.get("Rule") + " is not in referential");
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_EXIST.name());
                    }
                    if (!category.equals(ruleInReferential.get(RULE_TYPE).asText())) {
                        LOGGER.error(ERROR_CREATE_RULE + updateRule.get("Rule") + " is not a " + category);
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_CATEGORY.name());
                    }

                    try {
                        updateRule = computeEndDate((ObjectNode) updateRule, ruleInReferential);
                    } catch (AccessInternalRuleExecutionException e) {
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_START_DATE.name());
                    }
                    createdRules.add(updateRule);
                }
            }
        }

        return createdRules;
    }

    private boolean checkInheritancePrevention(JsonNode categoryNode) {
        JsonNode inheritance = categoryNode.get(INHERITANCE_KEY);
        return inheritance != null &&
            (inheritance.get(PREVENT_RULES_ID_KEY) != null || inheritance.get(PREVENT_INHERITANCE_KEY) != null);
    }

    private boolean checkEndDateInRule(JsonNode rule) {
        return rule.get("EndDate") != null;
    }

    private boolean checkRuleFinalAction(String category, JsonNode finalActionNode) {

        if (VitamConstants.TAG_RULE_APPRAISAL.equals(category)) {
            if (finalActionNode == null) {
                return false;
            }
            try {
                AppraisalRuleFinalAction.fromValue(finalActionNode.asText());
            } catch (IllegalArgumentException e) {
                LOGGER.info("While update rules, No Appraisal FinalAction match " + finalActionNode + e);
                return false;
            }
        } else if (VitamConstants.TAG_RULE_STORAGE.equals(category)) {
            if (finalActionNode == null) {
                return false;
            }
            try {
                StorageRuleFinalAction.fromValue(finalActionNode.asText());
            } catch (IllegalArgumentException e) {
                LOGGER.info("While update rules, No Storage FinalAction match " + finalActionNode + e);
                return false;
            }
        } else if (finalActionNode != null) {
            return false;
        }
        return true;
    }

    private JsonNode checkExistingRule(String ruleId) throws AccessInternalExecutionException {
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        JsonNode response = null;
        try {
            response = client.getRuleByID(ruleId);
        } catch (FileRulesException e) {
            LOGGER.info("While update rules, No rules find for id " + ruleId + e);
            return null;
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            throw new AccessInternalExecutionException("Error during checking existing rules", e);
        }
        return response.get(RESULTS).get(0);
    }



    private JsonNode computeEndDate(ObjectNode updatingRule, JsonNode ruleInReferential)
        throws AccessInternalRuleExecutionException {
        LocalDate endDate = null;

        // FIXME Start of duplicated method, need to add it in a common module
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String startDateString = updatingRule.get("StartDate") != null ? updatingRule.get("StartDate").asText() : null;
        String ruleId = updatingRule.get("Rule").asText();
        String currentRuleType = ruleInReferential.get(RULE_TYPE).asText();
        if (ParametersChecker.isNotEmpty(startDateString) && ParametersChecker.isNotEmpty(ruleId, currentRuleType)) {
            ParametersChecker.checkDateParam("wrong date format", startDateString);
            LocalDate startDate = LocalDate.parse(startDateString, timeFormatter);
            if (startDate.getYear() >= 9000) {
                throw new AccessInternalRuleExecutionException("Wrong Start Date");
            }

            final String duration = ruleInReferential.get(FileRules.RULEDURATION).asText();
            final String measurement = ruleInReferential.get(FileRules.RULEMEASUREMENT).asText();
            if (!"unlimited".equalsIgnoreCase(duration)) {
                final RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromType(measurement);
                endDate = startDate.plus(Integer.parseInt(duration), ruleMeasurement.getTemporalUnit());
            }
        }
        // End of duplicated method
        if (endDate != null) {
            updatingRule.put("EndDate", endDate.format(timeFormatter));
        }

        return updatingRule;
    }

    @Override
    public Response findDIPByOperationId(String id) throws AccessInternalExecutionException {
        final StorageClient storageClient =
            storageClientMock == null ? StorageClientFactory.getInstance().getClient() : storageClientMock;
        try {
            final Response response = storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, id,
                DataCategory.DIP);
            return new VitamAsyncInputStreamResponse(response, Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageServerClientException | StorageNotFoundException e) {
            throw new AccessInternalExecutionException(e);
        } finally {
            if (storageClientMock == null && storageClient != null) {
                storageClient.close();
            }
        }
    }

    private void checkArchiveUnitProfileQuery(UpdateParserMultiple updateParser, String idUnit)
        throws ArchiveUnitProfileNotFoundException, ArchiveUnitProfileInactiveException,
        InvalidCreateOperationException, InvalidParseOperationException,
        AdminManagementClientServerException, AccessInternalExecutionException,
        ArchiveUnitProfileEmptyControlSchemaException {
        boolean updateAupValue = false;
        String originalAupIdentifier = null;
        // first get aup information for the unit
        JsonNode aupInfo = getUnitArchiveUnitProfile(idUnit);
        if (aupInfo != null) {
            originalAupIdentifier = aupInfo.isArray() ? aupInfo.get(0).asText()
                : aupInfo.asText();
        }

        UpdateMultiQuery request = updateParser.getRequest();
        List<Action> actions = new ArrayList<Action>(request.getActions());
        Iterator<Action> iterator = actions.iterator();
        while (iterator.hasNext()) {
            Action action = iterator.next();
            JsonNode object = action.getCurrentObject();
            Iterator<String> fields = object.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (SedaConstants.TAG_ARCHIVE_UNIT_PROFILE.equals(field)) {
                    updateAupValue = true;
                    if (object.get(field) != null) {
                        String archiveUnitProfileIdentifier =
                            object.get(field).isArray() ? object.get(field).get(0).asText()
                                : object.get(field).asText();
                        object.get(field);
                        if (archiveUnitProfileIdentifier != null && !archiveUnitProfileIdentifier.isEmpty()) {
                            addActionAUProfileSchema(archiveUnitProfileIdentifier, request);
                        }
                    }
                }
            }
        }

        if (!updateAupValue && originalAupIdentifier != null && !originalAupIdentifier.isEmpty()) {
            addActionAUProfileSchema(originalAupIdentifier, request);
        }

    }

    private static void addActionAUProfileSchema(String archiveUnitProfileIdentifier,
        UpdateMultiQuery request)
        throws ArchiveUnitProfileNotFoundException, ArchiveUnitProfileInactiveException,
        InvalidCreateOperationException, InvalidParseOperationException,
        AdminManagementClientServerException, ArchiveUnitProfileEmptyControlSchemaException {
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(ArchiveUnitProfile.IDENTIFIER, archiveUnitProfileIdentifier));
            RequestResponse<ArchiveUnitProfileModel> response =
                adminClient.findArchiveUnitProfiles(select.getFinalSelect());
            ArchiveUnitProfileModel archiveUnitProfile = null;
            if (response.isOk() && ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().size() > 0) {
                archiveUnitProfile = ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().get(0);
                if (ArchiveUnitProfileStatus.ACTIVE.equals(archiveUnitProfile.getStatus())) {
                    if (controlSchemaIsEmpty(archiveUnitProfile)) {
                        throw new ArchiveUnitProfileEmptyControlSchemaException(
                            "Archive unit profile does not have a controlSchema");
                    } else {
                        Action action =
                            new SetAction(SchemaValidationUtils.TAG_SCHEMA_VALIDATION,
                                archiveUnitProfile.getControlSchema());
                        request.addActions(action);
                    }
                } else {
                    throw new ArchiveUnitProfileInactiveException("Archive unit profile is inactive");
                }
            } else {
                throw new ArchiveUnitProfileNotFoundException(DT_NO_EXTISTING);
            }
        } catch (AdminManagementClientServerException | InvalidParseOperationException |
            InvalidCreateOperationException e) {
            // AUP select could not be executed
            LOGGER.error(e);
            throw e;
        }
    }

    private static boolean controlSchemaIsEmpty(ArchiveUnitProfileModel archiveUnitProfile) {
        try {
            return archiveUnitProfile.getControlSchema() == null ||
                JsonHandler.isEmpty(archiveUnitProfile.getControlSchema());
        } catch (InvalidParseOperationException e) {
            return false;
        }
    }

    private static void addOntologyFieldsToBeUpdated(UpdateParserMultiple updateParser)
        throws InvalidCreateOperationException, AdminManagementClientServerException,
        InvalidParseOperationException {
        UpdateMultiQuery request = updateParser.getRequest();
        Select selectOntologies = new Select();
        List<OntologyModel> ontologyModelList = new ArrayList<OntologyModel>();
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            selectOntologies.setQuery(
                QueryHelper.and()
                    .add(QueryHelper.in(OntologyModel.TAG_TYPE, OntologyType.DOUBLE.getType(),
                        OntologyType.BOOLEAN.getType(),
                        OntologyType.DATE.getType(),
                        OntologyType.LONG.getType()))
                    .add(QueryHelper.in(OntologyModel.TAG_COLLECTIONS, MetadataType.UNIT.getName())));
            selectOntologies
                .setProjection(JsonHandler.getFromString("{\"$fields\": { \"Identifier\": 1, \"Type\": 1}}"));
            RequestResponse<OntologyModel> responseOntologies =
                adminClient.findOntologies(selectOntologies.getFinalSelect());
            if (responseOntologies.isOk() &&
                ((RequestResponseOK<OntologyModel>) responseOntologies).getResults().size() > 0) {
                ontologyModelList =
                    ((RequestResponseOK<OntologyModel>) responseOntologies).getResults();
            } else {
                // no external ontology, nothing to do
                return;
            }
            if (ontologyModelList.size() > 0) {
                ArrayNode ontologyArrayNode = JsonHandler.createArrayNode();
                ontologyModelList.forEach(ontology -> {
                    try {
                        ontologyArrayNode.add(JsonHandler.toJsonNode(ontology));
                    } catch (InvalidParseOperationException e) {
                        LOGGER.error("could not parse this ontology", e);
                    }
                });
                Action action =
                    new SetAction(SchemaValidationUtils.TAG_ONTOLOGY_FIELDS,
                        JsonHandler.unprettyPrint(ontologyArrayNode));
                request.addActions(action);
            }
        } catch (InvalidCreateOperationException | AdminManagementClientServerException |
            InvalidParseOperationException e) {
            throw e;
        }
    }

    public void checkClassificationLevel(JsonNode query)
        throws InvalidParseOperationException {

        String classificationFieldName = VitamFieldsHelper.management() + "." + VitamConstants.TAG_RULE_CLASSIFICATION + "." +
            SedaConstants.TAG_RULE_CLASSIFICATION_LEVEL;
        JsonNode classificationLevel = getValueForUpdateDsl(query, classificationFieldName);

        String classificationLevelValue;
        if (classificationLevel == null) {
            return;
        }

        if(!classificationLevel.isTextual()) {
            throw new IllegalArgumentException("Illegal value for Classification Level. Expected string");
        }
        classificationLevelValue = classificationLevel.asText();

        if (!ClassificationLevelUtil.checkClassificationLevel(classificationLevelValue)) {
            throw new IllegalArgumentException("Classification Level is not in the list of allowed values");
        }
    }

}
