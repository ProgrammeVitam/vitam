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
package fr.gouv.vitam.worker.core.plugin.probativevalue.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.ReportSummary;

import java.util.List;

public class ProbativeReportV2 {

    private final int reportVersion = 2;

    private final OperationSummary operationSummary;
    private final ReportSummary reportSummary;
    private final JsonNode context;
    private final List<ProbativeReportEntry> reportEntries;

    @JsonCreator
    public ProbativeReportV2(
        @JsonProperty("operationSummary") OperationSummary operationSummary,
        @JsonProperty("reportSummary") ReportSummary reportSummary,
        @JsonProperty("context") JsonNode context,
        @JsonProperty("reportEntries") List<ProbativeReportEntry> reportEntries
    ) {
        this.operationSummary = operationSummary;
        this.reportSummary = reportSummary;
        this.context = context;
        this.reportEntries = reportEntries;
    }

    @JsonProperty("ReportVersion")
    public int getReportVersion() {
        return reportVersion;
    }

    @JsonProperty("operationSummary")
    public OperationSummary getOperationSummary() {
        return operationSummary;
    }

    @JsonProperty("reportSummary")
    public ReportSummary getReportSummary() {
        return reportSummary;
    }

    @JsonProperty("context")
    public JsonNode getContext() {
        return context;
    }

    @JsonProperty("reportEntries")
    public List<ProbativeReportEntry> getReportEntries() {
        return reportEntries;
    }
}
