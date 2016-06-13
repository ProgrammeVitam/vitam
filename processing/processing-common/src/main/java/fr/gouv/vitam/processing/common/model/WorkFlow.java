/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.common.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 *
 * workflow class used for deserialize JSON file (root element)
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkFlow {

    private String id;
    private String comment;

    /**
     * steps properties, must be defined in JSON file(required)
     */
    @JsonProperty("steps")
    protected List<Step> steps;

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
            return new ArrayList<>();
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
     * @param comments
     */
    public WorkFlow setComment(String comments) {
        comment = comments;
        return this;
    }

    /**
     * toString : get the wortflowId and comments on workflow as String
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ID=" + getId() + "\n");
        sb.append("comments=" + getComment() + "\n");
        return sb.toString();
    }

}
