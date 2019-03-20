package fr.gouv.vitam.storage.engine.common.collection;

import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferSequence;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarReferentialEntity;

public enum OfferCollections {
    OFFER_LOG(OfferLog.class),
    OFFER_SEQUENCE(OfferSequence.class),
    OFFER_TAPE_CATALOG(TapeCatalog.class),
    OFFER_QUEUE(QueueMessageEntity.class),
    OFFER_OBJECT_REFERENTIAL(TapeLibraryObjectReferentialEntity.class),
    OFFER_TAR_REFERENTIAL(TapeLibraryTarReferentialEntity.class);

    private final Class<?> clazz;
    private String name;

    OfferCollections(Class<?> clazz) {
        this.clazz = clazz;
        this.name = clazz.getSimpleName();
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public void setPrefix(String prefix) { // NOSONAR
        this.name = prefix + getClazz().getSimpleName();
    }
}
