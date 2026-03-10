/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.server.simulator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * MongoDB Model for Tape Catalog
 * Stores information about tape cartridges and their content
 */
public class TapeCatalogModel {

    public static final String ID = "_id";
    public static final String DRIVE_INDEX = "driveIndex";
    public static final String SLOT_NUMBER = "slotNumber";
    public static final String TOTAL_STORED_FILES = "totalStoredFiles";
    public static final String IS_FULL = "isFull";
    public static final String TOTAL_SIZE_USED = "totalSizeUsed";
    public static final String CREATION_DATE = "CreationDate";
    public static final String UPDATE_DATE = "UpdateDate";
    public static final String VERSION_FIELD = "_v";

    @JsonProperty(ID)
    private String id;

    @JsonProperty(DRIVE_INDEX)
    private Integer driveIndex;

    @JsonProperty(SLOT_NUMBER)
    private Integer slotNumber;

    @JsonProperty(TOTAL_STORED_FILES)
    private int totalStoredFiles;

    @JsonProperty(IS_FULL)
    private boolean isFull;

    @JsonProperty(TOTAL_SIZE_USED)
    private Long totalSizeUsed;

    @JsonProperty(CREATION_DATE)
    private String creationDate;

    @JsonProperty(UPDATE_DATE)
    private String updateDate;

    @JsonProperty(VERSION_FIELD)
    private long version;

    public TapeCatalogModel() {
        this.creationDate = LocalDateTime.now().toString();
        this.updateDate = LocalDateTime.now().toString();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public TapeCatalogModel setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getDriveIndex() {
        return driveIndex;
    }

    public TapeCatalogModel setDriveIndex(Integer driveIndex) {
        this.driveIndex = driveIndex;
        return this;
    }

    public Integer getSlotNumber() {
        return slotNumber;
    }

    public TapeCatalogModel setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
        return this;
    }

    public int getTotalStoredFiles() {
        return totalStoredFiles;
    }

    public TapeCatalogModel setTotalStoredFiles(int totalStoredFiles) {
        this.totalStoredFiles = totalStoredFiles;
        return this;
    }

    public boolean isFull() {
        return isFull;
    }

    public TapeCatalogModel setFull(boolean full) {
        isFull = full;
        return this;
    }

    public Long getTotalSizeUsed() {
        return totalSizeUsed;
    }

    public TapeCatalogModel setTotalSizeUsed(Long totalSizeUsed) {
        this.totalSizeUsed = totalSizeUsed;
        return this;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public TapeCatalogModel setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public TapeCatalogModel setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public TapeCatalogModel setVersion(long version) {
        this.version = version;
        return this;
    }
}
