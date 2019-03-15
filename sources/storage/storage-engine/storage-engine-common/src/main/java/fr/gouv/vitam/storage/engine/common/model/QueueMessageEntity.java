package fr.gouv.vitam.storage.engine.common.model;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueueMessageEntity {
    public static final String ID = "_id";
    public static final String STATE = "state";
    public static final String CREATED = "created";
    public static final String PRIORITY = "priority";
    public static final String LAST_UPDATE = "lastUpdate";
    public static final String MESSAGE_TYPE = "messageType";


    @JsonProperty(ID)
    private String id;

    @JsonProperty(STATE)
    private QueueState state = QueueState.READY;

    @JsonProperty(LAST_UPDATE)
    private long lastUpdate = Calendar.getInstance().getTimeInMillis();


    @JsonProperty(CREATED)
    private long created = Calendar.getInstance().getTimeInMillis();

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

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
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
