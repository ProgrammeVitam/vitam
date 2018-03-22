/**
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
 */
package fr.gouv.vitam.common;

import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;


/**
 * This class contains Vitam Data
 */
public class VitamConfigurationParameters {

    private Boolean exportScore;
    private String secret;
    private Boolean filterActivation = true;
    private Integer maxConcurrentMultipleInputstreamHandler;

    /**
     * Distributor batch size
     */
    private Integer distributeurBatchSize;
    /**
     * Worker bulk size
     */
    private Integer workerBulkSize;    
    /**
     * Restore bulk size
     */
    private Integer restoreBulkSize;

    /**
     *
     */
    private Integer maxElasticsearchBulk;
    /**
     *
     */
    private Integer numberDbClientThread;
    /**
     *
     */
    private Integer numberEsQueue;
    private Integer maxCacheEntries;
    private Integer cacheControlDelay;

    private Integer adminTenant;

    private List<Integer> tenants;

    /**
     * /**
     * Default Vitam Config Folder
     */
    private String vitamConfigFolderDefault;
    /**
     * Default Vitam Config Folder
     */
    private String vitamDataFolderDefault;
    /**
     * Default Vitam Config Folder
     */
    private String vitamLogFolderDefault;
    /**
     * Default Vitam Config Folder
     */
    private String vitamTmpFolderDefault;
    /**
     * Default Chunk Size
     */
    private Integer chunkSize;

    /**
     * Default Async Workspace Queue Size
     */
    private Integer asyncWorkspaceQueueSize;

    /**
     * Default Recv Buffer Size
     */
    private Integer recvBufferSize;
    /**
     * Default Connection timeout
     */
    private Integer connectTimeout;
    /**
     * Default Read Timeout
     */
    private Integer readTimeout;

    /**
     * Max total concurrent clients
     */
    private Integer maxTotalClient;
    /**
     * Max concurrent clients associated to one host
     */
    private Integer maxClientPerHost;
    /**
     * Max delay to check an unused client in pool before being returned (Apache Only)
     */
    private Integer delayValidationAfterInactivity;
    /**
     * Max delay to check if no buffer is available while trying to continue to read (MultipleInputStreamHandler Only)
     * Not final to allow Junit to decrease it
     */
    private Integer delayMultipleInputstream;
    /**
     * Max delay to check if no buffer is available while trying to continue to read (SubStreams Only)
     */
    private Integer delayMultipleSubinputstream;
    /**
     * Default minimum thread pool size
     */
    private Integer minimumThreadPoolSize;
    /**
     * No check of unused client within pool (Apache Only)
     */
    private Integer noValidationAfterInactivity;
    /**
     * Max delay to get a client (Apache Only)
     */
    private Integer delayGetClient;
    /**
     * Specify the delay where connections returned to pool will be checked (Apache Only)
     */
    private Integer intervalDelayCheckIdle;
    /**
     * Specify the delay of unused connection returned in the pool before being really closed (Apache Only)
     */
    private Integer maxDelayUnusedConnection;
    /**
     * Use a new JAX_RS client each time
     */
    private Boolean useNewJaxrClient;

    /**
     * Default Digest Type for SECURITY
     */
    private String securityDigestType;
    /**
     * Default Digest Type for Vitam
     */
    private String defaultDigestType;
    /**
     * Default Digest Type for time stamp generation
     */
    private String defaultTimestampDigestType;
    /**
     * Acceptable Request Time elaps
     */
    private Long acceptableRequestTime;
    /**
     * MongoDB client configuration
     */
    private Integer threadsAllowedToBlockForConnectionMultipliers;
    /**
     * Retry repetition
     */
    private Integer retryNumber;
    /**
     * Retry delay
     */
    private Integer retryDelay;
    /**
     * Waiting delay (for wait(delay) method)
     */
    private Integer waitingDelay;
    /**
     * Allow client and Server Encoding request or response in GZIP format
     */
    private Boolean allowGzipEncoding;
    /**
     * Allow client to receive GZIP encoded response
     */
    private Boolean allowGzipDecoding;
    /**
     * Read ahead x4 Buffers
     */
    private Integer BUFFER_NUMBER;

