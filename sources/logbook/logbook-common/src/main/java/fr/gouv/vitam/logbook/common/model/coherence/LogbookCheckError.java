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
package fr.gouv.vitam.logbook.common.model.coherence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Description of LogbookCheckResult model. <br/>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogbookCheckError implements Serializable {

    /**
     * Operation identifier.
     */
    @JsonProperty("operationId")
    private String operationId;

    /**
     * Unit or Got LFC identifier.
     */
    @JsonProperty("lfcId")
    private String lfcId;

    /**
     * Checked property : evType.
     */
    @JsonProperty("checkedProperty")
    private String checkedProperty;

    /**
     * Actual logbook.
     */
    @JsonProperty("savedLogbookMsg")
    private String savedLogbookMsg;

    /**
     * Expected logbook.
     */
    @JsonProperty("expectedLogbookMsg")
    private String expectedLogbookMsg;

    public LogbookCheckError() {}

    /**
     * LogbookCheckResult constructor.
     *
     * @param operationId
     * @param lfcId
     * @param checkedProperty
     * @param savedLogbookMsg
     * @param expectedLogbookMsg
     */
    public LogbookCheckError(
        String operationId,
        String lfcId,
        String checkedProperty,
        String savedLogbookMsg,
        String expectedLogbookMsg
    ) {
        this.operationId = operationId;
        this.lfcId = lfcId;
        this.checkedProperty = checkedProperty;
        this.savedLogbookMsg = savedLogbookMsg;
        this.expectedLogbookMsg = expectedLogbookMsg;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getLfcId() {
        return lfcId;
    }

    public void setLfcId(String lfcId) {
        this.lfcId = lfcId;
    }

    public String getCheckedProperty() {
        return checkedProperty;
    }

    public void setCheckedProperty(String checkedProperty) {
        this.checkedProperty = checkedProperty;
    }

    public String getSavedLogbookMsg() {
        return savedLogbookMsg;
    }

    public void setSavedLogbookMsg(String savedLogbookMsg) {
        this.savedLogbookMsg = savedLogbookMsg;
    }

    public String getExpectedLogbookMsg() {
        return expectedLogbookMsg;
    }

    public void setExpectedLogbookMsg(String expectedLogbookMsg) {
        this.expectedLogbookMsg = expectedLogbookMsg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LogbookCheckError that = (LogbookCheckError) o;

        if (
            checkedProperty != null ? !checkedProperty.equals(that.checkedProperty) : that.checkedProperty != null
        ) return false;
        if (
            savedLogbookMsg != null ? !savedLogbookMsg.equals(that.savedLogbookMsg) : that.savedLogbookMsg != null
        ) return false;
        return expectedLogbookMsg != null
            ? expectedLogbookMsg.equals(that.expectedLogbookMsg)
            : that.expectedLogbookMsg == null;
    }

    @Override
    public int hashCode() {
        int result = checkedProperty != null ? checkedProperty.hashCode() : 0;
        result = 31 * result + (savedLogbookMsg != null ? savedLogbookMsg.hashCode() : 0);
        result = 31 * result + (expectedLogbookMsg != null ? expectedLogbookMsg.hashCode() : 0);
        return result;
    }
}
