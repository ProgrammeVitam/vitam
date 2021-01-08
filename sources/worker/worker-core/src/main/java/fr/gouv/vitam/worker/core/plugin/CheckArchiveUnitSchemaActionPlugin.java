/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.metadata.core.validation.OntologyValidator;
import fr.gouv.vitam.metadata.core.validation.UnitValidator;
import fr.gouv.vitam.processing.common.exception.ArchiveUnitContainSpecialCharactersException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.validation.MetadataValidationProvider;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CheckArchiveUnitSchema Plugin.<br>
 */

public class CheckArchiveUnitSchemaActionPlugin extends ActionHandler {
    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckArchiveUnitSchemaActionPlugin.class);

    private static final String CHECK_UNIT_SCHEMA_TASK_ID = "CHECK_UNIT_SCHEMA";

    private static final int UNIT_OUT_RANK = 0;

    private static final int ONTOLOGY_IN_RANK = 0;
    private static final String UNIT_SANITIZE = "UNIT_SANITIZE";

    private static final String ONTOLOGY_VALIDATION = "ONTOLOGY_VALIDATION";

    private static final String UNKNOWN_TECHNICAL_EXCEPTION = "Unknown technical exception";

    /**
     * Improper unit
     */
    static final String INVALID_UNIT = "INVALID_UNIT";
    /**
     * Rule's date in bad format
     */
    private static final String DATE_FORMAT = "DATE_FORMAT";
    /**
     * StartDate is after EndDate
     */
    static final String CONSISTENCY = "CONSISTENCY";

    private static final TypeReference<List<OntologyModel>> LIST_TYPE_REFERENCE = new TypeReference<List<OntologyModel>>() {};

    private final UnitValidator unitValidator;

    public CheckArchiveUnitSchemaActionPlugin() {
        this(MetadataValidationProvider.getInstance().getUnitValidator());
    }

    @VisibleForTesting
    CheckArchiveUnitSchemaActionPlugin(UnitValidator unitValidator) {
        this.unitValidator = unitValidator;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        final ItemStatus itemStatus = new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID);

        try {
            checkAUJsonAgainstSchema(handler, params, itemStatus);

            itemStatus.increment(StatusCode.OK);
            return new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID).setItemsStatus(CHECK_UNIT_SCHEMA_TASK_ID,
                itemStatus);

        } catch (MetadataValidationException e) {

            LOGGER.warn("Unit schema validation failed " + params.getObjectName(), e);

            switch (e.getErrorCode()) {

                case SCHEMA_VALIDATION_FAILURE: {

                    itemStatus.setGlobalOutcomeDetailSubcode(INVALID_UNIT);

                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
                    itemStatus.increment(StatusCode.KO);
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(object));
                    return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(),
                        itemStatus);
                }
                case ONTOLOGY_VALIDATION_FAILURE: {

                    itemStatus.setItemId(ONTOLOGY_VALIDATION);
                    itemStatus.increment(StatusCode.KO);
                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(object));
                    return new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID).setItemsStatus(CHECK_UNIT_SCHEMA_TASK_ID,
                        itemStatus);
                }
                case INVALID_UNIT_DATE_FORMAT: {

                    itemStatus.setGlobalOutcomeDetailSubcode(DATE_FORMAT);
                    itemStatus.increment(StatusCode.KO);
                    itemStatus.setEvDetailData(e.getMessage());
                    return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(),
                        itemStatus);
                }
                case INVALID_START_END_DATE: {

                    itemStatus.setGlobalOutcomeDetailSubcode(CONSISTENCY);
                    itemStatus.increment(StatusCode.KO);
                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(object));
                    return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(),
                        itemStatus);
                }
                case ARCHIVE_UNIT_PROFILE_SCHEMA_VALIDATION_FAILURE:
                case ARCHIVE_UNIT_PROFILE_SCHEMA_INACTIVE:
                case UNKNOWN_ARCHIVE_UNIT_PROFILE:
                case EMPTY_ARCHIVE_UNIT_PROFILE_SCHEMA:
                    // Should never occur (no AUP validation is done is this plugin)
                case RULE_UPDATE_HOLD_END_DATE_BEFORE_START_DATE:
                case RULE_UPDATE_UNEXPECTED_HOLD_END_DATE:
                    // Should never occur (unit rule update only)
                default:
                    throw new IllegalStateException("Unexpected value: " + e.getErrorCode());

            }
        } catch (final ArchiveUnitContainSpecialCharactersException e) {
            itemStatus.setItemId(UNIT_SANITIZE);
            itemStatus.increment(StatusCode.KO);
            final ObjectNode object = JsonHandler.createObjectNode();
            object.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
            itemStatus.setEvDetailData(JsonHandler.unprettyPrint(object));
            return new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID).setItemsStatus(CHECK_UNIT_SCHEMA_TASK_ID,
                itemStatus);
        } catch (final Exception e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID).setItemsStatus(itemStatus.getItemId(), itemStatus);
        }
    }

    private void checkAUJsonAgainstSchema(HandlerIO handlerIO, WorkerParameters params,
        ItemStatus itemStatus)
        throws ProcessingException, MetadataValidationException {
        final String objectName = params.getObjectName();

        // Load data
        ObjectNode archiveUnit;
        try (InputStream archiveUnitToJson =
            handlerIO.getInputStreamFromWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER +
                File.separator + objectName)) {

            archiveUnit = (ObjectNode) JsonHandler.getFromInputStream(archiveUnitToJson);

        } catch (InvalidParseOperationException |
            ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
            IOException e) {
            throw new ProcessingException(WORKSPACE_SERVER_ERROR, e);
        }

        // sanityChecker
        try {
            SanityChecker.checkJsonAll(archiveUnit);
        } catch (InvalidParseOperationException e) {
            itemStatus.setGlobalOutcomeDetailSubcode(INVALID_UNIT);
            final String err = "Sanity Checker failed for Archive Unit: " + e.getMessage();
            throw new ArchiveUnitContainSpecialCharactersException(err, e);
        }

        // Ontology verification and format conversion in needed
        Stopwatch ontologyTime = Stopwatch.createStarted();

        JsonNode archiveUnitJson = archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
        ObjectNode updatedArchiveUnitJson =
            checkFieldLengthAndForceFieldTypingUsingOntologies(handlerIO, archiveUnitJson, itemStatus);
        archiveUnit.set(SedaConstants.TAG_ARCHIVE_UNIT, updatedArchiveUnitJson);
        boolean isUpdateJsonMandatory = !archiveUnitJson.equals(updatedArchiveUnitJson);

        PerformanceLogger.getInstance().log("STP_UNIT_CHECK_AND_PROCESS", CHECK_UNIT_SCHEMA_TASK_ID, "validationOntology",
            ontologyTime.elapsed(TimeUnit.MILLISECONDS));

        handlerIO.addOutputResult(UNIT_OUT_RANK, archiveUnit, true, false);
        if (isUpdateJsonMandatory) {
            handlerIO.transferJsonToWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER, objectName, archiveUnit,
                false, false);
        }

        // Start / End Date validation + Internal schema validation
        Stopwatch validationJson = Stopwatch.createStarted();

        this.unitValidator.validateStartAndEndDates(updatedArchiveUnitJson);

        this.unitValidator.validateInternalSchema(updatedArchiveUnitJson);

        PerformanceLogger.getInstance().log("STP_UNIT_CHECK_AND_PROCESS", CHECK_UNIT_SCHEMA_TASK_ID, "validationJson",
            validationJson.elapsed(TimeUnit.MILLISECONDS));
    }

    private ObjectNode checkFieldLengthAndForceFieldTypingUsingOntologies(HandlerIO handlerIO, JsonNode
        archiveUnitJson,
        ItemStatus itemStatus) throws ProcessingException, MetadataValidationException {

        try {
            final File ontologyFile = (File) handlerIO.getInput(ONTOLOGY_IN_RANK);
            if (ontologyFile == null) {
                throw new IllegalStateException("Ontology file not found");
            }

            List<OntologyModel> ontologies = JsonHandler.getFromFileAsTypeReference(ontologyFile, LIST_TYPE_REFERENCE);
            OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologies);

            return ontologyValidator.verifyAndReplaceFields(archiveUnitJson);

        } catch (InvalidParseOperationException e) {
            itemStatus.increment(StatusCode.FATAL);
            throw new ProcessingException(UNKNOWN_TECHNICAL_EXCEPTION, e);
        }
    }
}
