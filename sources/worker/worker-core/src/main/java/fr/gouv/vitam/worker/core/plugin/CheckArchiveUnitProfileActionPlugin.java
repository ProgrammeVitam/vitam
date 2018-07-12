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
package fr.gouv.vitam.worker.core.plugin;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.ArchiveUnitProfileEmptyControlSchemaException;
import fr.gouv.vitam.common.exception.ArchiveUnitProfileInactiveException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationStatus;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ArchiveUnitContainSpecialCharactersException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CheckArchiveUnitProfile Plugin.<br>
 *
 */

public class CheckArchiveUnitProfileActionPlugin extends ActionHandler {
    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";

    private static final String UNKNOWN_TECHNICAL_EXCEPTION = "Unknown technical exception";
    private static final String CAN_NOT_SEARCH_PROFILE = "Can not search profile";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckArchiveUnitProfileActionPlugin.class);

    private static final String CHECK_UNIT_PROFILE_TASK_ID = "CHECK_ARCHIVE_UNIT_PROFILE";

    private static final String ARCHIVE_UNIT_PROFILE_SCHEMA = "ArchiveUnitProfileSchema";

    private static final int GUID_MAP_RANK = 0;

    private static final int UNIT_OUT_RANK = 0;

    private boolean isUpdateJsonMandatory = false;

    private final AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Empty constructor CheckArchiveUnitProfileActionPlugin
     *
     */
    public CheckArchiveUnitProfileActionPlugin() {
        this(AdminManagementClientFactory.getInstance());
    }

    /**
     * Empty constructor CheckArchiveUnitProfileActionPlugin
     * 
     * @param adminManagementClientFactory
     *
     */
    @VisibleForTesting
    public CheckArchiveUnitProfileActionPlugin(AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) throws ProcessingException {
        checkMandatoryParameters(params);

        final String objectName = params.getObjectName();
        final ItemStatus itemStatus = new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID);
        ObjectNode infoNode = JsonHandler.createObjectNode();
        String archiveUnitProfileIdentifier = "";
        String unitId = "";
        try (InputStream archiveUnitToJson =
            handlerIO.getInputStreamFromWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER +
                File.separator + objectName)) {

            JsonNode archiveUnit = JsonHandler.getFromInputStream(archiveUnitToJson);
            if (archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT).has(SedaConstants.TAG_ARCHIVE_UNIT_PROFILE)) {
                Map<String, Object> guidSystemArchiveUnit = null;
                InputStream guidMapTmpFile = null;
                final File file = (File) handlerIO.getInput(GUID_MAP_RANK);
                if (file != null) {
                    guidMapTmpFile = new FileInputStream(file);
                }

                if (guidMapTmpFile != null) {
                    guidSystemArchiveUnit = JsonHandler.getMapFromInputStream(guidMapTmpFile);
                    unitId = (String) guidSystemArchiveUnit.get(archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT)
                        .get(SedaConstants.PREFIX_ID).asText());
                }

                archiveUnitProfileIdentifier =
                    archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT).get(SedaConstants.TAG_ARCHIVE_UNIT_PROFILE)
                        .asText();

                SchemaValidationStatus schemaValidationStatus;
                schemaValidationStatus = checkAUAgainstAUProfileSchema(itemStatus, infoNode, archiveUnit,
                    archiveUnitProfileIdentifier);

                if (isUpdateJsonMandatory) {
                    handlerIO.addOuputResult(UNIT_OUT_RANK, archiveUnit, true, false);
                }

                switch (schemaValidationStatus.getValidationStatus()) {
                    case VALID:
                        itemStatus.increment(StatusCode.OK);
                        return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID,
                            itemStatus);
                    case NOT_JSON_FILE:
                        itemStatus.setGlobalOutcomeDetailSubcode(
                            CheckArchiveUnitProfileSchemaStatus.INVALID_AU_PROFILE.name());
                        fillItemStatus(itemStatus, infoNode, archiveUnitProfileIdentifier, unitId,
                            schemaValidationStatus.getValidationMessage());
                        return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(),
                            itemStatus);
                    case NOT_AU_JSON_VALID:
                        itemStatus
                            .setGlobalOutcomeDetailSubcode(CheckArchiveUnitProfileSchemaStatus.INVALID_UNIT.name());
                        fillItemStatus(itemStatus, infoNode, archiveUnitProfileIdentifier, unitId,
                            schemaValidationStatus.getValidationMessage());
                        return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(),
                            itemStatus);
                    case NOT_FOUND:
                        itemStatus.setGlobalOutcomeDetailSubcode(
                            CheckArchiveUnitProfileSchemaStatus.PROFILE_NOT_FOUND.name());
                        fillItemStatus(itemStatus, infoNode, archiveUnitProfileIdentifier, unitId,
                            schemaValidationStatus.getValidationMessage());
                        return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(),
                            itemStatus);
                }
            } else {
                itemStatus.increment(StatusCode.OK);
            }

            String evdev = JsonHandler.unprettyPrint(infoNode);
            itemStatus.setEvDetailData(evdev);
            itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), evdev);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("File could not be converted into json", e);
            itemStatus.increment(StatusCode.KO);
            itemStatus.setGlobalOutcomeDetailSubcode(
                CheckArchiveUnitProfileSchemaStatus.INVALID_UNIT.name());
            infoNode.put(SedaConstants.EV_DET_TECH_DATA,
                SchemaValidationStatus.SchemaValidationStatusEnum.NOT_AU_JSON_VALID.name());
            itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
            return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, itemStatus);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
            IOException e) {
            LOGGER.error(WORKSPACE_SERVER_ERROR);
            itemStatus.increment(StatusCode.FATAL);
        } catch (ArchiveUnitProfileInactiveException | ArchiveUnitProfileEmptyControlSchemaException aupException) {
            itemStatus.setGlobalOutcomeDetailSubcode(aupException instanceof ArchiveUnitProfileInactiveException ?
                CheckArchiveUnitProfileSchemaStatus.INACTIVE_STATUS.name() :
                CheckArchiveUnitProfileSchemaStatus.EMPTY_CONTROL_SCHEMA.name());
            fillItemStatus(itemStatus, infoNode, archiveUnitProfileIdentifier, unitId, aupException.getMessage());
            return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(),
                itemStatus);
        }

        return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, itemStatus);
    }

    private void fillItemStatus(ItemStatus itemStatus, ObjectNode infoNode, String archiveUnitProfileIdentifier,
        String unitId, String schemaValidationStatusMessage) {
        infoNode.put(SedaConstants.TAG_ARCHIVE_UNIT, unitId);
        infoNode.put(SedaConstants.TAG_ARCHIVE_UNIT_PROFILE, archiveUnitProfileIdentifier);
        infoNode.put(SedaConstants.EV_DET_TECH_DATA, schemaValidationStatusMessage);
        itemStatus.increment(StatusCode.KO);
        itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
    }

    private void checkAUProfileStatusAndControlSchema(ArchiveUnitProfileModel archiveUnitProfile)
        throws ArchiveUnitProfileInactiveException, ArchiveUnitProfileEmptyControlSchemaException {

        if(ArchiveUnitProfileStatus.INACTIVE.equals(archiveUnitProfile.getStatus())) {
            String errMsg = "The declared manifest "+ archiveUnitProfile.getName() + " ArchiveUnitProfile status is not active";
            LOGGER.error(errMsg);
            throw new ArchiveUnitProfileInactiveException(errMsg);
        } else {
            if (controlSchemaIsEmpty(archiveUnitProfile)) {
                String errMsg = "The declared manifest "+ archiveUnitProfile.getName() + " ArchiveUnitProfile does not have a controlSchema";
                LOGGER.error(errMsg, archiveUnitProfile.getName());
                throw new ArchiveUnitProfileEmptyControlSchemaException(errMsg);
            }
        }
    }

    private static boolean controlSchemaIsEmpty(ArchiveUnitProfileModel archiveUnitProfile) {
        try {
            return archiveUnitProfile.getControlSchema() == null || JsonHandler.isEmpty(archiveUnitProfile.getControlSchema());
        } catch (InvalidParseOperationException e) {
            return false;
        }
    }

    private SchemaValidationStatus checkAUAgainstAUProfileSchema(ItemStatus itemStatus, ObjectNode infoNode,
        JsonNode archiveUnit, String archiveUnitProfileIdentifier)
        throws ProcessingException, ArchiveUnitProfileEmptyControlSchemaException, ArchiveUnitProfileInactiveException {
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(ArchiveUnitProfile.IDENTIFIER, archiveUnitProfileIdentifier));
            RequestResponse<ArchiveUnitProfileModel> response =
                adminClient.findArchiveUnitProfiles(select.getFinalSelect());
            ArchiveUnitProfileModel archiveUnitProfile = null;
            if (response.isOk() && ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().size() > 0) {

                archiveUnitProfile = ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().get(0);

                checkAUProfileStatusAndControlSchema(archiveUnitProfile);

                SchemaValidationUtils validator =
                    new SchemaValidationUtils(archiveUnitProfile.getControlSchema(), true);

                try {
                    SanityChecker.checkJsonAll(archiveUnit);
                    if (archiveUnitProfile.getFields() != null && archiveUnitProfile.getFields().size() > 0) {
                        handleExternalOntologies(archiveUnit, archiveUnitProfile.getFields(), adminClient, itemStatus,
                            validator);
                    }
                } catch (InvalidParseOperationException e) {
                    itemStatus.setGlobalOutcomeDetailSubcode(
                        CheckArchiveUnitSchemaActionPlugin.CheckUnitSchemaStatus.INVALID_UNIT.toString());
                    final String err = "Sanity Checker failed for Archive Unit: " + e.getMessage();
                    LOGGER.error(err);
                    throw new ArchiveUnitContainSpecialCharactersException(err);
                }

                return validator.validateInsertOrUpdateUnit(archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT));
            } else {
                return new SchemaValidationStatus("Archive Unit Profile not found",
                    SchemaValidationStatus.SchemaValidationStatusEnum.NOT_FOUND);
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(CAN_NOT_SEARCH_PROFILE, e);
            infoNode.put(SedaConstants.EV_DET_TECH_DATA, CAN_NOT_SEARCH_PROFILE + " " + archiveUnitProfileIdentifier);
            return new SchemaValidationStatus("File is not a valid json file",
                SchemaValidationStatus.SchemaValidationStatusEnum.NOT_JSON_FILE);
        } catch(ArchiveUnitProfileInactiveException | ArchiveUnitProfileEmptyControlSchemaException auExcep) {
            throw auExcep;

        } catch (Exception e) {
            LOGGER.error(UNKNOWN_TECHNICAL_EXCEPTION, e);
            itemStatus.increment(StatusCode.FATAL);
            throw new ProcessingException(e);
        }
    }

    private void handleExternalOntologies(JsonNode archiveUnit, List<String> fields,
        AdminManagementClient adminClient, ItemStatus itemStatus, SchemaValidationUtils validator)
        throws ProcessingException {
        Select selectOntologies = new Select();
        Map<String, OntologyModel> ontologyModelMap = new HashMap<String, OntologyModel>();
        Map<String, OntologyModel> finalOntologyModelMapToCheck = new HashMap<String, OntologyModel>();
        try {
            selectOntologies.setQuery(QueryHelper.and().add(QueryHelper.eq(OntologyModel.TAG_ORIGIN, "EXTERNAL"))
                .add(QueryHelper.in(OntologyModel.TAG_TYPE, OntologyType.DOUBLE.getType(),
                    OntologyType.BOOLEAN.getType(),
                    OntologyType.LONG.getType())));
            RequestResponse<OntologyModel> responseOntologies =
                adminClient.findOntologies(selectOntologies.getFinalSelect());
            if (responseOntologies.isOk() &&
                ((RequestResponseOK<OntologyModel>) responseOntologies).getResults().size() > 0) {
                List<OntologyModel> ontologyModelList =
                    ((RequestResponseOK<OntologyModel>) responseOntologies).getResults();
                ontologyModelMap =
                    ontologyModelList.stream().collect(Collectors.toMap(OntologyModel::getIdentifier,
                        c -> c));
            } else {
                // no external ontology, nothing to do
                return;
            }
            for (String fieldToBeChecked : fields) {
                if (ontologyModelMap.containsKey(fieldToBeChecked)) {
                    finalOntologyModelMapToCheck.put(fieldToBeChecked, ontologyModelMap.get(fieldToBeChecked));
                }
            }
            if (finalOntologyModelMapToCheck.size() > 0) {
                try {
                    // that means a transformation could be done so we need to process the full json
                    JsonNode originalArchiveUnit = archiveUnit.deepCopy();
                    validator.loopAndReplaceInJson(archiveUnit, finalOntologyModelMapToCheck);
                    final String unitBeforeUpdate = JsonHandler.prettyPrint(originalArchiveUnit);
                    final String unitAfterUpdate = JsonHandler.prettyPrint(archiveUnit);
                    List<String> diff = VitamDocument.getUnifiedDiff(unitAfterUpdate, unitBeforeUpdate);
                    if (diff.size() > 0) {
                        isUpdateJsonMandatory = true;
                    }
                } catch (Exception e) {
                    // archive unit could not be transformed, so the error would be thrown later by the schema
                    // validation verification
                    LOGGER.error("Archive unit could not be transformed", e);
                }
            }
        } catch (InvalidCreateOperationException | AdminManagementClientServerException |
            InvalidParseOperationException e) {
            LOGGER.error(UNKNOWN_TECHNICAL_EXCEPTION, e);
            itemStatus.increment(StatusCode.FATAL);
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }


    /**
     * Check unit schema status values
     */
    public enum CheckArchiveUnitProfileSchemaStatus {
        /**
         * Improper unit
         */
        INVALID_UNIT,
        /**
         * Improper archive unit profile
         */
        INVALID_AU_PROFILE,
        /**
         * Required field empty
         */
        EMPTY_REQUIRED_FIELD,
        /**
         * Archive unit profile not found
         */
        PROFILE_NOT_FOUND,
        /**
         * inactive archive unit profile status
         */
        INACTIVE_STATUS,
        /**
         * Empty control schema
         */
        EMPTY_CONTROL_SCHEMA
    }

}
