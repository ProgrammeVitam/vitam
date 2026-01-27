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

package fr.gouv.vitam.collect.internal.core.jsonl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalServerSideException;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataLine;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataSelector;
import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidJsonlFormatException;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.IteratorHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.common.model.unit.UpdateOperationModel;
import fr.gouv.vitam.common.security.SanityChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_TITLE;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_TITLE_;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.DESCRIPTION_LEVEL_API_FIELD;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MANAGEMENT_FIELD;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MANAGEMENT_UPDATE_OPERATION;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MANAGEMENT_UPDATE_OPERATION_API_PATH;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MANAGEMENT_UPDATE_OPERATION_ARCHIVE_UNIT_IDENTIFIER_KEY_METADATA_NAME_API_PATH;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MANAGEMENT_UPDATE_OPERATION_ARCHIVE_UNIT_IDENTIFIER_KEY_METADATA_VALUE_API_PATH;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MANAGEMENT_UPDATE_OPERATION_SYSTEM_ID_API_PATH;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.UPDATE_OPERATION_API_FIELD;
import static fr.gouv.vitam.collect.internal.core.jsonl.JsonlHelper.getInitialUploadPath;
import static fr.gouv.vitam.common.GlobalDataRest.X_ATTACHEMENT_ID;
import static fr.gouv.vitam.common.model.unit.RuleModel.END_DATE;

public class JsonlMetadataFileValidator {

    private static final Set<String> ALLOWED_RESERVED_FIELD_NAMES = Set.of(
        VitamFieldsHelper.management(),
        VitamFieldsHelper.history()
    );

    private static final TypeReference<ArchiveUnitModel> ARCHIVE_UNIT_MODEL_TYPE_REFERENCE = new TypeReference<>() {};

