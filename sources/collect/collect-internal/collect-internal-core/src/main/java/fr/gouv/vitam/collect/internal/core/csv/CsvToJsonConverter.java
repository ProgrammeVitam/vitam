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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wnameless.json.unflattener.JsonUnflattener;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.collect.internal.core.csv.CsvHeaderFieldNameIterable.FieldEntry;
import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidCsvFormatException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ALGORITHM_ATTR_VALUE_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ALGORITHM_SUFFIX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_DESCRIPTION;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_DESCRIPTION_;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_TITLE;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.API_FIELD_TITLE_;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ATTR_HEADER_NAME_SUFFIX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_DESCRIPTION;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_TITLE;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.LANG_ATTR_VALUE_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.RULES_PREFIX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.RULES_SEPARATOR_PREFIX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST_SUFFIX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.buildPath;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.equalsOrStartsWith;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isContentDescriptionField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.isContentTitleField;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.matchesPattern;
import static fr.gouv.vitam.collect.internal.core.csv.FieldNameValidationUtils.MAX_FIELD_NAME_LENGTH;
import static fr.gouv.vitam.collect.internal.core.csv.FieldNameValidationUtils.validateRegularVitamFieldName;

public class CsvToJsonConverter {

    private final List<String> headerNames;
    private final Map<String, String> normalizedHeaderMap;

    public CsvToJsonConverter(SedaSchemaInfoResolver sedaSchemaInfoResolver, List<String> headerNames)
        throws CollectInvalidCsvFormatException {
        this.headerNames = headerNames;

        CsvMetadataValidator csvMetadataValidator = new CsvMetadataValidator();
        csvMetadataValidator.validateHeaderNames(sedaSchemaInfoResolver, headerNames);

        Map<String, String> normalizedContentHeaderMap = initializeContentHeaders(headerNames, sedaSchemaInfoResolver);
        Map<String, String> normalizedManagementHeaderMap = initializeManagementHeaders(
            headerNames,
            sedaSchemaInfoResolver
        );
        this.normalizedHeaderMap = mergeMaps(normalizedContentHeaderMap, normalizedManagementHeaderMap);
    }

    private static Map<String, String> initializeContentHeaders(
        List<String> headerNames,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) {
        return headerNames
            .stream()
            // Only retain "Content.*" fields
            .filter(CsvMetadataUtils::isContentField)
            // Exclude multi-lang field (Content.Title[.*] & Content.Description[.*])
            .filter(
                headerName ->
                    !CsvMetadataUtils.isContentTitleField(headerName) &&
                    !CsvMetadataUtils.isContentDescriptionField(headerName)
            )
            .collect(
                Collectors.toMap(
                    headerName -> headerName,
                    headerName -> normalizeContentHeaderName(sedaSchemaInfoResolver, headerName)
                )
            );
    }

    private static String normalizeContentHeaderName(SedaSchemaInfoResolver sedaSchemaInfoResolver, String headerName) {
        String normalizedHeaderName = null;
        for (FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
            if (fieldEntry.simpleSedaPath().equals(CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST)) {
                if (!fieldEntry.isDeclaredAsObject()) {
                    return buildPath(normalizedHeaderName, SIGNED_OBJECT_DIGEST_MESSAGE_DIGEST_SUFFIX);
                }
            }
            if (fieldEntry.simpleSedaPath().equals(CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR)) {
                return buildPath(normalizedHeaderName, ALGORITHM_SUFFIX);
            }

            SedaSchemaInfo schemaInfo = sedaSchemaInfoResolver.getContentSchemaInfo(fieldEntry.simpleSedaPath());
            if (schemaInfo == null) {
                normalizedHeaderName = buildPath(normalizedHeaderName, fieldEntry.sedaFieldName());
            } else {
                normalizedHeaderName = buildPath(normalizedHeaderName, schemaInfo.apiSubPath());
            }

            boolean isArray = schemaInfo == null || schemaInfo.isArray();
            if (isArray) {
                int arrayIndex = 0;
                if (fieldEntry.isDeclaredAsArray()) {
                    arrayIndex = fieldEntry.arrayIndex();
                }
                normalizedHeaderName = normalizedHeaderName + "[" + arrayIndex + "]";
            }
        }
        return normalizedHeaderName;
    }

    private Map<String, String> initializeManagementHeaders(
        List<String> headerNames,
        SedaSchemaInfoResolver sedaSchemaInfoResolver
    ) {
        return headerNames
            .stream()
            // Only retain "Management.*" fields
            .filter(CsvMetadataUtils::isManagementField)
            .collect(
                Collectors.toMap(
                    headerName -> headerName,
                    headerName -> normalizeManagementHeaderName(sedaSchemaInfoResolver, headerName)
                )
            );
    }

