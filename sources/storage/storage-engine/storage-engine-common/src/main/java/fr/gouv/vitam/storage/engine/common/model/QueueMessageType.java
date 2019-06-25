package fr.gouv.vitam.storage.engine.common.model;

public enum QueueMessageType {

    ReadOrder(ReadOrder.class),
    WriteOrder(WriteOrder.class),
    WriteBackupOrder(WriteOrder.class),
    TapeCatalog(TapeCatalog.class);



    private final Class<? extends QueueMessageEntity> clazz;

    QueueMessageType(Class<? extends QueueMessageEntity> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends QueueMessageEntity> getClazz() {
        return clazz;
    }
}
