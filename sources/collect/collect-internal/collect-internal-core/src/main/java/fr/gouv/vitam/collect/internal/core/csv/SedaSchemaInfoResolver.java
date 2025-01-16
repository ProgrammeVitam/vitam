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

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalServerSideException;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.schema.SchemaCardinality;
import fr.gouv.vitam.common.model.administration.schema.SchemaOrigin;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.model.administration.schema.SchemaType;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ARCHIVE_UNIT_PROFILE;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.CONTENT_SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.END_DATE_FIELD;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.FORBIDDEN_CONTENT_SEDA_PATHS;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.HASH_PREFIX;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MANAGEMENT_FIELD;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.buildPath;

/**
 * Allows resolving of "Content.*", "Management.*" & "ArchiveUnitProfile" schema fields by their full "SedaPath".
 * FIXME : To be refactored or deleted when story #14050 is implemented.
 */
public class SedaSchemaInfoResolver {

    private final Map<String, SedaSchemaInfo> contentSchemaBySedaPath;
    private final Map<String, SedaSchemaInfo> managementSedaSchemaBySedaPath;

    public SedaSchemaInfoResolver(AdminManagementClientFactory adminManagementClientFactory)
        throws CollectInternalException {
        List<SchemaResponse> allSchemaModels = loadUnitSchema(adminManagementClientFactory);
        List<SchemaResponse> sedaSchemaModels = skipSystemSchemaModels(allSchemaModels);
        hotfixSchemaModels(sedaSchemaModels);
        contentSchemaBySedaPath = getContentSchemaBySedaPath(sedaSchemaModels);
        managementSedaSchemaBySedaPath = getManagementSchemaBySedaPath(sedaSchemaModels);
    }

    @Deprecated
    private static void hotfixSchemaModels(List<SchemaResponse> schemaModels) {
        // FIXME : Patch seda field "eventDetailData" -> "EventDetailData" (Bug #14048)
        schemaModels
            .stream()
            .filter(model -> model.getPath().equals("Event.evDetData"))
            .forEach(model -> model.setSedaField("EventDetailData"));

        // FIXME : Patch SedaField for INTERNAL OBJECT schema model entries (Bug #14047)
        schemaModels
            .stream()
            .filter(model -> model.getOrigin() == SchemaOrigin.INTERNAL && model.getType() == SchemaType.OBJECT)
            .forEach(model -> {
                if (model.getApiPath().equals(CsvMetadataUtils.MANAGEMENT_FIELD)) {
                    model.setSedaField(CsvMetadataUtils.MANAGEMENT);
                } else if (
                    model.getApiPath().startsWith(CsvMetadataUtils.MANAGEMENT_FIELD) &&
                    StringUtils.equalsAny(model.getApiField(), END_DATE_FIELD, "Rules", "Inheritance")
                ) {
                    // #management.<RuleCategory>.Rules
                    // #management.<RuleCategory>.Rules.EndDate
                    // #management.<RuleCategory>.Inheritance
                    model.setSedaField(null);
                } else {
                    model.setSedaField(model.getFieldName());
                }
            });
    }

    private static List<SchemaResponse> skipSystemSchemaModels(List<SchemaResponse> schemaModels) {
        // Remove vitam-internal fields (#version, #computedInheritedRules...)
        // Remove virtual fields "Title_" & "Description_"
        // Remove rule end dates "#management.<RuleCategory>.Rules.EndDate" (computed)
        return schemaModels
            .stream()
            // Remove vitam-internal fields (#version, #computedInheritedRules...) but keep #management
            .filter(
                model ->
                    !StringUtils.startsWith(model.getApiPath(), HASH_PREFIX) ||
                    StringUtils.startsWith(model.getApiPath(), MANAGEMENT_FIELD)
            )
            // Remove virtual fields "Title_" & "Description_"
            .filter(
                model ->
                    !StringUtils.equalsAny(
                        model.getApiField(),
                        CsvMetadataUtils.API_FIELD_TITLE_,
                        CsvMetadataUtils.API_FIELD_DESCRIPTION_
                    )
            )
            // Remove rule EndDate fields
            .filter(
                model ->
                    !StringUtils.startsWith(model.getApiPath(), MANAGEMENT_FIELD) ||
                    !END_DATE_FIELD.equals(model.getApiField())
            )
            .toList();
    }