    public void validate(File jsonlMetadataFile, boolean isFirstUpload, boolean explicitAttachementMode)
        throws CollectInternalException {
        doSanityChecks(jsonlMetadataFile);

        try (
            JsonlErrorAccumulator errorAccumulator = new JsonlErrorAccumulator();
            InputStream inputStream = new FileInputStream(jsonlMetadataFile);
            CloseableIterator<CollectJsonMetadataLine> iterator = new JsonLineIterator<>(
                inputStream,
                CollectJsonMetadataLine.TYPE_REFERENCE
            )
        ) {
            for (int lineIndex = 0; iterator.hasNext(); lineIndex++) {
                CollectJsonMetadataLine entry = iterator.next();
                try {
                    // Validate line
                    validateMetadataIdentificationInformation(entry, lineIndex, isFirstUpload);
                    validateUnitContent(entry, lineIndex, isFirstUpload, explicitAttachementMode);
                } catch (CollectInternalInvalidRequestException e) {
                    errorAccumulator.report(e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new CollectInternalServerSideException(
                "An internal error occurred during jsonl metadata file validation",
                e
            );
        }
    }

    private static void doSanityChecks(File jsonlMetadataFile)
        throws CollectInternalInvalidRequestException, CollectInternalServerSideException {
        if (jsonlMetadataFile.length() == 0) {
            throw new CollectInternalInvalidRequestException("Empty jsonl file");
        }
        try {
            SanityChecker.checkJsonLines(jsonlMetadataFile);
        } catch (IOException e) {
            throw new CollectInternalServerSideException(
                "An internal error occurred during jsonl metadata file processing",
                e
            );
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            throw new CollectInternalInvalidRequestException(
                "Cannot validate json-lines request: " + e.getLocalizedMessage(),
                e
            );
        }
    }

    private void validateMetadataIdentificationInformation(
        CollectJsonMetadataLine entry,
        int lineIndex,
        boolean isFirstUpload
    ) throws CollectInternalInvalidRequestException {
        if (entry.getFile() == null && entry.getSelector() == null) {
            throw new CollectInternalInvalidRequestException(
                "Invalid entry at index: " + lineIndex + ". Missing metadata identification information."
            );
        }

        if (entry.getFile() != null && entry.getSelector() != null) {
            throw new CollectInternalInvalidRequestException(
                "Invalid entry at index: " +
                lineIndex +
                ". Fields '" +
                CollectJsonMetadataLine.FILE_FIELD +
                "' and '" +
                CollectJsonMetadataLine.SELECTOR_FIELD +
                "' are mutually exclusive."
            );
        }

        if (entry.getFile() != null) {
            validateFileIdentifier(entry.getFile(), lineIndex);
        }

        if (entry.getObjectFiles() != null) {
            validateObjectFiles(entry.getObjectFiles(), lineIndex, isFirstUpload);
        }

        if (entry.getSelector() != null) {
            validateSelector(entry.getSelector(), lineIndex, isFirstUpload);
        }
    }

    private void validateFileIdentifier(String fileValue, int lineIndex) throws CollectInternalInvalidRequestException {
        if (StringUtils.isBlank(fileValue)) {
            throw new CollectInternalInvalidRequestException(
                "Invalid entry at index: " + lineIndex + ". Empty unit file path '" + fileValue + "'"
            );
        }
        String path = FilenameUtils.normalize(fileValue);
        if (!FilenameUtils.equals(fileValue, path)) {
            throw new CollectInternalInvalidRequestException(
                "Invalid entry at index: " + lineIndex + ". Illegal unit file path '" + fileValue + "'"
            );
        }
    }

    private void validateObjectFiles(String objectFilesPath, int lineIndex, boolean isFirstUpload)
        throws CollectInternalInvalidRequestException {
        if (!isFirstUpload) {
            throw new CollectInternalInvalidRequestException(
                "Invalid entry at index: " +
                lineIndex +
                ". " +
                CollectJsonMetadataLine.OBJECT_FILES_FIELD +
                " field not allowed for update operations."
            );
        }
        if (StringUtils.isBlank(objectFilesPath)) {
            throw new CollectInternalInvalidRequestException(
                "Invalid entry at index: " + lineIndex + ". Empty ObjectFiles path."
            );
        }
        String normalizedPath = FilenameUtils.normalize(objectFilesPath);
        if (!FilenameUtils.equals(objectFilesPath, normalizedPath)) {
            throw new CollectInternalInvalidRequestException(
                "Invalid entry at index: " + lineIndex + ". Illegal ObjectFiles path '" + objectFilesPath + "'."
            );
        }
    }

    private void validateSelector(CollectJsonMetadataSelector selectorValue, int lineIndex, boolean isFirstUpload)
        throws CollectInternalInvalidRequestException {
        if (selectorValue.getEntries().isEmpty()) {
            throw new CollectInternalInvalidRequestException(
                "Invalid entry at index: " + lineIndex + ". Empty selectors"
            );
        }
        for (Map.Entry<String, ValueNode> entry : selectorValue.getEntries().entrySet()) {
            validateSelectorKey(entry.getKey(), lineIndex, isFirstUpload);
            validateSelectorValue(entry.getKey(), entry.getValue(), lineIndex);
            // TODO / Possible improvement - Check ontology type :
            //   - Exact match types are OK (keyword, long, double, bool, date).
            //   - Analyzed texts should not queried (otherwise, we won't be able to manage direct inserts, we'll be stuck forever using insert + update)
        }
    }

    private void validateSelectorKey(String key, int lineIndex, boolean isFirstUpload)
        throws CollectInternalInvalidRequestException {
        validateKeyNameFormat(key, lineIndex);

        if (isFirstUpload && !VitamFieldsHelper.uploadPath().equals(key)) {
            throw new CollectInternalInvalidRequestException(
                "Invalid selector key '" +
                key +
                "' for upload operation at index: " +
                lineIndex +
                ". Only " +
                CollectJsonMetadataLine.FILE_FIELD +
                " field Or " +
                CollectJsonMetadataLine.SELECTOR_FIELD +
                "." +
                VitamFieldsHelper.uploadPath() +
                " selector allowed for upload operations."
            );
        }
    }

    private static void validateKeyNameFormat(String key, int lineIndex) throws CollectInternalInvalidRequestException {
        // TODO: Field name checks should be unified
        if (StringUtils.isBlank(key)) {
            throw new CollectInternalInvalidRequestException(
                "Invalid field name: '" + key + "'  at index: " + lineIndex
            );
        }
        String[] fieldNames = StringUtils.split(key, '.');
        for (String fieldName : fieldNames) {
            if (
                fieldName.isEmpty() ||
                StringUtils.containsWhitespace(fieldName) ||
                fieldName.startsWith("_") ||
                fieldName.startsWith("$")
            ) {
                throw new CollectInternalInvalidRequestException(
                    "Invalid field name: '" + key + "'  at index: " + lineIndex
                );
            }
        }
    }

    private void validateSelectorValue(String key, ValueNode value, int lineIndex)
        throws CollectInternalInvalidRequestException {
        switch (value.getNodeType()) {
            case BOOLEAN:
            case NUMBER:
            case STRING:
                // OK
                break;
            case ARRAY:
                throw new CollectInternalInvalidRequestException(
                    "Invalid unit metadata at index: " +
                    lineIndex +
                    ". Invalid selector value for '" +
                    key +
                    "'." +
                    ". Arrays are not supported"
                );
            case NULL:
                throw new CollectInternalInvalidRequestException(
                    "Invalid unit metadata at index: " +
                    lineIndex +
                    ". Invalid selector value for '" +
                    key +
                    "'." +
                    ". Null value"
                );
            case BINARY:
            case MISSING:
            case OBJECT:
            case POJO:
            default:
                throw new IllegalStateException("Unexpected value: " + value.getNodeType());
        }
    }

    private void validateUnitContent(
        CollectJsonMetadataLine entry,
        int lineIndex,
        boolean isFirstUpload,
        boolean explicitAttachementMode
    ) throws CollectInternalInvalidRequestException {
        checkNonEmptyUnit(entry.getUnitContent(), lineIndex);
        validateReservedUnitFieldNames(entry.getUnitContent(), lineIndex);
        validateUnitFormat(entry, lineIndex, isFirstUpload, explicitAttachementMode);
    }

    private void checkNonEmptyUnit(ObjectNode unitContent, int lineIndex)
        throws CollectInternalInvalidRequestException {
        if (unitContent == null || unitContent.isEmpty()) {
            throw new CollectInternalInvalidRequestException(
                "Invalid unit metadata at index: " + lineIndex + ". Empty metadata content"
            );
        }
    }

    private static void validateReservedUnitFieldNames(ObjectNode unitContent, int lineIndex)
        throws CollectInternalInvalidRequestException {
        Iterator<String> it = unitContent.fieldNames();
        while (it.hasNext()) {
            String fieldName = it.next();
            if (StringUtils.containsWhitespace(fieldName) || fieldName.startsWith("$") || fieldName.startsWith("_")) {
                throw new CollectInternalInvalidRequestException(
                    "Invalid unit metadata at index: " + lineIndex + ". Illegal field name '" + fieldName + "'"
                );
            }
            if (fieldName.startsWith("#") && !ALLOWED_RESERVED_FIELD_NAMES.contains(fieldName)) {
                throw new CollectInternalInvalidRequestException(
                    "Invalid unit metadata at index: " + lineIndex + ". Forbidden field name '" + fieldName + "'"
                );
            }
            if (fieldName.contains(".")) {
                throw new CollectInternalInvalidRequestException(
                    "Invalid unit metadata at index: " +
                    lineIndex +
                    ". Field name must be root-level field: '" +
                    fieldName +
                    "'"
                );
            }
        }
    }

    private static void validateUnitFormat(
        CollectJsonMetadataLine entry,
        int lineIndex,
        boolean isFirstUpload,
        boolean explicitAttachementMode
    ) throws CollectInternalInvalidRequestException {
        ArchiveUnitModel archiveUnitModel;
        try {
            // Use strict deserializer to validate unit content structure & format
            archiveUnitModel = JsonHandler.getFromStrictJsonNode(
                entry.getUnitContent(),
                ARCHIVE_UNIT_MODEL_TYPE_REFERENCE
            );
        } catch (InvalidParseOperationException e) {
            throw new CollectInternalInvalidRequestException(
                "Invalid unit metadata at index: " +
                lineIndex +
                ". Unit format validation failed: " +
                e.getLocalizedMessage(),
                e
            );
        }

        validateUnitRulesEndDates(archiveUnitModel, lineIndex);

        validateUpdateOperation(entry, archiveUnitModel, lineIndex, isFirstUpload, explicitAttachementMode);
    }

    private static void validateUnitRulesEndDates(ArchiveUnitModel archiveUnitModel, int lineIndex)
        throws CollectInternalInvalidRequestException {
        if (archiveUnitModel.getManagement() != null) {
            validateRulesEndDates(archiveUnitModel.getManagement().getDissemination(), "Dissemination", lineIndex);
            validateRulesEndDates(archiveUnitModel.getManagement().getStorage(), "Storage", lineIndex);
            validateRulesEndDates(archiveUnitModel.getManagement().getAppraisal(), "Appraisal", lineIndex);
            validateRulesEndDates(archiveUnitModel.getManagement().getAccess(), "Access", lineIndex);
            validateRulesEndDates(archiveUnitModel.getManagement().getReuse(), "Reuse", lineIndex);
            validateRulesEndDates(archiveUnitModel.getManagement().getClassification(), "Classification", lineIndex);
            validateRulesEndDates(archiveUnitModel.getManagement().getHold(), "Hold", lineIndex);
        }
    }

    private static void validateRulesEndDates(RuleCategoryModel ruleCategoryModel, String ruleCategory, int lineIndex)
        throws CollectInternalInvalidRequestException {
        if (ruleCategoryModel == null || CollectionUtils.isEmpty(ruleCategoryModel.getRules())) {
            return;
        }
        for (RuleModel rule : ruleCategoryModel.getRules()) {
            if (rule.getEndDate() != null) {
                throw new CollectInternalInvalidRequestException(
                    "Invalid unit metadata at index: " +
                    lineIndex +
                    ". Unit " +
                    ruleCategory +
                    " Rules cannot contain '" +
                    END_DATE +
                    "' field."
                );
            }
        }
    }

    private static void validateUpdateOperation(
        CollectJsonMetadataLine entry,
        ArchiveUnitModel archiveUnitModel,
        int lineIndex,
        boolean isFirstUpload,
        boolean explicitAttachementMode
    ) throws CollectInternalInvalidRequestException {
        if (archiveUnitModel.getManagement() == null || archiveUnitModel.getManagement().getUpdateOperation() == null) {
            return;
        }

        if (explicitAttachementMode) {
            throw new CollectInternalInvalidRequestException(
                "Cannot set '" +
                MANAGEMENT_UPDATE_OPERATION_API_PATH +
                ".*' fields when explicit HTTP header '" +
                X_ATTACHEMENT_ID +
                "' is set"
            );
        }

        if (!isFirstUpload) {
            throw new CollectInternalInvalidRequestException(
                String.format(
                    "Invalid unit metadata at index: %d: '%s.*' fields not supported in update APIs.",
                    lineIndex,
                    MANAGEMENT_UPDATE_OPERATION
                )
            );
        }

        restrictUploadOperationToTopLevelUnits(entry, lineIndex);

        UpdateOperationModel updateOperation = archiveUnitModel.getManagement().getUpdateOperation();

        validateUpdateOperationFields(updateOperation, lineIndex);

        checkIncompatibleFieldsWithUpdateOperationFields(entry.getUnitContent(), lineIndex);
    }

    private static void restrictUploadOperationToTopLevelUnits(CollectJsonMetadataLine entry, int lineIndex)
        throws CollectInvalidJsonlFormatException {
        String uploadPath = getInitialUploadPath(entry);
        boolean isTopLevelFolder = !uploadPath.contains(File.separator);
        if (!isTopLevelFolder) {
            throw new CollectInvalidJsonlFormatException(
                "Invalid unit metadata at index: " +
                lineIndex +
                ". Only top-level (root) units can have '" +
                MANAGEMENT_UPDATE_OPERATION_API_PATH +
                ".*' fields."
            );
        }
    }

    private static void validateUpdateOperationFields(UpdateOperationModel updateOperation, int lineIndex)
        throws CollectInvalidJsonlFormatException {
        String systemId = updateOperation.getSystemId();

        String metadataName = updateOperation.getArchiveUnitIdentifierKey() != null
            ? updateOperation.getArchiveUnitIdentifierKey().getMetadataName()
            : null;
        String metadataValue = updateOperation.getArchiveUnitIdentifierKey() != null
            ? updateOperation.getArchiveUnitIdentifierKey().getMetadataValue()
            : null;

        if (systemId == null && metadataName == null && metadataValue == null) {
            throw new CollectInvalidJsonlFormatException(
                "Invalid unit metadata at index: " +
                lineIndex +
                ". Missing or empty '" +
                MANAGEMENT_UPDATE_OPERATION_API_PATH +
                "' field."
            );
        }

        if (metadataName != null && metadataValue == null) {
            throw new CollectInvalidJsonlFormatException(
                "Invalid unit metadata at index: " +
                lineIndex +
                ". Missing or empty '" +
                MANAGEMENT_UPDATE_OPERATION_ARCHIVE_UNIT_IDENTIFIER_KEY_METADATA_VALUE_API_PATH +
                "' field."
            );
        }

        if (metadataName == null && metadataValue != null) {
            throw new CollectInvalidJsonlFormatException(
                "Invalid unit metadata at index: " +
                lineIndex +
                ". Missing or empty '" +
                MANAGEMENT_UPDATE_OPERATION_ARCHIVE_UNIT_IDENTIFIER_KEY_METADATA_NAME_API_PATH +
                "' field."
            );
        }

        if (systemId != null && metadataName != null) {
            throw new CollectInvalidJsonlFormatException(
                "Invalid unit metadata at index: " +
                lineIndex +
                ". Both '" +
                MANAGEMENT_UPDATE_OPERATION_SYSTEM_ID_API_PATH +
                "' and '" +
                MANAGEMENT_UPDATE_OPERATION_ARCHIVE_UNIT_IDENTIFIER_KEY_METADATA_NAME_API_PATH +
                "' headers are set."
            );
        }
    }

    private static void checkIncompatibleFieldsWithUpdateOperationFields(ObjectNode unitContent, int lineIndex)
        throws CollectInvalidJsonlFormatException {
        for (String fieldName : IteratorUtils.asIterable(unitContent.fieldNames())) {
            switch (fieldName) {
                case API_FIELD_TITLE:
                case API_FIELD_TITLE_:
                case DESCRIPTION_LEVEL_API_FIELD:
                    // Skip Content.Title[.*] & Content.DescriptionLevel (exceptionally accepted as they are required in Seda ArchiveUnit declaration)
                    break;
                case MANAGEMENT_FIELD:
                    // When #management.UpdateOperation is set, no other #management.* field is allowed
                    ObjectNode managementNode = (ObjectNode) unitContent.get(MANAGEMENT_FIELD);
                    Optional<String> anyOtherManagementField = IteratorHelper.toStream(managementNode.fieldNames())
                        .filter(mgtFieldName -> !mgtFieldName.equals(UPDATE_OPERATION_API_FIELD))
                        .findFirst();

                    if (anyOtherManagementField.isPresent()) {
                        throw new CollectInvalidJsonlFormatException(
                            "Invalid unit metadata at index: " +
                            lineIndex +
                            ". Cannot set other metadata field '" +
                            MANAGEMENT_FIELD +
                            SEPARATOR +
                            anyOtherManagementField.get() +
                            "' when '" +
                            MANAGEMENT_UPDATE_OPERATION_API_PATH +
                            "' header is defined."
                        );
                    }
                    break;
                default:
                    throw new CollectInvalidJsonlFormatException(
                        "Invalid unit metadata at index: " +
                        lineIndex +
                        ". Cannot set other metadata field '" +
                        fieldName +
                        "' when '" +
                        MANAGEMENT_UPDATE_OPERATION_API_PATH +
                        "' header is defined."
                    );
            }
        }
    }
}
