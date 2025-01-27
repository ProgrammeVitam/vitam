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
package fr.gouv.vitam.functional.administration.core.agencies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
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
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.ErrorReportAgencies;
import fr.gouv.vitam.functional.administration.common.FileAgenciesErrorCode;
import fr.gouv.vitam.functional.administration.common.ReportConstants;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.AgencyImportDeletionException;
import fr.gouv.vitam.functional.administration.common.exception.InvalidFileException;
import fr.gouv.vitam.functional.administration.common.exception.InvalidFileFormatParseException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialImportInProgressException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.contract.AccessContractImpl;
import fr.gouv.vitam.functional.administration.core.contract.ContractService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.model.VitamConstants.JSON_EXTENSION;
import static fr.gouv.vitam.functional.administration.common.Agencies.DESCRIPTION;
import static fr.gouv.vitam.functional.administration.common.Agencies.IDENTIFIER;
import static fr.gouv.vitam.functional.administration.common.Agencies.NAME;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.AGENCIES;

/**
 * AgenciesService class allowing multiple operation on AgenciesService collection
 */
public class AgenciesService {

    /**
     * IMPORT_AGENCIES
     */
    static final String AGENCIES_IMPORT_EVENT = "IMPORT_AGENCIES";
    /**
     * AGENCIES_REPORT
     */
    private static final String AGENCIES_REPORT_EVENT = "AGENCIES_REPORT";
    /**
     * BACKUP_AGENCIES
     */
    private static final String AGENCIES_BACKUP_EVENT = "BACKUP_AGENCIES";
    /**
     * IMPORT_AGENCIES_BACKUP_CSV
     */
    private static final String IMPORT_AGENCIES_BACKUP_CSV = "IMPORT_AGENCIES_BACKUP_CSV";
    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AgenciesService.class);

    private static final String AGENCIES_IMPORT_ANOTHER_IMPORT_IN_PROGRESS =
        AGENCIES_IMPORT_EVENT + ".CHECK_CONCURRENT_AGENCIES_IMPORT";

    private static final String AGENCIES_IMPORT_AU_USAGE = AGENCIES_IMPORT_EVENT + ".USED_AU";
    private static final String AGENCIES_IMPORT_CONTRACT_USAGE = AGENCIES_IMPORT_EVENT + ".USED_CONTRACT";

    private static final String AGENCIES_IMPORT_DELETION_ERROR = "DELETION";
    private static final String AGENCIES_IMPORT_CONCURRENCE_ERROR = "CONCURRENCE";

    private static final String CSV = ".csv";

    private static final String INVALID_CSV_FILE = "Invalid CSV File";
    private static final String USED_AGENCIES_WANT_TO_BE_DELETED_ERROR = "used Agencies want to be deleted";

    private static final String MESSAGE_ERROR = "Import agency error > ";
    private static final String UND_ID = "_id";
    private static final String UND_TENANT = "_tenant";
    private static final String AGENCIES_PROCESS_IMPORT_ALREADY_EXIST =
        "There is already another agencies import in progress";
    public static final String CSV_MULTIPLE_STRINGS_SEPARATOR = "\\|";
    private final MongoDbAccessAdminImpl mongoAccess;
    private final FunctionalBackupService backupService;
    private final LogbookAgenciesImportManager manager;
    private final ContractService<AccessContractModel> accessContractService;
    private final VitamCounterService vitamCounterService;

    public AgenciesService(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        FunctionalBackupService backupService
    ) {
        this(
            mongoAccess,
            vitamCounterService,
            backupService,
            new LogbookAgenciesImportManager(LogbookOperationsClientFactory.getInstance()),
            new AccessContractImpl(mongoAccess, vitamCounterService)
        );
    }

    @VisibleForTesting
    public AgenciesService(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        FunctionalBackupService backupService,
        LogbookAgenciesImportManager manager,
        ContractService<AccessContractModel> accessContractService
    ) {
        this.mongoAccess = mongoAccess;
        this.backupService = backupService;
        this.manager = manager;
        this.vitamCounterService = vitamCounterService;
        this.accessContractService = accessContractService;
    }

    /**
     * Find all agencies used by access contracts
     *
     * @return list of agencies
     * @throws InvalidCreateOperationException thrown if the query could not be created
     * @throws VitamException thrown if an error is encountered
     */
    private Set<AgenciesModel> findAgenciesUsedByAccessContracts(Set<AgenciesModel> agenciesToCheck)
        throws InvalidCreateOperationException, VitamException {
        Set<AgenciesModel> usedAgenciesByContracts = new HashSet<>();

        for (AgenciesModel agency : agenciesToCheck) {
            if (isUsedByAccessContract(agency)) {
                usedAgenciesByContracts.add(agency);
            }
        }
        return usedAgenciesByContracts;
    }

    /**
     * Find all agencies used in metadata
     *
     * @return list of agencies
     * @throws InvalidCreateOperationException thrown if the query could not be created
     * @throws VitamException thrown if an error is encountered
     */
    private Set<AgenciesModel> findAgenciesUsedInMetadata(Set<AgenciesModel> agenciesToCheck)
        throws InvalidCreateOperationException, VitamException {
        Set<AgenciesModel> usedAgenciesByContracts = new HashSet<>();

        for (AgenciesModel agency : agenciesToCheck) {
            if (isUsedInMetadata(agency)) {
                usedAgenciesByContracts.add(agency);
            }
        }
        return usedAgenciesByContracts;
    }

    /**
     * convert csv file into a set of AgenciesModel
     *
     * @param csvFile the stream to be converted
     * @return a set of AgenciesModel
     * @throws IOException thrown if the file could be read
     */
    public AgenciesImportResult parseFile(File csvFile) throws IOException, ReferentialException {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        Map<Integer, List<ErrorReportAgencies>> errorsMap = new HashMap<>();
        int lineNumber = 1;
        final Map<String, AgenciesModel> agenciesModelMap = new HashMap<>();

        try (
            Reader reader = new InputStreamReader(
                BOMInputStream.builder().setFile(csvFile).get(),
                StandardCharsets.UTF_8
            )
        ) {
            try {
                final CSVParser parser = new CSVParser(
                    reader,
                    CSVFormat.DEFAULT.withHeader().withTrim().withIgnoreEmptyLines(false)
                );
                List<String> headerNames = parser.getHeaderNames();
                Set<String> duplicatedFields = headerNames
                    .stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) // Count occurrences
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

                if (CollectionUtils.isNotEmpty(duplicatedFields)) {
                    throw new InvalidFileException(
                        "Duplicate fields found in the CSV : " + String.join(",", duplicatedFields)
                    );
                }

                List<String> unknownFields = headerNames
                    .stream()
                    .filter(element -> !AgenciesModel.getAllFieldNames().contains(element))
                    .toList();
                if (CollectionUtils.isNotEmpty(unknownFields)) {
                    throw new InvalidFileException("Unknown fields found: " + String.join(",", unknownFields));
                }

                for (final CSVRecord record : parser) {
                    List<ErrorReportAgencies> errors = new ArrayList<>();
                    lineNumber++;
                    if (checkRecords(record)) {
                        final String identifier = record.get(AgenciesModel.TAG_IDENTIFIER);
                        final String name = record.get(AgenciesModel.TAG_NAME);
                        final String description = record.get(AgenciesModel.TAG_DESCRIPTION);

                        final AgenciesModel agenciesModel = new AgenciesModel(identifier, name, description, tenantId);
                        checkParametersNotEmpty(identifier, name, errors, lineNumber);

                        extractOptionalStringField(record, AgenciesModel.TAG_ENTITY_TYPE).ifPresent(
                            agenciesModel::setEntityType
                        );

                        extractOptionalStringArrayField(record, AgenciesModel.TAG_ALTERNATIVE_FORM).ifPresent(
                            agenciesModel::setAlternativeForm
                        );

                        extractOptionalStringArrayField(record, AgenciesModel.TAG_AUTHORIZED_FORM).ifPresent(
                            agenciesModel::setAuthorizedForm
                        );

                        extractOptionalStringField(record, AgenciesModel.TAG_BIOG_HIST).ifPresent(
                            agenciesModel::setBiogHist
                        );
                        extractOptionalStringField(record, AgenciesModel.TAG_GENERAL_CONTEXT).ifPresent(
                            agenciesModel::setGeneralContext
                        );

                        Optional<String> fromDateValue = extractOptionalDateField(record, AgenciesModel.TAG_FROM_DATE);
                        fromDateValue.ifPresent(agenciesModel::setFromDate);

                        Optional<String> toDateValue = extractOptionalDateField(record, AgenciesModel.TAG_TO_DATE);
                        toDateValue.ifPresent(agenciesModel::setToDate);

                        extractOptionalStringField(record, AgenciesModel.TAG_EVENT_DESCRIPTION).ifPresent(
                            agenciesModel::setEventDescription
                        );

                        extractOptionalStringField(record, AgenciesModel.TAG_ENTITY_ID).ifPresent(
                            agenciesModel::setEntityId
                        );
                        extractOptionalStringArrayField(record, AgenciesModel.TAG_NAME_ENTRY_PARALLEL).ifPresent(
                            agenciesModel::setNameEntryParallel
                        );
                        extractOptionalStringArrayField(record, AgenciesModel.TAG_FUNCTIONS).ifPresent(
                            agenciesModel::setFunctions
                        );
                        extractOptionalStringField(record, AgenciesModel.TAG_STRUCTURE_OR_GENEALOGY).ifPresent(
                            agenciesModel::setStructureOrGenealogy
                        );
                        extractOptionalStringArrayField(record, AgenciesModel.TAG_LEGAL_STATUSES).ifPresent(
                            agenciesModel::setLegalStatuses
                        );
                        extractOptionalStringArrayField(record, AgenciesModel.TAG_SOURCES).ifPresent(
                            agenciesModel::setSources
                        );
                        extractOptionalStringField(record, AgenciesModel.TAG_LOCAL_STATUS).ifPresent(
                            agenciesModel::setLocalStatus
                        );
                        extractOptionalStringArrayField(record, AgenciesModel.TAG_MANDATES).ifPresent(
                            agenciesModel::setMandates
                        );
                        extractOptionalStringField(record, AgenciesModel.TAG_MAINTENANCE_STATUS).ifPresent(
                            agenciesModel::setMaintenanceStatus
                        );

                        extractOptionalStringArrayField(record, AgenciesModel.TAG_PLACES).ifPresent(
                            agenciesModel::setPlaces
                        );

                        if (agenciesModelMap.containsKey(identifier)) {
                            errors.add(
                                new ErrorReportAgencies(
                                    FileAgenciesErrorCode.STP_IMPORT_AGENCIES_ID_DUPLICATION,
                                    lineNumber,
                                    agenciesModel
                                )
                            );
                        }

                        agenciesModelMap.put(identifier, agenciesModel);

                        if (!errors.isEmpty()) {
                            errorsMap.put(lineNumber, errors);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                String message = e.getMessage();
                if (message.contains("Name not found")) {
                    message = ReportConstants.FILE_INVALID + "Name";
                }
                if (message.contains("Identifier not found")) {
                    message = ReportConstants.FILE_INVALID + "Identifier";
                }
                if (message.contains("Description not found")) {
                    message = ReportConstants.FILE_INVALID + "Description";
                }

                throw new InvalidFileException(message);
            }

            return new AgenciesImportResult(new HashSet<>(agenciesModelMap.values()), errorsMap);
        }
    }

    private Optional<String> extractOptionalStringField(CSVRecord record, String fieldName) {
        return Optional.ofNullable(record.isSet(fieldName) ? record.get(fieldName) : null).filter(
            StringUtils::isNotEmpty
        );
    }

    private Optional<String> extractOptionalDateField(CSVRecord record, String fieldName)
        throws InvalidFileFormatParseException {
        try {
            if (!record.isSet(fieldName)) {
                return Optional.empty();
            }
            if (StringUtils.isEmpty(record.get(fieldName))) return Optional.empty();

            return Optional.of(LocalDateUtil.getFormattedDateOnlyForMongo(record.get(fieldName)));
        } catch (DateTimeParseException e) {
            throw new InvalidFileFormatParseException("The field " + fieldName + " contains bad date format value", e);
        }
    }

    private Optional<List<String>> extractOptionalStringArrayField(CSVRecord record, String fieldName)
        throws InvalidFileFormatParseException {
        if (record.isSet(fieldName)) {
            String fieldValue = record.get(fieldName);
            if (StringUtils.isNotEmpty(fieldValue)) {
                List<String> fieldValueTokens = Arrays.asList(fieldValue.split(CSV_MULTIPLE_STRINGS_SEPARATOR));
                List<String> fieldValueTokensCleaned = fieldValueTokens
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .toList();

                if (fieldValueTokensCleaned.size() < fieldValueTokens.size()) {
                    throw new InvalidFileFormatParseException(
                        "The field " + fieldName + " contains bad formatted values"
                    );
                }
                return Optional.of(fieldValueTokensCleaned);
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieve agencies in database
     *
     * @return list of agencies in database
     */
    public Set<AgenciesModel> retrieveAgenciesInDb() throws ReferentialException {
        final Select select = new Select();
        RequestResponseOK<Agencies> response = findAgencies(select.getFinalSelect()).getRequestResponseOK(
            select.getFinalSelect(),
            Agencies.class
        );
        return response.getResults().stream().map(Agencies::wrap).collect(Collectors.toSet());
    }

    private void checkParametersNotEmpty(String identifier, String name, List<ErrorReportAgencies> errors, int line) {
        List<String> missingParam = new ArrayList<>();
        if (StringUtils.isEmpty(identifier)) {
            missingParam.add(AgenciesModel.TAG_IDENTIFIER);
        }
        if (StringUtils.isEmpty(name)) {
            missingParam.add(AgenciesModel.TAG_NAME);
        }
        if (!missingParam.isEmpty()) {
            errors.add(
                new ErrorReportAgencies(
                    FileAgenciesErrorCode.STP_IMPORT_AGENCIES_MISSING_INFORMATIONS,
                    line,
                    String.join("", missingParam)
                )
            );
        }
    }

    private boolean checkRecords(CSVRecord record) {
        return record.get(IDENTIFIER) != null && record.get(NAME) != null && record.get(DESCRIPTION) != null;
    }

    /**
     * Import an input stream into agencies collection
     *
     * @param stream the stream to be imported
     * @param filename the file name
     * @return a response as a RequestResponse <AgenciesModel> object
     * @throws VitamException thrown if logbook could not be initialized
     */
    public RequestResponse<AgenciesModel> importAgencies(InputStream stream, String filename) throws VitamException {
        GUID eip = GUIDReader.getGUID(VitamThreadUtils.getVitamSession().getRequestId());
        manager.logStarted(eip, AGENCIES_IMPORT_EVENT);
        InputStream reportStream;
        File file = null;
        try {
            checkConcurrentImportOperation(eip);

            file = FileUtil.convertInputStreamToFile(stream, GUIDFactory.newGUID().getId(), AgenciesService.CSV);

            AgenciesImportResult agenciesImportResult = parseFile(file);

            if (agenciesImportResult.getErrorsMap().size() > 0) {
                throw new InvalidFileException(INVALID_CSV_FILE);
            }

            Set<AgenciesModel> agenciesToImport = agenciesImportResult.getAgenciesToImport();

            Set<AgenciesModel> agenciesInDb = retrieveAgenciesInDb();

            // agenciesToInsert
            Set<AgenciesModel> agenciesToUpdate = new HashSet<>(agenciesToImport);
            agenciesToUpdate.removeAll(agenciesInDb);

            Set<AgenciesModel> agenciesToInsert = agenciesToUpdate
                .stream()
                .filter(
                    e ->
                        agenciesInDb
                            .stream()
                            .map(AgenciesModel::getIdentifier)
                            .noneMatch(t -> t.equals(e.getIdentifier()))
                )
                .collect(Collectors.toSet());

            agenciesImportResult.setInsertedAgencies(agenciesToInsert);

            agenciesToUpdate.removeAll(agenciesToInsert);

            agenciesImportResult.setUpdatedAgencies(agenciesToUpdate);
            agenciesToUpdate.forEach(agencyModel -> agencyModel.setUpdateDate(LocalDateUtil.nowFormatted()));
            Set<AgenciesModel> agenciesToDelete = agenciesInDb
                .stream()
                .filter(
                    e ->
                        agenciesToImport
                            .stream()
                            .map(AgenciesModel::getIdentifier)
                            .noneMatch(t -> t.equals(e.getIdentifier()))
                )
                .collect(Collectors.toSet());

            agenciesImportResult.setDeletedAgencies(agenciesToDelete);
            try {
                Set<AgenciesModel> usedAgenciesToCheck = findAgenciesUsedByAccessContracts(agenciesToDelete);
                if (usedAgenciesToCheck.isEmpty()) {
                    manager.logEventSuccess(eip, AGENCIES_IMPORT_CONTRACT_USAGE);
                } else {
                    throw new AgencyImportDeletionException(USED_AGENCIES_WANT_TO_BE_DELETED_ERROR);
                }
                usedAgenciesToCheck = findAgenciesUsedInMetadata(agenciesToDelete);
                if (usedAgenciesToCheck.isEmpty()) {
                    manager.logEventSuccess(eip, AGENCIES_IMPORT_AU_USAGE);
                } else {
                    throw new AgencyImportDeletionException(USED_AGENCIES_WANT_TO_BE_DELETED_ERROR);
                }

                Set<AgenciesModel> usedAgenciesByContractsToUpdate = findAgenciesUsedByAccessContracts(
                    agenciesToUpdate
                );
                if (usedAgenciesByContractsToUpdate.isEmpty()) {
                    // OK
                    manager.logEventSuccess(eip, AGENCIES_IMPORT_CONTRACT_USAGE);
                } else {
                    // warning
                    final ArrayNode usedAgenciesContractNode = JsonHandler.createArrayNode();
                    usedAgenciesByContractsToUpdate.forEach(
                        agency -> usedAgenciesContractNode.add(agency.getIdentifier())
                    );

                    final ObjectNode data = JsonHandler.createObjectNode();
                    data.set(ReportConstants.ADDITIONAL_INFORMATION, usedAgenciesContractNode);

                    manager.setEvDetData(data);

                    manager.logEventWarning(eip, AGENCIES_IMPORT_CONTRACT_USAGE);
                }
                agenciesImportResult.setUsedAgenciesContract(usedAgenciesByContractsToUpdate);

                Set<AgenciesModel> usedAgenciesInMetadataToUpdate = findAgenciesUsedInMetadata(agenciesToUpdate);
                if (usedAgenciesByContractsToUpdate.isEmpty()) {
                    // OK
                    manager.logEventSuccess(eip, AGENCIES_IMPORT_AU_USAGE);
                } else {
                    // warning
                    final ArrayNode usedAgenciesContractNode = JsonHandler.createArrayNode();
                    usedAgenciesByContractsToUpdate.forEach(
                        agency -> usedAgenciesContractNode.add(agency.getIdentifier())
                    );

                    final ObjectNode data = JsonHandler.createObjectNode();
                    data.set(ReportConstants.ADDITIONAL_INFORMATION, usedAgenciesContractNode);

                    manager.setEvDetData(data);

                    manager.logEventWarning(eip, AGENCIES_IMPORT_AU_USAGE);
                }
                agenciesImportResult.setUsedAgenciesAU(usedAgenciesInMetadataToUpdate);
                commitAgencies(agenciesToInsert, agenciesToUpdate, agenciesToDelete);

                // store source File
                try (InputStream csvFileInputStream = new FileInputStream(file)) {
                    backupService.saveFile(
                        csvFileInputStream,
                        eip,
                        IMPORT_AGENCIES_BACKUP_CSV,
                        DataCategory.REPORT,
                        eip + CSV
                    );
                }
                // store collection
                backupService.saveCollectionAndSequence(
                    eip,
                    AGENCIES_BACKUP_EVENT,
                    FunctionalAdminCollections.AGENCIES,
                    eip.toString()
                );

                reportStream = generateReportOK(agenciesImportResult);
                // store report
                backupService.saveFile(
                    reportStream,
                    eip,
                    AGENCIES_REPORT_EVENT,
                    DataCategory.REPORT,
                    eip + JSON_EXTENSION
                );

                manager.logFinishSuccess(eip, filename, StatusCode.OK);
            } catch (final AgencyImportDeletionException e) {
                LOGGER.error(MESSAGE_ERROR, e);
                InputStream errorStream = generateErrorReport(agenciesImportResult);

                backupService.saveFile(
                    errorStream,
                    eip,
                    AGENCIES_REPORT_EVENT,
                    DataCategory.REPORT,
                    eip + JSON_EXTENSION
                );
                errorStream.close();

                ObjectNode errorMessage = JsonHandler.createObjectNode();
                String listAgencies = agenciesToDelete
                    .stream()
                    .map(AgenciesModel::getIdentifier)
                    .collect(Collectors.joining(","));
                errorMessage.put("Agencies ", listAgencies);

                return generateVitamBadRequestError(
                    eip,
                    errorMessage.toString(),
                    AgenciesService.AGENCIES_IMPORT_DELETION_ERROR
                );
            } catch (InvalidCreateOperationException | IOException e) {
                // FATAL ERROR
                LOGGER.error(MESSAGE_ERROR, e);
                return generateVitamFatalError(eip, MESSAGE_ERROR + e.getMessage());
            }
        } catch (ReferentialImportInProgressException e) {
            LOGGER.error(MESSAGE_ERROR, e);
            // no need to generate a report or store the file
            return generateVitamBadRequestError(
                eip,
                AgenciesService.AGENCIES_PROCESS_IMPORT_ALREADY_EXIST,
                AGENCIES_IMPORT_CONCURRENCE_ERROR
            );
        } catch (InvalidFileException | InvalidFileFormatParseException e) {
            LOGGER.error(MESSAGE_ERROR, e);
            return generateVitamBadRequestError(eip, MESSAGE_ERROR + e.getMessage(), null);
        } catch (IOException | IllegalPathException | VitamException e) {
            LOGGER.error(MESSAGE_ERROR, e);
            return generateVitamFatalError(eip, MESSAGE_ERROR + e.getMessage());
        } finally {
            if (file != null && !file.delete()) {
                LOGGER.warn("Failed to delete file");
            }
        }

        return new RequestResponseOK<AgenciesModel>().setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    private boolean isUsedByAccessContract(AgenciesModel agency)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException {
        final Select select = new Select();
        select.setQuery(in(AccessContract.ORIGINATINGAGENCIES, agency.getIdentifier()));
        final JsonNode queryDsl = select.getFinalSelect();

        RequestResponseOK<AccessContractModel> result = accessContractService.findContracts(queryDsl);

        return !result.getResults().isEmpty();
    }

    private boolean isUsedInMetadata(AgenciesModel agency)
        throws InvalidCreateOperationException, ReferentialException {
        Select select = new Select();
        select.setQuery(
            QueryHelper.and().add(QueryHelper.eq(AccessionRegisterDetail.ORIGINATING_AGENCY, agency.getIdentifier()))
        );
        final JsonNode queryDsl = select.getFinalSelect();
        DbRequestResult result = mongoAccess.findDocuments(
            queryDsl,
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY
        );
        RequestResponseOK<AccessionRegisterDetail> response = result.getRequestResponseOK(
            queryDsl,
            AccessionRegisterDetail.class
        );

        return !response.getResults().isEmpty();
    }

    private VitamError<AgenciesModel> generateVitamBadRequestError(GUID eip, String err, String subEvenType)
        throws VitamException {
        manager.logError(eip, err, subEvenType);
        return new VitamError<AgenciesModel>(VitamCode.AGENCIES_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
            .setCode(VitamCode.AGENCIES_VALIDATION_ERROR.getItem())
            .setDescription(err)
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private VitamError<AgenciesModel> generateVitamFatalError(GUID eip, String err) throws VitamException {
        manager.logEventFatal(eip, AGENCIES_IMPORT_EVENT);
        return new VitamError<AgenciesModel>(VitamCode.AGENCIES_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
            .setDescription(err)
            .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private void insertDocuments(Set<AgenciesModel> agenciesToInsert, Integer sequence)
        throws InvalidParseOperationException, ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        ArrayNode agenciesNodeToPersist = JsonHandler.createArrayNode();

        for (final AgenciesModel agency : agenciesToInsert) {
            String currentDate = LocalDateUtil.nowFormatted();
            agency.setId(GUIDFactory.newGUID().getId());
            agency.setTenant(ParameterHelper.getTenantParameter());
            agency.setCreationDate(currentDate);
            agency.setUpdateDate(currentDate);
            ObjectNode agencyNode = (ObjectNode) JsonHandler.toJsonNode(agency);
            JsonNode jsonNode = agencyNode.remove(VitamFieldsHelper.id());
            if (jsonNode != null) {
                agencyNode.set(UND_ID, jsonNode);
            }
            JsonNode hashTenant = agencyNode.remove(VitamFieldsHelper.tenant());
            if (hashTenant != null) {
                agencyNode.set(UND_TENANT, hashTenant);
            }
            agenciesNodeToPersist.add(agencyNode);
        }

        if (!agenciesToInsert.isEmpty()) {
            mongoAccess.insertDocuments(agenciesNodeToPersist, AGENCIES, sequence).close();
        }
    }

    private void commitAgencies(
        Set<AgenciesModel> agenciesToInsert,
        Set<AgenciesModel> agenciesToUpdate,
        Set<AgenciesModel> agenciesToDelete
    )
        throws InvalidParseOperationException, ReferentialException, InvalidCreateOperationException, SchemaValidationException, BadRequestException, DocumentAlreadyExistsException {
        Integer sequence = vitamCounterService.getNextSequence(
            ParameterHelper.getTenantParameter(),
            SequenceType.AGENCIES_SEQUENCE
        );

        for (AgenciesModel agency : agenciesToUpdate) {
            updateAgency(agency, sequence);
        }

        for (AgenciesModel agenciesModel : agenciesToDelete) {
            deleteAgency(agenciesModel);
        }

        if (!agenciesToInsert.isEmpty()) {
            insertDocuments(agenciesToInsert, sequence);
        }
    }

    private void updateAgency(AgenciesModel fileAgenciesModel, Integer sequence)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException, SchemaValidationException, BadRequestException {
        final UpdateParserSingle updateParser = new UpdateParserSingle(new VarNameAdapter());
        final Update updateFileAgencies = new Update();
        fileAgenciesModel.setUpdateDate(LocalDateUtil.nowFormatted());

        List<Action> actions = prepareUpdateActions(fileAgenciesModel);
        updateFileAgencies.setQuery(eq(AgenciesModel.TAG_IDENTIFIER, fileAgenciesModel.getIdentifier()));
        updateFileAgencies.addActions(actions.toArray(new Action[0]));

        updateParser.parse(updateFileAgencies.getFinalUpdate());

        mongoAccess.updateData(updateParser.getRequest().getFinalUpdate(), AGENCIES, sequence);
    }

    private List<Action> prepareUpdateActions(AgenciesModel fileAgenciesModel) throws InvalidCreateOperationException {
        List<Action> actions = new ArrayList<>();
        actions.add(new SetAction(AgenciesModel.TAG_NAME, fileAgenciesModel.getName()));
        actions.add(new SetAction(AgenciesModel.TAG_DESCRIPTION, fileAgenciesModel.getDescription()));
        actions.add(new SetAction(AgenciesModel.TAG_UPDATE_DATE, fileAgenciesModel.getUpdateDate()));
        if (StringUtils.isNotEmpty(fileAgenciesModel.getEntityType())) {
            actions.add(new SetAction(AgenciesModel.TAG_ENTITY_TYPE, fileAgenciesModel.getEntityType()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_ENTITY_TYPE));
        }
        if (CollectionUtils.isNotEmpty(fileAgenciesModel.getNameEntryParallel())) {
            actions.add(new SetAction(AgenciesModel.TAG_NAME_ENTRY_PARALLEL, fileAgenciesModel.getNameEntryParallel()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_NAME_ENTRY_PARALLEL));
        }
        if (CollectionUtils.isNotEmpty(fileAgenciesModel.getAuthorizedForm())) {
            actions.add(new SetAction(AgenciesModel.TAG_AUTHORIZED_FORM, fileAgenciesModel.getAuthorizedForm()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_AUTHORIZED_FORM));
        }
        if (CollectionUtils.isNotEmpty(fileAgenciesModel.getAlternativeForm())) {
            actions.add(new SetAction(AgenciesModel.TAG_ALTERNATIVE_FORM, fileAgenciesModel.getAlternativeForm()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_ALTERNATIVE_FORM));
        }
        if (StringUtils.isNotEmpty(fileAgenciesModel.getEntityId())) {
            actions.add(new SetAction(AgenciesModel.TAG_ENTITY_ID, fileAgenciesModel.getEntityId()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_ENTITY_ID));
        }
        if (StringUtils.isNotEmpty(fileAgenciesModel.getFromDate())) {
            actions.add(new SetAction(AgenciesModel.TAG_FROM_DATE, fileAgenciesModel.getFromDate()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_FROM_DATE));
        }
        if (StringUtils.isNotEmpty(fileAgenciesModel.getToDate())) {
            actions.add(new SetAction(AgenciesModel.TAG_TO_DATE, fileAgenciesModel.getToDate()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_TO_DATE));
        }
        if (CollectionUtils.isNotEmpty(fileAgenciesModel.getFunctions())) {
            actions.add(new SetAction(AgenciesModel.TAG_FUNCTIONS, fileAgenciesModel.getFunctions()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_FUNCTIONS));
        }
        if (StringUtils.isNotEmpty(fileAgenciesModel.getBiogHist())) {
            actions.add(new SetAction(AgenciesModel.TAG_BIOG_HIST, fileAgenciesModel.getBiogHist()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_BIOG_HIST));
        }
        if (CollectionUtils.isNotEmpty(fileAgenciesModel.getPlaces())) {
            actions.add(new SetAction(AgenciesModel.TAG_PLACES, fileAgenciesModel.getPlaces()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_PLACES));
        }
        if (CollectionUtils.isNotEmpty(fileAgenciesModel.getLegalStatuses())) {
            actions.add(new SetAction(AgenciesModel.TAG_LEGAL_STATUSES, fileAgenciesModel.getLegalStatuses()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_LEGAL_STATUSES));
        }
        if (CollectionUtils.isNotEmpty(fileAgenciesModel.getMandates())) {
            actions.add(new SetAction(AgenciesModel.TAG_MANDATES, fileAgenciesModel.getMandates()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_MANDATES));
        }
        if (StringUtils.isNotEmpty(fileAgenciesModel.getStructureOrGenealogy())) {
            actions.add(
                new SetAction(AgenciesModel.TAG_STRUCTURE_OR_GENEALOGY, fileAgenciesModel.getStructureOrGenealogy())
            );
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_STRUCTURE_OR_GENEALOGY));
        }
        if (StringUtils.isNotEmpty(fileAgenciesModel.getGeneralContext())) {
            actions.add(new SetAction(AgenciesModel.TAG_GENERAL_CONTEXT, fileAgenciesModel.getGeneralContext()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_GENERAL_CONTEXT));
        }
        if (StringUtils.isNotEmpty(fileAgenciesModel.getMaintenanceStatus())) {
            actions.add(new SetAction(AgenciesModel.TAG_MAINTENANCE_STATUS, fileAgenciesModel.getMaintenanceStatus()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_MAINTENANCE_STATUS));
        }

        if (StringUtils.isNotEmpty(fileAgenciesModel.getLocalStatus())) {
            actions.add(new SetAction(AgenciesModel.TAG_LOCAL_STATUS, fileAgenciesModel.getLocalStatus()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_LOCAL_STATUS));
        }

        if (CollectionUtils.isNotEmpty(fileAgenciesModel.getSources())) {
            actions.add(new SetAction(AgenciesModel.TAG_SOURCES, fileAgenciesModel.getSources()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_SOURCES));
        }

        if (StringUtils.isNotEmpty(fileAgenciesModel.getEventDescription())) {
            actions.add(new SetAction(AgenciesModel.TAG_EVENT_DESCRIPTION, fileAgenciesModel.getEventDescription()));
        } else {
            actions.add(new UnsetAction(AgenciesModel.TAG_EVENT_DESCRIPTION));
        }
        return actions;
    }

    private void deleteAgency(AgenciesModel fileAgenciesModel) throws BadRequestException, ReferentialException {
        final Delete delete = new Delete();
        DbRequestResult result;

        try {
            delete.setQuery(eq(AgenciesModel.TAG_IDENTIFIER, fileAgenciesModel.getIdentifier()));
            result = mongoAccess.deleteDocument(delete.getFinalDelete(), FunctionalAdminCollections.AGENCIES);
            result.close();
        } catch (final SchemaValidationException | InvalidCreateOperationException e) {
            throw new BadRequestException(e);
        }
    }

    /**
     * Find agencies with a specific query
     *
     * @param queryDsl the query to be executed
     * @return a DbRequestResult containing agencies
     * @throws ReferentialException thrown if the query could not be executed
     */
    public DbRequestResult findAgencies(JsonNode queryDsl) throws ReferentialException {
        return mongoAccess.findDocuments(queryDsl, AGENCIES);
    }

    private InputStream generateReportOK(AgenciesImportResult agenciesImportResult) {
        AgenciesReport reportFinal = generateReport(agenciesImportResult);
        return new ByteArrayInputStream(JsonHandler.unprettyPrint(reportFinal).getBytes(StandardCharsets.UTF_8));
    }

    private AgenciesReport generateReport(AgenciesImportResult agenciesImportResult) {
        AgenciesReport report = new AgenciesReport();
        HashMap<String, String> guidmasterNode = new HashMap<>();

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        guidmasterNode.put(ReportConstants.EV_TYPE, AGENCIES_IMPORT_EVENT);
        guidmasterNode.put(ReportConstants.EV_DATE_TIME, LocalDateUtil.nowFormatted());
        guidmasterNode.put(ReportConstants.EV_ID, operationId);

        report.setOperation(guidmasterNode);
        report.setAgenciesToImport(
            agenciesImportResult
                .getAgenciesToImport()
                .stream()
                .map(AgenciesModel::getIdentifier)
                .collect(Collectors.toList())
        );
        report.setInsertedAgencies(
            agenciesImportResult
                .getInsertedAgencies()
                .stream()
                .map(AgenciesModel::getIdentifier)
                .collect(Collectors.toList())
        );
        report.setUpdatedAgencies(
            agenciesImportResult
                .getUpdatedAgencies()
                .stream()
                .map(AgenciesModel::getIdentifier)
                .collect(Collectors.toList())
        );
        report.setUsedAgenciesByContracts(
            agenciesImportResult
                .getUsedAgenciesContract()
                .stream()
                .map(AgenciesModel::getIdentifier)
                .collect(Collectors.toList())
        );
        report.setUsedAgenciesByAu(
            agenciesImportResult
                .getUsedAgenciesAU()
                .stream()
                .map(AgenciesModel::getIdentifier)
                .collect(Collectors.toList())
        );
        report.setAgenciesToDelete(
            agenciesImportResult
                .getDeletedAgencies()
                .stream()
                .map(AgenciesModel::getIdentifier)
                .collect(Collectors.toList())
        );

        return report;
    }

    /**
     * Generate an error report
     *
     * @return an input stream containing the report
     */
    public InputStream generateErrorReport(AgenciesImportResult agenciesImportResult) {
        final AgenciesReport reportFinal = generateReport(agenciesImportResult);

        final ArrayNode messagesArrayNode = JsonHandler.createArrayNode();
        final HashMap<String, Object> errors = new HashMap<>();

        for (Integer line : agenciesImportResult.getErrorsMap().keySet()) {
            List<ErrorReportAgencies> errorsReports = agenciesImportResult.getErrorsMap().get(line);
            for (ErrorReportAgencies error : errorsReports) {
                final ObjectNode errorNode = JsonHandler.createObjectNode();
                errorNode.put(ReportConstants.CODE, error.getCode().name() + ".KO");
                errorNode.put(ReportConstants.MESSAGE, VitamErrorMessages.getFromKey(error.getCode().name()));
                switch (error.getCode()) {
                    case STP_IMPORT_AGENCIES_MISSING_INFORMATIONS:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION, error.getMissingInformations());
                        break;
                    case STP_IMPORT_AGENCIES_ID_DUPLICATION:
                        errorNode.put(ReportConstants.ADDITIONAL_INFORMATION, error.getFileAgenciesModel().getId());
                        break;
                    case STP_IMPORT_AGENCIES_NOT_CSV_FORMAT:
                    case STP_IMPORT_AGENCIES_DELETE_USED_AGENCIES:
                    case STP_IMPORT_AGENCIES_UPDATED_AGENCIES:
                    default:
                        break;
                }
                messagesArrayNode.add(errorNode);
            }
            errors.put(String.format("line %s", line), messagesArrayNode);
        }
        reportFinal.setErrors(errors);
        return new ByteArrayInputStream(JsonHandler.unprettyPrint(reportFinal).getBytes(StandardCharsets.UTF_8));
    }

    private void checkConcurrentImportOperation(GUID eip) throws VitamException {
        if (manager.isImportOperationInProgress(eip)) {
            throw new ReferentialImportInProgressException(AGENCIES_PROCESS_IMPORT_ALREADY_EXIST);
        }
        manager.logEventSuccess(eip, AGENCIES_IMPORT_ANOTHER_IMPORT_IN_PROGRESS);
    }
}
