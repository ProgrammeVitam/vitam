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
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationErrorCode;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.processing.common.exception.MetaDataContainSpecialCharactersException;
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
import java.util.concurrent.TimeUnit;

/**
 * CheckObjectGroupSchemaAction Plugin.<br>
 */

public class CheckObjectGroupSchemaActionPlugin extends ActionHandler {

    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckObjectGroupSchemaActionPlugin.class);

    private static final String CHECK_OG_SCHEMA_TASK_ID = "CHECK_OBJECT_GROUP_SCHEMA";

    private static final int OG_OUT_RANK = 0;

    private static final String OBJECT_GROUP_SANITIZE = "OBJECT_GROUP_SANITIZE";

    private static final String ONTOLOGY_VALIDATION = "ONTOLOGY_VALIDATION";

    /**
     * Improper unit
     */
    static final String INVALID_OG = "INVALID_OBJECT_GROUP";
    /**
     * Rule's date in bad format
     */
    private static final String DATE_FORMAT = "DATE_FORMAT";
    /**
     * StartDate is after EndDate
     */
    static final String CONSISTENCY = "CONSISTENCY";

    private final MetadataValidationProvider metadataValidationProvider;

    public CheckObjectGroupSchemaActionPlugin() {
        this(MetadataValidationProvider.getInstance());
    }

    @VisibleForTesting
    CheckObjectGroupSchemaActionPlugin(MetadataValidationProvider metadataValidationProvider) {
        this.metadataValidationProvider = metadataValidationProvider;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        final ItemStatus itemStatus = new ItemStatus(CHECK_OG_SCHEMA_TASK_ID);

        try {
            checkObjectGroupJsonAgainstSchema(handler, params, itemStatus);

            itemStatus.increment(StatusCode.OK);
            return new ItemStatus(CHECK_OG_SCHEMA_TASK_ID).setItemsStatus(CHECK_OG_SCHEMA_TASK_ID, itemStatus);
        } catch (MetadataValidationException e) {
            LOGGER.warn("Object group schema validation failed " + params.getObjectName(), e);

            if (e.getErrorCode().equals(MetadataValidationErrorCode.ONTOLOGY_VALIDATION_FAILURE)) {
                itemStatus.setItemId(ONTOLOGY_VALIDATION);
                itemStatus.increment(StatusCode.KO);
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
                itemStatus.setEvDetailData(JsonHandler.unprettyPrint(object));
                return new ItemStatus(CHECK_OG_SCHEMA_TASK_ID).setItemsStatus(CHECK_OG_SCHEMA_TASK_ID, itemStatus);
            } else {
                throw new IllegalStateException("Unexpected value: " + e.getErrorCode());
            }
        } catch (final MetaDataContainSpecialCharactersException e) {
            LOGGER.error(e);
            itemStatus.setItemId(OBJECT_GROUP_SANITIZE);
            itemStatus.increment(StatusCode.KO);
            final ObjectNode object = JsonHandler.createObjectNode();
            object.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
            itemStatus.setEvDetailData(JsonHandler.unprettyPrint(object));
            return new ItemStatus(CHECK_OG_SCHEMA_TASK_ID).setItemsStatus(CHECK_OG_SCHEMA_TASK_ID, itemStatus);
        } catch (final Exception e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(CHECK_OG_SCHEMA_TASK_ID).setItemsStatus(itemStatus.getItemId(), itemStatus);
        }
    }

    private void checkObjectGroupJsonAgainstSchema(HandlerIO handlerIO, WorkerParameters params, ItemStatus itemStatus)
        throws ProcessingException, MetadataValidationException {
        final String objectName = params.getObjectName();

        // Load data
        JsonNode objectGroupJson;
        try (
            InputStream archiveOgToJson = handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + objectName
            )
        ) {
            objectGroupJson = JsonHandler.getFromInputStream(archiveOgToJson);
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
            SanityChecker.checkJsonAll(objectGroupJson);
        } catch (InvalidParseOperationException e) {
            itemStatus.setGlobalOutcomeDetailSubcode(INVALID_OG);
            final String err = "Sanity Checker failed for Object Group: " + e.getMessage();
            throw new MetaDataContainSpecialCharactersException(err, e);
        }

        // Ontology verification and format conversion in needed
        Stopwatch ontologyTime = Stopwatch.createStarted();

        ObjectNode updatedOgJson = metadataValidationProvider
            .getObjectGroupOntologyValidator()
            .verifyAndReplaceFields(objectGroupJson);
        boolean isUpdateJsonMandatory = !objectGroupJson.equals(updatedOgJson);

        PerformanceLogger.getInstance()
            .log(
                "STP_OBJECT_GROUP_CHECK_AND_PROCESS",
                CHECK_OG_SCHEMA_TASK_ID,
                "validationOntology",
                ontologyTime.elapsed(TimeUnit.MILLISECONDS)
            );
        if (isUpdateJsonMandatory) {
            handlerIO.transferJsonToWorkspace(
                IngestWorkflowConstants.OBJECT_GROUP_FOLDER,
                objectName,
                updatedOgJson,
                false,
                false
            );
        }
    }
}
