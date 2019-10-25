/*
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
package fr.gouv.vitam.logbook.common.server.database.collections;

import java.util.List;

import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * LogbookTransformData util
 */
public class LogbookTransformData {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookTransformData.class);

    /**
     * Replace the "evDetData" value in the document and the sub-events from a string by a json object
     *
     * @param document logbook document
     */
    public void transformDataForElastic(Document document) {
        if (document.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
            String evDetDataString = (String) document.get(LogbookMongoDbName.eventDetailData.getDbname());
            LOGGER.debug(evDetDataString);
            try {
                JsonNode evDetData = JsonHandler.getFromString(evDetDataString);
                document.remove(LogbookMongoDbName.eventDetailData.getDbname());
                document.put(LogbookMongoDbName.eventDetailData.getDbname(), evDetData);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("EvDetData is not a json compatible field", e);
            }
        }
        if (document.get(LogbookMongoDbName.agIdExt.getDbname()) != null) {
            String agidExt = (String) document.get(LogbookMongoDbName.agIdExt.getDbname());
            LOGGER.debug(agidExt);
            try {
                JsonNode agidExtNode = JsonHandler.getFromString(agidExt);
                document.remove(LogbookMongoDbName.agIdExt.getDbname());
                document.put(LogbookMongoDbName.agIdExt.getDbname(), agidExtNode);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("agidExtNode is not a json compatible field", e);
            }
        }
        if (document.get(LogbookMongoDbName.rightsStatementIdentifier.getDbname()) != null) {
            String rightsStatementIdentifier =
                (String) document.get(LogbookMongoDbName.rightsStatementIdentifier.getDbname());
            LOGGER.debug(rightsStatementIdentifier);
            try {
                JsonNode rightsStatementIdentifierNode = JsonHandler.getFromString(rightsStatementIdentifier);
                document.remove(LogbookMongoDbName.rightsStatementIdentifier.getDbname());
                document
                    .put(LogbookMongoDbName.rightsStatementIdentifier.getDbname(), rightsStatementIdentifierNode);
            } catch (InvalidParseOperationException e) {
                LOGGER.warn("rightsStatementIdentifier is not a json compatible field", e);
            }
        }
        List<Document> eventDocuments = (List<Document>) document.get(LogbookDocument.EVENTS);
        if (eventDocuments != null) {
            for (Document eventDocument : eventDocuments) {
                if (eventDocument.getString(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
                    String eventEvDetDataString =
                        eventDocument.getString(LogbookMongoDbName.eventDetailData.getDbname());
                    Document eventEvDetDataDocument = Document.parse(eventEvDetDataString);
                    eventDocument.remove(LogbookMongoDbName.eventDetailData.getDbname());
                    eventDocument.put(LogbookMongoDbName.eventDetailData.getDbname(), eventEvDetDataDocument);
                }
                if (eventDocument.getString(LogbookMongoDbName.rightsStatementIdentifier.getDbname()) != null) {
                    String eventrightsStatementIdentifier =
                        eventDocument.getString(LogbookMongoDbName.rightsStatementIdentifier.getDbname());
                    Document eventEvDetDataDocument = Document.parse(eventrightsStatementIdentifier);
                    eventDocument.remove(LogbookMongoDbName.rightsStatementIdentifier.getDbname());
                    eventDocument.put(LogbookMongoDbName.rightsStatementIdentifier.getDbname(), eventEvDetDataDocument);
                }
                if (eventDocument.getString(LogbookMongoDbName.agIdExt.getDbname()) != null) {
                    String eventagIdExt =
                        eventDocument.getString(LogbookMongoDbName.agIdExt.getDbname());
                    Document eventEvDetDataDocument = Document.parse(eventagIdExt);
                    eventDocument.remove(LogbookMongoDbName.agIdExt.getDbname());
                    eventDocument.put(LogbookMongoDbName.agIdExt.getDbname(), eventEvDetDataDocument);
                }
            }
        }
        document.remove(LogbookDocument.EVENTS);
        document.put(LogbookDocument.EVENTS, eventDocuments);

    }
}
