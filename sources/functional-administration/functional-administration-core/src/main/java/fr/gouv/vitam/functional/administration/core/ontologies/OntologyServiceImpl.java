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
package fr.gouv.vitam.functional.administration.core.ontologies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.FindIterable;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamErrorMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ModelConstants;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyOrigin;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.functional.administration.common.ErrorReportOntologies;
import fr.gouv.vitam.functional.administration.common.Ontology;
import fr.gouv.vitam.functional.administration.common.OntologyErrorCode;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.exception.OntologyInternalExternalConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.ADDITIONAL_INFORMATION;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.CODE;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.ERROR;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.EV_DATE_TIME;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.EV_ID;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.EV_TYPE;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.JDO_DISPLAY;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.MESSAGE;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.OUT_MESSG;

/**
 * The implementation of the Ontology CRUD service
 */
public class OntologyServiceImpl implements OntologyService {

    static final String BACKUP_ONTOLOGY_EVENT = "STP_BACKUP_ONTOLOGY";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OntologyServiceImpl.class);
    private static final Integer ADMIN_TENANT = VitamConfiguration.getAdminTenant();
    private static final String IDENTIFIER = "IDENTIFIER";
    private static final String CTR_SCHEMA = "CTR_SCHEMA";
    private static final String ONTOLOGY_IMPORT_EVENT = "IMPORT_ONTOLOGY";
    private static final String CHECK_ONTOLOGY_IMPORT_EVENT = "CHECK_ONTOLOGY_IMPORT";

    private static final String ONTOLOGIES_IS_MANDATORY_PARAMETER = "ontologies parameter is mandatory";

    private static final String ONTOLOGIES_REPORT = "ONTOLOGY_REPORT";

    private static final String DELETED_ONTOLOGIES = "deletedOntologies";
    private static final String UPDATED_ONTOLOGIES = "updatedOntologies";
    private static final String CREATED_ONTOLOGIES = "createdOntologies";
    private static final Pattern INVALID_IDENTIFIER_PATTERN = Pattern.compile("^[_#\\s]|\\s");
    private static final String UND_TENANT = "_tenant";
    private static final String UND_ID = "_id";
    private static final Map<OntologyType, List<OntologyType>> typeMap = getOntologyTypeMap();
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final FunctionalBackupService functionalBackupService;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     * @param functionalBackupService the functional backup service
     */
    public OntologyServiceImpl(MongoDbAccessAdminImpl mongoAccess, FunctionalBackupService functionalBackupService) {
        this(mongoAccess, functionalBackupService, LogbookOperationsClientFactory.getInstance());
    }

    @VisibleForTesting
    public OntologyServiceImpl(
        MongoDbAccessAdminImpl mongoAccess,
        FunctionalBackupService functionalBackupService,
        LogbookOperationsClientFactory logbookOperationsClientFactory
    ) {
        this.mongoAccess = mongoAccess;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.functionalBackupService = functionalBackupService;
    }

    private static Map<OntologyType, List<OntologyType>> getOntologyTypeMap() {
        EnumMap<OntologyType, List<OntologyType>> ontologyTypeMap = new EnumMap<>(OntologyType.class);

        ontologyTypeMap.put(OntologyType.TEXT, Collections.singletonList(OntologyType.KEYWORD));
        ontologyTypeMap.put(OntologyType.KEYWORD, Collections.singletonList(OntologyType.TEXT));
        ontologyTypeMap.put(OntologyType.DATE, Arrays.asList(OntologyType.KEYWORD, OntologyType.TEXT));
        ontologyTypeMap.put(OntologyType.LONG, Collections.emptyList());
        ontologyTypeMap.put(OntologyType.DOUBLE, Collections.emptyList());
        ontologyTypeMap.put(OntologyType.BOOLEAN, Collections.emptyList());
        ontologyTypeMap.put(OntologyType.GEO_POINT, Arrays.asList(OntologyType.KEYWORD, OntologyType.TEXT));
        ontologyTypeMap.put(OntologyType.ENUM, Arrays.asList(OntologyType.KEYWORD, OntologyType.TEXT));
        return ontologyTypeMap;
    }

    @Override
    public RequestResponse<OntologyModel> importOntologies(boolean forceUpdate, List<OntologyModel> ontologyModelList)
        throws VitamException, IOException {
        return importOntologies(forceUpdate, ontologyModelList, false);
    }

    @Override
    public RequestResponse<OntologyModel> importInternalOntologies(List<OntologyModel> ontologyInternalModelList)
        throws VitamException, IOException {
        if (isExternalOntologies(ontologyInternalModelList)) {
            final String error = "Import ontologies error : The imported ontologies contains EXTERNAL fields.";
            LOGGER.error(error);

            return getVitamError(VitamCode.ONTOLOGY_IMPORT_ERROR.getItem(), error, StatusCode.KO).setHttpCode(
                Response.Status.BAD_REQUEST.getStatusCode()
            );
        }

        return importOntologies(true, ontologyInternalModelList, true);
    }

    private RequestResponse<OntologyModel> importOntologies(
        boolean forceUpdate,
        List<OntologyModel> ontologyModelList,
        boolean externalOntologyUpdate
    ) throws VitamException, IOException {
        ParametersChecker.checkParameter(ONTOLOGIES_IS_MANDATORY_PARAMETER, ontologyModelList);

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();

        GUID eip = GUIDReader.getGUID(operationId);

        Map<String, List<ErrorReportOntologies>> errors = new HashMap<>();
        OntologyManager manager = new OntologyManager(logbookOperationsClientFactory, eip, errors);
        manager.logStarted(ONTOLOGY_IMPORT_EVENT, null);

        try {
            VitamError checkResults = ontologyCommonChecks(
                ontologyModelList,
                externalOntologyUpdate,
                eip,
                errors,
                manager,
                false
            );

            if (checkResults != null) {
                return checkResults;
            }

            // Load current ontology from DB
            List<OntologyModel> actualOntologies = this.selectOntologies();

            List<OntologyModel> actualInternals = actualOntologies
                .stream()
                .filter(om -> om.getOrigin().equals(OntologyOrigin.INTERNAL))
                .collect(Collectors.toList());

            Map<String, OntologyModel> actualOntologiesMap = actualOntologies
                .stream()
                .collect(Collectors.toMap(OntologyModel::getIdentifier, item -> item));

            List<OntologyModel> toDelete = new ArrayList<>();
            List<OntologyModel> toCreate = new ArrayList<>();
            List<OntologyModel> toUpdate = new ArrayList<>();

            Set<String> importedOntologyIdentifiers = new HashSet<>();
            // Check if the ontologies must be created or updated
            for (final OntologyModel ontm : ontologyModelList) {
                // if the ontology in json file already exists in db, it will be updated, otherwise it will be created
                if (actualOntologiesMap.containsKey(ontm.getIdentifier())) {
                    toUpdate.add(ontm);
                } else {
                    toCreate.add(ontm);
                }
                importedOntologyIdentifiers.add(ontm.getIdentifier());
            }

            // when upgrade only : iterate over internal ontologies only to detect if thereis some items to delete
            if (externalOntologyUpdate) {
                for (final OntologyModel actualTenantOntology : actualInternals) {
                    /* if the internal ontologies in db doesn't exist in the json file anymore, it will be deleted
                    preserve the external ones though
                    */
                    if (!importedOntologyIdentifiers.contains(actualTenantOntology.getIdentifier())) {
                        toDelete.add(actualTenantOntology);
                    }
                }
            } else {
                // otherwise iterate over all and delete the missing ones.
                for (final OntologyModel actualTenantOntology : actualOntologies) {
                    //if the ontology in db doesn't exist in the json file anymore, it will be deleted
                    if (!importedOntologyIdentifiers.contains(actualTenantOntology.getIdentifier())) {
                        toDelete.add(actualTenantOntology);
                    }
                }
            }

            // Coherence checks (if no forced update)
            if (!forceUpdate) {
                // Type change validation of updated ontologies
                checkTypeChangeCompatibility(toUpdate, actualOntologiesMap, manager, errors);

                // Check
                checkUsedByArchiveUnitProfileValidator(toDelete, manager, errors);

                //ontologies to delete validation
                checkInternalFieldDelete(toDelete, manager, errors);
            }

            VitamError coherenceErrors = abortOnErrors(eip, errors, manager);
            if (coherenceErrors != null) {
                return coherenceErrors;
            }

            // If no errors are found, we create/update / delete the documents
            commitToDatabase(actualOntologiesMap, toDelete, toCreate, toUpdate);

            backupDatabaseToOffers(eip);

            InputStream errorStream = generateReportOK(toCreate, toDelete, toUpdate, eip);

            backupReport(errorStream, eip);
        } catch (SchemaValidationException e) {
            LOGGER.error(e);
            final String err = "Import ontologies schema error > " + e.getMessage();
            // logbook error event

            Map<String, List<ErrorReportOntologies>> exception = new HashMap<>();
            ErrorReportOntologies errorReport = new ErrorReportOntologies(
                OntologyErrorCode.STP_IMPORT_ONTOLOGIES_EXCEPTION,
                "",
                err,
                null
            );
            exception.put(err, Collections.singletonList(errorReport));
            InputStream errorStream = generateErrorReport(exception, StatusCode.KO, eip);
            manager.logValidationError(CTR_SCHEMA, null, err);
            backupReport(errorStream, eip);
            manager.logFatalError(ONTOLOGY_IMPORT_EVENT, null, null);
            return getVitamError(VitamCode.ONTOLOGY_VALIDATION_ERROR.getItem(), err, StatusCode.KO).setHttpCode(
                Response.Status.BAD_REQUEST.getStatusCode()
            );
        } catch (OntologyInternalExternalConflictException e) {
            LOGGER.error(e);
            String error = "Import/upgrade ontologies error : ";
            final String err = error + e.getMessage();
            Map<String, List<ErrorReportOntologies>> exception = new HashMap<>();
            ErrorReportOntologies errorReport = new ErrorReportOntologies(
                OntologyErrorCode.STP_IMPORT_ONTOLOGIES_INTERNAL_EXTERNAL_CONFLICT_EXCEPTION,
                "the merge between internal and external ontologies didn't succeed, correct the conflict",
                err,
                null
            );
            exception.put(error, Collections.singletonList(errorReport));
            InputStream errorStream = generateErrorReport(exception, StatusCode.KO, eip);
            backupReport(errorStream, eip);
            manager.logFatalError(ONTOLOGY_IMPORT_EVENT, null, err);
            return getVitamError(VitamCode.ONTOLOGY_IMPORT_ERROR.getItem(), err, StatusCode.KO).setHttpCode(
                Response.Status.BAD_REQUEST.getStatusCode()
            );
        } catch (Exception e) {
            LOGGER.error(e);
            final String err = "Import ontologies error : " + e.getMessage();
            Map<String, List<ErrorReportOntologies>> exception = new HashMap<>();
            ErrorReportOntologies errorReport = new ErrorReportOntologies(
                OntologyErrorCode.STP_IMPORT_ONTOLOGIES_EXCEPTION,
                "",
                err,
                null
            );
            exception.put(err, Collections.singletonList(errorReport));
            InputStream errorStream = generateErrorReport(exception, StatusCode.KO, eip);
            backupReport(errorStream, eip);
            manager.logFatalError(ONTOLOGY_IMPORT_EVENT, null, err);
            return getVitamError(VitamCode.ONTOLOGY_IMPORT_ERROR.getItem(), err, StatusCode.KO).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
            );
        }

        manager.logSuccess(ONTOLOGY_IMPORT_EVENT, null, null);

        return new RequestResponseOK<OntologyModel>()
            .addAllResults(ontologyModelList)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse<OntologyModel> checkUpgradeOntologies(List<OntologyModel> ontologyInternalModelList)
        throws VitamException {
        if (isExternalOntologies(ontologyInternalModelList)) {
            LOGGER.error("Import ontologies error : The imported ontologies contains EXTERNAL fields.");
            return new RequestResponseOK().setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
        return check(ontologyInternalModelList);
    }

    private RequestResponse<OntologyModel> check(List<OntologyModel> ontologyInternalModelList) throws VitamException {
        ParametersChecker.checkParameter(ONTOLOGIES_IS_MANDATORY_PARAMETER, ontologyInternalModelList);

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();

        GUID eip = GUIDReader.getGUID(operationId);

        Map<String, List<ErrorReportOntologies>> errors = new HashMap<>();
        OntologyManager manager = new OntologyManager(logbookOperationsClientFactory, eip, errors);
        // trace the event in logbook
        manager.logStarted(CHECK_ONTOLOGY_IMPORT_EVENT, null);
        try {
            VitamError checkResults = ontologyCommonChecks(ontologyInternalModelList, true, eip, errors, manager, true);

            if (checkResults != null) {
                return checkResults;
            }
        } catch (SchemaValidationException e) {
            LOGGER.error(e);
            manager.logValidationError(CTR_SCHEMA, null, e.getMessage());
            return getVitamError(
                VitamCode.ONTOLOGY_CHECK_ERROR.getItem(),
                "Import ontology errors : " + e.getMessage(),
                StatusCode.KO
            ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (OntologyInternalExternalConflictException e) {
            LOGGER.error(e);
            manager.logValidationError(CHECK_ONTOLOGY_IMPORT_EVENT, null, e.getMessage());
            return getVitamError(
                VitamCode.ONTOLOGY_CHECK_ERROR.getItem(),
                "Import ontology errors : " + e.getMessage(),
                StatusCode.KO
            ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (Exception e) {
            LOGGER.error(e);
            manager.logFatalError(CHECK_ONTOLOGY_IMPORT_EVENT, null, "Import ontologies error : " + e.getMessage());
            return getVitamError(
                VitamCode.ONTOLOGY_CHECK_ERROR.getItem(),
                "Import ontology errors : " + e.getMessage(),
                StatusCode.KO
            ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }

        manager.logSuccess(CHECK_ONTOLOGY_IMPORT_EVENT, null, null);
        return new RequestResponseOK<OntologyModel>()
            .addAllResults(ontologyInternalModelList)
            .setHttpCode(Response.Status.OK.getStatusCode());
    }

    private VitamError ontologyCommonChecks(
        List<OntologyModel> ontologyModelList,
        boolean externalOntologyUpdate,
        GUID eip,
        Map<String, List<ErrorReportOntologies>> errors,
        OntologyManager manager,
        boolean check
    ) throws VitamException, IOException {
        // Access permissions checks
        checkAdminTenant(manager, errors);
        VitamError tenantValidationErrors = check
            ? exitCheckOnErrors(errors, manager)
            : abortOnErrors(eip, errors, manager);

        if (tenantValidationErrors != null) {
            return tenantValidationErrors;
        }

        // Request validation checks
        validateRequest(ontologyModelList, errors, manager);

        VitamError requestValidationErrors = check
            ? exitCheckOnErrors(errors, manager)
            : abortOnErrors(eip, errors, manager);

        if (requestValidationErrors != null) {
            return requestValidationErrors;
        }

        // Load current ontology from DB
        List<OntologyModel> actualOntologies = this.selectOntologies();

        if (externalOntologyUpdate) {
            // initializing ontology at the first begining of install/upgrade, that's include handling external items if there is some
            List<OntologyModel> actualExternalOntologies = actualOntologies
                .stream()
                .filter(om -> om.getOrigin().equals(OntologyOrigin.EXTERNAL))
                .collect(Collectors.toList());

            // if there is external ontologies so we handle their merge with the actual list being imported.
            checkInternalWithExistingExternalConflict(ontologyModelList, actualExternalOntologies);
        }
        return null;
    }

    private boolean isExternalOntologies(List<OntologyModel> ontologyInternalModelList) {
        return ontologyInternalModelList.stream().anyMatch(oM -> oM.getOrigin().equals(OntologyOrigin.EXTERNAL));
    }

    private void checkInternalWithExistingExternalConflict(
        List<OntologyModel> ontologyModelList,
        List<OntologyModel> actualExternalOntologies
    ) throws OntologyInternalExternalConflictException {
        // Check that there is no internal ontology fields name conflict with already existing external ones.
        Map<String, OntologyModel> currentImportedOntologiesMap = ontologyModelList
            .stream()
            .collect(Collectors.toMap(oM -> oM.getIdentifier().toLowerCase(), item -> item));

        List<OntologyModel> conflictModels = new ArrayList<>();
        List<OntologyModel> existingModels = new ArrayList<>();
        for (final OntologyModel ontm : actualExternalOntologies) {
            if (ParametersChecker.isNotEmpty(ontm.getIdentifier())) {
                // if the ontology in json file contains some external ontologies already existed in the database so the merge is problematic !
                if (currentImportedOntologiesMap.containsKey(ontm.getIdentifier().trim().toLowerCase())) {
                    conflictModels.add(currentImportedOntologiesMap.get(ontm.getIdentifier().toLowerCase()));
                    existingModels.add(ontm);
                }
            }
            if (!conflictModels.isEmpty()) {
                String message = String.format(
                    "There is conflict between Ontologies being imported and those already exists in database : expected =  %s  but found = %s ",
                    conflictModels.stream().map(Objects::toString).collect(Collectors.joining(", ")),
                    existingModels.stream().map(Objects::toString).collect(Collectors.joining(", "))
                );
                throw new OntologyInternalExternalConflictException(message);
            }
        }
    }

    private VitamError exitCheckOnErrors(Map<String, List<ErrorReportOntologies>> errors, OntologyManager manager)
        throws VitamException {
        VitamError vitamError = null;
        if (!errors.isEmpty()) {
            String errorsDetails = errors
                .entrySet()
                .stream()
                .map(c -> c.getKey() + " : " + c.getValue().get(0).getMessage())
                .collect(Collectors.joining(","));

            manager.logValidationError(ONTOLOGY_IMPORT_EVENT, null, errorsDetails);

            vitamError = getVitamError(
                VitamCode.ONTOLOGY_CHECK_ERROR.getItem(),
                "Check ontology errors : " + errorsDetails,
                StatusCode.KO
            ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
        return vitamError;
    }

    private VitamError abortOnErrors(
        GUID eip,
        Map<String, List<ErrorReportOntologies>> errors,
        OntologyManager manager
    ) throws VitamException, IOException {
        VitamError vitamError = null;
        if (!errors.isEmpty()) {
            String errorsDetails = errors
                .entrySet()
                .stream()
                .map(c -> c.getKey() + " : " + c.getValue().get(0).getMessage())
                .collect(Collectors.joining(","));
            InputStream errorStream = generateErrorReport(errors, StatusCode.OK, eip);
            backupReport(errorStream, eip);
            manager.logValidationError(ONTOLOGY_IMPORT_EVENT, null, errorsDetails);

            vitamError = getVitamError(
                VitamCode.ONTOLOGY_IMPORT_ERROR.getItem(),
                "Global import ontology error",
                StatusCode.KO
            ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        }
        return vitamError;
    }

    private void checkAdminTenant(OntologyManager manager, Map<String, List<ErrorReportOntologies>> errors) {
        if (ADMIN_TENANT.equals(ParameterHelper.getTenantParameter())) {
            return;
        }

        manager.addError(
            VitamDocument.ID,
            new ErrorReportOntologies(
                OntologyErrorCode.STP_IMPORT_ONTOLOGIES_NOT_AUTHORIZED_FOR_TENANT,
                OntologyModel.TAG_IDENTIFIER,
                "Ontology import not authorized for tenant",
                null
            ),
            errors
        );
    }

    private void validateRequest(
        List<OntologyModel> ontologyModelList,
        Map<String, List<ErrorReportOntologies>> errors,
        OntologyManager manager
    ) {
        // Check format
        checkForbiddenFields(ontologyModelList, manager, errors);

        checkMandatoryFields(ontologyModelList, manager, errors);

        checkInvalidFieldFormat(ontologyModelList, manager, errors);

        // Check duplicates
        checkDuplicates(ontologyModelList, errors, manager);
    }

    private void checkForbiddenFields(
        List<OntologyModel> ontologyModelList,
        OntologyManager manager,
        Map<String, List<ErrorReportOntologies>> errors
    ) {
        for (OntologyModel ontology : ontologyModelList) {
            if (null != ontology.getId()) {
                manager.addError(
                    ontology.getIdentifier(),
                    new ErrorReportOntologies(
                        OntologyErrorCode.STP_IMPORT_ONTOLOGIES_ID_NOT_ALLOWED_IN_CREATE,
                        ModelConstants.TAG_ID,
                        "Forbidden field " + ModelConstants.TAG_ID,
                        ontology
                    ),
                    errors
                );
            }
        }
    }

    private void checkMandatoryFields(
        List<OntologyModel> ontologyModelList,
        OntologyManager manager,
        Map<String, List<ErrorReportOntologies>> errors
    ) {
        for (OntologyModel ontology : ontologyModelList) {
            if (ontology.getIdentifier() == null || ontology.getIdentifier().isEmpty()) {
                manager.addError(
                    ontology.getIdentifier(),
                    new ErrorReportOntologies(
                        OntologyErrorCode.STP_IMPORT_ONTOLOGIES_MISSING_INFORMATION,
                        Ontology.IDENTIFIER,
                        "The field " + Ontology.IDENTIFIER + " is mandatory",
                        ontology
                    ),
                    errors
                );
            }

            if (ontology.getType() == null) {
                manager.addError(
                    ontology.getIdentifier(),
                    new ErrorReportOntologies(
                        OntologyErrorCode.STP_IMPORT_ONTOLOGIES_MISSING_INFORMATION,
                        Ontology.TYPE,
                        "The field " + Ontology.TYPE + " is mandatory",
                        ontology
                    ),
                    errors
                );
            }

            if (ontology.getOrigin() == null) {
                manager.addError(
                    ontology.getIdentifier(),
                    new ErrorReportOntologies(
                        OntologyErrorCode.STP_IMPORT_ONTOLOGIES_MISSING_INFORMATION,
                        Ontology.ORIGIN,
                        "The field " + Ontology.ORIGIN + " is mandatory",
                        ontology
                    ),
                    errors
                );
            }
        }
    }

    private void checkInvalidFieldFormat(
        List<OntologyModel> ontologyModelList,
        OntologyManager manager,
        Map<String, List<ErrorReportOntologies>> errors
    ) {
        for (OntologyModel ontology : ontologyModelList) {
            // Identifier format (EXTERNAL)
            if (
                ontology.getOrigin() == OntologyOrigin.EXTERNAL &&
                ParametersChecker.isNotEmpty(ontology.getIdentifier())
            ) {
                final Matcher matcher = INVALID_IDENTIFIER_PATTERN.matcher(ontology.getIdentifier());
                if (matcher.find()) {
                    manager.addError(
                        ontology.getIdentifier(),
                        new ErrorReportOntologies(
                            OntologyErrorCode.STP_IMPORT_ONTOLOGIES_INVALID_PARAMETER,
                            OntologyModel.TAG_IDENTIFIER,
                            "Invalid field " + OntologyModel.TAG_IDENTIFIER,
                            ontology
                        ),
                        errors
                    );
                }
            }

            // Creation date
            String now = LocalDateUtil.nowFormatted();

            if (ontology.getCreationdate() == null || ontology.getCreationdate().trim().isEmpty()) {
                ontology.setCreationdate(now);
            } else {
                try {
                    ontology.setCreationdate(LocalDateUtil.getFormattedDateTimeForMongo(ontology.getCreationdate()));
                } catch (Exception e) {
                    LOGGER.error("Error ontology parse dates", e);

                    manager.addError(
                        ontology.getIdentifier(),
                        new ErrorReportOntologies(
                            OntologyErrorCode.STP_IMPORT_ONTOLOGIES_INVALID_PARAMETER,
                            OntologyModel.CREATION_DATE,
                            "Invalid field " + OntologyModel.CREATION_DATE,
                            ontology
                        ),
                        errors
                    );
                }
            }

            ontology.setLastupdate(now);
        }
    }

    private void checkDuplicates(
        List<OntologyModel> ontologyModelList,
        Map<String, List<ErrorReportOntologies>> errors,
        OntologyManager manager
    ) {
        // Check duplicates
        final Set<String> ontologyIdentifiers = new HashSet<>();
        for (final OntologyModel ontm : ontologyModelList) {
            // if an ontology with the same identifier or the same sedaField is already treated in ontologyModelList, then mark the current one as duplicated
            if (ParametersChecker.isNotEmpty(ontm.getIdentifier())) {
                if (ontologyIdentifiers.contains(ontm.getIdentifier().trim().toLowerCase())) {
                    manager.addError(
                        ontm.getIdentifier(),
                        new ErrorReportOntologies(
                            OntologyErrorCode.STP_IMPORT_ONTOLOGIES_IDENTIFIER_ALREADY_IN_ONTOLOGY,
                            OntologyModel.TAG_IDENTIFIER,
                            "Ontology identifier " + ontm.getIdentifier() + " already exists in the json file",
                            ontm
                        ),
                        errors
                    );
                } else {
                    ontologyIdentifiers.add(ontm.getIdentifier().trim().toLowerCase());
                    if (ontm.getSedaField() != null) {
                        ontologyIdentifiers.add(ontm.getSedaField().trim().toLowerCase());
                    }
                }
            }
        }
    }

    private void checkTypeChangeCompatibility(
        List<OntologyModel> toUpdate,
        Map<String, OntologyModel> currentOntologiesMap,
        OntologyManager manager,
        Map<String, List<ErrorReportOntologies>> errors
    ) {
        for (final OntologyModel ontm : toUpdate) {
            OntologyModel modelInDb = currentOntologiesMap.get(ontm.getIdentifier());
            OntologyType typeInDb = modelInDb.getType();
            OntologyType newType = ontm.getType();
            if (!newType.name().equalsIgnoreCase(typeInDb.toString())) {
                List<OntologyType> compatibleTypes = typeMap.get(typeInDb);
                if (!compatibleTypes.contains(newType)) {
                    manager.addError(
                        ontm.getIdentifier(),
                        new ErrorReportOntologies(
                            OntologyErrorCode.STP_IMPORT_ONTOLOGIES_UPDATE_INVALID_TYPE,
                            OntologyModel.TAG_TYPE,
                            "Change of type from " + typeInDb + " to " + newType + " is not possible",
                            ontm
                        ),
                        errors
                    );
                }
            }
        }
    }

    private void checkInternalFieldDelete(
        List<OntologyModel> toDelete,
        OntologyManager manager,
        Map<String, List<ErrorReportOntologies>> errors
    ) {
        for (final OntologyModel ontm : toDelete) {
            if (OntologyOrigin.INTERNAL.equals(ontm.getOrigin())) {
                manager.addError(
                    ontm.getIdentifier(),
                    new ErrorReportOntologies(
                        OntologyErrorCode.STP_IMPORT_ONTOLOGIES_DELETE_NOT_AUTHORIZED,
                        OntologyModel.TAG_IDENTIFIER,
                        "header \"" + GlobalDataRest.FORCE_UPDATE + "\" must be true",
                        ontm
                    ),
                    errors
                );
            }
        }
    }

    private void checkUsedByArchiveUnitProfileValidator(
        List<OntologyModel> toDelete,
        OntologyManager manager,
        Map<String, List<ErrorReportOntologies>> errors
    ) {
        // Select archive unit profiles
        List<ArchiveUnitProfile> archiveUnitProfiles = selectArchiveUnitProfiles();

        // Map archive unit profiles by fields
        MultiValuedMap<String, ArchiveUnitProfile> archiveUnitProfileByFieldName = new ArrayListValuedHashMap<>();
        for (ArchiveUnitProfile archiveUnitProfileModel : archiveUnitProfiles) {
            for (String field : archiveUnitProfileModel.getList(
                ArchiveUnitProfileModel.FIELDS,
                String.class,
                Collections.emptyList()
            )) {
                archiveUnitProfileByFieldName.put(field, archiveUnitProfileModel);
            }
        }

        // Look for referenced ontology field in an archive unit profile
        for (OntologyModel ontologyToDelete : toDelete) {
            if (ParametersChecker.isNotEmpty(ontologyToDelete.getIdentifier())) {
                for (ArchiveUnitProfile archiveUnitProfile : archiveUnitProfileByFieldName.get(
                    ontologyToDelete.getIdentifier()
                )) {
                    manager.addError(
                        ontologyToDelete.getIdentifier(),
                        new ErrorReportOntologies(
                            OntologyErrorCode.STP_IMPORT_ONTOLOGIES_DELETE_USED_ONTOLOGY,
                            OntologyModel.TAG_IDENTIFIER,
                            "Field " +
                            OntologyModel.TAG_IDENTIFIER +
                            " used by ArchiveUnitProfile " +
                            archiveUnitProfile.getIdentifier() +
                            " of tenant " +
                            archiveUnitProfile.getTenantId(),
                            ontologyToDelete
                        ),
                        errors
                    );
                }
            }
        }
    }

    private void commitToDatabase(
        Map<String, OntologyModel> currentOntologiesMap,
        List<OntologyModel> toDelete,
        List<OntologyModel> toCreate,
        List<OntologyModel> toUpdate
    )
        throws ReferentialException, SchemaValidationException, InvalidParseOperationException, InvalidCreateOperationException, BadRequestException, DocumentAlreadyExistsException {
        deleteOntologies(toDelete);

        createOntologies(toCreate);

        updateOntologies(currentOntologiesMap, toUpdate);
    }

    private void backupDatabaseToOffers(GUID eip) throws VitamException {
        //Store collection
        functionalBackupService.saveCollectionAndSequence(
            eip,
            BACKUP_ONTOLOGY_EVENT,
            FunctionalAdminCollections.ONTOLOGY,
            eip.toString()
        );
    }

    private List<ArchiveUnitProfile> selectArchiveUnitProfiles() {
        // Cannot use DSL. We need to find archive unit profiles from ALL tenants
        FindIterable<ArchiveUnitProfile> find = FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.<
            ArchiveUnitProfile
        >getCollection()
            .find();

        return IterableUtils.toList(find);
    }

    private List<OntologyModel> selectOntologies() throws ReferentialException, InvalidParseOperationException {
        final Select select = new Select();
        DbRequestResult result = mongoAccess.findDocuments(
            select.getFinalSelect(),
            FunctionalAdminCollections.ONTOLOGY
        );
        return result.getDocuments(Ontology.class, OntologyModel.class);
    }

    @Override
    public RequestResponseOK<OntologyModel> findOntologiesForCache(JsonNode queryDsl)
        throws InvalidParseOperationException {
        final RequestResponseOK<OntologyModel> response = new RequestResponseOK<>(queryDsl);

        FindIterable<Ontology> documents = FunctionalAdminCollections.ONTOLOGY.<Ontology>getCollection().find();
        for (Ontology document : documents) {
            response.addResult(BsonHelper.fromDocumentToObject(document, OntologyModel.class));
        }

        return response;
    }

    private void createOntologies(List<OntologyModel> toCreate)
        throws ReferentialException, SchemaValidationException, InvalidParseOperationException, DocumentAlreadyExistsException {
        if (toCreate.isEmpty()) {
            return;
        }

        Integer tenant = ParameterHelper.getTenantParameter();

        ArrayNode ontologiesToCreate = JsonHandler.createArrayNode();
        for (final OntologyModel ontm : toCreate) {
            ontm.setId(GUIDFactory.newOntologyGUID(tenant).getId());
            ontm.setTenant(tenant);
            JsonNode ontologyNode = buildOntologyNode(ontm);
            ontologiesToCreate.add(ontologyNode);
        }
        mongoAccess.insertDocuments(ontologiesToCreate, FunctionalAdminCollections.ONTOLOGY).close();
    }

    private JsonNode buildOntologyNode(OntologyModel ontm) throws InvalidParseOperationException {
        final ObjectNode ontologyNode = (ObjectNode) JsonHandler.toJsonNode(ontm);
        JsonNode jsonNode = ontologyNode.remove(VitamFieldsHelper.id());
        if (jsonNode != null) {
            ontologyNode.set(UND_ID, jsonNode);
        }
        JsonNode hashTenant = ontologyNode.remove(VitamFieldsHelper.tenant());
        if (hashTenant != null) {
            ontologyNode.set(UND_TENANT, hashTenant);
        }
        return ontologyNode;
    }

    @Override
    public RequestResponseOK<OntologyModel> findOntologies(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        try (DbRequestResult result = mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.ONTOLOGY)) {
            return result.getRequestResponseOK(queryDsl, Ontology.class, OntologyModel.class);
        }
    }

    private VitamError getVitamError(String vitamCode, String error, StatusCode statusCode) {
        return VitamErrorUtils.getVitamError(vitamCode, error, "Ontology", statusCode);
    }

    private void updateOntologies(Map<String, OntologyModel> currentOntologiesMap, List<OntologyModel> toUpdate)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException, SchemaValidationException, BadRequestException {
        if (toUpdate.isEmpty()) {
            return;
        }

        for (OntologyModel ontology : toUpdate) {
            ontology.setTenant(ParameterHelper.getTenantParameter());
            ontology.setId(currentOntologiesMap.get(ontology.getIdentifier()).getId());

            updateOntology(ontology);
        }
    }

    /**
     * Create QueryDsl to update the given ontology
     *
     * @param ontologyModel
     * @throws InvalidCreateOperationException
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     * @throws SchemaValidationException
     * @throws BadRequestException
     */
    private void updateOntology(OntologyModel ontologyModel)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException, SchemaValidationException, BadRequestException {
        // FIXME use bulk create instead like LogbookMongoDbAccessImpl.
        final UpdateParserSingle updateParser = new UpdateParserSingle(new VarNameAdapter());
        final Update updateOntologies = new Update();
        List<SetAction> actions = new ArrayList<>();
        String description = ontologyModel.getDescription() == null ? "" : ontologyModel.getDescription();
        SetAction setDescription = new SetAction(OntologyModel.TAG_DESCRIPTION, description);
        actions.add(setDescription);
        SetAction setUpdateDate = new SetAction(OntologyModel.LAST_UPDATE, LocalDateUtil.nowFormatted());
        actions.add(setUpdateDate);

        String apiField = ontologyModel.getApiField() == null ? "" : ontologyModel.getApiField();
        SetAction setApiValue;
        setApiValue = new SetAction(OntologyModel.TAG_APIFIELD, apiField);
        actions.add(setApiValue);

        String sedaField = ontologyModel.getSedaField() == null ? "" : ontologyModel.getSedaField();
        SetAction setSedaValue;
        setSedaValue = new SetAction(OntologyModel.TAG_SEDAFIELD, sedaField);
        actions.add(setSedaValue);

        SetAction setTypeValue;
        setTypeValue = new SetAction(OntologyModel.TAG_TYPE, ontologyModel.getType().toString());
        actions.add(setTypeValue);

        String shortName = ontologyModel.getShortName() == null ? "" : ontologyModel.getShortName();
        SetAction setShortName;
        setShortName = new SetAction(OntologyModel.TAG_SHORT_NAME, shortName);
        actions.add(setShortName);

        List<String> collections = ontologyModel.getCollections() == null
            ? new ArrayList<>()
            : ontologyModel.getCollections();
        SetAction setCollections;
        setCollections = new SetAction(OntologyModel.TAG_COLLECTIONS, collections);
        actions.add(setCollections);

        updateOntologies.setQuery(eq(OntologyModel.TAG_IDENTIFIER, ontologyModel.getIdentifier()));
        updateOntologies.addActions(actions.toArray(new SetAction[0]));
        updateParser.parse(updateOntologies.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        mongoAccess.updateData(queryDslForUpdate, FunctionalAdminCollections.ONTOLOGY);
    }

    /**
     * Delete ontologies
     *
     * @param ontologyModels the ontologyModel to delete
     */
    private void deleteOntologies(List<OntologyModel> ontologyModels) {
        if (ontologyModels.isEmpty()) {
            return;
        }

        try {
            List<String> ontologyIdentifiers = ontologyModels
                .stream()
                .map(OntologyModel::getIdentifier)
                .collect(Collectors.toList());

            for (List<String> ids : ListUtils.partition(ontologyIdentifiers, VitamConfiguration.getBatchSize())) {
                final Delete delete = new Delete();
                delete.setQuery(in(OntologyModel.TAG_IDENTIFIER, ids.toArray(new String[0])));
                mongoAccess.deleteCollectionForTesting(FunctionalAdminCollections.ONTOLOGY, delete);
            }
        } catch (InvalidCreateOperationException | DatabaseException e) {
            throw new RuntimeException("Could not delete ontologies", e);
        }
    }

    /**
     * generate an error Report
     *
     * @param errors the list of error for generated errors
     * @param status status
     * @param eipMaster eipMaster
     * @return the error report inputStream
     */
    private InputStream generateErrorReport(
        Map<String, List<ErrorReportOntologies>> errors,
        StatusCode status,
        GUID eipMaster
    ) {
        final ObjectNode reportFinal = JsonHandler.createObjectNode();
        final ObjectNode guidmasterNode = JsonHandler.createObjectNode();
        final ObjectNode lineNode = JsonHandler.createObjectNode();
        guidmasterNode.put(EV_TYPE, ONTOLOGY_IMPORT_EVENT);
        guidmasterNode.put(EV_DATE_TIME, LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.now()));
        if (eipMaster != null) {
            guidmasterNode.put(EV_ID, eipMaster.toString());
        }
        guidmasterNode.put(OUT_MESSG, VitamErrorMessages.getFromKey(ONTOLOGY_IMPORT_EVENT + "." + status));

        for (String identifier : errors.keySet()) {
            List<ErrorReportOntologies> errorsReports = errors.get(identifier);
            ArrayNode messagesArrayNode = JsonHandler.createArrayNode();
            for (ErrorReportOntologies error : errorsReports) {
                final ObjectNode errorNode = JsonHandler.createObjectNode();
                if (error.getOntologyModel() != null) {
                    errorNode.put(IDENTIFIER, error.getOntologyModel().getIdentifier());
                }
                errorNode.put(CODE, error.getCode() + ".KO");
                errorNode.put(MESSAGE, error.getMessage());
                errorNode.put(ADDITIONAL_INFORMATION, error.getFieldName());
                messagesArrayNode.add(errorNode);
            }
            lineNode.set(identifier, messagesArrayNode);
        }

        reportFinal.set(JDO_DISPLAY, guidmasterNode);
        if (!errors.isEmpty()) {
            reportFinal.set(ERROR, lineNode);
        }

        String json = JsonHandler.unprettyPrint(reportFinal);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * generate Ok Report
     *
     * @param createdOntologies the list of created ontologies
     * @param deletedOntologies the list of deleted ontologies
     * @param updatedOntologies the list of updated ontologies
     * @return the error report inputStream
     */
    private InputStream generateReportOK(
        List<OntologyModel> createdOntologies,
        List<OntologyModel> deletedOntologies,
        List<OntologyModel> updatedOntologies,
        GUID eip
    ) {
        final ObjectNode reportFinal = JsonHandler.createObjectNode();
        final ObjectNode guidmasterNode = JsonHandler.createObjectNode();
        final ArrayNode deletedArrayNode = JsonHandler.createArrayNode();
        final ArrayNode createdArrayNode = JsonHandler.createArrayNode();
        final ArrayNode updatedArrayNode = JsonHandler.createArrayNode();
        guidmasterNode.put(EV_TYPE, ONTOLOGY_IMPORT_EVENT);
        guidmasterNode.put(EV_DATE_TIME, LocalDateUtil.nowFormatted());
        guidmasterNode.put(EV_ID, eip.toString());
        guidmasterNode.put(OUT_MESSG, ONTOLOGY_IMPORT_EVENT);
        for (OntologyModel ontologyModel : createdOntologies) {
            createdArrayNode.add(ontologyModel.getIdentifier());
        }
        for (OntologyModel ontologyModel : updatedOntologies) {
            updatedArrayNode.add(ontologyModel.getIdentifier());
        }
        for (OntologyModel ontologyModel : deletedOntologies) {
            deletedArrayNode.add(ontologyModel.getIdentifier());
        }
        reportFinal.set(JDO_DISPLAY, guidmasterNode);
        reportFinal.set(DELETED_ONTOLOGIES, deletedArrayNode);
        reportFinal.set(UPDATED_ONTOLOGIES, updatedArrayNode);
        reportFinal.set(CREATED_ONTOLOGIES, createdArrayNode);
        String json = JsonHandler.unprettyPrint(reportFinal);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Save the report stream
     */
    private void backupReport(InputStream stream, GUID eip) throws VitamException, IOException {
        functionalBackupService.saveFile(stream, eip, ONTOLOGIES_REPORT, DataCategory.REPORT, eip + ".json");
        stream.close();
    }
}
