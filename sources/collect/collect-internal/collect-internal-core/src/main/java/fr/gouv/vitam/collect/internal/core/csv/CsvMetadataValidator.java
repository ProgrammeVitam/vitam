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

import fr.gouv.vitam.collect.common.exception.CollectInternalException;
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
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_DESCRIPTION_VALID_HEADER_NAME_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ALGORITHM;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_TITLE_VALID_HEADER_NAME_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.FILE_HEADER;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEDA_EXTENSION_POINTS;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR_CHAR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.STARTS_WITH_DIGIT_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.buildPath;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.equalsOrStartsWith;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isContentField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isFileField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isManagementField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.matchesPattern;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.parseIndexPattern;

public class CsvMetadataValidator {

    private static final Set<Character> RESERVED_CHARACTERS =
        "'\"`,;:°$§&# .*+=/|\\(){}[]@~^!?<>%".chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
    public static final int MAX_HEADER_NAME_LENGTH = 255;
    public static final int MAX_HEADER_NAMES = 10_000;
    // At least "File" header + 1 other header to update is expected
    public static final int MIN_HEADER_COUNT = 2;
    // Array index must be between 0 and 9999
    public static final int MAX_ARRAY_INDEX_LENGTH = 4;

    public void validateHeaderNames(SedaSchemaInfoResolver sedaSchemaInfoResolver, List<String> headerNames)
        throws CollectInternalException {
        globalHeaderNameChecks(headerNames);

        List<String> contentHeaderNames = headerNames.stream().filter(CsvMetadataUtils::isContentField).toList();
        validateContentHeaderNames(sedaSchemaInfoResolver, contentHeaderNames);

        List<String> managementHeaderNames = headerNames.stream().filter(CsvMetadataUtils::isManagementField).toList();
        validateManagementHeaderNames(managementHeaderNames);
    }

    private void globalHeaderNameChecks(List<String> headerNames) throws CollectInvalidCsvFormatException {
        checkTooManyHeaderNames(headerNames);
        checkDuplicateHeaderNames(headerNames);
        checkRequiredHeaderNames(headerNames);
        headerNameSanityChecks(headerNames);
        checkAttributeHeaderNames(headerNames);
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
                    "Invalid header name '" + StringUtils.abbreviate(headerName, MAX_HEADER_NAME_LENGTH) + "'. Too long"
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
        if (StringUtils.isAllBlank(fieldName)) {
            throw new CollectInvalidCsvFormatException("Invalid header name '" + headerName + "'. Empty field name");
        }
        for (char c : fieldName.toCharArray()) {
            if (Character.isISOControl(c) || !Character.isDefined(c) || RESERVED_CHARACTERS.contains(c)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header name '" + headerName + "'. Reserved characters"
                );
            }
        }
        if (fieldName.startsWith("_") || fieldName.startsWith("-")) {
            throw new CollectInvalidCsvFormatException(
                "Invalid header name '" + headerName + "'. Field name cannot start with '_' or '-'"
            );
        }

