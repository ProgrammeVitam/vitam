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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.guid.GUIDFactory;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * TapeCatalog
 */

@JsonInclude(NON_NULL)
public class TapeCatalog extends QueueMessageEntity {

    public static final String CODE = "code";
    public static final String ALTERNATIVE_CODE = "alternative_code";
    public static final String BUCKET = "bucket";
    public static final String LABEL = "label";
    public static final String LIBRARY = "library";
    public static final String TYPE = "type";
    public static final String CAPACITY = "capacity";
    public static final String FILE_COUNT = "file_count";
    public static final String CURRENT_LOCATION = "current_location";
    public static final String PREVIOUS_LOCATION = "previous_location";
    public static final String COMPRESSED = "compressed";
    public static final String WORM = "worm";
    public static final String VERSION = "_v";
    public static final String WRITTEN_BYTES = "written_bytes";
    public static final String TAPE_STATE = "tape_state";

    @JsonProperty(CODE)
    private String code;

    @JsonProperty(ALTERNATIVE_CODE)
    private String alternativeCode;

    @JsonProperty(BUCKET)
    private String bucket;

    @JsonProperty(LABEL)
    private TapeCatalogLabel label;

    @JsonProperty(LIBRARY)
    private String library;

    @JsonProperty(TYPE)
    private String type;

    @JsonProperty(WRITTEN_BYTES)
    private Long writtenBytes = 0l;

    @JsonProperty(TAPE_STATE)
    private TapeState tapeState = TapeState.EMPTY;

    @JsonProperty(CAPACITY)
    private Long capacity;

    @JsonProperty(FILE_COUNT)
    private Integer fileCount = 0;

    @JsonProperty(CURRENT_LOCATION)
    private TapeLocation currentLocation;

    @JsonProperty(PREVIOUS_LOCATION)
    private TapeLocation previousLocation;

    @JsonProperty(COMPRESSED)
    private boolean compressed;

    @JsonProperty(WORM)
    private boolean worm;

    @JsonProperty(VERSION)
    private int version;

    @JsonIgnore
    private Integer currentPosition = 0;

    public TapeCatalog() {
        super(GUIDFactory.newGUID().getId(), QueueMessageType.TapeCatalog);
    }

    public String getCode() {
        return code;
    }

    public TapeCatalog setCode(String code) {
        this.code = code;
        return this;
    }

    public TapeCatalogLabel getLabel() {
        return label;
    }

    public TapeCatalog setLabel(TapeCatalogLabel label) {
        this.label = label;
        return this;
    }

    public String getLibrary() {
        return library;
    }

    public TapeCatalog setLibrary(String library) {
        this.library = library;
        return this;
    }

    public String getType() {
        return type;
    }

    public TapeCatalog setType(String type) {
        this.type = type;
        return this;
    }

    public Long getWrittenBytes() {
        return writtenBytes;
    }

    public TapeCatalog setWrittenBytes(Long writtenBytes) {
        this.writtenBytes = writtenBytes;
        return this;
    }

    public TapeState getTapeState() {
        return tapeState;
    }

    public TapeCatalog setTapeState(TapeState tapeState) {
        this.tapeState = tapeState;
        return this;
    }

    public Long getCapacity() {
        return capacity;
    }

    public TapeCatalog setCapacity(Long capacity) {
        this.capacity = capacity;
        return this;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public TapeCatalog setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
        return this;
    }

    public TapeLocation getCurrentLocation() {
        return currentLocation;
    }

    public TapeCatalog setCurrentLocation(TapeLocation currentLocation) {
        this.currentLocation = currentLocation;
        return this;
    }

    public TapeLocation getPreviousLocation() {
        return previousLocation;
    }

    public TapeCatalog setPreviousLocation(TapeLocation previousLocation) {
        this.previousLocation = previousLocation;
        return this;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public TapeCatalog setCompressed(boolean compressed) {
        this.compressed = compressed;
        return this;
    }

    public boolean isWorm() {
        return worm;
    }

    public TapeCatalog setWorm(boolean worm) {
        this.worm = worm;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public TapeCatalog setVersion(int version) {
        this.version = version;
        return this;
    }

    public String getAlternativeCode() {
        return alternativeCode;
    }

    public TapeCatalog setAlternativeCode(String alternativeCode) {
        this.alternativeCode = alternativeCode;
        return this;
    }

    public String getBucket() {
        return bucket;
    }

    public TapeCatalog setBucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public Integer getCurrentPosition() {
        return currentPosition;
    }

    public TapeCatalog setCurrentPosition(Integer currentPosition) {
        this.currentPosition = currentPosition;
        return this;
    }
}
