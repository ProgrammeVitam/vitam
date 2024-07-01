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
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Lifecycle Tracability secure File Object that wrap data for each line in the secured file
 */
public class LifeCycleTraceabilitySecureFileObject {

    @JsonProperty("lEvtIdProc")
    private String lastEvtIdProc;

    @JsonProperty("lEvTypeProc")
    private String lastEvTypeProc;

    @JsonProperty("lEvDTime")
    private String lastEvDateTime;

    @JsonProperty("lfcId")
    private String lfcId;

    @JsonProperty("mdType")
    private MetadataType metadataType;

    @JsonProperty("version")
    private int version;

    @JsonProperty("ltEvtOutcome")
    private String lastEvtOutcome;

    @JsonProperty("hLFC")
    private String hashLFC;

    @JsonProperty("hLFCEvts")
    private String hashLFCEvents;

    @JsonProperty("hMetadata")
    private String hashMetadata;

    @JsonProperty("hGlobalFStorage")
    private String hashGlobalFromStorage;

    @JsonProperty("hGlobalDetails")
    private TraceabilityHashDetails traceabilityHashDetails;

    @JsonProperty("hOGDocsStorage")
    private List<ObjectGroupDocumentHash> objectGroupDocumentHashList;

    @JsonProperty("up")
    private List<String> up;

    @JsonProperty("idOG")
    private String idGot;

    /**
     * set lastEvtIdProc
     */
    public void setLastEvtIdProc(String lastEvtIdProc) {
        this.lastEvtIdProc = lastEvtIdProc;
    }

    /**
     * set lastEvTypeProc
     */
    public void setLastEvTypeProc(String lastEvTypeProc) {
        this.lastEvTypeProc = lastEvTypeProc;
    }

    /**
     * set lastEvDateTime
     */
    public void setLastEvDateTime(String lastEvDateTime) {
        this.lastEvDateTime = lastEvDateTime;
    }

    /**
     * set lfcId
     */
    public void setLfcId(String lfcId) {
        this.lfcId = lfcId;
    }

    /**
     * set metadataType
     */
    public void setMetadataType(MetadataType metadataType) {
        this.metadataType = metadataType;
    }

    /**
     * set version
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * set lastEvtOutcome
     */
    public void setLastEvtOutcome(String lastEvtOutcome) {
        this.lastEvtOutcome = lastEvtOutcome;
    }

    /**
     * set hashLFC
     */
    public void setHashLFC(String hashLFC) {
        this.hashLFC = hashLFC;
    }

    /**
     * Set hashLFCEvents
     *
     * @param hashLFCEvents
     */
    public void setHashLFCEvents(String hashLFCEvents) {
        this.hashLFCEvents = hashLFCEvents;
    }

    /**
     * set hashMetadata
     */
    public void setHashMetadata(String hashMetadata) {
        this.hashMetadata = hashMetadata;
    }

    /**
     * set hashGlobalFromStorage
     */
    public void setHashGlobalFromStorage(String hashGlobalFromStorage) {
        this.hashGlobalFromStorage = hashGlobalFromStorage;
    }

    /**
     * set objectGroupDocumentHashList
     */
    public void setObjectGroupDocumentHashList(List<ObjectGroupDocumentHash> objectGroupDocumentHashList) {
        this.objectGroupDocumentHashList = objectGroupDocumentHashList;
    }

    public LifeCycleTraceabilitySecureFileObject() {}

