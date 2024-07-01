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
package fr.gouv.vitam.processing.common.config;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;

/**
 * ServerConfiguration class contains the different parameter {hostName ,ipAddress, port }to connect a remote server
 * such as workspace or metaData
 */
public class ServerConfiguration extends DefaultVitamApplicationConfiguration {

    private String urlMetadata;
    private String urlWorkspace;

    private static Integer DEFAULTSCHEDULER_PERIOD = 1; // one hour
    private Integer workflowRefreshPeriod = DEFAULTSCHEDULER_PERIOD;

    private Integer processingCleanerPeriod;

    private Integer maxDistributionInMemoryBufferSize = 100_000;
    private Integer maxDistributionOnDiskBufferSize = 100_000_000;

    private Integer delayAsyncResourceMonitor = 300; // five minutes
    private Integer delayAsyncResourceCleaner = 300; // five minutes

    /**
     * @return the urlMetadata
     */
    public String getUrlMetadata() {
        if (urlMetadata == null) {
            return "";
        }
        return urlMetadata;
    }

    /**
     * @param urlMetadata the urlMetadata to set
     * @return the update ServerConfiguration object
     */
    public ServerConfiguration setUrlMetadata(String urlMetadata) {
        ParametersChecker.checkParameter("urlMetadata is a mandatory parameter", urlMetadata);
        this.urlMetadata = urlMetadata;
        return this;
    }

    /**
     * @return the urlWorkspace
     */
    public String getUrlWorkspace() {
        if (urlWorkspace == null) {
            return "";
        }
        return urlWorkspace;
    }

    /**
     * @param urlWorkspace the urlWorkspace to set
     * @return the update ServerConfiguration object
     */
    public ServerConfiguration setUrlWorkspace(String urlWorkspace) {
        ParametersChecker.checkParameter("urlWorkspace is a mandatory parameter", urlWorkspace);
        this.urlWorkspace = urlWorkspace;
        return this;
    }

    /**
     * getWorkflowRefreshPeriod, getter for workflow refresh period
     *
     * @return workflow refresh period
     */
    public Integer getWorkflowRefreshPeriod() {
        return workflowRefreshPeriod;
    }

    /**
     * setWorkflowRefreshPeriod, setter for workflow refresh period
     *
     * @param workflowRefreshPeriod
     */
    public void setWorkflowRefreshPeriod(Integer workflowRefreshPeriod) {
        this.workflowRefreshPeriod = workflowRefreshPeriod;
    }

    /**
     * getter  for processing cleaner period
     *
     * @return
     */
    public Integer getProcessingCleanerPeriod() {
        return processingCleanerPeriod;
    }

    /**
     * setter  for processing cleaner period
     *
     * @param processingCleanerPeriod
     */
    public void setProcessingCleanerPeriod(Integer processingCleanerPeriod) {
        this.processingCleanerPeriod = processingCleanerPeriod;
    }

    public Integer getMaxDistributionInMemoryBufferSize() {
        return maxDistributionInMemoryBufferSize;
    }

    public ServerConfiguration setMaxDistributionInMemoryBufferSize(Integer maxDistributionInMemoryBufferSize) {
        this.maxDistributionInMemoryBufferSize = maxDistributionInMemoryBufferSize;
        return this;
    }

    public Integer getMaxDistributionOnDiskBufferSize() {
        return maxDistributionOnDiskBufferSize;
    }

    public ServerConfiguration setMaxDistributionOnDiskBufferSize(Integer maxDistributionOnDiskBufferSize) {
        this.maxDistributionOnDiskBufferSize = maxDistributionOnDiskBufferSize;
        return this;
    }

    public Integer getDelayAsyncResourceMonitor() {
        return delayAsyncResourceMonitor;
    }

    public ServerConfiguration setDelayAsyncResourceMonitor(Integer delayAsyncResourceMonitor) {
        this.delayAsyncResourceMonitor = delayAsyncResourceMonitor;
        return this;
    }

    public Integer getDelayAsyncResourceCleaner() {
        return delayAsyncResourceCleaner;
    }

    public ServerConfiguration setDelayAsyncResourceCleaner(Integer delayAsyncResourceCleaner) {
        this.delayAsyncResourceCleaner = delayAsyncResourceCleaner;
        return this;
    }
}
