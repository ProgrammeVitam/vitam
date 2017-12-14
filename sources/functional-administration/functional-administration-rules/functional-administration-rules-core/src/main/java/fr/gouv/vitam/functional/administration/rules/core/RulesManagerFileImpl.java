/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.functional.administration.rules.core;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
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
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.ErrorReport;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.FileRulesErrorCode;
import fr.gouv.vitam.functional.administration.common.FilesSecurisator;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.ReferentialFileUtils;
import fr.gouv.vitam.functional.administration.common.ReportConstants;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.RuleTypeEnum;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesCsvException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesDeleteException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesUpdateException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
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
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;


/**
 * RulesManagerFileImpl
 * <p>
 * Manage the Rules File features
 */

public class RulesManagerFileImpl implements ReferentialFile<FileRules>, VitamAutoCloseable {

    private static final String RULES_FILE_STREAMIS_A_MANDATORY_PARAMETER = "rulesFileStreamis a mandatory parameter";
    private static final String RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER = "rulesFileStream is a mandatory parameter";
    private static final String RULES_PROCESS_IMPORT_ALREADY_EXIST =
        "There is already on file rules import in progress";
    private static final String DELETE_RULES_LINKED_TO_UNIT =
        "Error During Delete RuleFiles because this rule is linked to unit.";
    private static final String INVALID_CSV_FILE = "Invalid CSV File";
    private static final String TXT = ".txt";
    private static final String CSV = "csv";
    private static final String JSON = "json";
    private static final String FILE_NAME = "FileName";