    /**
     * default originatingAgency for DIP export when multiple originatingAgencies are in conflict in exported AU
     */
    private Map<Integer, String> defaultOriginatingAgencyForExport;
    
    /**
     * Setter for   vitamCleanPeriod;
     */
    public void setVitamCleanPeriod(Integer vitamCleanPeriod) {
        this.vitamCleanPeriod = vitamCleanPeriod;
    }

    /**
     * Vitam Clean period (In hours)
     */
    private Integer vitamCleanPeriod;

    /**
     * VitamData empty constructor for YAMLFactory
     */
    public VitamConfigurationParameters() {
        // empty
    }


    /**
     * Must return the value of a 'secret' attribute
     *
     * @return the secret value
     */
    public String getSecret() {

        if (Strings.isNullOrEmpty(secret)) {
            return "";
        }
        return secret;
    }

    /**
     * @param secret the Platform secret to set
     * @return this
     * @throws IllegalArgumentException if secret is Null Or Empty
     */
    public VitamConfigurationParameters setSecret(String secret) {
        ParametersChecker.checkParameter("Platform secret", secret);
        this.secret = secret;
        return this;
    }



    /**
     * Must return the value of a 'filterActivation' attribute
     *
     * @return the filterActivation
     */
    public Boolean isFilterActivation() {
        return filterActivation;
    }


    /**
     * @param filterActivation the filterActivation to set
     * @return this
     */
    public VitamConfigurationParameters setFilterActivation(Boolean filterActivation) {
        this.filterActivation = filterActivation;
        return this;
    }



    /**
     * Getter for   vitamConfigFolderDefault;
     */
    public String getVitamConfigFolderDefault() {
        return vitamConfigFolderDefault;
    }

    /**
     * Setter for   vitamConfigFolderDefault;
     */
    public void setVitamConfigFolderDefault(String vitamConfigFolderDefault) {
        this.vitamConfigFolderDefault = vitamConfigFolderDefault;
    }

    /**
     * Getter for   vitamDataFolderDefault;
     */
    public String getVitamDataFolderDefault() {
        return vitamDataFolderDefault;
    }

    /**
     * Setter for   vitamDataFolderDefault;
     */
    public void setVitamDataFolderDefault(String vitamDataFolderDefault) {
        this.vitamDataFolderDefault = vitamDataFolderDefault;
    }

    /**
     * Getter for   vitamLogFolderDefault;
     */
    public String getVitamLogFolderDefault() {
        return vitamLogFolderDefault;
    }

    /**
     * Setter for   vitamLogFolderDefault;
     */
    public void setVitamLogFolderDefault(String vitamLogFolderDefault) {
        this.vitamLogFolderDefault = vitamLogFolderDefault;
    }

    /**
     * Getter for   vitamTmpFolderDefault;
     */
    public String getVitamTmpFolderDefault() {
        return vitamTmpFolderDefault;
    }

    /**
     * Setter for   vitamTmpFolderDefault;
     */
    public void setVitamTmpFolderDefault(String vitamTmpFolderDefault) {
        this.vitamTmpFolderDefault = vitamTmpFolderDefault;
    }

    /**
     * Getter for   CHUNK_SIZE;
     */
    public Integer getChunkSize() {
        return chunkSize;
    }

    /**
     * Setter for   chunkSize;
     */
    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * @return the size of the queue of async workspace
     */
    public Integer getAsyncWorkspaceQueueSize() {
        return asyncWorkspaceQueueSize;
    }


    /**
     * @return the size of the queue of async workspace
     */
    public void setAsyncWorkspaceQueueSize(int queueSize) {
        asyncWorkspaceQueueSize = queueSize;
    }


    /**
     * Getter for   recvBufferSize;
     */
    public Integer getRecvBufferSize() {
        return recvBufferSize;
    }

    /**
     * Setter for   recvBufferSize;
     */
    public void setRecvBufferSize(Integer recvBufferSize) {
        this.recvBufferSize = recvBufferSize;
    }

    /**
     * Getter for   connectTimeout;
     */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Setter for   connectTimeout;
     */
    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Getter for   readTimeout;
     */
    public Integer getReadTimeout() {
        return readTimeout;
    }

