package fr.gouv.vitam.storage.engine.common.collection;

import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferSequence;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;

public enum OfferCollections {
    OFFER_LOG(OfferLog.class),
    OFFER_SEQUENCE(OfferSequence.class),
    OFFER_TAPE_CATALOG(TapeCatalog.class);

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
