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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handler checking that no objects in manifest
 */
public class CheckNoObjectsActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckNoObjectsActionHandler.class);

    /**
     * Handler's ID
     */
    private static final String HANDLER_ID = "CHECK_NO_OBJECT";

    private final SedaUtilsFactory sedaUtilsFactory;

    public CheckNoObjectsActionHandler() {
        this.sedaUtilsFactory = SedaUtilsFactory.getInstance();
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try {
            checkMandatoryIOParameter(handlerIO);

            if (!checkNoObjectInManifest(handlerIO)) {
                itemStatus.increment(StatusCode.KO);
            } else {
                itemStatus.increment(StatusCode.OK);
            }
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private boolean checkNoObjectInManifest(HandlerIO handlerIO) throws ProcessingException {
        final SedaUtils sedaUtils = sedaUtilsFactory.createSedaUtilsWithSedaIngestParams(handlerIO);

        InputStream xmlFile = null;
        final XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();

        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        xmlOutputFactory.setProperty(SedaConstants.STAX_PROPERTY_PREFIX_OUTPUT_SIDE, Boolean.TRUE);

        final QName binaryDataObject = new QName(
            sedaUtils.getSedaIngestParams().getNamespaceURI(),
            SedaConstants.TAG_BINARY_DATA_OBJECT
        );
        final QName physicalDataObject = new QName(
            sedaUtils.getSedaIngestParams().getNamespaceURI(),
            SedaConstants.TAG_PHYSICAL_DATA_OBJECT
        );
        XMLEventReader eventReader = null;
        try {
            try {
                xmlFile = handlerIO.getInputStreamFromWorkspace(
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
                );
            } catch (
                ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e1
            ) {
                LOGGER.error("Workspace error: Can not get file", e1);
                throw new ProcessingException(e1);
            }

            // Create event reader
            eventReader = xmlInputFactory.createXMLEventReader(xmlFile);

            while (true) {
                final XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();

                    // reach the start of an BinaryDataObject or PhysicalDataObject
                    if (element.getName().equals(binaryDataObject) || element.getName().equals(physicalDataObject)) {
                        return false;
                    }
                }
                if (event.isEndDocument()) {
                    LOGGER.debug("data : " + event);
                    break;
                }
            }
            LOGGER.debug("End of extracting  Uri from manifest");
        } catch (XMLStreamException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        } finally {
            try {
                if (eventReader != null) {
                    eventReader.close();
                }
            } catch (final XMLStreamException e) {
                LOGGER.warn(e);
            }
            StreamUtils.closeSilently(xmlFile);
        }
        return true;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing
    }
}
