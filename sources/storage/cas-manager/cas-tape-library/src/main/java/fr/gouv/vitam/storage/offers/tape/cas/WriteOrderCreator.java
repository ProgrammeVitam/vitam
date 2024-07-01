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
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessingException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessor;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;

import java.util.Arrays;

public class WriteOrderCreator extends QueueProcessor<WriteOrder> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteOrderCreator.class);

    private final ArchiveReferentialRepository archiveReferentialRepository;
    private final QueueRepository readWriteQueue;

    public WriteOrderCreator(
        ArchiveReferentialRepository archiveReferentialRepository,
        QueueRepository readWriteQueue
    ) {
        super("WriteOrderCreator");
        this.archiveReferentialRepository = archiveReferentialRepository;
        this.readWriteQueue = readWriteQueue;
    }

    @Override
    protected void processMessage(WriteOrder message) throws QueueProcessingException {
        try {
            sendMessageToQueue(message);
        } catch (Exception ex) {
            throw new QueueProcessingException(
                QueueProcessingException.RetryPolicy.RETRY,
                "Could not process message " + JsonHandler.unprettyPrint(message),
                ex
            );
        }
    }

    public void sendMessageToQueue(WriteOrder message) throws ArchiveReferentialException, QueueException {
        LOGGER.info("Write order generated for tar Id {} [bucket={}]", message.getArchiveId(), message.getBucket());

        // Mark tar archive as "ready"
        this.archiveReferentialRepository.updateLocationToReadyOnDisk(
                message.getArchiveId(),
                message.getSize(),
                message.getDigest()
            );

        // Schedule tar archive for copy on tape
        readWriteQueue.addIfAbsent(
            Arrays.asList(
                new QueryCriteria(WriteOrder.FILE_PATH, message.getFilePath(), QueryCriteriaOperator.EQ),
                new QueryCriteria(WriteOrder.MESSAGE_TYPE, QueueMessageType.WriteOrder.name(), QueryCriteriaOperator.EQ)
            ),
            message
        );
    }
}
