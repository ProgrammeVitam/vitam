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

package fr.gouv.vitam.functional.administration.core.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.model.administration.schema.SchemaInputModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.schema.SchemaImportValidationException;
import fr.gouv.vitam.functional.administration.common.schema.ErrorReportSchema;
import fr.gouv.vitam.functional.administration.common.schema.Schema;
import fr.gouv.vitam.functional.administration.common.schema.SchemaErrorCode;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SchemaValidationService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchemaValidationService.class);
    private static final Pattern PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)*$");
    private final MongoDbAccessReferential mongoDbAccessReferential;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public SchemaValidationService(
        MongoDbAccessReferential mongoDbAccessReferential,
        LogbookOperationsClientFactory logbookOperationsClientFactory
    ) {
        this.mongoDbAccessReferential = mongoDbAccessReferential;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    /**
     * Validate input schema list coherence
     *
     * @param externalSchemaInputList
     * @param currentUnitSchemaList
     * @param ontologyEltsMapByIdentifier
     * @throws VitamException
     * @throws InvalidCreateOperationException
     */
    public void validateExternalSchemaInputs(
        List<SchemaInputModel> externalSchemaInputList,
        List<SchemaResponse> currentUnitSchemaList,
        Map<String, OntologyModel> ontologyEltsMapByIdentifier,
        Map<String, List<ErrorReportSchema>> importErrors
    ) throws VitamException, InvalidCreateOperationException {
        validateExternalSchemaInputsConsistency(externalSchemaInputList, importErrors);

        Map<String, SchemaInputModel> externalSchemaInputsMapByPath = externalSchemaInputList
            .stream()
            .collect(Collectors.toMap(SchemaInputModel::getPath, schemaModel -> schemaModel));
        Map<String, SchemaResponse> currentUnitSchemaMapByPath = currentUnitSchemaList
            .stream()
            .collect(Collectors.toMap(SchemaResponse::getPath, schemaModel -> schemaModel));

        Integer currentTenant = ParameterHelper.getTenantParameter();
        if (currentTenant.equals(VitamConfiguration.getAdminTenant())) {
            checkExistingPathsAllTenantSchemaForAdminTenant(externalSchemaInputsMapByPath, importErrors);
        }
        checkExistingPathsInCurrentSchema(currentUnitSchemaMapByPath, externalSchemaInputsMapByPath, importErrors);

        checkFullPathsReturnNotfound(currentUnitSchemaMapByPath, externalSchemaInputsMapByPath, importErrors);

        checkPathsWithOntology(externalSchemaInputsMapByPath, ontologyEltsMapByIdentifier, importErrors);
    }

    private void checkExistingPathsAllTenantSchemaForAdminTenant(
        Map<String, SchemaInputModel> externalSchemaInputsMapByPath,
        Map<String, List<ErrorReportSchema>> importErrors
    ) throws VitamException, InvalidCreateOperationException {
        LOGGER.debug("Checking if paths already in schema of all tenants ");

        Set<Integer> tenants = VitamConfiguration.getTenants().stream().collect(Collectors.toSet());
        RequestResponseOK<SchemaModel> schemasResponse = findExternalSchema(
            SchemaCommonService.buildDslQueryForExtractingSchema(tenants, Collections.emptyList())
        );
        List<SchemaModel> unitSchemaForAllTenants = schemasResponse.getResults();

        Map<String, SchemaModel> currentUnitSchemaForAllTenantsMapByPath = unitSchemaForAllTenants
            .stream()
            .collect(Collectors.toMap(SchemaModel::getPath, schemaModel -> schemaModel));

        List<String> existingPathsInCurrentSchema = externalSchemaInputsMapByPath
            .keySet()
            .stream()
            .filter(externalSchemaPath -> currentUnitSchemaForAllTenantsMapByPath.containsKey(externalSchemaPath))
            .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(existingPathsInCurrentSchema)) {
            LOGGER.error("Paths already in current schema for the current or other tenants");
            existingPathsInCurrentSchema
                .stream()
                .forEach(
                    schemaPath ->
                        addError(
                            schemaPath,
                            new ErrorReportSchema(
                                SchemaErrorCode.IMPORT_SCHEMA_PATH_ALREADY_IN_SCHEMA,
                                externalSchemaInputsMapByPath.get(schemaPath),
                                "Paths already in current schema for the current or other tenants"
                            ),
                            importErrors
                        )
                );

            String message = String.format(
                "Paths already in current schema for the current or other tenants = %s",
                existingPathsInCurrentSchema.stream().collect(Collectors.joining(", "))
            );
            throw new SchemaImportValidationException(message);
        }
    }

    private void checkExistingPathsInCurrentSchema(
        Map<String, SchemaResponse> currentUnitSchemaMapByPath,
        Map<String, SchemaInputModel> externalSchemaInputsMapByPath,
        Map<String, List<ErrorReportSchema>> importErrors
    ) throws VitamException {
        LOGGER.debug("Checking paths already in current schema");

        Integer currentTenant = ParameterHelper.getTenantParameter();
        if (currentTenant.equals(VitamConfiguration.getAdminTenant())) {
            List<Integer> tenants = VitamConfiguration.getTenants();
        }

        List<String> existingPathsInCurrentSchema = externalSchemaInputsMapByPath
            .keySet()
            .stream()
            .filter(externalSchemaPath -> currentUnitSchemaMapByPath.containsKey(externalSchemaPath))
            .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(existingPathsInCurrentSchema)) {
            LOGGER.error("Paths already in current schema");
            existingPathsInCurrentSchema
                .stream()
                .forEach(
                    schemaPath ->
                        addError(
                            schemaPath,
                            new ErrorReportSchema(
                                SchemaErrorCode.IMPORT_SCHEMA_PATH_ALREADY_IN_SCHEMA,
                                externalSchemaInputsMapByPath.get(schemaPath),
                                "Path already in current schema"
                            ),
                            importErrors
                        )
                );

            String message = String.format(
                "Paths already in current schema = %s",
                existingPathsInCurrentSchema.stream().collect(Collectors.joining(", "))
            );
            throw new SchemaImportValidationException(message);
        }
    }

    private void validateExternalSchemaInputsConsistency(
        List<SchemaInputModel> externalSchemaInputList,
        Map<String, List<ErrorReportSchema>> importErrors
    ) throws SchemaImportValidationException {
        LOGGER.debug("Validating external schema inputs consistency ");
        if (CollectionUtils.isEmpty(externalSchemaInputList)) {
            LOGGER.error("Empty schema list");
            throw new SchemaImportValidationException("Empty schema list");
        }
        // Validate schema attributes
        Map<String, List<SchemaInputModel>> schemaInputsWithErrorsByAttribute = new HashMap<>();
        for (SchemaInputModel inputSchema : externalSchemaInputList) {
            // Validate Path
            final Matcher matcher = PATH_PATTERN.matcher(inputSchema.getPath());
            if (!matcher.find()) {
                schemaInputsWithErrorsByAttribute
                    .computeIfAbsent(SchemaInputModel.TAG_PATH, k -> new ArrayList<>())
                    .add(inputSchema);
            }
            // Validate ShortName
            String shortName = inputSchema.getShortName();
            if (StringUtils.isEmpty(shortName)) {
                schemaInputsWithErrorsByAttribute
                    .computeIfAbsent(SchemaInputModel.TAG_SHORT_NAME, k -> new ArrayList<>())
                    .add(inputSchema);
            }
            // Validate Description
            String description = inputSchema.getDescription();
            if (StringUtils.isEmpty(description)) {
                schemaInputsWithErrorsByAttribute
                    .computeIfAbsent(SchemaInputModel.TAG_DESCRIPTION, k -> new ArrayList<>())
                    .add(inputSchema);
            }
        }
        if (!schemaInputsWithErrorsByAttribute.isEmpty()) {
            LOGGER.error("Some inputs have validation errors");
            List<String> errorMessages = new ArrayList<>();
            for (String attribute : schemaInputsWithErrorsByAttribute.keySet()) {
                boolean isPath = SchemaInputModel.TAG_PATH.equals(attribute);
                List<SchemaInputModel> schemaInputModels = schemaInputsWithErrorsByAttribute.get(attribute);
                for (SchemaInputModel inputSchema : schemaInputModels) {
                    addError(
                        inputSchema.getPath(),
                        new ErrorReportSchema(
                            isPath
                                ? SchemaErrorCode.IMPORT_SCHEMA_WRONG_PATH_FORMAT
                                : SchemaErrorCode.IMPORT_SCHEMA_MISSING_INFORMATION,
                            inputSchema,
                            "Wrong attribute " + attribute
                        ),
                        importErrors
                    );
                }
                String paths = schemaInputModels
                    .stream()
                    .map(SchemaInputModel::getPath)
                    .collect(Collectors.joining(", "));
                errorMessages.add(String.format("%s (%s)", attribute, paths));
            }
            String message = String.format("Some inputs have validation errors: %s", String.join(", ", errorMessages));
            throw new SchemaImportValidationException(message);
        }
    }

    /**
     * Check that all paths exist in requested list or in internal or external
     *
     * @param currentUnitSchemaMapByPath
     * @param externalSchemaInputsMapByPath
     * @throws VitamException
     */

    private void checkFullPathsReturnNotfound(
        Map<String, SchemaResponse> currentUnitSchemaMapByPath,
        Map<String, SchemaInputModel> externalSchemaInputsMapByPath,
        Map<String, List<ErrorReportSchema>> importErrors
    ) throws VitamException {
        LOGGER.debug("Checking parents paths ");
        List<String> pathsWithErrors = new ArrayList<>();
        for (Map.Entry<String, SchemaInputModel> schemaModelEntry : externalSchemaInputsMapByPath.entrySet()) {
            checkExistingParentPath(
                currentUnitSchemaMapByPath,
                externalSchemaInputsMapByPath,
                schemaModelEntry.getKey(),
                pathsWithErrors
            );
        }
        if (!CollectionUtils.isEmpty(pathsWithErrors)) {
            LOGGER.error("Some paths with missing parents ");
            pathsWithErrors
                .stream()
                .forEach(
                    schemaPath ->
                        addError(
                            schemaPath,
                            new ErrorReportSchema(
                                SchemaErrorCode.IMPORT_SCHEMA_PATH_PARENT_MISSED,
                                externalSchemaInputsMapByPath.get(schemaPath),
                                "Paths with missing parents"
                            ),
                            importErrors
                        )
                );

            String message = String.format(
                "Paths with missing parents = %s",
                pathsWithErrors.stream().collect(Collectors.joining(", "))
            );
            throw new SchemaImportValidationException(message);
        }
    }

    private void checkSchemaPathsAreDeclaredAsOntologies(
        final Map<String, SchemaInputModel> externalSchemaInputsByPath,
        final Map<String, OntologyModel> ontologyByIdentifier,
        final Map<String, List<ErrorReportSchema>> importErrors
    ) throws SchemaImportValidationException {
        final List<String> leavesNotFoundInOntology = externalSchemaInputsByPath
            .values()
            .stream()
            .filter(SchemaInputModel::notObject)
            .map(schemaModel -> SchemaCommonService.extractLeafFromPath(schemaModel.getPath()))
            .collect(Collectors.toSet())
            .stream()
            .filter(leaf -> !ontologyByIdentifier.containsKey(leaf))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leavesNotFoundInOntology)) {
            return;
        }

        Map<String, List<String>> ontologyIdentifiersByLowerCase = ontologyByIdentifier
            .keySet()
            .stream()
            .collect(Collectors.groupingBy(String::toLowerCase, Collectors.toList()));

        Set<String> wrongIdentifiers = new HashSet<>();
        Map<String, List<String>> maybeWrongIdentifiers = new HashMap<>();
        leavesNotFoundInOntology.forEach(leafNotFound -> {
            if (ontologyIdentifiersByLowerCase.containsKey(leafNotFound.toLowerCase())) {
                maybeWrongIdentifiers.put(leafNotFound, ontologyIdentifiersByLowerCase.get(leafNotFound.toLowerCase()));
            } else {
                wrongIdentifiers.add(leafNotFound);
            }
        });
        if (CollectionUtils.isNotEmpty(wrongIdentifiers)) {
            final String baseMessage = "Path leaf not found in ontology";
            wrongIdentifiers.forEach(
                identifier ->
                    addError(
                        identifier,
                        new ErrorReportSchema(
                            SchemaErrorCode.IMPORT_SCHEMA_LEAF_NOT_FOUND,
                            externalSchemaInputsByPath.get(identifier),
                            baseMessage
                        ),
                        importErrors
                    )
            );
        }
        if (MapUtils.isNotEmpty(maybeWrongIdentifiers)) {
            final String baseMessage = "Path leaf not found in ontology but it may be a mistake: ";
            maybeWrongIdentifiers.forEach(
                (identifier, relatedIdentifiers) ->
                    addError(
                        identifier,
                        new ErrorReportSchema(
                            SchemaErrorCode.IMPORT_SCHEMA_LEAF_NOT_FOUND,
                            externalSchemaInputsByPath.get(identifier),
                            baseMessage + String.join(", ", relatedIdentifiers)
                        ),
                        importErrors
                    )
            );
        }

        final List<String> wrongIdentifiersMessages = wrongIdentifiers.stream().toList();
        final List<String> maybeWrongIdentifiersMessages = maybeWrongIdentifiers
            .entrySet()
            .stream()
            .map(entry -> entry.getKey() + " (existing relative: " + String.join(", ", entry.getValue()) + ")")
            .collect(Collectors.toList());
        final String computedMessage =
            "Path leaf not found in ontology: " +
            String.join(", ", ListUtils.union(wrongIdentifiersMessages, maybeWrongIdentifiersMessages));
        LOGGER.error(computedMessage);
        throw new SchemaImportValidationException(computedMessage);
    }

    private void checkSchemaObjectPathsAreNotDeclaredAsOntologies(
        final Map<String, SchemaInputModel> externalSchemaInputsMapByPath,
        final Map<String, OntologyModel> ontologyEltsMapByIdentifier,
        final Map<String, List<ErrorReportSchema>> importErrors
    ) throws SchemaImportValidationException {
        final String separator = ".";
        final List<String> pathsWithOntologyConflicts = externalSchemaInputsMapByPath
            .values()
            .stream()
            .map(inputElement -> {
                final String path = inputElement.isObject()
                    ? inputElement.getPath()
                    : StringUtils.substringBeforeLast(inputElement.getPath(), separator);
                return StringUtils.splitByWholeSeparator(path, separator);
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toCollection(HashSet::new))
            .stream()
            .filter(subPathElement -> {
                final boolean isLeaf = ontologyEltsMapByIdentifier.containsKey(subPathElement);
                final SchemaInputModel schemaInputModel = externalSchemaInputsMapByPath.get(subPathElement);
                if (schemaInputModel == null) {
                    return isLeaf;
                }

                return isLeaf && schemaInputModel.isObject();
            })
            .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(pathsWithOntologyConflicts)) {
            final String baseMessage = "Paths leafs declared as objects but already in ontology";
            checkErrors(
                pathsWithOntologyConflicts,
                baseMessage,
                SchemaErrorCode.IMPORT_SCHEMA_PATH_OBJECT_IN_ONTOLOGY,
                externalSchemaInputsMapByPath,
                importErrors
            );
        }
    }

    /**
     * As {@link fr.gouv.vitam.common.model.administration.schema.SchemaType} doesn't have a corresponding type for GEO_POINT, we prevent creating schema that matches an ontology of type GEO_POINT
     */
    private void checkSchemaDoesntMatchOntologyOfTypeGeoPoint(
        Map<String, SchemaInputModel> externalSchemaInputsMapByPath,
        Map<String, OntologyModel> ontologyEltsMapByIdentifier,
        Map<String, List<ErrorReportSchema>> importErrors
    ) throws SchemaImportValidationException {
        final List<String> pathsMatchingOntologyOfTypeGeoPoint = externalSchemaInputsMapByPath
            .values()
            .stream()
            .filter(schemaModelElt -> !Boolean.TRUE.equals(schemaModelElt.isObject()))
            .map(schemaModelElt -> SchemaCommonService.extractLeafFromPath(schemaModelElt.getPath()))
            .collect(Collectors.toSet())
            .stream()
            .filter(
                leaf ->
                    Optional.ofNullable(ontologyEltsMapByIdentifier.get(leaf))
                        .map(ontologyModel -> OntologyType.GEO_POINT.equals(ontologyModel.getType()))
                        .orElse(false)
            )
            .toList();
        if (CollectionUtils.isNotEmpty(pathsMatchingOntologyOfTypeGeoPoint)) {
            final String baseMessage = "Path matches an ontology of type GEO_POINT";
            checkErrors(
                pathsMatchingOntologyOfTypeGeoPoint,
                baseMessage,
                SchemaErrorCode.IMPORT_SCHEMA_LEAF_WRONG_TYPE,
                externalSchemaInputsMapByPath,
                importErrors
            );
        }
    }

    private void checkErrors(
        final List<String> errorPaths,
        final String baseMessage,
        final SchemaErrorCode errorCode,
        final Map<String, SchemaInputModel> externalSchemaInputsMapByPath,
        final Map<String, List<ErrorReportSchema>> importErrors
    ) throws SchemaImportValidationException {
        errorPaths.forEach(
            schemaPath ->
                addError(
                    schemaPath,
                    new ErrorReportSchema(errorCode, externalSchemaInputsMapByPath.get(schemaPath), baseMessage),
                    importErrors
                )
        );

        final String conflictingPaths = String.join(", ", errorPaths);
        final String message = String.format("%s = %s", baseMessage, conflictingPaths);
        LOGGER.error(message);
        throw new SchemaImportValidationException(message);
    }

    private void checkPathsWithOntology(
        final Map<String, SchemaInputModel> externalSchemaInputsMapByPath,
        final Map<String, OntologyModel> ontologyEltsMapByIdentifier,
        final Map<String, List<ErrorReportSchema>> importErrors
    ) throws VitamException {
        checkSchemaPathsAreDeclaredAsOntologies(
            externalSchemaInputsMapByPath,
            ontologyEltsMapByIdentifier,
            importErrors
        );
        checkSchemaObjectPathsAreNotDeclaredAsOntologies(
            externalSchemaInputsMapByPath,
            ontologyEltsMapByIdentifier,
            importErrors
        );
        checkSchemaDoesntMatchOntologyOfTypeGeoPoint(
            externalSchemaInputsMapByPath,
            ontologyEltsMapByIdentifier,
            importErrors
        );
    }

    private void checkExistingParentPath(
        Map<String, SchemaResponse> currentUnitSchemaMapByPath,
        Map<String, SchemaInputModel> externalInputSchemaMapByPath,
        String schemaPath,
        List<String> pathsWithErrors
    ) {
        LOGGER.debug("Checking parent paths of {}  ", schemaPath);

        if (currentUnitSchemaMapByPath.containsKey(schemaPath)) {
            return;
        }

        if (externalInputSchemaMapByPath.containsKey(schemaPath)) {
            if (schemaPath.contains(".")) {
                checkExistingParentPath(
                    currentUnitSchemaMapByPath,
                    externalInputSchemaMapByPath,
                    StringUtils.substringBeforeLast(schemaPath, "."),
                    pathsWithErrors
                );
            }
        } else {
            pathsWithErrors.add(schemaPath);
        }
    }

    private RequestResponseOK<SchemaModel> findExternalSchema(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        try (
            DbRequestResult result = mongoDbAccessReferential.findDocumentsWithoutRestrictionOnCurrentTenant(
                queryDsl,
                FunctionalAdminCollections.SCHEMA
            )
        ) {
            return result.getRequestResponseOK(queryDsl, Schema.class, SchemaModel.class);
        }
    }

    /**
     * Log validation error (business error)
     *
     * @param errorsDetails
     */
    public void logValidationError(GUID operationGuid, String eventType, String errorsDetails) throws VitamException {
        LOGGER.error("Validation errors on the input file {}", errorsDetails);
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            operationGuid,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.KO,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.KO),
            operationGuid
        );
        logbookMessageError(null, errorsDetails, logbookParameters);
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            logbookOperationsClient.update(logbookParameters);
        }
    }

    private void logbookMessageError(
        String objectId,
        String errorsDetails,
        LogbookOperationParameters logbookParameters
    ) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put("schemaCheck", errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (InvalidParseOperationException e) {
                // Do nothing
            }
        }
        if (null != objectId && !objectId.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectId);
        }
    }

    public void startLogBook(GUID operationGuid, String eventType)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationGuid,
            eventType,
            operationGuid,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.STARTED),
            operationGuid
        );

        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            logbookOperationsClient.create(logbookParameters);
        }
    }

    public void logError(GUID operationGuid, String eventType, String objectId, String errorsDetails)
        throws VitamException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            operationGuid,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.KO,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.KO),
            operationGuid
        );

        logbookMessageError(objectId, errorsDetails, logbookParameters);
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            logbookOperationsClient.update(logbookParameters);
        }
    }

    public void logSuccessLogBook(GUID operationGuid, String eventType) throws VitamException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            operationGuid,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.OK,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK),
            operationGuid
        );

        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            logbookOperationsClient.update(logbookParameters);
        }
    }

    public void addError(String path, ErrorReportSchema error, Map<String, List<ErrorReportSchema>> errors) {
        List<ErrorReportSchema> lineErrors = errors.get(path);
        if (lineErrors == null) {
            lineErrors = new ArrayList<>();
        }
        lineErrors.add(error);
        errors.put(path, lineErrors);
    }
}
