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
package fr.gouv.vitam.collect.internal.server;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;

import java.util.Map;

public class CollectConfiguration extends DbConfigurationImpl {

    private String workspaceUrl;

    private Map<Integer, Integer> statusTransactionDelayInMinutes;

    private int  statusTransactionThreadFrequency = 5;

    private Integer transactionStatusThreadPoolSize = 3;

    private Map<Integer, Integer> purgeTransactionDelayInMinutes;

    private int purgeTransactionThreadPoolSize = 3;

    private int purgeTransactionThreadFrequency = 60;

    /**
     * Must return the value of a 'workspaceUrl' attribute
     *
     * @return the workspaceUrl value
     */
    public String getWorkspaceUrl() {
        return workspaceUrl;
    }

    public Map<Integer, Integer> getPurgeTransactionDelayInMinutes() {
        return purgeTransactionDelayInMinutes;
    }

    public void setPurgeTransactionDelayInMinutes(
        Map<Integer, Integer> purgeTransactionDelayInMinutes) {
        this.purgeTransactionDelayInMinutes = purgeTransactionDelayInMinutes;
    }

    public int getPurgeTransactionThreadPoolSize() {
        return purgeTransactionThreadPoolSize;
    }

    public void setPurgeTransactionThreadPoolSize(int purgeTransactionThreadPoolSize) {
        this.purgeTransactionThreadPoolSize = purgeTransactionThreadPoolSize;
    }

    public int getPurgeTransactionThreadFrequency() {
        return purgeTransactionThreadFrequency;
    }

    public void setPurgeTransactionThreadFrequency(int purgeTransactionThreadFrequency) {
        this.purgeTransactionThreadFrequency = purgeTransactionThreadFrequency;
    }

    /**
     * @param workspaceUrl the workspace Url to set
     * @return this
     * @throws IllegalArgumentException if workspaceUrl is null or empty
     */
    public CollectConfiguration setWorkspaceUrl(String workspaceUrl) {
        ParametersChecker.checkParameter("workspaceUrl" + IS_A_MANDATORY_PARAMETER,
            workspaceUrl);
        this.workspaceUrl = workspaceUrl;
        return this;
    }

    public Map<Integer, Integer> getStatusTransactionDelayInMinutes() {
        return statusTransactionDelayInMinutes;
    }

    public void setStatusTransactionDelayInMinutes(
        Map<Integer, Integer> statusTransactionDelayInMinutes) {
        this.statusTransactionDelayInMinutes = statusTransactionDelayInMinutes;
    }

    public Integer getTransactionStatusThreadPoolSize() {
        return transactionStatusThreadPoolSize;
    }

    public void setTransactionStatusThreadPoolSize(Integer transactionStatusThreadPoolSize) {
        this.transactionStatusThreadPoolSize = transactionStatusThreadPoolSize;
    }

    public int getStatusTransactionThreadFrequency() {
        return statusTransactionThreadFrequency;
    }

    public void setStatusTransactionThreadFrequency(int statusTransactionThreadFrequency) {
        this.statusTransactionThreadFrequency = statusTransactionThreadFrequency;
    }
}