    /**
     * Setter for   readTimeout;
     */
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Getter for   maxTotalClient;
     */
    public Integer getMaxTotalClient() {
        return maxTotalClient;
    }

    /**
     * Setter for   maxTotalClient;
     */
    public void setMaxTotalClient(Integer maxTotalClient) {
        this.maxTotalClient = maxTotalClient;
    }

    /**
     * Getter for   maxClientPerHost;
     */
    public Integer getMaxClientPerHost() {
        return maxClientPerHost;
    }

    /**
     * Setter for   maxClientPerHost;
     */
    public void setMaxClientPerHost(Integer maxClientPerHost) {
        this.maxClientPerHost = maxClientPerHost;
    }

    /**
     * Getter for   delayValidationAfterInactivity;
     */
    public Integer getDelayValidationAfterInactivity() {
        return delayValidationAfterInactivity;
    }

    /**
     * Setter for   delayValidationAfterInactivity;
     */
    public void setDelayValidationAfterInactivity(Integer delayValidationAfterInactivity) {
        this.delayValidationAfterInactivity = delayValidationAfterInactivity;
    }

    /**
     * Getter for   delayMultipleInputstream;
     */
    public Integer getDelayMultipleInputstream() {
        return delayMultipleInputstream;
    }

    /**
     * Setter for   delayMultipleInputstream;
     */
    public void setDelayMultipleInputstream(Integer delayMultipleInputstream) {
        this.delayMultipleInputstream = delayMultipleInputstream;
    }

    /**
     * Getter for   delayMultipleSubinputstream;
     */
    public Integer getDelayMultipleSubinputstream() {
        return delayMultipleSubinputstream;
    }

    /**
     * Setter for   delayMultipleSubinputstream;
     */
    public void setDelayMultipleSubinputstream(Integer delayMultipleSubinputstream) {
        this.delayMultipleSubinputstream = delayMultipleSubinputstream;
    }

    /**
     * Getter for   minimumThreadPoolSize;
     */
    public Integer getMinimumThreadPoolSize() {
        return minimumThreadPoolSize;
    }

    /**
     * Setter for   minimumThreadPoolSize;
     */
    public void setMinimumThreadPoolSize(Integer minimumThreadPoolSize) {
        this.minimumThreadPoolSize = minimumThreadPoolSize;
    }

    /**
     * Getter for   noValidationAfterInactivity;
     */
    public Integer getNoValidationAfterInactivity() {
        return noValidationAfterInactivity;
    }

    /**
     * Setter for   noValidationAfterInactivity;
     */
    public void setNoValidationAfterInactivity(Integer noValidationAfterInactivity) {
        this.noValidationAfterInactivity = noValidationAfterInactivity;
    }

    /**
     * Getter for   delayGetClient;
     */
    public Integer getDelayGetClient() {
        return delayGetClient;
    }

    /**
     * Setter for   delayGetClient;
     */
    public void setDelayGetClient(Integer delayGetClient) {
        this.delayGetClient = delayGetClient;
    }

    /**
     * Getter for   intervalDelayCheckIdle;
     */
    public Integer getIntervalDelayCheckIdle() {
        return intervalDelayCheckIdle;
    }

    /**
     * Setter for   IntegerERVAL_DELAY_CHECK_IDLE;
     */
    public void setIntegerervalDelayCheckIdle(Integer IntegerervalDelayCheckIdle) {
        intervalDelayCheckIdle = IntegerervalDelayCheckIdle;
    }

    /**
     * Getter for   maxDelayUnusedConnection;
     */
    public Integer getMaxDelayUnusedConnection() {
        return maxDelayUnusedConnection;
    }

    /**
     * Setter for   maxDelayUnusedConnection;
     */
    public void setMaxDelayUnusedConnection(Integer maxDelayUnusedConnection) {
        this.maxDelayUnusedConnection = maxDelayUnusedConnection;
    }

    /**
     * Getter for   useNewJaxrClient;
     */
    public Boolean isUseNewJaxrClient() {
        return useNewJaxrClient;
    }

    /**
     * Setter for   useNewJaxrClient;
     */
    public void setUseNewJaxrClient(Boolean useNewJaxrClient) {
        this.useNewJaxrClient = useNewJaxrClient;
    }

