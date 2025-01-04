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
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static final int MAX_HEADER_NAMES = 10_000;
    // At least "File" header + 1 other header to update is expected
    public static final int MIN_HEADER_COUNT = 2;
    // Array index must be between 0 and 9999
    public static final int MAX_ARRAY_INDEX_LENGTH = 4;

    public void validateHeaderNames(SedaSchemaInfoResolver sedaSchemaInfoResolver, List<String> headerNames)
        throws CollectInvalidCsvFormatException {
        globalHeaderNameChecks(headerNames);

        List<String> contentHeaderNames = headerNames
            .stream()
            .filter(CsvMetadataUtils::isContentField)
            .collect(Collectors.toList());
        validateContentHeaderNames(sedaSchemaInfoResolver, contentHeaderNames);

        List<String> managementHeaderNames = headerNames
            .stream()
            .filter(CsvMetadataUtils::isManagementField)
            .collect(Collectors.toList());
        validateManagementHeaderNames(sedaSchemaInfoResolver, managementHeaderNames);
    }

    private void globalHeaderNameChecks(List<String> headerNames) throws CollectInvalidCsvFormatException {
        checkTooManyHeaderNames(headerNames);
        checkDuplicateHeaderNames(headerNames);
        checkRequiredHeaderNames(headerNames);
        headerNameSanityChecks(headerNames);
        checkAttributeHeaderNames(headerNames);
        checkHeaderImplicitAndExplicitArrayIndexMix(headerNames);
        checkInvalidArraysOfArrays(headerNames);
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

    private void headerNameSanityChecks(List<String> headerNames) throws CollectInvalidCsvFormatException {
        for (String headerName : headerNames) {
            if (headerName.length() > MAX_HEADER_NAME_LENGTH) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header name '" +
                    StringUtils.abbreviate(headerName, MAX_HEADER_NAME_LENGTH) +
                    "': Header name is too long"
                );
            }
            if (!isContentField(headerName) && !isManagementField(headerName) && !isFileField(headerName)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header name '" +
                    headerName +
                    "'. Only accepted names are 'File', 'Content.*' or 'Management.*'"
                );
            }
            String[] fieldNames = StringUtils.splitPreserveAllTokens(headerName, SEPARATOR_CHAR);
            for (String fieldName : fieldNames) {
                if (matchesPattern(fieldName, ARRAY_INDEX_PATTERN)) {
                    checkArrayIndexField(fieldName, headerName);
                } else {
                    checkIllegalFieldName(fieldName, headerName);
                }
            }
        }
    }

    public void checkIllegalFieldName(String fieldName, String headerName) throws CollectInvalidCsvFormatException {
        try {
            validateRegularVitamFieldName(fieldName);
        } catch (IllegalArgumentException e) {
            throw new CollectInvalidCsvFormatException("Invalid header name '" + headerName + "': " + e.getMessage());
        }
    }

    private void checkArrayIndexField(String fieldName, String headerName) throws CollectInvalidCsvFormatException {
        // Array index validation too large > 9999
        if (fieldName.length() > MAX_ARRAY_INDEX_LENGTH) {
            throw new CollectInvalidCsvFormatException(
                "Invalid header name '" + headerName + "'. Array index too large"
            );
        }
    }

    private void checkAttributeHeaderNames(List<String> headerNames) throws CollectInvalidCsvFormatException {
        // ".attr" can only be used as a suffix
        for (String headerName : headerNames) {
            if (headerName.contains(ATTR_HEADER_NAME_SUFFIX + SEPARATOR)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header name '" + headerName + "'. Reserved 'attr' keyword can only be used as a prefix"
                );
            }
        }

        // No "X.Y.Z.attr" header without corresponding "X.Y.Z" header
        Set<String> headerNameSet = new HashSet<>(headerNames);
        for (String headerName : headerNames) {
            if (headerName.endsWith(ATTR_HEADER_NAME_SUFFIX)) {
                String baseHeaderName = StringUtils.removeEnd(headerName, ATTR_HEADER_NAME_SUFFIX);
                if (!headerNameSet.contains(baseHeaderName)) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" +
                        headerName +
                        "'. " +
                        "Missing base header name '" +
                        baseHeaderName +
                        "'"
                    );
                }
            }
        }
    }

    private void checkHeaderImplicitAndExplicitArrayIndexMix(List<String> headerNames)
        throws CollectInvalidCsvFormatException {
        Set<String> fieldsWithArrayIndexes = new HashSet<>();
        Set<String> fieldsWithoutArrayIndexes = new HashSet<>();

        for (String headerName : headerNames) {
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                if (fieldEntry.isDeclaredAsArray()) {
                    fieldsWithArrayIndexes.add(fieldEntry.fullSedaPathWithoutLastArrayIndex());
                } else {
                    fieldsWithoutArrayIndexes.add(fieldEntry.fullSedaPathWithoutLastArrayIndex());
                }
            }
        }

        // No field declared with both explicit and implicit array index ("A" & "A.1")
        for (String fieldName : fieldsWithoutArrayIndexes) {
            if (fieldsWithArrayIndexes.contains(fieldName)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header names. Cannot mix implicit array and array index syntaxes for field '" +
                    fieldName +
                    "'"
                );
            }
        }
    }

    private void checkInvalidArraysOfArrays(List<String> headerNames) throws CollectInvalidCsvFormatException {
        for (String headerName : headerNames) {
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                if (matchesPattern(fieldEntry.sedaFieldName(), ARRAY_INDEX_PATTERN)) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" +
                        headerName +
                        "'. Invalid array declaration at '" +
                        fieldEntry.parentFullSedaPath() +
                        "'"
                    );
                }
            }
        }
    }

    private void validateContentHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        List<String> contentHeaderNames
    ) throws CollectInvalidCsvFormatException {
        preventUsingApiFieldNameAsSedaPath(sedaSchemaInfoResolver, contentHeaderNames);

        // No sparse arrays (ex. "Content.XYZ.0" & "Content.XYZ.2" without "Content.XYZ.1")
        checkSparseContentArraysHeaderNames(contentHeaderNames);

        // Content.Title[.*]
        validateSpecialContentTitleHeaderNames(
            contentHeaderNames.stream().filter(CsvMetadataUtils::isContentTitleField).collect(Collectors.toList())
        );

        // Content.Description[.*]
        validateSpecialContentDescriptionHeaderNames(
            contentHeaderNames.stream().filter(CsvMetadataUtils::isContentDescriptionField).collect(Collectors.toList())
        );

        List<String> mainContentHeaderNames = contentHeaderNames
            .stream()
            .filter(
                headerName ->
                    !CsvMetadataUtils.isContentTitleField(headerName) &&
                    !CsvMetadataUtils.isContentDescriptionField(headerName)
            )
            .collect(Collectors.toList());
        validateRegularContentHeaderNames(sedaSchemaInfoResolver, mainContentHeaderNames);
    }

    /**
     * Prevent using ApiPath as a substitute to SedaPath to avoid conflicts, duplicates & field injection...
     * - "Content.Title_" instead of "Content.Title" / "Content.Title.attr"
     * - "Content.Description_" instead of "Content.Description" / "Content.Description.attr"
     * - "Content.Event.<apiField>" (ex. "Content.Event.evId" instead of "Content.Event.EventIdentifier")
     */
    private void preventUsingApiFieldNameAsSedaPath(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        List<String> contentHeaderNames
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

        for (String headerName : contentHeaderNames) {
            // Remove array indexes
            String sedaPath = headerName.replaceAll("\\.\\d+$", "").replaceAll("\\.\\d+\\.", ".");
            for (String reservedSedaPath : reservedSedaPaths) {
                if (equalsOrStartsWith(sedaPath, reservedSedaPath)) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Header must be Seda field name"
                    );
                }
            }
        }
    }

    private void checkSparseContentArraysHeaderNames(List<String> headerNames) throws CollectInvalidCsvFormatException {
        HashSetValuedHashMap<String, Integer> fieldsWithArrayIndexes = new HashSetValuedHashMap<>();

        for (String headerName : headerNames) {
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                if (fieldEntry.isDeclaredAsArray()) {
                    fieldsWithArrayIndexes.put(fieldEntry.fullSedaPathWithoutLastArrayIndex(), fieldEntry.arrayIndex());
                }
            }
        }

        checkNoMissingArrayIndexes(fieldsWithArrayIndexes);
    }

    private void checkNoMissingArrayIndexes(HashSetValuedHashMap<String, Integer> fieldsWithArrayIndexes)
        throws CollectInvalidCsvFormatException {
        // No sparse arrays (ex. "A.0" & "A.2" without "A.1")
        for (String fieldName : fieldsWithArrayIndexes.keySet()) {
            Set<Integer> arrayIndexes = fieldsWithArrayIndexes.get(fieldName);
            for (int i = 0; i < arrayIndexes.size(); i++) {
                if (!arrayIndexes.contains(i)) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header names. Missing field '" + fieldName + "." + i + "'"
                    );
                }
            }
        }
    }

    private void validateSpecialContentTitleHeaderNames(List<String> headerNames)
        throws CollectInvalidCsvFormatException {
        for (String headerName : headerNames) {
            if (!matchesPattern(headerName, CONTENT_TITLE_VALID_HEADER_NAME_PATTERN)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header name '" + headerName + "'. Valid Content.Title.* expected"
                );
            }
        }
    }

    private void validateSpecialContentDescriptionHeaderNames(List<String> headerNames)
        throws CollectInvalidCsvFormatException {
        for (String headerName : headerNames) {
            if (!matchesPattern(headerName, CONTENT_DESCRIPTION_VALID_HEADER_NAME_PATTERN)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header name '" + headerName + "'. Valid Content.Description.* expected"
                );
            }
        }
    }

    private void validateRegularContentHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        List<String> mainContentHeaderNames
    ) throws CollectInvalidCsvFormatException {
        Map<String, SedaSchemaInfo> extraExternalSchemaFields = new HashMap<>();
        for (String headerName : mainContentHeaderNames) {
            preventAttributeInRegularHeaderName(headerName);

            SedaSchemaInfo parentSchemaInfo = null;
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                if (matchesPattern(fieldEntry.sedaFieldName(), ARRAY_INDEX_PATTERN)) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Invalid array index"
                    );
                }

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
                            throw new CollectInvalidCsvFormatException(
                                "Invalid header name '" +
                                headerName +
                                "'. Value field '" +
                                parentSedaPath +
                                "' cannot have a sub-field '" +
                                fieldEntry.sedaFieldName() +
                                "'"
                            );
                        }

                        if (!parentSchemaInfo.isSedaExtensionPoint()) {
                            throw new CollectInvalidCsvFormatException(
                                "Invalid header name '" +
                                headerName +
                                "'. Invalid seda extension point '" +
                                parentSedaPath +
                                "'"
                            );
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

                if (fieldEntry.isDeclaredAsArray() && !schemaInfo.isArray()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Field '" + currentSedaPath + "' is not an array"
                    );
                }
                if (fieldEntry.isDeclaredAsObject() && !schemaInfo.isObject()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Field '" + currentSedaPath + "' is not an object."
                    );
                }
                if (!fieldEntry.isDeclaredAsObject() && schemaInfo.isObject()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Field '" + currentSedaPath + "' is an object."
                    );
                }
                parentSchemaInfo = schemaInfo;
            }
        }
    }

    private void preventAttributeInRegularHeaderName(String headerName) throws CollectInvalidCsvFormatException {
        if (headerName.contains(ATTR_HEADER_NAME_SUFFIX + SEPARATOR)) {
            throw new CollectInvalidCsvFormatException(
                "Invalid header name '" + headerName + "'. Reserved 'attr' suffix"
            );
        }

        if (headerName.endsWith(ATTR_HEADER_NAME_SUFFIX)) {
            if (!matchesPattern(headerName, CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header name '" + headerName + "'. Reserved 'attr' suffix"
                );
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
        List<String> managementHeaderNames
    ) throws CollectInvalidCsvFormatException {
        validateManagementHeaderNamesAgainstSedaModel(managementHeaderNames, sedaSchemaInfoResolver);

        checkManagementHeaderArrayIndexes(managementHeaderNames, sedaSchemaInfoResolver);
    }

    private static void validateManagementHeaderNamesAgainstSedaModel(
        List<String> managementHeaderNames,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInvalidCsvFormatException {
        for (String headerName : managementHeaderNames) {
            for (CsvHeaderFieldNameIterable.FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
                if (matchesPattern(fieldEntry.sedaFieldName(), ARRAY_INDEX_PATTERN)) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Invalid array index"
                    );
                }

                SedaSchemaInfo sedaManagementModel = sedaSchemaInfoResolver.getManagementModelBySedaPath(
                    fieldEntry.simpleSedaPath()
                );
                if (sedaManagementModel == null) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" +
                        headerName +
                        "'. Invalid seda extension point '" +
                        fieldEntry.parentFullSedaPath() +
                        "'"
                    );
                }

                if (sedaManagementModel.isForbiddenCsvHeader()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header names. Invalid header name '" +
                        headerName +
                        "'. Seda Field '" +
                        fieldEntry.sedaFieldName() +
                        "' is forbidden."
                    );
                }

                if (fieldEntry.isDeclaredAsArray() && !sedaManagementModel.isArray()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" +
                        headerName +
                        "'. Field '" +
                        fieldEntry.simpleSedaPath() +
                        "' is not an array"
                    );
                }
                if (fieldEntry.isDeclaredAsObject() && !sedaManagementModel.isObject()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" +
                        headerName +
                        "'. Field '" +
                        fieldEntry.simpleSedaPath() +
                        "' is not an object."
                    );
                }
                if (!fieldEntry.isDeclaredAsObject() && sedaManagementModel.isObject()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" +
                        headerName +
                        "'. Field '" +
                        fieldEntry.simpleSedaPath() +
                        "' is an object."
                    );
                }
            }
        }
    }

    private void checkManagementHeaderArrayIndexes(
        List<String> managementHeaderNames,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInvalidCsvFormatException {
        checkSparseManagementArraysHeaderNames(managementHeaderNames, sedaSchemaInfoResolver);

        checkRulePropertiesWithIndexRelativeToRuleId(managementHeaderNames, sedaSchemaInfoResolver);
    }

    private void checkSparseManagementArraysHeaderNames(
        List<String> managementHeaderNames,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInvalidCsvFormatException {
        // Check for sparse arrays (ex. "Management.AppraisalRule.Rule.0" & "Management.AppraisalRule.Rule.2" without "Management.AppraisalRule.Rule.1")
        // /!\ Important : Some rule properties are declared with array index relative to rule id array index.
        //     Ex. "Management.AppraisalRule.Rule.0" & "Management.AppraisalRule.Rule.1" & "Management.AppraisalRule.StartDate.1"
        //     is a valid header name set, because Management.AppraisalRule.StartDate.1 is linked to Management.AppraisalRule.Rule.1

        HashSetValuedHashMap<String, Integer> fieldsWithArrayIndexes = new HashSetValuedHashMap<>();

        for (String headerName : managementHeaderNames) {
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

        checkNoMissingArrayIndexes(fieldsWithArrayIndexes);
    }

    private void checkRulePropertiesWithIndexRelativeToRuleId(
        List<String> managementHeaderNames,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) throws CollectInvalidCsvFormatException {
        // Some rule properties are declared with array index relative to rule id array index.
        // /!\ WARNING : array index might be implicit OR explicit.
        // Ex. "Management.AppraisalRule.StartDate.1" can only be declared along with a "Management.AppraisalRule.Rule.1"
        //     "Management.AppraisalRule.StartDate" can only be declared along with a "Management.AppraisalRule.Rule" (implicit .0) or "Management.AppraisalRule.Rule.0" (explicit .0)

        Set<String> ruleIdFullFieldNames = new HashSet<>();
        Map<String, String> rulePropertyToExpectedDeclaringRuleIdMap = new HashMap<>();

        for (String headerName : managementHeaderNames) {
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
                    }
                }
            }
        }

        for (String rulePropertyFieldName : rulePropertyToExpectedDeclaringRuleIdMap.keySet()) {
            String expectedDeclaringRuleId = rulePropertyToExpectedDeclaringRuleIdMap.get(rulePropertyFieldName);
            if (!ruleIdFullFieldNames.contains(expectedDeclaringRuleId)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header names. Rule property field '" +
                    rulePropertyFieldName +
                    "' does not have a corresponding '" +
                    expectedDeclaringRuleId +
                    "'."
                );
            }
        }
    }
}
