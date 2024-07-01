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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup.report;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.StatusCode;

import java.util.ArrayList;
import java.util.List;

public class IngestCleanupUnitReportEntry {

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private StatusCode status = StatusCode.UNKNOWN;

    @JsonProperty("errors")
    private List<String> errors;

    @JsonProperty("warnings")
    private List<String> warnings;

    public String getId() {
        return id;
    }

    public IngestCleanupUnitReportEntry setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Constant for report serialization
     */
    @JsonGetter("type")
    public MetadataType getMetadataType() {
        return MetadataType.Unit;
    }

    public StatusCode getStatus() {
        return status;
    }

    public IngestCleanupUnitReportEntry setStatus(StatusCode status) {
        this.status = status;
        return this;
    }

    public List<String> getErrors() {
        return errors;
    }

    public IngestCleanupUnitReportEntry setErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public IngestCleanupUnitReportEntry setWarnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }

    public void addWarning(String message) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(message);
    }

    public void updateStatus(StatusCode statusCode) {
        if (this.status.compareTo(statusCode) < 0) {
            this.status = statusCode;
        }
    }
}
