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
package fr.gouv.vitam.storage.offers.tape.parser;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.storage.offers.tape.dto.TapeCartridge;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlotType;
import org.apache.commons.lang3.StringUtils;

public class TapeLibraryStatusParser {

    private static final String DRIVE_VOLUME_TAG = ":VolumeTag = ";
    private static final String SLOT_VOLUME_TAG = ":VolumeTag=";
    private static final String DRIVE_ALTERNATE_VOLUME_TAG = ":AlternateVolumeTag = ";
    private static final String SLOT_ALTERNATE_VOLUME_TAG = ":AlternateVolumeTag=";
    private static final String STORAGE_CHANGER = "Storage Changer";
    private static final String SEPARATOR = ":";
    private static final String DRIVES = "Drives,";
    private static final String SLOTS = "Slots ( ";
    private static final String IMPORT_EXPORT = "Import/Export";
    private static final String IMPORT_EXPORT_TAG = " IMPORT/EXPORT";
    private static final String FULL_UNKNOWN_STORAGE_ELEMENT_LOADED = "Full (Unknown Storage Element Loaded)";
    private static final String STORAGE_ELEMENT = "Storage Element";
    private static final String LOADED = "Loaded";
    private static final String EMPTY = ":Empty";
    private static final String DATA_TRANSFER_ELEMENT = "Data Transfer Element";

    public TapeLibraryState parse(String output) {
        ParametersChecker.checkParameter("Output param is required", output);

        final TapeLibraryState tapeLibraryState = new TapeLibraryState();
        for (String s : output.split("\n")) {
            // Slots
            if (s.trim().startsWith(STORAGE_ELEMENT)) {
                TapeSlot tapeSlot = new TapeSlot();
                tapeLibraryState.addToSlots(tapeSlot);

                extractSlotIndexAndType(s, tapeSlot);

                if (s.contains(EMPTY)) {
                    continue;
                }

                TapeCartridge cartridge = new TapeCartridge();
                tapeSlot.setTape(cartridge);

                extractSlotVolumeTag(s, cartridge);
                // Drives
            } else if (s.trim().startsWith(DATA_TRANSFER_ELEMENT)) {
                TapeDrive tapeDrive = new TapeDrive();
                tapeLibraryState.addToDrives(tapeDrive);
                extractDriveIndex(s, tapeDrive);

                if (s.contains(EMPTY)) {
                    continue;
                }

                TapeCartridge cartridge = new TapeCartridge();
                tapeDrive.setTape(cartridge);

                extractCartridgeSlotIndex(s, cartridge);

                extractDriveVolumeTag(s, cartridge);
            } else if (s.trim().startsWith(STORAGE_CHANGER)) {
                extractHeader(tapeLibraryState, s);
            }
        }

        return tapeLibraryState;
    }

    private void extractCartridgeSlotIndex(String s, TapeCartridge cartridge) {
        if (!s.contains(FULL_UNKNOWN_STORAGE_ELEMENT_LOADED)) {
            String slotIndex = StringUtils.substringBetween(s, STORAGE_ELEMENT, LOADED);
            cartridge.setSlotIndex(Integer.valueOf(slotIndex.trim()));
        }
    }

    private void extractSlotIndexAndType(String s, TapeSlot tapeSlot) {
        String slotIndex;

        if (s.contains(IMPORT_EXPORT_TAG)) {
            slotIndex = StringUtils.substringBetween(s, STORAGE_ELEMENT, IMPORT_EXPORT_TAG);
            tapeSlot.setStorageElementType(TapeSlotType.IMPORTEXPORT);
        } else {
            slotIndex = StringUtils.substringBetween(s, STORAGE_ELEMENT, SEPARATOR);
            tapeSlot.setStorageElementType(TapeSlotType.SLOT);
        }

        tapeSlot.setIndex(Integer.valueOf(slotIndex.trim()));
    }

    private void extractDriveIndex(String s, TapeDrive tapeDrive) {
        String driveIndex = StringUtils.substringBetween(s, DATA_TRANSFER_ELEMENT, ":");
        tapeDrive.setIndex(Integer.valueOf(driveIndex.trim()));
    }

    private void extractHeader(TapeLibraryState tapeLibraryState, String s) {
        String device = StringUtils.substringBetween(s, STORAGE_CHANGER, SEPARATOR);
        String driveCount = StringUtils.substringBetween(s, SEPARATOR, DRIVES);
        String slotsCount = StringUtils.substringBetween(s, DRIVES, SLOTS);
        String mailBoxCount = StringUtils.substringBetween(s, SLOTS, IMPORT_EXPORT);

        tapeLibraryState.setDevice(device.trim());
        tapeLibraryState.setDriveCount(Integer.parseInt(driveCount.trim()));
        tapeLibraryState.setSlotsCount(Integer.parseInt(slotsCount.trim()));
        tapeLibraryState.setMailBoxCount(Integer.parseInt(mailBoxCount.trim()));
    }

    private void extractDriveVolumeTag(String s, TapeCartridge cartridge) {
        if (s.contains(DRIVE_VOLUME_TAG) && s.contains(DRIVE_ALTERNATE_VOLUME_TAG)) {
            cartridge.setVolumeTag(
                StringUtils.substringBetween(s, DRIVE_VOLUME_TAG, DRIVE_ALTERNATE_VOLUME_TAG).trim()
            );
            cartridge.setAlternateVolumeTag(StringUtils.substringAfterLast(s, DRIVE_ALTERNATE_VOLUME_TAG).trim());
        } else if (s.contains(DRIVE_VOLUME_TAG)) {
            cartridge.setVolumeTag(StringUtils.substringAfterLast(s, DRIVE_VOLUME_TAG).trim());
        }
    }

    private void extractSlotVolumeTag(String s, TapeCartridge cartridge) {
        if (s.contains(SLOT_VOLUME_TAG) && s.contains(SLOT_ALTERNATE_VOLUME_TAG)) {
            cartridge.setVolumeTag(StringUtils.substringBetween(s, SLOT_VOLUME_TAG, SLOT_ALTERNATE_VOLUME_TAG).trim());
            cartridge.setAlternateVolumeTag(StringUtils.substringAfterLast(s, SLOT_ALTERNATE_VOLUME_TAG).trim());
        } else if (s.contains(SLOT_VOLUME_TAG)) {
            cartridge.setVolumeTag(StringUtils.substringAfterLast(s, SLOT_VOLUME_TAG).trim());
        }
    }
}
