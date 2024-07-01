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
package fr.gouv.vitam.storage.offers.tape.spec;

import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface QueueRepository {
    void add(QueueMessageEntity queueMessageEntity) throws QueueException;

    void addIfAbsent(List<QueryCriteria> criteria, QueueMessageEntity queueMessageEntity) throws QueueException;

    void tryCancelIfNotStarted(List<QueryCriteria> criteria) throws QueueException;

    long remove(String queueMessageId) throws QueueException;

    /**
     * Make QueueMessageEntity COMPLETED
     *
     * @param queueMessageId
     * @return
     * @throws QueueException
     */
    long complete(String queueMessageId) throws QueueException;

    /**
     * Mark queueEntity as Error
     *
     * @param queueMessageId
     * @return
     * @throws QueueException
     */
    long markError(String queueMessageId) throws QueueException;

    /**
     * Mark queueEntity as READY
     *
     * @param queueMessageId
     * @return
     * @throws QueueException
     */
    long markReady(String queueMessageId) throws QueueException;

    long initializeOnBootstrap();

    <T> Optional<T> receive(QueueMessageType messageType) throws QueueException;

    <T> Optional<T> receive(QueueMessageType messageType, boolean usePriority) throws QueueException;

    <T> Optional<T> receive(Bson inQuery, QueueMessageType messageType) throws QueueException;

    <T> Optional<T> receive(Bson inQuery, QueueMessageType messageType, boolean usePriority) throws QueueException;
}
