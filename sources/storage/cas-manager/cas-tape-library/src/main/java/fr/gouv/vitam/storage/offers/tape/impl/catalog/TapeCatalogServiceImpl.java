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
package fr.gouv.vitam.storage.offers.tape.impl.catalog;

import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TapeCatalogServiceImpl implements TapeCatalogService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeCatalogServiceImpl.class);

    private final TapeCatalogRepository tapeCatalogRepository;

    public TapeCatalogServiceImpl(TapeCatalogRepository tapeCatalogRepository) {
        this.tapeCatalogRepository = tapeCatalogRepository;
    }

    @Override
    public void create(TapeCatalog tapeCatalog) throws TapeCatalogException {
        tapeCatalog.setId(GUIDFactory.newGUID().toString());
        tapeCatalogRepository.createTape(tapeCatalog);
    }

    @Override
    public boolean replace(TapeCatalog tapeCatalog) throws TapeCatalogException {
        return tapeCatalogRepository.replaceTape(tapeCatalog);
    }

    @Override
    public boolean update(String tapeId, Map<String, Object> criteria) throws TapeCatalogException {
        return tapeCatalogRepository.updateTape(tapeId, criteria);
    }

    @Override
    public Map<Integer, TapeCatalog> init(String tapeLibraryIdentifier, TapeLibrarySpec libraryState)
        throws TapeCatalogException {
        QueryCriteria criteria = new QueryCriteria(
            TapeCatalog.LIBRARY,
            tapeLibraryIdentifier,
            QueryCriteriaOperator.EQ
        );
        Map<String, TapeCatalog> existingTapes = tapeCatalogRepository
            .findTapes(List.of(criteria))
            .stream()
            .collect(Collectors.toMap(TapeCatalog::getCode, tape -> tape));

        Map<Integer, TapeCatalog> driveTape = new HashMap<>();
        for (TapeDrive drive : libraryState.getDrives()) {
            if (drive.getTape() != null) {
                TapeCatalog tape = new TapeCatalog();
                tape.setCode(drive.getTape().getVolumeTag());
                tape.setAlternativeCode(drive.getTape().getAlternateVolumeTag());
                tape.setLibrary(tapeLibraryIdentifier);

                TapeCatalog existingTape = existingTapes.remove(tape.getCode());

                TapeLocationType slotLocation = drive.getTape().getSlotIndex() <= libraryState.getSlotsCount()
                    ? TapeLocationType.SLOT
                    : TapeLocationType.IMPORTEXPORT;

                createOrUpdateTape(
                    tape,
                    existingTape,
                    new TapeLocation(drive.getTape().getSlotIndex(), slotLocation),
                    new TapeLocation(drive.getIndex(), TapeLocationType.DRIVE)
                );

                String tapeGuid = tape.getId();
                if (null != existingTape) {
                    tapeGuid = existingTape.getId();
                }
                driveTape.put(drive.getIndex(), findById(tapeGuid));
            }
        }

        for (TapeSlot slot : libraryState.getSlots()) {
            if (slot.getTape() != null) {
                TapeCatalog tape = new TapeCatalog();
                tape.setCode(slot.getTape().getVolumeTag());
                tape.setAlternativeCode(slot.getTape().getAlternateVolumeTag());
                tape.setLibrary(tapeLibraryIdentifier);

                TapeCatalog existingTape = existingTapes.remove(tape.getCode());
                TapeLocation tapeLocationInit = new TapeLocation(
                    slot.getIndex(),
                    slot.getStorageElementType().getTapeLocationType()
                );

                if (null == existingTape) {
                    LOGGER.info(
                        "A new tape (" +
                        slot.getTape().getVolumeTag() +
                        ") found in " +
                        slot.getStorageElementType().getTapeLocationType() +
                        " slot " +
                        slot.getIndex() +
                        ". It will be added to catalog"
                    );
                } else if (tapeLocationInit.equals(existingTape.getCurrentLocation())) {
                    LOGGER.info(
                        "Tape (" +
                        slot.getTape().getVolumeTag() +
                        ") has found in " +
                        slot.getStorageElementType().getTapeLocationType() +
                        " slot " +
                        slot.getIndex() +
                        ". Tape location matches last known location."
                    );
                } else {
                    LOGGER.warn(
                        "Tape (" +
                        existingTape.getCode() +
                        ") location changed. Catalog location : " +
                        JsonHandler.unprettyPrint(existingTape.getCurrentLocation()) +
                        " Robot status command location: " +
                        JsonHandler.unprettyPrint(tapeLocationInit) +
                        ". This tape may have conflict!"
                    );
                }

                createOrUpdateTape(tape, existingTape, tapeLocationInit, tapeLocationInit);
            }
        }

        existingTapes
            .values()
            .forEach(tape -> {
                LOGGER.warn(
                    "Tape (" +
                    tape.getCode() +
                    ") with  NOT FOUND in tape library ! " +
                    "Last catalog location : " +
                    JsonHandler.unprettyPrint(tape.getCurrentLocation()) +
                    ", last tape state : " +
                    tape.getTapeState()
                );
                tape.setCurrentLocation(new TapeLocation(-1, TapeLocationType.OUTSIDE));
                // Conflict because the tape maybe altered. Audit should fix this state and update state of the tape.
                // FIXME: 16/05/19 check state conflict when audit implemented
                tape.setTapeState(TapeState.CONFLICT);

                try {
                    tapeCatalogRepository.replaceTape(tape);
                } catch (TapeCatalogException e) {
                    String errorMsg = String.format("Error while updating tape %s", tape.getCode());
                    throw new RuntimeException(errorMsg, e);
                }
            });

        return driveTape;
    }

    private void createOrUpdateTape(
        TapeCatalog tape,
        TapeCatalog existingTape,
        TapeLocation previousLocation,
        TapeLocation currentLocation
    ) throws TapeCatalogException {
        if (existingTape != null) {
            Map<String, Object> updates = merge(tape, existingTape, previousLocation, currentLocation);

            if (!updates.isEmpty()) {
                boolean isUpdated = tapeCatalogRepository.updateTape(existingTape.getId(), updates);
                if (!isUpdated) {
                    String errorMsg = String.format("Error when updating tape %s", tape.getCode());
                    LOGGER.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            }
        } else {
            tape.setCurrentLocation(currentLocation);
            tape.setPreviousLocation(previousLocation);

            // Mark tape as running, because tape is already in drive
            if (Objects.equals(currentLocation.getLocationType(), TapeLocationType.DRIVE)) {
                tape.setState(QueueState.RUNNING);
            }

            tapeCatalogRepository.createTape(tape);
        }
    }

    private Map<String, Object> merge(
        TapeCatalog tape,
        TapeCatalog existingTape,
        TapeLocation previousLocation,
        TapeLocation currentLocation
    ) {
        Map<String, Object> updates;
        updates = new HashMap<>();

        tape.setId(existingTape.getId());

        if (!Objects.equals(existingTape.getAlternativeCode(), tape.getAlternativeCode())) {
            updates.put(TapeCatalog.ALTERNATIVE_CODE, tape.getAlternativeCode());
        }

        if (!Objects.equals(existingTape.getLibrary(), tape.getLibrary())) {
            updates.put(TapeCatalog.LIBRARY, tape.getLibrary());
        }

        if (!Objects.equals(existingTape.getPreviousLocation(), previousLocation)) {
            updates.put(TapeCatalog.PREVIOUS_LOCATION, previousLocation);
        }

        // Update current location and tape catalog queue state
        if (!Objects.equals(existingTape.getCurrentLocation(), currentLocation)) {
            TapeLocationType locationTypeInDB = existingTape.getCurrentLocation().getLocationType();
            TapeLocationType locationTypeInit = currentLocation.getLocationType();
            switch (locationTypeInit) {
                case DRIVE:
                    // Tape is in drive but catalog says that it is not (it is in slot).
                    if (Objects.equals(locationTypeInDB, TapeLocationType.SLOT)) {
                        updates.put(QueueMessageEntity.STATE, QueueState.RUNNING.getState());
                    }

                    updates.put(TapeCatalog.CURRENT_LOCATION, currentLocation);

                    break;
                case SLOT:
                case IMPORTEXPORT:
                    // Tape is in slot but catalog says that it is not (it is in drive).
                    if (Objects.equals(locationTypeInDB, TapeLocationType.DRIVE)) {
                        if (Objects.equals(existingTape.getState(), QueueState.RUNNING)) {
                            updates.put(QueueMessageEntity.STATE, QueueState.READY.getState());
                        }
                    }

                    updates.put(TapeCatalog.CURRENT_LOCATION, currentLocation);

                    break;
                case OUTSIDE:
                default:
                    throw new IllegalStateException(
                        "Unknown or robot status command should not return, such tapeLocationType :" + locationTypeInit
                    );
            }

            updates.put(TapeCatalog.CURRENT_LOCATION, currentLocation);
        }

        return updates;
    }

    @Override
    public TapeCatalog findById(String tapeId) throws TapeCatalogException {
        return tapeCatalogRepository.findTapeById(tapeId);
    }

    @Override
    public List<TapeCatalog> find(List<QueryCriteria> criteria) throws TapeCatalogException {
        return tapeCatalogRepository.findTapes(criteria);
    }

    @Override
    public void markReady(String queueId) throws QueueException {
        tapeCatalogRepository.markReady(queueId);
    }

    @Override
    public Optional<TapeCatalog> receive(Bson inQuery) throws QueueException {
        return tapeCatalogRepository.receive(inQuery, QueueMessageType.TapeCatalog);
    }
}
