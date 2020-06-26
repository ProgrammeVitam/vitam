/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.metadata.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;

import java.util.List;

/**
 * MetaDataConfiguration contains database access informations
 */
public class MetaDataConfiguration extends DbConfigurationImpl {

    private String workspaceUrl;
    private String urlProcessing;
    private String clusterName;
    private List<ElasticsearchNode> elasticsearchNodes;

    private int archiveUnitProfileCacheMaxEntries = 100;
    private int archiveUnitProfileCacheTimeoutInSeconds = 300;

    private int schemaValidatorCacheMaxEntries = 100;
    private int schemaValidatorCacheTimeoutInSeconds = 300;

    private int dipTimeToLiveInMinutes = 60 * 24 * 7;
    private int transfersSIPTimeToLiveInMinutes = 60 * 24 * 7;

    private List<ElasticsearchExternalMetadataMapping> elasticsearchExternalMetadataMappings;

    @JsonProperty("elasticsearchTenantIndexation")
    private MetadataIndexationConfiguration indexationConfiguration;

    /**
     * MetaDataConfiguration constructor
     *
     * @param mongoDbNodes database server IP addresses and ports
     * @param dbName database name
     * @param clusterName cluster name
     * @param elasticsearchNodes elasticsearch nodes
     */
    public MetaDataConfiguration(List<MongoDbNode> mongoDbNodes, String dbName, String clusterName,
        List<ElasticsearchNode> elasticsearchNodes, MappingLoader mappingLoader) {
        super(mongoDbNodes, dbName);
        ParametersChecker.checkParameter("elasticsearch cluster name is a mandatory parameter", clusterName);
        ParametersChecker.checkParameter("elasticsearch nodes are a mandatory parameter", elasticsearchNodes);
        this.clusterName = clusterName;
        this.elasticsearchNodes = elasticsearchNodes;
        this.elasticsearchExternalMetadataMappings = mappingLoader.getElasticsearchExternalMappings();
    }

    /**
     * MetaDataConfiguration constructor with authentication
     *
     * @param mongoDbNodes database server IP addresses and ports
     * @param dbName database name
     * @param clusterName cluster name
     * @param elasticsearchNodes elasticsearch nodes
     * @param dbAuthentication if authentication mode
     * @param dbUserName db user name
     * @param dbPassword db password
     */
    public MetaDataConfiguration(List<MongoDbNode> mongoDbNodes, String dbName, String clusterName,
        List<ElasticsearchNode> elasticsearchNodes, boolean dbAuthentication, String dbUserName, String dbPassword, MappingLoader mappingLoader) {
        super(mongoDbNodes, dbName, dbAuthentication, dbUserName, dbPassword);
        ParametersChecker.checkParameter("elasticsearch cluster name is a mandatory parameter", clusterName);
        ParametersChecker.checkParameter("elasticsearch nodes are a mandatory parameter", elasticsearchNodes);
        this.clusterName = clusterName;
        this.elasticsearchNodes = elasticsearchNodes;
        this.elasticsearchExternalMetadataMappings = mappingLoader.getElasticsearchExternalMappings();
    }

    /**
     * MetaDataConfiguration empty constructor for YAMLFactory
     */
    public MetaDataConfiguration() {
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


    public String getWorkspaceUrl() {
        return workspaceUrl;
    }

    public MetaDataConfiguration setWorkspaceUrl(String workspaceUrl) {
        this.workspaceUrl = workspaceUrl;
        return this;
    }

    public int getArchiveUnitProfileCacheMaxEntries() {
        return archiveUnitProfileCacheMaxEntries;
    }

    public MetaDataConfiguration setArchiveUnitProfileCacheMaxEntries(int archiveUnitProfileCacheMaxEntries) {
        this.archiveUnitProfileCacheMaxEntries = archiveUnitProfileCacheMaxEntries;
        return this;
    }

    public int getArchiveUnitProfileCacheTimeoutInSeconds() {
        return archiveUnitProfileCacheTimeoutInSeconds;
    }

    public MetaDataConfiguration setArchiveUnitProfileCacheTimeoutInSeconds(
        int archiveUnitProfileCacheTimeoutInSeconds) {
        this.archiveUnitProfileCacheTimeoutInSeconds = archiveUnitProfileCacheTimeoutInSeconds;
        return this;
    }

    public int getSchemaValidatorCacheMaxEntries() {
        return schemaValidatorCacheMaxEntries;
    }

    public MetaDataConfiguration setSchemaValidatorCacheMaxEntries(int schemaValidatorCacheMaxEntries) {
        this.schemaValidatorCacheMaxEntries = schemaValidatorCacheMaxEntries;
        return this;
    }

    public int getSchemaValidatorCacheTimeoutInSeconds() {
        return schemaValidatorCacheTimeoutInSeconds;
    }

    public MetaDataConfiguration setSchemaValidatorCacheTimeoutInSeconds(int schemaValidatorCacheTimeoutInSeconds) {
        this.schemaValidatorCacheTimeoutInSeconds = schemaValidatorCacheTimeoutInSeconds;
        return this;
    }

    public String getUrlProcessing() {
        return urlProcessing;
    }

    public void setUrlProcessing(String urlProcessing) {
        this.urlProcessing = urlProcessing;
    }

    public int getDipTimeToLiveInMinutes() {
        return dipTimeToLiveInMinutes;
    }

    public MetaDataConfiguration setDipTimeToLiveInMinutes(int dipTimeToLiveInMinutes) {
        this.dipTimeToLiveInMinutes = dipTimeToLiveInMinutes;
        return this;
    }

    public int getTransfersSIPTimeToLiveInMinutes() {
        return transfersSIPTimeToLiveInMinutes;
    }

    public void setTransfersSIPTimeToLiveInMinutes(int transfersSIPTimeToLiveInMinutes) {
        this.transfersSIPTimeToLiveInMinutes = transfersSIPTimeToLiveInMinutes;
    }

    public List<ElasticsearchExternalMetadataMapping> getElasticsearchExternalMetadataMappings() {
        return elasticsearchExternalMetadataMappings;
    }

    public void setElasticsearchExternalMetadataMappings(
        List<ElasticsearchExternalMetadataMapping> elasticsearchExternalMetadataMappings) {
        this.elasticsearchExternalMetadataMappings = elasticsearchExternalMetadataMappings;
    }

    public MetadataIndexationConfiguration getIndexationConfiguration() {
        return indexationConfiguration;
    }

    public MetaDataConfiguration setIndexationConfiguration(
        MetadataIndexationConfiguration indexationConfiguration) {
        this.indexationConfiguration = indexationConfiguration;
        return this;
    }
}
