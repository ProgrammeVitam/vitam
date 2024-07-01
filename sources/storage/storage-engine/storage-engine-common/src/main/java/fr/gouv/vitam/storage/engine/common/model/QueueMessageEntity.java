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
package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.LocalDateUtil;

public class QueueMessageEntity {

    public static final String ID = "_id";
    public static final String STATE = "queue_state";
    public static final String PRIORITY = "queue_priority";
    public static final String MESSAGE_TYPE = "queue_message_type";
    public static final String TAG_CREATION_DATE = "queue_creation_date";
    public static final String TAG_LAST_UPDATE = "queue_last-update";

    @JsonProperty(ID)
    private String id;

    @JsonProperty(STATE)
    private QueueState state = QueueState.READY;

    @JsonProperty(TAG_LAST_UPDATE)
    private String lastUpdate = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

    @JsonProperty(TAG_CREATION_DATE)
    private String created = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

    @JsonProperty(MESSAGE_TYPE)
    private QueueMessageType messageType;

    @JsonProperty(PRIORITY)
    private int priority = 1;

    public QueueMessageEntity(String id, QueueMessageType messageType) {
        this.id = id;
        this.messageType = messageType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public QueueState getState() {
        return state;
    }

    public void setState(QueueState state) {
        this.state = state;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public QueueMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(QueueMessageType messageType) {
        this.messageType = messageType;
    }
}
