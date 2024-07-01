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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.metadata.core.validation.UnitValidator;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.validation.MetadataValidationProvider;

import java.io.File;
import java.io.InputStream;
import java.time.Instant;

import static fr.gouv.vitam.common.SedaConstants.EV_DET_TECH_DATA;
import static fr.gouv.vitam.common.SedaConstants.PREFIX_ID;
import static fr.gouv.vitam.common.SedaConstants.PREFIX_WORK;
import static fr.gouv.vitam.common.SedaConstants.TAG_ARCHIVE_UNIT;
import static fr.gouv.vitam.common.SedaConstants.TAG_ARCHIVE_UNIT_PROFILE;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventDetailData;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.UP;
import static java.io.File.separator;

/**
 * CheckArchiveUnitProfile Plugin.<br>
 */

public class CheckArchiveUnitProfileActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckArchiveUnitProfileActionPlugin.class);

    private static final String CHECK_UNIT_PROFILE_TASK_ID = "CHECK_ARCHIVE_UNIT_PROFILE";

    static final String OUTCOME_DETAILS_NOT_AU_JSON_VALID = "NOT_AU_JSON_VALID";
    static final String OUTCOME_DETAILS_EMPTY_CONTROL_SCHEMA = "EMPTY_CONTROL_SCHEMA";
    static final String OUTCOME_DETAILS_NOT_FOUND = "NOT_FOUND";
    static final String OUTCOME_DETAILS_INACTIVE_STATUS = "INACTIVE_STATUS";

    private static final int GUID_MAP_RANK = 0;

    private final UnitValidator unitValidator;

    /**
     * Empty constructor CheckArchiveUnitProfileActionPlugin
     */
    public CheckArchiveUnitProfileActionPlugin() {
        this(MetadataValidationProvider.getInstance().getUnitValidator());
    }

    @VisibleForTesting
    public CheckArchiveUnitProfileActionPlugin(UnitValidator unitValidator) {
        this.unitValidator = unitValidator;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) throws ProcessingException {
        checkMandatoryParameters(params);
        final String objectName = params.getObjectName();
        final ItemStatus itemStatus = new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID);
        ObjectNode infoNode = JsonHandler.createObjectNode();
        String unitId = "";
        String url = ARCHIVE_UNIT_FOLDER + separator + objectName;
        try (InputStream archiveUnitInputStream = handlerIO.getInputStreamFromWorkspace(url)) {
            ObjectNode archiveUnit = (ObjectNode) JsonHandler.getFromInputStream(archiveUnitInputStream);
            JsonNode archiveUnitTag = archiveUnit.get(TAG_ARCHIVE_UNIT);

            boolean hasArchiveUnitProfile = archiveUnitTag.has(TAG_ARCHIVE_UNIT_PROFILE);
            if (!hasArchiveUnitProfile) {
                String evdev = JsonHandler.unprettyPrint(infoNode);
                itemStatus.setEvDetailData(evdev).setMasterData(eventDetailData.name(), evdev).increment(OK);
                return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(
                    CHECK_UNIT_PROFILE_TASK_ID,
                    itemStatus
                );
            }

            final File file = (File) handlerIO.getInput(GUID_MAP_RANK);
            String archiveUnitTagId = archiveUnitTag.get(PREFIX_ID).asText();
            if (file != null) {
                JsonNode fromFile = JsonHandler.getFromFile(file);
                unitId = fromFile.path(archiveUnitTagId).asText();
            }

            String archiveUnitProfileIdentifier = archiveUnitTag.get(TAG_ARCHIVE_UNIT_PROFILE).asText();

            try {
                PerformanceLogger.getInstance()
                    .log(
                        CHECK_UNIT_PROFILE_TASK_ID,
                        "Validation of archive against AUP.",
                        Instant.now().toEpochMilli()
                    );
                LOGGER.info(
                    "START - Validation of archive unit {} on {} - Time: {}",
                    archiveUnitTagId,
                    archiveUnitProfileIdentifier,
                    Instant.now().toEpochMilli()
                );
                checkAUAgainstAUProfileSchema(archiveUnit);
                PerformanceLogger.getInstance()
                    .log(
                        CHECK_UNIT_PROFILE_TASK_ID,
                        "Validation of archive against AUP.",
                        Instant.now().toEpochMilli()
                    );

                String evdev = JsonHandler.unprettyPrint(infoNode);
                itemStatus.setEvDetailData(evdev).setMasterData(eventDetailData.name(), evdev).increment(OK);
                return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(
                    CHECK_UNIT_PROFILE_TASK_ID,
                    itemStatus
                );
            } catch (MetadataValidationException e) {
                LOGGER.warn("Unit archive unit profile validation failed " + params.getObjectName(), e);

                String outcomeDetails;

                switch (e.getErrorCode()) {
                    case ARCHIVE_UNIT_PROFILE_SCHEMA_VALIDATION_FAILURE:
                        outcomeDetails = OUTCOME_DETAILS_NOT_AU_JSON_VALID;
                        break;
                    case EMPTY_ARCHIVE_UNIT_PROFILE_SCHEMA:
                        outcomeDetails = OUTCOME_DETAILS_EMPTY_CONTROL_SCHEMA;
                        break;
                    case UNKNOWN_ARCHIVE_UNIT_PROFILE:
                        outcomeDetails = OUTCOME_DETAILS_NOT_FOUND;
                        break;
                    case ARCHIVE_UNIT_PROFILE_SCHEMA_INACTIVE:
                        outcomeDetails = OUTCOME_DETAILS_INACTIVE_STATUS;
                        break;
                    case ONTOLOGY_VALIDATION_FAILURE:
                    case INVALID_UNIT_DATE_FORMAT:
                    case INVALID_START_END_DATE:
                    case SCHEMA_VALIDATION_FAILURE:
                    // Should never occur (Only AUP validation is done here)
                    case RULE_UPDATE_HOLD_END_DATE_BEFORE_START_DATE:
                    case RULE_UPDATE_UNEXPECTED_HOLD_END_DATE:
                    // Should never occur (unit rule update only)
                    default:
                        throw new IllegalStateException("Unexpected value: " + e.getErrorCode());
                }

                infoNode
                    .put(TAG_ARCHIVE_UNIT, unitId)
                    .put(TAG_ARCHIVE_UNIT_PROFILE, archiveUnitProfileIdentifier)
                    .put(EV_DET_TECH_DATA, e.getMessage());

                return createItemStatusKo(itemStatus, outcomeDetails, infoNode);
            }
        } catch (Exception e) {
            LOGGER.error(e);
            itemStatus.increment(FATAL);
            return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, itemStatus);
        }
    }

    private ItemStatus createItemStatusKo(ItemStatus subItemStatus, String outcomeDetail, JsonNode evDetailData) {
        subItemStatus
            .increment(KO)
            .setEvDetailData(JsonHandler.unprettyPrint(evDetailData))
            .setGlobalOutcomeDetailSubcode(outcomeDetail);
        return new ItemStatus(CHECK_UNIT_PROFILE_TASK_ID).setItemsStatus(CHECK_UNIT_PROFILE_TASK_ID, subItemStatus);
    }

    private void checkAUAgainstAUProfileSchema(ObjectNode archiveUnit) throws MetadataValidationException {
        // FIXME : What about "other" fields computed just before commit to db (eg. other graph fields)
        ObjectNode archiveUnitCopy = archiveUnit.get(TAG_ARCHIVE_UNIT).deepCopy();
        if (!archiveUnit.path(PREFIX_WORK).path(UP).isMissingNode()) {
            archiveUnitCopy.set(UP, archiveUnit.path(PREFIX_WORK).path(UP));
        }

        this.unitValidator.validateArchiveUnitProfile(archiveUnitCopy);
    }
}
