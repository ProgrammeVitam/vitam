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
package fr.gouv.vitam.storage.engine.server.accesslogger;

import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.text.SimpleDateFormat;


public class AccessLogServiceImpl implements AccessLogService {

    /* Classic logger or custom in file ? */
    private VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessLogServiceImpl.class);
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); // Voir le format ?

    public AccessLogServiceImpl() {
        super();
    }

    public AccessLogServiceImpl(VitamLogger lOGGER) {
        super();
        LOGGER = lOGGER;
    }

    @Override
    public void logAccess(VitamLogLevel level, AccessLogStructure data) throws StorageAccessLogException {
        String message = makeMessage(data);
        LOGGER.log(level, message);
    }

    @Override
    public void logAccess(AccessLogStructure data) throws StorageAccessLogException {
        String message = makeMessage(data);
        LOGGER.info(message);
    }

    private String makeMessage(AccessLogStructure data) throws StorageAccessLogException {
        ObjectNode logNode = JsonHandler.createObjectNode();
        logNode.set("Application", new TextNode(data.getApplicativeContextId()));
        logNode.set("ObjectId", new TextNode(data.getObjectId()));
        logNode.set("ResponseDate", new TextNode(dateFormatter.format(data.getResponseDate())));
        // What is it ? Should it be putted ?
        logNode.set("LinkedArchiveUnitId", new TextNode(data.getArchiveUnitId()));

        ObjectNode detailsNode = JsonHandler.createObjectNode();
        detailsNode.set("AccessContract", new TextNode(data.getAccessContractId()));
        detailsNode.set("XRequestId", new TextNode(data.getxRequestId()));
        detailsNode.set("ObjectType", new TextNode(data.getObjectType()));
        detailsNode.set("ObjectSize", new LongNode(data.getObjectSize()));
        // What is it ? Should it be putted ?
        detailsNode.set("IngestId", new TextNode(data.getIngestId()));

        logNode.set("OtherInformation", detailsNode);

        // No exception thrown, only return a empty JSON "{}" on error
        String dataMessage = JsonHandler.unprettyPrint(logNode);
        if(dataMessage.length() == 2) {
            throw new StorageAccessLogException("Unable to unpretty pring data " + data + ", finalMessage: " + dataMessage);
        }

        return dataMessage;
    }

}