    private static List<SchemaResponse> loadUnitSchema(AdminManagementClientFactory adminManagementClientFactory)
        throws CollectInternalException {
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            return ((RequestResponseOK<SchemaResponse>) client.getUnitSchema()).getResults();
        } catch (AdminManagementClientServerException e) {
            throw new CollectInternalServerSideException("Cannot load unit schema", e);
        }
    }

    private static Map<String, SedaSchemaInfo> getContentSchemaBySedaPath(List<SchemaResponse> contentSchemaModels) {
        List<SchemaResponse> sortedContentSchemaFields = contentSchemaModels
            .stream()
            // Only retain Content fields
            .filter(model -> !StringUtils.startsWith(model.getApiPath(), MANAGEMENT_FIELD))
            // FIXME : External fields do not have ApiField, ApiPath & SedaField (Bug #14049)
            .sorted(
                Comparator.comparing(
                    schema -> schema.getOrigin() == SchemaOrigin.INTERNAL ? schema.getApiPath() : schema.getPath()
                )
            )
            .toList();

        Map<String, String> apiPathToSedaPath = new HashMap<>();
        Map<String, SedaSchemaInfo> sedaPathToSedaInfo = new HashMap<>();

        for (SchemaResponse schema : sortedContentSchemaFields) {
            boolean isObject = schema.getType() == SchemaType.OBJECT;
            boolean isExternal = schema.getOrigin() == SchemaOrigin.EXTERNAL;
            boolean isArray =
                isExternal ||
                schema.getCardinality() == SchemaCardinality.MANY ||
                schema.getCardinality() == SchemaCardinality.MANY_REQUIRED;

            // FIXME : External fields do not have ApiField, ApiPath & SedaField (Bug #14049)
            String apiField = isExternal ? schema.getFieldName() : schema.getApiField();
            String apiPath = isExternal ? schema.getPath() : schema.getApiPath();
            String sedaField = isExternal ? schema.getFieldName() : schema.getSedaField();

            String sedaPath;
            if (!apiPath.contains(SEPARATOR)) {
                sedaPath = CONTENT_SEPARATOR + sedaField;
            } else {
                String parentApiPath = StringUtils.substringBeforeLast(apiPath, SEPARATOR);
                sedaPath = apiPathToSedaPath.get(parentApiPath) + SEPARATOR + sedaField;
            }

            // External fields may be extended if and only if they are defined as objects
            // Internal objects can be extended only for specific extension points.
            boolean isSedaExtensionPoint = isExternal
                ? isObject
                : CsvMetadataUtils.SEDA_EXTENSION_POINTS.contains(sedaPath);
            apiPathToSedaPath.put(apiPath, sedaPath);

            sedaPathToSedaInfo.put(
                sedaPath,
                new SedaSchemaInfo(
                    sedaPath,
                    apiPath,
                    apiField,
                    isObject,
                    isArray,
                    isExternal,
                    isSedaExtensionPoint,
                    false,
                    isForbiddenCsvHeader(sedaPath)
                )
            );
        }

        // Append root "Content" seda field
        sedaPathToSedaInfo.put(
            CONTENT,
            new SedaSchemaInfo(CONTENT, null, null, true, false, false, true, false, false)
        );

        return MapUtils.unmodifiableMap(sedaPathToSedaInfo);
    }

    private static boolean isForbiddenCsvHeader(String sedaPath) {
        return FORBIDDEN_CONTENT_SEDA_PATHS.contains(sedaPath);
    }

    private static Map<String, SedaSchemaInfo> getManagementSchemaBySedaPath(List<SchemaResponse> allSchemaModels) {
        List<SchemaResponse> sortedManagementSchemaModels = allSchemaModels
            .stream()
            .filter(model -> StringUtils.startsWith(model.getApiPath(), MANAGEMENT_FIELD))
            .sorted(Comparator.comparing(SchemaResponse::getApiPath))
            .toList();

        Map<String, String> apiPathToSedaPath = new HashMap<>();
        Map<String, SedaSchemaInfo> sedaPathToSedaInfo = new HashMap<>();

        for (SchemaResponse schema : sortedManagementSchemaModels) {
            if (schema.getSedaField() == null) {
                // Ignore Vitam fields without matching Seda fields (ex. "#management.AppraisalRule.Inheritance")
                continue;
            }

            String sedaPath;
            String apiSubPath;
            if (!schema.getApiPath().contains(SEPARATOR)) {
                apiSubPath = schema.getApiPath();
                sedaPath = schema.getSedaField();
            } else {
                sedaPath = schema.getSedaField();
                String parentPath = schema.getApiPath();
                apiSubPath = null;
                while (true) {
                    apiSubPath = buildPath(StringUtils.substringAfterLast(parentPath, SEPARATOR), apiSubPath);
                    parentPath = StringUtils.substringBeforeLast(parentPath, SEPARATOR);
                    if (apiPathToSedaPath.containsKey(parentPath)) {
                        sedaPath = buildPath(apiPathToSedaPath.get(parentPath), sedaPath);
                        break;
                    }
                }
            }
            apiPathToSedaPath.put(schema.getApiPath(), sedaPath);

            boolean isObject = schema.getType() == SchemaType.OBJECT;

            sedaPathToSedaInfo.put(
                sedaPath,
                new SedaSchemaInfo(
                    sedaPath,
                    schema.getApiPath(),
                    apiSubPath,
                    isObject,
                    isArrayManagementField(schema, sedaPath),
                    false,
                    false,
                    isSpecialRulePropertyArrayIndex(sedaPath),
                    false
                )
            );
        }

        // Add exceptions
        sedaPathToSedaInfo.put(
            "Management.LogBook",
            new SedaSchemaInfo("Management.LogBook", null, null, true, false, false, false, false, true)
        );

        sedaPathToSedaInfo.put(
            "Management.UpdateOperation",
            new SedaSchemaInfo(
                "Management.UpdateOperation",
                "#management.UpdateOperation",
                "UpdateOperation",
                true,
                false,
                false,
                false,
                false,
                true
            )
        );

        sedaPathToSedaInfo.put(
            ARCHIVE_UNIT_PROFILE,
            new SedaSchemaInfo(
                ARCHIVE_UNIT_PROFILE,
                ARCHIVE_UNIT_PROFILE,
                ARCHIVE_UNIT_PROFILE,
                false,
                false,
                false,
                false,
                false,
                false
            )
        );

        return MapUtils.unmodifiableMap(sedaPathToSedaInfo);
    }

    private static boolean isArrayManagementField(SchemaResponse schema, String sedaPath) {
        if (
            schema.getCardinality() == SchemaCardinality.MANY ||
            schema.getCardinality() == SchemaCardinality.MANY_REQUIRED
        ) {
            return true;
        }
        return CsvMetadataUtils.SEDA_MANAGEMENT_SPECIAL_ARRAY_FIELDS.contains(sedaPath);
    }

    private static boolean isSpecialRulePropertyArrayIndex(String sedaPath) {
        return CsvMetadataUtils.SEDA_MANAGEMENT_SPECIAL_RULE_PROPERTY_ARRAY_FIELDS.contains(sedaPath);
    }

    public SedaSchemaInfo getContentSchemaInfo(String sedaPath) {
        return contentSchemaBySedaPath.get(sedaPath);
    }

    @VisibleForTesting
    Collection<SedaSchemaInfo> getAllContentSchemaInfo() {
        return contentSchemaBySedaPath.values();
    }

    public SedaSchemaInfo getManagementModelBySedaPath(String sedaPath) {
        return managementSedaSchemaBySedaPath.get(sedaPath);
    }

    public List<SedaSchemaInfo> getChildContentSchemaInfo(String sedaPathPrefix) {
        return getChildSchemaInfo(sedaPathPrefix, contentSchemaBySedaPath);
    }

    public List<SedaSchemaInfo> getChildManagementSchemaInfo(String sedaPathPrefix) {
        return getChildSchemaInfo(sedaPathPrefix, managementSedaSchemaBySedaPath);
    }

    private List<SedaSchemaInfo> getChildSchemaInfo(
        String sedaPathPrefix,
        Map<String, SedaSchemaInfo> contentSchemaBySedaPath
    ) {
        return contentSchemaBySedaPath
            .values()
            .stream()
            .filter(s -> {
                if (!s.sedaPath().startsWith(sedaPathPrefix + SEPARATOR)) {
                    return false;
                }
                String subSedaPath = StringUtils.removeStart(s.sedaPath(), sedaPathPrefix + SEPARATOR);
                return !subSedaPath.contains(SEPARATOR);
            })
            .toList();
    }
}
