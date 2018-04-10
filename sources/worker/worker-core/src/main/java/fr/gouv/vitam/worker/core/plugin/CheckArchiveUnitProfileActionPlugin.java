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
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
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
        try (InputStream archiveUnitToJson =
                     handlerIO.getInputStreamFromWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER +
                             File.separator + objectName)) {

            JsonNode archiveUnit = JsonHandler.getFromInputStream(archiveUnitToJson);
            if (archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT).has(SedaConstants.TAG_ARCHIVE_UNIT_PROFILE)) {
                archiveUnitProfileIdentifier =
                        archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT).get(SedaConstants.TAG_ARCHIVE_UNIT_PROFILE).asText();

                SchemaValidationStatus schemaValidationStatus;
                schemaValidationStatus = checkAUAgainstAUProfileSchema(itemStatus, infoNode, archiveUnit,
                        archiveUnitProfileIdentifier);

                switch (schemaValidationStatus.getValidationStatus()) {
                    case VALID:
                        itemStatus.increment(StatusCode.OK);
                        return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID,
                            itemStatus);
                    case NOT_JSON_FILE:
                        itemStatus.setGlobalOutcomeDetailSubcode(CheckArchiveUnitProfileSchemaStatus.INVALID_AU_PROFILE.name());
                        infoNode.put(SedaConstants.EV_DET_TECH_DATA, schemaValidationStatus.getValidationMessage());
                        itemStatus.increment(StatusCode.KO);
                        itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                        return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(), itemStatus);
                    case NOT_AU_JSON_VALID:
                        itemStatus.setGlobalOutcomeDetailSubcode(CheckArchiveUnitProfileSchemaStatus.INVALID_UNIT.name());
                        infoNode.put(SedaConstants.EV_DET_TECH_DATA, schemaValidationStatus.getValidationMessage());
                        itemStatus.increment(StatusCode.KO);
                        itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                        return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(), itemStatus);
                    case NOT_FOUND:
                        itemStatus.setGlobalOutcomeDetailSubcode(CheckArchiveUnitProfileSchemaStatus.PROFILE_NOT_FOUND.name());
                        infoNode.put(SedaConstants.EV_DET_TECH_DATA, schemaValidationStatus.getValidationMessage());
                        itemStatus.increment(StatusCode.KO);
                        itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                        return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(), itemStatus);
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
        }
        return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, itemStatus);
    }

    private SchemaValidationStatus checkAUAgainstAUProfileSchema(ItemStatus itemStatus, ObjectNode infoNode,
                                                                 JsonNode archiveUnit, String archiveUnitProfileIdentifier)
            throws ProcessingException {
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(ArchiveUnitProfile.IDENTIFIER, archiveUnitProfileIdentifier));
            RequestResponse<ArchiveUnitProfileModel> response = adminClient.findArchiveUnitProfiles(select.getFinalSelect());
            ArchiveUnitProfileModel archiveUnitProfile = null;
            if (response.isOk() && ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().size() > 0) {
                archiveUnitProfile = ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().get(0);

                SchemaValidationUtils validator = new SchemaValidationUtils(archiveUnitProfile.getControlSchema(), true);

                try {
                    SanityChecker.checkJsonAll(archiveUnit);
                } catch (InvalidParseOperationException e) {
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckArchiveUnitSchemaActionPlugin.CheckUnitSchemaStatus.INVALID_UNIT.toString());
                    final String err = "Sanity Checker failed for Archive Unit: "+e.getMessage();
                    LOGGER.error(err);
                    throw new ArchiveUnitContainSpecialCharactersException(err);
                }

                return validator.validateJson(archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT),
                        ARCHIVE_UNIT_PROFILE_SCHEMA);
            } else {
                return new SchemaValidationStatus("Archive Unit Profile not found",
                        SchemaValidationStatus.SchemaValidationStatusEnum.NOT_FOUND);
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(CAN_NOT_SEARCH_PROFILE, e);
            infoNode.put(SedaConstants.EV_DET_TECH_DATA, CAN_NOT_SEARCH_PROFILE + " " + archiveUnitProfileIdentifier);
            return new SchemaValidationStatus("File is not a valid json file",
                    SchemaValidationStatus.SchemaValidationStatusEnum.NOT_JSON_FILE);
        } catch (Exception e) {
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
        PROFILE_NOT_FOUND;
    }

}
