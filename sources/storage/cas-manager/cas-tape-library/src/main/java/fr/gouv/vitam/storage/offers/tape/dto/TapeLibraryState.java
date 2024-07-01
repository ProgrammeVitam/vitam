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
package fr.gouv.vitam.storage.offers.tape.dto;

import java.util.ArrayList;
import java.util.List;

public class TapeLibraryState implements TapeLibrarySpec {

    private String device;
    private int driveCount;
    private int slotsCount;
    private int mailBoxCount;

    private List<TapeDrive> drives;
    private List<TapeSlot> slots;

    @Override
    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    @Override
    public int getDriveCount() {
        return driveCount;
    }

    public void setDriveCount(int driveCount) {
        this.driveCount = driveCount;
    }

    @Override
    public int getSlotsCount() {
        return slotsCount;
    }

    public void setSlotsCount(int slotsCount) {
        this.slotsCount = slotsCount;
    }

    @Override
    public int getMailBoxCount() {
        return mailBoxCount;
    }

    public void setMailBoxCount(int mailBoxCount) {
        this.mailBoxCount = mailBoxCount;
    }

    @Override
    public List<TapeDrive> getDrives() {
        return drives;
    }

    public void setDrives(List<TapeDrive> drives) {
        this.drives = drives;
    }

    @Override
    public List<TapeSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<TapeSlot> slots) {
        this.slots = slots;
    }

    public List<TapeDrive> addToDrives(TapeDrive tapeDrive) {
        if (null == drives) {
            drives = new ArrayList<>();
        }
        drives.add(tapeDrive);

        return drives;
    }

    public List<TapeSlot> addToSlots(TapeSlot tapeSlot) {
        if (null == slots) {
            slots = new ArrayList<>();
        }
        slots.add(tapeSlot);
        return slots;
    }
}
