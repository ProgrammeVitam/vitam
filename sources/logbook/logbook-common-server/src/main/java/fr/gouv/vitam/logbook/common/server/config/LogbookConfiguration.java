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
package fr.gouv.vitam.logbook.common.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Logbook configuration class mapping
 */
public final class LogbookConfiguration extends DbConfigurationImpl {

    private String p12LogbookPassword;

    private String p12LogbookFile;

    private String workspaceUrl;

    private String processingUrl;

    private String clusterName;

    private List<ElasticsearchNode> elasticsearchNodes;

    private List<LogbookEvent> alertEvents;

    /**
     * Temporization delay (in seconds) for recent logbook operation events.
     * Freshly created operation events are not secured right away to avoid missing events "not yet
     * commited" or with "server clock difference".
     */
    private Integer operationTraceabilityTemporizationDelay;

    /**
     * Max delay between 2 logbook operation traceability operations.
     * A new logbook operation traceability is required if tenant is active (new logbook operations,
     * other than traceability operations, are found), or last traceability operation is too old.
     */
    private Integer operationTraceabilityMaxRenewalDelay;
    private ChronoUnit operationTraceabilityMaxRenewalDelayUnit;

    /**
     * Temporization delay (in seconds) for recent logbook lifecycle events.
     * Freshly created lifecycle events are not secured right away to avoid missing events "not yet
     * commited" or with "server clock difference".
     */
    private Integer lifecycleTraceabilityTemporizationDelay;

    /**
     * Max delay between 2 LFC traceability operations.
     * A new LFC traceability is required if tenant is active (new LFCs are found),
     * or last traceability operation is too old.
     */
    private Integer lifecycleTraceabilityMaxRenewalDelay;
    private ChronoUnit lifecycleTraceabilityMaxRenewalDelayUnit;

    /**
     * Max event count to select during a single logbook lifecycle traceability workflow.
     */
    private Integer lifecycleTraceabilityMaxEntries;

    /**
     * Number of logbook operations that can be run in parallel.
     */
    private Integer operationTraceabilityThreadPoolSize;

    private int reconstructionMetricsCacheDurationInMinutes = 15;

    /**
     * List of events that are generated in a wf-operation but are not declared in the wf itself
     */
    private List<String> opEventsNotInWf;

    /**
     * List of operations that generate LFC (used for coherence check)
     */
    private List<String> opWithLFC;

    /**
     * List of events to skip for OP vs LFC check (used for coherence check)
     */
    private List<String> opLfcEventsToSkip;

    @JsonProperty("elasticsearchTenantIndexation")
    private LogbookIndexationConfiguration logbookTenantIndexation;

    /**
     * LogbookConfiguration constructor
     *
     * @param mongoDbNodes database server IP addresses and ports
     * @param dbName database name
     * @param clusterName eslasticsearch cluster name
     * @param elasticsearchNodes elasticsearch nodes
     */
    public LogbookConfiguration(
        List<MongoDbNode> mongoDbNodes,
        String dbName,
        String clusterName,
        List<ElasticsearchNode> elasticsearchNodes
    ) {
        super(mongoDbNodes, dbName);
        ParametersChecker.checkParameter("elasticsearch cluster name is a mandatory parameter", clusterName);
        ParametersChecker.checkParameter("elasticsearch nodes are a mandatory parameter", elasticsearchNodes);
        this.clusterName = clusterName;
        this.elasticsearchNodes = elasticsearchNodes;
    }

    /**
     * LogbookConfiguration constructor with db authentication
     *
     * @param mongoDbNodes database server IP addresses and ports
     * @param dbName database name
     * @param clusterName eslasticsearch cluster name
     * @param elasticsearchNodes elasticsearch nodes
     * @param dbAuthentication db authencation
     * @param dbUserName db authencation user
     * @param dbPassword db authencation password
     */
    public LogbookConfiguration(
        List<MongoDbNode> mongoDbNodes,
        String dbName,
        String clusterName,
        List<ElasticsearchNode> elasticsearchNodes,
        boolean dbAuthentication,
        String dbUserName,
        String dbPassword
    ) {
        super(mongoDbNodes, dbName, dbAuthentication, dbUserName, dbPassword);
        ParametersChecker.checkParameter("elasticsearch cluster name is a mandatory parameter", clusterName);
        ParametersChecker.checkParameter("elasticsearch nodes are a mandatory parameter", elasticsearchNodes);
        this.clusterName = clusterName;
        this.elasticsearchNodes = elasticsearchNodes;
    }

