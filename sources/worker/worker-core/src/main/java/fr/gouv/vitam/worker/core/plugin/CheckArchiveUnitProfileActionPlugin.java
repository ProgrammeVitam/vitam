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
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.ArchiveUnitProfileEmptyControlSchemaException;
import fr.gouv.vitam.common.exception.ArchiveUnitProfileInactiveException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationStatus;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.processing.common.exception.ArchiveUnitContainSpecialCharactersException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import static fr.gouv.vitam.common.SedaConstants.EV_DET_TECH_DATA;
import static fr.gouv.vitam.common.SedaConstants.PREFIX_ID;
import static fr.gouv.vitam.common.SedaConstants.PREFIX_WORK;
import static fr.gouv.vitam.common.SedaConstants.TAG_ARCHIVE_UNIT;
import static fr.gouv.vitam.common.SedaConstants.TAG_ARCHIVE_UNIT_PROFILE;
import static fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum.NOT_AU_JSON_VALID;
import static fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum.NOT_FOUND;
import static fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum.NOT_JSON_FILE;
import static fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum.VALID;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus.INACTIVE;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventDetailData;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.UP;
import static fr.gouv.vitam.worker.core.plugin.CheckArchiveUnitProfileActionPlugin.CheckArchiveUnitProfileSchemaStatus.EMPTY_CONTROL_SCHEMA;
import static fr.gouv.vitam.worker.core.plugin.CheckArchiveUnitProfileActionPlugin.CheckArchiveUnitProfileSchemaStatus.INACTIVE_STATUS;
import static fr.gouv.vitam.worker.core.plugin.CheckArchiveUnitProfileActionPlugin.CheckArchiveUnitProfileSchemaStatus.INVALID_UNIT;
import static java.io.File.separator;

/**
 * CheckArchiveUnitProfile Plugin.<br>
 */

