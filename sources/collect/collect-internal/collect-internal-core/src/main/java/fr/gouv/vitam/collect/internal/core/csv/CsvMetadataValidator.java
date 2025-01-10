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

import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidCsvFormatException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_DESCRIPTION_;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_TITLE_;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ARRAY_INDEX_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ATTR_HEADER_NAME_SUFFIX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_DESCRIPTION_VALID_HEADER_NAME_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ALGORITHM;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_TITLE_VALID_HEADER_NAME_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.FILE_HEADER;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.IMPLICIT_0_ARRAY_INDEX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MAX_HEADER_NAME_LENGTH;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.RULE_FIELD_NAME;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR_CHAR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.buildPath;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.equalsOrStartsWith;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isContentField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isFileField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isManagementField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.matchesPattern;
import static fr.gouv.vitam.collect.internal.core.csv.FieldNameValidationUtils.validateRegularVitamFieldName;

public class CsvMetadataValidator {

    private static final int MAX_HEADER_NAMES = 10_000;
    // At least "File" header + 1 other header to update is expected
    private static final int MIN_HEADER_COUNT = 2;
    // Array index must be between 0 and 9999
    private static final int MAX_ARRAY_INDEX_LENGTH = 4;

    public void validateHeaderNames(SedaSchemaInfoResolver sedaSchemaInfoResolver, List<String> headerNames)
        throws CollectInvalidCsvFormatException {
        // Blocker errors
        checkTooManyHeaderNames(headerNames);
        checkDuplicateHeaderNames(headerNames);
        checkRequiredHeaderNames(headerNames);

        // Per-header errors
        try (CsvHeaderValidationManager csvHeaderValidationManager = new CsvHeaderValidationManager(headerNames)) {
            commonHeaderNameChecks(csvHeaderValidationManager);

            validateContentHeaderNames(sedaSchemaInfoResolver, csvHeaderValidationManager);

            validateManagementHeaderNames(sedaSchemaInfoResolver, csvHeaderValidationManager);
        }
    }

    private void checkTooManyHeaderNames(List<String> headerNames) throws CollectInvalidCsvFormatException {
        if (headerNames.size() > MAX_HEADER_NAMES) {
            throw new CollectInvalidCsvFormatException(
                "Invalid header names. Too many header names " +
                headerNames.size() +
                " (max = " +
                MAX_HEADER_NAMES +
                ")"
            );
        }
    }

