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
package fr.gouv.vitam.logbook.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LifecycleTraceabilityStatus {

    @JsonProperty("completed")
    private boolean completed;

    @JsonProperty("paused")
    private boolean paused;

    @JsonProperty("outcome")
    private String outcome;

    @JsonProperty("maxEntriesReached")
    private boolean maxEntriesReached;

    /**
     * Empty constructor for deserialization
     */
    public LifecycleTraceabilityStatus() {
    }

    public LifecycleTraceabilityStatus(boolean completed, boolean paused, String outcome, boolean maxEntriesReached) {
        this.completed = completed;
        this.paused = paused;
        this.outcome = outcome;
        this.maxEntriesReached = maxEntriesReached;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public boolean isMaxEntriesReached() {
        return maxEntriesReached;
    }

    public void setMaxEntriesReached(boolean maxEntriesReached) {
        this.maxEntriesReached = maxEntriesReached;
    }

    @Override
    public String toString() {
        return "LifecycleTraceabilityStatus{" +
            "completed=" + completed +
            ", outcome='" + outcome + '\'' +
            ", maxEntriesReached=" + maxEntriesReached +
            '}';
    }
}
