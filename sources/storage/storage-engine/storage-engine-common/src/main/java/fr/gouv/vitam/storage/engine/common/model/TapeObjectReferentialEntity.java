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
import fr.gouv.vitam.common.ParametersChecker;

public class TapeObjectReferentialEntity {

    public static final String ID = "_id";
    public static final String SIZE = "size";
    public static final String DIGEST_TYPE = "digestType";
    public static final String DIGEST = "digest";
    public static final String STORAGE_ID = "storageId";
    public static final String LOCATION = "location";
    public static final String LAST_OBJECT_MODIFIED_DATE = "lastObjectModifiedDate";
    public static final String LAST_UPDATE_DATE = "lastUpdateDate";

    @JsonProperty(ID)
    private TapeLibraryObjectReferentialId id;

    @JsonProperty(SIZE)
    private long size;

    @JsonProperty(DIGEST_TYPE)
    private String digestType;

    @JsonProperty(DIGEST)
    private String digest;

    @JsonProperty(STORAGE_ID)
    private String storageId;

    @JsonProperty(LOCATION)
    private TapeLibraryObjectStorageLocation location;

    @JsonProperty(LAST_OBJECT_MODIFIED_DATE)
    private String lastObjectModifiedDate;

    @JsonProperty(LAST_UPDATE_DATE)
    private String lastUpdateDate;

    public TapeObjectReferentialEntity() {
        // Empty constructor for deserialization
    }

    public TapeObjectReferentialEntity(
        TapeLibraryObjectReferentialId id,
        long size,
        String digestType,
        String digest,
        String storageId,
        TapeLibraryObjectStorageLocation location,
        String lastObjectModifiedDate,
        String lastUpdateDate
    ) {
        ParametersChecker.checkParameter("Id is required", id);
        this.id = id;
        this.size = size;
        this.digestType = digestType;
        this.digest = digest;
        this.storageId = storageId;
        this.location = location;
        this.lastObjectModifiedDate = lastObjectModifiedDate;
        this.lastUpdateDate = lastUpdateDate;
    }

    public TapeLibraryObjectReferentialId getId() {
        return id;
    }

    public TapeObjectReferentialEntity setId(TapeLibraryObjectReferentialId id) {
        this.id = id;
        return this;
    }

    public long getSize() {
        return size;
    }

    public TapeObjectReferentialEntity setSize(long size) {
        this.size = size;
        return this;
    }

    public String getDigestType() {
        return digestType;
    }

    public TapeObjectReferentialEntity setDigestType(String digestType) {
        this.digestType = digestType;
        return this;
    }

    public String getDigest() {
        return digest;
    }

    public TapeObjectReferentialEntity setDigest(String digest) {
        this.digest = digest;
        return this;
    }

    public String getStorageId() {
        return storageId;
    }

    public TapeObjectReferentialEntity setStorageId(String storageId) {
        this.storageId = storageId;
        return this;
    }

    public TapeLibraryObjectStorageLocation getLocation() {
        return location;
    }

    public TapeObjectReferentialEntity setLocation(TapeLibraryObjectStorageLocation location) {
        this.location = location;
        return this;
    }

    public String getLastObjectModifiedDate() {
        return lastObjectModifiedDate;
    }

    public TapeObjectReferentialEntity setLastObjectModifiedDate(String lastObjectModifiedDate) {
        this.lastObjectModifiedDate = lastObjectModifiedDate;
        return this;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public TapeObjectReferentialEntity setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
        return this;
    }
}
