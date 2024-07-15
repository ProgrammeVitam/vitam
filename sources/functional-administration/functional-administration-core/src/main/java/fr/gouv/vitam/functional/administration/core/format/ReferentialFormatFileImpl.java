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
package fr.gouv.vitam.functional.administration.core.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.format.model.FileFormatImportEventDetails;
import fr.gouv.vitam.functional.administration.core.format.model.FileFormatModel;
import fr.gouv.vitam.functional.administration.core.format.model.FormatImportReport;
import fr.gouv.vitam.functional.administration.core.format.model.FunctionalOperationModel;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.functional.administration.common.FileFormat.CREATED_DATE;
import static fr.gouv.vitam.functional.administration.common.FileFormat.PUID;
import static fr.gouv.vitam.functional.administration.common.FileFormat.UPDATE_DATE;
import static fr.gouv.vitam.functional.administration.common.FileFormat.VERSION_PRONOM;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.FORMATS;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.REFERENTIAL_FORMAT_IMPORT;

/**
 * ReferentialFormatFileImpl implementing the ReferentialFormatFile interface
 */
public class ReferentialFormatFileImpl implements ReferentialFile<FileFormat>, VitamAutoCloseable {

    public static final String FILE_FORMAT_REPORT = "FILE_FORMAT_REPORT";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReferentialFormatFileImpl.class);
    private static final String BACKUP_FORMAT_EVENT = "STP_BACKUP_REFERENTIAL_FORMAT";
    private static final String VERSION = " version ";
    private static final String FILE_PRONOM = " du fichier de signature PRONOM (DROID_SignatureFile)";
    private final MongoDbAccessAdminImpl mongoAccess;
    private final FunctionalBackupService backupService;
    private final LogbookOperationsClient logbookOperationsClient;

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access for reference format configuration
     * @param vitamCounterService
     */
    public ReferentialFormatFileImpl(MongoDbAccessAdminImpl dbConfiguration, VitamCounterService vitamCounterService) {
        mongoAccess = dbConfiguration;
        backupService = new FunctionalBackupService(vitamCounterService);
        logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient();
    }

    @VisibleForTesting
    ReferentialFormatFileImpl(
        MongoDbAccessAdminImpl dbConfiguration,
        FunctionalBackupService backupService,
        LogbookOperationsClient logbookOperationsClient
    ) {
        this.mongoAccess = dbConfiguration;
        this.logbookOperationsClient = logbookOperationsClient;
        this.backupService = backupService;
    }

    @Override
    public void importFile(InputStream xmlPronom, String filename) throws VitamException {
        ParametersChecker.checkParameter("Pronom file is a mandatory parameter", xmlPronom);
        final List<FileFormatModel> newFileFormalModels = checkFile(xmlPronom);

        final GUID eip = createLogbook();

        final GUID eip1 = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        try {
            final ObjectNode selectAll = new Select().getFinalSelect();
            DbRequestResult result = mongoAccess.findDocuments(selectAll, FORMATS);
            List<FileFormat> documents = result.getDocuments(FileFormat.class);

            List<FileFormatModel> currentFileFormatModels = documents
                .stream()
                .map(this::toFileFormatModel)
                .collect(Collectors.toList());

            FormatImportReport report = generateReport(currentFileFormatModels, newFileFormalModels);

            deleteRemovedFormats(report);

            insertAddedFormats(report, newFileFormalModels);

            updateExistingFormats(report, newFileFormalModels);

            //store collection
            backupService.saveCollectionAndSequence(eip, BACKUP_FORMAT_EVENT, FORMATS, eip.toString());

            try (InputStream reportInputStream = JsonHandler.writeToInpustream(report)) {
                final String fileName = eip + ".json";
                backupService.saveFile(reportInputStream, eip, FILE_FORMAT_REPORT, DataCategory.REPORT, fileName);
            } catch (IOException | VitamException e) {
                throw new StorageException(e.getMessage(), e);
            }

            final LogbookOperationParameters logbookParametersEnd = createLogbookOperationParametersImport(
                filename,
                newFileFormalModels,
                eip,
                eip1,
                report
            );

            updateLogbook(logbookParametersEnd);
        } catch (final ReferentialException e) {
            LOGGER.error(e);

            LogbookOperationParameters logbookParametersEnd = createLogbookOperationParametersKo(eip, eip1);

            updateLogbook(logbookParametersEnd);

            throw e;
        }
    }

    private void deleteRemovedFormats(FormatImportReport report) throws ReferentialException {
        if (report.getRemovedPuids().isEmpty()) {
            return;
        }

        try {
            Delete delete = new Delete();
            delete.setQuery(in(PUID, report.getRemovedPuids().toArray(new String[0])));

            mongoAccess.deleteDocument(delete.getFinalDelete(), FORMATS);
        } catch (
            InvalidCreateOperationException | BadRequestException | ReferentialException | SchemaValidationException e
        ) {
            throw new ReferentialException("Could not delete removed formats", e);
        }
    }

    private void insertAddedFormats(FormatImportReport report, List<FileFormatModel> newFileFormatModels)
        throws ReferentialException {
        if (report.getAddedPuids().isEmpty()) {
            return;
        }

        try {
            List<FileFormatModel> fileFormatsToAdd = newFileFormatModels
                .stream()
                .filter(fileFormatModel -> report.getAddedPuids().contains(fileFormatModel.getPuid()))
                .collect(Collectors.toList());

            mongoAccess.insertDocuments((ArrayNode) JsonHandler.toJsonNode(fileFormatsToAdd), FORMATS).close();
        } catch (
            ReferentialException
            | DocumentAlreadyExistsException
            | SchemaValidationException
            | InvalidParseOperationException e
        ) {
            throw new ReferentialException("Could not insert added formats", e);
        }
    }

    private void updateExistingFormats(FormatImportReport report, List<FileFormatModel> newFileFormatModels)
        throws ReferentialException {
        List<FileFormatModel> existingFormatsToUpdate = newFileFormatModels
            .stream()
            .filter(fileFormatModel -> !report.getAddedPuids().contains(fileFormatModel.getPuid()))
            .filter(fileFormatModel -> !report.getRemovedPuids().contains(fileFormatModel.getPuid()))
            .collect(Collectors.toList());

        updateExistingFormats(existingFormatsToUpdate);
    }

    private void updateExistingFormats(List<FileFormatModel> fileFormatModels) throws ReferentialException {
        Map<String, JsonNode> fileFormatModelMap = fileFormatModels
            .stream()
            .collect(
                Collectors.toMap(FileFormatModel::getPuid, fileFormatModel -> {
                    try {
                        return JsonHandler.toJsonNode(fileFormatModel);
                    } catch (InvalidParseOperationException e) {
                        throw new VitamRuntimeException(
                            "Could not update format document with puid " + fileFormatModel.getPuid(),
                            e
                        );
                    }
                })
            );
        try {
            mongoAccess.replaceDocuments(fileFormatModelMap, PUID, FunctionalAdminCollections.FORMATS);
        } catch (DatabaseException e) {
            throw new ReferentialException(
                "Could not update format documents with PUIDs " + fileFormatModelMap.keySet(),
                e
            );
        }
    }

    private FormatImportReport generateReport(
        List<FileFormatModel> currentFileFormatModels,
        List<FileFormatModel> newFileFormatModels
    ) {
        FormatImportReport report = new FormatImportReport();

        FunctionalOperationModel operationModel = retrieveOperationModel();

        report.setOperation(operationModel);

        if (!currentFileFormatModels.isEmpty()) {
            report.setPreviousPronomVersion(currentFileFormatModels.get(0).getVersionPronom());
            report.setPreviousPronomCreationDate(currentFileFormatModels.get(0).getCreatedDate());
        }
        if (!newFileFormatModels.isEmpty()) {
            report.setNewPronomVersion(newFileFormatModels.get(0).getVersionPronom());
            report.setNewPronomCreationDate(newFileFormatModels.get(0).getCreatedDate());
        }

        Map<String, FileFormatModel> currentFileFormatModelsByPuid = mapByPuid(currentFileFormatModels);
        Map<String, FileFormatModel> newFileFormatModelsByPuid = mapByPuid(newFileFormatModels);

        SetUtils.SetView<String> removedPuids = SetUtils.difference(
            currentFileFormatModelsByPuid.keySet(),
            newFileFormatModelsByPuid.keySet()
        );
        for (String removedPuid : removedPuids) {
            LOGGER.warn("Removed puid: " + removedPuid);
            report.addRemovedPuids(removedPuid);
        }

        SetUtils.SetView<String> addedPuids = SetUtils.difference(
            newFileFormatModelsByPuid.keySet(),
            currentFileFormatModelsByPuid.keySet()
        );
        for (String addedPuid : addedPuids) {
            LOGGER.debug("Added puid: " + addedPuid);
            report.addAddedPuid(addedPuid);
        }

        SetUtils.SetView<String> commonPuids = SetUtils.intersection(
            newFileFormatModelsByPuid.keySet(),
            currentFileFormatModelsByPuid.keySet()
        );

        for (String commonPuid : commonPuids) {
            FileFormatModel currentFileFormatModel = currentFileFormatModelsByPuid.get(commonPuid);
            FileFormatModel newFileFormatModel = newFileFormatModelsByPuid.get(commonPuid);
            List<String> diff = diff(currentFileFormatModel, newFileFormatModel);

            if (diff.isEmpty()) {
                LOGGER.debug("Unchanged puid: " + commonPuid);
                continue;
            }

            LOGGER.debug("Updated puid: " + commonPuid);
            report.addUpdatedPuids(commonPuid, diff);
        }

        if (report.getPreviousPronomVersion() != null && report.getNewPronomVersion() != null) {
            int previousVersion = Integer.parseInt(report.getPreviousPronomVersion());
            int newVersion = Integer.parseInt(report.getNewPronomVersion());

            if (previousVersion == newVersion) {
                report.addWarning("Same referential version: " + newVersion);
            } else if (previousVersion > newVersion) {
                report.addWarning(
                    "New imported referential version " +
                    newVersion +
                    " is older than previous referential version " +
                    previousVersion
                );
            }
        }

        if (report.getPreviousPronomCreationDate() != null && report.getNewPronomCreationDate() != null) {
            String previousDate = LocalDateUtil.getFormattedDateTimeForMongo(report.getPreviousPronomCreationDate());
            String newDate = LocalDateUtil.getFormattedDateTimeForMongo(report.getNewPronomCreationDate());

            if (previousDate.equals(newDate)) {
                report.addWarning("Same referential date: " + report.getNewPronomCreationDate());
            } else if (previousDate.compareTo(newDate) > 0) {
                report.addWarning(
                    "New imported referential date " +
                    report.getNewPronomCreationDate() +
                    " is older than previous report date " +
                    report.getPreviousPronomCreationDate()
                );
            }
        }

        if (!removedPuids.isEmpty()) {
            report.addWarning(removedPuids.size() + " puids removed.");
        }

        if (report.getWarnings().isEmpty()) {
            report.setStatusCode(StatusCode.OK);
        } else {
            report.setStatusCode(StatusCode.WARNING);
        }

        return report;
    }

    private FunctionalOperationModel retrieveOperationModel() {
        try {
            JsonNode result = logbookOperationsClient.selectOperationById(
                VitamThreadUtils.getVitamSession().getRequestId()
            );

            return JsonHandler.getFromJsonNode(result.get(TAG_RESULTS).get(0), FunctionalOperationModel.class);
        } catch (LogbookClientException | InvalidParseOperationException e) {
            throw new VitamRuntimeException("Could not load operation data", e);
        }
    }

    private Map<String, FileFormatModel> mapByPuid(List<FileFormatModel> fileFormatModels) {
        return fileFormatModels
            .stream()
            .collect(Collectors.toMap(FileFormatModel::getPuid, fileFormatModel -> fileFormatModel));
    }

    private List<String> diff(FileFormatModel currentFileFormatModel, FileFormatModel newFileFormatModel) {
        String before = toComparableString(currentFileFormatModel);
        String after = toComparableString(newFileFormatModel);

        List<String> unifiedDiff = VitamDocument.getUnifiedDiff(before, after);

        List<String> concernedDiffLines = VitamDocument.getConcernedDiffLines(unifiedDiff);
        concernedDiffLines.sort(Comparator.naturalOrder());
        return concernedDiffLines;
    }

    private String toComparableString(FileFormatModel currentFileFormatModel) {
        try {
            ObjectNode currentJsonNode = (ObjectNode) JsonHandler.toJsonNode(currentFileFormatModel);
            // Exclude ignored fields from comparison
            currentJsonNode.remove(VERSION_PRONOM);
            currentJsonNode.remove(CREATED_DATE);
            currentJsonNode.remove(UPDATE_DATE);
            return JsonHandler.prettyPrint(currentJsonNode);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private FileFormatModel toFileFormatModel(FileFormat fileFormat) {
        try {
            return JsonHandler.getFromJsonNode(JsonHandler.toJsonNode(fileFormat), FileFormatModel.class);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException("Could parse current file formats", e);
        }
    }

    private LogbookOperationParameters createLogbookOperationParametersKo(GUID eip, GUID eip1) {
        return LogbookParameterHelper.newLogbookOperationParameters(
            eip1,
            REFERENTIAL_FORMAT_IMPORT.getEventType(),
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.KO,
            VitamLogbookMessages.getCodeOp(REFERENTIAL_FORMAT_IMPORT.getEventType(), StatusCode.KO),
            eip
        );
    }

    private void updateLogbook(LogbookOperationParameters logbookParametersEnd) throws ReferentialException {
        try {
            logbookOperationsClient.update(logbookParametersEnd);
        } catch (LogbookClientBadRequestException | LogbookClientNotFoundException | LogbookClientServerException e) {
            LOGGER.error(e);
            throw new ReferentialException(e);
        }
    }

    private LogbookOperationParameters createLogbookOperationParametersImport(
        String filename,
        List<FileFormatModel> pronomList,
        GUID eip,
        GUID eip1,
        FormatImportReport report
    ) throws InvalidParseOperationException {
        final LogbookOperationParameters logbookParametersEnd = LogbookParameterHelper.newLogbookOperationParameters(
            eip1,
            REFERENTIAL_FORMAT_IMPORT.getEventType(),
            eip,
            REFERENTIAL_FORMAT_IMPORT.getLogbookTypeProcess(),
            report.getStatusCode(),
            VitamLogbookMessages.getCodeOp(REFERENTIAL_FORMAT_IMPORT.getEventType(), report.getStatusCode()) +
            VERSION +
            pronomList.get(0).getVersionPronom() +
            FILE_PRONOM,
            eip
        );

        FileFormatImportEventDetails eventDetails = new FileFormatImportEventDetails().setFilename(filename);
        if (!report.getWarnings().isEmpty()) {
            eventDetails.setWarnings(report.getWarnings());
        }

        ObjectNode evDetData = (ObjectNode) JsonHandler.toJsonNode(eventDetails);
        logbookParametersEnd.putParameterValue(
            LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData)
        );

        return logbookParametersEnd;
    }

    private GUID createLogbook() throws ReferentialException {
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip;
        try {
            eip = GUIDReader.getGUID(operationId);
            final LogbookOperationParameters logbookParametersStart =
                LogbookParameterHelper.newLogbookOperationParameters(
                    eip,
                    REFERENTIAL_FORMAT_IMPORT.getEventType(),
                    eip,
                    LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(REFERENTIAL_FORMAT_IMPORT.getEventType(), StatusCode.STARTED),
                    eip
                );
            logbookOperationsClient.create(logbookParametersStart);
        } catch (
            LogbookClientBadRequestException
            | LogbookClientAlreadyExistsException
            | LogbookClientServerException
            | InvalidGuidOperationException e
        ) {
            LOGGER.error(e);
            throw new ReferentialException(e);
        }
        return eip;
    }

    /**
     * check PRONOM File and return all format as arraynode
     *
     * @param xmlPronom format file stream
     * @return arraynode of format
     * @throws ReferentialException
     */
    public List<FileFormatModel> checkFile(InputStream xmlPronom) throws ReferentialException {
        ParametersChecker.checkParameter("Pronom file is a mandatory parameter", xmlPronom);

        File xmlPronomFile = null;
        try {
            xmlPronomFile = PropertiesUtils.fileFromTmpFolder(
                VitamThreadUtils.getVitamSession().getRequestId() + ".xml"
            );

            FileUtils.copyInputStreamToFile(xmlPronom, xmlPronomFile);
            /*
             * Deserialize as json arrayNode, this operation will will ensure the format is valid first, else Exception is
             * thrown
             */

            return PronomParser.getPronom(xmlPronomFile);
        } catch (IOException e) {
            throw new ReferentialException(e);
        } finally {
            StreamUtils.closeSilently(xmlPronom);
            FileUtils.deleteQuietly(xmlPronomFile);
        }
    }

    @Override
    public FileFormat findDocumentById(String id) {
        return (FileFormat) mongoAccess.getDocumentByUniqueId(id, FORMATS, PUID);
    }

    @Override
    public RequestResponseOK<FileFormat> findDocuments(JsonNode select) throws ReferentialException {
        try (DbRequestResult result = mongoAccess.findDocuments(select, FORMATS)) {
            return result.getRequestResponseOK(select, FileFormat.class);
        }
    }

    @Override
    public void close() {
        // Empty
        logbookOperationsClient.close();
    }
}
