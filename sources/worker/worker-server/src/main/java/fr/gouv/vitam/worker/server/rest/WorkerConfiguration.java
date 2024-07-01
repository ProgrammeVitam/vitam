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
package fr.gouv.vitam.worker.server.rest;

import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Worker configuration class mapping
 */
public final class WorkerConfiguration extends DefaultVitamApplicationConfiguration {

    private String processingUrl;
    private String urlMetadata;
    private String urlWorkspace;
    private String registerServerHost;
    private int registerServerPort;
    private long registerDelay = 60; //in seconds
    private int registerRetry = 5;
    private int capacity = 1;
    private String workerFamily = "DefaultWorker";
    private List<String> indexInheritedRulesWithRulesIdByTenant = new ArrayList<>();
    private List<String> indexInheritedRulesWithAPIV2OutputByTenant = new ArrayList<>();

    private int archiveUnitProfileCacheMaxEntries = 100;
    private int archiveUnitProfileCacheTimeoutInSeconds = 300;

    private int schemaValidatorCacheMaxEntries = 100;
    private int schemaValidatorCacheTimeoutInSeconds = 300;

    /**
     * WorkerConfiguration empty constructor for YAMLFactory
     */
    public WorkerConfiguration() {
        // Empty constructor
    }

    /**
     * Get the processingUrl
     *
     * @return processingUrl
     */
    public String getProcessingUrl() {
        return processingUrl;
    }

    /**
     * The processingUrl setter
     *
     * @param processingUrl the processingUrl
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setProcessingUrl(String processingUrl) {
        this.processingUrl = processingUrl;
        return this;
    }

    /**
     * Get the urlMetadata
     *
     * @return urlMetadata
     */
    public String getUrlMetadata() {
        return urlMetadata;
    }

    /**
     * The urlMetadata setter
     *
     * @param urlMetadata the urlMetadata
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setUrlMetadata(String urlMetadata) {
        this.urlMetadata = urlMetadata;
        return this;
    }

    /**
     * Get the urlWorkspace
     *
     * @return urlWorkspace
     */
    public String getUrlWorkspace() {
        return urlWorkspace;
    }

    /**
     * The urlWorkspace setter
     *
     * @param urlWorkspace the urlWorkspace
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setUrlWorkspace(String urlWorkspace) {
        this.urlWorkspace = urlWorkspace;
        return this;
    }

    /**
     * Get the registerServerHost
     *
     * @return registerServerHost
     */
    public String getRegisterServerHost() {
        return registerServerHost;
    }

    /**
     * The registerServerHost setter
     *
     * @param registerServerHost the registerServerHost
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setRegisterServerHost(String registerServerHost) {
        this.registerServerHost = registerServerHost;
        return this;
    }

    /**
     * Get the registerServerPort
     *
     * @return registerServerPort
     */
    public int getRegisterServerPort() {
        return registerServerPort;
    }

    /**
     * The registerServerPort setter
     *
     * @param registerServerPort the registerServerPort
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setRegisterServerPort(int registerServerPort) {
        this.registerServerPort = registerServerPort;
        return this;
    }

    /**
     * Get the registerDelay
     *
     * @return registerDelay
     */
    public long getRegisterDelay() {
        return registerDelay;
    }

    /**
     * The registerDelay setter
     *
     * @param registerDelay the registerDelay
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setRegisterDelay(long registerDelay) {
        this.registerDelay = registerDelay;
        return this;
    }

    /**
     * Get the registerRetry
     *
     * @return registerRetry
     */
    public int getRegisterRetry() {
        return registerRetry;
    }

    /**
     * The registerRetry setter
     *
     * @param registerRetry the registerRetry
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setRegisterRetry(int registerRetry) {
        this.registerRetry = registerRetry;
        return this;
    }

    /**
     * Return the capacity (number of parallel steps that can handle the worker)
     *
     * @return the capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Set the capacity (number of parallel steps that can handle the worker)
     *
     * @param capacity the capacity to set
     * @return this
     */
    public WorkerConfiguration setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * Return the workerFamily name
     *
     * @return workerFamily
     */
    public String getWorkerFamily() {
        return workerFamily;
    }

    /**
     * Set the workerFamily name of this worker (Default Value : DefaultWorker)
     *
     * @param workerFamily the worker family as String
     * @return this
     */
    public WorkerConfiguration setWorkerFamily(String workerFamily) {
        this.workerFamily = workerFamily;
        return this;
    }

    public List<String> getIndexInheritedRulesWithRulesIdByTenant() {
        return indexInheritedRulesWithRulesIdByTenant;
    }

    public void setIndexInheritedRulesWithRulesIdByTenant(List<String> indexInheritedRulesWithRulesIdByTenant) {
        this.indexInheritedRulesWithRulesIdByTenant = indexInheritedRulesWithRulesIdByTenant;
    }

    public List<String> getIndexInheritedRulesWithAPIV2OutputByTenant() {
        return indexInheritedRulesWithAPIV2OutputByTenant;
    }

    public void setIndexInheritedRulesWithAPIV2OutputByTenant(List<String> indexInheritedRulesWithAPIV2OutputByTenant) {
        this.indexInheritedRulesWithAPIV2OutputByTenant = indexInheritedRulesWithAPIV2OutputByTenant;
    }

    public int getArchiveUnitProfileCacheMaxEntries() {
        return archiveUnitProfileCacheMaxEntries;
    }

    public WorkerConfiguration setArchiveUnitProfileCacheMaxEntries(int archiveUnitProfileCacheMaxEntries) {
        this.archiveUnitProfileCacheMaxEntries = archiveUnitProfileCacheMaxEntries;
        return this;
    }

    public int getArchiveUnitProfileCacheTimeoutInSeconds() {
        return archiveUnitProfileCacheTimeoutInSeconds;
    }

    public WorkerConfiguration setArchiveUnitProfileCacheTimeoutInSeconds(int archiveUnitProfileCacheTimeoutInSeconds) {
        this.archiveUnitProfileCacheTimeoutInSeconds = archiveUnitProfileCacheTimeoutInSeconds;
        return this;
    }

    public int getSchemaValidatorCacheMaxEntries() {
        return schemaValidatorCacheMaxEntries;
    }

    public WorkerConfiguration setSchemaValidatorCacheMaxEntries(int schemaValidatorCacheMaxEntries) {
        this.schemaValidatorCacheMaxEntries = schemaValidatorCacheMaxEntries;
        return this;
    }

    public int getSchemaValidatorCacheTimeoutInSeconds() {
        return schemaValidatorCacheTimeoutInSeconds;
    }

    public WorkerConfiguration setSchemaValidatorCacheTimeoutInSeconds(int schemaValidatorCacheTimeoutInSeconds) {
        this.schemaValidatorCacheTimeoutInSeconds = schemaValidatorCacheTimeoutInSeconds;
        return this;
    }
}