    private void checkDuplicateHeaderNames(Collection<String> headerNames) throws CollectInvalidCsvFormatException {
        HashSet<Object> headerNameSet = new HashSet<>();
        for (String headerName : headerNames) {
            if (!headerNameSet.add(headerName)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header names. Duplicate header name '" + headerName + "'"
                );
            }
        }
    }

    private void checkRequiredHeaderNames(List<String> headerNames) throws CollectInvalidCsvFormatException {
        if (!headerNames.contains(FILE_HEADER)) {
            throw new CollectInvalidCsvFormatException(
                "Invalid header names. Missing required '" + FILE_HEADER + "' header name"
            );
        }

        if (headerNames.size() < MIN_HEADER_COUNT) {
            throw new CollectInvalidCsvFormatException("Invalid header names. No header to set");
        }
    }

    private void commonHeaderNameChecks(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        checkHeaderNameTooLong(csvHeaderValidationManager);
        checkHeaderNameCategory(csvHeaderValidationManager);
        headerNameSanityChecks(csvHeaderValidationManager);
        checkAttributeHeaderNames(csvHeaderValidationManager);
        checkInvalidArraysOfArrays(csvHeaderValidationManager);
        checkHeaderImplicitAndExplicitArrayIndexMix(csvHeaderValidationManager);
    }

    private static void checkHeaderNameTooLong(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            if (headerName.length() > MAX_HEADER_NAME_LENGTH) {
                csvHeaderValidationManager.report(headerName, "Header name is too long");
            }
        }
    }

    private static void checkHeaderNameCategory(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            if (!isContentField(headerName) && !isManagementField(headerName) && !isFileField(headerName)) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Invalid header name '" +
                    headerName +
                    "'. Only accepted names are 'File', 'Content.*', 'Management.*' or 'ArchiveUnitProfile'"
                );
            }
        }
    }

    private void headerNameSanityChecks(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            headerNameSanityChecks(csvHeaderValidationManager, headerName);
        }
    }

    private static void headerNameSanityChecks(
        CsvHeaderValidationManager csvHeaderValidationManager,
        String headerName
    ) throws CollectInvalidCsvFormatException {
        String[] fieldNames = StringUtils.splitPreserveAllTokens(headerName, SEPARATOR_CHAR);
        for (String fieldName : fieldNames) {
            if (matchesPattern(fieldName, ARRAY_INDEX_PATTERN)) {
                // Array index validation too large > 9999
                if (fieldName.length() > MAX_ARRAY_INDEX_LENGTH) {
                    csvHeaderValidationManager.report(headerName, "Array index '" + fieldName + "' too large");
                    // No more processing of other fields of this header
                    return;
                }
            } else {
                try {
                    validateRegularVitamFieldName(fieldName);
                } catch (IllegalArgumentException e) {
                    csvHeaderValidationManager.report(headerName, e.getMessage());
                    // No more processing of other fields of this header
                    return;
                }
            }
        }
    }

    private void checkAttributeHeaderNames(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        // ".attr" can only be used as a suffix
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            if (headerName.contains(ATTR_HEADER_NAME_SUFFIX + SEPARATOR)) {
                csvHeaderValidationManager.report(headerName, "Reserved 'attr' keyword can only be used as a prefix");
            }
        }

        // No "X.Y.Z.attr" header without corresponding "X.Y.Z" header
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            if (headerName.endsWith(ATTR_HEADER_NAME_SUFFIX)) {
                String baseHeaderName = StringUtils.removeEnd(headerName, ATTR_HEADER_NAME_SUFFIX);
                if (!csvHeaderValidationManager.containsHeaderName(baseHeaderName)) {
                    csvHeaderValidationManager.report(headerName, "Missing base header name '" + baseHeaderName + "'");
                }
            }
        }
    }

    private void checkHeaderImplicitAndExplicitArrayIndexMix(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
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
                        "Invalid header names. Cannot mix implicit array and array index syntaxes for field '" +
                        fieldName +
                        "'"
                    );
                }
            }
        }
    }

    private void checkInvalidArraysOfArrays(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate()) {
            checkInvalidArraysOfArrays(csvHeaderValidationManager, headerName);
        }
    }

    private static void checkInvalidArraysOfArrays(
        CsvHeaderValidationManager csvHeaderValidationManager,
        String headerName
    ) throws CollectInvalidCsvFormatException {
        for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
            if (matchesPattern(fieldEntry.sedaFieldName(), ARRAY_INDEX_PATTERN)) {
                csvHeaderValidationManager.report(
                    headerName,
                    "'Invalid array declaration at '" + fieldEntry.parentFullSedaPath() + "'"
                );
                // No more processing of other fields of this header
                return;
            }
        }
    }

    private void validateContentHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        CsvHeaderValidationManager csvHeaderValidationManager
    ) throws CollectInvalidCsvFormatException {
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
    ) throws CollectInvalidCsvFormatException {
        List<String> reservedSedaPaths = new ArrayList<>();
        reservedSedaPaths.add(CONTENT_SEPARATOR + API_FIELD_TITLE_);
        reservedSedaPaths.add(CONTENT_SEPARATOR + API_FIELD_DESCRIPTION_);
        reservedSedaPaths.add(CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST);
        reservedSedaPaths.add(CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ALGORITHM);
        for (SedaSchemaInfo schemaInfo : sedaSchemaInfoResolver.getAllContentSchemaInfo()) {
            String contentApiPath = CONTENT_SEPARATOR + schemaInfo.apiPath();
            if (!contentApiPath.equals(schemaInfo.sedaPath())) {
                reservedSedaPaths.add(contentApiPath);
            }
        }

        for (String headerName : csvHeaderValidationManager.getRemainingContentHeaderNamesToValidate()) {
            // Remove array indexes
            String sedaPath = headerName.replaceAll("\\.\\d+$", "").replaceAll("\\.\\d+\\.", ".");
            if (reservedSedaPaths.stream().anyMatch(reservedPath -> equalsOrStartsWith(sedaPath, reservedPath))) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Header must be Seda field name instead of Vitam field name"
                );
            }
        }
    }

    private void checkSparseContentArraysHeaderNames(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
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
    ) throws CollectInvalidCsvFormatException {
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
                        csvHeaderValidationManager.report(
                            headerName,
                            "Expected header name '" +
                            buildPath(sedaPath, String.valueOf(i)) +
                            "' since header '" +
                            unexpectedHeaderNamePrefix +
                            "' is declared"
                        );
                    }
                    // No more processing of other fields of this header
                    break;
                }
            }
        }
    }

    private void validateSpecialContentTitleHeaderNames(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate(
            CsvMetadataUtils::isContentTitleField
        )) {
            if (!matchesPattern(headerName, CONTENT_TITLE_VALID_HEADER_NAME_PATTERN)) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Valid Content.Title[.*] or Content.Title[.*].attr expected"
                );
            }
        }
    }

    private void validateSpecialContentDescriptionHeaderNames(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        for (String headerName : csvHeaderValidationManager.getRemainingHeaderNamesToValidate(
            CsvMetadataUtils::isContentDescriptionField
        )) {
            if (!matchesPattern(headerName, CONTENT_DESCRIPTION_VALID_HEADER_NAME_PATTERN)) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Valid Content.Description[.*] or Content.Description[.*].attr expected"
                );
            }
        }
    }

    private void validateRegularContentHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        CsvHeaderValidationManager csvHeaderValidationManager
    ) throws CollectInvalidCsvFormatException {
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
                                "Value field '" +
                                parentSedaPath +
                                "' cannot have a sub-field '" +
                                fieldEntry.sedaFieldName() +
                                "'"
                            );
                            // No more processing of other fields of this header
                            break;
                        }

                        if (!parentSchemaInfo.isSedaExtensionPoint()) {
                            csvHeaderValidationManager.report(
                                headerName,
                                "Invalid seda extension point '" + parentSedaPath + "'"
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
                        "Seda Field '" + fieldEntry.sedaFieldName() + "' is forbidden."
                    );
                    // No more processing of other fields of this header
                    break;
                }

                if (fieldEntry.isDeclaredAsArray() && !schemaInfo.isArray()) {
                    csvHeaderValidationManager.report(headerName, "Field '" + currentSedaPath + "' is not an array");
                    // No more processing of other fields of this header
                    break;
                }
                if (fieldEntry.isDeclaredAsObject() && !schemaInfo.isObject()) {
                    csvHeaderValidationManager.report(headerName, "Field '" + currentSedaPath + "' is not an object.");
                    // No more processing of other fields of this header
                    break;
                }
                if (!fieldEntry.isDeclaredAsObject() && schemaInfo.isObject()) {
                    csvHeaderValidationManager.report(headerName, "Field '" + currentSedaPath + "' is an object.");
                    // No more processing of other fields of this header
                    break;
                }
                parentSchemaInfo = schemaInfo;
            }
        }
    }

    private void preventAttributeInRegularHeaderName(CsvHeaderValidationManager csvHeaderValidationManager)
        throws CollectInvalidCsvFormatException {
        for (String headerName : csvHeaderValidationManager.getRemainingMainContentHeaderNamesToValidate()) {
            if (headerName.contains(ATTR_HEADER_NAME_SUFFIX + SEPARATOR)) {
                csvHeaderValidationManager.report(headerName, "Reserved 'attr' suffix");
                // No more processing of other fields of this header
                break;
            }

            if (headerName.endsWith(ATTR_HEADER_NAME_SUFFIX)) {
                if (
                    !matchesPattern(headerName, CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN)
                ) {
                    csvHeaderValidationManager.report(headerName, "Reserved 'attr' suffix");
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
        CsvHeaderValidationManager csvHeaderValidationManager
    ) throws CollectInvalidCsvFormatException {
        validateManagementHeaderNamesAgainstSedaModel(csvHeaderValidationManager, sedaSchemaInfoResolver);

        checkManagementHeaderArrayIndexes(csvHeaderValidationManager, sedaSchemaInfoResolver);
    }

    private static void validateManagementHeaderNamesAgainstSedaModel(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInvalidCsvFormatException {
        for (String headerName : csvHeaderValidationManager.getRemainingManagementHeaderNamesToValidate()) {
            validateManagementHeaderNamesAgainstSedaModel(
                csvHeaderValidationManager,
                sedaSchemaInfoResolver,
                headerName
            );
        }
    }

    private static void validateManagementHeaderNamesAgainstSedaModel(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        String headerName
    ) throws CollectInvalidCsvFormatException {
        for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
            SedaSchemaInfo sedaManagementModel = sedaSchemaInfoResolver.getManagementModelBySedaPath(
                fieldEntry.simpleSedaPath()
            );

            if (sedaManagementModel == null) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Invalid seda extension point '" +
                    fieldEntry.parentFullSedaPath() +
                    "'. Unknown field '" +
                    fieldEntry.sedaFieldName() +
                    "'"
                );
                // No more processing of other fields of this header
                return;
            }

            if (sedaManagementModel.isForbiddenCsvHeader()) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Seda Field '" + fieldEntry.sedaFieldName() + "' is forbidden."
                );
                // No more processing of other fields of this header
                return;
            }

            if (fieldEntry.isDeclaredAsArray() && !sedaManagementModel.isArray()) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Field '" + fieldEntry.simpleSedaPath() + "' is not an array"
                );
                // No more processing of other fields of this header
                return;
            }

            if (fieldEntry.isDeclaredAsObject() && !sedaManagementModel.isObject()) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Field '" + fieldEntry.simpleSedaPath() + "' is not an object."
                );
                // No more processing of other fields of this header
                return;
            }

            if (!fieldEntry.isDeclaredAsObject() && sedaManagementModel.isObject()) {
                csvHeaderValidationManager.report(
                    headerName,
                    "Field '" + fieldEntry.simpleSedaPath() + "' is an object."
                );
                // No more processing of other fields of this header
                return;
            }
        }
    }

    private void checkManagementHeaderArrayIndexes(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInvalidCsvFormatException {
        checkSparseManagementArraysHeaderNames(csvHeaderValidationManager, sedaSchemaInfoResolver);

        checkRulePropertiesWithIndexRelativeToRuleId(csvHeaderValidationManager, sedaSchemaInfoResolver);
    }

    private void checkSparseManagementArraysHeaderNames(
        CsvHeaderValidationManager csvHeaderValidationManager,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInvalidCsvFormatException {
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
                    throw new IllegalStateException("Expected valid seda path '" + fieldEntry.simpleSedaPath() + "'");
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
    ) throws CollectInvalidCsvFormatException {
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
                    throw new IllegalStateException("Expected valid seda path '" + fieldEntry.simpleSedaPath() + "'");
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
                        "Rule property field '" +
                        rulePropertyFieldName +
                        "' does not have a corresponding '" +
                        expectedDeclaringRuleId +
                        "'."
                    );
                }
            }
        }
    }
}