    private String normalizeManagementHeaderName(SedaSchemaInfoResolver sedaSchemaInfoResolver, String headerName) {
        String normalizedHeaderName = null;
        for (FieldEntry fieldEntry : new CsvHeaderFieldNameIterable(headerName)) {
            SedaSchemaInfo schemaInfo = sedaSchemaInfoResolver.getManagementModelBySedaPath(
                fieldEntry.simpleSedaPath()
            );
            if (schemaInfo == null) {
                throw new IllegalStateException("Unexpected management seda header name '" + headerName + "'");
            }

            if (schemaInfo.isArray()) {
                int arrayIndex = 0;
                if (fieldEntry.isDeclaredAsArray()) {
                    arrayIndex = fieldEntry.arrayIndex();
                }

                if (schemaInfo.apiSubPath().startsWith(RULES_SEPARATOR_PREFIX)) {
                    // Split rule properties
                    // Ex. "Management.AppraisalRule.Rule.<index>" ==> "#management.AppraisalRule.Rules[<index>].Rule"
                    //     "Management.AppraisalRule.StartDate.<index>" ==> "#management.AppraisalRule.Rules[<index>].StartDate"
                    normalizedHeaderName = buildPath(normalizedHeaderName, RULES_PREFIX);
                    normalizedHeaderName = normalizedHeaderName + "[" + arrayIndex + "]";
                    normalizedHeaderName = buildPath(
                        normalizedHeaderName,
                        StringUtils.removeStart(schemaInfo.apiSubPath(), RULES_SEPARATOR_PREFIX)
                    );
                } else {
                    // Regular array
                    // Ex "Management.ReuseRule.RefNonRuleId.<index>" ==> "#management.ReuseRule.Inheritance.PreventRulesId[<inde>]"
                    normalizedHeaderName = buildPath(normalizedHeaderName, schemaInfo.apiSubPath());
                    normalizedHeaderName = normalizedHeaderName + "[" + arrayIndex + "]";
                }
            } else {
                // Single value
                // Ex. "Management.NeedAuthorization" ==> "#management.NeedAuthorization"
                //     "Management.AppraisalRule.PreventInheritance" ==> "#management.AppraisalRule.Inheritance.PreventInheritance"
                normalizedHeaderName = buildPath(normalizedHeaderName, schemaInfo.apiSubPath());
            }
        }
        return normalizedHeaderName;
    }

    private static ImmutableMap<String, String> mergeMaps(Map<String, String> map1, Map<String, String> map2) {
        return ImmutableMap.<String, String>builder().putAll(map1).putAll(map2).build();
    }

    public ObjectNode convertCsvRecordToJson(CSVRecord record) throws CollectInvalidCsvFormatException {
        SortedMap<String, String> flatFieldValueMap = new TreeMap<>();
        List<String> mainContentHeaderNames = headerNames
            .stream()
            // Skip "File" header
            .filter(headerName -> !CsvMetadataUtils.isFileField(headerName))
            .filter(headerName -> !isContentTitleField(headerName) && !isContentDescriptionField(headerName))
            .filter(headerName -> StringUtils.isNotEmpty(record.get(headerName)))
            .toList();

        for (String headerName : mainContentHeaderNames) {
            String flatFieldName = this.normalizedHeaderMap.get(headerName);
            String fieldValue = record.get(headerName);

            if (matchesPattern(headerName, CONTENT_SIGNATURE_REFERENCED_OBJECT_SIGNED_OBJECT_DIGEST_ATTR_PATTERN)) {
                fieldValue = validateAndFixSignatureReferencedObjectSignedObjectDigestAlgorithm(headerName, fieldValue);
            }

            flatFieldValueMap.put(flatFieldName, fieldValue);
        }

        // Handle special fields
        flatFieldValueMap.putAll(mapMultiLangAttrHeaders(record, CONTENT_TITLE, API_FIELD_TITLE, API_FIELD_TITLE_));
        flatFieldValueMap.putAll(
            mapMultiLangAttrHeaders(record, CONTENT_DESCRIPTION, API_FIELD_DESCRIPTION, API_FIELD_DESCRIPTION_)
        );

        // Check for missing array nodes (ex. "A[0].*" & "A[2].*", without "A[1].*"
        checkSparseHeaders(flatFieldValueMap);

        // Convert flat field names to json
        ObjectNode unitContent;
        final String jsonStr = JsonUnflattener.unflatten(flatFieldValueMap);
        try {
            unitContent = (ObjectNode) JsonHandler.getFromString(jsonStr);
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException("An error occurred during Csv metadata mapping", e);
        }
        return unitContent;
    }

    private static String validateAndFixSignatureReferencedObjectSignedObjectDigestAlgorithm(
        String headerName,
        String fieldValue
    ) throws CollectInvalidCsvFormatException {
        Matcher matcher = ALGORITHM_ATTR_VALUE_PATTERN.matcher(fieldValue);
        if (!matcher.find()) {
            throw new CollectInvalidCsvFormatException("Invalid algorithm attribute for header '" + headerName + "'");
        }
        fieldValue = matcher.group(1);
        return fieldValue;
    }

