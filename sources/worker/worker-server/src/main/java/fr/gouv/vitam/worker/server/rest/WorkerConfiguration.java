/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/

package fr.gouv.vitam.worker.server.rest;

/**
 * Worker configuration class mapping
 */
public final class WorkerConfiguration {

    private String jettyConfig;
    private String processingUrl;
    private String urlMetada;
    private String urlWorkspace;
    private String registerServerHost;
    private int registerServerPort;
    private long registerDelay = 60;
    private int registerRetry = 5;

    /**
     * WorkerConfiguration empty constructor for YAMLFactory
     */
    public WorkerConfiguration() {
        // Empty constructor
    }

    /**
     * getter jettyConfig
     * 
     * @return the jettyConfig
     */
    public String getJettyConfig() {
        return jettyConfig;
    }

    /**
     * The jettyConfig setter
     * 
     * @param jettyConfig the jetty config
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setJettyConfig(String jettyConfig) {
        this.jettyConfig = jettyConfig;
        return this;
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
     * Get the urlMetada
     * 
     * @return urlMetada
     */
    public String getUrlMetada() {
        return urlMetada;
    }

    /**
     * The urlMetada setter
     * 
     * @param urlMetada the urlMetada
     * @return the updated WorkerConfiguration object
     */
    public WorkerConfiguration setUrlMetada(String urlMetada) {
        this.urlMetada = urlMetada;
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

}
