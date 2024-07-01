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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FileFormatRejectedException;
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
import fr.gouv.vitam.common.model.administration.ContractsDetailsModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

/**
 * FormatIdentificationAction Plugin.<br>
 */
public class FormatIdentificationActionPlugin extends ActionHandler implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FormatIdentificationActionPlugin.class);

    /**
     * File format treatment
     */
    public static final String FILE_FORMAT = "FILE_FORMAT";

    /**
     * FileFormat PUID field key
     */
    public static final String PUID = "PUID";

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
    private static final String FILE_FORMAT_REJECTED = "REJECTED_FORMAT";

    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";
    private static final int OG_INPUT_RANK = 0;

    private static final String SUBSTATUS_UNKNOWN = "UNKNOWN";
    private static final String SUBSTATUS_UNCHARTED = "UNCHARTED";
    private static final String SUBSTATUS_REJECTED = "REJECTED";

    private static final int REFERENTIAL_INGEST_CONTRACT_PARAMETERS_RANK = 1;
    private static final String UNKNOWN_FORMAT = "unknown";

    private final AdminManagementClientFactory adminManagementClientFactory;
    private final FormatIdentifierFactory formatIdentifierFactory;

    /**
     * Empty constructor
     */
    public FormatIdentificationActionPlugin() {
        this(AdminManagementClientFactory.getInstance(), FormatIdentifierFactory.getInstance());
    }

    @VisibleForTesting
    public FormatIdentificationActionPlugin(
        AdminManagementClientFactory adminManagementClientFactory,
        FormatIdentifierFactory formatIdentifierFactory
    ) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.formatIdentifierFactory = formatIdentifierFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);
        LOGGER.debug("FormatIdentificationActionHandler running ...");

        final ItemStatus itemStatus = new ItemStatus(FILE_FORMAT);
        FormatIdentifier formatIdentifier;
        try {
            formatIdentifier = formatIdentifierFactory.getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
        } catch (final FormatIdentifierNotFoundException e) {
            LOGGER.error(
                VitamCodeHelper.getLogMessage(VitamCode.WORKER_FORMAT_IDENTIFIER_NOT_FOUND, FORMAT_IDENTIFIER_ID),
                e
            );
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(FILE_FORMAT).setItemsStatus(FILE_FORMAT, itemStatus);
        } catch (final FormatIdentifierFactoryException e) {
            LOGGER.error(
                VitamCodeHelper.getLogMessage(
                    VitamCode.WORKER_FORMAT_IDENTIFIER_IMPLEMENTATION_NOT_FOUND,
                    FORMAT_IDENTIFIER_ID
                ),
                e
            );
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(FILE_FORMAT).setItemsStatus(FILE_FORMAT, itemStatus);
        } catch (final FormatIdentifierTechnicalException e) {
            LOGGER.error(VitamCodeHelper.getLogMessage(VitamCode.WORKER_FORMAT_IDENTIFIER_TECHNICAL_INTERNAL_ERROR), e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(FILE_FORMAT).setItemsStatus(FILE_FORMAT, itemStatus);
        }
        try {
            // Get objectGroup metadatas
            final JsonNode jsonOG = (JsonNode) handlerIO.getInput(OG_INPUT_RANK);
            boolean metadataUpdated = false;

            final Map<String, String> objectIdToUri = getMapOfObjectsIdsAndUris(jsonOG);

            final JsonNode qualifiers = jsonOG.get(SedaConstants.PREFIX_QUALIFIERS);
            if (qualifiers != null) {
                final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
                if (versions != null && !versions.isEmpty()) {
                    for (final JsonNode versionsArray : versions) {
                        for (final JsonNode version : versionsArray) {
                            if (version.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                                File file = null;
                                try {
                                    final JsonNode jsonFormatIdentifier = version.get(
                                        SedaConstants.TAG_FORMAT_IDENTIFICATION
                                    );
                                    final String objectId = version.get(SedaConstants.PREFIX_ID).asText();
                                    // Retrieve the file
                                    file = loadFileFromWorkspace(handlerIO, objectIdToUri.get(objectId));

                                    final ObjectCheckFormatResult result = executeOneObjectFromOG(
                                        handlerIO,
                                        formatIdentifier,
                                        objectId,
                                        jsonFormatIdentifier,
                                        file,
                                        version
                                    );

                                    if (result.getMetadataUpdated()) {
                                        metadataUpdated = true;
                                    }

                                    // create ItemStatus for subtask
                                    ItemStatus subTaskItemStatus = new ItemStatus(FILE_FORMAT);
                                    subTaskItemStatus.increment(result.getStatus());
                                    itemStatus.increment(result.getStatus());

                                    if (result.getStatus().equals(StatusCode.KO)) {
                                        switch (result.getSubStatus()) {
                                            case FILE_FORMAT_NOT_FOUND:
                                                subTaskItemStatus.setGlobalOutcomeDetailSubcode(SUBSTATUS_UNKNOWN);
                                                break;
                                            case FILE_FORMAT_NOT_FOUND_REFERENTIAL_ERROR:
                                            case FILE_FORMAT_PUID_NOT_FOUND:
                                                subTaskItemStatus.setGlobalOutcomeDetailSubcode(SUBSTATUS_UNCHARTED);
                                                break;
                                            case FILE_FORMAT_REJECTED:
                                                subTaskItemStatus.setGlobalOutcomeDetailSubcode(SUBSTATUS_REJECTED);
                                                itemStatus.setGlobalOutcomeDetailSubcode(FILE_FORMAT_REJECTED);
                                                break;
                                        }
                                        LOGGER.error(JsonHandler.unprettyPrint(result));
                                    }

                                    itemStatus.setSubTaskStatus(objectId, subTaskItemStatus);

                                    if (result.getEventDetailData() != null) {
                                        itemStatus
                                            .getSubTaskStatus()
                                            .get(objectId)
                                            .setEvDetailData(result.getEventDetailData());
                                    }

                                    if (StatusCode.FATAL.equals(itemStatus.getGlobalStatus())) {
                                        return new ItemStatus(FILE_FORMAT).setItemsStatus(FILE_FORMAT, itemStatus);
                                    }
                                } finally {
                                    if (file != null) {
                                        try {
                                            Files.delete(file.toPath());
                                        } catch (IOException e) {
                                            LOGGER.error(e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (metadataUpdated) {
                handlerIO.transferJsonToWorkspace(
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER,
                    params.getObjectName(),
                    jsonOG,
                    false,
                    false
                );
            }
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
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

    private ObjectCheckFormatResult executeOneObjectFromOG(
        HandlerIO handlerIO,
        FormatIdentifier formatIdentifier,
        String objectId,
        JsonNode manifestFormatIdentification,
        File file,
        JsonNode version
    ) {
        final ObjectCheckFormatResult objectCheckFormatResult = new ObjectCheckFormatResult(objectId);
        objectCheckFormatResult.setStatus(StatusCode.OK);

        boolean formatUnidentifiedAuthorized = false;
        boolean everyFormatType = true;
        Set<String> formatTypeSet;
        try {
            IngestContractModel ingestContract = loadIngestContractFromWorkspace(handlerIO);
            everyFormatType = ingestContract.isEveryFormatType();
            formatUnidentifiedAuthorized = ingestContract.isFormatUnidentifiedAuthorized();
            formatTypeSet = ingestContract.getFormatType();

            if (!everyFormatType && !identifiedFormatsRestricted(manifestFormatIdentification, formatTypeSet)) {
                throw new FileFormatRejectedException("File format rejected in " + FORMAT_IDENTIFIER_ID);
            }

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
            try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
                result = adminClient.getFormats(select.getFinalSelect());
            }

            if (!result.isOk() || ((RequestResponseOK<FileFormatModel>) result).getResults().isEmpty()) {
                // format not found in vitam referential
                if (formatUnidentifiedAuthorized) {
                    checkNotFoundFormatIdentification(manifestFormatIdentification, version, objectCheckFormatResult);
                }
                objectCheckFormatResult.setStatus(StatusCode.KO);
                objectCheckFormatResult.setSubStatus(FILE_FORMAT_PUID_NOT_FOUND);
            } else {
                // check formatIdentification

                RequestResponseOK<FileFormatModel> requestResponseOK = (RequestResponseOK<FileFormatModel>) result;
                List<FileFormatModel> results = requestResponseOK.getResults();
                FileFormatModel refFormat = results.get(0);

                checkFormatIdentification(
                    manifestFormatIdentification,
                    version,
                    refFormat.getPuid(),
                    refFormat.getName(),
                    refFormat.getMimeType(),
                    objectCheckFormatResult
                );
            }
        } catch (
            InvalidParseOperationException
            | InvalidCreateOperationException
            | FormatIdentifierTechnicalException
            | IOException e
        ) {
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
            if (formatUnidentifiedAuthorized && everyFormatType) {
                checkNotFoundFormatIdentification(manifestFormatIdentification, version, objectCheckFormatResult);
                objectCheckFormatResult.setStatus(StatusCode.WARNING);
            } else {
                objectCheckFormatResult.setStatus(StatusCode.KO);
            }
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_NOT_FOUND);
        } catch (final FormatIdentifierNotFoundException e) {
            // identifier does not respond
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.FATAL);
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_TOOL_DOES_NOT_ANSWER);
        } catch (final FileFormatRejectedException e) {
            LOGGER.error(e);
            objectCheckFormatResult.setStatus(StatusCode.KO);
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_REJECTED);
        }

        return objectCheckFormatResult;
    }

    private JsonNode checkAndUpdateFormatIdentification(
        JsonNode manifestFormatIdentification,
        ObjectCheckFormatResult objectCheckFormatResult,
        String puid,
        String name,
        String mimeType,
        JsonNode version,
        ObjectNode diffJsonNodeToPopulate
    ) {
        boolean isMetadataLocallyUpdated = false;

        JsonNode newFormatIdentification = manifestFormatIdentification != null
            ? manifestFormatIdentification.deepCopy()
            : null;

        if ((newFormatIdentification == null || !newFormatIdentification.isObject()) && puid != null) {
            newFormatIdentification = JsonHandler.createObjectNode();
            ((ObjectNode) version).set(SedaConstants.TAG_FORMAT_IDENTIFICATION, newFormatIdentification);
        }

        if (newFormatIdentification != null) {
            isMetadataLocallyUpdated = checkAndUpdateManifestFormatFieldAgainstReferentialValue(
                newFormatIdentification,
                PUID,
                SedaConstants.TAG_FORMAT_ID,
                puid,
                diffJsonNodeToPopulate,
                objectCheckFormatResult
            );

            isMetadataLocallyUpdated = checkAndUpdateManifestFormatFieldAgainstReferentialValue(
                newFormatIdentification,
                SedaConstants.TAG_FORMAT_LITTERAL,
                SedaConstants.TAG_FORMAT_LITTERAL,
                name,
                diffJsonNodeToPopulate,
                objectCheckFormatResult
            ) ||
            isMetadataLocallyUpdated;

            isMetadataLocallyUpdated = checkAndUpdateManifestFormatFieldAgainstReferentialValue(
                newFormatIdentification,
                SedaConstants.TAG_MIME_TYPE,
                SedaConstants.TAG_MIME_TYPE,
                mimeType,
                diffJsonNodeToPopulate,
                objectCheckFormatResult
            ) ||
            isMetadataLocallyUpdated;
        }

        if (isMetadataLocallyUpdated) {
            objectCheckFormatResult.setMetadataUpdated(true);
        }

        if (StatusCode.WARNING.equals(objectCheckFormatResult.getStatus())) {
            objectCheckFormatResult.setSubStatus(FILE_FORMAT_UPDATED_FORMAT);
        }

        return newFormatIdentification;
    }

    private boolean checkAndUpdateManifestFormatFieldAgainstReferentialValue(
        JsonNode newFormatIdentification,
        String fieldName,
        String manifestFieldName,
        String referentialFormatFieldValue,
        ObjectNode diffJsonNodeToPopulate,
        ObjectCheckFormatResult objectCheckFormatResult
    ) {
        boolean isManifestFiedlUpdated = false;

        final String manifestFieldValue = newFormatIdentification.get(manifestFieldName) != null
            ? newFormatIdentification.get(manifestFieldName).asText()
            : null;

        if (referentialFormatFieldValue != null && !referentialFormatFieldValue.equals(manifestFieldValue)) {
            if (manifestFieldValue != null) {
                diffJsonNodeToPopulate.put("- " + fieldName, manifestFieldValue);
                if (fieldName.equals(PUID)) {
                    objectCheckFormatResult.setStatus(StatusCode.WARNING);
                }
            }
            ((ObjectNode) newFormatIdentification).put(manifestFieldName, referentialFormatFieldValue);
            diffJsonNodeToPopulate.put("+ " + fieldName, referentialFormatFieldValue);
            isManifestFiedlUpdated = true;
        }

        return isManifestFiedlUpdated;
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

    private File loadFileFromWorkspace(HandlerIO handlerIO, String filePath) throws ProcessingException {
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

    private Map<String, String> getMapOfObjectsIdsAndUris(JsonNode jsonOG) {
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
                    binaryObjectsToStore.put(
                        binaryObject.get(SedaConstants.PREFIX_ID).asText(),
                        binaryObject.get(SedaConstants.TAG_URI).asText()
                    );
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
        private boolean metadataUpdated;
        private String eventDetailData;

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

        public void setMetadataUpdated(boolean metadataUpdated) {
            this.metadataUpdated = metadataUpdated;
        }

        public boolean getMetadataUpdated() {
            return metadataUpdated;
        }

        public String getEventDetailData() {
            return eventDetailData;
        }

        public void setEventDetailData(String eventDetailData) {
            this.eventDetailData = eventDetailData;
        }
    }

    private IngestContractModel loadIngestContractFromWorkspace(HandlerIO handlerIO)
        throws InvalidParseOperationException {
        ContractsDetailsModel contractsDetailsModel = JsonHandler.getFromFile(
            (File) handlerIO.getInput(REFERENTIAL_INGEST_CONTRACT_PARAMETERS_RANK),
            ContractsDetailsModel.class
        );
        return contractsDetailsModel.getIngestContractModel();
    }

    private boolean identifiedFormatsRestricted(final JsonNode format, Set<String> formatTypeSet) {
        final JsonNode formatIdNode = format.get(SedaConstants.TAG_FORMAT_ID);
        return formatIdNode != null && formatTypeSet.contains(formatIdNode.asText());
    }

    private void checkNotFoundFormatIdentification(
        JsonNode formatIdentification,
        JsonNode version,
        ObjectCheckFormatResult objectCheckFormatResult
    ) {
        String formatId = UNKNOWN_FORMAT;
        String formatName = formatIdentification == null ||
            formatIdentification.get(SedaConstants.TAG_FORMAT_LITTERAL) == null
            ? ""
            : formatIdentification.get(SedaConstants.TAG_FORMAT_LITTERAL).asText();
        String mimeType = formatIdentification == null || formatIdentification.get(SedaConstants.TAG_MIME_TYPE) == null
            ? ""
            : formatIdentification.get(SedaConstants.TAG_MIME_TYPE).asText();

        checkFormatIdentification(
            formatIdentification,
            version,
            formatId,
            formatName,
            mimeType,
            objectCheckFormatResult
        );
    }

    private void checkFormatIdentification(
        JsonNode manifestFormatIdentification,
        JsonNode version,
        String puid,
        String name,
        String mimeType,
        ObjectCheckFormatResult objectCheckFormatResult
    ) {
        // check formatIdentification
        ObjectNode diffJsonObject = JsonHandler.createObjectNode();
        final JsonNode newFormatIdentification = checkAndUpdateFormatIdentification(
            manifestFormatIdentification,
            objectCheckFormatResult,
            puid,
            name,
            mimeType,
            version,
            diffJsonObject
        );
        if (diffJsonObject.size() > 0) {
            JsonNode wrappingDiffJsonObject = JsonHandler.createObjectNode().set("diff", diffJsonObject);
            objectCheckFormatResult.setEventDetailData(JsonHandler.unprettyPrint(wrappingDiffJsonObject));
        }
        // Reassign new format
        ((ObjectNode) version).set(SedaConstants.TAG_FORMAT_IDENTIFICATION, newFormatIdentification);
    }
}
