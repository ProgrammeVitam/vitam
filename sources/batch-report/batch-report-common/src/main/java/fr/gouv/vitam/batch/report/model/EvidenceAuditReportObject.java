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
package fr.gouv.vitam.batch.report.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * EvidenceAuditReportObject class
 */
public class EvidenceAuditReportObject {

    @JsonProperty("identifier")
    private String identifier;

    @JsonProperty("status")
    private String evidenceStatus;

    @JsonProperty("message")
    private String message;

    @JsonProperty("objectType")
    private String objectType;

    @JsonProperty("strategyId")
    private String strategyId;

    @JsonProperty("securedHash")
    private String securedHash;

    @JsonProperty("offersHashes")
    private Map<String, String> offersHashes;

    EvidenceAuditReportObject() {}

    @JsonCreator
    public EvidenceAuditReportObject(
        @JsonProperty("identifier") String identifier,
        @JsonProperty("status") String evidenceStatus,
        @JsonProperty("message") String message,
        @JsonProperty("objectType") String objectType,
        @JsonProperty("securedHash") String securedHash,
        @JsonProperty("strategyId") String strategyId,
        @JsonProperty("offersHashes") Map<String, String> offersHashes
    ) {
        this.identifier = identifier;
        this.evidenceStatus = evidenceStatus;
        this.message = message;
        this.objectType = objectType;
        this.securedHash = securedHash;
        this.strategyId = strategyId;
        this.offersHashes = offersHashes;
    }

    public EvidenceAuditReportObject(String id) {
        this.identifier = id;
        evidenceStatus = EvidenceStatus.OK.name();
    }

    /**
     * getter for identifier
     **/
    public String getIdentifier() {
        return identifier;
    }

    /**
     * setter for identifier
     **/
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * getter for evidenceStatus
     **/
    public String getEvidenceStatus() {
        return evidenceStatus;
    }

    /**
     * setter for evidenceStatus
     **/
    public void setEvidenceStatus(String evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    /**
     * getter for message
     **/
    public String getMessage() {
        return message;
    }

    /**
     * setter for message
     **/
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * getter for objectType
     **/
    public String getObjectType() {
        return objectType;
    }

    /**
     * setter for objectType
     **/
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    /**
     * getter for securedHash
     **/
    public String getSecuredHash() {
        return securedHash;
    }

    /**
     * setter for securedHash
     **/
    public void setSecuredHash(String securedHash) {
        this.securedHash = securedHash;
    }

    /**
     * getter for strategyId
     **/
    public String getStrategyId() {
        return strategyId;
    }

    /**
     * setter for strategyId
     **/
    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    /**
     * getter for offersHashes
     **/
    public Map<String, String> getOffersHashes() {
        return offersHashes;
    }

    /**
     * setter for offersHashes
     **/
    public void setOffersHashes(Map<String, String> offersHashes) {
        this.offersHashes = offersHashes;
    }
}
