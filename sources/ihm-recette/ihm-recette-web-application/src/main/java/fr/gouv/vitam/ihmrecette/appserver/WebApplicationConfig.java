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
package fr.gouv.vitam.ihmrecette.appserver;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.server.application.configuration.FunctionalAdminAdmin;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;

import java.util.List;

/**
 * Web Application Configuration class
 */
public class WebApplicationConfig extends MetaDataConfiguration {

    private int port;
    private String serverHost;
    private String baseUrl;
    private String baseUri;
    private String staticContent;
    private boolean secure;
    private List<String> secureMode;
    private String sipDirectory;
    private String performanceReportDirectory;
    private String masterdataDbName;
    private String logbookDbName;
    private String metadataDbName;
    private String testSystemSipDirectory;
    private String testSystemReportDirectory;
    private int ingestMaxThread;
    private FunctionalAdminAdmin functionalAdminAdmin;

    /**
     * Constructor for tests
     */
    WebApplicationConfig() {
        super();
    }

    /**
     * Default constructor (keep it ?)
     *
     * @param mongoDbNodes       nodes mongoDb
     * @param dbName             mongoDb name
     * @param clusterName        elastic search cluster name
     * @param elasticsearchNodes nodes elastic search
     */
    public WebApplicationConfig(List<MongoDbNode> mongoDbNodes, String dbName, String clusterName,
        List<ElasticsearchNode> elasticsearchNodes) {
        super(mongoDbNodes, dbName, clusterName, elasticsearchNodes);
    }

    /**
     * @return baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param baseUrl the base url
     */
    public WebApplicationConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public WebApplicationConfig setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    /**
     * @return static content
     */
    public String getStaticContent() {
        return staticContent;
    }

    /**
     * @param staticContent static content from server
     */
    public WebApplicationConfig setStaticContent(String staticContent) {
        this.staticContent = staticContent;
        return this;
    }

    /**
     * @return true if athentication needed, false otherwise
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * @param secure the secure access value
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * @return the directory path that contains SIPs ready for upload
     */
    public String getSipDirectory() {
        return sipDirectory;
    }

    /**
     * @param sipDirectory the directory path that contains SIPs ready for upload
     */
    public void setSipDirectory(String sipDirectory) {
        this.sipDirectory = sipDirectory;
    }

    /**
     * @return the server host
     */
    public String getServerHost() {
        return serverHost;
    }

    /**
     * @param serverHost the server host
     */
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    /**
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return masterdata database name
     */
    public String getMasterdataDbName() {
        return masterdataDbName;
    }

    /**
     * @param masterdataDbName masterdata database name
     */
    public void setMasterdataDbName(String masterdataDbName) {
        this.masterdataDbName = masterdataDbName;
    }

    /**
     * @return logbook database name
     */
    public String getLogbookDbName() {
        return logbookDbName;
    }

    /**
     * @param logbookDbName logbook database name
     */
    public void setLogbookDbName(String logbookDbName) {
        this.logbookDbName = logbookDbName;
    }

    /**
     * @return metadata database name
     */
    public String getMetadataDbName() {
        return metadataDbName;
    }

    /**
     * @param metadataDbName metadata database name
     */
    public void setMetadataDbName(String metadataDbName) {
        this.metadataDbName = metadataDbName;
    }

    /**
     * @return folder to store integration test
     */
    public String getTestSystemSipDirectory() {
        return testSystemSipDirectory;
    }

    /**
     * @param testSystemSipDirectory folder to store integration test
     */
    public void setTestSystemSipDirectory(String testSystemSipDirectory) {
        this.testSystemSipDirectory = testSystemSipDirectory;
    }

    /**
     * @return folder to store report of system test
     */
    public String getTestSystemReportDirectory() {
        return testSystemReportDirectory;
    }

    /**
     * @param testSystemReportDirectory folder to store report of system test
     */
    public void setTestSystemReportDirectory(String testSystemReportDirectory) {
        this.testSystemReportDirectory = testSystemReportDirectory;
    }

    /**
     * @return performance report directory
     */
    public String getPerformanceReportDirectory() {
        return performanceReportDirectory;
    }

    /**
     * @param performanceReportDirectory performance report directory
     */
    public void setPerformanceReportDirectory(String performanceReportDirectory) {
        this.performanceReportDirectory = performanceReportDirectory;
    }

    /**
     * get secure mode
     *
     * @return secure mode
     */
    public List<String> getSecureMode() {
        return secureMode;
    }

    /**
     * @param secureMode
     */
    public void setSecureMode(List<String> secureMode) {
        this.secureMode = secureMode;
    }

    public int getIngestMaxThread() {
        return ingestMaxThread;
    }

    public void setIngestMaxThread(int ingestMaxThread) {
        this.ingestMaxThread = ingestMaxThread;
    }

    public FunctionalAdminAdmin getFunctionalAdminAdmin() {
        return functionalAdminAdmin;
    }

    public void setFunctionalAdminAdmin(
        FunctionalAdminAdmin functionalAdminAdmin) {
        this.functionalAdminAdmin = functionalAdminAdmin;
    }
}
