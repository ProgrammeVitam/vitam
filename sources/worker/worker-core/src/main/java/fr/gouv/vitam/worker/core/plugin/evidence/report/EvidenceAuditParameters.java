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
package fr.gouv.vitam.worker.core.plugin.evidence.report;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;

import java.util.Map;

/**
 * Evidence audit Parameters
 */
public class EvidenceAuditParameters {

    private String fileName;
    private DigestType digestType;
    private String fileDigest;
    private String securisationOperationId;

    private String id;
    private MetadataType metadataType;
    private boolean isLastSecurisation;
    private LifeCycleTraceabilitySecureFileObject traceabilityLine;

    private StoredInfoResult mdOptimisticStorageInfo;
    private String hashMdFromDatabase;
    private int lfcVersion;
    private String hashLfcFromDatabase;
    private JsonNode storageMetadataResultListJsonNode;

    private String auditMessage;

    private EvidenceStatus evidenceStatus;

    private Map<String, JsonNode> objectStorageMetadataResultMap;
    private Map<String, StoredInfoResult> mdOptimisticStorageInfoMap;

    /**
     * getter for fileName
     **/
    public String getFileName() {
        return fileName;
    }

    /**
     * setter for fileName
     **/
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * getter for digestType
     **/
    public DigestType getDigestType() {
        return digestType;
    }

    /**
     * setter for digestType
     **/
    public void setDigestType(DigestType digestType) {
        this.digestType = digestType;
    }

    /**
     * getter for fileDigest
     **/
    public String getFileDigest() {
        return fileDigest;
    }

    /**
     * setter for fileDigest
     **/
    public void setFileDigest(String fileDigest) {
        this.fileDigest = fileDigest;
    }

    /**
     * getter for securisationOperationId
     **/
    public String getSecurisationOperationId() {
        return securisationOperationId;
    }

    /**
     * setter for securisationOperationId
     **/
    public void setSecurisationOperationId(String securisationOperationId) {
        this.securisationOperationId = securisationOperationId;
    }

    /**
     * getter for id
     **/
    public String getId() {
        return id;
    }

    /**
     * setter for id
     **/
    public void setId(String id) {
        this.id = id;
    }

    /**
     * getter for metadataType
     **/
    public MetadataType getMetadataType() {
        return metadataType;
    }

    /**
     * setter for metadataType
     **/
    public void setMetadataType(MetadataType metadataType) {
        this.metadataType = metadataType;
    }

    /**
     * getter for isLastSecurisation
     **/
    public boolean isLastSecurisation() {
        return isLastSecurisation;
    }

    /**
     * setter for isLastSecurisation
     **/
    public void setLastSecurisation(boolean lastSecurisation) {
        isLastSecurisation = lastSecurisation;
    }

    /**
     * getter for traceabilityLine
     **/
    public LifeCycleTraceabilitySecureFileObject getTraceabilityLine() {
        return traceabilityLine;
    }

    /**
     * setter for traceabilityLine
     **/
    public void setTraceabilityLine(LifeCycleTraceabilitySecureFileObject traceabilityLine) {
        this.traceabilityLine = traceabilityLine;
    }

    /**
     * getter for mdOptimisticStorageInfo
     **/
    public StoredInfoResult getMdOptimisticStorageInfo() {
        return mdOptimisticStorageInfo;
    }

    /**
     * setter for mdOptimisticStorageInfo
     **/
    public void setMdOptimisticStorageInfo(StoredInfoResult mdOptimisticStorageInfo) {
        this.mdOptimisticStorageInfo = mdOptimisticStorageInfo;
    }

    /**
     * getter for hashMdFromDatabase
     **/
    public String getHashMdFromDatabase() {
        return hashMdFromDatabase;
    }

    /**
     * setter for hashMdFromDatabase
     **/
    public void setHashMdFromDatabase(String hashMdFromDatabase) {
        this.hashMdFromDatabase = hashMdFromDatabase;
    }

    /**
     * getter for lfcVersion
     **/
    public int getLfcVersion() {
        return lfcVersion;
    }

    /**
     * setter for lfcVersion
     **/
    public void setLfcVersion(int lfcVersion) {
        this.lfcVersion = lfcVersion;
    }

    /**
     * getter for hashLfcFromDatabase
     **/
    public String getHashLfcFromDatabase() {
        return hashLfcFromDatabase;
    }

    /**
     * setter for hashLfcFromDatabase
     **/
    public void setHashLfcFromDatabase(String hashLfcFromDatabase) {
        this.hashLfcFromDatabase = hashLfcFromDatabase;
    }

    /**
     * getter for storageMetadataResultListJsonNode
     **/
    public JsonNode getStorageMetadataResultListJsonNode() {
        return storageMetadataResultListJsonNode;
    }

    /**
     * setter for storageMetadataResultListJsonNode
     **/
    public void setStorageMetadataResultListJsonNode(JsonNode storageMetadataResultListJsonNode) {
        this.storageMetadataResultListJsonNode = storageMetadataResultListJsonNode;
    }

    /**
     * getter for auditMessage
     **/
    public String getAuditMessage() {
        return auditMessage;
    }

    /**
     * setter for auditMessage
     **/
    public void setAuditMessage(String auditMessage) {
        this.auditMessage = auditMessage;
    }

    /**
     * getter for evidenceStatus
     **/
    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }

    /**
     * setter for evidenceStatus
     **/
    public void setEvidenceStatus(EvidenceStatus evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    /**
     * getter for objectStorageMetadataResultMap
     **/
    public Map<String, JsonNode> getObjectStorageMetadataResultMap() {
        return objectStorageMetadataResultMap;
    }

    /**
     * setter for objectStorageMetadataResultMap
     **/
    public void setObjectStorageMetadataResultMap(Map<String, JsonNode> objectStorageMetadataResultMap) {
        this.objectStorageMetadataResultMap = objectStorageMetadataResultMap;
    }

    /**
     * getter for mdOptimisticStorageInfoMap
     **/
    public Map<String, StoredInfoResult> getMdOptimisticStorageInfoMap() {
        return mdOptimisticStorageInfoMap;
    }

    /**
     * setter for mdOptimisticStorageInfoMap
     **/
    public void setMdOptimisticStorageInfoMap(Map<String, StoredInfoResult> mdOptimisticStorageInfoMap) {
        this.mdOptimisticStorageInfoMap = mdOptimisticStorageInfoMap;
    }
}
