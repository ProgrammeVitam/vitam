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
package fr.gouv.vitam.functional.administration.core.rules;

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
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
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
import fr.gouv.vitam.common.model.administration.RuleMeasurementEnum;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.ErrorReport;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.FileRulesCSV;
import fr.gouv.vitam.functional.administration.common.FileRulesErrorCode;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.ReportConstants;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesCsvException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesDeleteException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesDurationException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesIllegalDurationModeUpdateException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesReadException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialImportInProgressException;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.backup.RestoreBackupService;
import fr.gouv.vitam.functional.administration.core.reconstruction.RestoreBackupServiceImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.parameter.ParameterHelper.getTenantParameter;
import static fr.gouv.vitam.functional.administration.common.ReportConstants.ADDITIONAL_INFORMATION;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.RULES;
import static fr.gouv.vitam.functional.administration.common.utils.ArchiveUnitUpdateUtils.UNLIMITED_RULE_DURATION;

/**
 * RulesManagerFileImpl
 * <p>
 * Manage the Rules File features
 */

public class RulesManagerFileImpl implements ReferentialFile<FileRules> {

    private static final String RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER = "rulesFileStream is a mandatory parameter";
    private static final String RULES_PROCESS_IMPORT_ALREADY_EXIST =
        "There is already on file rules import in progress";
    private static final String DELETE_RULES_LINKED_TO_UNIT =
        "Error During Delete RuleFiles because this rule is linked to unit.";
    private static final String INVALID_CSV_FILE = "Invalid CSV File";
    private static final String RULE_DURATION_EXCEED = "Rule Duration Exceed";
    private static final String CSV = ".csv";

