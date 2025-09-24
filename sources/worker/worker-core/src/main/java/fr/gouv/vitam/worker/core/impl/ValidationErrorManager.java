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

package fr.gouv.vitam.worker.core.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.model.unit.ObjectGroupInfoInternalModel;
import fr.gouv.vitam.common.model.validations.ValidationError;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * UnitsRulesCompute Plugin.<br>
 */

public class ValidationErrorManager {

    private static final String UNIT_CHECK_STEP = "STP_UNIT_CHECK_AND_PROCESS";
    private static final String OG_CHECK_STEP = "STP_OG_CHECK_AND_TRANSFORME";
    private static final String JSON_EXTENSION = ".json";
    private static final String PATH_SEPARATOR = "/";

    /**
     * Empty constructor UnitsRulesComputePlugin
     */
    public ValidationErrorManager() {}

    public void handleValidationErrors(
        WorkFlowExecutionContext executionContext,
        Step step,
        MultiValuedMap<String, ValidationError> validationErrorsByObjectName,
        HandlerIO handlerIO
    ) throws ProcessingException {
        // FIXME: Hack - For now, ValidationError processing is restricted to the "STP_UNIT_CHECK_AND_PROCESS" & "STP_OG_CHECK_AND_TRANSFORME" steps.
        //  Other user stories should contain proper handling for ValidationErrors using a plugable validation error
        //  manager, a special purpose "finally" action, or other cleaner designs.

        if (validationErrorsByObjectName.isEmpty() || !WorkFlowExecutionContext.COLLECT.equals(executionContext)) {
            return;
        }

        if (UNIT_CHECK_STEP.equals(step.getStepName())) {
            for (String objectName : validationErrorsByObjectName.keySet()) {
                Collection<ValidationError> validationErrors = validationErrorsByObjectName.get(objectName);
                String unitId = Strings.CI.removeEnd(objectName, JSON_EXTENSION);
                handleUnitValidationErrors(unitId, validationErrors, handlerIO);
            }
        }

        if (OG_CHECK_STEP.equals(step.getStepName())) {
            for (String objectName : validationErrorsByObjectName.keySet()) {
                Collection<ValidationError> validationErrors = validationErrorsByObjectName.get(objectName);
                String objectGroupId = Strings.CI.removeEnd(objectName, JSON_EXTENSION);
                handleObjectGroupValidationErrors(objectGroupId, validationErrors, handlerIO);
            }
        }
    }

    void handleUnitValidationErrors(String unitId, Collection<ValidationError> validationErrors, HandlerIO handler)
        throws ProcessingException {
        ObjectNode archiveUnitJson = getArchiveUnitJson(unitId, handler);
        setUnitValidationErrors(archiveUnitJson, validationErrors);
        saveArchiveUnit(unitId, handler, archiveUnitJson);
    }

    void handleObjectGroupValidationErrors(
        String objectGroupId,
        Collection<ValidationError> validationErrors,
        HandlerIO handler
    ) throws ProcessingException {
        ObjectNode objectGroupJson = getObjectGroupJson(objectGroupId, handler);
        setValidationErrors(objectGroupJson, validationErrors);
        saveObjectGroup(objectGroupId, handler, objectGroupJson);

        List<String> parentUnitIds = getParentUnitIds(objectGroupJson);
        for (String parentUnitId : parentUnitIds) {
            reportObjectGroupValidationErrorsInParentUnit(validationErrors, handler, parentUnitId);
        }
    }

    private ObjectNode getArchiveUnitJson(String unitId, HandlerIO handlerIO) throws ProcessingException {
        return getMetadataJsonFromWorkspace(handlerIO, IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER, unitId);
    }

    private ObjectNode getObjectGroupJson(String objectGroupId, HandlerIO handlerIO) throws ProcessingException {
        return getMetadataJsonFromWorkspace(handlerIO, IngestWorkflowConstants.OBJECT_GROUP_FOLDER, objectGroupId);
    }

