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
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.RuleTypeEnum;
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

    private static final String RULES_FILE_STREAMIS_A_MANDATORY_PARAMETER = "rulesFileStreamis a mandatory parameter";
    private static final String RULES_FILE_STREAM_IS_A_MANDATORY_PARAMETER = "rulesFileStream is a mandatory parameter";
    private static final String RULES_COLLECTION_IS_NOT_EMPTY = "Rules has been already imported for the tenant { %d }";
    private static final String INVALID_CSV_FILE = "Invalid CSV File :";
    private static final String FILE_RULE_WITH_RULE_ID = "Rule with Id %s already exists";
    private static final String RULES_NOT_FOUND = "Rules not found";
    private static final String TXT = ".txt";
    private static final String TMP = "tmp";
    private static final String RULE_MEASUREMENT = "RuleMeasurement";
    private static final String RULE_DURATION = "RuleDuration";
    private static final String RULE_DESCRIPTION = "RuleDescription";
    private static final String RULE_VALUE = "RuleValue";
    private static final String RULE_TYPE = "RuleType";
    private static final String UNLIMITED = "unlimited";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RulesManagerFileImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private static final String RULEID = "RuleId";
    private LogbookOperationsClient client;

    private static String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private static String INVALIDPARAMETERS = "Invalid Parameter Value %s : %s";
    private static String NOT_SUPPORTED_VALUE = "The value %s of parameter %s is not supported";
    private static String MANDATORYRULEPARAMETERISMISSING = "The following mandatory parameters are missing %s";
    private static int YEAR_LIMIT = 999;
    private static int MONTH_LIMIT = YEAR_LIMIT * 12;
    private static int DAY_LIMIT = MONTH_LIMIT * 30;

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access admin configuration
     */
    public RulesManagerFileImpl(MongoDbAccessAdminImpl dbConfiguration) {
        mongoAccess = dbConfiguration;
    }

    @Override
    public void importFile(InputStream rulesFileStream)
        throws IOException, InvalidParseOperationException, ReferentialException {
        ParametersChecker.checkParameter(RULES_FILE_STREAMIS_A_MANDATORY_PARAMETER, rulesFileStream);
        /* To process import validate the file first */
        final ArrayNode validatedRules = checkFile(rulesFileStream);
        if (validatedRules != null) {
            try (LogbookOperationsClient client2 = LogbookOperationsClientFactory.getInstance().getClient()) {
                client = client2;
                final GUID eip = GUIDFactory.newOperationLogbookGUID(getTenant());
                final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
                    .newLogbookOperationParameters(eip, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                        StatusCode.STARTED,
                        VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.STARTED), eip);
                createLogBookEntry(logbookParametersStart);
                try {
                    mongoAccess.insertDocuments(validatedRules, FunctionalAdminCollections.RULES);
                    final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                        .newLogbookOperationParameters(eip, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                            StatusCode.OK, VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.OK),
                            eip);
                    updateLogBookEntry(logbookParametersEnd);

                } catch (final FileRulesException e) {
                    LOGGER.error(e.getMessage());
                    final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                        .newLogbookOperationParameters(eip, STP_IMPORT_RULES, eip, LogbookTypeProcess.MASTERDATA,
                            StatusCode.KO, VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.KO),
                            eip);
                    updateLogBookEntry(logbookParametersEnd);
                    throw new FileRulesException(e);
                }
            }
        }
    }

    /**
     * Create a LogBook Entry related to object's update
     *
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
        if (!isCollectionEmptyForTenant()) {
            throw new FileRulesException(String.format(RULES_COLLECTION_IS_NOT_EMPTY, getTenant()));
        }
        File csvFileReader = convertInputStreamToFile(rulesFileStream);
        try (FileReader reader = new FileReader(csvFileReader)) {
            @SuppressWarnings("resource")
            final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
            final HashSet<String> ruleIdSet = new HashSet<>();
            for (final CSVRecord record : parser) {
                try {
                    if (checkRecords(record)) {
                        final String ruleId = record.get(RULEID);
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
     * checkifTheCollectionIsEmptyBeforeImport : Check if the Collection is empty .
     *
     * @return
     *
     *
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
        select.addOrderByDescFilter(RULEID);
        final BooleanQuery query = and();
        query.add(exists(RULEID));
        select.setQuery(query);
        result = select.getFinalSelect();
        return result;
    }

    /**
     * Check if the rule duration is integer
     *
     *
     * @param ruleDuration
     * @throws FileRulesException
     */
    private void checkRuleDuration(String ruleDuration) throws FileRulesException {
        try {
            if (ruleDuration.equalsIgnoreCase(UNLIMITED)) {
                return;
            } else {
                int duration = Integer.parseInt(ruleDuration);
                if (duration < 0) {
                    throw new FileRulesException(String.format(INVALIDPARAMETERS, RULE_DURATION, ruleDuration));
                }
            }
        } catch (final NumberFormatException e) {
            throw new FileRulesException(String.format(INVALIDPARAMETERS, RULE_DURATION, ruleDuration));
        }
    }

    /**
     *
     * check if Records are not Empty
     * 
     * @param ruleId
     * @param ruleType
     * @param ruleValue
     * @param ruleDuration
     * @param ruleMeasurementValue
     * @throws FileRulesException thrown if one ore more parameters are missing
     *
     */
    private void checkParametersNotEmpty(String ruleId, String ruleType, String ruleValue, String ruleDuration,
        String ruleMeasurementValue) throws FileRulesException {
        StringBuffer missingParam = new StringBuffer();
        if (ruleId == null || ruleId.isEmpty()) {
            missingParam.append(",").append(RULEID);
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
        return record.get(RULEID) != null && record.get(RULE_TYPE) != null && record.get(RULE_VALUE) != null &&
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
            ((record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.YEAR.getType()) &&
                Integer.parseInt(record.get(RULE_DURATION)) > YEAR_LIMIT) ||
                (record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.MONTH.getType()) &&
                    Integer.parseInt(record.get(RULE_DURATION)) > MONTH_LIMIT) ||
                (record.get(RULE_MEASUREMENT).equalsIgnoreCase(RuleMeasurementEnum.DAY.getType()) &&
                    Integer.parseInt(record.get(RULE_DURATION)) > DAY_LIMIT)))
            throw new FileRulesException(
                String.format(INVALIDPARAMETERS, RULE_DURATION, record.get(RULE_DURATION)));

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
     *
     * @param rulesStream
     * @return
     * @throws IOException
     */
    private File convertInputStreamToFile(InputStream rulesStream) throws IOException {
        try {
            final File csvFile = File.createTempFile(TMP, TXT, new File(VitamConfiguration.getVitamTmpFolder()));
            Files.copy(rulesStream, csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return csvFile;
        } finally {
            StreamUtils.closeSilently(rulesStream);
        }
    }

    @Override
    public FileRules findDocumentById(String id) throws ReferentialException {
        FileRules fileRule = (FileRules) mongoAccess.getDocumentByUniqueId(id, FunctionalAdminCollections.RULES, FileRules.RULEID);
        if (fileRule == null) {
            throw new FileRulesException("FileRules Not Found");
        }
        return fileRule;
    }

    @Override
    public List<FileRules> findDocuments(JsonNode select) throws ReferentialException {
        try (
            final MongoCursor<VitamDocument<?>> rules = mongoAccess.findDocuments(select,
                FunctionalAdminCollections.RULES)) {

            final List<FileRules> result = new ArrayList<>();
            if (rules == null || !rules.hasNext()) {
                throw new FileRulesNotFoundException(RULES_NOT_FOUND);
            }
            while (rules.hasNext()) {
                result.add((FileRules) rules.next());
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
