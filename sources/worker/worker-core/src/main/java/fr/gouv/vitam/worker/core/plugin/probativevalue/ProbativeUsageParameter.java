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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Probative value Parameters
 */
public class ProbativeUsageParameter {

    private String usage ;

    private String id;

    private String binaryVersion;
    private VersionsModel versionsModel;



    private String archivalAgreement;

    private String secureOperationIdForOpId;

    private String securedOperationId;

    private String fileDigest;

    private String secureLogbookFileName;


    private String logBookSecuredOpiFileName;

    private DigestType digestType;

    private Map<String, String> versionSecuredFiles;

    private List<ProbativeCheckReport> reports;

    private boolean isLastSecuredOperation;

    private LifeCycleTraceabilitySecureFileObject traceabilityLine;

    private StoredInfoResult mdOptimisticStorageInfo;

    private String hashMdFromDatabase;

    private int lfcVersion;

    private String hashLfcFromDatabase;

    private JsonNode storageMetadataResultListJsonNode;

    private String message;

    private EvidenceStatus evidenceStatus;

    private Map<String, JsonNode> objectStorageMetadataResultMap;

    private Map<String, StoredInfoResult> mdOptimisticStorageInfoMap;

    private LogbookEvent storageLogbookEvent;

    private String evIdAppSession;

    private String agIdApp;

    private String hashEvents;

    private Map<String, JsonNode> versionLogbook;


    ProbativeUsageParameter(String usage) {
        reports = new ArrayList<>();
        versionLogbook = new HashMap<>();
        this.usage = usage;
    }


    public ProbativeUsageParameter() {
        this("BinaryMaster");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public VersionsModel getVersionsModel() {
        return versionsModel;
    }

    public void setVersionsModel(VersionsModel versionsModel) {
        this.versionsModel = versionsModel;
    }

    public String getArchivalAgreement() {
        return archivalAgreement;
    }

    public void setArchivalAgreement(String archivalAgreement) {
        this.archivalAgreement = archivalAgreement;
    }

    public String getSecureOperationIdForOpId() {
        return secureOperationIdForOpId;
    }

    public void setSecureOperationIdForOpId(String secureOperationIdForOpId) {
        this.secureOperationIdForOpId = secureOperationIdForOpId;
    }


    public String getSecuredOperationId() {
        return securedOperationId;
    }

    public void setSecuredOperationId(String securedOperationId) {
        this.securedOperationId = securedOperationId;
    }

    public String getFileDigest() {
        return fileDigest;
    }

    public void setFileDigest(String fileDigest) {
        this.fileDigest = fileDigest;
    }

    public String getSecureLogbookFileName() {
        return secureLogbookFileName;
    }

    public void setSecureLogbookFileName(String secureLogbookFileName) {
        this.secureLogbookFileName = secureLogbookFileName;
    }



    public String getLogBookSecuredOpiFileName() { return logBookSecuredOpiFileName;    }

    public void setLogBookSecuredOpiFileName(String logBookSecuredOpiFileName) {        this.logBookSecuredOpiFileName =
        logBookSecuredOpiFileName;    }

    public DigestType getDigestType() {
        return digestType;
    }

    public void setDigestType(DigestType digestType) {
        this.digestType = digestType;
    }

    public Map<String, String> getVersionSecuredFiles() {
        return versionSecuredFiles;
    }

    public void setVersionSecuredFiles(Map<String, String> versionSecuredFiles) {
        this.versionSecuredFiles = versionSecuredFiles;
    }

    public boolean isLastSecuredOperation() {
        return isLastSecuredOperation;
    }

    public void setLastSecuredOperation(boolean lastSecuredOperation) {
        isLastSecuredOperation = lastSecuredOperation;
    }

    public LifeCycleTraceabilitySecureFileObject getTraceabilityLine() {
        return traceabilityLine;
    }

    public void setTraceabilityLine(LifeCycleTraceabilitySecureFileObject traceabilityLine) {
        this.traceabilityLine = traceabilityLine;
    }

    public StoredInfoResult getMdOptimisticStorageInfo() {
        return mdOptimisticStorageInfo;
    }

    public void setMdOptimisticStorageInfo(StoredInfoResult mdOptimisticStorageInfo) {
        this.mdOptimisticStorageInfo = mdOptimisticStorageInfo;
    }

    public String getHashMdFromDatabase() {
        return hashMdFromDatabase;
    }

    public void setHashMdFromDatabase(String hashMdFromDatabase) {
        this.hashMdFromDatabase = hashMdFromDatabase;
    }

    public int getLfcVersion() {
        return lfcVersion;
    }

    public void setLfcVersion(int lfcVersion) {
        this.lfcVersion = lfcVersion;
    }

    public String getHashLfcFromDatabase() {
        return hashLfcFromDatabase;
    }

    public void setHashLfcFromDatabase(String hashLfcFromDatabase) {
        this.hashLfcFromDatabase = hashLfcFromDatabase;
    }

    public JsonNode getStorageMetadataResultListJsonNode() {
        return storageMetadataResultListJsonNode;
    }

    public void setStorageMetadataResultListJsonNode(JsonNode storageMetadataResultListJsonNode) {
        this.storageMetadataResultListJsonNode = storageMetadataResultListJsonNode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(EvidenceStatus evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    public Map<String, JsonNode> getObjectStorageMetadataResultMap() {
        return objectStorageMetadataResultMap;
    }

    public void setObjectStorageMetadataResultMap(
        Map<String, JsonNode> objectStorageMetadataResultMap) {
        this.objectStorageMetadataResultMap = objectStorageMetadataResultMap;
    }

    public Map<String, StoredInfoResult> getMdOptimisticStorageInfoMap() {
        return mdOptimisticStorageInfoMap;
    }

    public void setMdOptimisticStorageInfoMap(
        Map<String, StoredInfoResult> mdOptimisticStorageInfoMap) {
        this.mdOptimisticStorageInfoMap = mdOptimisticStorageInfoMap;
    }

    public List<ProbativeCheckReport> getReports() {
        return reports;
    }

    public void setReports(List<ProbativeCheckReport> reports) {
        this.reports = reports;
    }

    public LogbookEvent getStorageLogbookEvent() {
        return storageLogbookEvent;
    }

    public void setStorageLogbookEvent(LogbookEvent storageLogbookEvent) {
        this.storageLogbookEvent = storageLogbookEvent;
    }

    public String getEvIdAppSession() {
        return evIdAppSession;
    }

    public void setEvIdAppSession(String evIdAppSession) {
        this.evIdAppSession = evIdAppSession;
    }

    public String getAgIdApp() {
        return agIdApp;
    }

    public void setAgIdApp(String agIdApp) {
        this.agIdApp = agIdApp;
    }


    public String getHashEvents() {
        return hashEvents;
    }

    public void setHashEvents(String hashEvents) {
        this.hashEvents = hashEvents;
    }

    public Map<String, JsonNode> getVersionLogbook() {
        return versionLogbook;
    }

    public void setVersionLogbook(Map<String, JsonNode> versionLogbook) {
        this.versionLogbook = versionLogbook;
    }


    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getBinaryVersion() {
        return binaryVersion;
    }

    public void setBinaryVersion(String binaryVersion) {
        this.binaryVersion = binaryVersion;
    }
}
