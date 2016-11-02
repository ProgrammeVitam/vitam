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
package fr.gouv.vitam.function.administration.rules.core;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

/**
 * RulesManagerFileImpl
 *
 * Manage the Rules File features
 */

public class RulesManagerFileImpl implements ReferentialFile<FileRules>, VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RulesManagerFileImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private static final String COLLECTION_NAME = "RulesFile";

    private static final String MESSAGE_LOGBOOK_IMPORT = "Référentiel des règles de gestion importé avec succès ";
    private static final String MESSAGE_LOGBOOK_IMPORT_ERROR = "Echec de l'import du référentiel de règle de gestion";
    private static final String MESSAGE_LOGBOOK_DELETE = "Référentiel des règles de gestion purgé avec succès";
    private static final String RULEID = "RuleId";

    private LogbookOperationsClient client;
    private static String EVENT_TYPE_CREATE = "Import du référentiel des règles de gestion";
    private static String EVENT_TYPE_DELETE = "Suppression du référentiel de règle de gestion";
    private static LogbookTypeProcess LOGBOOK_PROCESS_TYPE = LogbookTypeProcess.MASTERDATA;
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
    public RulesManagerFileImpl(MongoDbAccessAdminImpl dbConfiguration) {
        mongoAccess = dbConfiguration;
    }

    @Override
    public void importFile(InputStream rulesFileStream)
        throws DatabaseConflictException, IOException, InvalidParseOperationException, ReferentialException {
        ParametersChecker.checkParameter("rulesFileStreamis a mandatory parameter", rulesFileStream);
        File csvFile = null;
        try (LogbookOperationsClient client2 = LogbookOperationsClientFactory.getInstance().getClient()) {
            this.client = client2;
            csvFile = convertInputStreamToFile(rulesFileStream);
            final GUID eip = GUIDFactory.newGUID();
            final LogbookOperationParameters logbookParametersStart =
                LogbookParametersFactory.newLogbookOperationParameters(
                    eip, EVENT_TYPE_CREATE, eip, LOGBOOK_PROCESS_TYPE, StatusCode.STARTED,
                    "Lancement de l’import du référentiel des règles de gestion ", eip);
            createLogBookEntry(logbookParametersStart);

            final GUID eip1 = GUIDFactory.newGUID();
            try {
                final ArrayNode rulesManagerList = RulesManagerParser.readObjectsFromCsvWriteAsArrayNode(csvFile);
                if (mongoAccess.getMongoDatabase().getCollection(COLLECTION_NAME).count() == 0) {
                    mongoAccess.insertDocuments(rulesManagerList, FunctionalAdminCollections.RULES);

                    final LogbookOperationParameters logbookParametersEnd =
                        LogbookParametersFactory.newLogbookOperationParameters(
                            eip1, EVENT_TYPE_CREATE, eip, LOGBOOK_PROCESS_TYPE, StatusCode.OK,
                            MESSAGE_LOGBOOK_IMPORT,
                            eip1);
                    updateLogBookEntry(logbookParametersEnd);
                } else {
                    final LogbookOperationParameters logbookParametersEnd =
                        LogbookParametersFactory.newLogbookOperationParameters(eip1, EVENT_TYPE_CREATE, eip,
                            LOGBOOK_PROCESS_TYPE, StatusCode.KO, MESSAGE_LOGBOOK_IMPORT_ERROR, eip1);
                    updateLogBookEntry(logbookParametersEnd);
                    throw new DatabaseConflictException("File rules collection is not empty");
                }
            } catch (final FileRulesException e) {
                LOGGER.error(e.getMessage());
                final LogbookOperationParameters logbookParametersEnd =
                    LogbookParametersFactory.newLogbookOperationParameters(eip1, EVENT_TYPE_CREATE, eip,
                        LOGBOOK_PROCESS_TYPE, StatusCode.KO, MESSAGE_LOGBOOK_IMPORT_ERROR, eip1);
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
        try (LogbookOperationsClient client2 = LogbookOperationsClientFactory.getInstance().getClient()) {
            this.client = client2;

            final GUID eip = GUIDFactory.newGUID();
            final LogbookOperationParameters logbookParametersStart =
                LogbookParametersFactory.newLogbookOperationParameters(
                    eip, EVENT_TYPE_DELETE, eip, LOGBOOK_PROCESS_TYPE, StatusCode.STARTED,
                    "Lancement de la purge du référentiel des règles de gestion ", eip);

            createLogBookEntry(logbookParametersStart);
            mongoAccess.deleteCollection(FunctionalAdminCollections.RULES);

            final GUID eip1 = GUIDFactory.newGUID();
            final LogbookOperationParameters logbookParametersEnd =
                LogbookParametersFactory.newLogbookOperationParameters(
                    eip1, EVENT_TYPE_DELETE, eip, LOGBOOK_PROCESS_TYPE, StatusCode.OK, MESSAGE_LOGBOOK_DELETE,
                    eip1);

            updateLogBookEntry(logbookParametersEnd);

        }
    }

    @Override
    public void checkFile(InputStream rulesFileStream)
        throws IOException, ReferentialException, InvalidParseOperationException, InvalidCreateOperationException {
        ParametersChecker.checkParameter("rulesFileStream is a mandatory parameter", rulesFileStream);
        if (checkifTheCollectionIsEmptyBeforeImport()) {
            throw new FileRulesException("The Collection is Not Empty");
        }
        File csvFileReader = null;
        try {
            csvFileReader = convertInputStreamToFile(rulesFileStream);
            try (FileReader reader = new FileReader(csvFileReader)) {
                @SuppressWarnings("resource")
                final CSVParser parser = new CSVParser(
                    reader,
                    CSVFormat.DEFAULT.withHeader());
                final HashSet<String> ruleIdSet = new HashSet<String>();
                for (final CSVRecord record : parser) {
                    try {
                        if (checkRecords(record)) {
                            final String ruleId = record.get(RULEID);
                            final String ruleType = record.get("RuleType");
                            final String ruleValue = record.get("RuleValue");
                            final String ruleDuration = record.get("RuleDuration");
                            final String ruleMeasurementValue = record.get("RuleMeasurement");

                            checkParametersNotEmpty(ruleId, ruleType, ruleValue, ruleDuration,
                                ruleMeasurementValue);
                            checkRuleDurationIsInteger(ruleDuration);
                            if (ruleIdSet.contains(ruleId)) {
                                throw new FileRulesException("File Rule with Rule Id" + ruleId + " Already exists");
                            }
                            ruleIdSet.add(ruleId);
                            if (!contains(ruleMeasurementValue)) {
                                throw new InvalidParameterException(INVALIDPARAMETERS + " : ruleMeasurement");
                            }
                        }
                    } catch (final Exception e) {
                        throw new FileRulesException("Invalid CSV File :" + e.getMessage());
                    }
                }
            }
        } finally {
            if (csvFileReader != null) {
                csvFileReader.delete();
            }
        }
    }

    /**
     * checkifTheCollectionIsEmptyBeforeImport : Check if the Collection is empty .
     *
     * @return
     *
     *
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     * @throws ReferentialException
     */
    private boolean checkifTheCollectionIsEmptyBeforeImport()
        throws InvalidParseOperationException, InvalidCreateOperationException, ReferentialException {
        return FunctionalAdminCollections.RULES.getCount() > 0;
    }

    /**
     * findExistsRuleQueryBuilder:Check if the Collection contains records
     *
     * @param rulesValue
     * @return the JsonNode answer
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     */
    public JsonNode findExistsRuleQueryBuilder()
        throws InvalidCreateOperationException, InvalidParseOperationException {
        JsonNode result;
        final Select select =
            new Select();
        select.addOrderByDescFilter(RULEID);
        final BooleanQuery query = and();
        query.add(exists(RULEID));
        select.setQuery(query);
        result = JsonHandler.getFromString(select.getFinalSelect().toString());
        return result;
    }

    /**
     * @param ruleDuration
     */
    private void checkRuleDurationIsInteger(String ruleDuration) {
        try {
            Integer.parseInt(ruleDuration);
        } catch (final NumberFormatException e) {
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
     *
     */
    private void checkParametersNotEmpty(String ruleId, String ruleType, String ruleValue, String ruleDuration,
        String ruleMeasurementValue) {
        if (ruleId.isEmpty() || ruleType.isEmpty() || ruleValue.isEmpty() || ruleDuration.isEmpty() ||
            ruleMeasurementValue.isEmpty()) {
            throw new InvalidParameterException(MANDATORYRULEPARAMETERISMISSING);
        }
    }

    /**
     * Check if Records is not null
     *
     * @param record
     * @return
     */
    private boolean checkRecords(CSVRecord record) {
        return record.get(RULEID) != null && record.get("RuleType") != null && record.get("RuleValue") != null &&
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
        for (final ruleMeasurement c : ruleMeasurement.values()) {
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
        try {
            final File csvFile = File.createTempFile("tmp", ".txt", new File(VitamConfiguration.getVitamTmpFolder()));
            Files.copy(
                rulesStream,
                csvFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            return csvFile;
        } finally {
            StreamUtils.closeSilently(rulesStream);
        }
    }

    @Override
    public FileRules findDocumentById(String id) throws ReferentialException {
        return (FileRules) mongoAccess.getDocumentById(id, FunctionalAdminCollections.RULES);
    }

    @Override
    public List<FileRules> findDocuments(JsonNode select) throws ReferentialException {
        try (@SuppressWarnings("unchecked")
        final MongoCursor<FileRules> rules =
            (MongoCursor<FileRules>) mongoAccess.select(select,
                FunctionalAdminCollections.RULES)) {
            final List<FileRules> result = new ArrayList<>();
            if (rules == null || !rules.hasNext()) {
                throw new FileRulesNotFoundException("Rules not found");
            }
            while (rules.hasNext()) {
                result.add(rules.next());
            }
            return result;
        } catch (final FileRulesException e) {
            LOGGER.error(e.getMessage());
            throw new FileRulesException(e);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
