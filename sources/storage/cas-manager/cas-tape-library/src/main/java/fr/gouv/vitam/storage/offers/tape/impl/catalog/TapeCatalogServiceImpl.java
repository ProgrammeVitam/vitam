/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.tape.impl.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;

public class TapeCatalogServiceImpl implements TapeCatalogService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeCatalogServiceImpl.class);

    private TapeCatalogRepository repository;

    public TapeCatalogServiceImpl(MongoDbAccess mongoDbAccess) {
        this.repository = new TapeCatalogRepository(mongoDbAccess.getMongoDatabase()
                .getCollection(OfferCollections.OFFER_TAPE_CATALOG.getName()));
    }

    @Override
    public void create(TapeCatalog tapeCatalog) throws InvalidParseOperationException {
        tapeCatalog.setId(GUIDFactory.newGUID().toString());
        repository.createTape(tapeCatalog);
    }

    @Override
    public boolean replace(TapeCatalog tapeCatalog) throws InvalidParseOperationException {
        return repository.replaceTape(tapeCatalog);
    }

    @Override
    public boolean update(String tapeId, Map<String, Object> criteria) {
        return repository.updateTape(tapeId, criteria);
    }

    @Override
    public boolean init(String tapeLibraryIdentifier, TapeLibraryState libraryState) {
        QueryCriteria criteria = new QueryCriteria(TapeCatalog.LIBRARY, tapeLibraryIdentifier, QueryCriteriaOperator.EQ);
        Map<String, TapeCatalog> existingTapes;

        try {
            existingTapes = repository.findTapes(Arrays.asList(criteria)).stream().collect(Collectors.toMap(TapeCatalog::getCode, tape -> tape));
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }

        for (TapeDrive drive : libraryState.getDrives()) {
            if (drive.getTape() != null) {
                TapeCatalog tape = new TapeCatalog();
                tape.setCode(drive.getTape().getVolumeTag());
                tape.setAlternativeCode(drive.getTape().getAlternateVolumeTag());
                tape.setLibrary(tapeLibraryIdentifier);
                tape.setCurrentLocation(new TapeLocation(drive.getIndex(), TapeLocationType.DIRVE));
                if (drive.getTape().getSlotIndex() != null) {
                    tape.setPreviousLocation(new TapeLocation(drive.getTape().getSlotIndex(), TapeLocationType.SLOT));
                }

                createOrUpdateTape(tape, existingTapes.get(tape.getCode()));
            }
        }

        for (TapeSlot slot : libraryState.getSlots()) {
            if (slot.getTape() != null) {
                TapeCatalog tape = new TapeCatalog();
                tape.setCode(slot.getTape().getVolumeTag());
                tape.setAlternativeCode(slot.getTape().getAlternateVolumeTag());
                tape.setLibrary(tapeLibraryIdentifier);
                tape.setCurrentLocation(new TapeLocation(slot.getIndex(), TapeLocationType.SLOT));

                createOrUpdateTape(tape, existingTapes.get(tape.getCode()));
            }
        }

        return true;
    }

    private void createOrUpdateTape(TapeCatalog tape, TapeCatalog existingTape) {
        if (existingTape != null) {
            Map<String, Object> updates = getUpdates(tape, existingTape);

            if (!updates.isEmpty()) {
                boolean isUpdated = repository.updateTape(existingTape.getId(), updates);
                if (!isUpdated) {
                    String errorMsg = String.format("Error when updating tape %s", tape.getCode());
                    LOGGER.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            }

        } else {
            try {
                repository.createTape(tape);
            } catch (InvalidParseOperationException e) {
                LOGGER.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    private Map<String, Object> getUpdates(TapeCatalog tape, TapeCatalog existingTape) {
        Map<String, Object> updates;updates = new HashMap<>();
        if (!existingTape.getAlternativeCode().equals(tape.getAlternativeCode())) {
            updates.put(TapeCatalog.ALTERNATIVE_CODE, tape.getAlternativeCode());
        }

        if (!existingTape.getLibrary().equals(tape.getLibrary())) {
            updates.put(TapeCatalog.LIBRARY, tape.getLibrary());
        }

        if (!existingTape.getCurrentLocation().equals(tape.getCurrentLocation())) {
            updates.put(TapeCatalog.CURRENT_LOCATION, tape.getAlternativeCode());
        }

        if (tape.getPreviousLocation() != null
                && (existingTape.getPreviousLocation() == null
                        || !existingTape.getPreviousLocation().equals(tape.getPreviousLocation()))) {
            updates.put(TapeCatalog.PREVIOUS_LOCATION, tape.getPreviousLocation());
        } else if(existingTape.getPreviousLocation() != null) {
            updates.put(TapeCatalog.PREVIOUS_LOCATION, tape.getPreviousLocation());
        }

        return updates;
    }

    @Override
    public TapeCatalog findById(String tapeId) throws InvalidParseOperationException {
        return repository.findTapeById(tapeId);
    }

    @Override
    public List<TapeCatalog> find(List<QueryCriteria> criteria) throws InvalidParseOperationException {
        return repository.findTapes(criteria);
    }
}
