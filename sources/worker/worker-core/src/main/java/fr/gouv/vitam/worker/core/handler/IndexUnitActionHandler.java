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
package fr.gouv.vitam.worker.core.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.Insert;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.Update;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * IndexUnit Handler
 */
public class IndexUnitActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexUnitActionHandler.class);
    private static final String HANDLER_ID = "UNIT_METADATA_INDEXATION";

    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String TAG_CONTENT = "Content";
    private static final String TAG_MANAGEMENT = "Management";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";

    private HandlerIO handlerIO;

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     */
    public IndexUnitActionHandler() {
        // Empty
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO param) {
        checkMandatoryParameters(params);
        handlerIO = param;
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        final String objectID = LogbookLifecycleWorkerHelper.getObjectID(params);

        try {
            try {
                checkMandatoryIOParameter(handlerIO);

                LogbookLifecycleWorkerHelper.updateLifeCycleStartStep(handlerIO.getHelper(),
                    logbookLifecycleUnitParameters,
                    params, HANDLER_ID, LogbookTypeProcess.INGEST);
                indexArchiveUnit(params, itemStatus);
            } catch (final ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }

            // Update lifeCycle
            try {
                logbookLifecycleUnitParameters.setFinalStatus(HANDLER_ID, null, itemStatus.getGlobalStatus(),
                    null);
                LogbookLifecycleWorkerHelper.setLifeCycleFinalEventStatusByStep(handlerIO.getHelper(),
                    logbookLifecycleUnitParameters,
                    itemStatus);

            } catch (final ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        } finally {
            try {
                handlerIO.getLifecyclesClient().bulkUpdateUnit(params.getContainerName(),
                    handlerIO.getHelper().removeUpdateDelegate(objectID));
            } catch (LogbookClientNotFoundException | LogbookClientBadRequestException |
                LogbookClientServerException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);

    }

    /**
     * Index archive unit
     * 
     * @param params work parameters
     * @param itemStatus item status
     * @throws ProcessingException when error in execution
     */
    private void indexArchiveUnit(WorkerParameters params, ItemStatus itemStatus) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);

        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();

        InputStream input;
        Response response = null;
        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            response = handlerIO
                .getInputStreamNoCachedFromWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName);

            if (response != null) {
                input = (InputStream) response.getEntity();

                final Map<String, Object> archiveDetailsRequiredForIndex =
                    convertArchiveUnitToJson(input, containerId, objectName);
                final JsonNode data = ((JsonNode) archiveDetailsRequiredForIndex.get("data")).get(ARCHIVE_UNIT);
                final Boolean existing = (Boolean) archiveDetailsRequiredForIndex.get("existing");

                RequestMultiple query;
                if (existing) {
                    query = new Update();
                } else {
                    query = new Insert();
                }
                // Add _up to archive unit json object
                if (archiveDetailsRequiredForIndex.get("up") != null) {
                    final ArrayNode parents = (ArrayNode) archiveDetailsRequiredForIndex.get("up");
                    query.addRoots(parents);
                }
                if (Boolean.TRUE.equals(existing)) {
                    // update case
                    computeExistingData(data, query);
                    metadataClient.updateUnitbyId(((Update) query).getFinalUpdate(),
                        (String) archiveDetailsRequiredForIndex.get("id"));
                } else {
                    // insert case
                    metadataClient.insertUnit(((Insert) query).addData((ObjectNode) data).getFinalInsert());
                }
                itemStatus.increment(StatusCode.OK);
            } else {
                LOGGER.error("Archive unit not found");
                throw new ProcessingException("Archive unit not found");
            }

        } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Internal Server Error", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Workspace Server Error");
            throw new ProcessingException(e);
        } finally {
            handlerIO.consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Import existing data and add them to the data defined in sip in update query
     * 
     * @param data sip defined data
     * @param query update query
     * @throws InvalidCreateOperationException exception while adding an action to the query
     */
    private void computeExistingData(final JsonNode data, RequestMultiple query)
        throws InvalidCreateOperationException {
        Iterator<String> fieldNames = data.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (data.get(fieldName).isArray()) {
                // if field is multiple values
                for (JsonNode fieldNode : (ArrayNode) data.get(fieldName)) {
                    ((Update) query)
                        .addActions(UpdateActionHelper.add(fieldName.replace("_", "#"), fieldNode.textValue()));
                }
            } else {
                // if field is single value
                String fieldValue = data.get(fieldName).textValue();
                if (!"#id".equals(fieldName) && fieldValue != null && !fieldValue.isEmpty()) {
                    ((Update) query)
                        .addActions(UpdateActionHelper.set(fieldName.replace("_", "#"), fieldValue));
                }
            }
        }
    }

    /**
     * Convert xml archive unit to json node for insert/update.
     * 
     * @param input xml archive unit
     * @param containerId container id
     * @param objectName unit file name
     * @return map of data
     * @throws InvalidParseOperationException exception while reading temporary json file
     * @throws ProcessingException exception while reading xml file
     */
    private Map<String, Object> convertArchiveUnitToJson(InputStream input, String containerId, String objectName)
        throws InvalidParseOperationException, ProcessingException {
        ParametersChecker.checkParameter("Input stream is a mandatory parameter", input);
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        final File tmpFile = handlerIO.getNewLocalFile(GUIDFactory.newGUID().toString());
        FileWriter tmpFileWriter = null;
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final JsonXMLConfig config = new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true)
            .namespaceDeclarations(false).build();

        JsonNode data = null;
        String parentsList = null;
        final Map<String, Object> archiveUnitDetails = new HashMap<>();

        String unitGuid = null;
        boolean existingUnit = false;

        XMLEventReader reader = null;

        try {
            tmpFileWriter = new FileWriter(tmpFile);
            reader = XMLInputFactory.newInstance().createXMLEventReader(input);

            final XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(tmpFileWriter);
            boolean contentWritable = true;
            while (true) {
                final XMLEvent event = reader.nextEvent();
                boolean eventWritable = true;
                if (event.isStartElement()) {
                    final StartElement startElement = event.asStartElement();
                    final Iterator<?> it = startElement.getAttributes();
                    final String tag = startElement.getName().getLocalPart();
                    if (it.hasNext() && !TAG_CONTENT.equals(tag) && contentWritable) {
                        writer.add(eventFactory.createStartElement("", "", tag));

                        if (ARCHIVE_UNIT.equals(tag)) {
                            unitGuid = ((Attribute) it.next()).getValue();
                            writer.add(eventFactory.createStartElement("", "", "#id"));
                            writer.add(eventFactory.createCharacters(unitGuid));
                            writer.add(eventFactory.createEndElement("", "", "#id"));
                        }
                        eventWritable = false;
                    }
                    switch (tag) {
                        case TAG_MANAGEMENT:
                            writer.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_MGT));
                            eventWritable = false;
                            contentWritable = true;
                            break;
                        case SedaConstants.PREFIX_OG:
                            writer.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_OG));
                            writer.add(eventFactory.createCharacters(reader.getElementText()));
                            writer.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_OG));
                            eventWritable = false;
                            break;
                        case IngestWorkflowConstants.UP_FIELD:
                            final XMLEvent upsEvent = reader.nextEvent();
                            if (!upsEvent.isEndElement() && upsEvent.isCharacters()) {
                                parentsList = upsEvent.asCharacters().getData();
                            }
                            eventWritable = false;
                            break;
                        case TAG_CONTENT:
                        case IngestWorkflowConstants.ROOT_TAG:
                        case IngestWorkflowConstants.WORK_TAG:
                            eventWritable = false;
                            break;
                        case IngestWorkflowConstants.EXISTING_TAG:
                            eventWritable = false;
                            existingUnit = Boolean.parseBoolean(reader.getElementText());
                            break;
                        case IngestWorkflowConstants.RULES:
                            reader.nextEvent();
                            eventWritable = false;
                            break;
                    }
                }

                if (event.isEndElement()) {
                    final EndElement endElement = event.asEndElement();
                    final String tag = endElement.getName().getLocalPart();

                    switch (tag) {
                        case ARCHIVE_UNIT:
                        case IngestWorkflowConstants.ROOT_TAG:
                        case IngestWorkflowConstants.WORK_TAG:
                        case IngestWorkflowConstants.UP_FIELD:
                        case IngestWorkflowConstants.EXISTING_TAG:
                        case IngestWorkflowConstants.RULES:
                            eventWritable = false;
                            break;
                        case TAG_CONTENT:
                            eventWritable = false;
                            contentWritable = false;
                            break;
                        case TAG_MANAGEMENT:
                            writer.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_MGT));
                            eventWritable = false;
                            break;

                    }
                }

                if (event.isEndDocument()) {
                    writer.add(event);
                    break;
                }
                if (eventWritable && contentWritable) {
                    writer.add(event);
                }
            }
            writer.close();
            tmpFileWriter.close();
            data = JsonHandler.getFromFile(tmpFile);
            // Add operation to OPS
            ((ObjectNode) data.get(ARCHIVE_UNIT)).putArray(VitamFieldsHelper.operations()).add(containerId);


            if (!tmpFile.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
            }

            // Prepare archive unit details required for index process
            archiveUnitDetails.put("data", data);

            // Add parents GUIDs
            if (parentsList != null) {
                final String[] parentsGuid = parentsList.split(IngestWorkflowConstants.UPS_SEPARATOR);
                final ArrayNode parentsArray = JsonHandler.createArrayNode();
                for (final String parent : parentsGuid) {
                    parentsArray.add(parent);
                }
                archiveUnitDetails.put("up", parentsArray);
            }

            archiveUnitDetails.put("existing", existingUnit);
            archiveUnitDetails.put("id", unitGuid);

        } catch (final XMLStreamException e) {
            LOGGER.debug("Can not read input stream");
            throw new ProcessingException(e);
        } catch (final IOException e) {
            LOGGER.debug("Closing stream error");
            throw new ProcessingException(e);
        } finally {
            StreamUtils.closeSilently(input);
            if (reader != null) {
                try {
                    reader.close();
                } catch (final XMLStreamException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        }
        return archiveUnitDetails;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Handler without parameters input
    }
}
