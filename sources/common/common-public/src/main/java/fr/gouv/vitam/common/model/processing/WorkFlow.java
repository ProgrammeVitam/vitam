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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * workflow class used for deserialize JSON file (root element)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkFlow {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("identifier")
    private String identifier;

    @JsonProperty("typeProc")
    private String typeProc;

    @JsonProperty("comment")
    private String comment;

    private LifecycleState lifecycleLog;

    /**
     * steps properties, must be defined in JSON file(required)
     */
    protected List<Step> steps;

    // Use only in mock and test
    public WorkFlow() {}

    @JsonCreator
    public WorkFlow(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("identifier") String identifier,
        @JsonProperty("typeProc") String typeProc,
        @JsonProperty("comment") String comment,
        @JsonProperty("lifecycleLog") LifecycleState lifecycleLog,
        @JsonProperty("steps") List<Step> steps
    ) {
        this.id = id;
        this.name = name;
        this.identifier = identifier;
        this.typeProc = typeProc;
        this.comment = comment;
        this.lifecycleLog = firstNonNull(lifecycleLog, LifecycleState.TEMPORARY);
        this.steps = steps;
        if (steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.forEach(step -> step.defaultLifecycleLog(this.lifecycleLog));
    }

    public static WorkFlow of(String id, String identifier, String evTypeProc) {
        WorkFlow workFlow = new WorkFlow();
        workFlow.setId(id);
        workFlow.setIdentifier(identifier);
        workFlow.setTypeProc(evTypeProc);
        return workFlow;
    }

    /**
     * getId, get id of workflow
     *
     * @return the workflowID
     */
    public String getId() {
        if (id == null) {
            return "";
        }
        return id;
    }

    /**
     * setId, set the id of workflow
     *
     * @param id as String
     * @return the WorkFlow instance with id setted
     */
    public WorkFlow setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * getName, get name of workflow
     *
     * @return the workflowName
     */
    public String getName() {
        if (name == null) {
            return "";
        }
        return name;
    }

    /**
     * setName, set the name of workflow
     *
     * @param name as String
     * @return the WorkFlow instance with name setted
     */
    public WorkFlow setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * getIdentifier, get identifier of workflow
     *
     * @return the workflowIdentifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * setIdentifier, set the identifier of workflow
     *
     * @param identifier as String
     * @return the WorkFlow instance with identifier setted
     */
    public WorkFlow setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * getTypeProc, get category of workflow
     *
     * @return the workflowType
     */
    public String getTypeProc() {
        if (typeProc == null) {
            return "";
        }
        return typeProc;
    }

    /**
     * setTypeProc, set the type of workflow
     *
     * @param typeProc as String
     * @return the WorkFlow instance with type setted
     */
    public WorkFlow setTypeProc(String typeProc) {
        this.typeProc = typeProc;
        return this;
    }

    /**
     * getComment
     *
     * @return comments on workflow
     */
    public String getComment() {
        if (comment == null) {
            return "";
        }
        return comment;
    }

    /**
     * getSteps(), get all step of workflow
     *
     * @return the list of type Step
     */
    public List<Step> getSteps() {
        if (steps == null) {
            return Collections.emptyList();
        }
        return steps;
    }

    /**
     * setSteps, set the steps to workflow
     *
     * @param steps as List
     * @return the Workflow instance with steps setted
     */
    public WorkFlow setSteps(List<Step> steps) {
        this.steps = steps;
        return this;
    }

    /**
     * setComment, set the comment for workflow
     *
     * @param comments of the workflow
     * @return the updated Workflow object
     */
    public WorkFlow setComment(String comments) {
        comment = comments;
        return this;
    }

    /**
     * toString : get the workflowId and comments on workflow as String
     */
    @Override
    public String toString() {
        return String.format(
            "ID=%s\nname=%s\nidentifier=%s\ntypeProc=%s\ncomments=%s\nlifecycleLog=%s\n",
            getId(),
            getName(),
            getIdentifier(),
            getTypeProc(),
            getComment(),
            getLifecycleLog()
        );
    }

    public LifecycleState getLifecycleLog() {
        return lifecycleLog;
    }

    public WorkFlow setLifecycleLog(LifecycleState lifecycleLog) {
        this.lifecycleLog = lifecycleLog;
        return this;
    }
}
