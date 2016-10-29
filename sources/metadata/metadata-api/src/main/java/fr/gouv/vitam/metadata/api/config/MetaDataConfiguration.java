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
package fr.gouv.vitam.metadata.api.config;

import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;

/**
 * MetaDataConfiguration contains database access informations
 */
public final class MetaDataConfiguration extends DbConfigurationImpl {

    private String jettyConfig;
    private String clusterName;
    private List<ElasticsearchNode> elasticsearchNodes;

    /**
     * MetaDataConfiguration constructor
     *
     * @param host database server IP address
     * @param port database server port
     * @param dbName database name
     * @param clusterName 
     * @param elasticsearchNodes elasticsearch nodes
     * @param jettyConfig jetty config fiel name
     */
    public MetaDataConfiguration(String host, int port, String dbName, String clusterName,
        List<ElasticsearchNode> elasticsearchNodes, String jettyConfig) {
        super(host, port, dbName);
        ParametersChecker.checkParameter("elasticsearch cluster name is a mandatory parameter", clusterName);
        ParametersChecker.checkParameter("elasticsearch nodes are a mandatory parameter", elasticsearchNodes);
        ParametersChecker.checkParameter("JettyConfig name is a mandatory parameter", jettyConfig);
        this.clusterName = clusterName;
        this.elasticsearchNodes = elasticsearchNodes;
        this.jettyConfig = jettyConfig;
    }
    
    /**
     * MetaDataConfiguration constructor with authentication
     *
     * @param host database server IP address
     * @param port database server port
     * @param dbName database name
     * @param clusterName 
     * @param elasticsearchNodes elasticsearch nodes
     * @param jettyConfig jetty config fiel name
     * @param dbAuthentication 
     * @param dbUserName 
     * @param dbPassword 
     */
    public MetaDataConfiguration(String host, int port, String dbName, String clusterName,
        List<ElasticsearchNode> elasticsearchNodes, String jettyConfig, boolean dbAuthentication, String dbUserName, String dbPassword) {
        super(host, port, dbName, dbAuthentication, dbUserName, dbPassword);
        ParametersChecker.checkParameter("elasticsearch cluster name is a mandatory parameter", clusterName);
        ParametersChecker.checkParameter("elasticsearch nodes are a mandatory parameter", elasticsearchNodes);
        ParametersChecker.checkParameter("JettyConfig name is a mandatory parameter", jettyConfig);
        this.clusterName = clusterName;
        this.elasticsearchNodes = elasticsearchNodes;
        this.jettyConfig = jettyConfig;
    }

    /**
     * MetaDataConfiguration empty constructor for YAMLFactory
     */
    public MetaDataConfiguration() {}

    /**
     * getter jettyConfig
     *
     * @return return the jettyConfig
     */
    public String getJettyConfig() {
        return jettyConfig;
    }

    /**
     * setter jettyConfig
     *
     * @param jettyConfig the jetty config
     * @return return the jettyConfig
     */
    public MetaDataConfiguration setJettyConfig(String jettyConfig) {
        this.jettyConfig = jettyConfig;
        return this;
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
    public MetaDataConfiguration setClusterName(String clusterName) {
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
     * @return MetaDataConfiguration
     */
    public MetaDataConfiguration setElasticsearchNodes(List<ElasticsearchNode> elasticsearchNodes) {
        this.elasticsearchNodes = elasticsearchNodes;
        return this;
    }

}
