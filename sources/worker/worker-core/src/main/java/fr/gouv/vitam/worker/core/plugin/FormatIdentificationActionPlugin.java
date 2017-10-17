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
package fr.gouv.vitam.worker.core.plugin;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.SedaConstants;
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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * FormatIdentificationAction Plugin.<br>
 *
 */

// TODO P1: refactor me
// TODO P0: review Logbook messages (operation / lifecycle)
// TODO P0: fully use VitamCode

public class FormatIdentificationActionPlugin extends ActionHandler implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FormatIdentificationActionPlugin.class);

    /**
     * File format treatment
     */
    public static final String FILE_FORMAT = "FILE_FORMAT";

    /**
     * Error list for file format treatment
     */
    private static final String FILE_FORMAT_TOOL_DOES_NOT_ANSWER = "TOOL_DOES_NOT_ANSWER";
    private static final String FILE_FORMAT_OBJECT_NOT_FOUND = "OBJECT_NOT_FOUND";
    private static final String FILE_FORMAT_NOT_FOUND = "NOT_FOUND";
    private static final String FILE_FORMAT_UPDATED_FORMAT = "UPDATED_FORMAT";
    private static final String FILE_FORMAT_PUID_NOT_FOUND = "PUID_NOT_FOUND";
    private static final String FILE_FORMAT_NOT_FOUND_REFERENTIAL_ERROR = "NOT_FOUND_REFERENTIAL";
    private static final String FILE_FORMAT_REFERENTIAL_TECHNICAL_ERROR = "FILE_FORMAT_REFERENTIAL_TECHNICAL_ERROR";

    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";
    private static final int OG_INPUT_RANK = 0;
    
    private static final String SUBSTATUS_UNKNOWN = "UNKNOWN";
    private static final String SUBSTATUS_UNCHARTED = "UNCHARTED";

    private HandlerIO handlerIO;
    private FormatIdentifier formatIdentifier;

    private boolean metadatasUpdated = false;
    String eventDetailData;
    private boolean asyncIO = false;

    /**
     * Empty constructor
     */
    public FormatIdentificationActionPlugin() {

    }


    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        handlerIO = handler;
        LOGGER.debug("FormatIdentificationActionHandler running ...");

        final ItemStatus itemStatus = new ItemStatus(FILE_FORMAT);

        try {
            formatIdentifier = FormatIdentifierFactory.getInstance().getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
        } catch (final FormatIdentifierNotFoundException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.WORKER_FORMAT_IDENTIFIER_NOT_FOUND,
                FORMAT_IDENTIFIER_ID), e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(FILE_FORMAT).setItemsStatus(FILE_FORMAT, itemStatus);
        } catch (final FormatIdentifierFactoryException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.WORKER_FORMAT_IDENTIFIER_IMPLEMENTATION_NOT_FOUND,
                FORMAT_IDENTIFIER_ID), e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(FILE_FORMAT).setItemsStatus(FILE_FORMAT, itemStatus);
        } catch (final FormatIdentifierTechnicalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.WORKER_FORMAT_IDENTIFIER_TECHNICAL_INTERNAL_ERROR),
                e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(FILE_FORMAT).setItemsStatus(FILE_FORMAT, itemStatus);
        }
        File file = null;
        try {
            // Get objectGroup metadatas
            final JsonNode jsonOG = (JsonNode) handlerIO.getInput(OG_INPUT_RANK);

            final Map<String, String> objectIdToUri = getMapOfObjectsIdsAndUris(jsonOG);

            final JsonNode qualifiers = jsonOG.get(SedaConstants.PREFIX_QUALIFIERS);
            if (qualifiers != null) {
                final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
                if (versions != null && !versions.isEmpty()) {
                    for (final JsonNode versionsArray : versions) {
                        for (final JsonNode version : versionsArray) {
                            if (version.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                                try {
                                    final JsonNode jsonFormatIdentifier =
                                        version.get(SedaConstants.TAG_FORMAT_IDENTIFICATION);
                                    final String objectId = version.get(SedaConstants.PREFIX_ID).asText();
                                    // Retrieve the file
                                    file = loadFileFromWorkspace(objectIdToUri.get(objectId));

                                    final ObjectCheckFormatResult result =
                                        executeOneObjectFromOG(objectId, jsonFormatIdentifier, file, version);

                                    // create ItemStatus for subtask
                                    ItemStatus subTaskItemStatus = new ItemStatus(FILE_FORMAT);
                                    subTaskItemStatus.increment(result.getStatus());
                                    itemStatus.increment(result.getStatus());
                                        
                                    if (result.getStatus().equals(StatusCode.KO)) {
                                        switch (result.getSubStatus()) {
                                            case FILE_FORMAT_NOT_FOUND:
                                                subTaskItemStatus.setItemId(FILE_FORMAT + "." + SUBSTATUS_UNKNOWN);
                                                itemStatus.setItemId(FILE_FORMAT + "." + SUBSTATUS_UNKNOWN);
                                                break;
                                            case FILE_FORMAT_NOT_FOUND_REFERENTIAL_ERROR:
                                                subTaskItemStatus.setItemId(FILE_FORMAT + "." + SUBSTATUS_UNCHARTED);
                                                itemStatus.setItemId(FILE_FORMAT + "." + SUBSTATUS_UNCHARTED);
                                                break;
                                            case FILE_FORMAT_PUID_NOT_FOUND:
                                                subTaskItemStatus.setItemId(FILE_FORMAT + "." + SUBSTATUS_UNCHARTED);
                                                itemStatus.setItemId(FILE_FORMAT + "." + SUBSTATUS_UNCHARTED);
                                                break;
                                        }
                                    }
                                    itemStatus.setSubTaskStatus(objectId, subTaskItemStatus);

                                    if (StatusCode.FATAL.equals(itemStatus.getGlobalStatus())) {
                                        return new ItemStatus(FILE_FORMAT).setItemsStatus(FILE_FORMAT, itemStatus);
                                    }
                                } finally {
                                    if (file != null) {
                                        file.delete();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (metadatasUpdated) {
                handlerIO.transferJsonToWorkspace(IngestWorkflowConstants.OBJECT_GROUP_FOLDER, 
                    params.getObjectName(), jsonOG, false, asyncIO);

                if (eventDetailData!=null){
                    itemStatus.setEvDetailData(eventDetailData);
                }
            }

        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        } finally {
            // delete the file
            if (file != null) {
                file.delete();
            }
        }

        if (itemStatus.getGlobalStatus().getStatusLevel() == StatusCode.UNKNOWN.getStatusLevel()) {
            itemStatus.increment(StatusCode.OK);
        }

        LOGGER.debug("FormatIdentificationActionHandler response: " + itemStatus.getGlobalStatus());
        return new ItemStatus(FILE_FORMAT).setItemsStatus(itemStatus.getItemId(), itemStatus);
    }


    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Do not know...
    }

    private ObjectCheckFormatResult executeOneObjectFromOG(String objectId,
        JsonNode formatIdentification,
        File file, JsonNode version) {
        final ObjectCheckFormatResult objectCheckFormatResult = new ObjectCheckFormatResult(objectId);
        objectCheckFormatResult.setStatus(StatusCode.OK);
        try {

            // check the file
            final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(file.toPath());

            final FormatIdentifierResponse format = getFirstPronomFormat(formats);
            if (format == null) {
                throw new FileFormatNotFoundException("File format not found in " + FORMAT_IDENTIFIER_ID);
            }

            final String formatId = format.getPuid();

            final Select select = new Select();
            select.setQuery(eq(FileFormat.PUID, formatId));
            final RequestResponse<FileFormatModel> result;
            try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
                result = adminClient.getFormats(select.getFinalSelect());
            }

            // TODO P1 : what should we do if more than 1 result (for the moment, we take into account the first one)
            if (!result.isOk() || ((RequestResponseOK<FileFormatModel>) result).getResults().size() == 0) {
                // format not found in vitam referential
                objectCheckFormatResult.setStatus(StatusCode.KO);
                objectCheckFormatResult.setSubStatus(FILE_FORMAT_PUID_NOT_FOUND);
            } else {
                // check formatIdentification

                RequestResponseOK<FileFormatModel> requestResponseOK = (RequestResponseOK<FileFormatModel>) result;
                List<FileFormatModel> results = requestResponseOK.getResults();
                FileFormatModel refFormat = results.get(0);
                final JsonNode newFormatIdentification =
                    checkAndUpdateFormatIdentification(objectId, formatIdentification,
                        objectCheckFormatResult, refFormat,
                        version);
                ObjectNode diffObject = JsonHandler.createObjectNode();
                diffObject.set( "-", formatIdentification );
                diffObject.set( "+", newFormatIdentification );
                ObjectNode object = JsonHandler.createObjectNode();
                object.set( "diff", diffObject );
                eventDetailData = object.toString();
                // Reassign new format
                ((ObjectNode) version).set(SedaConstants.TAG_FORMAT_IDENTIFICATION, newFormatIdentification);
            }
        } catch (InvalidParseOperationException | InvalidCreateOperationException | FormatIdentifierTechnicalException |
            IOException e) {
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.FATAL);
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_REFERENTIAL_TECHNICAL_ERROR);
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.KO);
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_NOT_FOUND_REFERENTIAL_ERROR);
        } catch (final FormatIdentifierBadRequestException e) {
            // path does not match a file
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.FATAL);
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_OBJECT_NOT_FOUND);
        } catch (final FileFormatNotFoundException e) {
            // format no found case
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.KO);
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_NOT_FOUND);

        } catch (final FormatIdentifierNotFoundException e) {
            // identifier does not respond
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.FATAL);
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_TOOL_DOES_NOT_ANSWER);

        }

        return objectCheckFormatResult;
    }

    private JsonNode checkAndUpdateFormatIdentification(String objectId,
        JsonNode formatIdentification,
        ObjectCheckFormatResult objectCheckFormatResult, FileFormatModel refFormat, JsonNode version) {

        final String puid = refFormat.getPuid();
        final StringBuilder diff = new StringBuilder();
        JsonNode newFormatIdentification = formatIdentification.deepCopy();
        if ((newFormatIdentification == null || !newFormatIdentification.isObject()) && puid != null) {
            newFormatIdentification = JsonHandler.createObjectNode();
            ((ObjectNode) version).set(SedaConstants.TAG_FORMAT_IDENTIFICATION, newFormatIdentification);
        }
        if (newFormatIdentification != null) {
            final String fiPuid = newFormatIdentification.get(SedaConstants.TAG_FORMAT_ID) != null
                ? newFormatIdentification.get(SedaConstants.TAG_FORMAT_ID).asText() : null;
            if (!puid.equals(fiPuid)) {
                objectCheckFormatResult.setStatus(StatusCode.WARNING);
                if (fiPuid != null) {
                    diff.append("- PUID : ");
                    diff.append(fiPuid);
                    diff.append('\n');
                }
                ((ObjectNode) newFormatIdentification).put(SedaConstants.TAG_FORMAT_ID, puid);
                diff.append("+ PUID : ");
                diff.append(puid);
                metadatasUpdated = true;
            }
            final String name = refFormat.getName();
            final String fiFormatLitteral = newFormatIdentification.get(SedaConstants.TAG_FORMAT_LITTERAL) != null
                ? newFormatIdentification.get(SedaConstants.TAG_FORMAT_LITTERAL).asText() : null;
            if (!name.equals(fiFormatLitteral)) {
                if (diff.length() != 0) {
                    diff.append('\n');
                }
                if (fiFormatLitteral != null) {
                    diff.append("- " + SedaConstants.TAG_FORMAT_LITTERAL + " : ");
                    diff.append(fiFormatLitteral);
                    diff.append('\n');
                }
                objectCheckFormatResult.setStatus(StatusCode.WARNING);
                ((ObjectNode) newFormatIdentification).put(SedaConstants.TAG_FORMAT_LITTERAL, name);
                diff.append("+ " + SedaConstants.TAG_FORMAT_LITTERAL + " : ");
                diff.append(name);
                metadatasUpdated = true;
            }
            final String mimeType = refFormat.getMimeType();
            final String fiMimeType = newFormatIdentification.get(SedaConstants.TAG_MIME_TYPE) != null
                ? newFormatIdentification.get(SedaConstants.TAG_MIME_TYPE).asText() : null;
            if (!mimeType.equals(fiMimeType)) {
                if (diff.length() != 0) {
                    diff.append('\n');
                }
                if (fiMimeType != null) {
                    diff.append("- " + SedaConstants.TAG_MIME_TYPE + " : ");
                    diff.append(fiMimeType);
                    diff.append('\n');
                }
                objectCheckFormatResult.setStatus(StatusCode.WARNING);
                ((ObjectNode) newFormatIdentification).put(SedaConstants.TAG_MIME_TYPE, mimeType);
                diff.append("+ " + SedaConstants.TAG_MIME_TYPE + " : ");
                diff.append(mimeType);
                metadatasUpdated = true;
            }
        }

        if (StatusCode.WARNING.equals(objectCheckFormatResult.getStatus())) {
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_UPDATED_FORMAT);
        }
        
        return newFormatIdentification;
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

    private File loadFileFromWorkspace(String filePath)
        throws ProcessingException {
        try {
            return handlerIO.getFileFromWorkspace(IngestWorkflowConstants.SEDA_FOLDER + "/" + filePath);
        } catch (final IOException e) {
            LOGGER.debug("Error while saving the file", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Workspace Server Error", e);
            throw new ProcessingException(e);
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
                if (binaryObject.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                    binaryObjectsToStore.put(binaryObject.get(SedaConstants.PREFIX_ID).asText(),
                        binaryObject.get(SedaConstants.TAG_URI).asText());
                }
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
        if (formatIdentifier != null) {
            formatIdentifier.close();
        }
    }
}
