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
package fr.gouv.vitam.storage.engine.common.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.List;

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

    @JsonProperty("strategy")
    private String strategy;

    @JsonProperty("nbCopy")
    private int nbCopy;

    @JsonProperty("offerIds")
    private List<String> offerIds;

    @JsonProperty("digestType")
    private String digestType;

    @JsonProperty("digest")
    private String digest;

    /**
     * @return the strategy
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * @param strategy the strategy to set
     * @return this
     */
    public StoredInfoResult setStrategy(String strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * @return the nbCopy
     */
    public int getNbCopy() {
        return nbCopy;
    }

    /**
     * @param nbCopy the nbCopy to set
     * @return this
     */
    public StoredInfoResult setNbCopy(int nbCopy) {
        this.nbCopy = nbCopy;
        return this;
    }

    /**
     * @return the offerIds
     */
    public List<String> getOfferIds() {
        return offerIds;
    }

    /**
     * @param offerIds the offerIds to set
     * @return this
     */
    public StoredInfoResult setOfferIds(List<String> offerIds) {
        this.offerIds = offerIds;
        return this;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id of global result
     * @return this
     */
    public StoredInfoResult setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return the info
     */
    public String getInfo() {
        return info;
    }

    /**
     * @param info of result
     * @return this
     */
    public StoredInfoResult setInfo(String info) {
        this.info = info;
        return this;
    }

    /**
     * @return the objectGroup Id
     */
    public String getObjectGroupId() {
        return objectGroupId;
    }

    /**
     * @param objectGroupId of global result
     * @return this
     */
    public StoredInfoResult setObjectGroupId(String objectGroupId) {
        this.objectGroupId = objectGroupId;
        return this;
    }

    /**
     * @return the unitIds
     */
    public List<String> getUnitIds() {
        return unitIds;
    }

    /**
     * @param unitIds of global result
     * @return this
     */
    public StoredInfoResult setUnitIds(List<String> unitIds) {
        this.unitIds = unitIds;
        return this;
    }

    /**
     * @return the creation Time
     */
    public String getCreationTime() {
        return creationTime;
    }

    /**
     * @param creationTime of global result
     * @return this
     */
    public StoredInfoResult setCreationTime(String creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    /**
     * @return the Last access Time
     */
    public String getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * @param lastAccessTime of global result
     * @return this
     */
    public StoredInfoResult setLastAccessTime(String lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
        return this;
    }

    /**
     * @return the Last checked Time
     */
    public String getLastCheckedTime() {
        return lastCheckedTime;
    }

    /**
     * @param lastCheckedTime of global result
     * @return this
     */
    public StoredInfoResult setLastCheckedTime(String lastCheckedTime) {
        this.lastCheckedTime = lastCheckedTime;
        return this;
    }

    /**
     * @return the Last modified Time
     */
    public String getLastModifiedTime() {
        return lastModifiedTime;
    }

    /**
     * @param lastModifiedTime of global result
     * @return this
     */
    public StoredInfoResult setLastModifiedTime(String lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
        return this;
    }

    /**
     * @return the digestType
     */
    public String getDigestType() {
        return digestType;
    }

    /**
     * @param digestType of global result
     * @return this
     */
    public StoredInfoResult setDigestType(String digestType) {
        this.digestType = digestType;
        return this;
    }

    /**
     * @return the digest
     */
    public String getDigest() {
        return digest;
    }

    /**
     * @param digest of global result
     * @return this
     */
    public StoredInfoResult setDigest(String digest) {
        this.digest = digest;
        return this;
    }

    @Override
    public String toString() {
        return JsonHandler.prettyPrint(this);
    }
}
