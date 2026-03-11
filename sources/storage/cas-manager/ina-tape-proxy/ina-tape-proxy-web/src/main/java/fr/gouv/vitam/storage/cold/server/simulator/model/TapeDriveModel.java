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
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDrive;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDriveSpec;

import java.time.LocalDateTime;

/**
 * Combined drive and library status model.
 * Extends {@link TapeDrive} (index + tape) and implements both {@link TapeDriveSpec}
 * carries the full drive state.
 */
public class TapeDriveModel {

    public static final String ID = "_id";
    public static final String INDEX = "index";
    public static final String TAPE_CODE = "tapeCode";
    public static final String POSITION = "position";
    public static final String INITIAL_TAPE_SLOT_NUMBER = "initialTapeSlotNumber";
    public static final String CREATION_DATE = "CreationDate";
    public static final String UPDATE_DATE = "UpdateDate";
    public static final String VERSION_FIELD = "_v";

    @JsonProperty(ID)
    private String id;

    @JsonProperty(INDEX)
    private Integer index;

    @JsonProperty(TAPE_CODE)
    private String tapeCode;

    @JsonProperty(INITIAL_TAPE_SLOT_NUMBER)
    private Integer initialTapeSlotNumber;

    @JsonProperty(POSITION)
    private Integer position;

    @JsonProperty(CREATION_DATE)
    private String creationDate;

    @JsonProperty(UPDATE_DATE)
    private String updateDate;

    @JsonProperty(VERSION_FIELD)
    private long version;

    public TapeDriveModel() {
        this.creationDate = LocalDateTime.now().toString();
        this.updateDate = LocalDateTime.now().toString();
    }

    // ===== Getters / setters =====
    public String getId() {
        return id;
    }

    public TapeDriveModel setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getIndex() {
        return index;
    }

    public TapeDriveModel setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public String getTapeCode() {
        return tapeCode;
    }

    public TapeDriveModel setTapeCode(String tapeCode) {
        this.tapeCode = tapeCode;
        return this;
    }

    public Integer getPosition() {
        return position;
    }

    public TapeDriveModel setPosition(Integer position) {
        this.position = position;
        return this;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public TapeDriveModel setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public TapeDriveModel setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public Integer getInitialTapeSlotNumber() {
        return initialTapeSlotNumber;
    }

    public TapeDriveModel setInitialTapeSlotNumber(Integer initialTapeSlotNumber) {
        this.initialTapeSlotNumber = initialTapeSlotNumber;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public TapeDriveModel setVersion(long version) {
        this.version = version;
        return this;
    }
}
