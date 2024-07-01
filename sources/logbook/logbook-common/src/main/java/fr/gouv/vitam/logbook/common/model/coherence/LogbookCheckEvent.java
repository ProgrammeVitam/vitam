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
 * Description of LogbookCheckEvent model. <br/>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogbookCheckEvent implements Serializable {

    /**
     * Event Type
     */
    @JsonProperty("eventType")
    private String eventType;

    /**
     * Outcome
     */
    @JsonProperty("outcome")
    private String outcome;

    /**
     * Outcome Detail
     */
    @JsonProperty("outcomeDetail")
    private String outcomeDetail;

    public LogbookCheckEvent() {}

    public LogbookCheckEvent(String evType, String outcome, String outDetail) {
        this.eventType = evType;
        this.outcome = outcome;
        this.outcomeDetail = outDetail;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getOutcomeDetail() {
        return outcomeDetail;
    }

    public void setOutcomeDetail(String outcomeDetail) {
        this.outcomeDetail = outcomeDetail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LogbookCheckEvent that = (LogbookCheckEvent) o;

        if (eventType != null ? !eventType.equals(that.eventType) : that.eventType != null) {
            return false;
        }
        if (outcome != null ? !outcome.equals(that.outcome) : that.outcome != null) {
            return false;
        }
        return outcomeDetail != null ? outcomeDetail.equals(that.outcomeDetail) : that.outcomeDetail == null;
    }

    @Override
    public int hashCode() {
        int result = eventType != null ? eventType.hashCode() : 0;
        result = 31 * result + (outcome != null ? outcome.hashCode() : 0);
        result = 31 * result + (outcomeDetail != null ? outcomeDetail.hashCode() : 0);
        return result;
    }
}