public class CheckArchiveUnitProfileActionPlugin extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckArchiveUnitProfileActionPlugin.class);

    private final AdminManagementClientFactory adminManagementClientFactory;

    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";
    private static final String UNKNOWN_TECHNICAL_EXCEPTION = "Unknown technical exception";
    private static final String CAN_NOT_SEARCH_PROFILE = "Can not search profile";
    private static final String CHECK_UNIT_PROFILE_TASK_ID = "CHECK_ARCHIVE_UNIT_PROFILE";

    private static final int GUID_MAP_RANK = 0;

    /**
     * Empty constructor CheckArchiveUnitProfileActionPlugin
     */
    public CheckArchiveUnitProfileActionPlugin() {
        this(AdminManagementClientFactory.getInstance());
    }

    /**
     * Empty constructor CheckArchiveUnitProfileActionPlugin
     *
     * @param adminManagementClientFactory
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
        String archiveUnitProfileIdentifier;
        String unitId = "";
        String url = ARCHIVE_UNIT_FOLDER + separator + objectName;
        try (InputStream archiveUnitInputStream = handlerIO.getInputStreamFromWorkspace(url)) {

            JsonNode archiveUnit = JsonHandler.getFromInputStream(archiveUnitInputStream);
            JsonNode archiveUnitTag = archiveUnit.get(TAG_ARCHIVE_UNIT);

            boolean hasArchiveUnitProfile = archiveUnitTag.has(TAG_ARCHIVE_UNIT_PROFILE);
            if (!hasArchiveUnitProfile) {
                String evdev = JsonHandler.unprettyPrint(infoNode);
                itemStatus.setEvDetailData(evdev)
                    .setMasterData(eventDetailData.name(), evdev)
                    .increment(OK);
                return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID)
                    .setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, itemStatus);
            }

            final File file = (File) handlerIO.getInput(GUID_MAP_RANK);
            String archiveUnitTagId = archiveUnitTag.get(PREFIX_ID).asText();
            if (file != null) {
                JsonNode fromFile = JsonHandler.getFromFile(file);
                unitId = fromFile.path(archiveUnitTagId).asText();
            }

            archiveUnitProfileIdentifier = archiveUnitTag.get(TAG_ARCHIVE_UNIT_PROFILE).asText();

            LOGGER.info("START - Validation of archive unit {} on {} - Time: {}", archiveUnitTagId, archiveUnitProfileIdentifier, Instant.now().toEpochMilli());
            SchemaValidationStatus schemaStatus = checkAUAgainstAUProfileSchema(
                itemStatus, infoNode, archiveUnit, archiveUnitProfileIdentifier
            );
            LOGGER.info("END - Validation of archive unit {} - Time: {}", archiveUnitTagId, Instant.now().toEpochMilli());

            if (VALID.equals(schemaStatus.getValidationStatus())) {
                String evdev = JsonHandler.unprettyPrint(infoNode);
                itemStatus.setEvDetailData(evdev)
                    .setMasterData(eventDetailData.name(), evdev)
                    .increment(OK);
                return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID)
                    .setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, itemStatus);
            }

            infoNode.put(TAG_ARCHIVE_UNIT, unitId)
                .put(TAG_ARCHIVE_UNIT_PROFILE, archiveUnitProfileIdentifier)
                .put(EV_DET_TECH_DATA, schemaStatus.getValidationMessage());

            return createItemStatusKo(itemStatus, schemaStatus.getValidationStatus().name(), infoNode);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("File could not be converted into json", e);
            infoNode.put(EV_DET_TECH_DATA, NOT_AU_JSON_VALID.name());
            return createItemStatusKo(itemStatus, INVALID_UNIT.name(), infoNode);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e) {
            LOGGER.error(WORKSPACE_SERVER_ERROR);
            itemStatus.increment(FATAL);
            return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, itemStatus);
        } catch (ArchiveUnitProfileInactiveException | ArchiveUnitProfileEmptyControlSchemaException e) {
            String detailSubcode = e instanceof ArchiveUnitProfileInactiveException
                ? INACTIVE_STATUS.name()
                : EMPTY_CONTROL_SCHEMA.name();

            infoNode.put(TAG_ARCHIVE_UNIT, unitId)
                .put(TAG_ARCHIVE_UNIT_PROFILE, e.getMessage())
                .put(EV_DET_TECH_DATA, e.getMessage());

            return createItemStatusKo(itemStatus, detailSubcode, infoNode);
        }
    }

    private ItemStatus createItemStatusKo(ItemStatus subItemStatus, String outcomeDetail, JsonNode evDetailData) {
        subItemStatus.increment(KO)
            .setEvDetailData(JsonHandler.unprettyPrint(evDetailData))
            .setGlobalOutcomeDetailSubcode(outcomeDetail);
        return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, subItemStatus);
    }

    private void checkAUProfileStatusAndControlSchema(ArchiveUnitProfileModel archiveUnitProfile)
        throws ArchiveUnitProfileInactiveException, ArchiveUnitProfileEmptyControlSchemaException {

        if (INACTIVE.equals(archiveUnitProfile.getStatus())) {
            String errMsg =
                "The declared manifest " + archiveUnitProfile.getName() + " ArchiveUnitProfile status is not active";
            LOGGER.error(errMsg);
            throw new ArchiveUnitProfileInactiveException(errMsg);
        } else if (controlSchemaIsEmpty(archiveUnitProfile)) {
            String errMsg = "The declared manifest " + archiveUnitProfile.getName() +
                " ArchiveUnitProfile does not have a controlSchema";
            LOGGER.error(errMsg, archiveUnitProfile.getName());
            throw new ArchiveUnitProfileEmptyControlSchemaException(errMsg);
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

    private SchemaValidationStatus checkAUAgainstAUProfileSchema(ItemStatus itemStatus, ObjectNode infoNode,
        JsonNode archiveUnit, String archiveUnitProfileIdentifier)
        throws ProcessingException, ArchiveUnitProfileEmptyControlSchemaException, ArchiveUnitProfileInactiveException {
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(ArchiveUnitProfile.IDENTIFIER, archiveUnitProfileIdentifier));
            RequestResponse<ArchiveUnitProfileModel> response =
                adminClient.findArchiveUnitProfiles(select.getFinalSelect());
            ArchiveUnitProfileModel archiveUnitProfile;
            if (response.isOk() && ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().size() > 0) {

                archiveUnitProfile = ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().get(0);

                checkAUProfileStatusAndControlSchema(archiveUnitProfile);

                SchemaValidationUtils validator =
                    new SchemaValidationUtils(archiveUnitProfile.getControlSchema(), true);

                try {
                    SanityChecker.checkJsonAll(archiveUnit);
                } catch (InvalidParseOperationException e) {
                    itemStatus.setGlobalOutcomeDetailSubcode(
                        CheckArchiveUnitSchemaActionPlugin.CheckUnitSchemaStatus.INVALID_UNIT.toString());
                    final String err = "Sanity Checker failed for Archive Unit: " + e.getMessage();
                    throw new ArchiveUnitContainSpecialCharactersException(err);
                }

                ObjectNode archiveUnitCopy = archiveUnit.get(TAG_ARCHIVE_UNIT).deepCopy();
                if (!archiveUnit.path(PREFIX_WORK).path(UP).isMissingNode()) {
                    archiveUnitCopy.set(UP, archiveUnit.path(PREFIX_WORK).path(UP));
                }

                return validator.validateInsertOrUpdateUnit(archiveUnitCopy);
            } else {
                return new SchemaValidationStatus("Archive Unit Profile not found", NOT_FOUND);
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(CAN_NOT_SEARCH_PROFILE, e);
            infoNode
                .put(EV_DET_TECH_DATA, String.format("%s %s", CAN_NOT_SEARCH_PROFILE, archiveUnitProfileIdentifier));
            return new SchemaValidationStatus("File is not a valid json file", NOT_JSON_FILE);
        } catch (ArchiveUnitProfileInactiveException | ArchiveUnitProfileEmptyControlSchemaException e) {
            throw e;
        } catch (Exception e) {
            itemStatus.increment(FATAL);
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