    /**
     * Getter for   securityDigestType;
     */
    public String getSecurityDigestType() {
        return securityDigestType;
    }

    /**
     * Setter for   securityDigestType;
     */
    public void setSecurityDigestType(String securityDigestType) {
        this.securityDigestType = securityDigestType;
    }

    /**
     * Getter for   defaultDigestType;
     */
    public String getDefaultDigestType() {
        return defaultDigestType;
    }

    /**
     * Setter for   defaultDigestType;
     */
    public void setDefaultDigestType(String defaultDigestType) {
        this.defaultDigestType = defaultDigestType;
    }

    /**
     * Getter for   defaultTimestampDigestType;
     */
    public String getDefaultTimestampDigestType() {
        return defaultTimestampDigestType;
    }

    /**
     * Setter for   defaultTimestampDigestType;
     */
    public void setDefaultTimestampDigestType(String defaultTimestampDigestType) {
        this.defaultTimestampDigestType = defaultTimestampDigestType;
    }

    /**
     * Getter for   acceptableRequestTime;
     */
    public Long getAcceptableRequestTime() {
        return acceptableRequestTime;
    }

    /**
     * Setter for   acceptableRequestTime;
     */
    public void setAcceptableRequestTime(Long acceptableRequestTime) {
        this.acceptableRequestTime = acceptableRequestTime;
    }

    /**
     * Getter for   threadsAllowedToBlockForConnectionMultipliers;
     */
    public Integer getThreadsAllowedToBlockForConnectionMultipliers() {
        return threadsAllowedToBlockForConnectionMultipliers;
    }

    /**
     * Setter for   threadsAllowedToBlockForConnectionMultipliers;
     */
    public void setThreadsAllowedToBlockForConnectionMultipliers(
        Integer threadsAllowedToBlockForConnectionMultipliers) {
        this.threadsAllowedToBlockForConnectionMultipliers = threadsAllowedToBlockForConnectionMultipliers;
    }

    /**
     * Getter for   retryNumber;
     */
    public Integer getRetryNumber() {
        return retryNumber;
    }

    /**
     * Setter for   retryNumber;
     */
    public void setRetryNumber(Integer retryNumber) {
        this.retryNumber = retryNumber;
    }

    /**
     * Getter for   retryDelay;
     */
    public Integer getRetryDelay() {
        return retryDelay;
    }

