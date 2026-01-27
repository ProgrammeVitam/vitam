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
import fr.gouv.vitam.collect.common.exception.CollectInternalServerSideException;
import fr.gouv.vitam.collect.common.exception.CollectInternalSingleErrorsDetailException;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorMessagesEnum;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorParamEnum;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataLine;
import fr.gouv.vitam.collect.internal.core.common.CollectJsonMetadataSelector;
import fr.gouv.vitam.collect.internal.core.helpers.CollectErrorDetailHelper;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.IteratorHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.error.VitamErrorDetails;
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
import java.util.HashMap;
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
                } catch (CollectInternalSingleErrorsDetailException e) {
                    VitamErrorDetails vitamErrorDetails = e.getErrorsDetailsList().getFirst(); // Single error
                    CollectErrorMessagesEnum key = CollectErrorMessagesEnum.valueOf(vitamErrorDetails.getKey());
                    Map<CollectErrorParamEnum, String> params = new HashMap<>();
                    vitamErrorDetails.getArgs().forEach((k, v) -> params.put(CollectErrorParamEnum.valueOf(k), v));
                    errorAccumulator.reportOneError(key, params);
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
        throws CollectInternalServerSideException, CollectInternalSingleErrorsDetailException {
        if (jsonlMetadataFile.length() == 0) {
            throw CollectErrorDetailHelper.generateException(CollectErrorMessagesEnum.EMPTY_JSONL_FILE);
        }
        try {
            SanityChecker.checkJsonLines(jsonlMetadataFile);
        } catch (IOException e) {
            throw new CollectInternalServerSideException(
                "An internal error occurred during jsonl metadata file processing",
                e
            );
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.CANNOT_VALIDATE_JSON_LINES_REQUEST,
                Map.of(CollectErrorParamEnum.MESSAGE, e.getLocalizedMessage()),
                e
            );
        }
    }

    private void validateMetadataIdentificationInformation(
        CollectJsonMetadataLine entry,
        int lineIndex,
        boolean isFirstUpload
    ) throws CollectInternalSingleErrorsDetailException {
        if (entry.getFile() == null && entry.getSelector() == null) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_MISSING_METADATA_IDENTIFICATION_INFORMATION,
                Map.of(CollectErrorParamEnum.INDEX, Integer.toString(lineIndex))
            );
        }

        if (entry.getFile() != null && entry.getSelector() != null) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_FIELDS_MUTUALLY_EXCLUSIVE,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    Integer.toString(lineIndex),
                    CollectErrorParamEnum.FILE_FIELD,
                    CollectJsonMetadataLine.FILE_FIELD,
                    CollectErrorParamEnum.SELECTOR_FIELD,
                    CollectJsonMetadataLine.SELECTOR_FIELD
                )
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

    private void validateFileIdentifier(String fileValue, int lineIndex)
        throws CollectInternalSingleErrorsDetailException {
        if (StringUtils.isBlank(fileValue)) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_EMPTY_UNIT_FILE_PATH,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    Integer.toString(lineIndex),
                    CollectErrorParamEnum.FILE_VALUE,
                    fileValue
                )
            );
        }
        String path = FilenameUtils.normalize(fileValue);
        if (!FilenameUtils.equals(fileValue, path)) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_INVALID_UNIT_FILE_PATH,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    Integer.toString(lineIndex),
                    CollectErrorParamEnum.FILE_VALUE,
                    fileValue
                )
            );
        }
    }

    private void validateObjectFiles(String objectFilesPath, int lineIndex, boolean isFirstUpload)
        throws CollectInternalSingleErrorsDetailException {
        if (!isFirstUpload) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_FIELD_NOT_ALLOWED_FOR_UPDATE_OPERATIONS,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    Integer.toString(lineIndex),
                    CollectErrorParamEnum.OBJECT_FILES_FIELD,
                    CollectJsonMetadataLine.OBJECT_FILES_FIELD
                )
            );
        }
        if (StringUtils.isBlank(objectFilesPath)) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_EMPTY_OBJECT_FILE_PATH,
                Map.of(CollectErrorParamEnum.INDEX, Integer.toString(lineIndex))
            );
        }
        String normalizedPath = FilenameUtils.normalize(objectFilesPath);
        if (!FilenameUtils.equals(objectFilesPath, normalizedPath)) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_ILLEGAL_OBJECT_FILE_PATH,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    Integer.toString(lineIndex),
                    CollectErrorParamEnum.OBJECT_FILES_PATH,
                    objectFilesPath
                )
            );
        }
    }

    private void validateSelector(CollectJsonMetadataSelector selectorValue, int lineIndex, boolean isFirstUpload)
        throws CollectInternalSingleErrorsDetailException {
        if (selectorValue.getEntries().isEmpty()) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_EMPTY_SELECTORS,
                Map.of(CollectErrorParamEnum.INDEX, Integer.toString(lineIndex))
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
        throws CollectInternalSingleErrorsDetailException {
        validateKeyNameFormat(key, lineIndex);

        if (isFirstUpload && !VitamFieldsHelper.uploadPath().equals(key)) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_ENTRY_ONLY_FILE_OR_SELECTOR_ALLOWED_FOR_UPLOAD_OPERATIONS,
                Map.of(
                    CollectErrorParamEnum.KEY,
                    key,
                    CollectErrorParamEnum.INDEX,
                    Integer.toString(lineIndex),
                    CollectErrorParamEnum.FILE_FIELD,
                    CollectJsonMetadataLine.FILE_FIELD,
                    CollectErrorParamEnum.SELECTOR_FIELD,
                    CollectJsonMetadataLine.SELECTOR_FIELD,
                    CollectErrorParamEnum.UPLOAD_PATH,
                    VitamFieldsHelper.uploadPath()
                )
            );
        }
    }

    private static void validateKeyNameFormat(String key, int lineIndex)
        throws CollectInternalSingleErrorsDetailException {
        // TODO: Field name checks should be unified
        if (StringUtils.isBlank(key)) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_FIELD_NAME_AT_INDEX,
                Map.of(CollectErrorParamEnum.FIELD, key, CollectErrorParamEnum.INDEX, Integer.toString(lineIndex))
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
                throw CollectErrorDetailHelper.generateException(
                    CollectErrorMessagesEnum.INVALID_FIELD_NAME_AT_INDEX,
                    Map.of(CollectErrorParamEnum.FIELD, key, CollectErrorParamEnum.INDEX, Integer.toString(lineIndex))
                );
            }
        }
    }

    private void validateSelectorValue(String key, ValueNode value, int lineIndex)
        throws CollectInternalSingleErrorsDetailException {
        switch (value.getNodeType()) {
            case BOOLEAN:
            case NUMBER:
            case STRING:
                // OK
                break;
            case ARRAY:
                throw CollectErrorDetailHelper.generateException(
                    CollectErrorMessagesEnum.INVALID_ENTRY_INVALID_SELECTOR_VALUE_ARRAYS_NOT_SUPPORTED,
                    Map.of(CollectErrorParamEnum.KEY, key, CollectErrorParamEnum.INDEX, Integer.toString(lineIndex))
                );
            case NULL:
                throw CollectErrorDetailHelper.generateException(
                    CollectErrorMessagesEnum.INVALID_ENTRY_INVALID_SELECTOR_VALUE_NULL_VALUE,
                    Map.of(CollectErrorParamEnum.KEY, key, CollectErrorParamEnum.INDEX, Integer.toString(lineIndex))
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
    ) throws CollectInternalSingleErrorsDetailException {
        checkNonEmptyUnit(entry.getUnitContent(), lineIndex);
        validateReservedUnitFieldNames(entry.getUnitContent(), lineIndex);
        validateUnitFormat(entry, lineIndex, isFirstUpload, explicitAttachementMode);
    }

    private void checkNonEmptyUnit(ObjectNode unitContent, int lineIndex)
        throws CollectInternalSingleErrorsDetailException {
        if (unitContent == null || unitContent.isEmpty()) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_UNIT_METADATA_EMPTY_METADATA_CONTENT,
                Map.of(CollectErrorParamEnum.INDEX, String.valueOf(lineIndex))
            );
        }
    }

    private static void validateReservedUnitFieldNames(ObjectNode unitContent, int lineIndex)
        throws CollectInternalSingleErrorsDetailException {
        Iterator<String> it = unitContent.fieldNames();
        while (it.hasNext()) {
            String fieldName = it.next();
            if (StringUtils.containsWhitespace(fieldName) || fieldName.startsWith("$") || fieldName.startsWith("_")) {
                throw CollectErrorDetailHelper.generateException(
                    CollectErrorMessagesEnum.INVALID_UNIT_METADATA_ILLEGAL_FIELD_NAME,
                    Map.of(
                        CollectErrorParamEnum.INDEX,
                        String.valueOf(lineIndex),
                        CollectErrorParamEnum.FIELD,
                        fieldName
                    )
                );
            }
            if (fieldName.startsWith("#") && !ALLOWED_RESERVED_FIELD_NAMES.contains(fieldName)) {
                throw CollectErrorDetailHelper.generateException(
                    CollectErrorMessagesEnum.INVALID_UNIT_METADATA_FORBIDDEN_FIELD_NAME,
                    Map.of(
                        CollectErrorParamEnum.INDEX,
                        String.valueOf(lineIndex),
                        CollectErrorParamEnum.FIELD,
                        fieldName
                    )
                );
            }
            if (fieldName.contains(".")) {
                throw CollectErrorDetailHelper.generateException(
                    CollectErrorMessagesEnum.INVALID_UNIT_METADATA_FIELD_NAME_MUST_BE_ROOT_LEVEL_FIELD,
                    Map.of(
                        CollectErrorParamEnum.INDEX,
                        String.valueOf(lineIndex),
                        CollectErrorParamEnum.FIELD,
                        fieldName
                    )
                );
            }
        }
    }

    private static void validateUnitFormat(
        CollectJsonMetadataLine entry,
        int lineIndex,
        boolean isFirstUpload,
        boolean explicitAttachementMode
    ) throws CollectInternalSingleErrorsDetailException {
        ArchiveUnitModel archiveUnitModel;
        try {
            // Use strict deserializer to validate unit content structure & format
            archiveUnitModel = JsonHandler.getFromStrictJsonNode(
                entry.getUnitContent(),
                ARCHIVE_UNIT_MODEL_TYPE_REFERENCE
            );
        } catch (InvalidParseOperationException e) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_UNIT_METADATA_UNIT_FORMAT_VALIDATION_FAILED,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    Integer.toString(lineIndex),
                    CollectErrorParamEnum.MESSAGE,
                    e.getLocalizedMessage()
                )
            );
        }

        validateUnitRulesEndDates(archiveUnitModel, lineIndex);

        validateUpdateOperation(entry, archiveUnitModel, lineIndex, isFirstUpload, explicitAttachementMode);
    }

    private static void validateUnitRulesEndDates(ArchiveUnitModel archiveUnitModel, int lineIndex)
        throws CollectInternalSingleErrorsDetailException {
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
        throws CollectInternalSingleErrorsDetailException {
        if (ruleCategoryModel == null || CollectionUtils.isEmpty(ruleCategoryModel.getRules())) {
            return;
        }
        for (RuleModel rule : ruleCategoryModel.getRules()) {
            if (rule.getEndDate() != null) {
                throw CollectErrorDetailHelper.generateException(
                    CollectErrorMessagesEnum.INVALID_UNIT_METADATA_UNIT_RULES_CANNOT_CONTAINS_END_DATE_FIELD,
                    Map.of(
                        CollectErrorParamEnum.INDEX,
                        Integer.toString(lineIndex),
                        CollectErrorParamEnum.RULE,
                        ruleCategory,
                        CollectErrorParamEnum.FIELD,
                        END_DATE
                    )
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
    ) throws CollectInternalSingleErrorsDetailException {
        if (archiveUnitModel.getManagement() == null || archiveUnitModel.getManagement().getUpdateOperation() == null) {
            return;
        }

        if (explicitAttachementMode) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.CANNOT_SET_FIELDS_WHEN_EXPLICIT_HTTP_HEADER_IS_SET,
                Map.of(
                    CollectErrorParamEnum.PATH,
                    MANAGEMENT_UPDATE_OPERATION_API_PATH,
                    CollectErrorParamEnum.HEADER,
                    X_ATTACHEMENT_ID
                )
            );
        }

        if (!isFirstUpload) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_UNIT_METADATA_FIELDS_NOT_SUPPORTED_IN_UPDATE_APIS,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    Integer.toString(lineIndex),
                    CollectErrorParamEnum.FIELD,
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
        throws CollectInternalSingleErrorsDetailException {
        String uploadPath = getInitialUploadPath(entry);
        boolean isTopLevelFolder = !uploadPath.contains(File.separator);
        if (!isTopLevelFolder) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_UNIT_METADATA_ONLY_TOP_LEVEL_UNIT_CAN_HAVE_MANAGEMENT_UPDATE_FIELDS,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    String.valueOf(lineIndex),
                    CollectErrorParamEnum.PATH,
                    MANAGEMENT_UPDATE_OPERATION_API_PATH
                )
            );
        }
    }

    private static void validateUpdateOperationFields(UpdateOperationModel updateOperation, int lineIndex)
        throws CollectInternalSingleErrorsDetailException {
        String systemId = updateOperation.getSystemId();

        String metadataName = updateOperation.getArchiveUnitIdentifierKey() != null
            ? updateOperation.getArchiveUnitIdentifierKey().getMetadataName()
            : null;
        String metadataValue = updateOperation.getArchiveUnitIdentifierKey() != null
            ? updateOperation.getArchiveUnitIdentifierKey().getMetadataValue()
            : null;

        if (systemId == null && metadataName == null && metadataValue == null) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_UNIT_METADATA_MISSING_OR_EMPTY_MANAGEMENT_UPDATE_FIELDS,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    String.valueOf(lineIndex),
                    CollectErrorParamEnum.PATH,
                    MANAGEMENT_UPDATE_OPERATION_API_PATH
                )
            );
        }

        if (metadataName != null && metadataValue == null) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_UNIT_METADATA_MISSING_OR_EMPTY_MANAGEMENT_UPDATE_FIELDS,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    String.valueOf(lineIndex),
                    CollectErrorParamEnum.PATH,
                    MANAGEMENT_UPDATE_OPERATION_ARCHIVE_UNIT_IDENTIFIER_KEY_METADATA_VALUE_API_PATH
                )
            );
        }

        if (metadataName == null && metadataValue != null) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_UNIT_METADATA_MISSING_OR_EMPTY_MANAGEMENT_UPDATE_FIELDS,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    String.valueOf(lineIndex),
                    CollectErrorParamEnum.PATH,
                    MANAGEMENT_UPDATE_OPERATION_ARCHIVE_UNIT_IDENTIFIER_KEY_METADATA_NAME_API_PATH
                )
            );
        }

        if (systemId != null && metadataName != null) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.INVALID_UNIT_METADATA_BOTH_API_PATH_HEADERS_ARE_SET,
                Map.of(
                    CollectErrorParamEnum.INDEX,
                    String.valueOf(lineIndex),
                    CollectErrorParamEnum.SYSTEM_ID,
                    MANAGEMENT_UPDATE_OPERATION_SYSTEM_ID_API_PATH,
                    CollectErrorParamEnum.METADATA_NAME,
                    MANAGEMENT_UPDATE_OPERATION_ARCHIVE_UNIT_IDENTIFIER_KEY_METADATA_NAME_API_PATH
                )
            );
        }
    }

    private static void checkIncompatibleFieldsWithUpdateOperationFields(ObjectNode unitContent, int lineIndex)
        throws CollectInternalSingleErrorsDetailException {
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
                        throw CollectErrorDetailHelper.generateException(
                            CollectErrorMessagesEnum.INVALID_UNIT_METADATA_CANNOT_SET_OTHER_METADATA_FIELD_WHEN_UPDATE_OPERATION_HEADER_DEFINED,
                            Map.of(
                                CollectErrorParamEnum.INDEX,
                                String.valueOf(lineIndex),
                                CollectErrorParamEnum.FIELD,
                                MANAGEMENT_FIELD + SEPARATOR + anyOtherManagementField.get(),
                                CollectErrorParamEnum.HEADER,
                                MANAGEMENT_UPDATE_OPERATION_API_PATH
                            )
                        );
                    }
                    break;
                default:
                    throw CollectErrorDetailHelper.generateException(
                        CollectErrorMessagesEnum.INVALID_UNIT_METADATA_CANNOT_SET_OTHER_METADATA_FIELD_WHEN_UPDATE_OPERATION_HEADER_DEFINED,
                        Map.of(
                            CollectErrorParamEnum.INDEX,
                            String.valueOf(lineIndex),
                            CollectErrorParamEnum.FIELD,
                            fieldName,
                            CollectErrorParamEnum.HEADER,
                            MANAGEMENT_UPDATE_OPERATION_API_PATH
                        )
                    );
            }
        }
    }
}
