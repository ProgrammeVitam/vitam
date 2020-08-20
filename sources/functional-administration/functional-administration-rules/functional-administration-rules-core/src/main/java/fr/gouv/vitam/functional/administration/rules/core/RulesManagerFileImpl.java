/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.functional.administration.rules.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamErrorMessages;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.ErrorReport;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.FileRulesErrorCode;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.ReferentialFileUtils;
import fr.gouv.vitam.functional.administration.common.ReportConstants;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.api.RestoreBackupService;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesCsvException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesDeleteException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesDurationException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesReadException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesUpdateException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.impl.RestoreBackupServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVRecord;

import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.parameter.ParameterHelper.getTenantParameter;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.ADDITIONAL_INFORMATION;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.RULES;


/**
 * RulesManagerFileImpl
 * <p>
 * Manage the Rules File features
 */

public class RulesManagerFileImpl implements ReferentialFile<FileRules> {

    private static final String RULES_FILE_STREAMIS_A_MANDATORY_PARAMETER = "rulesFileStreamis a mandatory parameter";
    private static final String RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER = "rulesFileStream is a mandatory parameter";
    private static final String RULES_PROCESS_IMPORT_ALREADY_EXIST =
        "There is already on file rules import in progress";
    private static final String DELETE_RULES_LINKED_TO_UNIT =
        "Error During Delete RuleFiles because this rule is linked to unit.";
    private static final String INVALID_CSV_FILE = "Invalid CSV File";
    private static final String RULE_DURATION_EXCEED = "Rule Duration Exceed";
    private static final String CSV = ".csv";