    private static final String TMP = "tmp";
    private static final String RULE_MEASUREMENT = "RuleMeasurement";
    private static final String RULE_DURATION = "RuleDuration";
    private static final String RULE_DESCRIPTION = "RuleDescription";
    private static final String RULE_VALUE = "RuleValue";
    private static final String RULE_TYPE = "RuleType";
    private static final String UPDATE_DATE = "UpdateDate";
    private static final String UNLIMITED = "unlimited";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RulesManagerFileImpl.class);
    private static final String STP_IMPORT_RULES_SUCCESS =
        "Succès du processus d'enregistrement de la copie du référentiel des règles de gestion";

    private static final String USED_DELETED_RULES = "usedDeletedRules";
    private static final String USED_UPDATED_RULES = "usedUpdatedRules";
    private static final String RESULTS = "$results";
    private final MongoDbAccessAdminImpl mongoAccess;
    private static final String RULE_ID = "RuleId";
    private final VitamCounterService vitamCounterService;
    private static final String STORAGE_RULES_WORKSPACE = "RULES";
    private final WorkspaceClientFactory workspaceClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final FilesSecurisator securisator;



    // event in logbook
    private static String UPDATE_RULES_ARCHIVE_UNITS = "UPDATE_RULES_ARCHIVE_UNITS";
    private static final String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private static final String CHECK_RULES = "CHECK_RULES";
    private static final String CHECK_RULES_INVALID_CSV = "INVALID_CSV";
    private static final String CHECK_RULES_IMPORT_IN_PROCESS = "IMPORT_IN_PROCESS";
    private static final String COMMIT_RULES = "COMMIT_RULES";
    private static final String USED_DELETED_RULE_IDS = "usedDeletedRuleIds";
    private static final String DELETED_RULE_IDS = "deletedRuleIds";
    private static final String USED_UPDATED_RULE_IDS = "usedUpdatedRuleIds";
    private static final String RULES_REPORT = "RULES_REPORT";
    private static final String DEFAULT_STRATEGY = "default";


    private static String NB_DELETED = "nbDeleted";
    private static String NB_UPDATED = "nbUpdated";
    private static String NB_INSERTED = "nbInserted";
    private static int YEAR_LIMIT = 999;
    private static int MONTH_LIMIT = YEAR_LIMIT * 12;
    private static int DAY_LIMIT = MONTH_LIMIT * 30;



    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access admin configuration
     * @param securisator
     */
    public RulesManagerFileImpl(MongoDbAccessAdminImpl dbConfiguration, VitamCounterService vitamCounterService,
        FilesSecurisator securisator) {
        this(dbConfiguration, vitamCounterService, securisator,
            LogbookOperationsClientFactory.getInstance(),
            StorageClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance(), MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    RulesManagerFileImpl(MongoDbAccessAdminImpl dbConfiguration,
        VitamCounterService vitamCounterService,
        FilesSecurisator securisator, LogbookOperationsClientFactory logbookOperationsClientFactory,
        StorageClientFactory storageClientFactory, WorkspaceClientFactory workspaceClientFactory,
        MetaDataClientFactory metaDataClientFactory) {
        this.mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        this.securisator = securisator;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;

    }

    @Override
    public void importFile(InputStream rulesFileStream, String filename)
        throws IOException, InvalidParseOperationException, ReferentialException, InvalidCreateOperationException,
        LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {
        ParametersChecker.checkParameter(RULES_FILE_STREAMIS_A_MANDATORY_PARAMETER, rulesFileStream);
        File file = convertInputStreamToFile(rulesFileStream, CSV);
        Map<Integer, List<ErrorReport>> errors = new HashMap<>();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(getTenant());
        final GUID eip1 = GUIDFactory.newOperationLogbookGUID(getTenant());
        List<FileRulesModel> usedDeletedRulesForReport = new ArrayList<>();
        List<FileRulesModel> usedUpdateRulesForReport = new ArrayList<>();
        Set<String> notUsedDeletedRulesForReport = new HashSet<>();
        Set<String> notUsedUpdateRulesForReport = new HashSet<>();
        List<FileRulesModel> fileRulesModelToInsert = new ArrayList<>();
        List<FileRulesModel> fileRulesModelToDelete = new ArrayList<>();
        List<FileRulesModel> fileRulesModelToUpdate = new ArrayList<>();
        List<FileRulesModel> fileRulesModelsToImport = new ArrayList<>();
        ArrayNode validatedRules = JsonHandler.createArrayNode();
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            try {
                initStpImportRulesLogbookOperation(eip, client);
                if (!isImportOperationInProgress(client)) {
                    /* To process import validate the file first */
                    validatedRules =
                        checkFile(new FileInputStream(file), errors, usedDeletedRulesForReport,
                            usedUpdateRulesForReport,
                            notUsedDeletedRulesForReport, notUsedUpdateRulesForReport);
                    if (validatedRules != null) {
                        generateReportCommitAndSecureFileRules(file, eip, eip1, notUsedDeletedRulesForReport,
                            fileRulesModelToInsert, fileRulesModelToDelete, fileRulesModelToUpdate, validatedRules,
                            errors,
                            filename, client);
                    }
                } else {
                    throw new FileRulesImportInProgressException(RULES_PROCESS_IMPORT_ALREADY_EXIST);
                }
            } catch (FileRulesDeleteException e) {
                generateReportWhenFileRulesDeletedExceptionAppend(file,
                    errors, eip, eip1, usedDeletedRulesForReport, usedUpdateRulesForReport,
                    notUsedDeletedRulesForReport, fileRulesModelToInsert, fileRulesModelToDelete,
                    fileRulesModelToUpdate, fileRulesModelsToImport, validatedRules, filename, client);
                throw e;
            } catch (FileRulesUpdateException e) {
                generateReportWhenFileRulesUpdatedExceptionAppend(file,
                    errors, eip, eip1, usedDeletedRulesForReport, usedUpdateRulesForReport,
                    notUsedDeletedRulesForReport, fileRulesModelToInsert, fileRulesModelToDelete,
                    fileRulesModelToUpdate, fileRulesModelsToImport, validatedRules, filename, client);
            } catch (FileRulesCsvException e) {
                try {
                    updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(CHECK_RULES_INVALID_CSV, eip, client);
                    generateReport(errors, eip, usedDeletedRulesForReport, usedUpdateRulesForReport, client);
                    updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename, client);
                } catch (StorageException | LogbookClientServerException | LogbookClientAlreadyExistsException |
                    LogbookClientBadRequestException e1) {
                    throw e1;
                }
                throw e;
            } catch (FileRulesException e) {
                throw e;
            } catch (FileRulesImportInProgressException e) {
                updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(CHECK_RULES_IMPORT_IN_PROCESS, eip,
                    client);
                updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename, client);
                throw new FileRulesImportInProgressException(RULES_PROCESS_IMPORT_ALREADY_EXIST);
            } catch (LogbookClientException e) {
                throw new FileRulesException(e);
            } finally {
                file.delete();
            }
        }
    }

    private void generateReportCommitAndSecureFileRules(File file, final GUID eip, final GUID eip1,
        Set<String> notUsedDeletedRulesForReport, List<FileRulesModel> fileRulesModelToInsert,
        List<FileRulesModel> fileRulesModelToDelete, List<FileRulesModel> fileRulesModelToUpdate,
        ArrayNode validatedRules, Map<Integer, List<ErrorReport>> errors, String filename,
        LogbookOperationsClient client)
        throws IOException, ReferentialException, InvalidParseOperationException,
        InvalidCreateOperationException {
        List<FileRulesModel> fileRulesModelsToImport;
        List<FileRules> fileRulesInDb = findAllFileRulesQueryBuilder();
        List<FileRulesModel> fileRulesModelsInDb = transformFileRulesToFileRulesModel(fileRulesInDb);
        fileRulesModelsToImport = transformJsonNodeToFileRulesModel(validatedRules);
        createListToimportUpdateDelete(fileRulesModelsToImport, fileRulesModelsInDb,
            fileRulesModelToDelete, fileRulesModelToUpdate, fileRulesModelToInsert);
        try {
            updateCheckFileRulesLogbookOperationOk(CHECK_RULES, StatusCode.OK,
                notUsedDeletedRulesForReport,
                eip, client);
            generateReport(errors, eip, new ArrayList<>(), new ArrayList<>(), client);

            commitRules(fileRulesModelToUpdate, fileRulesModelToDelete, validatedRules,
                fileRulesModelToInsert,
                fileRulesModelsToImport, eip, client);

            final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();
            final Digest digest = new Digest(digestType);
            digest.update(new FileInputStream(file));
            store(eip, new FileInputStream(file), CSV, digest.toString());
            storeJson(eip);
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.OK, filename, client);
        } catch (final FileRulesException e) {
            LOGGER.error(e);
            throw e;
        } catch (StorageException | LogbookClientServerException |
            LogbookClientBadRequestException | LogbookClientAlreadyExistsException e) {
            LOGGER.error(e);
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename, client);
            throw new FileRulesException(e);
        }
    }


    /**
     * Generate Report When FileRules Updated Exception Append
     *
     * @param file to import
     * @param errors errors of the report to build
     * @param eip eip for logbookOperation
     * @param eip1 eip1 for logbookOperation
     * @param usedDeletedRulesForReport used Deleted Rules For Report
     * @param usedUpdateRulesForReport used Update Rules For Report
     * @param notUsedDeletedRulesForReport not Used Deleted Rules For Report
     * @param fileRulesModelToInsert Rules Model To Insert
     * @param fileRulesModelToDelete Rules Model To Delete
     * @param fileRulesModelToUpdate Rules Model To Update
     * @param fileRulesModelsToImport Rules Models To Import
     * @param validatedRules Rules to import
     * @param filename the filename of the file to import
     * @throws IOException
     * @throws FileNotFoundException
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     * @throws FileRulesException
     */
    private void generateReportWhenFileRulesUpdatedExceptionAppend(File file, Map<Integer, List<ErrorReport>> errors,
        final GUID eip, final GUID eip1, List<FileRulesModel> usedDeletedRulesForReport,
        List<FileRulesModel> usedUpdateRulesForReport, Set<String> notUsedDeletedRulesForReport,
        List<FileRulesModel> fileRulesModelToInsert, List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToUpdate, List<FileRulesModel> fileRulesModelsToImport,
        ArrayNode validatedRules, String filename, LogbookOperationsClient client)
        throws IOException, FileNotFoundException, ReferentialException, InvalidParseOperationException,
        InvalidCreateOperationException, FileRulesException {
        try {
            generateReport(errors, eip, usedDeletedRulesForReport, usedUpdateRulesForReport, client);
            Set<String> usedUpdateRules = new HashSet<>();
            for (FileRulesModel fileRuleModel : usedUpdateRulesForReport) {
                usedUpdateRules.add(fileRuleModel.getRuleId());
            }
            updateCheckFileRulesLogbookOperationForUpdate(usedUpdateRules,
                notUsedDeletedRulesForReport,
                eip, client);

            commitRules(usedUpdateRulesForReport, fileRulesModelToDelete, validatedRules,
                fileRulesModelToInsert,
                fileRulesModelsToImport, eip, client);

            final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();
            final Digest digest = new Digest(digestType);
            digest.update(new FileInputStream(file));

            store(eip, new FileInputStream(file), CSV, digest.toString());
            storeJson(eip);

            if (!usedUpdateRulesForReport.isEmpty()) {
                // #2201 - we now launch the process that will update units
                launchWorkflow(usedUpdateRulesForReport, client);
            }
            // TODO #2201 : Create Workflow for update AU linked to unit
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.WARNING, filename, client);
        } catch (LogbookClientServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException | StorageException e) {
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename, client);
            throw new FileRulesException(e);
        } catch (FileRulesException e) {
            throw new FileRulesException(e);
        }
    }

    /**
     * Generate Report When File Rules Deleted Exception Append
     *
     * @param file to import
     * @param errors errors of the report to build
     * @param eip eip for logbookOperation
     * @param eip1 eip1 for logbookOperation
     * @param usedDeletedRulesForReport used Deleted Rules For Report
     * @param usedUpdateRulesForReport used Update Rules For Report
     * @param notUsedDeletedRulesForReport not Used Deleted Rules For Report
     * @param fileRulesModelToInsert Rules Model To Insert
     * @param fileRulesModelToDelete Rules Model To Delete
     * @param fileRulesModelToUpdate Rules Model To Update
     * @param fileRulesModelsToImport Rules Models To Import
     * @param validatedRules Rules to import
     * @param filename filename of the file to import
     * @throws IOException
     * @throws FileNotFoundException
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     * @throws FileRulesException
     */
    private void generateReportWhenFileRulesDeletedExceptionAppend(File file, Map<Integer, List<ErrorReport>> errors,
        final GUID eip, final GUID eip1, List<FileRulesModel> usedDeletedRulesForReport,
        List<FileRulesModel> usedUpdateRulesForReport, Set<String> notUsedDeletedRulesForReport,
        List<FileRulesModel> fileRulesModelToInsert, List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToUpdate, List<FileRulesModel> fileRulesModelsToImport,
        ArrayNode validatedRules, String filename, LogbookOperationsClient client)
        throws IOException, FileNotFoundException, ReferentialException, InvalidParseOperationException,
        InvalidCreateOperationException, FileRulesException {
        try {
            generateReport(errors, eip, usedDeletedRulesForReport, usedUpdateRulesForReport, client);
            Set<String> fileRulesIdLinkedToUnitForDelete = new HashSet<>();
            for (FileRulesModel fileRuleModel : usedDeletedRulesForReport) {
                fileRulesIdLinkedToUnitForDelete.add(fileRuleModel.getRuleId());
            }
            updateCheckFileRulesLogbookOperationForDelete(CHECK_RULES, StatusCode.KO,
                fileRulesIdLinkedToUnitForDelete,
                eip, client);
            LOGGER.error(String.format(DELETE_RULES_LINKED_TO_UNIT));
            throw new FileRulesException(String.format(DELETE_RULES_LINKED_TO_UNIT));
        } catch (LogbookClientServerException | LogbookClientBadRequestException |
            LogbookClientAlreadyExistsException | StorageException e) {
            updateStpImportRulesLogbookOperation(eip, eip1, StatusCode.KO, filename, client);
            throw new FileRulesException(e);
        }
    }

    /**
     * Check File Linked To Au for generated errors report
     *
     * @param validatedRules the rules to check
     * @param filesRulesDeleted file rules deleted
     * @param filesRulesUpdated file rules updated
     * @param fileRulesNotLinkedToUnitForDelete file rules not linked to unit for delete
     * @param fileRulesNotLinkedToUnitForUpdate file rules not linked to unit for update
     * @throws FileRulesException
     * @throws InvalidParseOperationException
     */
    public void checkRulesLinkedToAu(ArrayNode validatedRules, List<FileRulesModel> filesRulesDeleted,
        List<FileRulesModel> filesRulesUpdated, Set<String> fileRulesNotLinkedToUnitForDelete,
        Set<String> fileRulesNotLinkedToUnitForUpdate)
        throws FileRulesException, InvalidParseOperationException {
        List<FileRules> fileRulesInDb = findAllFileRulesQueryBuilder();
        List<FileRulesModel> fileRulesModelsInDb = transformFileRulesToFileRulesModel(fileRulesInDb);
        List<FileRulesModel> fileRulesModelToDelete = new ArrayList<>();
        List<FileRulesModel> fileRulesModelToInsert = new ArrayList<>();
        List<FileRulesModel> fileRulesModelToUpdate = new ArrayList<>();
        List<FileRulesModel> fileRulesModelsToImport = transformJsonNodeToFileRulesModel(validatedRules);
        createListToimportUpdateDelete(fileRulesModelsToImport, fileRulesModelsInDb,
            fileRulesModelToDelete, fileRulesModelToUpdate, fileRulesModelToInsert);
        Set<String> fileRulesIdLinkedToUnitForDelete = new HashSet<>();
        if (fileRulesModelToDelete.size() > 0) {
            if (checkUnitLinkedToFileRules(fileRulesModelToDelete, fileRulesIdLinkedToUnitForDelete,
                fileRulesNotLinkedToUnitForDelete)) {
                // Generate FileRules linkedToUnit for error report
                for (FileRulesModel fileRulesModel : fileRulesModelToDelete) {
                    if (fileRulesIdLinkedToUnitForDelete.contains(fileRulesModel.getRuleId())) {
                        filesRulesDeleted.add(fileRulesModel);
                    }
                }
            }
        }
        if (fileRulesModelToUpdate.size() > 0) {
            Set<String> fileRulesIdLinkedToUnit = new HashSet<>();
            if (checkUnitLinkedToFileRules(fileRulesModelToUpdate, fileRulesIdLinkedToUnit,
                fileRulesNotLinkedToUnitForUpdate)) {
                for (FileRulesModel fileRulesModel : fileRulesModelToUpdate) {
                    if (fileRulesIdLinkedToUnit.contains(fileRulesModel.getRuleId())) {
                        filesRulesUpdated.add(fileRulesModel);
                    }
                }
            }
        }
    }

    /**
     * update STP_IMPORT_RULES LogbookOperation
     *
     * @param eip GUID master
     * @param eip1 GUID of the eventIdentifier
     * @param status Logbook status
     * @throws InvalidParseOperationException
     */
    private void updateStpImportRulesLogbookOperation(final GUID eip, final GUID eip1, StatusCode status,
        String filename, LogbookOperationsClient client)
        throws InvalidParseOperationException {
        final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
            .newLogbookOperationParameters(eip1, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                status, VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, status),
                eip1);
        ReferentialFileUtils.addFilenameInLogbookOperation(filename, logbookParametersEnd);
        updateLogBookEntry(logbookParametersEnd, client);
    }

    /**
     * Init logbook operation STP_IMPORT_RULES
     *
     * @param eip GUID master
     */
    private void initStpImportRulesLogbookOperation(final GUID eip, LogbookOperationsClient client) {
        final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
            .newLogbookOperationParameters(eip, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.STARTED), eip);
        createLogBookEntry(logbookParametersStart, client);
    }

    /**
     * Method that is responsible of launching workflow that will update archive units after rules has been updated
     *
     * @param usedUpdateRulesForReport file rules used to a unit
     * @throws InvalidParseOperationException
     */
    private void launchWorkflow(List<FileRulesModel> usedUpdateRulesForReport, LogbookOperationsClient client)
        throws InvalidParseOperationException {

        try (ProcessingManagementClient processManagementClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {
            ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final FileRulesModel ruleNode : usedUpdateRulesForReport) {
                arrayNode.add(JsonHandler.toJsonNode(ruleNode));
            }
            final GUID updateOperationGUID = GUIDFactory.newOperationLogbookGUID(getTenant());
            final LogbookOperationParameters logbookUpdateParametersStart = LogbookParametersFactory
                .newLogbookOperationParameters(updateOperationGUID, UPDATE_RULES_ARCHIVE_UNITS,
                    updateOperationGUID,
                    LogbookTypeProcess.UPDATE,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(UPDATE_RULES_ARCHIVE_UNITS, StatusCode.STARTED),
                    updateOperationGUID);
            createLogBookEntry(logbookUpdateParametersStart, client);
            try {
                copyFilesOnWorkspaceUpdateWorkflow(
                    JsonHandler.writeToInpustream(arrayNode),
                    updateOperationGUID.getId());

                processManagementClient.initVitamProcess(Contexts.UPDATE_RULES_ARCHIVE_UNITS.name(),
                    updateOperationGUID.getId(), UPDATE_RULES_ARCHIVE_UNITS);
                LOGGER.debug("Started Update in Resource");
                RequestResponse<ItemStatus> ret =
                    processManagementClient
                        .updateOperationActionProcess(ProcessAction.RESUME.getValue(),
                            updateOperationGUID.getId());

                if (Status.ACCEPTED.getStatusCode() != ret.getStatus()) {
                    throw new VitamClientException("Process couldnt be executed");
                }

            } catch (ContentAddressableStorageAlreadyExistException |
                ContentAddressableStorageServerException | InternalServerException |
                VitamClientException | BadRequestException e) {
                LOGGER.error(e);
                final LogbookOperationParameters logbookUpdateParametersEnd =
                    LogbookParametersFactory
                        .newLogbookOperationParameters(updateOperationGUID,
                            UPDATE_RULES_ARCHIVE_UNITS,
                            updateOperationGUID,
                            LogbookTypeProcess.UPDATE,
                            StatusCode.KO,
                            VitamLogbookMessages.getCodeOp(UPDATE_RULES_ARCHIVE_UNITS,
                                StatusCode.KO),
                            updateOperationGUID);
                updateLogBookEntry(logbookUpdateParametersEnd, client);
            }
        }
    }

    /**
     * Commit in mongo/elastic for update, delete, insert
     *
     * @param fileRulesModelToUpdate fileRulesModelToUpdate
     * @param fileRulesModelToDelete fileRulesModelToDelete
     * @param validatedRules all the given rules to import
     * @param fileRulesModelToInsert fileRulesModelToInsert
     * @param fileRulesModelsToImport fileRulesModelsToImport
     * @return true if commited
     * @throws FileRulesException
     */
    private boolean commitRules(List<FileRulesModel> fileRulesModelToUpdate,
        List<FileRulesModel> fileRulesModelToDelete,
        ArrayNode validatedRules, List<FileRulesModel> fileRulesModelToInsert,
        List<FileRulesModel> fileRulesModelsToImport, GUID eipMaster, LogbookOperationsClient client)
        throws FileRulesException, LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {
        boolean secureRules = false;
        try {
            Integer sequence = vitamCounterService
                .getSequence(ParameterHelper.getTenantParameter(), SequenceType.RULES_SEQUENCE);
            for (FileRulesModel fileRulesModel : fileRulesModelToUpdate) {
                updateFileRules(fileRulesModel, sequence);
            }
            if (!fileRulesModelToInsert.isEmpty() && fileRulesModelToInsert.containsAll(fileRulesModelsToImport)) {
                commit(validatedRules);
                secureRules = true;
            } else if (!fileRulesModelToInsert.isEmpty()) {
                final JsonNode fileRulesNodeToInsert = JsonHandler.toJsonNode(fileRulesModelToInsert);
                if (fileRulesNodeToInsert != null && fileRulesNodeToInsert.isArray()) {
                    final ArrayNode fileRulesArrayToInsert = (ArrayNode) fileRulesNodeToInsert;
                    commit(fileRulesArrayToInsert);
                    secureRules = true;
                }
            }
            for (FileRulesModel fileRulesModel : fileRulesModelToDelete) {
                deleteFileRules(fileRulesModel, FunctionalAdminCollections.RULES);
            }
            updateCommitFileRulesLogbookOperationOkOrKo(COMMIT_RULES, StatusCode.OK, eipMaster,
                fileRulesModelToUpdate, fileRulesModelToDelete, fileRulesModelToInsert, client);

            return secureRules;
        } catch (ReferentialException | InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(e);
            updateCommitFileRulesLogbookOperationOkOrKo(COMMIT_RULES, StatusCode.KO, eipMaster,
                fileRulesModelToUpdate, fileRulesModelToDelete, fileRulesModelToInsert, client);
            throw new FileRulesException(e);
        }
    }

    private void commit(ArrayNode validatedRules)
        throws ReferentialException, LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException, InvalidParseOperationException, InvalidCreateOperationException {
        Integer sequence = vitamCounterService
            .getNextSequence(ParameterHelper.getTenantParameter(), SequenceType.RULES_SEQUENCE);
        mongoAccess.insertDocuments(validatedRules, FunctionalAdminCollections.RULES, sequence);

    }

    private void store(GUID eipMaster, InputStream stream, String extension, String digest)
        throws ReferentialException, InvalidParseOperationException, InvalidCreateOperationException,
        LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {
        Integer sequence = vitamCounterService
            .getSequence(ParameterHelper.getTenantParameter(), SequenceType.RULES_SEQUENCE);
        securisator.secureFiles(sequence, stream, extension, eipMaster, digest, LogbookTypeProcess.STORAGE_RULE,
            StorageCollectionType.RULES, STP_IMPORT_RULES, STORAGE_RULES_WORKSPACE);
    }

    private void storeJson(GUID eipMaster)
        throws ReferentialException, InvalidParseOperationException, InvalidCreateOperationException,
        LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {
        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        final RequestResponseOK<FileRules> documents = findDocuments(parser.getRequest().getFinalSelect());
        String json = JsonHandler.toJsonNode(documents.getResults()).toString();
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();
        final Digest digest = new Digest(digestType);
        digest.update(json.getBytes(StandardCharsets.UTF_8));
        store(eipMaster, stream, JSON, digest.toString());
    }

    /**
     * Init COMMIT_RULES LogbookOperation step
     *
     * @param operationFileRules
     * @param statusCode
     */
    private void initCommitFileRulesLogbookOperation(String operationFileRules, StatusCode statusCode, GUID eipMaster,
        LogbookOperationsClient client) {
        final GUID eipTask = GUIDFactory.newOperationLogbookGUID(getTenant());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParametersFactory
                .newLogbookOperationParameters(eipTask, operationFileRules, eipMaster,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(operationFileRules, statusCode), eipTask);
        updateLogBookEntry(logbookOperationParameters, client);
    }

    /**
     * Update COMMIT_RULES logbookOperation step
     *
     * @param operationFileRules
     * @param statusCode
     * @param evIdentifierProcess
     * @param fileRulesModelToUpdate
     * @param fileRulesModelToDelete
     * @param fileRulesModelToInsert
     */
    private void updateCommitFileRulesLogbookOperationOkOrKo(String operationFileRules, StatusCode statusCode,
        GUID evIdentifierProcess, List<FileRulesModel> fileRulesModelToUpdate,
        List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToInsert, LogbookOperationsClient client) {
        final ObjectNode evDetData = JsonHandler.createObjectNode();
        evDetData.put(NB_DELETED, fileRulesModelToDelete.size());
        evDetData.put(NB_UPDATED, fileRulesModelToUpdate.size());
        evDetData.put(NB_INSERTED, fileRulesModelToInsert.size());
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenant());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParametersFactory
                .newLogbookOperationParameters(evid, operationFileRules, evIdentifierProcess,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(COMMIT_RULES, statusCode), evid);
        logbookOperationParameters.putParameterValue(LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData));
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            operationFileRules +
                "." + statusCode);
        updateLogBookEntry(logbookOperationParameters, client);
    }

    private void updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(String subEvenType,
        GUID evIdentifierProcess, LogbookOperationsClient client) {
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenant());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParametersFactory.newLogbookOperationParameters(
                evid, CHECK_RULES, evIdentifierProcess,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(CHECK_RULES, subEvenType, StatusCode.KO), evid);
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail,
            VitamLogbookMessages.getOutcomeDetail(CHECK_RULES, subEvenType, StatusCode.KO));
        updateLogBookEntry(logbookOperationParameters, client);
    }


    /**
     * Update CHECK_RULES LogbookOperation step
     *
     * @param operationFileRules
     * @param statusCode
     * @param fileRulesIdsLinkedToUnit
     * @param evIdentifierProcess
     */
    private void updateCheckFileRulesLogbookOperationOk(String operationFileRules, StatusCode statusCode,
        Set<String> fileRulesIdsLinkedToUnit, GUID evIdentifierProcess, LogbookOperationsClient client) {
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenant());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParametersFactory
                .newLogbookOperationParameters(evid, operationFileRules, evIdentifierProcess,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(CHECK_RULES, statusCode), evid);
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
        updateLogBookEntry(logbookOperationParameters, client);
    }

    /**
     * Update Check_Rules LogbookOperation for Ko
     *
     * @param operationFileRules
     * @param statusCode
     * @param fileRulesIdsLinkedToUnit
     * @param evIdentifierProcess
     */
    private void updateCheckFileRulesLogbookOperationForDelete(String operationFileRules, StatusCode statusCode,
        Set<String> fileRulesIdsLinkedToUnit, GUID evIdentifierProcess, LogbookOperationsClient client) {
        final ObjectNode usedDeleteRuleIds = JsonHandler.createObjectNode();
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        for (String fileRulesId : fileRulesIdsLinkedToUnit) {
            arrayNode.add(fileRulesId);
        }
        usedDeleteRuleIds.set(USED_DELETED_RULE_IDS, arrayNode);
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenant());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParametersFactory
                .newLogbookOperationParameters(evid, operationFileRules, evIdentifierProcess,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(CHECK_RULES, statusCode), evid);
        logbookOperationParameters.putParameterValue(LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(usedDeleteRuleIds));
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, operationFileRules +
            "." + statusCode);
        updateLogBookEntry(logbookOperationParameters, client);
    }

    /**
     * Update Check_Rules LogbookOperation when Au is linked to unit
     *
     * @param fileRulesIdsLinkedToUnit
     * @param deleteRulesIds
     * @param evIdentifierProcess
     */
    private void updateCheckFileRulesLogbookOperationForUpdate(
        Set<String> fileRulesIdsLinkedToUnit, Set<String> deleteRulesIds, GUID evIdentifierProcess,
        LogbookOperationsClient client) {
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
        final GUID evid = GUIDFactory.newOperationLogbookGUID(getTenant());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParametersFactory
                .newLogbookOperationParameters(evid, CHECK_RULES, evIdentifierProcess,
                    LogbookTypeProcess.MASTERDATA,
                    StatusCode.WARNING,
                    VitamLogbookMessages.getCodeOp(CHECK_RULES, StatusCode.WARNING), evid);
        logbookOperationParameters.putParameterValue(LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData));
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, CHECK_RULES +
            "." + StatusCode.WARNING);
        updateLogBookEntry(logbookOperationParameters, client);
    }

    /**
     * Create a LogBook Entry related to object's update
     *
     * @param logbookParametersEnd
     */
    private void updateLogBookEntry(LogbookOperationParameters logbookParametersEnd, LogbookOperationsClient client) {
        try {
            client.update(logbookParametersEnd);
        } catch (LogbookClientBadRequestException | LogbookClientNotFoundException | LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Create a LogBook Entry related to object's creation
     *
     * @param logbookParametersStart
     */
    private void createLogBookEntry(LogbookOperationParameters logbookParametersStart, LogbookOperationsClient client) {
        try {
            client.create(logbookParametersStart);
        } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
            LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public ArrayNode checkFile(InputStream rulesFileStream, Map<Integer, List<ErrorReport>> errorsMap,
        List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules, Set<String> notUsedDeletedRules,
        Set<String> notUsedUpdatedRules)
        throws IOException, ReferentialException, InvalidParseOperationException {
        ParametersChecker.checkParameter(RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER, rulesFileStream);
        File csvFileReader = convertInputStreamToFile(rulesFileStream, TXT);
        try (FileReader reader = new FileReader(csvFileReader)) {
            @SuppressWarnings("resource")
            final CSVParser parser =
                new CSVParser(reader, CSVFormat.DEFAULT.withHeader().withTrim());
            final HashSet<String> ruleIdSet = new HashSet<>();
            int lineNumber = 1;
            try {
                for (final CSVRecord record : parser) {
                    List<ErrorReport> errors = new ArrayList<>();
                    lineNumber++;
                    if (checkRecords(record)) {
                        final String ruleId = record.get(RULE_ID);
                        final String ruleType = record.get(RULE_TYPE);
                        final String ruleValue = record.get(RULE_VALUE);
                        final String ruleDuration = record.get(RULE_DURATION);
                        final String ruleMeasurementValue = record.get(RULE_MEASUREMENT);
                        final FileRulesModel fileRulesModel =
                            new FileRulesModel(ruleId, ruleType, ruleValue, null,
                                ruleDuration, ruleMeasurementValue);
                        checkParametersNotEmpty(ruleId, ruleType, ruleValue, ruleDuration, ruleMeasurementValue,
                            errors, lineNumber);
                        checkRuleDuration(fileRulesModel, errors, lineNumber);
                        if (ruleIdSet.contains(ruleId)) {
                            errors
                                .add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_RULEID_DUPLICATION,
                                    lineNumber, fileRulesModel));
                        }
                        ruleIdSet.add(ruleId);
                        if (!containsRuleMeasurement(ruleMeasurementValue)) {
                            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULEMEASUREMENT,
                                lineNumber,
                                fileRulesModel));
                        }
                        if (!containsRuleType(ruleType)) {
                            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULETYPE_UNKNOW,
                                lineNumber, fileRulesModel));
                        }
                        checkAssociationRuleDurationRuleMeasurementLimit(record, errors, lineNumber, fileRulesModel);
                        if (errors.size() > 0) {
                            errorsMap.put(lineNumber, errors);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                String message = e.getMessage();

                if (message.contains(RULE_ID + " not found")) {
                    message = ReportConstants.FILE_INVALID + RULE_ID;
                }
                if (message.contains(RULE_TYPE + " not found")) {
                    message = ReportConstants.FILE_INVALID + RULE_TYPE;
                }
                if (message.contains(RULE_VALUE + " not found")) {
                    message = ReportConstants.FILE_INVALID + RULE_VALUE;
                }
                if (message.contains(RULE_DURATION + " not found")) {
                    message = ReportConstants.FILE_INVALID + RULE_DURATION;
                }
                if (message.contains(RULE_DESCRIPTION + " not found")) {
                    message = ReportConstants.FILE_INVALID + RULE_DESCRIPTION;
                }
                if (message.contains(RULE_MEASUREMENT + " not found")) {
                    message = ReportConstants.FILE_INVALID + RULE_MEASUREMENT;
                }
                throw new ReferentialException(message);
            } catch (Exception e) {
                throw new ReferentialException(e);
            }
        }
        if (csvFileReader != null) {
            final ArrayNode readRulesAsJson = RulesManagerParser.readObjectsFromCsvWriteAsArrayNode(csvFileReader);
            checkRulesLinkedToAu(readRulesAsJson, usedDeletedRules, usedUpdatedRules, notUsedDeletedRules,
                notUsedUpdatedRules);
            if (errorsMap.size() > 0) {
                throw new FileRulesCsvException(INVALID_CSV_FILE);
            }
            if (usedDeletedRules.size() > 0) {
                throw new FileRulesDeleteException("used Rules want to be deleted");
            }
            if (usedUpdatedRules.size() > 0) {
                throw new FileRulesUpdateException("used Rules want to be updated");
            }
            csvFileReader.delete();
            return readRulesAsJson;
        }
        /* this line is reached only if temporary file is null */
        throw new FileRulesException(INVALID_CSV_FILE);
    }


    /**
     * Save the error report in storage
     *
     * @param errors the given of errors to consume for generate error report
     * @param eipMaster GUID of the process
     * @param usedDeletedRules list of fileRules that attempt to be deleted but have reference to unit
     * @param usedUpdatedRules list of fileRules that attempt to be updated but have reference to unit
     * @throws StorageException
     * @throws LogbookClientServerException
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientAlreadyExistsException
     * @throws FileRulesException
     */
    private void generateReport(Map<Integer, List<ErrorReport>> errors, GUID eipMaster,
        List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules, LogbookOperationsClient client)
        throws StorageException, LogbookClientServerException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException, FileRulesException {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient()) {
            final GUID eip1 = GUIDFactory.newOperationLogbookGUID(tenantId);
            final String fileName = eipMaster + ".json";
            final String uri = String.format("%s/%s", STORAGE_RULES_WORKSPACE, fileName);
            InputStream stream = null;
            if (!errors.isEmpty() || !usedDeletedRules.isEmpty()) {
                if (eipMaster != null) {
                    stream =
                        generateErrorReport(errors, usedDeletedRules, usedUpdatedRules, StatusCode.KO, eipMaster);
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
                stream = generateReportOK(errors, usedDeletedRules, usedUpdatedRules, eipMaster);
            }
            try {
                workspaceClient.createContainer(fileName);
                workspaceClient.putObject(fileName, uri, stream);
                final ObjectDescription description = new ObjectDescription();
                description.setWorkspaceContainerGUID(fileName);
                description.setWorkspaceObjectURI(uri);

                try {
                    storageClient.storeFileFromWorkspace(
                        DEFAULT_STRATEGY, StorageCollectionType.REPORTS, fileName, description);
                    workspaceClient.deleteContainer(fileName, true);
                } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                    StorageServerClientException | ContentAddressableStorageNotFoundException e) {
                    final LogbookOperationParameters logbookParametersEnd =
                        LogbookParametersFactory
                            .newLogbookOperationParameters(eip1, RULES_REPORT,
                                eipMaster, LogbookTypeProcess.STORAGE_RULE,
                                StatusCode.KO, VitamLogbookMessages.getCodeOp(
                                    RULES_REPORT,
                                    StatusCode.KO),
                                eip1);
                    updateLogBookEntry(logbookParametersEnd, client);

                    LOGGER.error("unable to store file", e);
                    throw new StorageException(e);
                }
                final LogbookOperationParameters logbookParametersEnd =
                    LogbookParametersFactory
                        .newLogbookOperationParameters(eip1, RULES_REPORT,
                            eipMaster, LogbookTypeProcess.STORAGE_RULE,
                            StatusCode.OK, VitamLogbookMessages.getCodeOp(
                                RULES_REPORT, StatusCode.OK),
                            eip1);
                final ObjectNode evDetData = JsonHandler.createObjectNode();
                evDetData.put(FILE_NAME, fileName);
                logbookParametersEnd.putParameterValue(LogbookParameterName.eventDetailData,
                    JsonHandler.unprettyPrint(evDetData));
                updateLogBookEntry(logbookParametersEnd, client);
            } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException e) {
                LOGGER.error("unable to create container or store file in workspace", e);
                final LogbookOperationParameters logbookParametersEnd =
                    LogbookParametersFactory
                        .newLogbookOperationParameters(eip1, RULES_REPORT,
                            eipMaster, LogbookTypeProcess.STORAGE_RULE,
                            StatusCode.KO, VitamLogbookMessages.getCodeOp(
                                RULES_REPORT, StatusCode.KO),
                            eip1);
                updateLogBookEntry(logbookParametersEnd, client);
                throw new StorageException(e);
            } finally {
                StreamUtils.closeSilently(stream);
            }
        }
    }



    /**
     * Check Referential To Import for create ruleFiles to delete, update, insert
     *
     * @param fileRulesModelsToImport the given list with all fileRules to import
     * @param fileRulesModelsInDb the given list with all fileRulesInDb
     * @param fileRulesModelToDelete the given list with fileRules to delete
     * @param fileRulesModelToUpdate the given list with fileRules to update
     * @param fileRulesModelToInsert the given list with fileRules to insert
     */
    private void createListToimportUpdateDelete(List<FileRulesModel> fileRulesModelsToImport,
        List<FileRulesModel> fileRulesModelsInDb, List<FileRulesModel> fileRulesModelToDelete,
        List<FileRulesModel> fileRulesModelToUpdate, List<FileRulesModel> fileRulesModelToInsert) {
        for (FileRulesModel fileRulesModel : fileRulesModelsToImport) {
            for (FileRulesModel fileRulesModelInDb : fileRulesModelsInDb) {
                if (fileRulesModelInDb.equals(fileRulesModel) &&
                    (!fileRulesModelInDb.getRuleDuration().equals(fileRulesModel.getRuleDuration()) ||
                        !fileRulesModelInDb.getRuleMeasurement().equals(fileRulesModel.getRuleMeasurement()) ||
                        !fileRulesModelInDb.getRuleDescription().equals(fileRulesModel.getRuleDescription()) ||
                        !fileRulesModelInDb.getRuleValue().equals(fileRulesModel.getRuleValue()) ||
                        !fileRulesModelInDb.getRuleType().equals(fileRulesModel.getRuleType()))) {
                    fileRulesModelToUpdate.add(fileRulesModel);
                }
            }
        }
        fileRulesModelToInsert.addAll(fileRulesModelsToImport);
        fileRulesModelToDelete.addAll(fileRulesModelsInDb);
        fileRulesModelToDelete.removeAll(fileRulesModelsToImport);

        fileRulesModelToInsert.removeAll(fileRulesModelsInDb);
    }

    /**
     * Transform List of FileRules To List of FileRulesModel
     *
     * @param fileRules fileRules in db
     * @return List of FilesRulesModel
     */
    private List<FileRulesModel> transformFileRulesToFileRulesModel(List<FileRules> fileRules) {
        List<FileRulesModel> filesRulesModels = new ArrayList<FileRulesModel>();
        if (fileRules != null && !fileRules.isEmpty()) {
            for (FileRules rule : fileRules) {
                filesRulesModels.add(new FileRulesModel(rule.getRuleid(), rule.getRuletype(), rule.getRulevalue(), rule
                    .getRuledescription(), rule.getRuleduration(), rule.getRulemeasurement()));

            }
        }
        return filesRulesModels;
    }

    /**
     * Transform JsonNode to To filesRulesModel
     *
     * @param fileRulesNode JsonNode to transform
     * @return List of FilesRulesModel
     */
    private List<FileRulesModel> transformJsonNodeToFileRulesModel(JsonNode fileRulesNode) {
        final JsonNode rulesJsonNode = fileRulesNode;
        List<FileRulesModel> filesRulesModels = new ArrayList<FileRulesModel>();
        try {
            if (rulesJsonNode != null && rulesJsonNode.isArray()) {
                final ArrayNode arrayNode = (ArrayNode) rulesJsonNode;
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



    /**
     * Check existence of file rules linked to unit in database
     *
     * @param fileRulesModelToCheck fileRulesModelToCheck
     * @param rulesLinkedToUnit rulesLinkedToUnit
     * @param rulesNotLinkedToUnit rulesNotLinkedToUnit
     * @return true if a given FileRules is linked to a unit, false if none of them are linked to a unit
     * @throws InvalidParseOperationException
     */
    public boolean checkUnitLinkedToFileRules(List<FileRulesModel> fileRulesModelToCheck,
        Set<String> rulesLinkedToUnit, Set<String> rulesNotLinkedToUnit)
        throws InvalidParseOperationException {
        boolean linked = false;
        try {
            for (FileRulesModel fileRulesModel : fileRulesModelToCheck) {
                ArrayNode arrayNodeResult =
                    checkUnitLinkedtofileRulesInDatabase(fileRulesLinkedToUnitQueryBuilder(fileRulesModel),
                        fileRulesModel.getRuleId());
                if (arrayNodeResult != null && arrayNodeResult.size() > 0) {
                    linked = true;
                    rulesLinkedToUnit.add(fileRulesModel.getRuleId());
                } else {
                    rulesNotLinkedToUnit.add(fileRulesModel.getRuleId());
                }
            }
        } catch (FileFormatNotFoundException e) {
            LOGGER.error(e);
        } catch (ReferentialException e) {
            LOGGER.error(e);
        }
        return linked;

    }


    /**
     * Create QueryDsl for update the given FileRules
     *
     * @param fileRulesModel FileRulesModel to update
     * @param sequence
     * @throws InvalidCreateOperationException
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     */
    private void updateFileRules(FileRulesModel fileRulesModel, Integer sequence)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException {
        // FIXME use bulk create instead like LogbookMongoDbAccessImpl.
        final UpdateParserSingle updateParser = new UpdateParserSingle(new VarNameAdapter());
        final Update updateFileRules = new Update();
        List<SetAction> actions = new ArrayList<SetAction>();
        SetAction setRuleValue;
        setRuleValue = new SetAction(RULE_VALUE, fileRulesModel.getRuleValue());
        actions.add(setRuleValue);
        SetAction setRuleDescription = new SetAction(RULE_DESCRIPTION, fileRulesModel.getRuleDescription());
        actions.add(setRuleDescription);
        SetAction setUpdateDate =
            new SetAction(UPDATE_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        actions.add(setUpdateDate);
        SetAction setRuleMeasurement = new SetAction(RULE_MEASUREMENT, fileRulesModel.getRuleMeasurement());
        actions.add(setRuleMeasurement);
        SetAction setRuleDuration = new SetAction(RULE_DURATION, fileRulesModel.getRuleDuration());
        actions.add(setRuleDuration);
        SetAction setRuleType = new SetAction(RULE_TYPE, fileRulesModel.getRuleType());
        actions.add(setRuleType);
        updateFileRules.setQuery(eq(RULE_ID, fileRulesModel.getRuleId()));
        updateFileRules.addActions(actions.toArray(new SetAction[actions.size()]));
        updateParser.parse(updateFileRules.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        mongoAccess.updateData(queryDslForUpdate, FunctionalAdminCollections.RULES, sequence);
    }

    /**
     * Delete fileRules by id
     *
     * @param fileRulesModel fileRulesModel to delete
     * @param collection the given FunctionalAdminCollections
     */
    public void deleteFileRules(FileRulesModel fileRulesModel, FunctionalAdminCollections collection) {
        final Delete delete = new Delete();
        DbRequestResult result = null;
        DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection());
        try {
            delete.setQuery(eq(RULE_ID, fileRulesModel.getRuleId()));
            result = dbrequest.execute(delete);
            result.close();
        } catch (InvalidParseOperationException | BadRequestException | InvalidCreateOperationException | DatabaseException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Construct query DSL for find all FileRules (referential)
     *
     * @return list of FileRules in database
     */
    private List<FileRules> findAllFileRulesQueryBuilder() {
        final Select select = new Select();
        List<FileRules> fileRules = new ArrayList<FileRules>();
        try {
            RequestResponseOK<FileRules> response = findDocuments(select.getFinalSelect());
            if (response != null) {
                return response.getResults();
            }
        } catch (ReferentialException e) {
            LOGGER.error("ReferentialException", e);
        }
        return fileRules;
    }


    /**
     * Construct query dsl Query for find unit attached to fileRules
     *
     * @return query dsl Query for find unit attached to fileRules
     */
    private JsonNode fileRulesLinkedToUnitQueryBuilder(FileRulesModel fileRulesModels) {
        final SelectMultiQuery selectMultiple = new SelectMultiQuery();
        StringBuffer sb = new StringBuffer();
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

    /**
     * checkifTheCollectionIsEmptyBeforeImport : Check if the Collection is empty .
     *
     * @return true if the given collection is empty for the given tenant false if it's not
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     * @throws ReferentialException
     */
    private boolean isCollectionEmptyForTenant() throws ReferentialException {
        return FunctionalAdminCollections.RULES.getCollection().count(eq(VitamDocument.TENANT_ID, getTenant())) == 0;
    }

    private Integer getTenant() {
        return ParameterHelper.getTenantParameter();
    }

    /**
     * findExistsRuleQueryBuilder:Check if the Collection contains records
     *
     * @return the JsonNode answer
     * @throws InvalidCreateOperationException if exception occurred when create query
     * @throws InvalidParseOperationException if parse json query exception occurred
     */
    public JsonNode findExistsRuleQueryBuilder()
        throws InvalidCreateOperationException, InvalidParseOperationException {
        JsonNode result;
        final Select select = new Select();
        select.addOrderByDescFilter(RULE_ID);
        final BooleanQuery query = (BooleanQuery) and();
        query.add(exists(RULE_ID));
        select.setQuery(query);
        result = select.getFinalSelect();
        return result;
    }

    /**
     * Check if the rule duration is integer
     *
     * @param errors list of errors to set
     * @param line the given line to treat
     * @throws FileRulesException
     */
    private void checkRuleDuration(FileRulesModel fileRulesModel, List<ErrorReport> errors, int line) {
        if (fileRulesModel.getRuleDuration().equalsIgnoreCase(UNLIMITED)) {
            return;
        } else {
            final int duration = parseWithDefault(fileRulesModel.getRuleDuration());
            if (duration < 0) {
                errors.add(
                    new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_RULEDURATION, line,
                        fileRulesModel));
            }
        }
    }

    private int parseWithDefault(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException err) {
            return -777;
        }
    }

    /**
     * check if Records are not Empty
     *
     * @param ruleId ruleId
     * @param ruleType ruleType
     * @param ruleValue ruleValue
     * @param ruleDuration ruleDuration
     * @param ruleMeasurementValue ruleMeasurementValue
     * @param errors list of errors to set
     * @param line the given line to treat
     * @throws FileRulesException thrown if one ore more parameters are missing
     */
    private void checkParametersNotEmpty(String ruleId, String ruleType, String ruleValue, String ruleDuration,
        String ruleMeasurementValue, List<ErrorReport> errors, int line) {
        List<String> missingParam = new ArrayList<>();
        if (ruleId == null || ruleId.isEmpty()) {
            missingParam.add(RULE_ID);
        }
        if (ruleType == null || ruleType.isEmpty()) {
            missingParam.add(RULE_TYPE);
        }
        if (ruleValue == null || ruleValue.isEmpty()) {
            missingParam.add(RULE_VALUE);
        }
        if (ruleDuration == null || ruleDuration.isEmpty()) {
            missingParam.add(RULE_DURATION);
        }
        if (ruleMeasurementValue == null || ruleMeasurementValue.isEmpty()) {
            missingParam.add(RULE_MEASUREMENT);
        }
        if (missingParam.size() > 0) {
            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_MISSING_INFORMATION, line,
                missingParam.stream().collect(Collectors.joining())));
        }
    }

    /**
     * Check if Records is not null
     *
     * @param record the given record to import
     * @return true if no parameters of the csv is empty false if one or more parameters is empty
     */
    private boolean checkRecords(CSVRecord record) {
        return record.get(RULE_ID) != null && record.get(RULE_TYPE) != null && record.get(RULE_VALUE) != null &&
            record.get(RULE_DURATION) != null && record.get(RULE_DESCRIPTION) != null &&
            record.get(RULE_MEASUREMENT) != null;
    }


    /**
     * Check if Rule duration associated to rule measurement respect the limit of 999 years
     *
     * @param record the list of record to check
     * @param errors the list of errors
     * @param line the current line
     * @param fileRuleModel the current object that contains all the record to check
     * @throws FileRulesException
     */
    private void checkAssociationRuleDurationRuleMeasurementLimit(CSVRecord record, List<ErrorReport> errors, int line,
        FileRulesModel fileRuleModel)
        throws FileRulesException {
        try {
            if (!record.get(RULE_DURATION).equalsIgnoreCase(UNLIMITED) &&
                (record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.YEAR.getType()) &&
                    Integer.parseInt(record.get(RULE_DURATION)) > YEAR_LIMIT ||
                    record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.MONTH.getType()) &&
                        Integer.parseInt(record.get(RULE_DURATION)) > MONTH_LIMIT ||
                    record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.DAY.getType()) &&
                        Integer.parseInt(record.get(RULE_DURATION)) > DAY_LIMIT)) {
                errors
                    .add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_TOTALDURATION, line, fileRuleModel));
            }
        } catch (NumberFormatException e) {
            errors.add(new ErrorReport(FileRulesErrorCode.STP_IMPORT_RULES_WRONG_TOTALDURATION, line, fileRuleModel));
        }

    }


    /**
     * Check if RuleMeasurement is included in the Enumeration
     *
     * @param ruleMeasurement ruleMeasurement to test
     * @return true if ruleMeasurement is in the authorise RuleMeasurementEnum false if it's not
     */
    private static boolean containsRuleMeasurement(String ruleMeasurement) {
        for (final RuleMeasurementEnum c : RuleMeasurementEnum.values()) {
            if (c.getType().equalsIgnoreCase(ruleMeasurement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if RuleType is included in the Enumeration
     *
     * @param ruleType ruleType
     * @return true if ruleType is in the authorise RuleTypeEnum false if it's not
     */
    private static boolean containsRuleType(String ruleType) {
        for (final RuleTypeEnum c : RuleTypeEnum.values()) {
            if (c.getType().equalsIgnoreCase(ruleType)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Convert a given input stream to a file
     *
     * @param rulesStream
     * @param extension
     * @return
     * @throws IOException
     */
    private File convertInputStreamToFile(InputStream rulesStream, String extension) throws IOException {
        try {
            final File csvFile = File.createTempFile(TMP, extension, new File(VitamConfiguration.getVitamTmpFolder()));
            Files.copy(rulesStream, csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return csvFile;
        } finally {
            StreamUtils.closeSilently(rulesStream);
        }
    }

    @Override
    public FileRules findDocumentById(String id) throws ReferentialException {
        FileRules fileRule =
            (FileRules) mongoAccess.getDocumentByUniqueId(id, FunctionalAdminCollections.RULES, FileRules.RULEID);
        if (fileRule == null) {
            throw new FileRulesException("FileRules Not Found");
        }
        return fileRule;
    }

    @Override
    public RequestResponseOK<FileRules> findDocuments(JsonNode select) throws ReferentialException {
        try (DbRequestResult result =
            mongoAccess.findDocuments(select, FunctionalAdminCollections.RULES)) {
            final RequestResponseOK<FileRules> list = result.getRequestResponseOK(select, FileRules.class);
            return list;
        } catch (final FileRulesException e) {
            LOGGER.error(e.getMessage());
            throw new ReferentialException(e);
        }
    }


    /**
     * Check if an Import operation is in progress
     *
     * @param client the LogbookOperations Client
     * @return true if an import operation is launche / false if not an import operation is in progress
     * @throws LogbookClientException when error
     */
    private boolean isImportOperationInProgress(LogbookOperationsClient client) throws LogbookClientException {
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
            JsonNode logbookResult = client.selectOperation(select.getFinalSelect());
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
        } catch (LogbookClientNotFoundException e) {
            // TODO: ugly catch because if there is no result on logbook with dsl query, the logbook throws a
            // NotFoundException. If I fix this, everything may be broken (check LogbookOperationImpl.select method)
            // Hope for the best here.
            LOGGER.warn(e);
            return false;
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            // May not happen
            LOGGER.error(e);
            throw new LogbookClientServerException(e);
        }
    }

    /**
     * find document based on DSL query with DbRequest multiple
     *
     * @param select query
     * @param ruleFilesId Identifier
     * @return vitam document list
     * @throws FileFormatNotFoundException when no results found
     * @throws ReferentialException when error occurs
     */
    private ArrayNode checkUnitLinkedtofileRulesInDatabase(JsonNode select, String ruleFilesId)
        throws FileFormatNotFoundException, ReferentialException {
        ArrayNode resultUnitsArray = null;
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            LOGGER.debug("Selected Query For linked unit: " + select.toString());
            final JsonNode unitsResultNode = metaDataClient.selectUnits(select);
            resultUnitsArray = (ArrayNode) unitsResultNode.get(RESULTS);

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
     * @param status
     * @param eipMaster
     * @return the error report inputStream
     * @throws FileRulesException
     */
    public InputStream generateErrorReport(Map<Integer, List<ErrorReport>> errors,
        List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules,
        StatusCode status, GUID eipMaster)
        throws FileRulesException {
        final ObjectNode reportFinal = JsonHandler.createObjectNode();
        final ObjectNode guidmasterNode = JsonHandler.createObjectNode();
        final ObjectNode lineNode = JsonHandler.createObjectNode();
        final ArrayNode usedDeletedArrayNode = JsonHandler.createArrayNode();
        final ArrayNode usedUpdatedArrayNode = JsonHandler.createArrayNode();
        guidmasterNode.put(ReportConstants.EV_TYPE, STP_IMPORT_RULES);
        guidmasterNode.put(ReportConstants.EV_DATE_TIME, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        if (eipMaster != null) {
            guidmasterNode.put(ReportConstants.EV_ID, eipMaster.toString());
        }
        guidmasterNode.put(ReportConstants.OUT_MESSG, VitamErrorMessages.getFromKey(STP_IMPORT_RULES + "." + status));

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
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION,
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
                    case STP_IMPORT_RULES_NOT_CSV_FORMAT:
                    case STP_IMPORT_RULES_DELETE_USED_RULES:
                    case STP_IMPORT_RULES_UPDATED_RULES:
                    default:
                        break;
                }
                messagesArrayNode.add(errorNode);
            }
            lineNode.set(String.format("line %s", line), messagesArrayNode);
        }
        for (FileRulesModel fileRulesModel : usedDeletedRules) {
            usedDeletedArrayNode.add(fileRulesModel.toString());
        }
        for (FileRulesModel fileRulesModel : usedUpdatedRules) {
            usedUpdatedArrayNode.add(fileRulesModel.toString());
        }
        reportFinal.set(ReportConstants.JDO_DISPLAY, guidmasterNode);
        if (!errors.isEmpty()) {
            reportFinal.set(ReportConstants.ERROR, lineNode);
        }
        reportFinal.set(USED_DELETED_RULES, usedDeletedArrayNode);
        reportFinal.set(USED_UPDATED_RULES, usedUpdatedArrayNode);
        String json = JsonHandler.unprettyPrint(reportFinal);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

    }


    /**
     * generate Error Report
     *
     * @param errors the list of error for generated errors
     * @param usedDeletedRules list of fileRules that attempt to be deleted but have reference to unit
     * @param usedUpdatedRules list of fileRules that attempt to be updated but have reference to unit
     * @return the error report inputStream
     * @throws FileRulesException
     */
    private InputStream generateReportOK(Map<Integer, List<ErrorReport>> errors,
        List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules, GUID eip)
        throws FileRulesException {
        final ObjectNode reportFinal = JsonHandler.createObjectNode();
        final ObjectNode guidmasterNode = JsonHandler.createObjectNode();
        final ArrayNode usedDeletedArrayNode = JsonHandler.createArrayNode();
        final ArrayNode usedUpdatedArrayNode = JsonHandler.createArrayNode();
        guidmasterNode.put(ReportConstants.EV_TYPE, STP_IMPORT_RULES);
        guidmasterNode.put(ReportConstants.EV_DATE_TIME, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        guidmasterNode.put(ReportConstants.EV_ID, eip.toString());
        guidmasterNode.put(ReportConstants.OUT_MESSG,
            STP_IMPORT_RULES_SUCCESS);
        for (FileRulesModel fileRulesModel : usedDeletedRules) {
            usedDeletedArrayNode.add(fileRulesModel.toString());
        }
        for (FileRulesModel fileRulesModel : usedUpdatedRules) {
            usedUpdatedArrayNode.add(fileRulesModel.toString());
        }
        reportFinal.set(ReportConstants.JDO_DISPLAY, guidmasterNode);
        reportFinal.set(USED_DELETED_RULES, usedDeletedArrayNode);
        reportFinal.set(USED_UPDATED_RULES, usedUpdatedArrayNode);
        String json = JsonHandler.unprettyPrint(reportFinal);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

    }

    private void copyFilesOnWorkspaceUpdateWorkflow(InputStream stream, String containerName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {
        try (
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();) {
            workspaceClient.createContainer(containerName);
            workspaceClient.putObject(containerName,
                UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON,
                stream);
        }

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

}