    /**
     * Constructor to set the attributes for lfc tracibility file lines
     *
     * @param lastEvtIdProc lastEvtIdProc
     * @param lastEvTypeProc lastEvTypeProc
     * @param lastEvDateTime lastEvDateTime
     * @param lfcId lfcId
     * @param metadataType metadataType
     * @param version version
     * @param lastEvtOutcome lastEvtOutcome
     * @param hashLFC hashLFC
     * @param hashMetadata hashMetadata
     * @param hashGlobalFromStorage hashGlobalFromStorage
     */
    public LifeCycleTraceabilitySecureFileObject(
        String lastEvtIdProc,
        String lastEvTypeProc,
        String lastEvDateTime,
        String lfcId,
        MetadataType metadataType,
        int version,
        String lastEvtOutcome,
        String hashLFC,
        String hashLFCEvents,
        String hashMetadata,
        String hashGlobalFromStorage,
        List<ObjectGroupDocumentHash> objectGroupDocumentHashList
    ) {
        this.lastEvtIdProc = lastEvtIdProc;
        this.lastEvTypeProc = lastEvTypeProc;
        this.lastEvDateTime = lastEvDateTime;
        this.lfcId = lfcId;
        this.metadataType = metadataType;
        this.version = version;
        this.lastEvtOutcome = lastEvtOutcome;
        this.hashLFC = hashLFC;
        this.hashLFCEvents = hashLFCEvents;

        this.hashMetadata = hashMetadata;
        this.hashGlobalFromStorage = hashGlobalFromStorage;

        if (objectGroupDocumentHashList != null && !objectGroupDocumentHashList.isEmpty()) {
            this.objectGroupDocumentHashList = objectGroupDocumentHashList;
        }
    }

    /**
     * getter for lastEvtIdProc
     *
     * @return lastEvtIdProc value
     */
    public String getLastEvtIdProc() {
        return lastEvtIdProc;
    }

    /**
     * getter for lastEvTypeProc
     *
     * @return lastEvTypeProc value
     */
    public String getLastEvTypeProc() {
        return lastEvTypeProc;
    }

    /**
     * getter for lastEvDateTime
     *
     * @return lastEvDateTime value
     */
    public String getLastEvDateTime() {
        return lastEvDateTime;
    }

    /**
     * getter for lfcId
     *
     * @return lfcId value
     */
    public String getLfcId() {
        return lfcId;
    }

    /**
     * getter for metadataType
     *
     * @return metadataType value
     */
    public MetadataType getMetadataType() {
        return metadataType;
    }

    /**
     * getter for version
     *
     * @return version value
     */
    public int getVersion() {
        return version;
    }

    /**
     * getter for lastEvtOutcome
     *
     * @return lastEvtOutcome value
     */
    public String getLastEvtOutcome() {
        return lastEvtOutcome;
    }

    /**
     * getter for hashLFC
     *
     * @return hashLFC value
     */
    public String getHashLFC() {
        return hashLFC;
    }

    /**
     * getter fro hashLFCEvents
     *
     * @return hashLFCEvents
     */
    public String getHashLFCEvents() {
        return hashLFCEvents;
    }

    /**
     * getter for hashMetadata
     *
     * @return hashMetadata value
     */
    public String getHashMetadata() {
        return hashMetadata;
    }

    /**
     * getter for hashGlobalFromStorage
     *
     * @return hashGlobalFromStorage value
     */
    public String getHashGlobalFromStorage() {
        return hashGlobalFromStorage;
    }

    /**
     * getter for objectGroupDocumentHashList
     *
     * @return objectGroupDocumentHashList value
     */
    public List<ObjectGroupDocumentHash> getObjectGroupDocumentHashList() {
        return objectGroupDocumentHashList;
    }

    /**
     * getter for up
     **/
    public List<String> getUp() {
        return up;
    }

    /**
     * setter for up
     **/
    public void setUp(List<String> up) {
        this.up = up;
    }

    /**
     * getter for idGot
     **/
    public String getIdGot() {
        return idGot;
    }

    /**
     * setter for idGot
     **/
    public void setIdGot(String idGot) {
        this.idGot = idGot;
    }

    public TraceabilityHashDetails getTraceabilityHashDetails() {
        return traceabilityHashDetails;
    }

    public LifeCycleTraceabilitySecureFileObject setTraceabilityHashDetails(
        TraceabilityHashDetails traceabilityHashDetails
    ) {
        this.traceabilityHashDetails = traceabilityHashDetails;
        return this;
    }
}