    private static final String TMP = "tmp";
    private static final String UPDATE_DATE = "UpdateDate";
    private static final String UNLIMITED = "unlimited";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RulesManagerFileImpl.class);
    private static final String STP_IMPORT_RULES_SUCCESS =
        "Succès du processus d'enregistrement de la copie du référentiel des règles de gestion";

    private static final String STP_IMPORT_RULES_FAILURE =
        "Echec du processus d'enregistrement de la copie du référentiel des règles de gestion";


    private static final String RESULTS = "$results";
    private static final int MAX_DURATION = 2147483647;
    private static final AlertService alertService = new AlertServiceImpl();
    private static final String STP_IMPORT_RULES_BACKUP = "STP_IMPORT_RULES_BACKUP";
    private static final String STP_IMPORT_RULES_BACKUP_CSV = "STP_IMPORT_RULES_BACKUP_CSV";
    private static final String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private static final String CHECK_RULES = "CHECK_RULES";
    private static final String CHECK_RULES_INVALID_CSV = "INVALID_CSV";
    private static final String STP_IMPORT_RULES_ENCODING_NOT_UTF_EIGHT = "INVALID_CSV_ENCODING_NOT_UTF_EIGHT";
    private static final String MAX_DURATION_EXCEEDS = "MAX_DURATION_EXCEEDS";
    private static final String CHECK_RULES_IMPORT_IN_PROCESS = "IMPORT_IN_PROCESS";
    private static final String COMMIT_RULES = "COMMIT_RULES";
    private static final String USED_DELETED_RULE_IDS = "usedDeletedRuleIds";
    private static final String DELETED_RULE_IDS = "deletedRuleIds";
    private static final String USED_UPDATED_RULE_IDS = "usedUpdatedRuleIds";
    private static final String RULES_REPORT = "RULES_REPORT";
    private static final List<String> fileRulesModelToInsertFinal = new ArrayList<>();
    private static final List<String> fileRulesModelToDeleteFinal = new ArrayList<>();
    private static final List<String> fileRulesModelToUpdateFinal = new ArrayList<>();
    private static final String NB_DELETED = "nbDeleted";
    private static final String NB_UPDATED = "nbUpdated";
    private static final String NB_INSERTED = "nbInserted";
    private static final int YEAR_LIMIT = 999;
    private static final int MONTH_LIMIT = YEAR_LIMIT * 12;
    private static final int DAY_LIMIT = MONTH_LIMIT * 30;
    private final MongoDbAccessAdminImpl mongoAccess;
    private final VitamCounterService vitamCounterService;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final FunctionalBackupService backupService;
    private final String UPDATE_RULES_ARCHIVE_UNITS = Contexts.UPDATE_RULES_ARCHIVE_UNITS.name();
    private final OntologyLoader ontologyLoader;
    private final VitamRuleService vitamRuleService;

    public RulesManagerFileImpl(MongoDbAccessAdminImpl dbConfiguration, VitamCounterService vitamCounterService,
        OntologyLoader ontologyLoader, VitamRuleService vitamRuleService) {
        backupService = new FunctionalBackupService(vitamCounterService);
        this.mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        logbookOperationsClientFactory = LogbookOperationsClientFactory.getInstance();
        metaDataClientFactory = MetaDataClientFactory.getInstance();
        workspaceClientFactory = WorkspaceClientFactory.getInstance();
        processingManagementClientFactory = ProcessingManagementClientFactory.getInstance();
        this.ontologyLoader = ontologyLoader;
        this.vitamRuleService = vitamRuleService;
    }

    @VisibleForTesting
    RulesManagerFileImpl(MongoDbAccessAdminImpl dbConfiguration,
        VitamCounterService vitamCounterService,
        FunctionalBackupService backupService, LogbookOperationsClientFactory logbookOperationsClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        OntologyLoader ontologyLoader,
        VitamRuleService vitamRuleService) {
        this.mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.backupService = backupService;
        this.ontologyLoader = ontologyLoader;
        this.vitamRuleService = vitamRuleService;
    }

    @Override
    public void importFile(InputStream rulesFileStream, String filename)
        throws IOException, InvalidParseOperationException, ReferentialException, StorageException,
        InvalidGuidOperationException {
        ParametersChecker.checkParameter(RULES_FILE_STREAMIS_A_MANDATORY_PARAMETER, rulesFileStream);
        Map<Integer, List<ErrorReport>> errors = new HashMap<>();
        List<FileRulesModel> usedDeletedRulesForReport = new ArrayList<>();
        List<FileRulesModel> usedUpdateRulesForReport = new ArrayList<>();
        List<FileRulesModel> usedUpdateRulesForUpdateUnit = new ArrayList<>();
        Set<String> notUsedDeletedRulesForReport = new HashSet<>();
        Set<String> notUsedUpdateRulesForReport = new HashSet<>();
        List<FileRulesModel> fileRulesModelToInsert = new ArrayList<>();
        List<FileRulesModel> fileRulesModelToDelete = new ArrayList<>();
        List<FileRulesModel> fileRulesModelToUpdate = new ArrayList<>();
        List<FileRulesModel> fileRulesModelToUpdateThenUpdateUnit = new ArrayList<>();
        List<FileRulesModel> fileRulesModelsToImport = new ArrayList<>();
        ArrayNode validatedRules = JsonHandler.createArrayNode();
        final GUID eip = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
        final GUID eip1 = GUIDFactory.newEventGUID(eip);

        initStpImportRulesLogbookOperation(eip);
        if(isImportOperationInProgress()) {
            throw new FileRulesImportInProgressException(RULES_PROCESS_IMPORT_ALREADY_EXIST);
        }

        File file = convertInputStreamToFile(rulesFileStream, CSV);
        try {
            validatedRules =
                checkFile(new FileInputStream(file), errors, usedDeletedRulesForReport,
                    usedUpdateRulesForReport, usedUpdateRulesForUpdateUnit, fileRulesModelToInsert,
                    notUsedDeletedRulesForReport, notUsedUpdateRulesForReport);
            if (validatedRules != null) {
                // Clear list because generateReport re-generate insert list
                fileRulesModelToInsert.clear();
                generateReportCommitAndSecureFileRules(file, eip, eip1, notUsedDeletedRulesForReport,
                    fileRulesModelToInsert, fileRulesModelToDelete, fileRulesModelToUpdate,
                    fileRulesModelToUpdateThenUpdateUnit, validatedRules, errors, filename);
            }
        } catch (FileRulesDeleteException e) {
            generateReportWhenFileRulesDeletedExceptionAppend(file,
                errors, eip, eip1, usedDeletedRulesForReport, usedUpdateRulesForReport,
                notUsedDeletedRulesForReport, fileRulesModelToInsert, fileRulesModelToDelete,
                fileRulesModelToUpdate, fileRulesModelsToImport, validatedRules, filename);
            throw e;
        } catch (FileRulesUpdateException e) {
            generateReportWhenFileRulesUpdatedExceptionAppend(file,
                errors, eip, eip1, usedDeletedRulesForReport, usedUpdateRulesForReport,
                notUsedDeletedRulesForReport, fileRulesModelToInsert, fileRulesModelToDelete,
                fileRulesModelsToImport, usedUpdateRulesForUpdateUnit, validatedRules, filename);
        } catch (FileRulesCsvException e) {
            updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(CHECK_RULES_INVALID_CSV, eip);
            generateReport(errors, eip, usedDeletedRulesForReport, usedUpdateRulesForReport);
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename);
            throw e;
        } catch (FileRulesDurationException e) {
            alertService.createAlert(RULE_DURATION_EXCEED);
            updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(MAX_DURATION_EXCEEDS, eip);
            generateReport(errors, eip, usedDeletedRulesForReport, usedUpdateRulesForReport);
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename);
            throw e;
        } catch (IOException e) {
            handleIOException(filename, usedDeletedRulesForReport, usedUpdateRulesForReport, eip, eip1, e);
            throw e;
        } catch (FileRulesImportInProgressException e) {
            updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(CHECK_RULES_IMPORT_IN_PROCESS, eip);
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename);
            throw new FileRulesImportInProgressException(RULES_PROCESS_IMPORT_ALREADY_EXIST);
        } finally {
            Files.delete(file.toPath());
        }

    }

    private void handleIOException(String filename,
                                   List<FileRulesModel> usedDeletedRulesForReport,
                                   List<FileRulesModel> usedUpdateRulesForReport,
                                   GUID eip,
                                   GUID eip1,
                                   IOException e)
            throws StorageException, InvalidParseOperationException {
        updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(STP_IMPORT_RULES_ENCODING_NOT_UTF_EIGHT, eip);
        final String jsonReportFile = eip + ".json";
        InputStream stream = generateReportKO(usedDeletedRulesForReport, usedUpdateRulesForReport, eip);
        try {
            backupService.saveFile(stream, eip, RULES_REPORT, DataCategory.REPORT, jsonReportFile);
        } catch (VitamException ve) {
            throw new StorageException(ve.getMessage(), ve);
        }
        updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO,
            "Error Encoding File : " + filename + " : " + e.getMessage());
    }

    private void generateReportCommitAndSecureFileRules(File file, final GUID eip, final GUID eip1,
        Set<String> notUsedDeletedRulesForReport, List<FileRulesModel> fileRulesModelToInsert,
        List<FileRulesModel> fileRulesModelToDelete, List<FileRulesModel> fileRulesModelToUpdate,
        List<FileRulesModel> fileRulesModelToUpdateThenUpdateUnit,
        ArrayNode validatedRules, Map<Integer, List<ErrorReport>> errors, String filename)
        throws IOException, ReferentialException, InvalidParseOperationException {
        List<FileRulesModel> fileRulesModelsToImport;
        List<FileRules> fileRulesInDb = findAllFileRulesQueryBuilder();
        List<FileRulesModel> fileRulesModelsInDb = transformFileRulesToFileRulesModel(fileRulesInDb);
        fileRulesModelsToImport = transformJsonNodeToFileRulesModel(validatedRules);
        createListToimportUpdateDelete(fileRulesModelsToImport, fileRulesModelsInDb,
            fileRulesModelToDelete, fileRulesModelToUpdate, fileRulesModelToInsert,
            fileRulesModelToUpdateThenUpdateUnit);
        InputStream fileInputStream = null;
        try {
            updateCheckFileRulesLogbookOperationOk(CHECK_RULES, StatusCode.OK,
                notUsedDeletedRulesForReport,
                eip);
            for (FileRulesModel fileRule : fileRulesModelToDelete) {
                fileRulesModelToDeleteFinal.add(fileRule.getRuleId());
            }
            for (FileRulesModel fileRule : fileRulesModelToUpdate) {
                fileRulesModelToUpdateFinal.add(fileRule.getRuleId());
            }
            generateReport(errors, eip, new ArrayList<>(), new ArrayList<>());

            commitRules(fileRulesModelToUpdate, fileRulesModelToDelete, validatedRules,
                fileRulesModelToInsert,
                fileRulesModelsToImport, eip);

            fileInputStream = new FileInputStream(file);
            backupService.saveFile(fileInputStream, eip, STP_IMPORT_RULES_BACKUP_CSV,
                DataCategory.RULES, (eip != null ? eip.getId() : "default") + CSV);

            backupService.saveCollectionAndSequence(eip, STP_IMPORT_RULES_BACKUP, RULES,
                    eip != null ? eip.getId() : null);

            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.OK, filename);
        } catch (final FileRulesException e) {
            LOGGER.error(e);
            throw e;
        } catch (VitamException e) {
            LOGGER.error(e);
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename);
            throw new FileRulesException(e);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    private void generateReportWhenFileRulesUpdatedExceptionAppend(File file, Map<Integer, List<ErrorReport>> errors,
        final GUID eip, final GUID eip1, List<FileRulesModel> usedDeletedRulesForReport,
        List<FileRulesModel> usedUpdateRulesForReport, Set<String> notUsedDeletedRulesForReport,
        List<FileRulesModel> fileRulesModelToInsert, List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelsToImport, List<FileRulesModel> usedUpdateRulesForUpdateUnit,
        ArrayNode validatedRules, String filename)
        throws IOException, ReferentialException, InvalidParseOperationException {
        InputStream fileInputStream = null;
        try {
            usedUpdateRulesForReport.addAll(usedUpdateRulesForUpdateUnit);

            generateReport(errors, eip, usedDeletedRulesForReport, usedUpdateRulesForReport);
            Set<String> usedUpdateRules = new HashSet<>();
            for (FileRulesModel fileRuleModel : usedUpdateRulesForReport) {
                usedUpdateRules.add(fileRuleModel.getRuleId());
            }
            updateCheckFileRulesLogbookOperationForUpdate(usedUpdateRules,
                notUsedDeletedRulesForReport,
                eip);

            commitRules(usedUpdateRulesForReport, fileRulesModelToDelete, validatedRules,
                fileRulesModelToInsert,
                fileRulesModelsToImport, eip);

            final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();
            final Digest digest = new Digest(digestType);
            digest.update(new FileInputStream(file));

            fileInputStream = new FileInputStream(file);
            backupService.saveFile(fileInputStream, eip, STP_IMPORT_RULES_BACKUP_CSV,
                DataCategory.RULES, (eip != null ? eip.getId() : "default") + CSV);

            backupService.saveCollectionAndSequence(eip, STP_IMPORT_RULES_BACKUP, RULES,
                    eip != null ? eip.getId() : null);

            if (!usedUpdateRulesForUpdateUnit.isEmpty()) {
                // #2201 - we now launch the process that will update units
                launchWorkflow(usedUpdateRulesForUpdateUnit);
            }
            // TODO #2201 : Create Workflow for update AU linked to unit
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.WARNING, filename);
        } catch (VitamException e) {
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename);
            throw new FileRulesException(e);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    private void generateReportWhenFileRulesDeletedExceptionAppend(File file, Map<Integer, List<ErrorReport>> errors,
        final GUID eip, final GUID eip1, List<FileRulesModel> usedDeletedRulesForReport,
        List<FileRulesModel> usedUpdateRulesForReport, Set<String> notUsedDeletedRulesForReport,
        List<FileRulesModel> fileRulesModelToInsert, List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToUpdate, List<FileRulesModel> fileRulesModelsToImport,
        ArrayNode validatedRules, String filename)
        throws ReferentialException, InvalidParseOperationException {
        try {
            generateReport(errors, eip, usedDeletedRulesForReport, usedUpdateRulesForReport);
            Set<String> fileRulesIdLinkedToUnitForDelete = new HashSet<>();
            for (FileRulesModel fileRuleModel : usedDeletedRulesForReport) {
                fileRulesIdLinkedToUnitForDelete.add(fileRuleModel.getRuleId());
            }
            updateCheckFileRulesLogbookOperationForDelete(CHECK_RULES, StatusCode.KO,
                fileRulesIdLinkedToUnitForDelete,
                eip);
            LOGGER.error(DELETE_RULES_LINKED_TO_UNIT);
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename);
            throw new FileRulesException(DELETE_RULES_LINKED_TO_UNIT);
        } catch (StorageException e) {
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename);
            throw new FileRulesException(e);
        }
    }

    private void updateStpImportRulesLogbookOperation(final GUID eip, final GUID eip1, StatusCode status,
        String filename)
        throws InvalidParseOperationException {
        final LogbookOperationParameters logbookParametersEnd = LogbookParameterHelper
            .newLogbookOperationParameters(eip1, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                status, VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, status),
                eip);
        ReferentialFileUtils.addFilenameInLogbookOperation(filename, logbookParametersEnd);
        updateLogBookEntry(logbookParametersEnd);
    }

    private void initStpImportRulesLogbookOperation(final GUID eip) {
        final LogbookOperationParameters logbookParametersStart = LogbookParameterHelper
            .newLogbookOperationParameters(eip, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.STARTED), eip);
        createLogBookEntry(logbookParametersStart);
    }

    private void launchWorkflow(List<FileRulesModel> usedUpdateRulesForReport)
        throws InvalidParseOperationException, InvalidGuidOperationException {

        try (ProcessingManagementClient processManagementClient = processingManagementClientFactory.getClient()) {
            ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final FileRulesModel ruleNode : usedUpdateRulesForReport) {
                arrayNode.add(JsonHandler.toJsonNode(ruleNode));
            }

            final GUID updateOperationGUID = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
            final GUID reqId = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
            // FIXME: 01/01/2020 why operation id  != requestId. We use request id to monitor operations ?!
            final LogbookOperationParameters logbookUpdateParametersStart = LogbookParameterHelper
                .newLogbookOperationParameters(updateOperationGUID, UPDATE_RULES_ARCHIVE_UNITS,
                    updateOperationGUID,
                    LogbookTypeProcess.UPDATE,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(UPDATE_RULES_ARCHIVE_UNITS, StatusCode.STARTED),
                    reqId);
            createLogBookEntry(logbookUpdateParametersStart);
            try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

                workspaceClient.createContainer(updateOperationGUID.getId());

                workspaceClient.putObject(updateOperationGUID.getId(),
                    UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON,
                    JsonHandler.writeToInpustream(arrayNode));

                // store original query in workspace
                workspaceClient
                    .putObject(updateOperationGUID.getId(), OperationContextMonitor.OperationContextFileName,
                        writeToInpustream(
                            OperationContextModel.get(usedUpdateRulesForReport)));


                // compress file to backup
                OperationContextMonitor
                    .compressInWorkspace(workspaceClientFactory, updateOperationGUID.getId(),
                        Contexts.UPDATE_RULES_ARCHIVE_UNITS.getLogbookTypeProcess(),
                        OperationContextMonitor.OperationContextFileName);


                processManagementClient.initVitamProcess(updateOperationGUID.getId(), UPDATE_RULES_ARCHIVE_UNITS);
                LOGGER.debug("Started Update in Resource");
                RequestResponse<ItemStatus> ret =
                    processManagementClient
                        .updateOperationActionProcess(ProcessAction.RESUME.getValue(),
                            updateOperationGUID.getId());

                if (Status.ACCEPTED.getStatusCode() != ret.getStatus()) {
                    throw new VitamClientException("Process couldnt be executed");
                }

            } catch (ContentAddressableStorageServerException | InternalServerException |
                VitamClientException | BadRequestException | OperationContextException e) {
                LOGGER.error(e);
                final LogbookOperationParameters logbookUpdateParametersEnd =
                    LogbookParameterHelper
                        .newLogbookOperationParameters(updateOperationGUID,
                            UPDATE_RULES_ARCHIVE_UNITS,
                            updateOperationGUID,
                            LogbookTypeProcess.UPDATE,
                            StatusCode.KO,
                            VitamLogbookMessages.getCodeOp(UPDATE_RULES_ARCHIVE_UNITS,
                                StatusCode.KO),
                            reqId);
                updateLogBookEntry(logbookUpdateParametersEnd);
            }
        }
    }

    private void commitRules(List<FileRulesModel> fileRulesModelToUpdate,
        List<FileRulesModel> fileRulesModelToDelete,
        ArrayNode validatedRules, List<FileRulesModel> fileRulesModelToInsert,
        List<FileRulesModel> fileRulesModelsToImport, GUID eipMaster)
        throws FileRulesException {
        try {
            Integer sequence = vitamCounterService
                .getSequence(getTenantParameter(), SequenceType.RULES_SEQUENCE);
            for (FileRulesModel fileRulesModel : fileRulesModelToUpdate) {
                updateFileRules(fileRulesModel, sequence);
            }
            if (validatedRules != null && validatedRules.size() > 0 &&
                fileRulesModelToInsert.containsAll(fileRulesModelsToImport)) {
                commit(validatedRules);
            } else if (!fileRulesModelToInsert.isEmpty()) {
                final JsonNode fileRulesNodeToInsert = JsonHandler.toJsonNode(fileRulesModelToInsert);
                if (fileRulesNodeToInsert != null && fileRulesNodeToInsert.isArray()) {
                    final ArrayNode fileRulesArrayToInsert = (ArrayNode) fileRulesNodeToInsert;
                    commit(fileRulesArrayToInsert);
                }
            }
            for (FileRulesModel fileRulesModel : fileRulesModelToDelete) {
                deleteFileRules(fileRulesModel);
            }
            updateCommitFileRulesLogbookOperationOkOrKo(COMMIT_RULES, StatusCode.OK, eipMaster,
                fileRulesModelToUpdate, fileRulesModelToDelete, fileRulesModelToInsert);
        } catch (ReferentialException | DocumentAlreadyExistsException | InvalidCreateOperationException | InvalidParseOperationException |
            SchemaValidationException | BadRequestException e) {
            LOGGER.error(e);
            updateCommitFileRulesLogbookOperationOkOrKo(COMMIT_RULES, StatusCode.KO, eipMaster,
                fileRulesModelToUpdate, fileRulesModelToDelete, fileRulesModelToInsert);
            throw new FileRulesException(e);
        }
    }

    private void commit(ArrayNode validatedRules)
        throws ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        if (validatedRules.size() > 0) {
            Integer sequence = vitamCounterService
                .getNextSequence(getTenantParameter(), SequenceType.RULES_SEQUENCE);
            mongoAccess.insertDocuments(validatedRules, RULES, sequence);
        }

    }

    private void updateCommitFileRulesLogbookOperationOkOrKo(String operationFileRules, StatusCode statusCode,
        GUID evIdentifierProcess, List<FileRulesModel> fileRulesModelToUpdate,
        List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToInsert) {
        final ObjectNode evDetData = JsonHandler.createObjectNode();
        evDetData.put(NB_DELETED, fileRulesModelToDelete.size());
        evDetData.put(NB_UPDATED, fileRulesModelToUpdate.size());
        evDetData.put(NB_INSERTED, fileRulesModelToInsert.size());
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParameterHelper
                .newLogbookOperationParameters(evid, operationFileRules, evIdentifierProcess,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(COMMIT_RULES, statusCode), evIdentifierProcess);
        logbookOperationParameters.putParameterValue(LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData));
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            operationFileRules +
                "." + statusCode);
        updateLogBookEntry(logbookOperationParameters);
    }

    private void updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(String subEvenType,
        GUID evIdentifierProcess) {
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParameterHelper.newLogbookOperationParameters(
                evid, CHECK_RULES, evIdentifierProcess,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(CHECK_RULES, subEvenType, StatusCode.KO),
                evIdentifierProcess);
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            VitamLogbookMessages.getOutcomeDetail(CHECK_RULES, subEvenType, StatusCode.KO));
        updateLogBookEntry(logbookOperationParameters);
    }

    private void updateCheckFileRulesLogbookOperationOk(String operationFileRules, StatusCode statusCode,
        Set<String> fileRulesIdsLinkedToUnit, GUID evIdentifierProcess) {
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParameterHelper
                .newLogbookOperationParameters(evid, operationFileRules,
                    evIdentifierProcess,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(CHECK_RULES, statusCode),
                    evIdentifierProcess);
        if (!fileRulesIdsLinkedToUnit.isEmpty()) {
            final ObjectNode usedDeleteRuleIds = JsonHandler.createObjectNode();
            final ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (String fileRulesId : fileRulesIdsLinkedToUnit) {
                arrayNode.add(fileRulesId);
            }
            usedDeleteRuleIds.set(DELETED_RULE_IDS, arrayNode);
            logbookOperationParameters.putParameterValue(LogbookParameterName.eventDetailData,
                JsonHandler.unprettyPrint(usedDeleteRuleIds));
        }

        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, operationFileRules +
            "." + statusCode);
        updateLogBookEntry(logbookOperationParameters);
    }

    private void updateCheckFileRulesLogbookOperationForDelete(String operationFileRules, StatusCode statusCode,
        Set<String> fileRulesIdsLinkedToUnit, GUID evIdentifierProcess) {
        final ObjectNode usedDeleteRuleIds = JsonHandler.createObjectNode();
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        for (String fileRulesId : fileRulesIdsLinkedToUnit) {
            arrayNode.add(fileRulesId);
        }
        usedDeleteRuleIds.set(USED_DELETED_RULE_IDS, arrayNode);
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParameterHelper
                .newLogbookOperationParameters(evid, operationFileRules, evIdentifierProcess,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(CHECK_RULES, statusCode),
                    evIdentifierProcess);
        logbookOperationParameters.putParameterValue(LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(usedDeleteRuleIds));
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, operationFileRules +
            "." + statusCode);
        updateLogBookEntry(logbookOperationParameters);
    }

    private void updateCheckFileRulesLogbookOperationForUpdate(
        Set<String> fileRulesIdsLinkedToUnit, Set<String> deleteRulesIds, GUID evIdentifierProcess) {
        final ObjectNode evDetData = JsonHandler.createObjectNode();
        final ArrayNode updatedArrayNode = JsonHandler.createArrayNode();
        if (deleteRulesIds.size() > 0) {
            final ArrayNode deletedArrayNode = JsonHandler.createArrayNode();
            for (String fileRulesId : deleteRulesIds) {
                deletedArrayNode.add(fileRulesId);
            }
            evDetData.set(DELETED_RULE_IDS, deletedArrayNode);
        }
        for (String fileRulesIds : fileRulesIdsLinkedToUnit) {
            updatedArrayNode.add(fileRulesIds);
        }
        evDetData.set(USED_UPDATED_RULE_IDS, updatedArrayNode);
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParameterHelper
                .newLogbookOperationParameters(evid, CHECK_RULES, evIdentifierProcess,
                    LogbookTypeProcess.MASTERDATA,
                    StatusCode.WARNING,
                    VitamLogbookMessages.getCodeOp(CHECK_RULES, StatusCode.WARNING),
                    evIdentifierProcess);
        logbookOperationParameters.putParameterValue(LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData));
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, CHECK_RULES +
            "." + StatusCode.WARNING);
        updateLogBookEntry(logbookOperationParameters);
    }

    private void updateLogBookEntry(LogbookOperationParameters logbookParametersEnd) {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            client.update(logbookParametersEnd);
        } catch (LogbookClientBadRequestException | LogbookClientNotFoundException | LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void createLogBookEntry(LogbookOperationParameters logbookParametersStart) {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            client.create(logbookParametersStart);
        } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
            LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Checks File : checks if a stream of referential data is valid
     *
     * @param rulesFileStream as InputStream
     * @param errorsMap List of string that contains errors
     * @param usedDeletedRules used rules in AU that want to delete
     * @param usedUpdatedRules used rules in AU that want to update
     * @param insertRules inserted rules
     * @param usedUpdateRulesForUpdateUnit used rules in AU that want to be updated for real purpose (duration)
     * @param notUsedDeletedRules not used rules in AU that want to delete
     * @param notUsedUpdatedRules Updated rules not used in AU
     * @return The JsonArray containing the referential data if they are all valid
     * @throws ReferentialException when there is errors import
     * @throws IOException when there is IO Exception
     * @throws InvalidParseOperationException
     */
    public ArrayNode checkFile(InputStream rulesFileStream, Map<Integer, List<ErrorReport>> errorsMap,
        List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules,
        List<FileRulesModel> usedUpdateRulesForUpdateUnit, List<FileRulesModel> insertRules,
        Set<String> notUsedDeletedRules, Set<String> notUsedUpdatedRules)
        throws IOException, ReferentialException, InvalidParseOperationException {

        ParametersChecker.checkParameter(RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER, rulesFileStream);

        File file = convertInputStreamToFile(rulesFileStream, CSV);
        Map<String, FileRulesModel> rulesFromFile = getRulesFromCSV(new FileInputStream(file));
        Map<String, FileRulesModel> rulesInDatabase = getAllRulesInDB();
        RuleImportDiff ruleImportDiff = new RuleImportDiff(rulesFromFile, rulesInDatabase);

        List<FileRulesModel> usedRulesToUpdate = getUsedRules(ruleImportDiff.getRulesToUpdateUnsafely());
        List<FileRulesModel> usedRulesToDelete = getUsedRules(ruleImportDiff.getRulesToDelete());

        //TODO: change return type to RuleImportDiff to handle this instead of changing all those collections...
        usedDeletedRules.clear();
        usedDeletedRules.addAll(usedRulesToDelete);
        usedUpdatedRules.clear();
        usedUpdatedRules.addAll(getUsedRules(ruleImportDiff.getRulesToUpdate()));
        insertRules.clear();
        insertRules.addAll(ruleImportDiff.getRulesToInsert());
        usedUpdateRulesForUpdateUnit.clear();
        usedUpdateRulesForUpdateUnit.addAll(usedRulesToUpdate);
        notUsedDeletedRules.clear();
        Set<String> unusedDeletedRulesIds = getUnusedRules(ruleImportDiff.getRulesToDelete()).stream()
                .map(FileRulesModel::getRuleId)
                .collect(Collectors.toSet());
        notUsedDeletedRules.addAll(unusedDeletedRulesIds);
        notUsedUpdatedRules.clear();
        Set<String> unusedUpdatedRulesIds = getUnusedRules(ruleImportDiff.getRulesToUpdate()).stream()
                .map(FileRulesModel::getRuleId)
                .collect(Collectors.toSet());
        notUsedUpdatedRules.addAll(unusedUpdatedRulesIds);

        if (!usedRulesToDelete.isEmpty()) {
            throw new FileRulesDeleteException("used Rules want to be deleted");
        }
        if (!usedRulesToUpdate.isEmpty()) {
            throw new FileRulesUpdateException("used Rules want to be updated");
        }

        return RulesManagerParser.convertFileRulesModelListToArrayNode(rulesFromFile.values());
    }

    private Map<String, FileRulesModel> getAllRulesInDB() throws ReferentialException {
        List<FileRules> rulesInDB = findAllFileRulesInDB();
        return rulesInDB.stream()
                .map(RulesManagerFileImpl::convertFileRulesToFilesRulesModel)
                .collect(Collectors.toMap(FileRulesModel::getRuleId, r -> r));
    }

    private List<FileRules> findAllFileRulesInDB() throws ReferentialException {
        final Select select = new Select();
        List<FileRules> fileRules = new ArrayList<>();
        RequestResponseOK<FileRules> response = findDocuments(select.getFinalSelect());
        if (response != null) {
            return response.getResults();
        }
        return fileRules;
    }

    private static FileRulesModel convertFileRulesToFilesRulesModel(FileRules fileRule) {
        return new FileRulesModel(fileRule.getRuleid(),
                fileRule.getRuletype(),
                fileRule.getRulevalue(),
                fileRule.getRuledescription(),
                fileRule.getRuleduration(),
                fileRule.getRulemeasurement());
    }

    private Map<String, FileRulesModel> getRulesFromCSV(InputStream ruleInputStream)
            throws IOException, FileRulesReadException {
        Map<String, FileRulesModel> rulesModel = new HashMap<>();
        Map<Integer, List<ErrorReport>> errorsMap = new HashMap<>();

        final CsvMapper csvMapper = new CsvMapper();
        MappingIterator<FileRulesModel> csvReader = csvMapper.readerFor(FileRulesModel.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(ruleInputStream);
        int lineNumber = 1;
        List<FileRulesModel> rules = csvReader.readAll();
        for (FileRulesModel rule : rules) {
            lineNumber++;
            List<ErrorReport> errors = new ArrayList<>();

            rule.setUpdateDate(LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
            rule.setCreationDate(LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));

            errors.addAll(checkParametersNotEmpty(rule, lineNumber));
            errors.addAll(checkRuleDuration(rule, lineNumber));

            if (rulesModel.containsKey(rule.getRuleId())) {
                errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_RULEID_DUPLICATION,
                        lineNumber, rule));
            }
            RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromType(rule.getRuleMeasurement());
            if (ruleMeasurement == null) {
                errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULEMEASUREMENT,
                        lineNumber, rule));
            }
            RuleType ruletype = RuleType.getEnumFromName(rule.getRuleType());
            if (ruletype == null) {
                errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULETYPE_UNKNOW,
                        lineNumber, rule));
            }
            if (ruletype != null && ruleMeasurement != null) {
                errors.addAll(checkAssociationRuleDurationRuleMeasurementLimit(lineNumber, rule));
                errors.addAll(checkRuleDurationWithConfiguration(lineNumber, rule));
            }

            if (!errors.isEmpty()) {
                errorsMap.put(lineNumber, errors);
            }

            rulesModel.put(rule.getRuleId(), rule);
        }

        if (!errorsMap.isEmpty()) {
            Set<ErrorReport> hasDurationExceededErrors = errorsMap.values().stream()
                    .flatMap(Collection::stream)
                    .filter(e -> e.getCode().equals(FileRulesErrorCode.STP_IMPORT_RULES_RULEDURATION_EXCEED))
                    .collect(Collectors.toSet());
            if (!hasDurationExceededErrors.isEmpty()) {
                throw new FileRulesDurationException(RULE_DURATION_EXCEED, errorsMap);
            }
            throw new FileRulesCsvException(INVALID_CSV_FILE, errorsMap);
        }

        return rulesModel;
    }

    private List<FileRulesModel> getUsedRules(List<FileRulesModel> rules) {
        return rules.stream().filter(this::isRuleUsedByUnit).collect(Collectors.toList());
    }

    private List<FileRulesModel> getUnusedRules(List<FileRulesModel> rules) {
        return rules.stream().filter(r -> !isRuleUsedByUnit(r)).collect(Collectors.toList());
    }


    private boolean isRuleUsedByUnit(FileRulesModel rule) {
        ArrayNode unitsUsedByRule = checkUnitLinkedtofileRulesInDatabase(fileRulesLinkedToUnitQueryBuilder(rule));
        return unitsUsedByRule != null && !unitsUsedByRule.isEmpty();
    }

    private void generateReport(Map<Integer, List<ErrorReport>> errors, GUID eipMaster,
        List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules)
        throws StorageException {
        final String fileName = eipMaster + ".json";
        InputStream stream;
        if (!errors.isEmpty() || !usedDeletedRules.isEmpty()) {
            if (eipMaster != null) {
                stream = generateErrorReport(errors, usedDeletedRules, usedUpdatedRules, StatusCode.KO, eipMaster);
            } else {
                stream = generateErrorReport(errors, usedDeletedRules, usedUpdatedRules, StatusCode.KO, null);
            }
        } else if (!usedUpdatedRules.isEmpty()) {
            if (eipMaster != null) {
                stream = generateErrorReport(errors, usedDeletedRules, usedUpdatedRules, StatusCode.WARNING,
                    eipMaster);
            } else {
                stream = generateErrorReport(errors, usedDeletedRules, usedUpdatedRules, StatusCode.WARNING,
                    null);
            }
        } else {
            stream = generateReportOK(usedDeletedRules, usedUpdatedRules, eipMaster);
        }

        try {
            backupService.saveFile(stream, eipMaster, RULES_REPORT, DataCategory.REPORT, fileName);
        } catch (VitamException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private void createListToimportUpdateDelete(List<FileRulesModel> fileRulesModelsToImport,
        List<FileRulesModel> fileRulesModelsInDb, List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToUpdate, List<FileRulesModel> fileRulesModelToInsert,
        List<FileRulesModel> fileRulesModelToUpdateThenUpdateUnit) {

        RuleImportDiff ruleImportDiff = new RuleImportDiff(fileRulesModelsToImport, fileRulesModelsInDb);
        fileRulesModelToUpdate.addAll(ruleImportDiff.getRulesToUpdate());
        fileRulesModelToUpdateThenUpdateUnit.addAll(ruleImportDiff.getRulesToUpdateUnsafely());
        fileRulesModelToInsert.addAll(ruleImportDiff.getRulesToInsert());
        fileRulesModelToDelete.addAll(ruleImportDiff.getRulesToDelete());
    }

    private List<FileRulesModel> transformFileRulesToFileRulesModel(List<FileRules> fileRules) {
        List<FileRulesModel> filesRulesModels = new ArrayList<>();
        if (fileRules != null && !fileRules.isEmpty()) {
            for (FileRules rule : fileRules) {
                filesRulesModels.add(new FileRulesModel(rule.getRuleid(), rule.getRuletype(), rule.getRulevalue(), rule
                    .getRuledescription(), rule.getRuleduration(), rule.getRulemeasurement()));

            }
        }
        return filesRulesModels;
    }

    private List<FileRulesModel> transformJsonNodeToFileRulesModel(JsonNode fileRulesNode) {
        List<FileRulesModel> filesRulesModels = new ArrayList<>();
        try {
            if (fileRulesNode != null && fileRulesNode.isArray()) {
                final ArrayNode arrayNode = (ArrayNode) fileRulesNode;
                for (JsonNode jsonNode : arrayNode) {
                    FileRulesModel fileRulesModel = JsonHandler.getFromJsonNode(jsonNode, FileRulesModel.class);
                    filesRulesModels.add(fileRulesModel);
                }
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
        }
        return filesRulesModels;
    }

    private void updateFileRules(FileRulesModel fileRulesModel, Integer sequence)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException,
        SchemaValidationException, BadRequestException {
        // FIXME use bulk create instead like LogbookMongoDbAccessImpl.
        final UpdateParserSingle updateParser = new UpdateParserSingle(new VarNameAdapter());
        final Update updateFileRules = new Update();
        List<SetAction> actions = new ArrayList<>();
        SetAction setRuleValue;
        setRuleValue = new SetAction(FileRulesModel.TAG_RULE_VALUE, fileRulesModel.getRuleValue());
        actions.add(setRuleValue);
        if (fileRulesModel.getRuleDescription() != null) {
            SetAction setRuleDescription =
                    new SetAction(FileRulesModel.TAG_RULE_DESCRIPTION, fileRulesModel.getRuleDescription());
            actions.add(setRuleDescription);
        }
        SetAction setUpdateDate =
            new SetAction(UPDATE_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        actions.add(setUpdateDate);
        SetAction setRuleMeasurement =
            new SetAction(FileRulesModel.TAG_RULE_MEASUREMENT, fileRulesModel.getRuleMeasurement());
        actions.add(setRuleMeasurement);
        SetAction setRuleDuration = new SetAction(FileRulesModel.TAG_RULE_DURATION, fileRulesModel.getRuleDuration());
        actions.add(setRuleDuration);
        SetAction setRuleType = new SetAction(FileRulesModel.TAG_RULE_TYPE, fileRulesModel.getRuleType());
        actions.add(setRuleType);
        updateFileRules.setQuery(eq(FileRulesModel.TAG_RULE_ID, fileRulesModel.getRuleId()));
        updateFileRules.addActions(actions.toArray(SetAction[]::new));
        updateParser.parse(updateFileRules.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        mongoAccess.updateData(queryDslForUpdate, RULES, sequence);
    }

    private void deleteFileRules(FileRulesModel fileRulesModel) {
        final Delete delete = new Delete();
        try {
            delete.setQuery(eq(FileRulesModel.TAG_RULE_ID, fileRulesModel.getRuleId()));
            DbRequestResult result = mongoAccess.deleteCollectionForTesting(RULES, delete);
            result.close();
        } catch (InvalidCreateOperationException | DatabaseException e) {
            LOGGER.error(e);
        }
    }

    private List<FileRules> findAllFileRulesQueryBuilder() throws ReferentialException {
        final Select select = new Select();
        List<FileRules> fileRules = new ArrayList<>();
        RequestResponseOK<FileRules> response = findDocuments(select.getFinalSelect());
        if (response != null) {
            return response.getResults();
        }
        return fileRules;
    }

    private JsonNode fileRulesLinkedToUnitQueryBuilder(FileRulesModel fileRulesModels) {
        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        StringBuilder sb = new StringBuilder();
        sb.append("#management.").append(fileRulesModels.getRuleType()).append(".Rules").append(".Rule");
        try {
            ObjectNode projectionNode = JsonHandler.createObjectNode();
            // FIXME Add limit when Dbrequest is Fix and when distinct is implement in DbRequest:
            ObjectNode objectNode = JsonHandler.createObjectNode();
            objectNode.put("#id", 1);
            projectionNode.set("$fields", objectNode);
            ArrayNode arrayNode = JsonHandler.createArrayNode();
            selectMultiple.setQuery(eq(sb.toString(), fileRulesModels.getRuleId()));
            selectMultiple.addRoots(arrayNode);
            selectMultiple.addProjection(projectionNode);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("Query construction not valid ", e);
        }
        return selectMultiple.getFinalSelect();

    }

    private List<ErrorReport> checkRuleDuration(FileRulesModel fileRulesModel, int line) {
        List<ErrorReport> errors = new ArrayList<>();
        if (!UNLIMITED.equalsIgnoreCase(fileRulesModel.getRuleDuration())) {
            final int duration = parseWithDefault(fileRulesModel.getRuleDuration());
            if (duration < 0) {
                errors.add(
                        new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULEDURATION, line,
                                fileRulesModel));
            }
        }

        return errors;
    }

    private int parseWithDefault(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException err) {
            return -777;
        }
    }

    private List<ErrorReport> checkParametersNotEmpty(FileRulesModel fileRulesModel, int line) {
        List<ErrorReport> errors = new ArrayList<>();
        List<String> missingParam = new ArrayList<>();
        if (fileRulesModel.getRuleId() == null || fileRulesModel.getRuleId().isEmpty()) {
            missingParam.add(FileRulesModel.TAG_RULE_ID);
        }
        if (fileRulesModel.getRuleType() == null || fileRulesModel.getRuleType().isEmpty()) {
            missingParam.add(FileRulesModel.TAG_RULE_TYPE);
        }
        if (fileRulesModel.getRuleValue() == null || fileRulesModel.getRuleValue().isEmpty()) {
            missingParam.add(FileRulesModel.TAG_RULE_VALUE);
        }
        if (fileRulesModel.getRuleDuration() == null || fileRulesModel.getRuleDuration().isEmpty()) {
            missingParam.add(FileRulesModel.TAG_RULE_DURATION);
        }
        if (fileRulesModel.getRuleMeasurement() == null || fileRulesModel.getRuleMeasurement().isEmpty()) {
            missingParam.add(FileRulesModel.TAG_RULE_MEASUREMENT);
        }
        if (!missingParam.isEmpty()) {
            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_NOT_CSV_FORMAT, line,
                    String.join("", missingParam)));
        }

        return errors;
    }

    private List<ErrorReport> checkAssociationRuleDurationRuleMeasurementLimit(int line, FileRulesModel fileRuleModel) {
        List<ErrorReport> errors = new ArrayList<>();
        try {
            if (!fileRuleModel.getRuleDuration().equalsIgnoreCase(UNLIMITED) &&
                    (fileRuleModel.getRuleMeasurement().equalsIgnoreCase(RuleMeasurementEnum.YEAR.getType()) &&
                            Integer.parseInt(fileRuleModel.getRuleDuration()) > YEAR_LIMIT ||
                            fileRuleModel.getRuleMeasurement().equalsIgnoreCase(RuleMeasurementEnum.MONTH.getType()) &&
                                    Integer.parseInt(fileRuleModel.getRuleDuration()) > MONTH_LIMIT ||
                            fileRuleModel.getRuleMeasurement().equalsIgnoreCase(RuleMeasurementEnum.DAY.getType()) &&
                                    Integer.parseInt(fileRuleModel.getRuleDuration()) > DAY_LIMIT)) {
                errors
                        .add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_TOTALDURATION, line, fileRuleModel));
            }
        } catch (NumberFormatException e) {
            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_TOTALDURATION, line, fileRuleModel));
        }

        return errors;
    }

    private List<ErrorReport> checkRuleDurationWithConfiguration(int line, FileRulesModel fileRuleModel) {
        List<ErrorReport> errors = new ArrayList<>();
        String ruleType = fileRuleModel.getRuleType();

        String[] min = vitamRuleService.getMinimumRuleDuration(getTenantParameter(), ruleType).split(" ");
        int durationConf = 0;
        if (min.length == 2) {
            durationConf = calculDuration(min[0], min[1]);
        }
        int durationRule = calculDuration(fileRuleModel.getRuleDuration(), fileRuleModel.getRuleMeasurement());

        if (durationRule < durationConf) {
            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_RULEDURATION_EXCEED, line, fileRuleModel));
        }

        return errors;
    }

    private int calculDuration(String ruleDuration, String ruleMeasurement) {
        int duration = 0;

        if (ruleDuration.equalsIgnoreCase(UNLIMITED)) {
            return MAX_DURATION;
        }

        if (ruleDuration.matches("[0-9]+")) {
            duration = Integer.parseInt(ruleDuration);
        }

        switch (ruleMeasurement.toLowerCase()) {
            case "year":
                return duration * 365;
            case "month":
                return duration * 30;
            case "day":
                return duration;
            default:
                return 0;
        }
    }

    private File convertInputStreamToFile(InputStream rulesStream, String extension) throws IOException {
        try {
            final File csvFile = FileUtil.createFileInTempDirectoryWithPathCheck(TMP, extension);
            Files.copy(rulesStream, csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return csvFile;
        } finally {
            StreamUtils.closeSilently(rulesStream);
        }
    }

    @Override
    public FileRules findDocumentById(String id) {
        return (FileRules) mongoAccess.getDocumentByUniqueId(id, RULES, FileRules.RULEID);
    }

    @Override
    public RequestResponseOK<FileRules> findDocuments(JsonNode select) throws ReferentialException {
        try (DbRequestResult result =
            mongoAccess.findDocuments(select, RULES)) {
            return result.getRequestResponseOK(select, FileRules.class);
        }
    }

    private boolean isImportOperationInProgress() throws FileRulesException {
        try {
            final Select select = new Select();
            select.setLimitFilter(0, 1);
            select.addOrderByDescFilter(LogbookMongoDbName.eventDateTime.getDbname());
            select.setQuery(eq(
                String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.eventType.getDbname()),
                STP_IMPORT_RULES));
            select.addProjection(
                JsonHandler.createObjectNode().set(BuilderToken.PROJECTION.FIELDS.exactToken(),
                    JsonHandler.createObjectNode()
                        .put(BuilderToken.PROJECTIONARGS.ID.exactToken(), 1)
                        .put(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.eventType.getDbname()),
                            1)));
            JsonNode logbookResult =
                logbookOperationsClientFactory.getClient().selectOperation(select.getFinalSelect());
            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(logbookResult);
            // one result and last event type is STP_IMPORT_RULES -> import in progress
            if (requestResponseOK.getHits().getSize() != 0) {
                JsonNode result = requestResponseOK.getResults().get(0);
                if (result.get(LogbookDocument.EVENTS) != null && result.get(LogbookDocument.EVENTS).size() > 0) {
                    JsonNode lastEvent =
                        result.get(LogbookDocument.EVENTS).get(result.get(LogbookDocument.EVENTS).size() - 1);
                    return !STP_IMPORT_RULES.equals(lastEvent.get(LogbookMongoDbName.eventType.getDbname()).asText());
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException | LogbookClientException e) {
            throw new FileRulesException(e);
        }
    }

    private ArrayNode checkUnitLinkedtofileRulesInDatabase(JsonNode select) {
        ArrayNode resultUnitsArray = null;
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            LOGGER.debug("Selected Query For linked unit: " + select.toString());
            final JsonNode unitsResultNode = metaDataClient.selectUnits(select);
            if (unitsResultNode != null) {
                resultUnitsArray = (ArrayNode) unitsResultNode.get(RESULTS);
            }
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException |
            InvalidParseOperationException e) {
            LOGGER.error(e);
        }
        return resultUnitsArray;
    }

    /**
     * generate Error Report
     *
     * @param errors the list of error for generated errors
     * @param usedDeletedRules list of fileRules that attempt to be deleted but have reference to unit
     * @param usedUpdatedRules list of fileRules that attempt to be updated but have reference to unit
     * @param status status
     * @param eipMaster eipMaster
     * @return the error report inputStream
     */
    public InputStream generateErrorReport(Map<Integer, List<ErrorReport>> errors,
        List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules,
        StatusCode status, GUID eipMaster) {
        final FileRulesManagementReport reportFinal = new FileRulesManagementReport();
        final HashMap<String, String> guidmasterNode = new HashMap<>();
        final ArrayList<String> usedDeletedFileRuleList = new ArrayList<>();
        final ArrayList<String> usedUpdatedFileRuleList = new ArrayList<>();
        guidmasterNode.put(ReportConstants.EV_TYPE, STP_IMPORT_RULES);
        guidmasterNode.put(ReportConstants.EV_DATE_TIME, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        if (eipMaster != null) {
            guidmasterNode.put(ReportConstants.EV_ID, eipMaster.toString());
        }
        guidmasterNode.put(ReportConstants.OUT_MESSG, VitamErrorMessages.getFromKey(STP_IMPORT_RULES + "." + status));
        final HashMap<String, Object> storeErrors = new HashMap<>();
        for (Integer line : errors.keySet()) {
            List<ErrorReport> errorsReports = errors.get(line);
            ArrayNode messagesArrayNode = JsonHandler.createArrayNode();
            for (ErrorReport error : errorsReports) {
                final ObjectNode errorNode = JsonHandler.createObjectNode();
                errorNode.put(ReportConstants.CODE, error.getCode().name() + ".KO");
                errorNode.put(ReportConstants.MESSAGE, VitamErrorMessages.getFromKey(error.getCode().name()));
                switch (error.getCode()) {
                    case STP_IMPORT_RULES_MISSING_INFORMATION:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION,
                            error.getMissingInformations());
                        break;
                    case STP_IMPORT_RULES_RULEID_DUPLICATION:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION,
                            error.getFileRulesModel().getRuleId());
                        break;
                    case STP_IMPORT_RULES_WRONG_RULEDURATION:
                        errorNode.put(ADDITIONAL_INFORMATION,
                            error.getFileRulesModel().getRuleDuration());
                        break;
                    case STP_IMPORT_RULES_WRONG_RULEMEASUREMENT:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION,
                            error.getFileRulesModel().getRuleMeasurement());
                        break;
                    case STP_IMPORT_RULES_WRONG_RULETYPE_UNKNOW:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION,
                            error.getFileRulesModel().getRuleType());
                        break;
                    case STP_IMPORT_RULES_WRONG_TOTALDURATION:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION,
                            error.getFileRulesModel().getRuleDuration() + " " +
                                error.getFileRulesModel().getRuleMeasurement());
                        break;
                    case STP_IMPORT_RULES_RULEDURATION_EXCEED:
                        ObjectNode info = JsonHandler.createObjectNode();
                        info.put("RuleType", error.getFileRulesModel().getRuleType());
                        info.put("RuleDurationMin", vitamRuleService.getMinimumRuleDuration(getTenantParameter(),
                            error.getFileRulesModel().getRuleType()));
                        errorNode.set(ReportConstants.ADDITIONAL_INFORMATION, info);
                        break;
                    case STP_IMPORT_RULES_NOT_CSV_FORMAT:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION, error.getMissingInformations());
                        break;
                    case STP_IMPORT_RULES_DELETE_USED_RULES:
                    case STP_IMPORT_RULES_UPDATED_RULES:
                    default:
                        break;
                }
                messagesArrayNode.add(errorNode);
            }
            storeErrors.put(String.format("line %s", line), messagesArrayNode);
        }
        for (FileRulesModel fileRulesModel : usedDeletedRules) {
            usedDeletedFileRuleList.add(fileRulesModel.toString());
        }
        for (FileRulesModel fileRulesModel : usedUpdatedRules) {
            usedUpdatedFileRuleList.add(fileRulesModel.toString());
        }
        reportFinal.setJdo(guidmasterNode);
        if (!errors.isEmpty()) {
            reportFinal.setError(storeErrors);
        }
        reportFinal.setUsedFileRulesToDelete(usedDeletedFileRuleList);
        reportFinal.setUsedFileRulesToUpdate(usedUpdatedFileRuleList);
        reportFinal.setFileRulesToDelete(fileRulesModelToDeleteFinal);
        reportFinal.setFileRulesToUpdate(fileRulesModelToUpdateFinal);
        return new ByteArrayInputStream(JsonHandler.unprettyPrint(reportFinal).getBytes(StandardCharsets.UTF_8));

    }

    private InputStream generateReportKO(List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules, GUID eip) {
        final FileRulesManagementReport reportFinal = new FileRulesManagementReport();
        final HashMap<String, String> guidmasterNode = new HashMap<>();
        final ArrayList<String> usedDeletedFileRuleList = new ArrayList<>();
        final ArrayList<String> usedUpdatedFileRuleList = new ArrayList<>();

        guidmasterNode.put(ReportConstants.EV_TYPE, STP_IMPORT_RULES);
        guidmasterNode.put(ReportConstants.EV_DATE_TIME, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        guidmasterNode.put(ReportConstants.EV_ID, eip.toString());
        guidmasterNode.put(ReportConstants.OUT_MESSG, STP_IMPORT_RULES_FAILURE);

        for (FileRulesModel fileRulesModel : usedDeletedRules) {
            usedDeletedFileRuleList.add(fileRulesModel.toString());
        }
        for (FileRulesModel fileRulesModel : usedUpdatedRules) {
            usedUpdatedFileRuleList.add(fileRulesModel.toString());
        }
        reportFinal.setJdo(guidmasterNode);
        reportFinal.setUsedFileRulesToDelete(usedDeletedFileRuleList);
        reportFinal.setUsedFileRulesToUpdate(usedUpdatedFileRuleList);
        reportFinal.setFileRulesToDelete(fileRulesModelToDeleteFinal);
        reportFinal.setFileRulesToUpdate(fileRulesModelToUpdateFinal);
        return new ByteArrayInputStream(JsonHandler.unprettyPrint(reportFinal).getBytes(StandardCharsets.UTF_8));

    }

    private InputStream generateReportOK(List<FileRulesModel> usedDeletedRules,
                                         List<FileRulesModel> usedUpdatedRules, GUID eip) {
        final FileRulesManagementReport reportFinal = new FileRulesManagementReport();
        final HashMap<String, String> guidmasterNode = new HashMap<>();
        final ArrayList<String> usedDeletedFileRuleList = new ArrayList<>();
        final ArrayList<String> usedUpdatedFileRuleList = new ArrayList<>();
        guidmasterNode.put(ReportConstants.EV_TYPE, STP_IMPORT_RULES);
        guidmasterNode.put(ReportConstants.EV_DATE_TIME, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        guidmasterNode.put(ReportConstants.EV_ID, eip.toString());
        guidmasterNode.put(ReportConstants.OUT_MESSG,
            STP_IMPORT_RULES_SUCCESS);
        for (FileRulesModel fileRulesModel : usedDeletedRules) {
            usedDeletedFileRuleList.add(fileRulesModel.toString());
        }
        for (FileRulesModel fileRulesModel : usedUpdatedRules) {
            usedUpdatedFileRuleList.add(fileRulesModel.toString());
        }
        reportFinal.setJdo(guidmasterNode);
        reportFinal.setUsedFileRulesToDelete(usedDeletedFileRuleList);
        reportFinal.setUsedFileRulesToUpdate(usedUpdatedFileRuleList);
        reportFinal.setFileRulesToDelete(fileRulesModelToDeleteFinal);
        reportFinal.setFileRulesToUpdate(fileRulesModelToUpdateFinal);
        reportFinal.setFileRulesToImport(fileRulesModelToInsertFinal);
        return new ByteArrayInputStream(JsonHandler.unprettyPrint(reportFinal).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * get the rule file from collection
     *
     * @param tenant
     * @return ArrayNode
     * @throws InvalidParseOperationException
     */
    public ArrayNode getRuleFromCollection(int tenant) throws InvalidParseOperationException {
        return backupService
            .getCollectionInJson(backupService.getCurrentCollection(RULES, tenant));
    }

    /**
     * get the last rule file from offer
     *
     * @param tenant
     * @return ArrayNode
     */
    public ArrayNode getRuleFromOffer(int tenant) {
        Integer originalTenant = VitamThreadUtils.getVitamSession().getTenantId();
        VitamThreadUtils.getVitamSession().setTenantId(tenant);

        RestoreBackupService restoreBackupService = new RestoreBackupServiceImpl();
        Optional<CollectionBackupModel> collectionBackup =
            restoreBackupService.readLatestSavedFile(VitamConfiguration.getDefaultStrategy(), RULES);
        ArrayNode arrayNode = JsonHandler.createArrayNode();

        if (collectionBackup.isPresent()) {
            try {
                arrayNode = (ArrayNode) JsonHandler.toJsonNode(collectionBackup.get().getDocuments());
            } catch (InvalidParseOperationException e) {
                LOGGER.debug("ERROR: The file isn't in JSON type");
            }
        }

        VitamThreadUtils.getVitamSession().setTenantId(originalTenant);
        return arrayNode;
    }

    /**
     * Check if two arrayNodes are the same
     *
     * @param array1
     * @param array2
     * @param tenant
     * @return true if rule conformity, false if not
     */
    public boolean checkRuleConformity(ArrayNode array1, ArrayNode array2, int tenant) {

        if (!array1.toString().equals(array2.toString())) {
            JsonNode patch = JsonDiff.asJson(array1, array2);
            alertService.createAlert("Check failed: the security save of the rules repository of tenant " + tenant +
                " is not equal to its value in database.\n" + patch);
            return false;
        }

        return true;
    }
}
