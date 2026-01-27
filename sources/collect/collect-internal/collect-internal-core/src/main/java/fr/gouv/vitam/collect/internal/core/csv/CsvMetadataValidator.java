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

package fr.gouv.vitam.collect.internal.core.csv;

import fr.gouv.vitam.collect.common.exception.CollectInternalMultipleErrorsDetailsException;
import fr.gouv.vitam.collect.common.exception.CollectInternalSingleErrorsDetailException;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorMessagesEnum;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorParamEnum;
import fr.gouv.vitam.collect.internal.core.helpers.CollectErrorDetailHelper;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_DESCRIPTION_;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_TITLE_;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ARRAY_INDEX_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ATTR_HEADER_NAME_SUFFIX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_DESCRIPTION;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_DESCRIPTION_VALID_HEADER_NAME_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ALGORITHM;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_TITLE;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_TITLE_VALID_HEADER_NAME_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.FILE_HEADER;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.IMPLICIT_0_ARRAY_INDEX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.IsObjectFilesField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MAX_HEADER_NAME_LENGTH;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.PREFIX_ID_HEADER;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.RULE_FIELD_NAME;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR_CHAR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.buildPath;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.equalsOrStartsWith;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isContentField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isFileField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isIdField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isManagementField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.matchesPattern;
import static fr.gouv.vitam.collect.internal.core.csv.FieldNameValidationUtils.validateRegularVitamFieldName;

public class CsvMetadataValidator {

    private static final int MAX_HEADER_NAMES = 10_000;
    // At least "File" header + 1 other header to update is expected
    private static final int MIN_HEADER_COUNT = 2;
    // Array index must be between 0 and 9999
    private static final int MAX_ARRAY_INDEX_LENGTH = 4;

