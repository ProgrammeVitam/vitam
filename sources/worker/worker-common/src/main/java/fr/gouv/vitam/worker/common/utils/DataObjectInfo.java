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
package fr.gouv.vitam.worker.common.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.DigestType;

/**
 * The class DataObjectInfo is stored all information of the DataObjectInfo
 */
public final class DataObjectInfo {

    private String id;
    private String version;
    private int rank = 1;
    private String uri;
    private String messageDigest;
    private Long size;
    private DigestType algo;
    private String physicalId;
    private String type;
    private Boolean isSizeIncorrect = Boolean.FALSE;
    private ObjectNode diffSizeJson;

    /**
     * @return id of the data object
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id of the data to set
     * @return DataObjectInfo
     */
    public DataObjectInfo setId(String id) {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);
        this.id = id;
        return this;
    }

    /**
     * @return version of the data object
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version of the data to set
     * @return BinaryObjectInfo
     */
    public DataObjectInfo setVersion(String version) {
        ParametersChecker.checkParameter("version is a mandatory parameter", version);
        this.version = version;
        return this;
    }

    /**
     * @return uri of the data as String
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri uri of the data object as String
     * @return BinaryObjectInfo
     */
    public DataObjectInfo setUri(String uri) {
        ParametersChecker.checkParameter("uri is a mandatory parameter", uri);
        this.uri = uri;
        return this;
    }

    /**
     * @return messageDigest as String
     */
    public String getMessageDigest() {
        return messageDigest;
    }

    /**
     * @param messageDigest the message digest of the data to set
     * @return BinaryObjectInfo
     */
    public DataObjectInfo setMessageDigest(String messageDigest) {
        // Digest should not be null, but checkDigest Handler will manage null case
        this.messageDigest = messageDigest;
        return this;
    }

    /**
     * @return size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @param size the size of the data to set
     * @return BinaryObjectInfo
     */
    public DataObjectInfo setSize(Long size) {
        this.size = size;
        return this;
    }

    /**
     * @return DigestType
     */
    public DigestType getAlgo() {
        return algo;
    }

    /**
     * @param algo digest algorithm
     * @return BinaryObjectInfo
     */
    public DataObjectInfo setAlgo(DigestType algo) {
        ParametersChecker.checkParameter("algo is a mandatory parameter", algo);
        this.algo = algo;
        return this;
    }

    /**
     * @return the rank in version
     */
    public int getRank() {
        return rank;
    }

    /**
     * @param rank the rank in version
     * @return this
     */
    public DataObjectInfo setRank(int rank) {
        this.rank = rank;
        return this;
    }

    /**
     * @return type as String
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type of the data to set
     * @return BinaryObjectInfo
     */
    public DataObjectInfo setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * @return physicalId as String
     */
    public String getPhysicalId() {
        return physicalId;
    }

    /**
     * @param physicalId the physical Id of the data to set
     * @return BinaryObjectInfo
     */
    public DataObjectInfo setPhysicalId(String physicalId) {
        this.physicalId = physicalId;
        return this;
    }

    /**
     * Field to conatain the diff if size has not the same between manifest and binary file
     *
     * @return
     */

    public ObjectNode getDiffSizeJson() {
        return diffSizeJson;
    }

    public DataObjectInfo setDiffSizeJson(ObjectNode diffSizeJson) {
        this.diffSizeJson = diffSizeJson;
        return this;
    }

    public Boolean getSizeIncorrect() {
        return isSizeIncorrect;
    }

    public DataObjectInfo setSizeIncorrect(Boolean sizeIncorrect) {
        this.isSizeIncorrect = sizeIncorrect;
        return this;
    }
}
