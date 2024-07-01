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
package fr.gouv.vitam.batch.report.model.entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class PreservationReportEntry extends ReportEntry {

    public static final String UNIT_ID = "unitId";
    public static final String OBJECT_GROUP_ID = "objectGroupId";
    public static final String PROCESS_ID = "processId";
    public static final String TENANT = "_tenant";
    public static final String CREATION_DATE_TIME = "creationDateTime";
    public static final String STATUS = "status";
    public static final String ACTION = "action";
    public static final String GRIFFIN_ID = "griffinId";
    public static final String SCENARIO_ID = "preservationScenarioId";
    public static final String ANALYSE_RESULT = "analyseResult";
    public static final String INPUT_OBJECT_ID = "inputObjectId";
    public static final String OUTPUT_OBJECT_ID = "outputObjectId";

    private final String processId;
    private final int tenant;
    private final String creationDateTime;
    private final PreservationStatus status;
    private final String unitId;
    private final String objectGroupId;
    private final ActionTypePreservation action;
    private final String analyseResult;
    private final String inputObjectId;
    private final String outputObjectId;
    private final String griffinId;
    private final String preservationScenarioId;

    @JsonCreator
    public PreservationReportEntry(
        @JsonProperty(DETAIL_ID) String detailId,
        @JsonProperty(PROCESS_ID) String processId,
        @JsonProperty(TENANT) int tenant,
        @JsonProperty(CREATION_DATE_TIME) String creationDateTime,
        @JsonProperty(STATUS) PreservationStatus status,
        @JsonProperty(UNIT_ID) String unitId,
        @JsonProperty(OBJECT_GROUP_ID) String objectGroupId,
        @JsonProperty(ACTION) ActionTypePreservation action,
        @JsonProperty(ANALYSE_RESULT) String analyseResult,
        @JsonProperty(INPUT_OBJECT_ID) String inputObjectId,
        @JsonProperty(OUTPUT_OBJECT_ID) String outputObjectId,
        @JsonProperty(OUTCOME) String outcome,
        @JsonProperty(GRIFFIN_ID) String griffinId,
        @JsonProperty(SCENARIO_ID) String preservationScenarioId
    ) {
        super(outcome, "preservation", detailId);
        this.processId = processId;
        this.tenant = tenant;
        this.creationDateTime = creationDateTime;
        this.status = status;
        this.unitId = unitId;
        this.objectGroupId = objectGroupId;
        this.action = action;
        this.analyseResult = analyseResult;
        this.inputObjectId = inputObjectId;
        this.outputObjectId = outputObjectId;
        this.griffinId = griffinId;
        this.preservationScenarioId = preservationScenarioId;
    }

    @JsonProperty(PROCESS_ID)
    public String getProcessId() {
        return processId;
    }

    @JsonProperty(TENANT)
    public int getTenant() {
        return tenant;
    }

    @JsonProperty(CREATION_DATE_TIME)
    public String getCreationDateTime() {
        return creationDateTime;
    }

    @JsonProperty(STATUS)
    public PreservationStatus getStatus() {
        return status;
    }

    @JsonProperty(UNIT_ID)
    public String getUnitId() {
        return unitId;
    }

    @JsonProperty(OBJECT_GROUP_ID)
    public String getObjectGroupId() {
        return objectGroupId;
    }

    @JsonProperty(ACTION)
    public ActionTypePreservation getAction() {
        return action;
    }

    @JsonProperty(ANALYSE_RESULT)
    public String getAnalyseResult() {
        return analyseResult;
    }

    @JsonProperty(INPUT_OBJECT_ID)
    public String getInputObjectId() {
        return inputObjectId;
    }

    @JsonProperty(OUTPUT_OBJECT_ID)
    public String getOutputObjectId() {
        return outputObjectId;
    }

    @JsonProperty(GRIFFIN_ID)
    public String getGriffinId() {
        return griffinId;
    }

    @JsonProperty(SCENARIO_ID)
    public String getPreservationScenarioId() {
        return preservationScenarioId;
    }
}
