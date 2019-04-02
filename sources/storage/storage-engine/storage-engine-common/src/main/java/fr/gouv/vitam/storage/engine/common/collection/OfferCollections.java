package fr.gouv.vitam.storage.engine.common.collection;

import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferSequence;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeTarReferentialEntity;

public enum OfferCollections {
    /*
     * Global collections
     */
    OFFER_LOG(OfferLog.class, OfferLog.class.getSimpleName()),
    OFFER_SEQUENCE(OfferSequence.class, OfferSequence.class.getSimpleName()),

    /*
     * Tape storage collections
     */
    TAPE_CATALOG(TapeCatalog.class, "TapeCatalog"),
    TAPE_QUEUE_MESSAGE(QueueMessageEntity.class, "TapeQueueMessage"),
    TAPE_OBJECT_REFERENTIAL(TapeObjectReferentialEntity.class, "TapeObjectReferential"),
    TAPE_TAR_REFERENTIAL(TapeTarReferentialEntity.class, "TapeTarReferential");

    private final Class<?> clazz;
    private String name;
    private String baseName;

    OfferCollections(Class<?> clazz, String baseName) {
        this.clazz = clazz;
        this.baseName = baseName;
        this.name = baseName;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getName() {
        return this.name;
    }

    public void setPrefix(String prefix) { // NOSONAR
        this.name = prefix + this.baseName;
    }
}
