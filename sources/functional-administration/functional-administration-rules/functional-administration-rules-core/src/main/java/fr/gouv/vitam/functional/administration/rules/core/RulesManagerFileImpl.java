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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;

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
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.client.model.FileRulesModel;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.RuleTypeEnum;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.counter.SequenceType;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
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
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * RulesManagerFileImpl
 *
 * Manage the Rules File features
 */

public class RulesManagerFileImpl implements ReferentialFile<FileRules>, VitamAutoCloseable {

    private static final String RULES_FILE_STREAMIS_A_MANDATORY_PARAMETER = "rulesFileStreamis a mandatory parameter";
    private static final String RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER = "rulesFileStream is a mandatory parameter";
    private static final String RULES_PROCESS_IMPORT_ALREADY_EXIST =
        "There is already on file rules import in progress";
    private static final String DELETE_RULES_LINKED_TO_UNIT =
        "Error During Delete RuleFiles because this rule is linked to unit. RulesId: %s";
    private static final String UPDATE_RULES_LINKED_TO_UNIT =
        "Warn During update RuleFiles because this rule is linked to unit. RulesId: %s";
    private static final String INVALID_CSV_FILE = "Invalid CSV File :";
    private static final String FILE_RULE_WITH_RULE_ID = "Rule with Id %s already exists";
    private static final String TXT = ".txt";
    private static final String CSV = "csv";
    private static final String JSON = "json";

    private static final String TMP = "tmp";
    private static final String RULE_MEASUREMENT = "RuleMeasurement";
    private static final String RULE_DURATION = "RuleDuration";
    private static final String RULE_DESCRIPTION = "RuleDescription";
    private static final String RULE_VALUE = "RuleValue";
    private static final String RULE_TYPE = "RuleType";
    private static final String UPDATE_DATE = "UpdateDate";
    private static final String UNLIMITED = "unlimited";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RulesManagerFileImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private static final String RULE_ID = "RuleId";
    private final VitamCounterService vitamCounterService;
    private LogbookOperationsClient client;

    private static String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private static String UPDATE_RULES_ARCHIVE_UNITS = "UPDATE_RULES_ARCHIVE_UNITS";
    private static String CHECK_RULES = "CHECK_RULES";
    private static String COMMIT_RULES = "COMMIT_RULES";
    private static String USED_DELETED_RULE_IDS = "usedDeletedRuleIds";
    private static String DELETED_RULE_IDS = "deletedRuleIds";
    private static String USED_UPDATED_RULE_IDS = "usedUpdatedRuleIds";
    private static String NB_DELETED = "nbDeleted";
    private static String NB_UPDATED = "nbUpdated";
    private static String NB_INSERTED = "nbInserted";
    private static String INVALIDPARAMETERS = "Invalid Parameter Value %s : %s";
    private static String NOT_SUPPORTED_VALUE = "The value %s of parameter %s is not supported";
    private static String MANDATORYRULEPARAMETERISMISSING = "The following mandatory parameters are missing %s";
    private static int YEAR_LIMIT = 999;
    private static int MONTH_LIMIT = YEAR_LIMIT * 12;
    private static int DAY_LIMIT = MONTH_LIMIT * 30;
    private final RulesSecurisator securisator;

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access admin configuration
     * @param securisator
     */
    public RulesManagerFileImpl(MongoDbAccessAdminImpl dbConfiguration, VitamCounterService vitamCounterService,
        RulesSecurisator securisator) {
        mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        this.securisator = securisator;
    }

