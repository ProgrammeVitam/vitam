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
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Query model to search in process.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessQuery {

    @JsonProperty("id")
    private String id;

    @JsonProperty("states")
    private List<String> states;

    @JsonProperty("statuses")
    private List<String> statuses;

    @JsonProperty("workflows")
    private List<String> workflows;

    @JsonProperty("listSteps")
    private List<String> listSteps;

    @JsonProperty("listProcessTypes")
    private List<String> listProcessTypes;

    // TODO: use LocaDate instead of String here, but need serializer / deserializer (see JavaTimeModule)
    @JsonProperty("startDateMin")
    private String startDateMin;

    // TODO: use LocaDate instead of String here, but need serializer / deserializer (see JavaTimeModule)
    @JsonProperty("startDateMax")
    private String startDateMax;

    /**
     * Constructor without fields use for jackson
     */
    public ProcessQuery() {}

    /**
     * @param id
     * @param states
     * @param statuses
     * @param workflows
     * @param listSteps
     * @param listProcessTypes
     * @param startDateMin
     * @param startDateMax
     */
    public ProcessQuery(
        String id,
        List<String> states,
        List<String> statuses,
        List<String> workflows,
        List<String> listSteps,
        List<String> listProcessTypes,
        String startDateMin,
        String startDateMax
    ) {
        this.id = id;
        this.states = states;
        this.statuses = statuses;
        this.workflows = workflows;
        this.listSteps = listSteps;
        this.listProcessTypes = listProcessTypes;
        this.startDateMin = startDateMin;
        this.startDateMax = startDateMax;
    }

    /**
     * Gets the id
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the states
     *
     * @return the states
     */
    public List<String> getStates() {
        return states;
    }

    /**
     * Gets the statuses
     *
     * @return the statuses
     */
    public List<String> getStatuses() {
        return statuses;
    }

    /**
     * Gets the workflows
     *
     * @return the workflows
     */
    public List<String> getWorkflows() {
        return workflows;
    }

    /**
     * Gets steps list
     *
     * @return the steps
     */
    public List<String> getListSteps() {
        return listSteps;
    }

    /**
     * Gets process types list
     *
     * @return the listProcessTypes
     */
    public List<String> getListProcessTypes() {
        return listProcessTypes;
    }

    /**
     * Gets the startDateMin
     *
     * @return the startDateMin
     */
    public String getStartDateMin() {
        return startDateMin;
    }

    /**
     * Gets the startDateMax
     *
     * @return the startDateMax
     */
    public String getStartDateMax() {
        return startDateMax;
    }

    /**
     * Sets the id
     *
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the states
     *
     * @param states the states to set
     */
    public void setStates(List<String> states) {
        this.states = states;
    }

    /**
     * Sets the statuses
     *
     * @param statuses the statuses to set
     */
    public void setStatuses(List<String> statuses) {
        this.statuses = statuses;
    }

    /**
     * Sets the workflows
     *
     * @param workflows the workflows to set
     */
    public void setWorkflows(List<String> workflows) {
        this.workflows = workflows;
    }

    /**
     * Sets the steps
     *
     * @param listSteps the steps to set
     */
    public void setListSteps(List<String> listSteps) {
        this.listSteps = listSteps;
    }

    /**
     * Sets the process types
     *
     * @param listProcessTypes the process types to set
     */
    public void setListProcessTypes(List<String> listProcessTypes) {
        this.listProcessTypes = listProcessTypes;
    }

    /**
     * Sets the startDateMin
     *
     * @param startDateMin the startDateMin to set
     */
    public void setStartDateMin(String startDateMin) {
        this.startDateMin = startDateMin;
    }

    /**
     * Sets the startDateMax
     *
     * @param startDateMax the startDateMax to set
     */
    public void setStartDateMax(String startDateMax) {
        this.startDateMax = startDateMax;
    }

    @Override
    public String toString() {
        return (
            "{id:" +
            id +
            ", states: " +
            states +
            ", statuses: " +
            statuses +
            ", workflows: " +
            workflows +
            ", steps: " +
            listSteps +
            ", min: " +
            startDateMin +
            ", max: " +
            startDateMax +
            "}"
        );
    }
}
