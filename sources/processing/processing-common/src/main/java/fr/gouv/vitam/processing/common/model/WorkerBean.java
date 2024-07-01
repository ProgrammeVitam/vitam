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
package fr.gouv.vitam.processing.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.ParametersChecker;

/**
 * Worker class used for deserialize JSON file (root element)
 */
public class WorkerBean {

    @JsonProperty("name")
    private String name;

    @JsonProperty("workerId")
    private String workerId;

    @JsonProperty("family")
    private String family;

    @JsonProperty("capacity")
    private int capacity = 1;

    @JsonProperty("status")
    private String status;

    /**
     * Worker configuration
     */
    private WorkerRemoteConfiguration configuration;

    /**
     * Constructor for test purpose
     */
    WorkerBean() {}

    /**
     * @param name : the name of the worker
     * @param family : the family of the worker
     * @param capacity : the capacity of the worker
     * @param status : the status of the worker
     * @param configuration : the configuration of the worker
     */
    @JsonCreator
    public WorkerBean(
        @JsonProperty("name") String name,
        @JsonProperty("family") String family,
        @JsonProperty("capacity") int capacity,
        @JsonProperty("status") String status,
        @JsonProperty("configuration") WorkerRemoteConfiguration configuration
    ) {
        ParametersChecker.checkParameter("name is a mandatory parameter", name);
        ParametersChecker.checkParameter("family is a mandatory parameter", family);
        ParametersChecker.checkParameter("capacity is a mandatory parameter", capacity);
        ParametersChecker.checkParameter("status is a mandatory parameter", status);
        ParametersChecker.checkParameter("configuration is a mandatory parameter", configuration);

        this.name = name;
        this.family = family;
        this.capacity = capacity;
        this.status = status;
        this.configuration = configuration;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the worker name to set
     * @return the updated WorkerBean object
     */
    public WorkerBean setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the id of the worker
     */
    public String getWorkerId() {
        return workerId;
    }

    /**
     * @param workerId the workerId to set
     * @return the updated WorkerBean object
     */
    public WorkerBean setWorkerId(String workerId) {
        this.workerId = workerId;
        return this;
    }

    /**
     * @return the family
     */
    public String getFamily() {
        return family;
    }

    /**
     * @param family the worker Family to set
     * @return the updated WorkerBean object
     */
    public WorkerBean setFamily(String family) {
        this.family = family;
        return this;
    }

    /**
     * @return the capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * @param capacity the capacity to set
     * @return the updated WorkerBean object
     */
    public WorkerBean setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     * @return the updated WorkerBean object
     */
    public WorkerBean setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * @return the WorkerRemoteConfiguration including properties to connect to the Worker
     */
    public WorkerRemoteConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the WorkerRemoteConfiguration to set
     * @return the updated WorkerBean object
     */
    public WorkerBean setConfiguration(WorkerRemoteConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * toString : get the workerId, workerName, workerFamily, status on worker as String
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("workerId=" + getWorkerId() + "\n");
        sb.append("workerName=" + getName() + "\n");
        sb.append("workerFamily=" + getFamily() + "\n");
        sb.append("workerStatus=" + getStatus() + "\n");
        if (getConfiguration() != null) {
            sb.append("configuration = " + getConfiguration().toString() + "\n");
        }
        return sb.toString();
    }
}
