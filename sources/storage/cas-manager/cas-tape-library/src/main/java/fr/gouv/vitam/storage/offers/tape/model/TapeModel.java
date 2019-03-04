/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.tape.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLocation;

/**
 * TapeModel
 */

@JsonInclude(NON_NULL)
public class TapeModel {
    public static final String ID = "_id";
    public static final String CODE = "code";
    public static final String LABEL = "label";
    public static final String LIBRARY = "library";
    public static final String TYPE = "type";
    public static final String USED_SIZE = "used_size";
    public static final String CAPACITY = "capacity";
    public static final String FILE_COUNT = "file_count";
    public static final String CURRENT_LOCATION = "current_location";
    public static final String PREVIOUS_LOCATION = "previous_location";
    public static final String COMPRESSED = "compressed";
    public static final String WORM = "worm";
    public static final String VERSION = "_v";



    @JsonProperty(ID)
    private String id;

    @JsonProperty(CODE)
    private String code;

    @JsonProperty(LABEL)
    private String label;

    @JsonProperty(LIBRARY)
    private String library;

    @JsonProperty(TYPE)
    private String type;

    @JsonProperty(USED_SIZE)
    private Long usedSize;

    @JsonProperty(CAPACITY)
    private Long capacity;

    @JsonProperty(FILE_COUNT)
    private Long fileCount;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLibrary() {
        return library;
    }

    public void setLibrary(String library) {
        this.library = library;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(Long usedSize) {
        this.usedSize = usedSize;
    }

    public Long getCapacity() {
        return capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }

    public Long getFileCount() {
        return fileCount;
    }

    public void setFileCount(Long fileCount) {
        this.fileCount = fileCount;
    }

    public TapeLocation getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(TapeLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    public TapeLocation getPreviousLocation() {
        return previousLocation;
    }

    public void setPreviousLocation(TapeLocation previousLocation) {
        this.previousLocation = previousLocation;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public boolean isWorm() {
        return worm;
    }

    public void setWorm(boolean worm) {
        this.worm = worm;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