    /**
     * LogbookConfiguration empty constructor for YAMLFactory
     */
    public LogbookConfiguration() {}

    /**
     * @return password of p12
     */
    public String getP12LogbookPassword() {
        return p12LogbookPassword;
    }

    /**
     * @param p12LogbookPassword file to set
     */
    public void setP12LogbookPassword(String p12LogbookPassword) {
        this.p12LogbookPassword = p12LogbookPassword;
    }

    /**
     * @return p12 logbook file
     */
    public String getP12LogbookFile() {
        return p12LogbookFile;
    }

    /**
     * @param p12LogbookFile file to set
     */
    public void setP12LogbookFile(String p12LogbookFile) {
        this.p12LogbookFile = p12LogbookFile;
    }

    /**
     * @return url workspace
     */
    public String getWorkspaceUrl() {
        return workspaceUrl;
    }

    /**
     * @param workspaceUrl to set
     */
    public void setWorkspaceUrl(String workspaceUrl) {
        this.workspaceUrl = workspaceUrl;
    }

    /**
     * @return url processing
     */
    public String getProcessingUrl() {
        return processingUrl;
    }

    /**
     * @param processingUrl to set
     */
    public void setProcessingUrl(String processingUrl) {
        this.processingUrl = processingUrl;
    }

    /**
     * @return the clusterName
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @param clusterName the clusterName to set
     * @return this
     */
    public LogbookConfiguration setClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    /**
     * @return the elasticsearchNodes
     */
    public List<ElasticsearchNode> getElasticsearchNodes() {
        return elasticsearchNodes;
    }

    /**
     * @param elasticsearchNodes the elasticsearchNodes to set
     * @return LogbookConfiguration
     */
    public LogbookConfiguration setElasticsearchNodes(List<ElasticsearchNode> elasticsearchNodes) {
        this.elasticsearchNodes = elasticsearchNodes;
        return this;
    }

    /**
     * @return the alertEvents
     */
    public List<LogbookEvent> getAlertEvents() {
        return alertEvents;
    }

    /**
     * @param alertEvents to set
     */
    public void setAlertEvents(List<LogbookEvent> alertEvents) {
        this.alertEvents = alertEvents;
    }

    /**
     * @return opWithLFC
     */
    public List<String> getOpWithLFC() {
        return opWithLFC;
    }

    /**
     * @param opWithLFC to set
     */
    public void setOpWithLFC(List<String> opWithLFC) {
        this.opWithLFC = opWithLFC;
    }

    /**
     * @return opLfcEventsToSkip
     */
    public List<String> getOpLfcEventsToSkip() {
        return opLfcEventsToSkip;
    }

    /**
     * @param opLfcEventsToSkip to set
     */
    public void setOpLfcEventsToSkip(List<String> opLfcEventsToSkip) {
        this.opLfcEventsToSkip = opLfcEventsToSkip;
    }

    /**
     * @return opEventsNotInWf
     */
    public List<String> getOpEventsNotInWf() {
        return opEventsNotInWf;
    }

    /**
     * @param opEventsNotInWf to set
     */
    public void setOpEventsNotInWf(List<String> opEventsNotInWf) {
        this.opEventsNotInWf = opEventsNotInWf;
    }

    /**
     * Gets the temporization delay (in seconds) for recent logbook operation events.
     *
     * @return The temporization delay (in seconds).
     */
    public Integer getOperationTraceabilityTemporizationDelay() {
        return operationTraceabilityTemporizationDelay;
    }

