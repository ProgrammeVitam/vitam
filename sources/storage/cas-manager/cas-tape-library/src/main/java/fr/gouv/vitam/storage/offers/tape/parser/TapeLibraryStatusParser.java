package fr.gouv.vitam.storage.offers.tape.parser;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.storage.offers.tape.dto.TapeCartridge;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlotType;
import org.apache.commons.lang3.StringUtils;

public class TapeLibraryStatusParser {
    public static final String DRIVE_VOLUME_TAG = ":VolumeTag = ";
    public static final String SLOT_VOLUME_TAG = ":VolumeTag=";
    public static final String DRIVE_ALTERNATE_VOLUME_TAG = ":AlternateVolumeTag = ";
    public static final String SLOT_ALTERNATE_VOLUME_TAG = ":AlternateVolumeTag=";
    public static final String STORAGE_CHANGER = "Storage Changer";
    public static final String SEPARATOR = ":";
    public static final String DRIVES = "Drives,";
    public static final String SLOTS = "Slots ( ";
    public static final String IMPORT_EXPORT = "Import/Export";
    public static final String IMPORT_EXPORT_TAG = " IMPORT/EXPORT";
    public static final String FULL_UNKNOWN_STORAGE_ELEMENT_LOADED = "Full (Unknown Storage Element Loaded)";
    public static final String STORAGE_ELEMENT = "Storage Element";
    public static final String LOADED = "Loaded";
    public static final String EMPTY = ":Empty";
    public static final String DATA_TRANSFER_ELEMENT = "Data Transfer Element";


    public TapeLibraryState parse(String output) {
        ParametersChecker.checkParameter("Output param is required", output);

        final TapeLibraryState tapeLibraryState = new TapeLibraryState(StatusCode.OK);
        for (String s : output.split("\\|")) {

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
        tapeLibraryState.setDriveCount(Integer.valueOf(driveCount.trim()));
        tapeLibraryState.setSlotsCount(Integer.valueOf(slotsCount.trim()));
        tapeLibraryState.setMailBoxCount(Integer.valueOf(mailBoxCount.trim()));
    }

    private void extractDriveVolumeTag(String s, TapeCartridge cartridge) {
        if (s.contains(DRIVE_VOLUME_TAG) && s.contains(DRIVE_ALTERNATE_VOLUME_TAG)) {
            cartridge.setVolumeTag(StringUtils.substringBetween(s, DRIVE_VOLUME_TAG, DRIVE_ALTERNATE_VOLUME_TAG));
            cartridge.setAlternateVolumeTag(StringUtils.substringAfterLast(s, DRIVE_ALTERNATE_VOLUME_TAG));
        } else if (s.contains(DRIVE_VOLUME_TAG)) {
            cartridge.setVolumeTag(StringUtils.substringAfterLast(s, DRIVE_VOLUME_TAG));
        }
    }

    private void extractSlotVolumeTag(String s, TapeCartridge cartridge) {
        if (s.contains(SLOT_VOLUME_TAG) && s.contains(SLOT_ALTERNATE_VOLUME_TAG)) {
            cartridge.setVolumeTag(StringUtils.substringBetween(s, SLOT_VOLUME_TAG, SLOT_ALTERNATE_VOLUME_TAG));
            cartridge.setAlternateVolumeTag(StringUtils.substringAfterLast(s, SLOT_ALTERNATE_VOLUME_TAG));
        } else if (s.contains(SLOT_VOLUME_TAG)) {
            cartridge.setVolumeTag(StringUtils.substringAfterLast(s, SLOT_VOLUME_TAG));
        }
    }
}
