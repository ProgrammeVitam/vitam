package fr.gouv.vitam.storage.engine.common.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.LocalDateUtil;

public class QueueMessageEntity {
    public static final String ID = "_id";
    public static final String STATE = "state";
    public static final String PRIORITY = "priority";
    public static final String MESSAGE_TYPE = "messageType";
    public static final String TAG_CREATION_DATE = "creationDate";
    public static final String TAG_LAST_UPDATE = "lastUpdate";

    @JsonProperty(ID)
    private String id;

    @JsonProperty(STATE)
    private QueueState state = QueueState.READY;

    @JsonProperty(TAG_LAST_UPDATE)
    private String lastUpdate = LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now());


    @JsonProperty(TAG_CREATION_DATE)
    private String created = LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now());

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