    /**
     * Sets the temporization delay (in seconds) for recent logbook operation events.
     */
    public void setOperationTraceabilityTemporizationDelay(Integer operationTraceabilityTemporizationDelay) {
        this.operationTraceabilityTemporizationDelay = operationTraceabilityTemporizationDelay;
    }

    /**
     * Gets temporization delay (in seconds) for recent logbook lifecycle events
     */
    public Integer getLifecycleTraceabilityTemporizationDelay() {
        return lifecycleTraceabilityTemporizationDelay;
    }

    /**
     * Sets temporization delay (in seconds) for recent logbook lifecycle events
     */
    public void setLifecycleTraceabilityTemporizationDelay(Integer lifecycleTraceabilityTemporizationDelay) {
        this.lifecycleTraceabilityTemporizationDelay = lifecycleTraceabilityTemporizationDelay;
    }

    /**
     * Gets max event count to select during a single logbook lifecycle traceability workflow.
     */
    public Integer getLifecycleTraceabilityMaxEntries() {
        return lifecycleTraceabilityMaxEntries;
    }

    /**
     * Sets max event count to select during a single logbook lifecycle traceability workflow.
     */
    public void setLifecycleTraceabilityMaxEntries(Integer lifecycleTraceabilityMaxEntries) {
        this.lifecycleTraceabilityMaxEntries = lifecycleTraceabilityMaxEntries;
    }

    public Integer getOperationTraceabilityMaxRenewalDelay() {
        return operationTraceabilityMaxRenewalDelay;
    }

    public void setOperationTraceabilityMaxRenewalDelay(Integer operationTraceabilityMaxRenewalDelay) {
        this.operationTraceabilityMaxRenewalDelay = operationTraceabilityMaxRenewalDelay;
    }

    public ChronoUnit getOperationTraceabilityMaxRenewalDelayUnit() {
        return operationTraceabilityMaxRenewalDelayUnit;
    }

    public LogbookConfiguration setOperationTraceabilityMaxRenewalDelayUnit(
        ChronoUnit operationTraceabilityMaxRenewalDelayUnit
    ) {
        this.operationTraceabilityMaxRenewalDelayUnit = operationTraceabilityMaxRenewalDelayUnit;
        return this;
    }

    public Integer getLifecycleTraceabilityMaxRenewalDelay() {
        return lifecycleTraceabilityMaxRenewalDelay;
    }

    public void setLifecycleTraceabilityMaxRenewalDelay(Integer lifecycleTraceabilityMaxRenewalDelay) {
        this.lifecycleTraceabilityMaxRenewalDelay = lifecycleTraceabilityMaxRenewalDelay;
    }

    public ChronoUnit getLifecycleTraceabilityMaxRenewalDelayUnit() {
        return lifecycleTraceabilityMaxRenewalDelayUnit;
    }

    public void setLifecycleTraceabilityMaxRenewalDelayUnit(ChronoUnit lifecycleTraceabilityMaxRenewalDelayUnit) {
        this.lifecycleTraceabilityMaxRenewalDelayUnit = lifecycleTraceabilityMaxRenewalDelayUnit;
    }

    public Integer getOperationTraceabilityThreadPoolSize() {
        return operationTraceabilityThreadPoolSize;
    }

    public void setOperationTraceabilityThreadPoolSize(int operationTraceabilityThreadPoolSize) {
        this.operationTraceabilityThreadPoolSize = operationTraceabilityThreadPoolSize;
    }

    public LogbookIndexationConfiguration getLogbookTenantIndexation() {
        return logbookTenantIndexation;
    }

    public LogbookConfiguration setLogbookTenantIndexation(LogbookIndexationConfiguration logbookTenantIndexation) {
        this.logbookTenantIndexation = logbookTenantIndexation;
        return this;
    }

    public int getReconstructionMetricsCacheDurationInMinutes() {
        return reconstructionMetricsCacheDurationInMinutes;
    }

    public void setReconstructionMetricsCacheDurationInMinutes(int reconstructionMetricsCacheDurationInMinutes) {
        this.reconstructionMetricsCacheDurationInMinutes = reconstructionMetricsCacheDurationInMinutes;
    }
}