    @Override
    public void importFile(InputStream rulesFileStream)
        throws IOException, InvalidParseOperationException, ReferentialException, InvalidCreateOperationException {
        ParametersChecker.checkParameter(RULES_FILE_STREAMIS_A_MANDATORY_PARAMETER, rulesFileStream);
        File file = convertInputStreamToFile(rulesFileStream, CSV);
        try (LogbookOperationsClient client2 = LogbookOperationsClientFactory.getInstance().getClient()) {
            client = client2;
            if (!isImportOperationInProgress()) {
                /* To process import validate the file first */
                final ArrayNode validatedRules = checkFile(new FileInputStream(file));
                if (validatedRules != null) {
                    final GUID eip = GUIDFactory.newOperationLogbookGUID(getTenant());

                    List<FileRules> fileRulesInDb = findAllFileRulesQueryBuilder();
                    List<FileRulesModel> fileRulesModelsInDb = transformFileRulesToFileRulesModel(fileRulesInDb);
                    // #1737 : List of file rules to delete, update, insert
                    List<FileRulesModel> fileRulesModelToDelete = new ArrayList<>();
                    List<FileRulesModel> fileRulesModelToInsert = new ArrayList<>();
                    List<FileRulesModel> fileRulesModelToUpdate = new ArrayList<>();
                    List<FileRulesModel> fileRulesModelsToImport = transformJsonNodeToFileRulesModel(validatedRules);
                    createListToimportUpdateDelete(fileRulesModelsToImport, fileRulesModelsInDb,
                        fileRulesModelToDelete,
                        fileRulesModelToUpdate, fileRulesModelToInsert);
                    final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
                        .newLogbookOperationParameters(eip, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                            StatusCode.STARTED,
                            VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.STARTED), eip);
                    createLogBookEntry(logbookParametersStart);

                    final GUID eip1 = GUIDFactory.newOperationLogbookGUID(getTenant());
                    try {
                        initCheckFileRulesLogbookOperation(CHECK_RULES, StatusCode.STARTED, eip);
                        Set<String> fileRulesIdLinkedToUnitForDelete = new HashSet<String>();
                        Set<String> fileRulesNotLinkedToUnitForDelete = new HashSet<String>();
                        if (fileRulesModelToDelete.size() > 0) {
                            if (checkUnitLinkedToFileRules(fileRulesModelToDelete, fileRulesIdLinkedToUnitForDelete,
                                fileRulesNotLinkedToUnitForDelete)) {
                                updateCheckFileRulesLogbookOperationForDelete(CHECK_RULES, StatusCode.KO,
                                    fileRulesIdLinkedToUnitForDelete,
                                    eip);
                                String joined = StringUtils.join(fileRulesIdLinkedToUnitForDelete);
                                LOGGER.error(String.format(DELETE_RULES_LINKED_TO_UNIT, joined));
                                throw new FileRulesException(
                                    String.format(DELETE_RULES_LINKED_TO_UNIT, joined));
                            }
                        }
                        if (fileRulesModelToUpdate.size() > 0) {
                            Set<String> fileRulesIdLinkedToUnit = new HashSet<String>();
                            Set<String> fileRulesNotLinkedToUnitForUpdate = new HashSet<String>();
                            if (checkUnitLinkedToFileRules(fileRulesModelToUpdate, fileRulesIdLinkedToUnit,
                                fileRulesNotLinkedToUnitForUpdate)) {
                                updateCheckFileRulesLogbookOperationForUpdate(fileRulesIdLinkedToUnit,
                                    fileRulesNotLinkedToUnitForDelete,
                                    eip);
                                LOGGER.warn(UPDATE_RULES_LINKED_TO_UNIT, StringUtils.join(fileRulesIdLinkedToUnit));

                            } else {
                                updateCheckFileRulesLogbookOperationOk(CHECK_RULES, StatusCode.OK,
                                    fileRulesNotLinkedToUnitForDelete,
                                    eip);
                            }
                        } else {
                            updateCheckFileRulesLogbookOperationOk(CHECK_RULES, StatusCode.OK,
                                fileRulesNotLinkedToUnitForDelete,
                                eip);
                        }
                        boolean commitOk = commitRules(fileRulesModelToUpdate, fileRulesModelToDelete, validatedRules,
                            fileRulesModelToInsert,
                            fileRulesModelsToImport, eip);

                        final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();
                        final Digest digest = new Digest(digestType);
                        digest.update(new FileInputStream(file));

                        store(eip, new FileInputStream(file), CSV, digest.toString());
                        // store json
                        if (commitOk) {
                            Integer sequence = vitamCounterService
                                .getSequence(ParameterHelper.getTenantParameter(),
                                    SequenceType.RULES_SEQUENCE.getName());
                            storeJson(eip, sequence);
                        }
                        // else no rule modified ?
                        // store Json

                        final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                            .newLogbookOperationParameters(eip1, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                                StatusCode.OK, VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.OK),
                                eip1);
                        updateLogBookEntry(logbookParametersEnd);

                        if (!fileRulesModelToUpdate.isEmpty()) {
                            // #2201 - we now launch the process that will update units
                            launchWorkflow(fileRulesModelToUpdate);

                        }
                    } catch (final FileRulesException | StorageException | LogbookClientServerException |
                        LogbookClientBadRequestException |
                        LogbookClientAlreadyExistsException e) {
                        LOGGER.error(e);
                        final LogbookOperationParameters logbookParametersEnd =
                            LogbookParametersFactory
                                .newLogbookOperationParameters(eip1, STP_IMPORT_RULES, eip,
                                    LogbookTypeProcess.MASTERDATA,
                                    StatusCode.KO, VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.KO),
                                    eip1);
                        updateLogBookEntry(logbookParametersEnd);
                        throw new FileRulesException(e);
                    }
                }
            } else {
                throw new FileRulesImportInProgressException(RULES_PROCESS_IMPORT_ALREADY_EXIST);
            }
        } catch (LogbookClientException e) {
            LOGGER.error(e);
            throw new FileRulesException(e);
        } finally {
            file.delete();
        }
    }

    /**
     * Method that is responsible of launching workflow that will update archive units after rules has been updated
     * 
     * @param fileRulesModelToUpdate
     * @throws InvalidParseOperationException
     */
    private void launchWorkflow(List<FileRulesModel> fileRulesModelToUpdate) throws InvalidParseOperationException {

        try (ProcessingManagementClient processManagementClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {
            ArrayNode arrayNode = JsonHandler.createArrayNode();
            for (final FileRulesModel ruleNode : fileRulesModelToUpdate) {
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
            createLogBookEntry(logbookUpdateParametersStart);
            try {
                securisator.copyFilesOnWorkspaceUpdateWorkflow(
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
                updateLogBookEntry(logbookUpdateParametersEnd);
            }
        }
    }

    /**
     * Commit in mongo/elastic for update, delete, insert
     *
     * @param fileRulesModelToUpdate
     * @param fileRulesModelToDelete
     * @param validatedRules
     * @param fileRulesModelToInsert
     * @param fileRulesModelsToImport
     * @return true if commited
     * @throws FileRulesException
     */
    private boolean commitRules(List<FileRulesModel> fileRulesModelToUpdate,
        List<FileRulesModel> fileRulesModelToDelete,
        ArrayNode validatedRules, List<FileRulesModel> fileRulesModelToInsert,
        List<FileRulesModel> fileRulesModelsToImport, GUID eipMaster)
        throws FileRulesException, LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {
        initCommitFileRulesLogbookOperation(COMMIT_RULES, StatusCode.STARTED, eipMaster);
        boolean secureRules = false;
        try {
            Integer sequence = vitamCounterService
                .getSequence(ParameterHelper.getTenantParameter(), SequenceType.RULES_SEQUENCE.getName());
            for (FileRulesModel fileRulesModel : fileRulesModelToUpdate) {
                updateFileRules(fileRulesModel, sequence);
            }
            if (fileRulesModelToInsert.containsAll(fileRulesModelsToImport)) {
                commit(validatedRules);
                secureRules = true;
            } else if (fileRulesModelToInsert.size() > 0) {
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
                fileRulesModelToUpdate, fileRulesModelToDelete, fileRulesModelToInsert);

            return secureRules;
        } catch (ReferentialException | InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(e);
            updateCommitFileRulesLogbookOperationOkOrKo(COMMIT_RULES, StatusCode.KO, eipMaster,
                fileRulesModelToUpdate, fileRulesModelToDelete, fileRulesModelToInsert);
            throw new FileRulesException(e);
        }
    }

    private void commit(ArrayNode validatedRules)
        throws ReferentialException, LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException, InvalidParseOperationException, InvalidCreateOperationException {
        Integer sequence = vitamCounterService
            .getNextSequence(ParameterHelper.getTenantParameter(), SequenceType.RULES_SEQUENCE.getName());
        mongoAccess.insertDocuments(validatedRules, FunctionalAdminCollections.RULES, sequence);

    }

    private void store(GUID eipMaster, InputStream stream, String extension, String digest)
        throws ReferentialException, InvalidParseOperationException, InvalidCreateOperationException,
        LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {
        Integer sequence = vitamCounterService
            .getSequence(ParameterHelper.getTenantParameter(), SequenceType.RULES_SEQUENCE.getName());
        securisator.secureFileRules(sequence, stream, extension, eipMaster, digest);
    }

    private void storeJson(GUID eipMaster, Integer sequence)
        throws ReferentialException, InvalidParseOperationException, InvalidCreateOperationException,
        LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {
        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(eq("#version", sequence.toString()));
        final RequestResponseOK<FileRules> documents = findDocuments(parser.getRequest().getFinalSelect());
        // FIXME use JsonHandler
        String json = new Gson().toJson(documents.getResults());
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
     * @return
     */
    private void initCommitFileRulesLogbookOperation(String operationFileRules, StatusCode statusCode, GUID eipMaster) {
        final GUID eipTask = GUIDFactory.newOperationLogbookGUID(getTenant());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParametersFactory
                .newLogbookOperationParameters(eipTask, operationFileRules, eipMaster,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(operationFileRules, statusCode), eipTask);
        updateLogBookEntry(logbookOperationParameters);
    }

    /**
     * Init CHECK_RULES LogbookOperation step
     *
     * @param operationFileRules
     * @param statusCode
     * @return
     */
    private void initCheckFileRulesLogbookOperation(String operationFileRules, StatusCode statusCode, GUID eipMaster) {
        final GUID eipTask = GUIDFactory.newOperationLogbookGUID(getTenant());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParametersFactory
                .newLogbookOperationParameters(eipTask, operationFileRules, eipMaster,
                    LogbookTypeProcess.MASTERDATA,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(operationFileRules, statusCode), eipTask);
        updateLogBookEntry(logbookOperationParameters);
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
        List<FileRulesModel> fileRulesModelToInsert) {
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
        updateLogBookEntry(logbookOperationParameters);
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
        Set<String> fileRulesIdsLinkedToUnit, GUID evIdentifierProcess) {
        final ObjectNode usedDeleteRuleIds = JsonHandler.createObjectNode();
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        for (String fileRulesId : fileRulesIdsLinkedToUnit) {
            arrayNode.add(fileRulesId);
        }
        usedDeleteRuleIds.set(DELETED_RULE_IDS, arrayNode);
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
        updateLogBookEntry(logbookOperationParameters);
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
        Set<String> fileRulesIdsLinkedToUnit, GUID evIdentifierProcess) {
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
        updateLogBookEntry(logbookOperationParameters);
    }

    /**
     * Update Check_Rules LogbookOperation when Au is linked to unit
     *
     * 
     * @param fileRulesIdsLinkedToUnit
     * @param deleteRulesIds
     * @param evIdentifierProcess
     */
    private void updateCheckFileRulesLogbookOperationForUpdate(
        Set<String> fileRulesIdsLinkedToUnit, Set<String> deleteRulesIds, GUID evIdentifierProcess) {
        final ObjectNode evDetData = JsonHandler.createObjectNode();
        final ArrayNode updatedArrayNode = JsonHandler.createArrayNode();
        final ArrayNode deletedArrayNode = JsonHandler.createArrayNode();
        for (String fileRulesId : deleteRulesIds) {
            deletedArrayNode.add(fileRulesId);
        }
        for (String fileRulesIds : fileRulesIdsLinkedToUnit) {
            updatedArrayNode.add(fileRulesIds);
        }
        evDetData.set(DELETED_RULE_IDS, deletedArrayNode);
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
        updateLogBookEntry(logbookOperationParameters);
    }

    /**
     * Create a LogBook Entry related to object's update
     *
     * @param logbookParametersEnd
     */
    private void updateLogBookEntry(LogbookOperationParameters logbookParametersEnd) {
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
    private void createLogBookEntry(LogbookOperationParameters logbookParametersStart) {
        try {
            client.create(logbookParametersStart);
        } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
            LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public ArrayNode checkFile(InputStream rulesFileStream)
        throws IOException, ReferentialException, InvalidParseOperationException {
        ParametersChecker.checkParameter(RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER, rulesFileStream);
        File csvFileReader = convertInputStreamToFile(rulesFileStream, TXT);
        try (FileReader reader = new FileReader(csvFileReader)) {
            @SuppressWarnings("resource")
            final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
            final HashSet<String> ruleIdSet = new HashSet<>();
            for (final CSVRecord record : parser) {
                try {
                    if (checkRecords(record)) {
                        final String ruleId = record.get(RULE_ID);
                        final String ruleType = record.get(RULE_TYPE);
                        final String ruleValue = record.get(RULE_VALUE);
                        final String ruleDuration = record.get(RULE_DURATION);
                        final String ruleMeasurementValue = record.get(RULE_MEASUREMENT);
                        checkParametersNotEmpty(ruleId, ruleType, ruleValue, ruleDuration, ruleMeasurementValue);
                        checkRuleDuration(ruleDuration);
                        if (ruleIdSet.contains(ruleId)) {
                            throw new FileRulesException(String.format(FILE_RULE_WITH_RULE_ID, ruleId));
                        }
                        ruleIdSet.add(ruleId);
                        if (!containsRuleMeasurement(ruleMeasurementValue)) {
                            throw new FileRulesException(
                                String.format(NOT_SUPPORTED_VALUE, RULE_MEASUREMENT, ruleMeasurementValue));
                        }
                        if (!containsRuleType(ruleType)) {
                            throw new FileRulesException(
                                String.format(NOT_SUPPORTED_VALUE, RULE_TYPE, ruleType));
                        }
                        checkAssociationRuleDurationRuleMeasurementLimit(record);
                    }
                } catch (final Exception e) {
                    throw new FileRulesException(INVALID_CSV_FILE + e.getMessage());
                }
            }
        }
        if (csvFileReader != null) {
            final ArrayNode readRulesAsJson = RulesManagerParser.readObjectsFromCsvWriteAsArrayNode(csvFileReader);
            csvFileReader.delete();
            return readRulesAsJson;
        }
        /* this line is reached only if temporary file is null */
        throw new FileRulesException(INVALID_CSV_FILE);
    }


    /**
     * Check Referential To Import for create ruleFiles to delete, update, insert
     *
     * @param fileRulesModelsToImport
     * @param fileRulesModelsInDb
     * @param fileRulesModelToDelete
     * @param fileRulesModelToUpdate
     * @param fileRulesModelToInsert
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
                        !fileRulesModelInDb.getRuleValue().equals(fileRulesModel.getRuleValue()))) {
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
     * Check existance of file rules linked to unit in database
     * 
     * @throws InvalidParseOperationException
     */
    public boolean checkUnitLinkedToFileRules(List<FileRulesModel> fileRulesModelToCheck,
        Set<String> rulesLinkedToUnit, Set<String> rulesNotLinkedToUnit)
        throws InvalidParseOperationException {
        boolean linked = false;
        try {
            for (FileRulesModel fileRulesModel : fileRulesModelToCheck) {
                ArrayNode arrayNodeResult =
                    checkUnitLinkedtofileRules(fileRulesLinkedToUnitQueryBuilder(fileRulesModel),
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
        Date date;
        try {
            date = LocalDateUtil.getDate(LocalDateUtil.getString(LocalDateTime.now()));
        } catch (ParseException e) {
            throw new InvalidParseOperationException("Invalid date");
        }
        final LocalDateTime localTime = LocalDateUtil.fromDate(date);
        SetAction setUpdateDate =
            new SetAction(UPDATE_DATE, localTime.toString());
        actions.add(setUpdateDate);
        SetAction setRuleMeasurement = new SetAction(RULE_MEASUREMENT, fileRulesModel.getRuleMeasurement());
        actions.add(setRuleMeasurement);
        SetAction setRuleDuration = new SetAction(RULE_DURATION, fileRulesModel.getRuleDuration());
        actions.add(setRuleDuration);
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
        } catch (InvalidParseOperationException | InvalidCreateOperationException | DatabaseException e) {
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
            // fileRules = findDocuments(select.getFinalSelect());
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
     * @return
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
     * @param ruleDuration
     * @throws FileRulesException
     */
    private void checkRuleDuration(String ruleDuration) throws FileRulesException {
        try {
            if (ruleDuration.equalsIgnoreCase(UNLIMITED)) {
                return;
            } else {
                final int duration = Integer.parseInt(ruleDuration);
                if (duration < 0) {
                    throw new FileRulesException(String.format(INVALIDPARAMETERS, RULE_DURATION, ruleDuration));
                }
            }
        } catch (final NumberFormatException e) {
            throw new FileRulesException(String.format(INVALIDPARAMETERS, RULE_DURATION, ruleDuration));
        }
    }

    /**
     * check if Records are not Empty
     *
     * @param ruleId
     * @param ruleType
     * @param ruleValue
     * @param ruleDuration
     * @param ruleMeasurementValue
     * @throws FileRulesException thrown if one ore more parameters are missing
     */
    private void checkParametersNotEmpty(String ruleId, String ruleType, String ruleValue, String ruleDuration,
        String ruleMeasurementValue) throws FileRulesException {
        final StringBuffer missingParam = new StringBuffer();
        if (ruleId == null || ruleId.isEmpty()) {
            missingParam.append(",").append(RULE_ID);
        }
        if (ruleType == null || ruleType.isEmpty()) {
            missingParam.append(",").append(RULE_TYPE);
        }
        if (ruleValue == null || ruleValue.isEmpty()) {
            missingParam.append(",").append(RULE_VALUE);
        }
        if (ruleDuration == null || ruleDuration.isEmpty()) {
            missingParam.append(",").append(RULE_DURATION);
        }
        if (ruleMeasurementValue == null || ruleMeasurementValue.isEmpty()) {
            missingParam.append(",").append(RULE_MEASUREMENT);
        }
        if (missingParam.length() > 0) {
            throw new FileRulesException(String.format(MANDATORYRULEPARAMETERISMISSING, missingParam.toString()));
        }
    }

    /**
     * Check if Records is not null
     *
     * @param record
     * @return
     */
    private boolean checkRecords(CSVRecord record) {
        return record.get(RULE_ID) != null && record.get(RULE_TYPE) != null && record.get(RULE_VALUE) != null &&
            record.get(RULE_DURATION) != null && record.get(RULE_DESCRIPTION) != null &&
            record.get(RULE_MEASUREMENT) != null;
    }


    /**
     * Check if Rule duration associated to rule measurement respect the limit of 999 years
     *
     * @param record
     * @throws FileRulesException
     */
    private void checkAssociationRuleDurationRuleMeasurementLimit(CSVRecord record) throws FileRulesException {
        if (!record.get(RULE_DURATION).equalsIgnoreCase(UNLIMITED) &&
            (record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.YEAR.getType()) &&
                Integer.parseInt(record.get(RULE_DURATION)) > YEAR_LIMIT ||
                record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.MONTH.getType()) &&
                    Integer.parseInt(record.get(RULE_DURATION)) > MONTH_LIMIT ||
                record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.DAY.getType()) &&
                    Integer.parseInt(record.get(RULE_DURATION)) > DAY_LIMIT)) {
            throw new FileRulesException(
                String.format(INVALIDPARAMETERS, RULE_DURATION, record.get(RULE_DURATION)));
        }

    }


    /**
     * Check if RuleMeasurement is included in the Enumeration
     *
     * @param test
     * @return
     */
    private static boolean containsRuleMeasurement(String test) {
        for (final RuleMeasurementEnum c : RuleMeasurementEnum.values()) {
            if (c.getType().equalsIgnoreCase(test)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if RuleType is included in the Enumeration
     *
     * @param test
     * @return
     */
    private static boolean containsRuleType(String test) {
        for (final RuleTypeEnum c : RuleTypeEnum.values()) {
            if (c.getType().equalsIgnoreCase(test)) {
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
     * @return true if an import operation is launche / false if not an import operation is in progress
     * @throws LogbookClientException when error
     */
    private boolean isImportOperationInProgress() throws LogbookClientException {
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
                        .put(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.outcome.name()), 1)));
            JsonNode logbookResult = client.selectOperation(select.getFinalSelect());
            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(logbookResult);
            // one result and statuscode is STARTED -> import in progress
            if (requestResponseOK.getHits().getSize() != 0) {
                JsonNode result = requestResponseOK.getResults().get(0);
                return StatusCode.STARTED.name().equals(
                    result.get(LogbookDocument.EVENTS).get(0).get(LogbookMongoDbName.outcome.name()).asText());
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
    private ArrayNode checkUnitLinkedtofileRules(JsonNode select, String ruleFilesId)
        throws FileFormatNotFoundException, ReferentialException {
        ArrayNode resultUnitsArray = null;
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            LOGGER.debug("Selected Query For linked unit: " + select.toString());
            final JsonNode unitsResultNode = metaDataClient.selectUnits(select);
            resultUnitsArray = (ArrayNode) unitsResultNode.get("$results");

        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException |
            InvalidParseOperationException e) {
            LOGGER.error(e);
        }
        return resultUnitsArray;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
