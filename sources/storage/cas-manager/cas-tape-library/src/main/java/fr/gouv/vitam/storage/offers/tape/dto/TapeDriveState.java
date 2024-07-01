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

public class TapeDriveState implements TapeDriveSpec {

    private String description;
    private Integer fileNumber;
    private Integer blockNumber;
    private Integer partition;
    private Long tapeBlockSize;
    private String densityCode;
    private String cartridge;
    private Integer errorCountSinceLastStatus;
    private String statusBits;
    private List<TapeDriveStatus> driveStatuses = new ArrayList<>();

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Integer getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(Integer fileNumber) {
        this.fileNumber = fileNumber;
    }

    public Integer getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Integer blockNumber) {
        this.blockNumber = blockNumber;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Long getTapeBlockSize() {
        return tapeBlockSize;
    }

    public void setTapeBlockSize(Long tapeBlockSize) {
        this.tapeBlockSize = tapeBlockSize;
    }

    public String getDensityCode() {
        return densityCode;
    }

    public void setDensityCode(String densityCode) {
        this.densityCode = densityCode;
    }

    @Override
    public String getCartridge() {
        return cartridge;
    }

    public void setCartridge(String cartridge) {
        this.cartridge = cartridge;
    }

    @Override
    public Integer getErrorCountSinceLastStatus() {
        return errorCountSinceLastStatus;
    }

    public void setErrorCountSinceLastStatus(Integer errorCountSinceLastStatus) {
        this.errorCountSinceLastStatus = errorCountSinceLastStatus;
    }

    public String getStatusBits() {
        return statusBits;
    }

    public void setStatusBits(String statusBits) {
        this.statusBits = statusBits;
    }

    @Override
    public List<TapeDriveStatus> getDriveStatuses() {
        return driveStatuses;
    }

    public void setDriveStatuses(List<TapeDriveStatus> driveStatuses) {
        this.driveStatuses = driveStatuses;
    }

    public List<TapeDriveStatus> addToDriveStatuses(TapeDriveStatus driveStatus) {
        driveStatuses.add(driveStatus);

        return this.driveStatuses;
    }
}
