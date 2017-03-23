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

package fr.gouv.vitam.storage.engine.common.model.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data structure representing global result from a 'createObject' request
 */
public class StoredInfoResult {
    private String id;
    private String info;
    private String objectGroupId;
    private List<String> unitIds;
    @JsonProperty("creation_time")
    private String creationTime;
    @JsonProperty("last_access_time")
    private String lastAccessTime;
    @JsonProperty("last_checked_time")
    private String lastCheckedTime;
    @JsonProperty("last_modified_time")
    private String lastModifiedTime;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id of global result
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the info
     */
    public String getInfo() {
        return info;
    }

    /**
     * @param info of result 
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * @return the objectGroup Id
     */
    public String getObjectGroupId() {
        return objectGroupId;
    }

    /**
     * @param objectGroupId of global result
     */
    public void setObjectGroupId(String objectGroupId) {
        this.objectGroupId = objectGroupId;
    }

    /**
     * @return the unitIds
     */
    public List<String> getUnitIds() {
        return unitIds;
    }

    /**
     * @param unitIds of global result
     */
    public void setUnitIds(List<String> unitIds) {
        this.unitIds = unitIds;
    }

    /**
     * @return the creation Time
     */
    public String getCreationTime() {
        return creationTime;
    }

    /**
     * @param creationTime of global result
     */
    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * @return the Last access Time
     */
    public String getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * @param lastAccessTime of global result
     */
    public void setLastAccessTime(String lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    /**
     * @return the Last checked Time
     */
    public String getLastCheckedTime() {
        return lastCheckedTime;
    }

    /**
     * @param lastCheckedTime of global result
     */
    public void setLastCheckedTime(String lastCheckedTime) {
        this.lastCheckedTime = lastCheckedTime;
    }

    /**
     * @return the Last modified Time
     */
    public String getLastModifiedTime() {
        return lastModifiedTime;
    }

    /**
     * @param lastModifiedTime of global result
     */
    public void setLastModifiedTime(String lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
