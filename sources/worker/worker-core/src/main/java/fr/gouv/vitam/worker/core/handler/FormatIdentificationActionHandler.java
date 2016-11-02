/**
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
 */
package fr.gouv.vitam.worker.core.handler;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gc.iotools.stream.is.InputStreamFromOutputStream;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * FormatIdentification Handler.<br>
 *
 */

// TODO P1: refactor me
// TODO P0: review Logbook messages (operation / lifecycle)
// TODO P0: fully use VitamCode

public class FormatIdentificationActionHandler extends ActionHandler implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FormatIdentificationActionHandler.class);

    private static final String HANDLER_ID = "OG_OBJECTS_FORMAT_CHECK";
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";

    // TODO P0 should not be a private attribute -> to refactor
    private final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters = LogbookParametersFactory
        .newLogbookLifeCycleObjectGroupParameters();

    private FormatIdentifier formatIdentifier;
    private final LogbookLifeCyclesClient logbookClient =
        LogbookLifeCyclesClientFactory.getInstance().getClient();

    private boolean metadatasUpdated = false;

    /**
     * Empty constructor
     */
    public FormatIdentificationActionHandler() {

    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }


    @Override
    public CompositeItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        LOGGER.debug("FormatIdentificationActionHandler running ...");

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            SedaUtils.updateLifeCycleByStep(logbookClient,logbookLifecycleObjectGroupParameters, params);
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
            return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        try {
            formatIdentifier = FormatIdentifierFactory.getInstance().getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
        } catch (final FormatIdentifierNotFoundException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.WORKER_FORMAT_IDENTIFIER_NOT_FOUND,
                FORMAT_IDENTIFIER_ID), e);
            itemStatus.increment(StatusCode.FATAL);
            return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        } catch (final FormatIdentifierFactoryException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.WORKER_FORMAT_IDENTIFIER_IMPLEMENTATION_NOT_FOUND,
                FORMAT_IDENTIFIER_ID), e);
            itemStatus.increment(StatusCode.FATAL);
            return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        } catch (final FormatIdentifierTechnicalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.WORKER_FORMAT_IDENTIFIER_TECHNICAL_INTERNAL_ERROR), e);
            itemStatus.increment(StatusCode.FATAL);
            return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }
        String filename = null;
        //WorkspaceClientFactory.changeMode(params.getUrlWorkspace());
        try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            // Get objectGroup metadatas
            final JsonNode jsonOG = getJsonFromWorkspace(workspaceClient, params.getContainerName(),
                IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName());

            final Map<String, String> objectIdToUri = getMapOfObjectsIdsAndUris(jsonOG);

            final JsonNode qualifiers = jsonOG.get(SedaConstants.PREFIX_QUALIFIERS);
            if (qualifiers != null) {
                final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
                if (versions != null && !versions.isEmpty()) {
                    for (final JsonNode versionsArray : versions) {
                        for (final JsonNode version : versionsArray) {
                            try {
                                final JsonNode jsonFormatIdentifier =
                                    version.get(SedaConstants.TAG_FORMAT_IDENTIFICATION);
                                final String objectId = version.get(SedaConstants.PREFIX_ID).asText();


                                // Retrieve the file
                                filename = loadFileFromWorkspace(workspaceClient, params.getContainerName(),
                                    objectIdToUri.get(objectId), params.getObjectName(), objectId);

                                ObjectCheckFormatResult result =
                                    executeOneObjectFromOG(objectId, jsonFormatIdentifier, filename, version);

                                itemStatus.increment(result.getStatus());

                                if (StatusCode.FATAL.equals(itemStatus.getGlobalStatus())) {
                                    return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                                }
                            } finally {
                                if (filename != null) {
                                    deleteLocalFile(filename);
                                }
                                filename = null;
                            }
                        }
                    }
                }
            }

            if (metadatasUpdated) {
                try (final InputStreamFromOutputStream<String> isos = new InputStreamFromOutputStream<String>() {

                    @Override
                    protected String produce(OutputStream sink) throws Exception {
                        JsonHandler.writeAsOutputStream(jsonOG, sink);
                        return params.getObjectName();
                    }
                }) {
                    workspaceClient.putObject(params.getContainerName(),
                        IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName(),
                        isos);
                } catch (IOException e) {
                    throw new ProcessingException("Issue while reading/writing the ObjectGroup", e);
                }
            }

        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (final ContentAddressableStorageServerException e) {
            // workspace error
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        } finally {
            try {
                // delete the file
                deleteLocalFile(filename);
            } catch (final ProcessingException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }

        try {

            commitLifecycleLogbook(itemStatus, params.getObjectName());
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            // FIXME P0 WORKFLOW is it warning of something else ? is it really KO logbook message ?
            if (!StatusCode.FATAL.equals(itemStatus.getGlobalStatus()) &&
                !StatusCode.KO.equals(itemStatus.getGlobalStatus())) {
                itemStatus.setItemId("LOGBOOK_COMMIT_KO");
                itemStatus.increment(StatusCode.WARNING);
            } else {
                itemStatus.setItemId("LOGBOOK_COMMIT_KO");
                itemStatus.increment(StatusCode.KO);
            }

        }

        if (itemStatus.getGlobalStatus().getStatusLevel() == StatusCode.UNKNOWN.getStatusLevel()) {
            itemStatus.increment(StatusCode.OK);
        }

        LOGGER.debug("FormatIdentificationActionHandler response: " + itemStatus.getGlobalStatus());
        return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * Update lifecycle logbook at the end off process
     *
     * @param response the process result
     * @throws ProcessingException thrown if one error occurred
     */
    private void commitLifecycleLogbook(ItemStatus itemStatus, String ogID) throws ProcessingException {
        // TODO P0 WORKFLOW use the real message sub code
        // FIXME P0 TODO WORKFLOW MUST BE itemStatus.getmESSAGE()
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            VitamLogbookMessages.getCodeLfc(itemStatus.getItemId(), itemStatus.getGlobalStatus()));
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, ogID);
        SedaUtils.setLifeCycleFinalEventStatusByStep(logbookClient,logbookLifecycleObjectGroupParameters,
            itemStatus.getGlobalStatus());
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Do not know...
    }

    private ObjectCheckFormatResult executeOneObjectFromOG(String objectId, JsonNode formatIdentification,
        String filename, JsonNode version) {
        final ObjectCheckFormatResult objectCheckFormatResult = new ObjectCheckFormatResult(objectId);
        objectCheckFormatResult.setStatus(StatusCode.OK);
        objectCheckFormatResult.setSubStatus("FILE_FORMAT_OK");
        try {

            // check the file
            final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(Paths.get(filename));

            final FormatIdentifierResponse format = getFirstPronomFormat(formats);
            if (format == null) {
                throw new FileFormatNotFoundException("File format not found in " + FORMAT_IDENTIFIER_ID);
            }

            final String formatId = format.getPuid();

            final Select select = new Select();
            select.setQuery(eq(FileFormat.PUID, formatId));
            final JsonNode result;
            try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
                result = adminClient.getFormats(select.getFinalSelect());
            }

            // TODO P1 : what should we do if more than 1 result (for the moment, we take into account the first one)
            if (result.size() == 0) {
                // format not found in vitam referential
                objectCheckFormatResult.setStatus(StatusCode.KO);
                objectCheckFormatResult.setSubStatus("FILE_FORMAT_PUID_NOT_FOUND");
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                    objectCheckFormatResult.getSubStatus());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    "PUID " +
                        "trouvé : " + formatId);
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
                    objectCheckFormatResult.getStatus().name());
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, objectId);
                try {
                    updateLifeCycleLogbook(logbookLifecycleObjectGroupParameters);
                } catch (final LogbookClientException e) {
                    LOGGER.error(e);
                    objectCheckFormatResult.setStatus(StatusCode.FATAL);
                    objectCheckFormatResult.setSubStatus("FILE_FORMAT_TECHNICAL_ERROR");
                }
            } else {
                // check formatIdentification
                checkAndUpdateFormatIdentification(objectId, formatIdentification, objectCheckFormatResult, result,
                    version);
            }
        } catch (ReferentialException | InvalidParseOperationException | InvalidCreateOperationException |
            IOException e) {
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.FATAL);
            objectCheckFormatResult.setSubStatus("FILE_FORMAT_REFERENTIAL_ERROR");
        } catch (final FormatIdentifierBadRequestException e) {
            // path does not match a file
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.FATAL);
            objectCheckFormatResult.setSubStatus("FILE_FORMAT_OBJECT_NOT_FOUND");
        } catch (final FileFormatNotFoundException e) {
            // format no found case
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.KO);
            objectCheckFormatResult.setSubStatus("FILE_FORMAT_NOT_FOUND");
            // TODO P0 WORKFLOW : use sub status for lifecycle message, for now we send the substatus
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                objectCheckFormatResult.getSubStatus());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                objectCheckFormatResult.getStatus().name());
            try {
                updateLifeCycleLogbook(logbookLifecycleObjectGroupParameters);
            } catch (final LogbookClientException exc) {
                LOGGER.error(exc);
                objectCheckFormatResult.setStatus(StatusCode.FATAL);
                objectCheckFormatResult.setSubStatus("FILE_FORMAT_TECHNICAL_ERROR");
            }
        } catch (final FormatIdentifierTechnicalException e) {
            // technical error
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.FATAL);
            objectCheckFormatResult.setSubStatus("FILE_FORMAT_TECHNICAL_ERROR");
        } catch (final FormatIdentifierNotFoundException e) {
            // identifier does not respond
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.FATAL);
            objectCheckFormatResult.setSubStatus("FILE_FORMAT_TOOL_DOES_NOT_ANSWER");
        }
        return objectCheckFormatResult;
    }

    private void checkAndUpdateFormatIdentification(String objectId, JsonNode formatIdentification,
        ObjectCheckFormatResult objectCheckFormatResult, JsonNode result, JsonNode version) {
        final JsonNode refFormat = result.get(0);
        final JsonNode puid = refFormat.get(FileFormat.PUID);
        final StringBuilder diff = new StringBuilder();
        if ((formatIdentification == null || !formatIdentification.isObject()) && puid != null) {
            formatIdentification = JsonHandler.createObjectNode();
            ((ObjectNode) version).set(SedaConstants.TAG_FORMAT_IDENTIFICATION, formatIdentification);
        }
        if (formatIdentification != null) {
            JsonNode fiPuid = formatIdentification.get(SedaConstants.TAG_FORMAT_ID);
            if (!puid.equals(fiPuid)) {
                objectCheckFormatResult.setStatus(StatusCode.WARNING);
                if (fiPuid != null && fiPuid.size() != 0) {
                    diff.append("- PUID : ");
                    diff.append(fiPuid);
                    diff.append('\n');
                }
                ((ObjectNode) formatIdentification).set(SedaConstants.TAG_FORMAT_ID, puid);
                diff.append("+ PUID : ");
                diff.append(puid);
                metadatasUpdated = true;
            }
            final JsonNode name = refFormat.get(FileFormat.NAME);
            JsonNode fiFormatLitteral = formatIdentification.get(SedaConstants.TAG_FORMAT_LITTERAL);
            if (!name.equals(fiFormatLitteral)) {
                if (diff.length() != 0) {
                    diff.append('\n');
                }
                if (fiFormatLitteral != null && fiFormatLitteral.size() != 0) {
                    diff.append("- " + SedaConstants.TAG_FORMAT_LITTERAL + " : ");
                    diff.append(fiFormatLitteral);
                    diff.append('\n');
                }
                objectCheckFormatResult.setStatus(StatusCode.WARNING);
                ((ObjectNode) formatIdentification).set(SedaConstants.TAG_FORMAT_LITTERAL, name);
                diff.append("+ " + SedaConstants.TAG_FORMAT_LITTERAL + " : ");
                diff.append(name);
                metadatasUpdated = true;
            }
            final JsonNode mimeType = refFormat.get(FileFormat.MIME_TYPE);
            JsonNode fiMimeType = formatIdentification.get(SedaConstants.TAG_MIME_TYPE);
            if (!mimeType.equals(fiMimeType)) {
                if (diff.length() != 0) {
                    diff.append('\n');
                }
                if (fiMimeType != null && fiMimeType.size() != 0) {
                    diff.append("- " + SedaConstants.TAG_MIME_TYPE + " : ");
                    diff.append(fiMimeType);
                    diff.append('\n');
                }
                objectCheckFormatResult.setStatus(StatusCode.WARNING);
                ((ObjectNode) formatIdentification).set(SedaConstants.TAG_MIME_TYPE, mimeType);
                diff.append("+ " + SedaConstants.TAG_MIME_TYPE + " : ");
                diff.append(mimeType);
                metadatasUpdated = true;
            }
        }

        if (StatusCode.WARNING.equals(objectCheckFormatResult.getStatus())) {
            objectCheckFormatResult.setSubStatus("FILE_FORMAT_METADATA_UPDATE");
            // TODO P0 WORKFLOW : use sub status for lifecycle message, for now we send the substatus
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                objectCheckFormatResult.getSubStatus());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                // TODO P0 WORKFLOW : "Des informations de formats ont été complétées par Vitam :\n" + diff.toString());
                VitamLogbookMessages.getCodeLfc(
                    logbookLifecycleObjectGroupParameters.getParameterValue(LogbookParameterName.eventType),
                    objectCheckFormatResult.getSubStatus(), objectCheckFormatResult.getStatus(), diff.toString()));

            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcome,
                objectCheckFormatResult.getStatus().name());
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, objectId);
            // TODO P1 : create a real json object
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventDetailData,
                "{\"diff\": \"" + diff.toString().replaceAll("\"", "'") + "\"}");
            try {
                updateLifeCycleLogbook(logbookLifecycleObjectGroupParameters);
            } catch (final LogbookClientException e) {
                LOGGER.error(e);
                objectCheckFormatResult.setStatus(StatusCode.FATAL);
                objectCheckFormatResult.setSubStatus("FILE_FORMAT_TECHNICAL_ERROR");
            }
        }
    }

    private void updateLifeCycleLogbook(LogbookLifeCycleObjectGroupParameters logbookParameters)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        logbookClient.update(logbookParameters);
    }

    /**
     * Retrieve the first corresponding file format from pronom referentiel
     *
     * @param formats formats list to analyse
     * @return the first pronom file format or null if not found
     */
    private FormatIdentifierResponse getFirstPronomFormat(List<FormatIdentifierResponse> formats) {
        for (final FormatIdentifierResponse format : formats) {
            if (FormatIdentifierSiegfried.PRONOM_NAMESPACE.equals(format.getMatchedNamespace())) {
                return format;
            }
        }
        return null;
    }

    /**
     * Retrieve a json file as a {@link JsonNode} from the workspace.
     *
     * @param workspaceClient workspace connector
     * @param containerId container id
     * @param jsonFilePath path in workspace of the json File
     * @return JsonNode of the json file
     * @throws ProcessingException throws when error occurs
     */
    private JsonNode getJsonFromWorkspace(WorkspaceClient workspaceClient, String containerId, String jsonFilePath)
        throws ProcessingException {
        try (InputStream is =
            workspaceClient.getObject(containerId, jsonFilePath)) {
            if (is != null) {
                return JsonHandler.getFromInputStream(is, JsonNode.class);
            } else {
                LOGGER.error("Object group not found");
                throw new ProcessingException("Object group not found");
            }
        } catch (InvalidParseOperationException | IOException e) {
            LOGGER.debug("Json wrong format", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Workspace Server Error", e);
            throw new ProcessingException(e);
        }
    }

    private String loadFileFromWorkspace(WorkspaceClient workspaceClient, String containerId, String filePath,
        String objectGroupGuid, String objectGuid)
        throws ProcessingException {
        try (InputStream is =
            workspaceClient.getObject(containerId, IngestWorkflowConstants.SEDA_FOLDER + "/" + filePath)) {
            final String fileName = containerId + objectGroupGuid + objectGuid;
            final File file = new File(VitamConfiguration.getVitamTmpFolder() + "/" + fileName);

            if (!file.exists()) {
                try (OutputStream os = new FileOutputStream(file)) {
                    IOUtils.copy(is, os);
                }
                return fileName;
            } else {
                throw new ProcessingException("Cannot save file because it already exists");
            }
        } catch (final IOException e) {
            LOGGER.debug("Error while saving the file", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Workspace Server Error", e);
            throw new ProcessingException(e);
        }
    }


    private void deleteLocalFile(String fileName)
        throws ProcessingException {
        if (fileName != null && !fileName.isEmpty()) {
            final File file = new File(VitamConfiguration.getVitamTmpFolder() + "/" + fileName);
            if (file.exists()) {
                file.delete();
            } else {
                LOGGER.warn("Cannot delete file because it does not exists {}", fileName);
            }
        }
    }


    private Map<String, String> getMapOfObjectsIdsAndUris(JsonNode jsonOG) throws ProcessingException {
        final Map<String, String> binaryObjectsToStore = new HashMap<>();

        // Filter on objectGroup objects ids to retrieve only binary objects
        // informations linked to the ObjectGroup
        final JsonNode work = jsonOG.get(SedaConstants.PREFIX_WORK);
        final JsonNode qualifiers = work.get(SedaConstants.PREFIX_QUALIFIERS);
        if (qualifiers == null) {
            return binaryObjectsToStore;
        }

        final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
        if (versions == null || versions.isEmpty()) {
            return binaryObjectsToStore;
        }
        for (final JsonNode version : versions) {
            for (final JsonNode binaryObject : version) {
                binaryObjectsToStore.put(binaryObject.get(SedaConstants.PREFIX_ID).asText(),
                    binaryObject.get(SedaConstants.TAG_URI).asText());
            }
        }
        return binaryObjectsToStore;
    }

    /**
     * Object used to keep all file format result for all objects. Not really actually used, but can be usefull
     */
    private class ObjectCheckFormatResult {
        private final String objectId;
        private StatusCode status;
        private String subStatus;

        ObjectCheckFormatResult(String objectId) {
            this.objectId = objectId;
        }

        public void setStatus(StatusCode status) {
            this.status = status;
        }

        public void setSubStatus(String subStatus) {
            this.subStatus = subStatus;
        }

        @SuppressWarnings("unused")
        public String getObjectId() {
            return objectId;
        }

        public StatusCode getStatus() {
            return status;
        }

        public String getSubStatus() {
            return subStatus;
        }
    }

    @Override
    public void close() {
        if (logbookClient != null) {
            logbookClient.close();
        }
    }
}
