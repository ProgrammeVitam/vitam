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
package fr.gouv.vitam.functional.administration.format.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server2.application.configuration.DbConfiguration;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
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
 * ReferentialFormatFileImpl implementing the ReferentialFormatFile interface
 */
public class ReferentialFormatFileImpl implements ReferentialFile<FileFormat>, AutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReferentialFormatFileImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private static final String COLLECTION_NAME = "FileFormat";
    private static final String MESSAGE_LOGBOOK_IMPORT = "Succès de l'import du Référentiel de format : ";
    private static final String MESSAGE_LOGBOOK_IMPORT_ERROR = "Erreur de l'import du Référentiel de format";
    private static final String MESSAGE_LOGBOOK_DELETE = "Succès de suppression du Référentiel de format";

    private static final String EVENT_TYPE_CREATE = "CREATE";
    private static final String EVENT_TYPE_DELETE = "DELETE";
    private static final LogbookTypeProcess LOGBOOK_PROCESS_TYPE = LogbookTypeProcess.MASTERDATA;

    /**
     * Constructor
     *
     * @param dbConfiguration
     */
    public ReferentialFormatFileImpl(DbConfiguration dbConfiguration) {
        mongoAccess = MongoDbAccessAdminFactory.create(dbConfiguration);
    }

    @Override
    public void importFile(InputStream xmlPronom) throws ReferentialException, DatabaseConflictException {
        ParametersChecker.checkParameter("Pronom file is a mandatory parameter", xmlPronom);
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            final GUID eip = GUIDFactory.newGUID();
            final LogbookOperationParameters logbookParametersStart =
                LogbookParametersFactory.newLogbookOperationParameters(
                    eip, EVENT_TYPE_CREATE, eip, LOGBOOK_PROCESS_TYPE, StatusCode.STARTED,
                    "start importing referential file ", eip);
            try {
                client.create(logbookParametersStart);
            } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
                LogbookClientServerException e) {
                LOGGER.error(e);
                throw new ReferentialException(e);
            }

            final GUID eip1 = GUIDFactory.newGUID();
            try {
                final ArrayNode pronomList = PronomParser.getPronom(xmlPronom);
                if (mongoAccess.getMongoDatabase().getCollection(COLLECTION_NAME).count() == 0) {
                    mongoAccess.insertDocuments(pronomList, FunctionalAdminCollections.FORMATS);

                    final LogbookOperationParameters logbookParametersEnd =
                        LogbookParametersFactory.newLogbookOperationParameters(
                            eip1, EVENT_TYPE_CREATE, eip, LOGBOOK_PROCESS_TYPE, StatusCode.OK,
                            MESSAGE_LOGBOOK_IMPORT + " version " + pronomList.get(0).get("VersionPronom").textValue() +
                                " du fichier de signature PRONOM (DROID_SignatureFile)",
                            eip1);

                    try {
                        client.update(logbookParametersEnd);
                    } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                        LogbookClientServerException e) {
                        LOGGER.error(e);
                        throw new ReferentialException(e);
                    }
                } else {
                    final LogbookOperationParameters logbookParametersEnd =
                        LogbookParametersFactory.newLogbookOperationParameters(eip1, EVENT_TYPE_CREATE, eip,
                            LOGBOOK_PROCESS_TYPE, StatusCode.KO, MESSAGE_LOGBOOK_IMPORT_ERROR, eip1);
                    try {
                        client.update(logbookParametersEnd);
                    } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                        LogbookClientServerException e) {
                        LOGGER.error(e);
                        throw new ReferentialException(e);
                    }

                    throw new DatabaseConflictException("File format collection is not empty");
                }
            } catch (final ReferentialException e) {
                LOGGER.error(e.getMessage());
                final LogbookOperationParameters logbookParametersEnd =
                    LogbookParametersFactory.newLogbookOperationParameters(eip, EVENT_TYPE_CREATE, eip,
                        LOGBOOK_PROCESS_TYPE, StatusCode.KO, MESSAGE_LOGBOOK_IMPORT_ERROR, eip);
                try {
                    client.update(logbookParametersEnd);
                } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                    LogbookClientServerException e1) {
                    LOGGER.error(e1);
                    throw new ReferentialException(e1);
                }
                throw new ReferentialException(e);
            }
        }
    }

    @Override
    public void deleteCollection() {
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            final GUID eip = GUIDFactory.newGUID();
            final LogbookOperationParameters logbookParametersStart =
                LogbookParametersFactory.newLogbookOperationParameters(
                    eip, EVENT_TYPE_DELETE, eip, LOGBOOK_PROCESS_TYPE, StatusCode.STARTED,
                    "start deleting referential format from database ", eip);
            try {
                client.create(logbookParametersStart);
            } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
                LogbookClientServerException e) {
                LOGGER.error(e);
            }

            mongoAccess.deleteCollection(FunctionalAdminCollections.FORMATS);

            final GUID eip1 = GUIDFactory.newGUID();
            final LogbookOperationParameters logbookParametersEnd =
                LogbookParametersFactory.newLogbookOperationParameters(
                    eip1, EVENT_TYPE_DELETE, eip, LOGBOOK_PROCESS_TYPE, StatusCode.OK, MESSAGE_LOGBOOK_DELETE,
                    eip1);

            try {
                client.update(logbookParametersEnd);
            } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                LogbookClientServerException e) {
                LOGGER.error(e);
            }
        }
    }

    @Override
    public void checkFile(InputStream xmlPronom) throws ReferentialException {
        ParametersChecker.checkParameter("Pronom file is a mandatory parameter", xmlPronom);
        try {
            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            final XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(xmlPronom);
            try {
                while (eventReader.hasNext()) {
                    eventReader.nextEvent();
                }
            } finally {
                eventReader.close();
                StreamUtils.closeSilently(xmlPronom);
            }
        } catch (final XMLStreamException e) {
            LOGGER.error(e);
            throw new ReferentialException("Invalid Format", e);
        }
    }

    @Override
    public FileFormat findDocumentById(String id) throws ReferentialException {
        try {
            return (FileFormat) mongoAccess.getDocumentById(id, FunctionalAdminCollections.FORMATS);
        } catch (final ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatException(e);
        }
    }

    @Override
    public List<FileFormat> findDocuments(JsonNode select) throws ReferentialException {
        try (@SuppressWarnings("unchecked")
        final MongoCursor<FileFormat> formats =
            (MongoCursor<FileFormat>) mongoAccess.select(select, FunctionalAdminCollections.FORMATS)) {
            final List<FileFormat> result = new ArrayList<>();
            if (formats == null || !formats.hasNext()) {
                throw new FileFormatNotFoundException("Format not found");
            }
            while (formats.hasNext()) {
                result.add(formats.next());
            }
            return result;
        } catch (final ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (mongoAccess != null) {
            mongoAccess.close();
        }
    }
}