    private static final String UPDATE_DATE = "UpdateDate";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RulesManagerFileImpl.class);

    private static final int MAX_DURATION = 2147483647;
    private static final AlertService alertService = new AlertServiceImpl();
    private static final String STP_IMPORT_RULES_BACKUP = "STP_IMPORT_RULES_BACKUP";
    private static final String STP_IMPORT_RULES_BACKUP_CSV = "STP_IMPORT_RULES_BACKUP_CSV";
    private static final String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private static final String CHECK_RULES_INVALID_CSV = "INVALID_CSV";
    private static final String STP_IMPORT_RULES_ENCODING_NOT_UTF_EIGHT = "INVALID_CSV_ENCODING_NOT_UTF_EIGHT";
    private static final String MAX_DURATION_EXCEEDS = "MAX_DURATION_EXCEEDS";
    private static final String CHECK_RULES_IMPORT_IN_PROCESS = "IMPORT_IN_PROCESS";
    private static final String RULES_REPORT = "RULES_REPORT";
    private static final int YEAR_LIMIT = 999;
    private static final int MONTH_LIMIT = YEAR_LIMIT * 12;
    private static final int DAY_LIMIT = MONTH_LIMIT * 30;
    private final MongoDbAccessAdminImpl mongoAccess;
    private final VitamCounterService vitamCounterService;
    private final MetaDataClientFactory metaDataClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final FunctionalBackupService backupService;
    private final VitamRuleService vitamRuleService;
    private final LogbookRuleImportManager logbookRuleImportManager;
    private final int ruleAuditThreadPoolSize;
    private final RestoreBackupService restoreBackupService;

    public RulesManagerFileImpl(
        MongoDbAccessAdminImpl dbConfiguration,
        VitamCounterService vitamCounterService,
        VitamRuleService vitamRuleService,
        int ruleAuditThreadPoolSize
    ) {
        backupService = new FunctionalBackupService(vitamCounterService);
        this.mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        metaDataClientFactory = MetaDataClientFactory.getInstance();
        workspaceClientFactory = WorkspaceClientFactory.getInstance();
        processingManagementClientFactory = ProcessingManagementClientFactory.getInstance();
        this.vitamRuleService = vitamRuleService;
        this.logbookRuleImportManager = new LogbookRuleImportManager(LogbookOperationsClientFactory.getInstance());
        this.ruleAuditThreadPoolSize = ruleAuditThreadPoolSize;
        this.restoreBackupService = new RestoreBackupServiceImpl();
    }

    @VisibleForTesting
    RulesManagerFileImpl(
        MongoDbAccessAdminImpl dbConfiguration,
        VitamCounterService vitamCounterService,
        FunctionalBackupService backupService,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        VitamRuleService vitamRuleService,
        int ruleAuditThreadPoolSize,
        RestoreBackupService restoreBackupService
    ) {
        this.mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        this.metaDataClientFactory = metaDataClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.backupService = backupService;
        this.vitamRuleService = vitamRuleService;
        this.logbookRuleImportManager = new LogbookRuleImportManager(logbookOperationsClientFactory);
        this.ruleAuditThreadPoolSize = ruleAuditThreadPoolSize;
        this.restoreBackupService = restoreBackupService;
    }

    private static FileRulesModel convertFileRulesToFilesRulesModel(FileRules fileRule) {
        return new FileRulesModel(
            fileRule.getRuleid(),
            fileRule.getRuletype(),
            fileRule.getRulevalue(),
            fileRule.getRuledescription(),
            fileRule.getRuleduration(),
            fileRule.getRulemeasurement()
        );
    }

    @Override
    public void importFile(InputStream rulesFileStream, String filename)
        throws IOException, InvalidParseOperationException, ReferentialException, StorageException, InvalidGuidOperationException, LogbookClientException, IllegalPathException {
        ParametersChecker.checkParameter(RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER, rulesFileStream);

        final GUID eip = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());

        logbookRuleImportManager.initStpImportRulesLogbookOperation(eip);

        checkConcurrentImportOperation(filename, eip);

        File file = FileUtil.convertInputStreamToFile(rulesFileStream, GUIDFactory.newGUID().getId(), CSV);
        try {
            Map<String, FileRulesModel> rulesFromFile = processRuleParsing(file, filename, eip);

            RuleImportResultSet ruleImportResultSet = processRuleValidation(eip, rulesFromFile, filename);

            processCommitToDb(filename, ruleImportResultSet, eip, file);
        } finally {
            Files.delete(file.toPath());
        }
    }

    public Map<String, FileRulesModel> processRuleParsing(File file, String filename, GUID eip)
        throws FileRulesReadException, StorageException, InvalidParseOperationException, IOException, LogbookClientException {
        Map<String, FileRulesModel> rulesFromFile;

        try {
            rulesFromFile = getRulesFromCSV(new FileInputStream(file));
        } catch (FileRulesCsvException e) {
            this.logbookRuleImportManager.updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(
                    CHECK_RULES_INVALID_CSV,
                    eip
                );
            generateReport(
                StatusCode.KO,
                e.getErrorsMap(),
                eip,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );
            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, StatusCode.KO, filename);
            throw e;
        } catch (FileRulesDurationException e) {
            alertService.createAlert(RULE_DURATION_EXCEED);
            this.logbookRuleImportManager.updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(
                    MAX_DURATION_EXCEEDS,
                    eip
                );
            generateReport(
                StatusCode.KO,
                e.getErrorsMap(),
                eip,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );
            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, StatusCode.KO, filename);
            throw e;
        } catch (IOException e) {
            handleIOException(filename, eip, e);
            throw e;
        }
        return rulesFromFile;
    }

    private RuleImportResultSet processRuleValidation(
        GUID eip,
        Map<String, FileRulesModel> rulesFromFile,
        String filename
    ) throws ReferentialException, InvalidParseOperationException, LogbookClientException {
        try {
            return checkFile(rulesFromFile);
        } catch (FileRulesDeleteException e) {
            generateReportWhenFileRulesDeletedExceptionAppend(eip, e.getUsedDeletedRules(), filename);
            throw e;
        } catch (FileRulesIllegalDurationModeUpdateException e) {
            generateReportWhenFileRulesIllegalDurationModeUpdateException(
                eip,
                e.getUsedRulesWithDurationModeUpdate(),
                filename
            );
            throw e;
        }
    }

    private void checkConcurrentImportOperation(String filename, GUID eip)
        throws FileRulesException, InvalidParseOperationException, ReferentialImportInProgressException, LogbookClientException {
        if (logbookRuleImportManager.isImportOperationInProgress()) {
            this.logbookRuleImportManager.updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(
                    CHECK_RULES_IMPORT_IN_PROCESS,
                    eip
                );
            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, StatusCode.KO, filename);
            throw new ReferentialImportInProgressException(RULES_PROCESS_IMPORT_ALREADY_EXIST);
        }
    }

    private void handleIOException(String filename, GUID eip, IOException e)
        throws StorageException, InvalidParseOperationException, LogbookClientException {
        this.logbookRuleImportManager.updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(
                STP_IMPORT_RULES_ENCODING_NOT_UTF_EIGHT,
                eip
            );
        generateReport(
            StatusCode.KO,
            Collections.emptyMap(),
            eip,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
        this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(
                eip,
                StatusCode.KO,
                "Error Encoding File : " + filename + " : " + e.getMessage()
            );
    }

    private void processCommitToDb(String filename, RuleImportResultSet ruleImportResultSet, GUID eip, File file)
        throws IOException, ReferentialException, InvalidParseOperationException, LogbookClientException {
        InputStream fileInputStream = null;
        try {
            StatusCode statusCode = ruleImportResultSet.getUsedUpdateRulesForUpdateUnit().isEmpty()
                ? StatusCode.OK
                : StatusCode.WARNING;

            generateReport(
                statusCode,
                Collections.emptyMap(),
                eip,
                Collections.emptyList(),
                Collections.emptyList(),
                ruleImportResultSet.getUsedRulesToUpdate(),
                ruleImportResultSet.getUnusedRulesToDelete(),
                ruleImportResultSet.getRulesToUpdate(),
                ruleImportResultSet.getRulesToInsert()
            );

            this.logbookRuleImportManager.updateCheckFileRulesLogbookOperation(
                    statusCode,
                    ruleImportResultSet
                        .getUsedRulesToUpdate()
                        .stream()
                        .map(FileRulesModel::getRuleId)
                        .collect(Collectors.toSet()),
                    ruleImportResultSet
                        .getUnusedRulesToDelete()
                        .stream()
                        .map(FileRulesModel::getRuleId)
                        .collect(Collectors.toSet()),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    eip
                );

            commitRules(
                ruleImportResultSet.getRulesToUpdate(),
                ruleImportResultSet.getUnusedRulesToDelete(),
                ruleImportResultSet.getRulesToInsert(),
                eip
            );

            fileInputStream = new FileInputStream(file);
            backupService.saveFile(
                fileInputStream,
                eip,
                STP_IMPORT_RULES_BACKUP_CSV,
                DataCategory.RULES,
                eip.getId() + CSV
            );

            backupService.saveCollectionAndSequence(eip, STP_IMPORT_RULES_BACKUP, RULES, eip.getId());

            if (!ruleImportResultSet.getUsedUpdateRulesForUpdateUnit().isEmpty()) {
                // #2201 - we now launch the process that will update units
                launchWorkflow(ruleImportResultSet.getUsedUpdateRulesForUpdateUnit());
            }

            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, statusCode, filename);
        } catch (VitamException e) {
            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, StatusCode.KO, filename);
            throw new FileRulesException(e);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    private void generateReportWhenFileRulesDeletedExceptionAppend(
        final GUID eip,
        List<FileRulesModel> usedDeletedRulesForReport,
        String filename
    ) throws ReferentialException, InvalidParseOperationException, LogbookClientException {
        try {
            generateReport(
                StatusCode.KO,
                Collections.emptyMap(),
                eip,
                usedDeletedRulesForReport,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );
            this.logbookRuleImportManager.updateCheckFileRulesLogbookOperation(
                    StatusCode.KO,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    usedDeletedRulesForReport.stream().map(FileRulesModel::getRuleId).collect(Collectors.toSet()),
                    Collections.emptySet(),
                    eip
                );
            LOGGER.error(DELETE_RULES_LINKED_TO_UNIT);
            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, StatusCode.KO, filename);
            throw new FileRulesException(DELETE_RULES_LINKED_TO_UNIT);
        } catch (StorageException e) {
            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, StatusCode.KO, filename);
            throw new FileRulesException(e);
        }
    }

    private void generateReportWhenFileRulesIllegalDurationModeUpdateException(
        GUID eip,
        List<FileRulesModel> usedRulesWithDurationModeUpdate,
        String filename
    ) throws ReferentialException, InvalidParseOperationException, LogbookClientException {
        try {
            generateReport(
                StatusCode.KO,
                Collections.emptyMap(),
                eip,
                Collections.emptyList(),
                usedRulesWithDurationModeUpdate,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );
            Set<String> usedRuleIdsWithDurationModeUpdate = usedRulesWithDurationModeUpdate
                .stream()
                .map(FileRulesModel::getRuleId)
                .collect(Collectors.toSet());
            this.logbookRuleImportManager.updateCheckFileRulesLogbookOperation(
                    StatusCode.KO,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    usedRuleIdsWithDurationModeUpdate,
                    eip
                );
            LOGGER.error(
                "Cannot update used rule duration mode (null to defined of from defined to null) " +
                usedRuleIdsWithDurationModeUpdate
            );
            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, StatusCode.KO, filename);
            throw new FileRulesException("Cannot update used rule duration mode");
        } catch (StorageException e) {
            this.logbookRuleImportManager.updateStpImportRulesLogbookOperation(eip, StatusCode.KO, filename);
            throw new FileRulesException(e);
        }
    }

    private void launchWorkflow(List<FileRulesModel> usedUpdateRulesForReport)
        throws InvalidParseOperationException, InvalidGuidOperationException, LogbookClientException {
        try (ProcessingManagementClient processManagementClient = processingManagementClientFactory.getClient()) {
            ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final FileRulesModel ruleNode : usedUpdateRulesForReport) {
                arrayNode.add(JsonHandler.toJsonNode(ruleNode));
            }

            final GUID updateOperationGUID = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
            final GUID reqId = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
            // FIXME: 01/01/2020 why operation id  != requestId. We use request id to monitor operations ?!
            logbookRuleImportManager.initializeUnitRuleUpdateWorkflowLogbook(updateOperationGUID, reqId);
            try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
                workspaceClient.createContainer(updateOperationGUID.getId());

                workspaceClient.putObject(
                    updateOperationGUID.getId(),
                    UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON,
                    JsonHandler.writeToInpustream(arrayNode)
                );

                // store original query in workspace
                workspaceClient.putObject(
                    updateOperationGUID.getId(),
                    OperationContextMonitor.OperationContextFileName,
                    writeToInpustream(OperationContextModel.get(usedUpdateRulesForReport))
                );

                // compress file to backup
                OperationContextMonitor.compressInWorkspace(
                    workspaceClientFactory,
                    updateOperationGUID.getId(),
                    Contexts.UPDATE_RULES_ARCHIVE_UNITS.getLogbookTypeProcess(),
                    OperationContextMonitor.OperationContextFileName
                );

                processManagementClient.initVitamProcess(
                    updateOperationGUID.getId(),
                    Contexts.UPDATE_RULES_ARCHIVE_UNITS.name()
                );
                LOGGER.debug("Started Update in Resource");
                RequestResponse<ItemStatus> ret = processManagementClient.updateOperationActionProcess(
                    ProcessAction.RESUME.getValue(),
                    updateOperationGUID.getId()
                );

                if (Status.ACCEPTED.getStatusCode() != ret.getStatus()) {
                    throw new VitamClientException("Process couldnt be executed");
                }
            } catch (
                ContentAddressableStorageServerException
                | InternalServerException
                | VitamClientException
                | BadRequestException
                | OperationContextException e
            ) {
                LOGGER.error(e);
                logbookRuleImportManager.updateUnitRuleUpdateWorkflowLogbook(updateOperationGUID, reqId);
            }
        }
    }

    private void commitRules(
        List<FileRulesModel> fileRulesModelToUpdate,
        List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToInsert,
        GUID eipMaster
    ) throws FileRulesException, LogbookClientException {
        try {
            Integer sequence = vitamCounterService.getSequence(getTenantParameter(), SequenceType.RULES_SEQUENCE);
            for (FileRulesModel fileRulesModel : fileRulesModelToUpdate) {
                updateFileRules(fileRulesModel, sequence);
            }
            if (!fileRulesModelToInsert.isEmpty()) {
                final JsonNode fileRulesNodeToInsert = JsonHandler.toJsonNode(fileRulesModelToInsert);
                if (fileRulesNodeToInsert != null && fileRulesNodeToInsert.isArray()) {
                    final ArrayNode fileRulesArrayToInsert = (ArrayNode) fileRulesNodeToInsert;
                    commit(fileRulesArrayToInsert);
                }
            }
            for (FileRulesModel fileRulesModel : fileRulesModelToDelete) {
                deleteFileRules(fileRulesModel);
            }
            this.logbookRuleImportManager.updateCommitFileRulesLogbookOperationOkOrKo(
                    StatusCode.OK,
                    eipMaster,
                    fileRulesModelToDelete.size(),
                    fileRulesModelToUpdate.size(),
                    fileRulesModelToInsert.size()
                );
        } catch (
            ReferentialException
            | DocumentAlreadyExistsException
            | InvalidCreateOperationException
            | InvalidParseOperationException
            | SchemaValidationException
            | BadRequestException e
        ) {
            LOGGER.error(e);
            this.logbookRuleImportManager.updateCommitFileRulesLogbookOperationOkOrKo(
                    StatusCode.KO,
                    eipMaster,
                    fileRulesModelToDelete.size(),
                    fileRulesModelToUpdate.size(),
                    fileRulesModelToInsert.size()
                );
            throw new FileRulesException(e);
        }
    }

    private void commit(ArrayNode validatedRules)
        throws ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        if (validatedRules.size() > 0) {
            Integer sequence = vitamCounterService.getNextSequence(getTenantParameter(), SequenceType.RULES_SEQUENCE);
            mongoAccess.insertDocuments(validatedRules, RULES, sequence);
        }
    }

    /**
     * Checks File : checks if a stream of referential data is valid
     *
     * @param rulesToImport rules to import
     * @return The rules to import
     * @throws ReferentialException when there is errors import
     */
    public RuleImportResultSet checkFile(Map<String, FileRulesModel> rulesToImport) throws ReferentialException {
        Map<String, FileRulesModel> rulesInDatabase = getAllRulesInDB();
        RuleImportDiff ruleImportDiff = new RuleImportDiff(rulesToImport, rulesInDatabase);

        List<FileRulesModel> usedRulesToDelete = getUsedRules(ruleImportDiff.getRulesToDelete());

        if (!usedRulesToDelete.isEmpty()) {
            throw new FileRulesDeleteException("used Rules want to be deleted", usedRulesToDelete);
        }

        List<FileRulesModel> usedRulesWithDurationModeUpdate = getUsedRules(
            ruleImportDiff.getRulesWithDurationModeUpdate()
        );

        if (!usedRulesWithDurationModeUpdate.isEmpty()) {
            throw new FileRulesIllegalDurationModeUpdateException(
                "Cannot update rule duration mode (defined/undefined) for used rule",
                usedRulesWithDurationModeUpdate
            );
        }

        List<FileRulesModel> usedUpdatedRules = getUsedRules(ruleImportDiff.getRulesToUpdate());
        List<FileRulesModel> usedUpdateRulesForUpdateUnit = getUsedRules(ruleImportDiff.getRulesToUpdateUnsafely());

        return new RuleImportResultSet(
            ruleImportDiff.getRulesToInsert(),
            ruleImportDiff.getRulesToUpdate(),
            ruleImportDiff.getRulesToDelete(),
            usedUpdatedRules,
            usedUpdateRulesForUpdateUnit
        );
    }

    private Map<String, FileRulesModel> getAllRulesInDB() throws ReferentialException {
        List<FileRules> rulesInDB = findAllFileRulesInDB();
        return rulesInDB
            .stream()
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

    public Map<String, FileRulesModel> getRulesFromCSV(InputStream ruleInputStream)
        throws IOException, FileRulesReadException {
        Map<String, FileRulesModel> rulesModel = new HashMap<>();
        Map<Integer, List<ErrorReport>> errorsMap = new HashMap<>();

        final CsvMapper csvMapper = new CsvMapper();
        MappingIterator<FileRulesCSV> csvReader = csvMapper
            .readerFor(FileRulesCSV.class)
            .with(CsvSchema.emptySchema().withHeader())
            .readValues(ruleInputStream);
        int lineNumber = 1;
        List<FileRulesCSV> rules = csvReader.readAll();
        for (FileRulesCSV rule : rules) {
            lineNumber++;
            List<ErrorReport> errors = new ArrayList<>();

            rule.setRuleDuration(StringUtils.defaultIfEmpty(rule.getRuleDuration(), null));
            rule.setRuleMeasurement(StringUtils.defaultIfEmpty(rule.getRuleMeasurement(), null));

            errors.addAll(checkParametersNotEmpty(rule, lineNumber));
            errors.addAll(checkRuleDuration(rule, lineNumber));

            if (rulesModel.containsKey(rule.getRuleId())) {
                errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_RULEID_DUPLICATION, lineNumber, rule));
            }
            RuleMeasurementEnum ruleMeasurement = null;
            if (rule.getRuleMeasurement() != null) {
                try {
                    ruleMeasurement = RuleMeasurementEnum.getEnumFromType(rule.getRuleMeasurement());
                } catch (IllegalStateException e) {
                    errors.add(
                        new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULEMEASUREMENT, lineNumber, rule)
                    );
                }
            }
            RuleType ruletype = RuleType.getEnumFromName(rule.getRuleType());
            if (ruletype == null) {
                errors.add(
                    new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULETYPE_UNKNOW, lineNumber, rule)
                );
            }
            if (ruletype != null && ruleMeasurement != null && rule.getRuleDuration() != null) {
                errors.addAll(checkAssociationRuleDurationRuleMeasurementLimit(lineNumber, rule));
                errors.addAll(checkRuleDurationWithConfiguration(lineNumber, rule));
            }

            if (!errors.isEmpty()) {
                errorsMap.put(lineNumber, errors);
                rulesModel.put(rule.getRuleId(), new FileRulesModel());
            } else {
                FileRulesModel fileRulesModel = getFileRulesModelFromCSV(rule);
                rulesModel.put(rule.getRuleId(), fileRulesModel);
            }
        }

        if (!errorsMap.isEmpty()) {
            Set<ErrorReport> hasDurationExceededErrors = errorsMap
                .values()
                .stream()
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

    @Nonnull
    private FileRulesModel getFileRulesModelFromCSV(FileRulesCSV rule) {
        FileRulesModel fileRulesModel = new FileRulesModel();
        fileRulesModel.setRuleId(rule.getRuleId());
        fileRulesModel.setRuleType(RuleType.getEnumFromName(rule.getRuleType()));
        fileRulesModel.setRuleDuration(rule.getRuleDuration());
        fileRulesModel.setRuleDescription(rule.getRuleDescription());
        fileRulesModel.setRuleValue(rule.getRuleValue());
        fileRulesModel.setRuleMeasurement(RuleMeasurementEnum.getEnumFromType(rule.getRuleMeasurement()));
        fileRulesModel.setUpdateDate(LocalDateUtil.nowFormatted());
        fileRulesModel.setCreationDate(LocalDateUtil.nowFormatted());
        return fileRulesModel;
    }

    private List<FileRulesModel> getUsedRules(List<FileRulesModel> rules) {
        return rules.stream().filter(this::isRuleUsedByUnit).collect(Collectors.toList());
    }

    private boolean isRuleUsedByUnit(FileRulesModel rule) {
        try {
            List<JsonNode> unitsUsedByRule = checkRuleReferencedByUnitInDatabase(
                fileRulesLinkedToUnitQueryBuilder(rule)
            );
            return !CollectionUtils.isEmpty(unitsUsedByRule);
        } catch (
            MetaDataDocumentSizeException
            | MetaDataExecutionException
            | InvalidParseOperationException
            | MetaDataClientServerException e
        ) {
            throw new RuntimeException("Could not check rule references", e);
        }
    }

    private void generateReport(
        StatusCode statusCode,
        Map<Integer, List<ErrorReport>> errors,
        GUID eipMaster,
        List<FileRulesModel> usedDeletedRules,
        List<FileRulesModel> usedRulesWithDurationModeUpdate,
        List<FileRulesModel> usedUpdatedRules,
        List<FileRulesModel> fileRulesToDelete,
        List<FileRulesModel> fileRulesToUpdate,
        List<FileRulesModel> fileRulesToInsert
    ) throws StorageException {
        final String fileName = eipMaster + ".json";
        InputStream stream = generateReportContent(
            errors,
            usedDeletedRules,
            usedRulesWithDurationModeUpdate,
            usedUpdatedRules,
            fileRulesToDelete,
            fileRulesToUpdate,
            fileRulesToInsert,
            statusCode,
            eipMaster
        );

        try {
            backupService.saveFile(stream, eipMaster, RULES_REPORT, DataCategory.REPORT, fileName);
        } catch (VitamException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private void updateFileRules(FileRulesModel fileRulesModel, Integer sequence)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException, SchemaValidationException, BadRequestException {
        // FIXME use bulk create instead like LogbookMongoDbAccessImpl.
        final UpdateParserSingle updateParser = new UpdateParserSingle(new VarNameAdapter());
        final Update updateFileRules = new Update();
        List<Action> actions = new ArrayList<>();
        SetAction setRuleValue;
        setRuleValue = new SetAction(FileRulesModel.RULE_VALUE, fileRulesModel.getRuleValue());
        actions.add(setRuleValue);
        SetAction setRuleDescription = new SetAction(
            FileRulesModel.RULE_DESCRIPTION,
            fileRulesModel.getRuleDescription()
        );
        actions.add(setRuleDescription);
        SetAction setUpdateDate = new SetAction(UPDATE_DATE, LocalDateUtil.nowFormatted());
        actions.add(setUpdateDate);

        if (fileRulesModel.getRuleMeasurement() != null) {
            SetAction setRuleMeasurement = new SetAction(
                FileRulesModel.RULE_MEASUREMENT,
                fileRulesModel.getRuleMeasurement().name()
            );
            actions.add(setRuleMeasurement);
        } else {
            UnsetAction unsetRuleMeasurement = new UnsetAction(FileRulesModel.RULE_MEASUREMENT);
            actions.add(unsetRuleMeasurement);
        }

        if (fileRulesModel.getRuleDuration() != null) {
            SetAction setRuleDuration = new SetAction(FileRulesModel.RULE_DURATION, fileRulesModel.getRuleDuration());
            actions.add(setRuleDuration);
        } else {
            UnsetAction unsetRuleDuration = new UnsetAction(FileRulesModel.RULE_DURATION);
            actions.add(unsetRuleDuration);
        }

        SetAction setRuleType = new SetAction(FileRulesModel.RULE_TYPE, fileRulesModel.getRuleType().name());
        actions.add(setRuleType);
        updateFileRules.setQuery(eq(FileRulesModel.RULE_ID, fileRulesModel.getRuleId()));
        updateFileRules.addActions(actions.toArray(Action[]::new));
        updateParser.parse(updateFileRules.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        mongoAccess.updateData(queryDslForUpdate, RULES, sequence);
    }

    private void deleteFileRules(FileRulesModel fileRulesModel) {
        final Delete delete = new Delete();
        try {
            delete.setQuery(eq(FileRulesModel.RULE_ID, fileRulesModel.getRuleId()));
            DbRequestResult result = mongoAccess.deleteCollectionForTesting(RULES, delete);
            result.close();
        } catch (InvalidCreateOperationException | DatabaseException e) {
            LOGGER.error(e);
        }
    }

    private JsonNode fileRulesLinkedToUnitQueryBuilder(FileRulesModel fileRulesModels) {
        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        try {
            String ruleFieldName = "#management." + fileRulesModels.getRuleType() + ".Rules.Rule";
            selectMultiple.setQuery(eq(ruleFieldName, fileRulesModels.getRuleId()));
            selectMultiple.addUsedProjection(VitamFieldsHelper.id());
            selectMultiple.setLimitFilter(0, 1);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new RuntimeException("Query construction not valid ", e);
        }
        return selectMultiple.getFinalSelect();
    }

    private List<ErrorReport> checkRuleDuration(FileRulesCSV fileRulesCSV, int line) {
        List<ErrorReport> errors = new ArrayList<>();
        if (
            fileRulesCSV.getRuleDuration() != null &&
            !UNLIMITED_RULE_DURATION.equalsIgnoreCase(fileRulesCSV.getRuleDuration())
        ) {
            final int duration = parseWithDefault(fileRulesCSV.getRuleDuration());
            if (duration < 0) {
                errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULEDURATION, line, fileRulesCSV));
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

    private List<ErrorReport> checkParametersNotEmpty(FileRulesCSV fileRulesModel, int line) {
        List<ErrorReport> errors = new ArrayList<>();
        List<String> missingParam = new ArrayList<>();
        if (StringUtils.isEmpty(fileRulesModel.getRuleId())) {
            missingParam.add(FileRulesCSV.TAG_RULE_ID);
        }
        if (StringUtils.isEmpty(fileRulesModel.getRuleType())) {
            missingParam.add(FileRulesCSV.TAG_RULE_TYPE);
        }
        if (StringUtils.isEmpty(fileRulesModel.getRuleValue())) {
            missingParam.add(FileRulesCSV.TAG_RULE_VALUE);
        }

        boolean isHoldRule = RuleType.HoldRule.isNameEquals(fileRulesModel.getRuleType());
        boolean hasRuleDuration = fileRulesModel.getRuleDuration() != null;
        boolean hasRuleMeasurement = fileRulesModel.getRuleMeasurement() != null;

        if (isHoldRule) {
            // Rule duration is optional for HoldRules
            // But if duration is present, both duration value & unit must be provided

            if (hasRuleMeasurement && !hasRuleDuration) {
                missingParam.add(FileRulesCSV.TAG_RULE_DURATION);
            }
            if (hasRuleDuration && !hasRuleMeasurement) {
                missingParam.add(FileRulesCSV.TAG_RULE_MEASUREMENT);
            }
        } else {
            if (!hasRuleDuration) {
                missingParam.add(FileRulesCSV.TAG_RULE_DURATION);
            }
            if (!hasRuleMeasurement) {
                missingParam.add(FileRulesCSV.TAG_RULE_MEASUREMENT);
            }
        }

        if (!missingParam.isEmpty()) {
            errors.add(
                new ErrorReport(
                    FileRulesErrorCode.STP_IMPORT_RULES_NOT_CSV_FORMAT,
                    line,
                    String.join(", ", missingParam)
                )
            );
        }

        return errors;
    }

    private List<ErrorReport> checkAssociationRuleDurationRuleMeasurementLimit(int line, FileRulesCSV fileRulesCSV) {
        List<ErrorReport> errors = new ArrayList<>();
        try {
            if (
                !fileRulesCSV.getRuleDuration().equalsIgnoreCase(UNLIMITED_RULE_DURATION) &&
                ((fileRulesCSV.getRuleMeasurement().equalsIgnoreCase(RuleMeasurementEnum.YEAR.getType()) &&
                        Integer.parseInt(fileRulesCSV.getRuleDuration()) > YEAR_LIMIT) ||
                    (fileRulesCSV.getRuleMeasurement().equalsIgnoreCase(RuleMeasurementEnum.MONTH.getType()) &&
                        Integer.parseInt(fileRulesCSV.getRuleDuration()) > MONTH_LIMIT) ||
                    (fileRulesCSV.getRuleMeasurement().equalsIgnoreCase(RuleMeasurementEnum.DAY.getType()) &&
                        Integer.parseInt(fileRulesCSV.getRuleDuration()) > DAY_LIMIT))
            ) {
                errors.add(
                    new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_TOTALDURATION, line, fileRulesCSV)
                );
            }
        } catch (NumberFormatException e) {
            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_TOTALDURATION, line, fileRulesCSV));
        }

        return errors;
    }

    private List<ErrorReport> checkRuleDurationWithConfiguration(int line, FileRulesCSV fileRulesCSV) {
        List<ErrorReport> errors = new ArrayList<>();
        String ruleType = fileRulesCSV.getRuleType();

        String[] min = vitamRuleService.getMinimumRuleDuration(getTenantParameter(), ruleType).split(" ");
        int durationConf = 0;
        if (min.length == 2) {
            durationConf = calculDuration(min[0], min[1]);
        }
        int durationRule = calculDuration(fileRulesCSV.getRuleDuration(), fileRulesCSV.getRuleMeasurement());

        if (durationRule < durationConf) {
            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_RULEDURATION_EXCEED, line, fileRulesCSV));
        }

        return errors;
    }

    private int calculDuration(String ruleDuration, String ruleMeasurement) {
        int duration = 0;

        if (ruleDuration.equalsIgnoreCase(UNLIMITED_RULE_DURATION)) {
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

    @Override
    public FileRules findDocumentById(String id) {
        return (FileRules) mongoAccess.getDocumentByUniqueId(id, RULES, FileRules.RULEID);
    }

    @Override
    public RequestResponseOK<FileRules> findDocuments(JsonNode select) throws ReferentialException {
        try (DbRequestResult result = mongoAccess.findDocuments(select, RULES)) {
            return result.getRequestResponseOK(select, FileRules.class);
        }
    }

    private List<JsonNode> checkRuleReferencedByUnitInDatabase(JsonNode select)
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException, MetaDataClientServerException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            final JsonNode unitsResultNode = metaDataClient.selectUnits(select);
            return RequestResponseOK.getFromJsonNode(unitsResultNode).getResults();
        }
    }

    /**
     * generate Error Report
     *
     * @param errors the list of error for generated errors
     * @param usedDeletedRules list of fileRules that attempt to be deleted but have reference to unit
     * @param usedRulesWithDurationModeUpdate list of fileRules referenced by a unit, with duration mode update (defined to undefined, or undefined to defined)
     * @param usedUpdatedRules list of fileRules that attempt to be updated but have reference to unit
     * @param status status
     * @param eipMaster eipMaster
     * @return the error report inputStream
     */
    public InputStream generateReportContent(
        Map<Integer, List<ErrorReport>> errors,
        List<FileRulesModel> usedDeletedRules,
        List<FileRulesModel> usedRulesWithDurationModeUpdate,
        List<FileRulesModel> usedUpdatedRules,
        List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToUpdate,
        List<FileRulesModel> fileRulesModelToInsert,
        StatusCode status,
        GUID eipMaster
    ) {
        final FileRulesManagementReport reportFinal = new FileRulesManagementReport();
        final HashMap<String, String> guidmasterNode = new HashMap<>();
        final ArrayList<String> usedDeletedFileRuleList = new ArrayList<>();
        final ArrayList<String> usedUpdatedFileRuleList = new ArrayList<>();
        guidmasterNode.put(ReportConstants.EV_TYPE, STP_IMPORT_RULES);
        guidmasterNode.put(ReportConstants.EV_DATE_TIME, LocalDateUtil.nowFormatted());
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
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION, error.getMissingInformations());
                        break;
                    case STP_IMPORT_RULES_RULEID_DUPLICATION:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION, error.getFileRulesCSV().getRuleId());
                        break;
                    case STP_IMPORT_RULES_WRONG_RULEDURATION:
                        errorNode.put(ADDITIONAL_INFORMATION, error.getFileRulesCSV().getRuleDuration());
                        break;
                    case STP_IMPORT_RULES_WRONG_RULEMEASUREMENT:
                        errorNode.put(
                            ReportConstants.ADDITIONAL_INFORMATION,
                            error.getFileRulesCSV().getRuleMeasurement()
                        );
                        break;
                    case STP_IMPORT_RULES_WRONG_RULETYPE_UNKNOW:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION, error.getFileRulesCSV().getRuleType());
                        break;
                    case STP_IMPORT_RULES_WRONG_TOTALDURATION:
                        errorNode.put(
                            ReportConstants.ADDITIONAL_INFORMATION,
                            error.getFileRulesCSV().getRuleDuration() +
                            " " +
                            error.getFileRulesCSV().getRuleMeasurement()
                        );
                        break;
                    case STP_IMPORT_RULES_RULEDURATION_EXCEED:
                        ObjectNode info = JsonHandler.createObjectNode();
                        info.put("RuleType", error.getFileRulesCSV().getRuleType());
                        info.put(
                            "RuleDurationMin",
                            vitamRuleService.getMinimumRuleDuration(
                                getTenantParameter(),
                                error.getFileRulesCSV().getRuleType()
                            )
                        );
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
        reportFinal.setUsedRulesWithDurationModeUpdate(rulesToRuleIds(usedRulesWithDurationModeUpdate));
        reportFinal.setFileRulesToDelete(rulesToRuleIds(fileRulesModelToDelete));
        reportFinal.setFileRulesToUpdate(rulesToRuleIds(fileRulesModelToUpdate));
        reportFinal.setFileRulesToImport(rulesToRuleIds(fileRulesModelToInsert));
        return new ByteArrayInputStream(JsonHandler.unprettyPrint(reportFinal).getBytes(StandardCharsets.UTF_8));
    }

    private List<String> rulesToRuleIds(List<FileRulesModel> rules) {
        return rules.stream().map(FileRulesModel::getRuleId).collect(Collectors.toList());
    }

    /**
     * get the rule file from collection
     *
     * @param tenant
     * @return ArrayNode
     * @throws InvalidParseOperationException
     */
    private ArrayNode getRuleFromCollection(int tenant) throws InvalidParseOperationException {
        return backupService.getCollectionInJson(RULES, tenant);
    }

    /**
     * get the last rule file from offer
     *
     * @param tenant
     * @return ArrayNode
     */
    private ArrayNode getRuleFromOffer(int tenant) {
        Integer originalTenant = VitamThreadUtils.getVitamSession().getTenantId();
        VitamThreadUtils.getVitamSession().setTenantId(tenant);

        Optional<CollectionBackupModel> collectionBackup = restoreBackupService.readLatestSavedFile(
            VitamConfiguration.getDefaultStrategy(),
            null,
            RULES
        );
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
    boolean checkRuleConformity(ArrayNode array1, ArrayNode array2, int tenant) {
        if (!array1.toString().equals(array2.toString())) {
            JsonNode patch = JsonDiff.asJson(array1, array2);
            alertService.createAlert(
                "Check failed: the security save of the rules repository of tenant " +
                tenant +
                " is not equal to its value in database.\n" +
                patch
            );
            return false;
        }

        return true;
    }

    public void checkRuleConformity(List<Integer> tenants) throws ReferentialException {
        int threadPoolSize = Math.min(this.ruleAuditThreadPoolSize, tenants.size());
        ExecutorService executorService = ExecutorUtils.createScalableBatchExecutorService(threadPoolSize);

        try {
            List<CompletableFuture<Boolean>> completableFutures = new ArrayList<>();

            for (Integer tenantId : tenants) {
                CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync(
                    () -> {
                        Thread.currentThread().setName("CheckRuleConformity-" + tenantId);
                        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                        try {
                            return checkRuleConformity(
                                getRuleFromCollection(tenantId),
                                getRuleFromOffer(tenantId),
                                tenantId
                            );
                        } catch (Exception e) {
                            throw new RuntimeException("An error occurred during rule audit for tenant " + tenantId, e);
                        }
                    },
                    executorService
                );
                completableFutures.add(completableFuture);
            }

            boolean allTenantsSucceeded = true;
            for (CompletableFuture<Boolean> completableFuture : completableFutures) {
                try {
                    Boolean success = completableFuture.get();
                    if (!success) {
                        allTenantsSucceeded = false;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ReferentialException("Rule audit interrupted", e);
                } catch (ExecutionException e) {
                    LOGGER.error("Rule audit operation failed", e);
                    allTenantsSucceeded = false;
                }
            }

            if (!allTenantsSucceeded) {
                throw new ReferentialException("One or more tenants failed rule audit");
            }
        } finally {
            executorService.shutdown();
        }
    }
}
