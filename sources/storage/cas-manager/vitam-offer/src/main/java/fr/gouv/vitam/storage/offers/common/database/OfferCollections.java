package fr.gouv.vitam.storage.offers.common.database;

import fr.gouv.vitam.storage.engine.common.model.OfferLog;

public enum OfferCollections {
    OFFER_LOG(OfferLog.class),
    OFFER_SEQUENCE(OfferSequence.class);

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
