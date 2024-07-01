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
package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TapeArchiveReferentialEntity {

    public static final String ID = "_id";
    public static final String LOCATION = "location";
    public static final String LAST_UPDATE_DATE = "lastUpdateDate";
    public static final String SIZE = "size";
    public static final String DIGEST = "digest";
    public static final String ENTRY_TYPE = "entryTape";

    @JsonProperty(ID)
    private String archiveId;

    @JsonProperty(LOCATION)
    private TapeLibraryArchiveStorageLocation location;

    @JsonProperty(ENTRY_TYPE)
    private EntryType entryTape = EntryType.DATA;

    @JsonProperty(SIZE)
    private Long size;

    @JsonProperty(DIGEST)
    private String digestValue;

    @JsonProperty(LAST_UPDATE_DATE)
    private String lastUpdateDate;

    public TapeArchiveReferentialEntity() {
        // Empty constructor for deserialization
    }

    public TapeArchiveReferentialEntity(
        String archiveId,
        TapeLibraryArchiveStorageLocation location,
        Long size,
        String digestValue,
        String lastUpdateDate
    ) {
        this.archiveId = archiveId;
        this.location = location;
        this.size = size;
        this.digestValue = digestValue;
        this.lastUpdateDate = lastUpdateDate;
    }

    public TapeArchiveReferentialEntity(
        String archiveId,
        TapeLibraryArchiveStorageLocation location,
        EntryType entryTape,
        Long size,
        String digestValue,
        String lastUpdateDate
    ) {
        this.archiveId = archiveId;
        this.location = location;
        this.entryTape = entryTape;
        this.size = size;
        this.digestValue = digestValue;
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getArchiveId() {
        return archiveId;
    }

    public TapeArchiveReferentialEntity setArchiveId(String archiveId) {
        this.archiveId = archiveId;
        return this;
    }

    public TapeLibraryArchiveStorageLocation getLocation() {
        return location;
    }

    public TapeArchiveReferentialEntity setLocation(TapeLibraryArchiveStorageLocation location) {
        this.location = location;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public TapeArchiveReferentialEntity setSize(Long size) {
        this.size = size;
        return this;
    }

    public String getDigestValue() {
        return digestValue;
    }

    public TapeArchiveReferentialEntity setDigestValue(String digestValue) {
        this.digestValue = digestValue;
        return this;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public TapeArchiveReferentialEntity setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
        return this;
    }

    public EntryType getEntryTape() {
        return entryTape;
    }

    public TapeArchiveReferentialEntity setEntryTape(EntryType entryTape) {
        this.entryTape = entryTape;
        return this;
    }
}
