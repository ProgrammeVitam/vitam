/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.logbook;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Logbook Common event model
 */
public class LogbookEvent {

    public static final String EV_ID = "evId";
    public static final String EV_PARENT_ID = "evParentId";
    public static final String EV_TYPE = "evType";
    public static final String EV_DATE_TIME = "evDateTime";
    public static final String EV_ID_PROC = "evIdProc";
    public static final String EV_TYPE_PROC = "evTypeProc";
    public static final String OUTCOME = "outcome";
    public static final String OUT_DETAIL = "outDetail";
    public static final String OUT_MESSG = "outMessg";
    public static final String AG_ID = "agId";
    public static final String OB_ID = "obId";
    public static final String EV_DET_DATA = "evDetData";
    public static final String RIGHTS_STATEMENT_IDENTIFIER = "rightsStatementIdentifier";
    public static final String LAST_PERSISTED_DATE = "_lastPersistedDate";

    @JsonProperty(EV_ID)
    private String evId;

    @JsonProperty(EV_PARENT_ID)
    private String evParentId;

    @JsonProperty(EV_TYPE)
    private String evType;

    @JsonProperty(EV_DATE_TIME)
    private String evDateTime;

    @JsonProperty(EV_ID_PROC)
    private String evIdProc;

    @JsonProperty(EV_TYPE_PROC)
    private String evTypeProc;

    @JsonProperty(OUTCOME)
    private String outcome;

    @JsonProperty(OUT_DETAIL)
    private String outDetail;

    @JsonProperty(OUT_MESSG)
    private String outMessg;

    @JsonProperty(AG_ID)
    private String agId;

    @JsonProperty(OB_ID)
    private String obId;

    @JsonProperty(EV_DET_DATA)
    private String evDetData;

    @JsonProperty(RIGHTS_STATEMENT_IDENTIFIER)
    private String rightsStatementIdentifier;

    @JsonProperty(LAST_PERSISTED_DATE)
    private String lastPersistedDate;

    /**
     * @return the evId
     */
    public String getEvId() {
        return evId;
    }

    /**
     * @param evId the evId to set
     */
    public void setEvId(String evId) {
        this.evId = evId;
    }

    /**
     * @return the evParentId
     */
    public String getEvParentId() {
        return evParentId;
    }

    /**
     * @param evParentId the evParentId to set
     */
    public void setEvParentId(String evParentId) {
        this.evParentId = evParentId;
    }

    /**
     * @return the evType
     */
    public String getEvType() {
        return evType;
    }

    /**
     * @param evType the evType to set
     */
    public void setEvType(String evType) {
        this.evType = evType;
    }

    /**
     * @return the evDateTime
     */
    public String getEvDateTime() {
        return evDateTime;
    }

    /**
     * @param evDateTime the evDateTime to set
     */
    public void setEvDateTime(String evDateTime) {
        this.evDateTime = evDateTime;
    }

    /**
     * @return the evIdProc
     */
    public String getEvIdProc() {
        return evIdProc;
    }

    /**
     * @param evIdProc the evIdProc to set
     */
    public void setEvIdProc(String evIdProc) {
        this.evIdProc = evIdProc;
    }

    /**
     * @return the evTypeProc
     */
    public String getEvTypeProc() {
        return evTypeProc;
    }

    /**
     * @param evTypeProc the evTypeProc to set
     */
    public void setEvTypeProc(String evTypeProc) {
        this.evTypeProc = evTypeProc;
    }

    /**
     * @return the outcome
     */
    public String getOutcome() {
        return outcome;
    }

    /**
     * @param outcome the outcome to set
     */
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    /**
     * @return the outDetail
     */
    public String getOutDetail() {
        return outDetail;
    }

    /**
     * @param outDetail the outDetail to set
     */
    public void setOutDetail(String outDetail) {
        this.outDetail = outDetail;
    }

    /**
     * @return the outMessg
     */
    public String getOutMessg() {
        return outMessg;
    }

    /**
     * @param outMessg the outMessg to set
     */
    public void setOutMessg(String outMessg) {
        this.outMessg = outMessg;
    }

    /**
     * @return the agId
     */
    public String getAgId() {
        return agId;
    }

    /**
     * @param agId the agId to set
     */
    public void setAgId(String agId) {
        this.agId = agId;
    }

    /**
     * @return the obId
     */
    public String getObId() {
        return obId;
    }

    /**
     * @param obId the obId to set
     */
    public void setObId(String obId) {
        this.obId = obId;
    }

    /**
     * @return the evDetData
     */
    public String getEvDetData() {
        return evDetData;
    }

    /**
     * @param evDetData the evDetData to set
     */
    public void setEvDetData(String evDetData) {
        this.evDetData = evDetData;
    }

    /**
     * Rights statement Identifier
     *
     * @return rightsStatementIdentifier
     */
    public String getRightsStatementIdentifier() {
        return rightsStatementIdentifier;
    }

    /**
     * Rights statement Identifier
     *
     * @param rightsStatementIdentifier
     */
    public void setRightsStatementIdentifier(String rightsStatementIdentifier) {
        this.rightsStatementIdentifier = rightsStatementIdentifier;
    }

    public String getLastPersistedDate() {
        return lastPersistedDate;
    }

    public void setLastPersistedDate(String lastPersistedDate) {
        this.lastPersistedDate = lastPersistedDate;
    }
}
