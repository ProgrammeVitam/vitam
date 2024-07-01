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
package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class CompactedOfferLog {

    public static final String SEQUENCE_START = "SequenceStart";
    public static final String SEQUENCE_END = "SequenceEnd";
    public static final String CONTAINER = "Container";
    public static final String LOGS = "Logs";

    @JsonProperty(SEQUENCE_START)
    private long sequenceStart;

    @JsonProperty(SEQUENCE_END)
    private long sequenceEnd;

    @JsonProperty("CompactionDateTime")
    private LocalDateTime compactionDateTime;

    @JsonProperty(CONTAINER)
    private String container;

    @JsonProperty(LOGS)
    private List<OfferLog> logs;

    public CompactedOfferLog() {}

    public CompactedOfferLog(
        long sequenceStart,
        long sequenceEnd,
        LocalDateTime compactionDateTime,
        String container,
        List<OfferLog> logs
    ) {
        this.sequenceStart = sequenceStart;
        this.sequenceEnd = sequenceEnd;
        this.compactionDateTime = Objects.requireNonNull(compactionDateTime);
        this.container = Objects.requireNonNull(container);
        this.logs = Objects.requireNonNull(logs);
    }

    public long getSequenceStart() {
        return sequenceStart;
    }

    public void setSequenceStart(long sequenceStart) {
        this.sequenceStart = sequenceStart;
    }

    public long getSequenceEnd() {
        return sequenceEnd;
    }

    public void setSequenceEnd(long sequenceEnd) {
        this.sequenceEnd = sequenceEnd;
    }

    public LocalDateTime getCompactionDateTime() {
        return compactionDateTime;
    }

    public void setCompactionDateTime(LocalDateTime compactionDateTime) {
        this.compactionDateTime = compactionDateTime;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public List<OfferLog> getLogs() {
        return logs;
    }

    public void setLogs(List<OfferLog> logs) {
        this.logs = logs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompactedOfferLog that = (CompactedOfferLog) o;
        return (
            sequenceStart == that.sequenceStart &&
            sequenceEnd == that.sequenceEnd &&
            compactionDateTime.equals(that.compactionDateTime) &&
            container.equals(that.container) &&
            Objects.equals(logs, that.logs)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequenceStart, sequenceEnd, compactionDateTime, container, logs);
    }

    @Override
    public String toString() {
        return (
            "OfferLogCompaction{" +
            "sequenceStart=" +
            sequenceStart +
            ", sequenceEnd=" +
            sequenceEnd +
            ", compactionDateTime=" +
            compactionDateTime +
            ", container='" +
            container +
            '\'' +
            '}'
        );
    }
}
