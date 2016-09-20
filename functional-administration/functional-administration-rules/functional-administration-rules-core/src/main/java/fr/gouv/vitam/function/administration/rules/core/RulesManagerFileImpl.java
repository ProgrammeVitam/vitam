/**
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
 */
package fr.gouv.vitam.function.administration.rules.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DbConfiguration;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;

/**
 * RulesManagerFileImpl
 * 
 * Manage the Rules File features
 */

public class RulesManagerFileImpl implements ReferentialFile<FileRules> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RulesManagerFileImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private static final String COLLECTION_NAME = "RulesFile";
    private static final String MESSAGE_LOGBOOK_IMPORT = "Succès de l'import du Règle de gestion : ";
    private static final String MESSAGE_LOGBOOK_IMPORT_ERROR = "Erreur de l'import du Règle de gestion ";
    private static final String MESSAGE_LOGBOOK_DELETE = "Succès de suppression du Règle de gestion";



    private static LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
    private static String EVENT_TYPE_CREATE = "CREATE";
    private static String EVENT_TYPE_DELETE = "DELETE";
    private static LogbookTypeProcess LOGBOOK_PROCESS_TYPE = LogbookTypeProcess.RULESMANAGER;
    private static String INVALIDPARAMETERS = "Invalid Parameters Value";
    private static String MANDATORYRULEPARAMETERISMISSING = "Check Parameters : Mandatory rule Parameters is missing";

    private enum ruleMeasurement {
        MOIS("Mois"), JOURS("Jours"), ANNEE("Année"), SECONDES("Secondes");

        private final String type;

        /**
         * Constructor
         */
        private ruleMeasurement(String ruleMeasurement) {
            type = ruleMeasurement;
        }

        private String getType() {
            return type;
        }
    }

    /**
     * Constructor
     * 
     * @param dbConfiguration
     */
    public RulesManagerFileImpl(DbConfiguration dbConfiguration) {
        this.mongoAccess = MongoDbAccessAdminFactory.create(dbConfiguration);
    }

    @Override
    public void importFile(InputStream rulesFileStream)
        throws DatabaseConflictException, IOException, InvalidParseOperationException, ReferentialException {
        ParametersChecker.checkParameter("rulesFileStreamis a mandatory parameter", rulesFileStream);
        File csvFile = null;
        try {
            csvFile = convertInputStreamToFile(rulesFileStream);
            GUID eip = GUIDFactory.newGUID();
            LogbookOperationParameters logbookParametersStart =
                LogbookParametersFactory.newLogbookOperationParameters(
                    eip, EVENT_TYPE_CREATE, eip, LOGBOOK_PROCESS_TYPE, LogbookOutcome.STARTED,
                    "start importing Rules file ", eip);
            createLogBookEntry(logbookParametersStart);

            eip = GUIDFactory.newGUID();
            try {
                ArrayNode rulesManagerList = RulesManagerParser.readObjectsFromCsvWriteAsArrayNode(csvFile);
                if (this.mongoAccess.getMongoDatabase().getCollection(COLLECTION_NAME).count() == 0) {
                    this.mongoAccess.insertDocuments(rulesManagerList, FunctionalAdminCollections.RULES);

                    LogbookOperationParameters logbookParametersEnd =
                        LogbookParametersFactory.newLogbookOperationParameters(
                            eip, EVENT_TYPE_CREATE, eip, LOGBOOK_PROCESS_TYPE, LogbookOutcome.OK,
                            MESSAGE_LOGBOOK_IMPORT,
                            eip);
                    updateLogBookEntry(logbookParametersEnd);
                } else {
                    LogbookOperationParameters logbookParametersEnd =
                        LogbookParametersFactory.newLogbookOperationParameters(eip, EVENT_TYPE_CREATE, eip,
                            LOGBOOK_PROCESS_TYPE, LogbookOutcome.ERROR, MESSAGE_LOGBOOK_IMPORT_ERROR, eip);
                    updateLogBookEntry(logbookParametersEnd);
                    throw new DatabaseConflictException("File rules collection is not empty");
                }
            } catch (FileRulesException e) {
                LOGGER.error(e.getMessage());
                LogbookOperationParameters logbookParametersEnd =
                    LogbookParametersFactory.newLogbookOperationParameters(eip, EVENT_TYPE_CREATE, eip,
                        LOGBOOK_PROCESS_TYPE, LogbookOutcome.ERROR, MESSAGE_LOGBOOK_IMPORT_ERROR, eip);
                updateLogBookEntry(logbookParametersEnd);
                throw new FileRulesException(e);
            }
        } finally {
            if (csvFile != null) {
                csvFile.delete();
            }
        }
    }

    /**
     * @param logbookParametersEnd
     */
    private void updateLogBookEntry(LogbookOperationParameters logbookParametersEnd) {
        try {
            client.update(logbookParametersEnd);
        } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
            LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
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
    public void deleteCollection() {
        GUID eip = GUIDFactory.newGUID();
        LogbookOperationParameters logbookParametersStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip, EVENT_TYPE_DELETE, eip, LOGBOOK_PROCESS_TYPE, LogbookOutcome.STARTED,
            "start deleting rulesManager from database ", eip);
        createLogBookEntry(logbookParametersStart);
        this.mongoAccess.deleteCollection(FunctionalAdminCollections.RULES);

        eip = GUIDFactory.newGUID();
        LogbookOperationParameters logbookParametersEnd =
            LogbookParametersFactory.newLogbookOperationParameters(
                eip, EVENT_TYPE_DELETE, eip, LOGBOOK_PROCESS_TYPE, LogbookOutcome.OK, MESSAGE_LOGBOOK_DELETE,
                eip);

        updateLogBookEntry(logbookParametersEnd);

    }

    @Override
    public void checkFile(InputStream rulesFileStream) throws FileRulesException, IOException {
        ParametersChecker.checkParameter("rulesFileStream is a mandatory parameter", rulesFileStream);
        File csvFile = null;
        try {
            csvFile = convertInputStreamToFile(rulesFileStream);
            try (FileReader reader = new FileReader(csvFile)) {
                CSVParser parser = new CSVParser(
                    reader,
                    CSVFormat.DEFAULT.withHeader());
                for (CSVRecord record : parser) {
                    try {
                        if (checkRecords(record)) {
                            String ruleId = record.get("RuleId");
                            String ruleType = record.get("RuleType");
                            String ruleValue = record.get("RuleValue");
                            String ruleDuration = record.get("RuleDuration");
                            String ruleMeasurementValue = record.get("RuleMeasurement");

                            if (checkParametersNotEmpty(ruleId, ruleType, ruleValue, ruleDuration,
                                ruleMeasurementValue)) {
                                throw new InvalidParameterException(MANDATORYRULEPARAMETERISMISSING);
                            }
                            checkRuleDurationIsInteger(ruleDuration);
                            if (!contains(ruleMeasurementValue)) {
                                throw new InvalidParameterException(INVALIDPARAMETERS + " : ruleMeasurement");
                            }
                        }
                    } catch (Exception e) {
                        throw new FileRulesException("Invalid CSV File :" + e);
                    }
                }
            }
        } finally {
            if (csvFile != null) {
                csvFile.delete();
            }
        }
    }

    /**
     * @param ruleDuration
     */
    private void checkRuleDurationIsInteger(String ruleDuration) {
        try {
            Integer.parseInt(ruleDuration);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(INVALIDPARAMETERS + " : ruleDuration");
        }
    }

    /**
     * 
     * check if Records is not Empty
     * 
     * @param ruleId
     * @param ruleType
     * @param ruleValue
     * @param ruleDuration
     * @param ruleMeasurementValue
     * @return
     */
    private boolean checkParametersNotEmpty(String ruleId, String ruleType, String ruleValue, String ruleDuration,
        String ruleMeasurementValue) {
        return ruleId.isEmpty() || ruleType.isEmpty() || ruleValue.isEmpty() || ruleDuration.isEmpty() ||
            ruleMeasurementValue.isEmpty();
    }

    /**
     * Check if Records is not null
     * 
     * @param record
     * @return
     */
    private boolean checkRecords(CSVRecord record) {
        return record.get("RuleId") != null && record.get("RuleType") != null && record.get("RuleValue") != null &&
            record.get("RuleDuration") != null && record.get("RuleDescription") != null &&
            record.get("RuleMeasurement") != null;
    }

    /**
     * Check if RuleMeasurement is included in the Enumeration
     * 
     * @param test
     * @return
     */
    private static boolean contains(String test) {
        for (ruleMeasurement c : ruleMeasurement.values()) {
            if (c.getType().equals(test)) {
                return true;
            }
        }
        return false;
    }


    /**
     * @param rulesStream
     * @return
     * @throws IOException
     */
    private File convertInputStreamToFile(InputStream rulesStream) throws IOException {
        File csvFile = File.createTempFile("tmp", ".txt", new File(SystemPropertyUtil.getVitamTmpFolder()));
        Files.copy(
            rulesStream,
            csvFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING);
        return csvFile;
    }

    @Override
    public FileRules findDocumentById(String id) throws ReferentialException {
        return (FileRules) this.mongoAccess.getDocumentById(id, FunctionalAdminCollections.RULES);
    }

    @Override
    public List<FileRules> findDocuments(JsonNode select) throws ReferentialException {
        try {
            MongoCursor<FileRules> rules =
                (MongoCursor<FileRules>) this.mongoAccess.select(select,
                    FunctionalAdminCollections.RULES);
            final List<FileRules> result = new ArrayList<>();
            if (rules == null || !rules.hasNext()) {
                throw new FileRulesNotFoundException("Rules not found");
            }
            while (rules.hasNext()) {
                result.add(rules.next());
            }
            return result;
        } catch (FileRulesException e) {
            LOGGER.error(e.getMessage());
            throw new FileRulesException(e);
        }
    }
}
