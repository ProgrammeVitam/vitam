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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.model.StatusCode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;

public class ProbativeReportEntry {

    private final List<String> unitIds;
    private final String objectGroupId;
    private final String objectId;
    private final String usageVersion;
    private final List<ProbativeOperation> operations;
    private final List<ProbativeCheck> checks;
    private final String evStartDateTime;
    private final String evEndDateTime;
    private final StatusCode status;

    @JsonCreator
    public ProbativeReportEntry(
        @JsonProperty("unitIds") List<String> unitIds,
        @JsonProperty("objectGroupId") String objectGroupId,
        @JsonProperty("objectId") String objectId,
        @JsonProperty("usageVersion") String usageVersion,
        @JsonProperty("operations") List<ProbativeOperation> operations,
        @JsonProperty("checks") List<ProbativeCheck> checks,
        @JsonProperty("evStartDateTime") String evStartDateTime,
        @JsonProperty("evEndDateTime") String evEndDateTime,
        @JsonProperty("status") StatusCode status
    ) {
        this.unitIds = unitIds;
        this.objectGroupId = objectGroupId;
        this.objectId = objectId;
        this.usageVersion = usageVersion;
        this.operations = operations;
        this.checks = checks;
        this.evStartDateTime = evStartDateTime;
        this.evEndDateTime = evEndDateTime;
        this.status = status;
    }

    public ProbativeReportEntry(
        String evStartDateTime,
        List<String> unitIds,
        String objectGroupId,
        String objectId,
        String usageVersion,
        List<ProbativeOperation> operations,
        List<ProbativeCheck> checks
    ) {
        this.unitIds = unitIds;
        this.objectGroupId = objectGroupId;
        this.objectId = objectId;
        this.usageVersion = usageVersion;
        this.operations = operations;
        this.checks = checks;
        this.evStartDateTime = evStartDateTime;
        this.evEndDateTime = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
        this.status = getStatus(operations, checks);
    }

    @JsonIgnore
    private StatusCode getStatus(List<ProbativeOperation> operations, List<ProbativeCheck> checks) {
        if (
            operations.stream().allMatch(Objects::nonNull) &&
            operations.size() == 3 &&
            checks.stream().allMatch(Objects::nonNull) &&
            checks.stream().allMatch(c -> OK.equals(c.getStatus())) &&
            checks.size() == ChecksInformation.values().length
        ) {
            return OK;
        }

        if (
            operations.stream().allMatch(Objects::nonNull) &&
            operations.size() == 3 &&
            checks.stream().allMatch(Objects::nonNull) &&
            checks.stream().noneMatch(c -> KO.equals(c.getStatus())) &&
            checks.size() == ChecksInformation.values().length
        ) {
            return WARNING;
        }

        return KO;
    }

    private ProbativeReportEntry(
        String evStartDateTime,
        List<String> unitIds,
        String objectGroupId,
        String objectId,
        String usageVersion
    ) {
        this.unitIds = unitIds;
        this.objectGroupId = objectGroupId;
        this.objectId = objectId;
        this.usageVersion = usageVersion;
        this.operations = Collections.emptyList();
        this.checks = Collections.emptyList();
        this.evStartDateTime = evStartDateTime;
        this.evEndDateTime = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
        this.status = KO;
    }

    @JsonIgnore
    public static ProbativeReportEntry koFrom(
        String evStartDateTime,
        List<String> unitIds,
        String objectGroupId,
        String objectId,
        String usageVersion
    ) {
        return new ProbativeReportEntry(evStartDateTime, unitIds, objectGroupId, objectId, usageVersion);
    }

    @JsonIgnore
    public static ProbativeReportEntry koFrom(
        String startEntryCreation,
        List<String> unitIds,
        String objectGroupId,
        String objectId,
        String usageVersion,
        List<ProbativeOperation> probativeOperations
    ) {
        return new ProbativeReportEntry(
            startEntryCreation,
            unitIds,
            objectGroupId,
            objectId,
            usageVersion,
            probativeOperations,
            Collections.emptyList()
        );
    }

    @JsonIgnore
    public static ProbativeReportEntry koFrom(
        String startEntryCreation,
        List<String> unitIds,
        String objectGroupId,
        String objectId,
        String usageVersion,
        List<ProbativeOperation> probativeOperations,
        List<ProbativeCheck> probativeChecks
    ) {
        return new ProbativeReportEntry(
            startEntryCreation,
            unitIds,
            objectGroupId,
            objectId,
            usageVersion,
            probativeOperations,
            probativeChecks
        );
    }

    @JsonProperty("unitIds")
    public List<String> getUnitIds() {
        return unitIds;
    }

    @JsonProperty("objectGroupId")
    public String getObjectGroupId() {
        return objectGroupId;
    }

    @JsonProperty("objectId")
    public String getObjectId() {
        return objectId;
    }

    @JsonProperty("usageVersion")
    public String getUsageVersion() {
        return usageVersion;
    }

    @JsonProperty("operations")
    public List<ProbativeOperation> getOperations() {
        return operations;
    }

    @JsonProperty("checks")
    public List<ProbativeCheck> getChecks() {
        return checks;
    }

    @JsonProperty("evStartDateTime")
    public String getEvStartDateTime() {
        return evStartDateTime;
    }

    @JsonProperty("evEndDateTime")
    public String getEvEndDateTime() {
        return evEndDateTime;
    }

    @JsonProperty("status")
    public StatusCode getStatus() {
        return status;
    }
}
