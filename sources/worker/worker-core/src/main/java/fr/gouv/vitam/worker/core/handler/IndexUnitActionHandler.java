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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.multiple.Insert;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * IndexUnit Handler
 */
public class IndexUnitActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexUnitActionHandler.class);
    private static final String HANDLER_ID = "IndexUnit";

    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String TAG_CONTENT = "Content";
    private static final String TAG_MANAGEMENT = "Management";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";

    private final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters = LogbookParametersFactory
        .newLogbookLifeCycleUnitParameters();
    private HandlerIO handlerIO;

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     */
    public IndexUnitActionHandler() {}

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public EngineResponse execute(WorkerParameters params, HandlerIO param) {
        checkMandatoryParameters(params);
        handlerIO = param;
        final EngineResponse response =
            new ProcessResponse().setStatus(StatusCode.OK);

        try {
            checkMandatoryIOParameter(handlerIO);
            SedaUtils.updateLifeCycleByStep(logbookLifecycleUnitParameters, params);
            indexArchiveUnit(params);
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.FATAL);
        }
        // Update lifeCycle
        try {
            if (response.getStatus().equals(StatusCode.FATAL)) {
                logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    OutcomeMessage.INDEX_UNIT_KO.value());
            } else {
                logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    OutcomeMessage.INDEX_UNIT_OK.value());
            }
            SedaUtils.setLifeCycleFinalEventStatusByStep(logbookLifecycleUnitParameters, response.getStatus());
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.WARNING);
        }

        return response;
    }

    /**
     * @param params work parameters
     * @throws ProcessingException when error in execution
     */
    private void indexArchiveUnit(WorkerParameters params) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);

        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();

        // TODO : whould use worker configuration instead of the processing configuration
        final WorkspaceClient workspaceClient = WorkspaceClientFactory.create(params.getUrlWorkspace());
        final MetaDataClient metadataClient = MetaDataClientFactory.create(params.getUrlMetadata());
        InputStream input;

        try {
            input =
                workspaceClient.getObject(containerId, IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName);

            if (input != null) {
                final Insert insertQuery = new Insert();

                final List<Object> archiveDetailsRequiredTForIndex =
                    convertArchiveUnitToJson(input, containerId, objectName);
                final JsonNode json = ((JsonNode) archiveDetailsRequiredTForIndex.get(0)).get(ARCHIVE_UNIT);

                // Add _up to archive unit json object
                if (archiveDetailsRequiredTForIndex.size() == 2) {
                    final ArrayNode parents = (ArrayNode) archiveDetailsRequiredTForIndex.get(1);
                    insertQuery.addRoots(parents);
                }

                final String insertRequest = insertQuery.addData((ObjectNode) json).getFinalInsert().toString();
                metadataClient.insertUnit(insertRequest);
            } else {
                LOGGER.error("Archive unit not found");
                throw new ProcessingException("Archive unit not found");
            }

        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Internal Server Error", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Workspace Server Error");
            throw new ProcessingException(e);
        }
    }

    private List<Object> convertArchiveUnitToJson(InputStream input, String containerId, String objectName)
        throws InvalidParseOperationException, ProcessingException {
        ParametersChecker.checkParameter("Input stream is a mandatory parameter", input);
        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        final File tmpFile = PropertiesUtils.fileFromTmpFolder(GUIDFactory.newGUID().toString());
        FileWriter tmpFileWriter = null;
        final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        final JsonXMLConfig config = new JsonXMLConfigBuilder().build();

        JsonNode data = null;
        String parentsList = null;
        final List<Object> archiveUnitDetails = new ArrayList<Object>();
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
                    if (it.hasNext() && tag != TAG_CONTENT && contentWritable) {
                        writer.add(eventFactory.createStartElement("", "", tag));

                        if (ARCHIVE_UNIT.equals(tag)) {
                            writer.add(eventFactory.createStartElement("", "", "#id"));
                            writer.add(eventFactory.createCharacters(((Attribute) it.next()).getValue()));
                            writer.add(eventFactory.createEndElement("", "", "#id"));
                        }
                        eventWritable = false;
                    }

                    if (TAG_CONTENT.equals(tag)) {
                        eventWritable = false;
                    }

                    if (TAG_MANAGEMENT.equals(tag)) {
                        writer.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_MGT));
                        eventWritable = false;
                    }
                    
                    if (SedaConstants.PREFIX_OG.equals(tag)) {
                        writer.add(eventFactory.createStartElement("", "", SedaConstants.PREFIX_OG));
                        writer.add(eventFactory.createCharacters(reader.getElementText()));
                        writer.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_OG));
                        eventWritable = false;
                    }

                    if (IngestWorkflowConstants.UP_FIELD.equals(tag)) {
                        final XMLEvent upsEvent = reader.nextEvent();
                        if (!upsEvent.isEndElement() && upsEvent.isCharacters()) {
                            parentsList = upsEvent.asCharacters().getData();
                        }
                    }

                    if (IngestWorkflowConstants.ROOT_TAG.equals(tag) || IngestWorkflowConstants.WORK_TAG.equals(tag) ||
                        IngestWorkflowConstants.UP_FIELD.equals(tag)) {
                        eventWritable = false;
                    }
                }

                if (event.isEndElement()) {
                    final EndElement endElement = event.asEndElement();
                    final String tag = endElement.getName().getLocalPart();


                    if (ARCHIVE_UNIT.equals(tag) || IngestWorkflowConstants.ROOT_TAG.equals(tag) ||
                        IngestWorkflowConstants.WORK_TAG.equals(tag) || IngestWorkflowConstants.UP_FIELD.equals(tag)) {
                        eventWritable = false;
                    }

                    if (TAG_CONTENT.equals(tag)) {
                        eventWritable = false;
                        contentWritable = false;
                    }

                    if (TAG_MANAGEMENT.equals(tag)) {
                        writer.add(eventFactory.createEndElement("", "", SedaConstants.PREFIX_MGT));
                        eventWritable = false;
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
            ((ObjectNode) data).putArray(SedaConstants.PREFIX_OPS).add(containerId);
            if (!tmpFile.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
            }

            // Prepare archive unit details required for index process
            archiveUnitDetails.add(data);

            // Add parents GUIDs
            if (parentsList != null) {
                final String[] parentsGuid = parentsList.split(IngestWorkflowConstants.UPS_SEPARATOR);
                final ArrayNode parentsArray = JsonHandler.createArrayNode();
                for (final String parent : parentsGuid) {
                    parentsArray.add(parent);
                }
                archiveUnitDetails.add(parentsArray);
            }

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
                } catch (XMLStreamException e) {
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
