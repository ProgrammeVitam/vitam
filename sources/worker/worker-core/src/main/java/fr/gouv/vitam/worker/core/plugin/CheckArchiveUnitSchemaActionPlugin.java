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
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ContractsDetailsModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.validations.ValidationError;
import fr.gouv.vitam.common.model.validations.ValidationErrorHelper;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.processing.common.exception.MetaDataContainSpecialCharactersException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.signingInformation.IngestContractChecker;
import fr.gouv.vitam.worker.core.plugin.signingInformation.exception.SigningInformationException;
import fr.gouv.vitam.worker.core.validation.MetadataValidationProvider;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * CheckArchiveUnitSchema Plugin.<br>
 */

public class CheckArchiveUnitSchemaActionPlugin extends ActionHandler {

    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckArchiveUnitSchemaActionPlugin.class);

    private static final String CHECK_UNIT_SCHEMA_TASK_ID = "CHECK_UNIT_SCHEMA";

    private static final int UNIT_OUT_RANK = 0;
    private static final int REFERENTIAL_INGEST_CONTRACT_IN_RANK = 1;

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

    private final MetadataValidationProvider metadataValidationProvider;

    public CheckArchiveUnitSchemaActionPlugin() {
        this(MetadataValidationProvider.getInstance());
    }

    @VisibleForTesting
    CheckArchiveUnitSchemaActionPlugin(MetadataValidationProvider metadataValidationProvider) {
        this.metadataValidationProvider = metadataValidationProvider;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        try {
            ObjectNode archiveUnit = loadArchiveUnit(handler, params);
            checkAUJsonAgainstSchema(handler, params, archiveUnit);

            Stopwatch checkUnitTime = Stopwatch.createStarted();
            IngestContractModel contractModel = loadIngestContractFromWorkspace(handler);
            IngestContractChecker checkUnitActionHandler = new IngestContractChecker(archiveUnit, contractModel);
            checkUnitActionHandler.check();
            PerformanceLogger.getInstance()
                .log(
                    "STP_UNIT_CHECK_AND_PROCESS",
                    CHECK_UNIT_SCHEMA_TASK_ID,
                    "checkUnit",
                    checkUnitTime.elapsed(TimeUnit.MILLISECONDS)
                );

            final ItemStatus itemStatus = new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID);
            itemStatus.increment(StatusCode.OK);
            return new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID).setItemsStatus(CHECK_UNIT_SCHEMA_TASK_ID, itemStatus);
        } catch (MetadataValidationException e) {
            LOGGER.warn("Unit schema validation failed " + params.getObjectName(), e);

            String outcomeDetail =
                switch (e.getErrorCode()) {
                    case SCHEMA_VALIDATION_FAILURE, ONTOLOGY_VALIDATION_FAILURE -> INVALID_UNIT;
                    case INVALID_UNIT_DATE_FORMAT -> DATE_FORMAT;
                    case INVALID_START_END_DATE -> CONSISTENCY;
                    case ARCHIVE_UNIT_PROFILE_SCHEMA_VALIDATION_FAILURE,
                        ARCHIVE_UNIT_PROFILE_SCHEMA_INACTIVE,
                        UNKNOWN_ARCHIVE_UNIT_PROFILE,
                        EMPTY_ARCHIVE_UNIT_PROFILE_SCHEMA -> throw new IllegalStateException(
                        "Should never occur (no AUP validation is done is this plugin)"
                    );
                    case RULE_UPDATE_HOLD_END_DATE_BEFORE_START_DATE,
                        RULE_UPDATE_UNEXPECTED_HOLD_END_DATE -> throw new IllegalStateException(
                        "Should never occur (unit rule update only)"
                    );
                };
            return handleValidationError(e, outcomeDetail);
        } catch (final MetaDataContainSpecialCharactersException e) {
            LOGGER.warn(e);
            return handleValidationError(e, INVALID_UNIT);
        } catch (SigningInformationException e) {
            LOGGER.warn(e);
            return handleValidationError(e, e.getErrorCode());
        } catch (final Exception e) {
            LOGGER.error(e);
            final ItemStatus itemStatus = new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID).setItemsStatus(itemStatus.getItemId(), itemStatus);
        }
    }

    private static ItemStatus handleValidationError(Exception e, String outcomeDetail) {
        ItemStatus itemStatus = new ItemStatus(CHECK_UNIT_SCHEMA_TASK_ID);
        itemStatus.setGlobalOutcomeDetailSubcode(outcomeDetail);

        final ObjectNode object = JsonHandler.createObjectNode();
        object.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
        String evDetailData = JsonHandler.unprettyPrint(object);
        itemStatus.setEvDetailData(evDetailData);

        ValidationError validationError = ValidationErrorHelper.createValidationError(
            CHECK_UNIT_SCHEMA_TASK_ID,
            outcomeDetail,
            evDetailData
        );
        itemStatus.increment(StatusCode.KO, validationError);
        return new ItemStatus(itemStatus.getItemId()).setItemsStatus(itemStatus.getItemId(), itemStatus);
    }

    private void checkAUJsonAgainstSchema(HandlerIO handlerIO, WorkerParameters params, ObjectNode archiveUnit)
        throws ProcessingException, MetadataValidationException {
        final String objectName = params.getObjectName();

        // Ontology verification and format conversion in needed
        Stopwatch ontologyTime = Stopwatch.createStarted();

        JsonNode archiveUnitJson = archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
        ObjectNode updatedArchiveUnitJson = metadataValidationProvider
            .getUnitOntologyValidator()
            .verifyAndReplaceFields(archiveUnitJson);
        archiveUnit.set(SedaConstants.TAG_ARCHIVE_UNIT, updatedArchiveUnitJson);
        boolean isUpdateJsonMandatory = !archiveUnitJson.equals(updatedArchiveUnitJson);

        PerformanceLogger.getInstance()
            .log(
                "STP_UNIT_CHECK_AND_PROCESS",
                CHECK_UNIT_SCHEMA_TASK_ID,
                "validationOntology",
                ontologyTime.elapsed(TimeUnit.MILLISECONDS)
            );

        handlerIO.addOutputResult(UNIT_OUT_RANK, archiveUnit, true, false);
        if (isUpdateJsonMandatory) {
            handlerIO.transferJsonToWorkspace(
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER,
                objectName,
                archiveUnit,
                false,
                false
            );
        }

        // Start / End Date validation + Internal schema validation
        Stopwatch validationJson = Stopwatch.createStarted();

        this.metadataValidationProvider.getUnitValidator().validateStartAndEndDates(updatedArchiveUnitJson);

        this.metadataValidationProvider.getUnitValidator().validateInternalSchema(updatedArchiveUnitJson);

        PerformanceLogger.getInstance()
            .log(
                "STP_UNIT_CHECK_AND_PROCESS",
                CHECK_UNIT_SCHEMA_TASK_ID,
                "validationJson",
                validationJson.elapsed(TimeUnit.MILLISECONDS)
            );
    }

    private ObjectNode loadArchiveUnit(HandlerIO handlerIO, WorkerParameters params) throws ProcessingException {
        ObjectNode archiveUnit;
        final String objectName = params.getObjectName();
        try (
            InputStream archiveUnitToJson = handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + objectName
            )
        ) {
            archiveUnit = (ObjectNode) JsonHandler.getFromInputStream(archiveUnitToJson);
        } catch (
            InvalidParseOperationException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageServerException
            | IOException e
        ) {
            throw new ProcessingException(WORKSPACE_SERVER_ERROR, e);
        }

        // sanityChecker
        try {
            SanityChecker.checkJsonAll(archiveUnit);
        } catch (InvalidParseOperationException e) {
            final String err = "Sanity Checker failed for Archive Unit: " + e.getMessage();
            throw new MetaDataContainSpecialCharactersException(err, e);
        }

        return archiveUnit;
    }

    private IngestContractModel loadIngestContractFromWorkspace(HandlerIO handlerIO)
        throws InvalidParseOperationException {
        ContractsDetailsModel contractsDetailsModel = JsonHandler.getFromFile(
            (File) handlerIO.getInput(REFERENTIAL_INGEST_CONTRACT_IN_RANK),
            ContractsDetailsModel.class
        );
        return contractsDetailsModel.getIngestContractModel();
    }
}