    private static ObjectNode getMetadataJsonFromWorkspace(HandlerIO handlerIO, String path, String metadataId)
        throws ProcessingException {
        try (
            InputStream inputStream = handlerIO.getInputStreamFromWorkspace(
                WorkFlowExecutionContext.VITAM,
                path + PATH_SEPARATOR + metadataId + JSON_EXTENSION
            )
        ) {
            return (ObjectNode) JsonHandler.getFromInputStream(inputStream);
        } catch (
            ContentAddressableStorageServerException
            | ContentAddressableStorageNotFoundException
            | IOException
            | InvalidParseOperationException e
        ) {
            throw new ProcessingException("Could not load metadata file " + path + " from workspace", e);
        }
    }

    /**
     * Sets unit validation errors, but keeps object group validation errors.
     */
    private void setUnitValidationErrors(ObjectNode archiveUnit, Collection<ValidationError> unitValidationErrors) {
        ObjectNode archiveUnitJson = (ObjectNode) archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
        setValidationErrors(archiveUnitJson, unitValidationErrors);
    }

    private void setValidationErrors(ObjectNode metadataJson, Collection<ValidationError> validationErrors) {
        try {
            metadataJson.set(MetadataDocument.ERRORS, JsonHandler.toJsonNode(validationErrors));
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException("Cannot serialize validation errors", e);
        }
    }

    private void saveArchiveUnit(String unitId, HandlerIO handlerIO, ObjectNode archiveUnit)
        throws ProcessingException {
        saveMetadataToWorkspace(handlerIO, IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER, unitId, archiveUnit);
    }

    private void saveObjectGroup(String unitId, HandlerIO handlerIO, ObjectNode archiveUnit)
        throws ProcessingException {
        saveMetadataToWorkspace(handlerIO, IngestWorkflowConstants.OBJECT_GROUP_FOLDER, unitId, archiveUnit);
    }

    private static void saveMetadataToWorkspace(
        HandlerIO handlerIO,
        String folderName,
        String metadataId,
        ObjectNode archiveUnit
    ) throws ProcessingException {
        handlerIO.transferJsonToWorkspace(
            WorkFlowExecutionContext.VITAM,
            folderName,
            metadataId + JSON_EXTENSION,
            archiveUnit,
            false,
            false
        );
    }

    private static List<String> getParentUnitIds(ObjectNode objectGroupJson) {
        ArrayNode parentUnitNodes = (ArrayNode) objectGroupJson.get(ObjectGroup.UP);
        List<String> parentUnitIds = new ArrayList<>();
        for (JsonNode parentUnit : parentUnitNodes) {
            parentUnitIds.add(parentUnit.asText());
        }
        return parentUnitIds;
    }

    private void reportObjectGroupValidationErrorsInParentUnit(
        Collection<ValidationError> objectGroupValidationErrors,
        HandlerIO handler,
        String parentUnitId
    ) throws ProcessingException {
        ObjectNode parentUnit = getArchiveUnitJson(parentUnitId, handler);
        ObjectNode archiveUnitJson = (ObjectNode) parentUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
        try {
            ObjectGroupInfoInternalModel objectGroupInfoInternalModel = archiveUnitJson.has(Unit.OBJECT_GROUP_INFO)
                ? JsonHandler.getFromJsonNode(
                    archiveUnitJson.get(Unit.OBJECT_GROUP_INFO),
                    ObjectGroupInfoInternalModel.class
                )
                : new ObjectGroupInfoInternalModel();

            objectGroupInfoInternalModel.setErrors(new ArrayList<>(objectGroupValidationErrors));
            archiveUnitJson.set(Unit.OBJECT_GROUP_INFO, JsonHandler.toJsonNode(objectGroupInfoInternalModel));
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException("Cannot serialize/deserialize validation errors", e);
        }
        saveArchiveUnit(parentUnitId, handler, parentUnit);
    }
}
