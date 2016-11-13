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
package fr.gouv.vitam.worker.core.handler;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * calculate archive unit 's Maturity date
 */
public class UnitsRulesComputeHandler extends ActionHandler {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UnitsRulesComputeHandler.class);

    private static final String HANDLER_ID = "UNITS_RULES_COMPUTE";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";
    private static final String AU_PREFIX_WITH_END_DATE = "WithEndDte_";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    private static final String AU_NOT_HAVE_RULES = "Archive unit does not have rules";
    private static final String CHECKS_RULES = "Rules checks problem: missing parameters";

    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";



    private final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters = LogbookParametersFactory
        .newLogbookLifeCycleUnitParameters();

    /**
     * Empty constructor UnitsRulesComputeHandler
     *
     */
    public UnitsRulesComputeHandler() {}

    @Override
    public CompositeItemStatus execute(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug("UNITS_RULES_COMPUTE in execute");
        long time = System.currentTimeMillis();
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            calculateMaturityDate(params, itemStatus);
            itemStatus.increment(StatusCode.OK);
            updateUnitLifeCycle(logbookLifecycleUnitParameters, params, StatusCode.OK);
        } catch (ProcessingException e) {
            LOGGER.debug(e);
            try {
                updateUnitLifeCycle(logbookLifecycleUnitParameters, params, StatusCode.KO);
            } catch (ProcessingException e1) {
                LOGGER.debug(e);
                itemStatus.increment(StatusCode.KO);
            }
            itemStatus.increment(StatusCode.KO);
        }


        LOGGER.debug("[exit] execute... /Elapsed Time:" + ((System.currentTimeMillis() - time) / 1000) + "s");
        return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub

    }


    private void calculateMaturityDate(WorkerParameters params, ItemStatus itemStatus) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();

        try (final WorkspaceClient workspaceClient =
            WorkspaceClientFactory.getInstance().getClient(); InputStream xmlInput =
                workspaceClient.getObject(containerId,
                    IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName)) {
           
            if (xmlInput != null) {
                // Parse RULES in management Archive unit, and add EndDate
                parseXmlRulesAndUpdateEndDate(xmlInput, objectName, containerId, params, itemStatus, workspaceClient);
            } else {
                LOGGER.error("Archive unit not found");
                throw new ProcessingException("Archive unit not found");
            }
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Workspace Server Error");
            throw new ProcessingException(e);
        } catch (IOException e) {
            LOGGER.error("Workspace Server Error");
            throw new ProcessingException(e);
        } catch (XMLStreamException e) {
            LOGGER.error("Workspace Server Error");
            throw new ProcessingException(e);
        }
    }

    /**
     * findRulesValueQueryBuilders: select query
     *
     * @param rulesId
     * @return the JsonNode answer
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     * @throws ProcessingException
     */

    private JsonNode findRulesValueQueryBuilders(Set<String> rulesId)
        throws InvalidCreateOperationException, InvalidParseOperationException,
        IOException, ProcessingException {
        final Select select =
            new Select();
        select.addOrderByDescFilter(FileRules.RULEID);
        final BooleanQuery query = or();
        for (String ruleId : rulesId) {
            query.add(eq(FileRules.RULEID, ruleId));
        }
        select.setQuery(query);
        
        try (AdminManagementClient adminManagementClient =
            AdminManagementClientFactory.getInstance().getClient();){
            return adminManagementClient.getRules(select.getFinalSelect());
        } catch (final VitamException e) {
            throw new ProcessingException(e);
        }
        
    }

    /**
     * 
     * parses xml unit file and add endate
     * 
     * @param xmlInput
     * @param params
     * @param itemStatus
     * @param workspaceClient
     * @throws IOException
     * @throws XMLStreamException
     * @throws ProcessingException
     */
    private void parseXmlRulesAndUpdateEndDate(InputStream xmlInput, String objectName, String containerName,
        WorkerParameters params, ItemStatus itemStatus, WorkspaceClient workspaceClient)
        throws IOException, XMLStreamException, ProcessingException {
        Set<String> rulesToApply;
        JsonNode rulesResults = null;
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        final File fileWithEndDate = PropertiesUtils.fileFromTmpFolder(AU_PREFIX_WITH_END_DATE + objectName);
        final FileWriter tmpFileWriter = new FileWriter(fileWithEndDate);
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        final XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(tmpFileWriter);
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEventReader reader = null;
        XMLEvent event = null;
        try {
            reader = xmlInputFactory.createXMLEventReader(xmlInput);
            while (true) {
                event = reader.nextEvent();
                if (event.isStartElement()) {
                    switch (event.asStartElement().getName()
                        .getLocalPart()) {
                        case SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT:
                            writer.add(event);

                            event = (XMLEvent) reader.next();
                            writer.add(event);
                            // list of rules
                            if (event.isCharacters()) {
                                rulesToApply = Sets
                                    .newHashSet(Splitter.on(SedaConstants.RULE_SEPARATOR)
                                        .split(event.asCharacters().getData()));
                                if (rulesToApply == null || rulesToApply.isEmpty()) {
                                    LOGGER.debug(AU_NOT_HAVE_RULES);

                                    if (!fileWithEndDate.delete()) {
                                        LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
                                    }
                                    return;
                                }
                                // logBook lifecycle started
                                updateUnitLifeCycle(logbookLifecycleUnitParameters, params, StatusCode.STARTED);
                                // search ref rules
                                rulesResults = findRulesValueQueryBuilders(rulesToApply);
                                LOGGER.debug("rulesResults for archive unit id: " + objectName +
                                    " && containerName is :" + containerName + " is :" + rulesResults);
                            }
                            // end of list
                            event = (XMLEvent) reader.next();
                            writer.add(event);
                            break;
                        case SedaConstants.TAG_RULE_ACCESS:
                        case SedaConstants.TAG_RULE_REUSE:
                        case SedaConstants.TAG_RULE_STORAGE:
                        case SedaConstants.TAG_RULE_APPRAISAL:
                        case SedaConstants.TAG_RULE_CLASSIFICATION:
                        case SedaConstants.TAG_RULE_DISSEMINATION: {
                            writer.add(event);
                            String ruleId = "";
                            String startDate = "";
                            String endDateAsString = "";
                            boolean isNotEndRuleTag = true;
                            while (isNotEndRuleTag) {
                                event = reader.nextEvent();
                                if (event.isStartElement()) {
                                    switch (event.asStartElement().getName()
                                        .getLocalPart()) {
                                        case SedaConstants.TAG_RULE_RULE:
                                            writer.add(event);
                                            event = (XMLEvent) reader.next();
                                            writer.add(event);
                                            if (event.isCharacters()) {
                                                ruleId = event.asCharacters().getData();
                                            }
                                            event = (XMLEvent) reader.next();
                                            writer.add(event);
                                            break;
                                        case SedaConstants.TAG_RULE_START_DATE:
                                            writer.add(event);
                                            event = (XMLEvent) reader.next();
                                            writer.add(event);
                                            if (event.isCharacters()) {
                                                startDate = event.asCharacters().getData();
                                            }
                                            event = (XMLEvent) reader.next();
                                            writer.add(event);
                                            // add End date
                                            endDateAsString = getEndDate(startDate, ruleId, rulesResults);

                                            if (StringUtils.isNotBlank(endDateAsString)) {
                                                writer.add(eventFactory.createStartElement("", "",
                                                    SedaConstants.TAG_RULE_END_DATE));
                                                writer.add(eventFactory.createCharacters(endDateAsString));
                                                writer.add(eventFactory.createEndElement("", "",
                                                    SedaConstants.TAG_RULE_END_DATE));
                                            }
                                            break;

                                        default:
                                            writer.add(event);
                                    }


                                } else if (event.isEndElement()) {
                                    switch (event.asEndElement().getName()
                                        .getLocalPart()) {
                                        case SedaConstants.TAG_RULE_ACCESS:
                                        case SedaConstants.TAG_RULE_REUSE:
                                        case SedaConstants.TAG_RULE_STORAGE:
                                        case SedaConstants.TAG_RULE_APPRAISAL:
                                        case SedaConstants.TAG_RULE_CLASSIFICATION:
                                        case SedaConstants.TAG_RULE_DISSEMINATION: {
                                            isNotEndRuleTag = false;
                                            writer.add(event);
                                            break;
                                        }
                                        default:
                                            writer.add(event);
                                    }
                                } else {
                                    writer.add(event);
                                }
                            }
                            // break supported rule
                            break;
                        }

                        default:
                            writer.add(event);
                    }

                } else if (event.isEndElement()) {

                    if (IngestWorkflowConstants.ROOT_TAG.equals(event.asEndElement().getName()
                        .getLocalPart())) {
                        writer.add(event);
                        break;
                    }

                    switch (event.asEndElement().getName()
                        .getLocalPart()) {
                        default:
                            writer.add(event);
                    }

                } else {
                    writer.add(event);
                }
            }

        } catch (Exception e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (XMLStreamException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        }
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventType, itemStatus.getItemId());

        // Write to workspace
        try {
            workspaceClient.putObject(containerName,
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName,
                new FileInputStream(fileWithEndDate));
        } catch (final ContentAddressableStorageServerException e) {
            LOGGER.error("Can not write to workspace ", e);
            if (!fileWithEndDate.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
            }
            throw new ProcessingException(e);
        }
    }

    private JsonNode getRuleNodeByID(String ruleId, JsonNode jsonResult) {
        if (jsonResult != null) {
            for (Iterator<JsonNode> iteRule = jsonResult.iterator(); iteRule.hasNext();) {
                JsonNode jsonNode = iteRule.next();
                String ruleIdFromList = jsonNode.get(FileRules.RULEID).asText();
                if (!StringUtils.isBlank(ruleId) && ruleId.equals(ruleIdFromList)) {
                    return jsonNode;
                }
            }
        }
        return JsonHandler.createObjectNode();
    }

    private String getEndDate(String startDateString, String ruleId, JsonNode rulesResults)
        throws FileRulesException, InvalidParseOperationException, ParseException, ProcessingException {
        if (!StringUtils.isBlank(startDateString) && !StringUtils.isBlank(ruleId)) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
            Date startDate = simpleDateFormat.parse(startDateString);
            JsonNode ruleNode = getRuleNodeByID(ruleId, rulesResults);
            if (checkRulesParameters(ruleNode)) {
                String duration = ruleNode.get(FileRules.RULEDURATION).asText();
                String measurement = ruleNode.get(FileRules.RULEMEASUREMENT).asText();
                RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromMonth(measurement);
                int calendarUnit = ruleMeasurement.getCalendarUnitType();
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                cal.add(calendarUnit, Integer.parseInt(duration));
                return simpleDateFormat.format(cal.getTime());
            } else {
                throw new ProcessingException(CHECKS_RULES);
            }

        }
        return "";

    }

    /**
     * @param ruleNode
     */
    private boolean checkRulesParameters(JsonNode ruleNode) {
        return (ruleNode != null && ruleNode.get(FileRules.RULEDURATION) != null &&
            ruleNode.get(FileRules.RULEMEASUREMENT) != null);
    }

    private void updateUnitLifeCycle(LogbookLifeCycleUnitParameters logbookLifecycleParameters, WorkerParameters params,
        StatusCode statusCode)
        throws ProcessingException {
        try (LogbookLifeCyclesClient logbookClient = LogbookLifeCyclesClientFactory.getInstance().getClient()) {

            String extension = FilenameUtils.getExtension(params.getObjectName());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.objectIdentifier,
                params.getObjectName().replace("." + extension, ""));
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
                params.getContainerName());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventType,
                getId());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventIdentifier,
                getId());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
                params.getCurrentStep());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.outcome,
                statusCode.toString());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                statusCode.toString());
            logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeLfc(getId(), statusCode));

            logbookClient.update(logbookLifecycleParameters);
        } catch (final LogbookClientBadRequestException e) {
            LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }
}