    public void validateHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        List<String> headerNames,
        boolean isFirstUpload
    ) throws CollectInternalSingleErrorsDetailException, CollectInternalMultipleErrorsDetailsException {
        // Blocker errors
        checkTooManyHeaderNames(headerNames);
        checkDuplicateHeaderNames(headerNames);
        checkRequiredHeaderNames(headerNames);
        if (!isFirstUpload) {
            checkFileAndIdHeaderNames(headerNames);
        }

        // Per-header errors
        try (CsvHeaderValidationManager csvHeaderValidationManager = new CsvHeaderValidationManager(headerNames)) {
            commonHeaderNameChecks(csvHeaderValidationManager);

            validateContentHeaderNames(sedaSchemaInfoResolver, csvHeaderValidationManager);

            validateManagementHeaderNames(sedaSchemaInfoResolver, csvHeaderValidationManager, isFirstUpload);
        }
    }

    private void checkTooManyHeaderNames(List<String> headerNames) throws CollectInternalSingleErrorsDetailException {
        if (headerNames.size() > MAX_HEADER_NAMES) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.TOO_MANY_HEADER_NAMES,
                Map.of(
                    CollectErrorParamEnum.SIZE,
                    Integer.toString(headerNames.size()),
                    CollectErrorParamEnum.MAX_SIZE,
                    Integer.toString(MAX_HEADER_NAMES)
                )
            );
        }
    }

    private void checkDuplicateHeaderNames(Collection<String> headerNames)
        throws CollectInternalSingleErrorsDetailException {
        HashSet<Object> headerNameSet = new HashSet<>();
        for (String headerName : headerNames) {
            if (!headerNameSet.add(headerName)) {
                throw CollectErrorDetailHelper.generateException(
                    CollectErrorMessagesEnum.DUPLICATE_HEADER_NAME,
                    Map.of(CollectErrorParamEnum.HEADER, headerName)
                );
            }
        }
    }

    private void checkRequiredHeaderNames(List<String> headerNames) throws CollectInternalSingleErrorsDetailException {
        if (!headerNames.contains(FILE_HEADER) && !headerNames.contains(PREFIX_ID_HEADER)) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.MISSING_REQUIRED_FILE_HEADER,
                Map.of()
            );
        }

        if (headerNames.size() < MIN_HEADER_COUNT) {
            throw CollectErrorDetailHelper.generateException(CollectErrorMessagesEnum.NO_HEADER_TO_SET, Map.of());
        }
    }

    private void checkFileAndIdHeaderNames(List<String> headerNames) throws CollectInternalSingleErrorsDetailException {
        if (headerNames.contains(FILE_HEADER) && headerNames.contains(PREFIX_ID_HEADER)) {
            throw CollectErrorDetailHelper.generateException(
                CollectErrorMessagesEnum.MULTIPLE_SELECTOR_HEADERS,
                Map.of()
            );
        }
    }

    private void commonHeaderNameChecks(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        checkHeaderNameTooLong(csvHeaderValidationManager);
        checkHeaderNameCategory(csvHeaderValidationManager);
        headerNameSanityChecks(csvHeaderValidationManager);
        checkAttributeHeaderNames(csvHeaderValidationManager);
        checkInvalidArraysOfArrays(csvHeaderValidationManager);
        checkHeaderImplicitAndExplicitArrayIndexMix(csvHeaderValidationManager);
    }

    private static void checkHeaderNameTooLong(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            if (headerName.length() > MAX_HEADER_NAME_LENGTH) {
                csvHeaderValidationManager.report(headerName, CollectErrorMessagesEnum.HEADER_NAME_TOO_LONG);
            }
        }
    }

    private static void checkHeaderNameCategory(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            if (
                !isContentField(headerName) &&
                !isManagementField(headerName) &&
                !isFileField(headerName) &&
                !isIdField(headerName) &&
                !IsObjectFilesField(headerName)
            ) {
                csvHeaderValidationManager.report(headerName, CollectErrorMessagesEnum.INVALID_HEADER_NAME);
            }
        }
    }

    private void headerNameSanityChecks(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            headerNameSanityChecks(csvHeaderValidationManager, headerName);
        }
    }

    private static void headerNameSanityChecks(
        CsvHeaderValidationManager csvHeaderValidationManager,
        String headerName
    ) throws CollectInternalMultipleErrorsDetailsException {
        String[] fieldNames = StringUtils.splitPreserveAllTokens(headerName, SEPARATOR_CHAR);
        for (String fieldName : fieldNames) {
            if (matchesPattern(fieldName, ARRAY_INDEX_PATTERN)) {
                // Array index validation too large > 9999
                if (fieldName.length() > MAX_ARRAY_INDEX_LENGTH) {
                    csvHeaderValidationManager.report(
                        headerName,
                        CollectErrorMessagesEnum.ARRAY_INDEX_TOO_LARGE,
                        Map.of(CollectErrorParamEnum.FIELD, fieldName)
                    );
                    // No more processing of other fields of this header
                    return;
                }
            } else {
                CollectErrorMessagesEnum errorMessage = validateRegularVitamFieldName(fieldName);
                if (errorMessage != null) {
                    csvHeaderValidationManager.report(headerName, errorMessage);
                    // No more processing of other fields of this header
                    return;
                }
            }
        }
    }

    private void checkAttributeHeaderNames(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        // ".attr" can only be used as a suffix
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            if (headerName.contains(ATTR_HEADER_NAME_SUFFIX + SEPARATOR)) {
                csvHeaderValidationManager.report(headerName, CollectErrorMessagesEnum.ATTR_KEYWORD_ONLY_AS_SUFFIX);
            }
        }

        // No "X.Y.Z.attr" header without corresponding "X.Y.Z" header
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            if (headerName.endsWith(ATTR_HEADER_NAME_SUFFIX)) {
                String baseHeaderName = Strings.CS.removeEnd(headerName, ATTR_HEADER_NAME_SUFFIX);
                if (!csvHeaderValidationManager.containsHeaderName(baseHeaderName)) {
                    csvHeaderValidationManager.report(
                        headerName,
                        CollectErrorMessagesEnum.MISSING_BASE_HEADER_NAME,
                        Map.of(CollectErrorParamEnum.BASE_HEADER, baseHeaderName)
                    );
                }
            }
        }
    }

    private void checkHeaderImplicitAndExplicitArrayIndexMix(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        Set<String> fieldsWithArrayIndexes = new HashSet<>();
        Set<String> fieldsWithoutArrayIndexes = new HashSet<>();

        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                if (fieldEntry.isDeclaredAsArray()) {
                    fieldsWithArrayIndexes.add(fieldEntry.fullSedaPathWithoutLastArrayIndex());
                } else {
                    fieldsWithoutArrayIndexes.add(fieldEntry.fullSedaPathWithoutLastArrayIndex());
                }
            }
        }

        // No field declared with both explicit and implicit array index ("A" & "A.1", or "A" & "A.0")
        for (String fieldName : fieldsWithoutArrayIndexes) {
            if (fieldsWithArrayIndexes.contains(fieldName)) {
                // Report error for all matching header names
                for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidateByPrefix(
                    fieldName
                )) {
                    csvHeaderValidationManager.report(
                        headerName,
                        CollectErrorMessagesEnum.CANNOT_MIX_IMPLICIT_ARRAY_AND_ARRAY_INDEX_SYNTAXES,
                        Map.of(CollectErrorParamEnum.FIELD, fieldName)
                    );
                }
            }
        }
    }

    private void checkInvalidArraysOfArrays(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            checkInvalidArraysOfArrays(csvHeaderValidationManager, headerName);
        }
    }

    private static void checkInvalidArraysOfArrays(
        CsvHeaderValidationManager csvHeaderValidationManager,
        String headerName
    ) throws CollectInternalMultipleErrorsDetailsException {
        for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
            if (matchesPattern(fieldEntry.sedaFieldName(), ARRAY_INDEX_PATTERN)) {
                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.INVALID_ARRAY_DECLARATION,
                    Map.of(CollectErrorParamEnum.PATH, fieldEntry.parentFullSedaPath())
                );
                // No more processing of other fields of this header
                return;
            }
        }
    }

    private void validateContentHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        CsvHeaderValidationManager csvHeaderValidationManager
    ) throws CollectInternalMultipleErrorsDetailsException {
        // Prevent using Api fields names (ex "Content.Event.evId" instead of "Content.Event.EventIdentifier")
        preventUsingApiFieldNameAsSedaPath(sedaSchemaInfoResolver, csvHeaderValidationManager);

        // No sparse arrays (ex. "Content.XYZ.0" & "Content.XYZ.2" without "Content.XYZ.1")
        checkSparseContentArraysHeaderNames(csvHeaderValidationManager);

        // Content.Title[.*]
        validateSpecialContentTitleHeaderNames(csvHeaderValidationManager);

        // Content.Description[.*]
        validateSpecialContentDescriptionHeaderNames(csvHeaderValidationManager);

        validateRegularContentHeaderNames(sedaSchemaInfoResolver, csvHeaderValidationManager);
    }

    /**
     * Prevent using ApiPath as a substitute to SedaPath to avoid conflicts, duplicates & field injection...
     * - "Content.Title_" instead of "Content.Title" / "Content.Title.attr"
     * - "Content.Description_" instead of "Content.Description" / "Content.Description.attr"
     * - "Content.Event.<apiField>" (ex. "Content.Event.evId" instead of "Content.Event.EventIdentifier")
     */
    private void preventUsingApiFieldNameAsSedaPath(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        CsvHeaderValidationManager csvHeaderValidationManager
    ) throws CollectInternalMultipleErrorsDetailsException {
        Map<String, String> reservedSedaPaths = new HashMap<>();
        reservedSedaPaths.put(CONTENT_SEPARATOR + API_FIELD_TITLE_, CONTENT_TITLE);
        reservedSedaPaths.put(CONTENT_SEPARATOR + API_FIELD_DESCRIPTION_, CONTENT_DESCRIPTION);
        reservedSedaPaths.put(
            CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST,
            CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST
        );
        reservedSedaPaths.put(
            CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ALGORITHM,
            CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR
        );
        for (SedaSchemaInfo schemaInfo : sedaSchemaInfoResolver.getAllContentSchemaInfo()) {
            String contentApiPath = CONTENT_SEPARATOR + schemaInfo.apiPath();
            if (!contentApiPath.equals(schemaInfo.sedaPath())) {
                reservedSedaPaths.put(contentApiPath, schemaInfo.sedaPath());
            }
        }

        for (String headerName : csvHeaderValidationManager.getRemainingContentHeaderNamesToValidate()) {
            // Remove array indexes
            String sedaPath = headerName.replaceAll("\\.\\d+$", "").replaceAll("\\.\\d+\\.", ".");
            Optional<Map.Entry<String, String>> reservedPathEntry = reservedSedaPaths
                .entrySet()
                .stream()
                .filter(reservedPath -> equalsOrStartsWith(sedaPath, reservedPath.getKey()))
                .findFirst();
            if (reservedPathEntry.isPresent()) {
                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.HEADER_MUST_BE_SEDA_INSTEAD_OF_VITAM,
                    Map.of(
                        CollectErrorParamEnum.PATH,
                        reservedPathEntry.get().getValue(),
                        CollectErrorParamEnum.FIELD,
                        reservedPathEntry.get().getKey()
                    )
                );
            }
        }
    }

    private void checkSparseContentArraysHeaderNames(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        HashSetValuedHashMap<String, Integer> sedaPathWithArrayIndexes = new HashSetValuedHashMap<>();

        for (String headerName : csvHeaderValidationManager.getRemainingContentHeaderNamesToValidate()) {
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                if (fieldEntry.isDeclaredAsArray()) {
                    sedaPathWithArrayIndexes.put(
                        fieldEntry.fullSedaPathWithoutLastArrayIndex(),
                        fieldEntry.arrayIndex()
                    );
                }
            }
        }

        checkNoMissingArrayIndexes(sedaPathWithArrayIndexes, csvHeaderValidationManager);
    }

    private void checkNoMissingArrayIndexes(
        HashSetValuedHashMap<String, Integer> sedaPathWithArrayIndexes,
        CsvHeaderValidationManager csvHeaderValidationManager
    ) throws CollectInternalMultipleErrorsDetailsException {
        // No sparse arrays (ex. "A.0" & "A.2" without "A.1")
        for (String sedaPath : sedaPathWithArrayIndexes.keySet()) {
            Set<Integer> arrayIndexes = sedaPathWithArrayIndexes.get(sedaPath);
            for (int i = 0; i < arrayIndexes.size(); i++) {
                if (!arrayIndexes.contains(i)) {
                    int finalI = i;
                    int nextHeaderIndex = arrayIndexes
                        .stream()
                        .mapToInt(index -> index)
                        .filter(index -> index > finalI)
                        .min()
                        .orElseThrow();

                    String unexpectedHeaderNamePrefix = buildPath(sedaPath, String.valueOf(nextHeaderIndex));
                    for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidateByPrefix(
                        unexpectedHeaderNamePrefix
                    )) {
                        final String path = buildPath(sedaPath, String.valueOf(i));
                        csvHeaderValidationManager.report(
                            headerName,
                            CollectErrorMessagesEnum.EXPECTED_HEADER_NAME_SINCE_HEADER_DECLARED,
                            Map.of(
                                CollectErrorParamEnum.PATH,
                                path,
                                CollectErrorParamEnum.UNEXPECTED_HEADER,
                                unexpectedHeaderNamePrefix
                            )
                        );
                    }
                    // No more processing of other fields of this header
                    break;
                }
            }
        }
    }

    private void validateSpecialContentTitleHeaderNames(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate(
            CsvMetadataUtils::isContentTitleField
        )) {
            if (!matchesPattern(headerName, CONTENT_TITLE_VALID_HEADER_NAME_PATTERN)) {
                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.VALID_CONTENT_TITLE_ATTR_EXPECTED
                );
            }
        }
    }

    private void validateSpecialContentDescriptionHeaderNames(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate(
            CsvMetadataUtils::isContentDescriptionField
        )) {
            if (!matchesPattern(headerName, CONTENT_DESCRIPTION_VALID_HEADER_NAME_PATTERN)) {
                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.VALID_CONTENT_DESCRIPTION_ATTR_EXPECTED
                );
            }
        }
    }

    private void validateRegularContentHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        CsvHeaderValidationManager csvHeaderValidationManager
    ) throws CollectInternalMultipleErrorsDetailsException {
        preventAttributeInRegularHeaderName(csvHeaderValidationManager);

        Map<String, SedaSchemaInfo> extraExternalSchemaFields = new HashMap<>();
        for (String headerName : csvHeaderValidationManager.getRemainingMainContentHeaderNamesToValidate()) {
            SedaSchemaInfo parentSchemaInfo = null;
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                String parentSedaPath = parentSchemaInfo == null ? null : parentSchemaInfo.sedaPath();
                String currentSedaPath = buildPath(parentSedaPath, fieldEntry.sedaFieldName());

                currentSedaPath = patchSpecialSignedObjectDigestPath(currentSedaPath, fieldEntry.isDeclaredAsObject());

                SedaSchemaInfo schemaInfo = sedaSchemaInfoResolver.getContentSchemaInfo(currentSedaPath);
                if (schemaInfo == null) {
                    // Unknown external schema field can only be added on top of
                    // - Seda extension points
                    // - External object fields
                    // - Extra / unknown external object fields

                    if (parentSchemaInfo != null) {
                        if (!parentSchemaInfo.isObject()) {
                            csvHeaderValidationManager.report(
                                headerName,
                                CollectErrorMessagesEnum.VALUE_FIELD_CANNOT_HAVE_SUBFIELD,
                                Map.of(
                                    CollectErrorParamEnum.PATH,
                                    parentSedaPath,
                                    CollectErrorParamEnum.FIELD,
                                    fieldEntry.sedaFieldName()
                                )
                            );
                            // No more processing of other fields of this header
                            break;
                        }

                        if (!parentSchemaInfo.isSedaExtensionPoint()) {
                            List<String> availableSedaFields = sedaSchemaInfoResolver
                                .getChildContentSchemaInfo(parentSedaPath)
                                .stream()
                                .filter(s -> !s.isForbiddenCsvHeader())
                                .map(s -> StringUtils.removeStart(s.sedaPath(), parentSedaPath + SEPARATOR))
                                .sorted()
                                .toList();

                            csvHeaderValidationManager.report(
                                headerName,
                                CollectErrorMessagesEnum.INVALID_SEDA_EXTENSION_POINT,
                                Map.of(
                                    CollectErrorParamEnum.PATH,
                                    parentSedaPath,
                                    CollectErrorParamEnum.FIELD,
                                    fieldEntry.sedaFieldName(),
                                    CollectErrorParamEnum.AVAILABLE,
                                    availableSedaFields.stream().collect(Collectors.joining(", ", "[", "]"))
                                )
                            );

                            // No more processing of other fields of this header
                            break;
                        }
                    }

                    if (!extraExternalSchemaFields.containsKey(currentSedaPath)) {
                        String apiPath = parentSchemaInfo != null
                            ? buildPath(parentSchemaInfo.apiPath(), fieldEntry.sedaFieldName())
                            : fieldEntry.sedaFieldName();
                        extraExternalSchemaFields.put(
                            currentSedaPath,
                            new SedaSchemaInfo(
                                currentSedaPath,
                                apiPath,
                                fieldEntry.sedaFieldName(),
                                fieldEntry.isDeclaredAsObject(),
                                true,
                                true,
                                true,
                                false,
                                false
                            )
                        );
                    }

                    schemaInfo = extraExternalSchemaFields.get(currentSedaPath);
                }

                if (schemaInfo.isForbiddenCsvHeader()) {
                    csvHeaderValidationManager.report(
                        headerName,
                        CollectErrorMessagesEnum.SEDA_FIELD_RESERVED_OR_FORBIDDEN,
                        Map.of(CollectErrorParamEnum.PATH, fieldEntry.simpleSedaPath())
                    );
                    // No more processing of other fields of this header
                    break;
                }

                if (fieldEntry.isDeclaredAsArray() && !schemaInfo.isArray()) {
                    final String path = currentSedaPath;
                    csvHeaderValidationManager.report(
                        headerName,
                        CollectErrorMessagesEnum.FIELD_NOT_ARRAY,
                        Map.of(CollectErrorParamEnum.PATH, path)
                    );
                    // No more processing of other fields of this header
                    break;
                }
                if (fieldEntry.isDeclaredAsObject() && !schemaInfo.isObject()) {
                    final String path = currentSedaPath;
                    csvHeaderValidationManager.report(
                        headerName,
                        CollectErrorMessagesEnum.FIELD_NOT_OBJECT,
                        Map.of(CollectErrorParamEnum.PATH, path)
                    );
                    // No more processing of other fields of this header
                    break;
                }
                if (!fieldEntry.isDeclaredAsObject() && schemaInfo.isObject()) {
                    final String path = currentSedaPath;
                    csvHeaderValidationManager.report(
                        headerName,
                        CollectErrorMessagesEnum.FIELD_IS_OBJECT,
                        Map.of(CollectErrorParamEnum.PATH, path)
                    );
                    // No more processing of other fields of this header
                    break;
                }
                parentSchemaInfo = schemaInfo;
            }
        }
    }

    private void preventAttributeInRegularHeaderName(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInternalMultipleErrorsDetailsException {
        for (String headerName : csvHeaderValidationManager.getRemainingMainContentHeaderNamesToValidate()) {
            if (headerName.contains(ATTR_HEADER_NAME_SUFFIX + SEPARATOR)) {
                csvHeaderValidationManager.report(headerName, CollectErrorMessagesEnum.RESERVED_ATTR_SUFFIX, Map.of());
                // No more processing of other fields of this header
                break;
            }

            if (headerName.endsWith(ATTR_HEADER_NAME_SUFFIX)) {
                if (
                    !matchesPattern(headerName, CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN)
                ) {
                    csvHeaderValidationManager.report(
                        headerName,
                        CollectErrorMessagesEnum.RESERVED_ATTR_SUFFIX,
                        Map.of()
                    );
                }
                // No more processing of other fields of this header
                break;
            }
        }
    }

    private static String patchSpecialSignedObjectDigestPath(String currentSedaPath, boolean isDeclaredAsObject) {
        if (currentSedaPath.equals(CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST) && !isDeclaredAsObject) {
            currentSedaPath = CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST;
        }
        if (currentSedaPath.equals(CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR)) {
            currentSedaPath = CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ALGORITHM;
        }
        return currentSedaPath;
    }

    private void validateManagementHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        CsvHeaderValidationManager csvHeaderValidationManager,
        boolean isFirstUpload
    ) throws CollectInternalMultipleErrorsDetailsException, CollectInternalSingleErrorsDetailException {
        validateManagementHeaderNamesAgainstSedaModel(csvHeaderValidationManager, sedaSchemaInfoResolver);

        checkManagementHeaderArrayIndexes(csvHeaderValidationManager, sedaSchemaInfoResolver);

        preventUpdateOperationHeadersUsageOnUpdateMode(csvHeaderValidationManager, isFirstUpload);
    }

    private static void validateManagementHeaderNamesAgainstSedaModel(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInternalMultipleErrorsDetailsException {
        for (String headerName : csvHeaderValidationManager.getRemainingManagementHeaderNamesToValidate()) {
            validateManagementHeaderNamesAgainstSedaModel(
                csvHeaderValidationManager,
                sedaSchemaInfoResolver,
                headerName
            );
        }
    }

    private void preventUpdateOperationHeadersUsageOnUpdateMode(
        CsvHeaderValidationManager csvHeaderValidationManager,
        boolean isFirstUpload
    ) throws CollectInternalMultipleErrorsDetailsException {
        if (isFirstUpload) {
            return;
        }

        Iterable<String> updateOperationHeaderNames = csvHeaderValidationManager.getRemainingHeaderNamesToValidate(
            CsvMetadataUtils::isManagementUpdateOperationField
        );
        for (String headerName : updateOperationHeaderNames) {
            csvHeaderValidationManager.report(
                headerName,
                CollectErrorMessagesEnum.DECLARING_MANAGEMENT_UPDATE_OPERATION_HEADERS_NOT_SUPPORTED,
                Map.of()
            );
        }
    }

    private static void validateManagementHeaderNamesAgainstSedaModel(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        String headerName
    ) throws CollectInternalMultipleErrorsDetailsException {
        for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
            SedaSchemaInfo sedaManagementModel = sedaSchemaInfoResolver.getManagementModelBySedaPath(
                fieldEntry.simpleSedaPath()
            );

            if (sedaManagementModel == null) {
                List<String> availableSedaFields = sedaSchemaInfoResolver
                    .getChildManagementSchemaInfo(fieldEntry.parentSimpleSedaPath())
                    .stream()
                    .filter(s -> !s.isForbiddenCsvHeader())
                    .map(s -> StringUtils.removeStart(s.sedaPath(), fieldEntry.parentSimpleSedaPath() + SEPARATOR))
                    .sorted()
                    .toList();

                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.INVALID_SEDA_EXTENSION_POINT,
                    Map.of(
                        CollectErrorParamEnum.PATH,
                        fieldEntry.parentFullSedaPath(),
                        CollectErrorParamEnum.FIELD,
                        fieldEntry.sedaFieldName(),
                        CollectErrorParamEnum.AVAILABLE,
                        availableSedaFields.stream().collect(Collectors.joining(", ", "[", "]"))
                    )
                );
                // No more processing of other fields of this header
                return;
            }

            if (sedaManagementModel.isForbiddenCsvHeader()) {
                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.SEDA_FIELD_RESERVED_OR_FORBIDDEN,
                    Map.of(CollectErrorParamEnum.PATH, fieldEntry.simpleSedaPath())
                );
                // No more processing of other fields of this header
                return;
            }

            if (fieldEntry.isDeclaredAsArray() && !sedaManagementModel.isArray()) {
                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.FIELD_NOT_ARRAY,
                    Map.of(CollectErrorParamEnum.PATH, fieldEntry.simpleSedaPath())
                );
                // No more processing of other fields of this header
                return;
            }

            if (fieldEntry.isDeclaredAsObject() && !sedaManagementModel.isObject()) {
                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.FIELD_NOT_OBJECT,
                    Map.of(CollectErrorParamEnum.PATH, fieldEntry.simpleSedaPath())
                );
                // No more processing of other fields of this header
                return;
            }

            if (!fieldEntry.isDeclaredAsObject() && sedaManagementModel.isObject()) {
                csvHeaderValidationManager.report(
                    headerName,
                    CollectErrorMessagesEnum.FIELD_IS_OBJECT,
                    Map.of(CollectErrorParamEnum.PATH, fieldEntry.simpleSedaPath())
                );
                // No more processing of other fields of this header
                return;
            }
        }
    }

    private void checkManagementHeaderArrayIndexes(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInternalMultipleErrorsDetailsException, CollectInternalSingleErrorsDetailException {
        checkSparseManagementArraysHeaderNames(csvHeaderValidationManager, sedaSchemaInfoResolver);

        checkRulePropertiesWithIndexRelativeToRuleId(csvHeaderValidationManager, sedaSchemaInfoResolver);
    }

    private void checkSparseManagementArraysHeaderNames(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInternalMultipleErrorsDetailsException, CollectInternalSingleErrorsDetailException {
        // Check for sparse arrays (ex. "Management.AppraisalRule.Rule.0" & "Management.AppraisalRule.Rule.2" without "Management.AppraisalRule.Rule.1")
        // /!\ Important : Some rule properties are declared with array index relative to rule id array index.
        //     Ex. "Management.AppraisalRule.Rule.0" & "Management.AppraisalRule.Rule.1" & "Management.AppraisalRule.StartDate.1"
        //     is a valid header name set, because Management.AppraisalRule.StartDate.1 is linked to Management.AppraisalRule.Rule.1

        HashSetValuedHashMap<String, Integer> fieldsWithArrayIndexes = new HashSetValuedHashMap<>();

        for (String headerName : csvHeaderValidationManager.getRemainingManagementHeaderNamesToValidate()) {
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                SedaSchemaInfo sedaManagementModel = sedaSchemaInfoResolver.getManagementModelBySedaPath(
                    fieldEntry.simpleSedaPath()
                );
                if (sedaManagementModel == null) {
                    throw CollectErrorDetailHelper.generateException(
                        CollectErrorMessagesEnum.EXPECTED_VALID_SEDA_PATH,
                        Map.of(CollectErrorParamEnum.PATH, fieldEntry.simpleSedaPath())
                    );
                }

                if (fieldEntry.isDeclaredAsArray()) {
                    // Skip rule properties linked to a declaring rule id
                    if (!sedaManagementModel.isSpecialRulePropertyArrayIndex()) {
                        fieldsWithArrayIndexes.put(
                            buildPath(fieldEntry.parentFullSedaPath(), fieldEntry.sedaFieldName()),
                            fieldEntry.arrayIndex()
                        );
                    }
                }
            }
        }

        checkNoMissingArrayIndexes(fieldsWithArrayIndexes, csvHeaderValidationManager);
    }

    private void checkRulePropertiesWithIndexRelativeToRuleId(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInternalMultipleErrorsDetailsException, CollectInternalSingleErrorsDetailException {
        // Some rule properties are declared with array index relative to rule id array index.
        // /!\ WARNING : array index might be implicit OR explicit.
        // Ex. "Management.AppraisalRule.StartDate.1" can only be declared along with a "Management.AppraisalRule.Rule.1"
        //     "Management.AppraisalRule.StartDate" can only be declared along with a "Management.AppraisalRule.Rule" (implicit .0) or "Management.AppraisalRule.Rule.0" (explicit .0)

        Set<String> ruleIdFullFieldNames = new HashSet<>();
        Map<String, String> rulePropertyToExpectedDeclaringRuleIdMap = new HashMap<>();
        MultiValuedMap<String, String> rulePropertyToInitialHeaderNames = new ArrayListValuedHashMap<>();

        for (String headerName : csvHeaderValidationManager.getRemainingManagementHeaderNamesToValidate()) {
            String fullFieldNameWithArrayIndex = null;
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                String fullParentFieldName = fullFieldNameWithArrayIndex;
                fullFieldNameWithArrayIndex = buildPath(fullFieldNameWithArrayIndex, fieldEntry.sedaFieldName());

                SedaSchemaInfo sedaManagementModel = sedaSchemaInfoResolver.getManagementModelBySedaPath(
                    fieldEntry.simpleSedaPath()
                );
                if (sedaManagementModel == null) {
                    throw CollectErrorDetailHelper.generateException(
                        CollectErrorMessagesEnum.EXPECTED_VALID_SEDA_PATH,
                        Map.of(CollectErrorParamEnum.PATH, fieldEntry.simpleSedaPath())
                    );
                }

                if (sedaManagementModel.isArray()) {
                    String arrayIndex = IMPLICIT_0_ARRAY_INDEX;
                    if (fieldEntry.isDeclaredAsArray()) {
                        arrayIndex = String.valueOf(fieldEntry.arrayIndex());
                    }
                    fullFieldNameWithArrayIndex = buildPath(fullFieldNameWithArrayIndex, arrayIndex);

                    // Register all declared "Rule" field names (Management.*.Rule.<index>)
                    if (RULE_FIELD_NAME.equals(fieldEntry.sedaFieldName())) {
                        ruleIdFullFieldNames.add(fullFieldNameWithArrayIndex);
                    }

                    // Register rule properties along their expected "Rule" field name
                    if (sedaManagementModel.isSpecialRulePropertyArrayIndex()) {
                        rulePropertyToExpectedDeclaringRuleIdMap.put(
                            fullFieldNameWithArrayIndex,
                            buildPath(buildPath(fullParentFieldName, RULE_FIELD_NAME), arrayIndex)
                        );

                        rulePropertyToInitialHeaderNames.put(fullFieldNameWithArrayIndex, headerName);
                    }
                }
            }
        }

        for (String rulePropertyFieldName : rulePropertyToExpectedDeclaringRuleIdMap.keySet()) {
            String expectedDeclaringRuleId = rulePropertyToExpectedDeclaringRuleIdMap.get(rulePropertyFieldName);
            if (!ruleIdFullFieldNames.contains(expectedDeclaringRuleId)) {
                for (String initialHeaderName : rulePropertyToInitialHeaderNames.get(rulePropertyFieldName)) {
                    csvHeaderValidationManager.report(
                        initialHeaderName,
                        CollectErrorMessagesEnum.RULE_PROPERTY_FIELD_DOES_NOT_HAVE_CORRESPONDING_RULE_ID,
                        Map.of(
                            CollectErrorParamEnum.FIELD,
                            rulePropertyFieldName,
                            CollectErrorParamEnum.RULE_ID,
                            expectedDeclaringRuleId
                        )
                    );
                }
            }
        }
    }
}