    private void checkSparseHeaders(SortedMap<String, String> flatFieldValueMap)
        throws CollectInvalidCsvFormatException {
        HashSetValuedHashMap<String, Integer> flatFieldArrayIndexes = new HashSetValuedHashMap<>();
        for (String flatFieldName : flatFieldValueMap.keySet()) {
            int arrayStartSeparatorIndex = -1;
            while ((arrayStartSeparatorIndex = flatFieldName.indexOf('[', arrayStartSeparatorIndex + 1)) != -1) {
                int arrayEndSeparatorIndex = flatFieldName.indexOf(']', arrayStartSeparatorIndex + 1);
                String flatArrayFieldName = flatFieldName.substring(0, arrayStartSeparatorIndex);
                Integer arrayIndex = Integer.parseInt(
                    flatFieldName.substring(arrayStartSeparatorIndex + 1, arrayEndSeparatorIndex)
                );
                flatFieldArrayIndexes.put(flatArrayFieldName, arrayIndex);
            }
        }
        for (String flatFieldName : flatFieldArrayIndexes.keySet()) {
            Set<Integer> arrayIndexes = flatFieldArrayIndexes.get(flatFieldName);
            for (int i = 0; i < arrayIndexes.size(); i++) {
                if (!arrayIndexes.contains(i)) {
                    throw new CollectInvalidCsvFormatException(
                        "Missing value for " + flatFieldName.replaceAll("\\[", ".").replaceAll("]", "") + "." + i
                    );
                }
            }
        }
    }

    private Map<String, String> mapMultiLangAttrHeaders(
        CSVRecord record,
        String sedaFieldName,
        String singleValueApiFieldName,
        String multiValueApiFieldName
    ) throws CollectInvalidCsvFormatException {
        List<String> fieldHeaderNames = headerNames
            .stream()
            .filter(headerName -> equalsOrStartsWith(headerName, sedaFieldName))
            .filter(headerName -> !record.get(headerName).isEmpty())
            .toList();

        Map<Integer, String> valueByIndex = new HashMap<>();
        Map<Integer, String> langAttrByIndex = new HashMap<>();
        for (String fieldHeaderName : fieldHeaderNames) {
            String value = record.get(fieldHeaderName);
            Integer arrayIndex = getMultiLangHeaderArrayIndex(fieldHeaderName, sedaFieldName);

            if (fieldHeaderName.endsWith(ATTR_HEADER_NAME_SUFFIX)) {
                Matcher matcher = LANG_ATTR_VALUE_PATTERN.matcher(value);
                if (!matcher.find()) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid xml:lang attribute for header '" + fieldHeaderName + "'"
                    );
                }
                String lang = matcher.group(1);
                langAttrByIndex.put(arrayIndex, lang);
            } else {
                valueByIndex.put(arrayIndex, value);
            }
        }

        for (int arrayIndex = 0; arrayIndex < valueByIndex.size(); arrayIndex++) {
            if (!valueByIndex.containsKey(arrayIndex)) {
                throw new CollectInvalidCsvFormatException(
                    "Missing value for '" + sedaFieldName + "." + arrayIndex + "'"
                );
            }
        }

        for (Integer arrayIndex : langAttrByIndex.keySet()) {
            if (!valueByIndex.containsKey(arrayIndex)) {
                throw new CollectInvalidCsvFormatException(
                    "Missing value for " + sedaFieldName + "." + arrayIndex + " header"
                );
            }
        }

        Map<String, String> flatFieldValueMap = new HashMap<>();
        for (int arrayIndex = 0; arrayIndex < valueByIndex.size(); arrayIndex++) {
            String value = valueByIndex.get(arrayIndex);
            if (langAttrByIndex.containsKey(arrayIndex)) {
                String lang = langAttrByIndex.get(arrayIndex);
                try {
                    validateRegularVitamFieldName(lang);
                } catch (IllegalArgumentException e) {
                    throw new CollectInvalidCsvFormatException(
                        "Invalid lang value '" +
                        StringEscapeUtils.escapeJava(StringUtils.abbreviate(lang, MAX_FIELD_NAME_LENGTH)) +
                        "' for '" +
                        sedaFieldName +
                        ".*': " +
                        e.getMessage()
                    );
                }
                String fieldName = multiValueApiFieldName + SEPARATOR + lang;
                if (flatFieldValueMap.containsKey(fieldName)) {
                    throw new CollectInvalidCsvFormatException(
                        "Multiple values for '" + sedaFieldName + "' header with same lang attribute '" + lang + "'"
                    );
                }
                flatFieldValueMap.put(fieldName, value);
            } else {
                if (flatFieldValueMap.containsKey(singleValueApiFieldName)) {
                    throw new CollectInvalidCsvFormatException("Multiple values for '" + sedaFieldName + "' header");
                }
                flatFieldValueMap.put(singleValueApiFieldName, value);
            }
        }

        return flatFieldValueMap;
    }

    private static Integer getMultiLangHeaderArrayIndex(String fieldHeaderName, String sedaFieldName) {
        String baseHeaderName = StringUtils.substringBeforeLast(fieldHeaderName, ATTR_HEADER_NAME_SUFFIX);
        if (baseHeaderName.equals(sedaFieldName)) {
            return 0;
        }
        String arrayIndexStr = StringUtils.substringAfter(baseHeaderName, sedaFieldName + SEPARATOR);
        return Integer.parseInt(arrayIndexStr);
    }

    @VisibleForTesting
    Map<String, String> getNormalizedHeaderMap() {
        return normalizedHeaderMap;
    }
}