        if (matchesPattern(fieldName, STARTS_WITH_DIGIT_PATTERN)) {
            throw new CollectInvalidCsvFormatException(
                "Invalid header name '" + headerName + "'. Field name cannot start with a digit"
            );
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

    private void checkHeaderArrayIndexes(List<String> headerNames) throws CollectInvalidCsvFormatException {
        HashSetValuedHashMap<String, Integer> fieldsWithArrayIndexes = new HashSetValuedHashMap<>();
        Set<String> fieldsWithoutArrayIndexes = new HashSet<>();

        for (String headerName : headerNames) {
            String[] parts = StringUtils.splitPreserveAllTokens(headerName, SEPARATOR_CHAR);
            String fieldName = null;
            for (int i = 0; i < parts.length; i++) {
                fieldName = buildPath(fieldName, parts[i]);
                if ((i + 1 < parts.length) && matchesPattern(parts[i + 1], ARRAY_INDEX_PATTERN)) {
                    fieldsWithArrayIndexes.put(fieldName, parseIndexPattern(headerName, parts[i + 1]));
                } else {
                    fieldsWithoutArrayIndexes.add(fieldName);
                }
            }
        }

        // No field declared with both explicit and implicit array index ("A" & "A.1")
        for (String fieldName : fieldsWithoutArrayIndexes) {
            if (fieldsWithArrayIndexes.containsKey(fieldName)) {
                throw new CollectInvalidCsvFormatException(
                    "Invalid header names. Cannot mix implicit array and array index syntaxes for field '" +
                    fieldName +
                    "'"
                );
            }
        }

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

    private void validateContentHeaderNames(
        SedaSchemaInfoResolver sedaSchemaInfoResolver,
        List<String> contentHeaderNames
    ) throws CollectInternalException {
        preventUsingApiFieldNameAsSedaPath(sedaSchemaInfoResolver, contentHeaderNames);

        // Array indexes can only be checked for Content.*. Management.* fields require special handling
        checkHeaderArrayIndexes(contentHeaderNames);

        // Content.Title[.*]
        validateSpecialContentTitleHeaderNames(
            contentHeaderNames.stream().filter(CsvMetadataUtils::isContentTitleField).toList()
        );

        // Content.Description[.*]
        validateSpecialContentDescriptionHeaderNames(
            contentHeaderNames.stream().filter(CsvMetadataUtils::isContentDescriptionField).toList()
        );

        List<String> mainContentHeaderNames = contentHeaderNames
            .stream()
            .filter(
                headerName ->
                    !CsvMetadataUtils.isContentTitleField(headerName) &&
                    !CsvMetadataUtils.isContentDescriptionField(headerName)
            )
            .toList();
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
        for (SchemaInfo schemaInfo : sedaSchemaInfoResolver.getAllContentFieldSchemaInfo()) {
            String contentApiPath = CONTENT_SEPARATOR + schemaInfo.apiPath();
            if (!contentApiPath.equals(schemaInfo.sedaPath())) {
                reservedSedaPaths.add(contentApiPath);
            }
        }

        for (String headerName : contentHeaderNames) {
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
        Map<String, SchemaInfo> extraExternalSchemaFields = new HashMap<>();
        for (String headerName : mainContentHeaderNames) {
            preventAttributeInRegularHeaderName(headerName);

            String fullFieldName = StringUtils.removeStart(headerName, CONTENT_SEPARATOR);
            String[] fieldNames = StringUtils.splitPreserveAllTokens(fullFieldName, SEPARATOR_CHAR);
            if (fieldNames.length == 0) {
                throw new CollectInvalidCsvFormatException("Invalid header name '" + headerName + "'");
            }

            SchemaInfo parentSchemaInfo = null;
            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];

                if (matchesPattern(fieldName, ARRAY_INDEX_PATTERN)) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Invalid array index"
                    );
                }

                boolean isDeclaredAsArray = false;
                if (i + 1 < fieldNames.length && matchesPattern(fieldNames[i + 1], ARRAY_INDEX_PATTERN)) {
                    isDeclaredAsArray = true;
                    i++;
                }
                boolean isDeclaredAsObject = (i + 1 < fieldNames.length);

                String parentSedaPath = parentSchemaInfo == null ? CONTENT : parentSchemaInfo.sedaPath();
                String currentSedaPath = buildPath(parentSedaPath, fieldName);

                currentSedaPath = patchSpecialSignedObjectDigestPath(currentSedaPath, isDeclaredAsObject);

                SchemaInfo schemaInfo = sedaSchemaInfoResolver.getContentFieldSchemaInfo(currentSedaPath);
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
                                fieldName +
                                "'"
                            );
                        }

                        if (!parentSchemaInfo.isExternal() && !SEDA_EXTENSION_POINTS.contains(parentSedaPath)) {
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
                            ? buildPath(parentSchemaInfo.apiPath(), fieldName)
                            : fieldName;
                        extraExternalSchemaFields.put(
                            currentSedaPath,
                            new SchemaInfo(currentSedaPath, apiPath, fieldName, isDeclaredAsObject, true, true)
                        );
                    }

                    schemaInfo = extraExternalSchemaFields.get(currentSedaPath);
                }

                if (isDeclaredAsArray && !schemaInfo.isArray()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Field '" + currentSedaPath + "' is not an array"
                    );
                }
                if (isDeclaredAsObject && !schemaInfo.isObject()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid header name '" + headerName + "'. Field '" + currentSedaPath + "' is not an object."
                    );
                }
                if (!isDeclaredAsObject && schemaInfo.isObject()) {
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

    private void validateManagementHeaderNames(List<String> managementHeaderNames) {
        // FIXME : Management field names need special handling (out of scope)
    }
}
