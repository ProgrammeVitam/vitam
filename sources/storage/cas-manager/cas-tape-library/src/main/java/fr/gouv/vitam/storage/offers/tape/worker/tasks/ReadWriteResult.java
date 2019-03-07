package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;

public class ReadWriteResult {
    private TapeCatalog currentTape;

    public TapeCatalog getCurrentTape() {
        return currentTape;
    }

    public void setCurrentTape(TapeCatalog currentTape) {
        this.currentTape = currentTape;
    }
}
