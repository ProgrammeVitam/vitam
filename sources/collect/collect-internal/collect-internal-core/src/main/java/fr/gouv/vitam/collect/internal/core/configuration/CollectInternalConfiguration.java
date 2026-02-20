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
package fr.gouv.vitam.collect.internal.core.configuration;

import com.google.common.annotations.Beta;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;

import java.util.Map;

public class CollectInternalConfiguration extends DbConfigurationImpl {

    private String urlWorkspace;
    private String urlWorkspaceCollect;

    private int statusTransactionThreadFrequency = 5;

    private Integer transactionStatusThreadPoolSize = 3;

    private Map<Integer, Integer> purgeTransactionDelayInMinutes;

    private int purgeTransactionThreadPoolSize = 3;

    private int purgeTransactionThreadFrequency = 60;

    @Beta
    private boolean applyJsltPostDynamicAttachement;

    private int bulkAtomicUpdateThreadPoolSize = 8;
    private int bulkAtomicUpdateThreadPoolQueueSize = 16;
    private int bulkAtomicUpdateBatchSize = 100;
    private int maxWaitDelayForTransactionValidationInSeconds = 3600; // Default to 1 hour

    /**
     * Must return the value of a 'urlWorkspace' attribute
     *
     * @return the urlWorkspace value
     */
    public String getUrlWorkspace() {
        return urlWorkspace;
    }

    public Map<Integer, Integer> getPurgeTransactionDelayInMinutes() {
        return purgeTransactionDelayInMinutes;
    }

    public void setPurgeTransactionDelayInMinutes(Map<Integer, Integer> purgeTransactionDelayInMinutes) {
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
     * @param urlWorkspace the workspace Url to set
     * @return this
     * @throws IllegalArgumentException if urlWorkspace is null or empty
     */
    public CollectInternalConfiguration setUrlWorkspace(String urlWorkspace) {
        ParametersChecker.checkParameter("urlWorkspace" + IS_A_MANDATORY_PARAMETER, urlWorkspace);
        this.urlWorkspace = urlWorkspace;
        return this;
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

    public int getBulkAtomicUpdateThreadPoolSize() {
        return bulkAtomicUpdateThreadPoolSize;
    }

    public CollectInternalConfiguration setBulkAtomicUpdateThreadPoolSize(int bulkAtomicUpdateThreadPoolSize) {
        this.bulkAtomicUpdateThreadPoolSize = bulkAtomicUpdateThreadPoolSize;
        return this;
    }

    public int getBulkAtomicUpdateThreadPoolQueueSize() {
        return bulkAtomicUpdateThreadPoolQueueSize;
    }

    public CollectInternalConfiguration setBulkAtomicUpdateThreadPoolQueueSize(
        int bulkAtomicUpdateThreadPoolQueueSize
    ) {
        this.bulkAtomicUpdateThreadPoolQueueSize = bulkAtomicUpdateThreadPoolQueueSize;
        return this;
    }

    public int getBulkAtomicUpdateBatchSize() {
        return bulkAtomicUpdateBatchSize;
    }

    public CollectInternalConfiguration setBulkAtomicUpdateBatchSize(int bulkAtomicUpdateBatchSize) {
        this.bulkAtomicUpdateBatchSize = bulkAtomicUpdateBatchSize;
        return this;
    }

    @Beta
    public boolean isApplyJsltPostDynamicAttachement() {
        return applyJsltPostDynamicAttachement;
    }

    @Beta
    public CollectInternalConfiguration setApplyJsltPostDynamicAttachement(boolean applyJsltPostDynamicAttachement) {
        this.applyJsltPostDynamicAttachement = applyJsltPostDynamicAttachement;
        return this;
    }

    public String getUrlWorkspaceCollect() {
        return urlWorkspaceCollect;
    }

    public CollectInternalConfiguration setUrlWorkspaceCollect(String urlWorkspaceCollect) {
        ParametersChecker.checkParameter("urlWorkspaceCollect" + IS_A_MANDATORY_PARAMETER, urlWorkspaceCollect);
        this.urlWorkspaceCollect = urlWorkspaceCollect;
        return this;
    }

    public int getMaxWaitDelayForTransactionValidationInSeconds() {
        return maxWaitDelayForTransactionValidationInSeconds;
    }

    public CollectInternalConfiguration setMaxWaitDelayForTransactionValidationInSeconds(
        int maxWaitDelayForTransactionValidationInSeconds
    ) {
        this.maxWaitDelayForTransactionValidationInSeconds = maxWaitDelayForTransactionValidationInSeconds;
        return this;
    }
}
