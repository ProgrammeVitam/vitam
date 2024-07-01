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
package fr.gouv.vitam.common.model.processing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.ItemStatus;

import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.model.processing.LifecycleState.DISABLED;

/**
 * Step Object in process workflow
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Step {

    @JsonProperty("id")
    private String id;

    @JsonProperty("workerGroupId")
    private String workerGroupId = "DefaultWorker";

    @JsonProperty("stepName")
    private String stepName;

    @JsonProperty("behavior")
    private ProcessBehavior behavior;

    @JsonProperty("distribution")
    private Distribution distribution;

    @JsonProperty("actions")
    private List<Action> actions;

    @JsonProperty("waitFor")
    private String waitFor;

    @JsonIgnore
    private ItemStatus stepResponses;

    private volatile PauseOrCancelAction pauseOrCancelAction = PauseOrCancelAction.ACTION_RUN;

    public Step() {}

    @JsonCreator
    public Step(
        @JsonProperty("id") String id,
        @JsonProperty("workerGroupId") String workerGroupId,
        @JsonProperty("stepName") String stepName,
        @JsonProperty("behavior") ProcessBehavior behavior,
        @JsonProperty("distribution") Distribution distribution,
        @JsonProperty("actions") List<Action> actions,
        @JsonProperty("waitFor") String waitFor
    ) {
        this.id = id;
        this.workerGroupId = workerGroupId;
        this.stepName = stepName;
        this.behavior = behavior;
        this.distribution = distribution;
        this.actions = actions;
        this.waitFor = waitFor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * getActions
     *
     * @return the list of actions to be executed for the step
     */
    public List<Action> getActions() {
        if (actions == null) {
            return Collections.emptyList();
        }
        return actions;
    }

    /**
     * @param actions the list of actions
     * @return the updated Step object
     */
    public Step setActions(List<Action> actions) {
        this.actions = actions;
        return this;
    }

    /**
     * @return workerGroupId the id of the WorkerGroup for the step
     */
    public String getWorkerGroupId() {
        if (workerGroupId == null) {
            return "";
        }
        return workerGroupId;
    }

    /**
     * @param workerGroupId the id of the WorkerGroup for the step
     * @return the updated Step object
     */
    public Step setWorkerGroupId(String workerGroupId) {
        this.workerGroupId = workerGroupId;
        return this;
    }

    /**
     * @return the step Name
     */
    public String getStepName() {
        if (stepName == null) {
            return "";
        }
        return stepName;
    }

    /**
     * @param stepName the step Name to set
     * @return the updated Step object
     */
    public Step setStepName(String stepName) {
        this.stepName = stepName;
        return this;
    }

    /**
     * getDistribution
     *
     * @return the distribution object of step
     */
    public Distribution getDistribution() {
        if (distribution == null) {
            return new Distribution();
        }
        return distribution;
    }

    /**
     * setDistribution
     *
     * @param distribution object
     * @return the Step instance with distribution value setted
     */
    public Step setDistribution(Distribution distribution) {
        this.distribution = distribution;
        return this;
    }

    /**
     * @return the stepType
     */
    public ProcessBehavior getBehavior() {
        return behavior;
    }

    /**
     * @param behavior the Process Behavior to set
     * @return the updated Step
     */
    public Step setBehavior(ProcessBehavior behavior) {
        this.behavior = behavior;
        return this;
    }

    /**
     * @return the stepResponses
     */
    public ItemStatus getStepResponses() {
        return stepResponses;
    }

    /**
     * @param stepResponses the stepResponses to set
     * @return this
     */
    public Step setStepResponses(ItemStatus stepResponses) {
        this.stepResponses = stepResponses;
        return this;
    }

    /**
     * @return True if this step is blocking
     */
    @JsonIgnore
    public boolean isBlocking() {
        return getBehavior().equals(ProcessBehavior.BLOCKING);
    }

    @JsonIgnore
    public boolean isFinally() {
        return getBehavior().equals(ProcessBehavior.FINALLY);
    }

    public PauseOrCancelAction getPauseOrCancelAction() {
        return pauseOrCancelAction;
    }

    public Step setPauseOrCancelAction(PauseOrCancelAction pauseOrCancelAction) {
        this.pauseOrCancelAction = pauseOrCancelAction;
        return this;
    }

    public void defaultLifecycleLog(LifecycleState lifecycleLog) {
        actions.forEach(action -> {
            boolean distributed = distribution.getKind().isDistributed();
            if (distributed) {
                action.getActionDefinition().defaultLifecycleLog(lifecycleLog);
            } else {
                action.getActionDefinition().defaultLifecycleLog(DISABLED);
            }
        });
    }

    public String getWaitFor() {
        return waitFor;
    }

    public void setWaitFor(String waitFor) {
        this.waitFor = waitFor;
    }
}
