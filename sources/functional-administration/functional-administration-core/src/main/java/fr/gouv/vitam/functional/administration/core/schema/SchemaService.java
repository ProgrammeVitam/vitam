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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.TypeDetail;
import fr.gouv.vitam.common.model.administration.schema.SchemaCategory;
import fr.gouv.vitam.common.model.administration.schema.SchemaInputModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaOrigin;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.model.administration.schema.SchemaStringSizeType;
import fr.gouv.vitam.common.model.administration.schema.SchemaType;
import fr.gouv.vitam.common.model.administration.schema.SchemaTypeDetail;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.schema.SchemaImportValidationException;
import fr.gouv.vitam.functional.administration.common.schema.ErrorReportSchema;
import fr.gouv.vitam.functional.administration.common.schema.Schema;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.ontologies.OntologyService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.apache.commons.collections4.CollectionUtils;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.functional.administration.core.schema.SchemaService.TenantScope.ALL_TENANTS;
import static fr.gouv.vitam.functional.administration.core.schema.SchemaService.TenantScope.CURRENT_TENANT;
import static fr.gouv.vitam.functional.administration.core.schema.SchemaService.TenantScope.INCLUDE_ADMIN_TENANT;

/**
 * This service to manage schema operations
 */
public class SchemaService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchemaService.class);

    private static final String COLLECTION_UNIT = "Unit";
    private static final String COLLECTION_OBJECTGROUP = "ObjectGroup";

    private static final String SCHEMA_JSON_IS_MANDATORY_PARAMETER =
        "The json input of external schema type is mandatory";
    public static final String SCHEMA_IMPORT_EVENT = "IMPORT_EXTERNAL_SCHEMA";

    private static final String SCHEMA_REPORT = "SCHEMA_REPORT";
    static final String BACKUP_SCHEMA_EVENT = "BACKUP_SCHEMA";

    public static final String DELETE_SCHEMA_EVENT = "DELETE_EXTERNAL_SCHEMA";

    public static final String VITAM_UNIT_INTERNAL_SCHEMA_JSON = "vitam-unit-internal-schema.json";
    public static final String VITAM_OBJECT_GROUP_INTERNAL_SCHEMA_JSON = "vitam-object-group-internal-schema.json";

    private final MongoDbAccessAdminImpl mongoDbAccessReferential;

    private final FunctionalBackupService functionalBackupService;

    private final OntologyService ontologyService;

    private final SchemaValidationService schemaValidationService;

    /**
     * SchemaService Constructor
     */

    public SchemaService(
        final MongoDbAccessAdminImpl mongoDbAccessReferential,
        final FunctionalBackupService functionalBackupService,
        final OntologyService ontologyService
    ) {
        this(
            mongoDbAccessReferential,
            functionalBackupService,
            ontologyService,
            LogbookOperationsClientFactory.getInstance()
        );
    }

    @VisibleForTesting
    public SchemaService(
        final MongoDbAccessAdminImpl mongoDbAccessReferential,
        final FunctionalBackupService functionalBackupService,
        final OntologyService ontologyService,
        final LogbookOperationsClientFactory logbookOperationsClientFactory
    ) {
        this.ontologyService = ontologyService;
        this.mongoDbAccessReferential = mongoDbAccessReferential;
        this.functionalBackupService = functionalBackupService;
        this.schemaValidationService = new SchemaValidationService(
            mongoDbAccessReferential,
            logbookOperationsClientFactory
        );
    }

    /**
     * Retrieve unit external schema
     *
     * @return the list of external  schema for unit collection
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     * @throws InvalidCreateOperationException
     */
    private List<SchemaResponse> findUnitExternalSchema(TenantScope tenantScope)
        throws InvalidParseOperationException, ReferentialException, InvalidCreateOperationException {
        LOGGER.info("retrieving unit external schema ");
        return decorateSchema(mapSchemaDbEntityToModel(loadCurrentExternalSchema(tenantScope)), COLLECTION_UNIT);
    }

    /**
     * Retrieve the schema list for unit collection
     *
     * @return schema list for unit collection
     * @throws InvalidParseOperationException
     * @throws IOException
     */
    public List<SchemaResponse> findUnitInternalSchema()
        throws InvalidParseOperationException, IOException, ReferentialException {
        LOGGER.info("retrieving internal unit schema ");
        return loadUnitInternalSchema();
    }

    /**
     * Retrieve internal and external schema list
     *
     * @return
     * @throws InvalidParseOperationException
     * @throws IOException
     * @throws ReferentialException
     * @throws InvalidCreateOperationException
     */
    public List<SchemaResponse> findUnitSchema()
        throws InvalidParseOperationException, IOException, ReferentialException, InvalidCreateOperationException {
        LOGGER.info("retrieving internal and external unit schema ");
        return Stream.concat(
            loadUnitInternalSchema().stream(),
            findUnitExternalSchema(INCLUDE_ADMIN_TENANT).stream()
        ).collect(Collectors.toList());
    }

    /**
     * Retrieve Object group schema list
     *
     * @return
     * @throws InvalidParseOperationException
     * @throws IOException
     */
    public List<SchemaResponse> findObjectGroupInternalSchema()
        throws InvalidParseOperationException, IOException, ReferentialException {
        LOGGER.info("retrieving ObjectGroup schema ");
        InputStream isObjectGroupInternalSchema = loadObjectGroupInternalSchema();
        List<SchemaResponse> objectGroupSchemaModels = JsonHandler.getFromInputStreamAsTypeReference(
            isObjectGroupInternalSchema,
            new TypeReference<>() {}
        );
        return decorateSchema(objectGroupSchemaModels, COLLECTION_OBJECTGROUP);
    }

    /**
     * Import external schema on current tenant
     *
     * @param externalSchemaList
     * @return
     * @throws VitamException
     */
    public RequestResponse<SchemaModel> importExternalSchemaElements(List<SchemaInputModel> externalSchemaList)
        throws VitamException {
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID importExternalSchemaOpId = GUIDReader.getGUID(operationId);
        SchemaImportReport schemaImportReport = SchemaCommonService.initSchemaImportReport(importExternalSchemaOpId);
        Map<String, List<ErrorReportSchema>> importErrors = new HashMap<>();
        try {
            ParametersChecker.checkParameter(SCHEMA_JSON_IS_MANDATORY_PARAMETER, externalSchemaList);
            schemaValidationService.startLogBook(importExternalSchemaOpId, SCHEMA_IMPORT_EVENT);

            List<OntologyModel> ontologiesElts = loadFullOntologiesElts();
            Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologiesElts
                .stream()
                .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));

            List<SchemaResponse> currentUnitSchemaList = new ArrayList<>(loadUnitInternalSchema());
            currentUnitSchemaList.addAll(this.findUnitExternalSchema(INCLUDE_ADMIN_TENANT));

            schemaValidationService.validateExternalSchemaInputs(
                externalSchemaList,
                currentUnitSchemaList,
                ontologyEltsMapByIdentifier,
                importErrors
            );

            List<Schema> dbSchemaModelList = SchemaCommonService.mapSchemaFromInputParameters(
                externalSchemaList,
                ontologyEltsMapByIdentifier
            );
            persistImportedSchemaList(dbSchemaModelList);

            backupSchemaDatabaseToOffers(importExternalSchemaOpId);
            SchemaCommonService.fillSchemaImportReportOK(
                schemaImportReport,
                dbSchemaModelList,
                importExternalSchemaOpId
            );
            backupReport(schemaImportReport, importExternalSchemaOpId);
            schemaValidationService.logSuccessLogBook(importExternalSchemaOpId, SCHEMA_IMPORT_EVENT);
            return new RequestResponseOK<SchemaModel>().setHttpCode(Response.Status.CREATED.getStatusCode());
        } catch (SchemaImportValidationException e) {
            LOGGER.error(e);
            if (importErrors != null) {
                SchemaCommonService.fillSchemaImportReportError(
                    schemaImportReport,
                    importErrors.keySet(),
                    StatusCode.KO,
                    importExternalSchemaOpId
                );
                backupReport(schemaImportReport, importExternalSchemaOpId);
            }
            schemaValidationService.logValidationError(importExternalSchemaOpId, SCHEMA_IMPORT_EVENT, e.getMessage());

            return SchemaCommonService.getVitamError(
                VitamCode.SCHEMA_CHECK_ERROR.getItem(),
                "Error importing schema : " + e.getMessage(),
                StatusCode.KO
            ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (Exception e) {
            LOGGER.error(e);
            if (importErrors != null) {
                SchemaCommonService.fillSchemaImportReportError(
                    schemaImportReport,
                    importErrors.keySet(),
                    StatusCode.FATAL,
                    importExternalSchemaOpId
                );
                backupReport(schemaImportReport, importExternalSchemaOpId);
            }
            String errorDetails = e.getMessage();
            LOGGER.error("Validation errors on the input file {}", errorDetails);
            schemaValidationService.logError(importExternalSchemaOpId, SCHEMA_IMPORT_EVENT, null, errorDetails);
            return SchemaCommonService.getVitamError(
                VitamCode.SCHEMA_CHECK_ERROR.getItem(),
                "Error importing schema : " + errorDetails,
                StatusCode.KO
            ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    private List<SchemaResponse> mapSchemaDbEntityToModel(List<SchemaModel> currentExternalSchemaList) {
        return currentExternalSchemaList
            .stream()
            .map(schemaElt -> {
                final SchemaResponse schemaResponse = new SchemaResponse();
                schemaResponse.setPath(schemaElt.getPath());
                schemaResponse.setShortName(schemaElt.getShortName());
                schemaResponse.setCardinality(schemaElt.getCardinality());
                schemaResponse.setTenant(schemaElt.getTenant());
                schemaResponse.setCollection(schemaElt.getCollection());
                schemaResponse.setDescription(schemaElt.getDescription());
                schemaResponse.setCategory(SchemaCategory.OTHER);

                final String pathLeaf = SchemaCommonService.extractLeafFromPath(schemaElt.getPath());
                schemaResponse.setFieldName(pathLeaf);
                schemaResponse.setOrigin(schemaElt.getOrigin());
                if (Boolean.TRUE.equals(schemaElt.getObject())) {
                    schemaResponse.setType(SchemaType.OBJECT);
                }
                // If not an OBJECT, type will be defined from ontology
                return schemaResponse;
            })
            .collect(Collectors.toList());
    }

    /**
     * Retrieve external schema for Admin and current tenant
     *
     * @return
     * @throws InvalidCreateOperationException
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     */
    private List<SchemaModel> loadCurrentExternalSchema(TenantScope tenantScope)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException {
        Set<Integer> filteredTenants = new HashSet<>();
        switch (tenantScope) {
            case INCLUDE_ADMIN_TENANT:
                filteredTenants.add(VitamThreadUtils.getVitamSession().getTenantId());
                filteredTenants.add(VitamConfiguration.getAdminTenant());
                break;
            case CURRENT_TENANT:
                filteredTenants.add(VitamThreadUtils.getVitamSession().getTenantId());
                break;
            case ALL_TENANTS:
                filteredTenants.addAll(VitamConfiguration.getTenants());
                break;
        }

        RequestResponseOK<SchemaModel> schemasResponse = findExternalSchemaByQueryDsl(
            SchemaCommonService.buildDslQueryForExtractingSchema(filteredTenants, Collections.emptyList())
        );
        return schemasResponse.getResults();
    }

    /**
     * Save filtered schema list on DB
     *
     * @param externalSchemaList
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     * @throws SchemaImportValidationException
     * @throws DocumentAlreadyExistsException
     */
    private void persistImportedSchemaList(List<Schema> externalSchemaList)
        throws InvalidParseOperationException, ReferentialException, DocumentAlreadyExistsException, SchemaValidationException {
        DbRequestResult dbRequestResult = null;
        try {
            if (externalSchemaList.isEmpty()) {
                return;
            }
            ArrayNode schemaListToPersist = JsonHandler.createArrayNode();
            for (final Schema schemaModelToPersist : externalSchemaList) {
                final ObjectNode schemaModelToPersistNode = (ObjectNode) JsonHandler.toJsonNode(schemaModelToPersist);
                schemaListToPersist.add(schemaModelToPersistNode);
            }
            dbRequestResult = mongoDbAccessReferential.insertDocuments(
                schemaListToPersist,
                FunctionalAdminCollections.SCHEMA
            );
        } finally {
            if (dbRequestResult != null) {
                dbRequestResult.close();
            }
        }
    }

    private void backupSchemaDatabaseToOffers(GUID eip) throws VitamException {
        //Store collection
        functionalBackupService.saveCollectionAndSequence(
            eip,
            BACKUP_SCHEMA_EVENT,
            FunctionalAdminCollections.SCHEMA,
            eip.toString()
        );
    }

    /**
     * Retrieve the ontologies
     *
     * @return
     * @throws InvalidCreateOperationException
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     */
    private List<OntologyModel> loadFullOntologiesElts() throws ReferentialException, InvalidParseOperationException {
        RequestResponseOK<OntologyModel> ontologiesResponse = ontologyService.findOntologies(
            JsonHandler.createObjectNode()
        );
        return ontologiesResponse.getResults();
    }

    private List<SchemaResponse> loadUnitInternalSchema()
        throws IOException, InvalidParseOperationException, ReferentialException {
        LOGGER.info("loading internal schema from file vitam-unit-internal-schema.json ");
        final InputStream unitInternalSchemaInputStream = PropertiesUtils.getResourceAsStream(
            VITAM_UNIT_INTERNAL_SCHEMA_JSON
        );
        return decorateSchema(
            JsonHandler.getFromInputStreamAsTypeReference(unitInternalSchemaInputStream, new TypeReference<>() {}),
            COLLECTION_UNIT
        );
    }

    private List<SchemaResponse> decorateSchema(List<SchemaResponse> schemaResponses, String collection)
        throws ReferentialException, InvalidParseOperationException {
        Select select = new Select();
        try {
            select.setQuery(QueryHelper.in(OntologyModel.TAG_COLLECTIONS, collection));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException("Unable to create select query", e);
        }
        final Map<String, OntologyModel> mapOntologiesByIdentifier = ontologyService
            .findOntologies(select.getFinalSelect())
            .getResults()
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyModel -> ontologyModel));

        return schemaResponses
            .stream()
            .peek(schemaResponse -> {
                if (!SchemaType.OBJECT.equals(schemaResponse.getType())) {
                    final OntologyModel ontologyElt = mapOntologiesByIdentifier.get(schemaResponse.getFieldName());
                    if (ontologyElt == null) {
                        final String message = String.format("No ontology found for path %s", schemaResponse.getPath());
                        LOGGER.error(message);
                        throw new IllegalStateException(message);
                    }
                    final TypeDetail typeDetail = ontologyElt.getTypeDetail();
                    if (typeDetail == null) {
                        final String message = String.format(
                            "Ontology %s has no TypeDetail",
                            ontologyElt.getIdentifier()
                        );
                        LOGGER.error(message);
                        throw new IllegalStateException(message);
                    }
                    schemaResponse.setTypeDetail(SchemaTypeDetail.valueOf(typeDetail.getType()));
                    Optional.ofNullable(ontologyElt.getStringSize()).ifPresent(
                        stringSize -> schemaResponse.setStringSize(SchemaStringSizeType.valueOf(stringSize.getSize()))
                    );
                    schemaResponse.setDescription(ontologyElt.getDescription());
                    if (SchemaOrigin.EXTERNAL.equals(schemaResponse.getOrigin()) && schemaResponse.getType() == null) {
                        schemaResponse.setType(SchemaType.valueOf(ontologyElt.getType().getType()));
                    }
                }
            })
            .collect(Collectors.toList());
    }

    private InputStream loadObjectGroupInternalSchema() throws IOException {
        LOGGER.info("loading internal schema from file vitam-object-group-internal-schema.json ");
        return PropertiesUtils.getResourceAsStream(VITAM_OBJECT_GROUP_INTERNAL_SCHEMA_JSON);
    }

    /**
     * Save the report stream
     */
    private void backupReport(SchemaImportReport schemaImportReport, GUID eip) throws VitamException {
        try (InputStream reportInputStream = JsonHandler.writeToInpustream(schemaImportReport)) {
            final String fileName = eip + ".json";
            functionalBackupService.saveFile(reportInputStream, eip, SCHEMA_REPORT, DataCategory.REPORT, fileName);
        } catch (IOException | VitamException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private RequestResponseOK<SchemaModel> findExternalSchemaByQueryDsl(JsonNode queryDsl)
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

    public void checkAndDeleteExternalSchemaElementsByPaths(List<String> pathsToDelete, boolean includeAllTenant)
        throws InvalidCreateOperationException, IOException, VitamException {
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID deleteExternalSchemaOpId = GUIDReader.getGUID(operationId);

        try {
            /* Start logbook entry */
            schemaValidationService.startLogBook(deleteExternalSchemaOpId, DELETE_SCHEMA_EVENT);

            List<String> internalPaths = getInternalPaths(pathsToDelete);
            if (!internalPaths.isEmpty()) {
                throw new BadRequestException(
                    "Some paths cannot be deleted because they are internal: " + internalPaths
                );
            }

            List<String> nonExistingPaths = getNonExistingPaths(pathsToDelete);
            if (!nonExistingPaths.isEmpty()) {
                throw new BadRequestException(
                    "Some paths cannot be deleted because they do not exist: " + nonExistingPaths
                );
            }

            final List<SchemaModel> unitExternalSchemas = loadCurrentExternalSchema(
                includeAllTenant ? ALL_TENANTS : CURRENT_TENANT
            );

            List<String> existingPaths = unitExternalSchemas.stream().map(SchemaModel::getPath).toList();

            List<String> nonDeletablePaths = pathsToDelete
                .stream()
                .filter(
                    pathToDelete ->
                        existingPaths
                            .stream()
                            .anyMatch(
                                existingPath ->
                                    existingPath.startsWith(pathToDelete) && !pathsToDelete.contains(existingPath)
                            )
                )
                .toList();

            if (!nonDeletablePaths.isEmpty()) {
                throw new BadRequestException(
                    "Some paths cannot be deleted because they are referenced by other schemas:"
                );
            } else {
                LOGGER.debug("All selected paths are deletable.");
                deleteExternalSchemaElementsByPaths(pathsToDelete);
            }

            /* Mark deletion as successful */
            schemaValidationService.logSuccessLogBook(deleteExternalSchemaOpId, DELETE_SCHEMA_EVENT);
        } catch (Exception e) {
            /* Mark deletion as failed */
            String errorDetails = e.getMessage();
            LOGGER.error("Unable to perform delete: {}", errorDetails);
            schemaValidationService.logError(deleteExternalSchemaOpId, DELETE_SCHEMA_EVENT, null, errorDetails);
            throw e;
        }
    }

    private List<String> getInternalPaths(List<String> pathsToDelete)
        throws ReferentialException, IOException, InvalidParseOperationException {
        final List<SchemaResponse> unitInternalSchema = loadUnitInternalSchema();

        List<String> internalPaths = unitInternalSchema
            .stream()
            .map(SchemaResponse::getPath)
            .collect(Collectors.toList());

        List<String> internalPathsToDelete = pathsToDelete
            .stream()
            .filter(internalPaths::contains)
            .collect(Collectors.toList());

        return internalPathsToDelete;
    }

    private List<String> getNonExistingPaths(List<String> pathsToDelete)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException {
        final List<SchemaModel> unitExternalSchemas = loadCurrentExternalSchema(CURRENT_TENANT);

        List<String> existingPaths = unitExternalSchemas
            .stream()
            .map(SchemaModel::getPath)
            .collect(Collectors.toList());

        List<String> nonExistingPaths = pathsToDelete
            .stream()
            .filter(pathToDelete -> !existingPaths.contains(pathToDelete))
            .collect(Collectors.toList());

        return nonExistingPaths;
    }

    private void deleteExternalSchemaElementsByPaths(List<String> schemaPathsToDelete)
        throws BadRequestException, ReferentialException {
        final Delete delete = new Delete();

        try {
            if (CollectionUtils.isNotEmpty(schemaPathsToDelete)) {
                delete.setQuery(QueryHelper.in(SchemaModel.TAG_PATH, schemaPathsToDelete.toArray(String[]::new)));
                mongoDbAccessReferential.deleteDocument(delete.getFinalDelete(), FunctionalAdminCollections.SCHEMA);
            }
        } catch (final SchemaValidationException | InvalidCreateOperationException e) {
            throw new BadRequestException(e);
        }
    }

    public enum TenantScope {
        INCLUDE_ADMIN_TENANT,
        CURRENT_TENANT,
        ALL_TENANTS,
    }
}