    /**
     * Setter for   retryDelay;
     */
    public void setRetryDelay(Integer retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * Getter for   waitingDelay;
     */
    public Integer getWaitingDelay() {
        return waitingDelay;
    }

    /**
     * Setter for   waitingDelay;
     */
    public void setWaitingDelay(Integer waitingDelay) {
        this.waitingDelay = waitingDelay;
    }

    /**
     * Getter for   allowGzipEncoding;
     */
    public Boolean isAllowGzipEncoding() {
        return allowGzipEncoding;
    }

    /**
     * Setter for   allowGzipEncoding;
     */
    public void setAllowGzipEncoding(Boolean allowGzipEncoding) {
        this.allowGzipEncoding = allowGzipEncoding;
    }

    /**
     * Getter for   allowGzipDecoding;
     */
    public Boolean isAllowGzipDecoding() {
        return allowGzipDecoding;
    }

    /**
     * Setter for   allowGzipDecoding;
     */
    public void setAllowGzipDecoding(Boolean allowGzipDecoding) {
        this.allowGzipDecoding = allowGzipDecoding;
    }

    /**
     * Getter for   BUFFER_NUMBER;
     */
    public Integer getBufferNumber() {
        return BUFFER_NUMBER;
    }

    /**
     * Setter for   BUFFER_NUMBER;
     */
    public void setBufferNumber(Integer bufferNumber) {
        BUFFER_NUMBER = bufferNumber;
    }

    /**
     * getter for maxConcurrentMultipleInputstreamHandler
     *
     * @return
     */
    public Integer getMaxConcurrentMultipleInputstreamHandler() {
        return maxConcurrentMultipleInputstreamHandler;
    }

    /**
     * setter for maxConcurrentMultipleInputstreamHandler
     *
     * @return
     */
    public void setMaxConcurrentMultipleInputstreamHandler(int maxConcurrentMultipleInputstreamHandler) {
        this.maxConcurrentMultipleInputstreamHandler = maxConcurrentMultipleInputstreamHandler;
    }

    public Integer getVitamCleanPeriod() {
        return vitamCleanPeriod;
    }


    /**
     * Getter for   exportScore;
     */
    public Boolean isExportScore() {
        return exportScore;
    }

    /**
     * Setter for   exportScore;
     */
    public void setExportScore(boolean exportScore) {
        this.exportScore = exportScore;
    }



    /**
     * Getter for   distributeurBatchSize;
     */
    public Integer getDistributeurBatchSize() {
        return distributeurBatchSize;
    }

    /**
     * Setter for   distributeurBatchSize;
     */
    public void setDistributeurBatchSize(Integer distributeurBatchSize) {
        this.distributeurBatchSize = distributeurBatchSize;
    }


    /**
     * Getter for worker bulk size
     * @return
     */
    public Integer getWorkerBulkSize() {
        return workerBulkSize;
    }

    /**
     * Setter for worker bulk size
     * @param workerBulkSize
     */
    public void setWorkerBulkSize(Integer workerBulkSize) {
        this.workerBulkSize = workerBulkSize;
    }

    /**
    * Getter for restore bulk size
    * 
    * @return
    */
   public Integer getRestoreBulkSize() {
       return restoreBulkSize;
   }

   /**
    * Getter for restore bulk size
    * 
    * @return restoreBulkSize
    */
   public void setRestoreBulkSize(int restoreBulkSize) {
       this.restoreBulkSize = restoreBulkSize;
   }


    /**
     * Getter for   maxElasticsearchBulk;
     */
    public Integer getMaxElasticsearchBulk() {
        return maxElasticsearchBulk;
    }

    /**
     * Setter for   maxElasticsearchBulk;
     */
    public void setMaxElasticsearchBulk(Integer maxElasticsearchBulk) {
        this.maxElasticsearchBulk = maxElasticsearchBulk;
    }

    /**
     * Getter for   numberDbClientThread;
     */
    public Integer getNumberDbClientThread() {
        return numberDbClientThread;
    }

    /**
     * Setter for   numberDbClientThread;
     */
    public void setNumberDbClientThread(Integer numberDbClientThread) {
        this.numberDbClientThread = numberDbClientThread;
    }

    /**
     * Getter for   numberEsQueue;
     */
    public Integer getNumberEsQueue() {
        return numberEsQueue;
    }

    /**
     * Setter for   numberEsQueue;
     */
    public void setNumberEsQueue(Integer numberEsQueue) {
        this.numberEsQueue = numberEsQueue;
    }



    /**
     * Getter for   cacheControlDelay;
     */
    public Integer getCacheControlDelay() {
        return cacheControlDelay;
    }

    /**
     * Getter for   maxCacheEntries;
     */
    public Integer getMaxCacheEntries() {
        return maxCacheEntries;
    }

    /**
     * Setter for   maxCacheEntries;
     */
    public void setMaxCacheEntries(int maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
    }


    /**
     * Getter for tenant admin
     *
     * @return adminTenant
     */
    public Integer getAdminTenant() {
        return adminTenant;
    }

    /**
     * Setter for tenantAdmin
     *
     * @param adminTenant
     */
    public void setAdminTenant(Integer adminTenant) {
        this.adminTenant = adminTenant;
    }

    /**
     * Setter for list of tenant
     *
     * @return
     */
    public List<Integer> getTenants() {
        return tenants;
    }

    /**
     * @param tenants
     */
    public void setTenants(List<Integer> tenants) {
        this.tenants = tenants;
    }


    /**
     * Getter for defaultOriginatingAgencyForExport
     * 
     * @return defaultOriginatingAgencyForExport
     */
    public Map<Integer, String> getDefaultOriginatingAgencyForExport() {
        return defaultOriginatingAgencyForExport;
    }

    /**
     * @param defaultOriginatingAgencyForExport
     */
    public void setDefaultOriginatingAgencyForExport(Map<Integer, String> defaultOriginatingAgencyForExport) {
        this.defaultOriginatingAgencyForExport = defaultOriginatingAgencyForExport;
    }
    
    
}
