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

package fr.gouv.vitam.logbook.common.server;

import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;

/**
 * Logbook configuration class mapping
 */
public final class LogbookConfiguration extends DbConfigurationImpl {
    // Empty

    private String p12LogbookPassword;

    private String p12LogbookFile;

    private String workspaceUrl;
    
    private String processingUrl;

    private String clusterName;

    private List<ElasticsearchNode> elasticsearchNodes;    
    
    private List<LogbookEvent> alertEvents;

    /**
     * The overlap delay (in seconds) for logbook operation traceability events. Used to catch up possibly missed events
     * due to clock difference.
     */
    private Integer operationTraceabilityOverlapDelay;

    /**
     * The overlap delay (in seconds) for logbook lifecyle traceability events. Used to catch up possibly missed events
     * due to clock difference.
     */
    private Integer lifecycleTraceabilityOverlapDelay;
    

    /**
     * LogbookConfiguration constructor
     *
     * @param mongoDbNodes database server IP addresses and ports
     * @param dbName database name
     * @param clusterName eslasticsearch cluster name
     * @param elasticsearchNodes elasticsearch nodes
     */
    public LogbookConfiguration(List<MongoDbNode> mongoDbNodes, String dbName, String clusterName,
        List<ElasticsearchNode> elasticsearchNodes) {
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
    public LogbookConfiguration(List<MongoDbNode> mongoDbNodes, String dbName, String clusterName,
        List<ElasticsearchNode> elasticsearchNodes, boolean dbAuthentication, String dbUserName, String dbPassword) {
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
     * Gets the overlap delay (in seconds) for logbook operation traceability events. Used to catch up possibly missed events
     * due to clock difference.
     * @return The overlap delay (in seconds).
     */
    public Integer getOperationTraceabilityOverlapDelay() {
        return operationTraceabilityOverlapDelay;
    }

    /**
     * Sets the overlap delay (in seconds) for logbook operation traceability events.
     */
    public void setOperationTraceabilityOverlapDelay(Integer operationTraceabilityOverlapDelay) {
        this.operationTraceabilityOverlapDelay = operationTraceabilityOverlapDelay;
    }

    /**
     * Gets the overlap delay (in seconds) for logbook lifecyle traceability events. Used to catch up possibly missed events
     * due to clock difference.
     * @return The overlap delay (in seconds).
     */
    public Integer getLifecycleTraceabilityOverlapDelay() {
        return lifecycleTraceabilityOverlapDelay;
    }

    /**
     * Sets the overlap delay (in seconds) for logbook lifecycle traceability events.
     */
    public void setLifecycleTraceabilityOverlapDelay(Integer lifecycleTraceabilityOverlapDelay) {
        this.lifecycleTraceabilityOverlapDelay = lifecycleTraceabilityOverlapDelay;
    }
}
