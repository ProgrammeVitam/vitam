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

import com.google.common.base.Strings;
import fr.gouv.vitam.common.configuration.ClassificationLevel;

import java.util.List;
import java.util.Map;


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
    /*
     * The number of elements per file
     */
    private Integer storeGraphElementsPerFile;

    /**
     * The overlap delay (in seconds) for store graph operation. Used to do not treat elements in critical state due to
     * clock difference or GC slow down or VM freeze
     */
    private Integer storeGraphOverlapDelay;

    /**
     * Data migration bulk size
     */
    private Integer migrationBulkSize;

    /**
     * The time in seconds 60*60*24*30 (default 30 days) to wait before deleting reconstructed with only graph data
     * units The unit should contains only graph data and the graph last persisted date should be 30 day older
     */
    private Integer deleteIncompleteReconstructedUnitDelay;

    /**
     * The number of retry executing action when optimistic lock occurs
     */
    private Integer optimisticLockRetryNumber;
    /**
     * Optimistic lock sleep time in milliseconds, the sleep time after each retry
     */
    private Integer optimisticLockSleepTime;

    /**
     * This is a limitation of lucene.
     * Fields whose UTF8 encoding is longer than the max length 32766 are not accepted
     */
    private Integer keywordMaxLength;
    /**
     * There is not a limitation in lucene for text fields.
     * In VITAM, to enable sorting on some fields (title, ...), those fields are also not analysed (fielddata set to true)
     *
     * Problem:
     *    - Indexing text fields with value length > keywordMaxLength
     *    - Change mapping on ES to set fielddata = true on those fields
     *    - Re-index
     *    => Lucene will throws an exception as keywords can't be longer than max length (keywordMaxLength)
     *
     * So this is a vitam limitation.
     */
    private Integer textMaxLength;



    /**
     *
     */
    private Integer numberEsQueue;
    private Integer maxCacheEntries;

    /**
     * Max entries allowed for mass distribution
     */
    private Long distributionThreshold;

    /**
     * Max entries allowed for elimination analysis
     */
    private Long eliminationAnalysisThreshold;

    /**
     * Max entries allowed for elimination action
     */
    private Long eliminationActionThreshold;
    
    /**
     * Expire time for the cache entries in seconds (5 minutes by default)
     */
    private static Integer expireCacheEntriesDelay;

    private Integer cacheControlDelay;

    private Integer adminTenant;

    private List<Integer> tenants;

    /**
     * /** Default Vitam Config Folder
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
     * Allow client and Server Encoding request or response in GZIP format
     */
    private Boolean forceChunkModeInputStream;

    /**
     * default originatingAgency for DIP export when multiple originatingAgencies are in conflict in exported AU
     */
    private Map<Integer, String> defaultOriginatingAgencyForExport;

    /**
     * Setter for vitamCleanPeriod;
     */
    public void setVitamCleanPeriod(Integer vitamCleanPeriod) {
        this.vitamCleanPeriod = vitamCleanPeriod;
    }

    /**
     * Vitam Clean period (In hours)
     */
    private Integer vitamCleanPeriod;

    /**
     * Max dsl queries per reclassification request
     */
    private Integer reclassificationMaxBulkThreshold;
    /**
     * Max units to update per reclassification request
     */
    private Integer reclassificationMaxUnitsThreshold;
    /**
     * Max dsl queries per reclassification request
     */
    private Integer reclassificationMaxGuildListSizeInLogbookOperation;

    /**
     * classification level for the Vitam plateform useful for worker ingest / mass update / update unit
     */
    private ClassificationLevel classificationLevel;

    /**
     * Environment name used for storage offer container prefix (by default, set to empty string)
     */
    private String environmentName;

    private Integer ontologyCacheMaxEntries = 100;

    private Integer ontologyCacheTimeoutInSeconds = 300;

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
     * Getter for vitamConfigFolderDefault;
     */
    public String getVitamConfigFolderDefault() {
        return vitamConfigFolderDefault;
    }

    /**
     * Setter for vitamConfigFolderDefault;
     */
    public void setVitamConfigFolderDefault(String vitamConfigFolderDefault) {
        this.vitamConfigFolderDefault = vitamConfigFolderDefault;
    }

    /**
     * Getter for vitamDataFolderDefault;
     */
    public String getVitamDataFolderDefault() {
        return vitamDataFolderDefault;
    }

    /**
     * Setter for vitamDataFolderDefault;
     */
    public void setVitamDataFolderDefault(String vitamDataFolderDefault) {
        this.vitamDataFolderDefault = vitamDataFolderDefault;
    }

    /**
     * Getter for vitamLogFolderDefault;
     */
    public String getVitamLogFolderDefault() {
        return vitamLogFolderDefault;
    }

    /**
     * Setter for vitamLogFolderDefault;
     */
    public void setVitamLogFolderDefault(String vitamLogFolderDefault) {
        this.vitamLogFolderDefault = vitamLogFolderDefault;
    }

    /**
     * Getter for vitamTmpFolderDefault;
     */
    public String getVitamTmpFolderDefault() {
        return vitamTmpFolderDefault;
    }

    /**
     * Setter for vitamTmpFolderDefault;
     */
    public void setVitamTmpFolderDefault(String vitamTmpFolderDefault) {
        this.vitamTmpFolderDefault = vitamTmpFolderDefault;
    }

    /**
     * Getter for CHUNK_SIZE;
     */
    public Integer getChunkSize() {
        return chunkSize;
    }

    /**
     * Setter for chunkSize;
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
     * Getter for recvBufferSize;
     */
    public Integer getRecvBufferSize() {
        return recvBufferSize;
    }

    /**
     * Setter for recvBufferSize;
     */
    public void setRecvBufferSize(Integer recvBufferSize) {
        this.recvBufferSize = recvBufferSize;
    }

    /**
     * Getter for connectTimeout;
     */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Setter for connectTimeout;
     */
    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Getter for readTimeout;
     */
    public Integer getReadTimeout() {
        return readTimeout;
    }

    /**
     * Setter for readTimeout;
     */
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Getter for maxTotalClient;
     */
    public Integer getMaxTotalClient() {
        return maxTotalClient;
    }

    /**
     * Setter for maxTotalClient;
     */
    public void setMaxTotalClient(Integer maxTotalClient) {
        this.maxTotalClient = maxTotalClient;
    }

    /**
     * Getter for maxClientPerHost;
     */
    public Integer getMaxClientPerHost() {
        return maxClientPerHost;
    }

    /**
     * Setter for maxClientPerHost;
     */
    public void setMaxClientPerHost(Integer maxClientPerHost) {
        this.maxClientPerHost = maxClientPerHost;
    }

    /**
     * Getter for delayValidationAfterInactivity;
     */
    public Integer getDelayValidationAfterInactivity() {
        return delayValidationAfterInactivity;
    }

    /**
     * Setter for delayValidationAfterInactivity;
     */
    public void setDelayValidationAfterInactivity(Integer delayValidationAfterInactivity) {
        this.delayValidationAfterInactivity = delayValidationAfterInactivity;
    }

    /**
     * Getter for delayMultipleInputstream;
     */
    public Integer getDelayMultipleInputstream() {
        return delayMultipleInputstream;
    }

    /**
     * Setter for delayMultipleInputstream;
     */
    public void setDelayMultipleInputstream(Integer delayMultipleInputstream) {
        this.delayMultipleInputstream = delayMultipleInputstream;
    }

    /**
     * Getter for delayMultipleSubinputstream;
     */
    public Integer getDelayMultipleSubinputstream() {
        return delayMultipleSubinputstream;
    }

    /**
     * Setter for delayMultipleSubinputstream;
     */
    public void setDelayMultipleSubinputstream(Integer delayMultipleSubinputstream) {
        this.delayMultipleSubinputstream = delayMultipleSubinputstream;
    }

    /**
     * Getter for minimumThreadPoolSize;
     */
    public Integer getMinimumThreadPoolSize() {
        return minimumThreadPoolSize;
    }

    /**
     * Setter for minimumThreadPoolSize;
     */
    public void setMinimumThreadPoolSize(Integer minimumThreadPoolSize) {
        this.minimumThreadPoolSize = minimumThreadPoolSize;
    }

    /**
     * Getter for noValidationAfterInactivity;
     */
    public Integer getNoValidationAfterInactivity() {
        return noValidationAfterInactivity;
    }

    /**
     * Setter for noValidationAfterInactivity;
     */
    public void setNoValidationAfterInactivity(Integer noValidationAfterInactivity) {
        this.noValidationAfterInactivity = noValidationAfterInactivity;
    }

    /**
     * Getter for delayGetClient;
     */
    public Integer getDelayGetClient() {
        return delayGetClient;
    }

    /**
     * Setter for delayGetClient;
     */
    public void setDelayGetClient(Integer delayGetClient) {
        this.delayGetClient = delayGetClient;
    }

    /**
     * Getter for intervalDelayCheckIdle;
     */
    public Integer getIntervalDelayCheckIdle() {
        return intervalDelayCheckIdle;
    }

    /**
     * Setter for IntegerERVAL_DELAY_CHECK_IDLE;
     */
    public void setIntegerervalDelayCheckIdle(Integer IntegerervalDelayCheckIdle) {
        intervalDelayCheckIdle = IntegerervalDelayCheckIdle;
    }

    /**
     * Getter for maxDelayUnusedConnection;
     */
    public Integer getMaxDelayUnusedConnection() {
        return maxDelayUnusedConnection;
    }

    /**
     * Setter for maxDelayUnusedConnection;
     */
    public void setMaxDelayUnusedConnection(Integer maxDelayUnusedConnection) {
        this.maxDelayUnusedConnection = maxDelayUnusedConnection;
    }

    /**
     * Getter for useNewJaxrClient;
     */
    public Boolean isUseNewJaxrClient() {
        return useNewJaxrClient;
    }

    /**
     * Setter for useNewJaxrClient;
     */
    public void setUseNewJaxrClient(Boolean useNewJaxrClient) {
        this.useNewJaxrClient = useNewJaxrClient;
    }

    /**
     * Getter for securityDigestType;
     */
    public String getSecurityDigestType() {
        return securityDigestType;
    }

    /**
     * Setter for securityDigestType;
     */
    public void setSecurityDigestType(String securityDigestType) {
        this.securityDigestType = securityDigestType;
    }

    /**
     * Getter for defaultDigestType;
     */
    public String getDefaultDigestType() {
        return defaultDigestType;
    }

    /**
     * Setter for defaultDigestType;
     */
    public void setDefaultDigestType(String defaultDigestType) {
        this.defaultDigestType = defaultDigestType;
    }

    /**
     * Getter for defaultTimestampDigestType;
     */
    public String getDefaultTimestampDigestType() {
        return defaultTimestampDigestType;
    }

    /**
     * Setter for defaultTimestampDigestType;
     */
    public void setDefaultTimestampDigestType(String defaultTimestampDigestType) {
        this.defaultTimestampDigestType = defaultTimestampDigestType;
    }

    /**
     * Getter for acceptableRequestTime;
     */
    public Long getAcceptableRequestTime() {
        return acceptableRequestTime;
    }

    /**
     * Setter for acceptableRequestTime;
     */
    public void setAcceptableRequestTime(Long acceptableRequestTime) {
        this.acceptableRequestTime = acceptableRequestTime;
    }

    /**
     * Getter for threadsAllowedToBlockForConnectionMultipliers;
     */
    public Integer getThreadsAllowedToBlockForConnectionMultipliers() {
        return threadsAllowedToBlockForConnectionMultipliers;
    }

    /**
     * Setter for threadsAllowedToBlockForConnectionMultipliers;
     */
    public void setThreadsAllowedToBlockForConnectionMultipliers(
        Integer threadsAllowedToBlockForConnectionMultipliers) {
        this.threadsAllowedToBlockForConnectionMultipliers = threadsAllowedToBlockForConnectionMultipliers;
    }

    /**
     * Getter for retryNumber;
     */
    public Integer getRetryNumber() {
        return retryNumber;
    }

    /**
     * Setter for retryNumber;
     */
    public void setRetryNumber(Integer retryNumber) {
        this.retryNumber = retryNumber;
    }

    /**
     * Getter for retryDelay;
     */
    public Integer getRetryDelay() {
        return retryDelay;
    }

    /**
     * Setter for retryDelay;
     */
    public void setRetryDelay(Integer retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * Getter for waitingDelay;
     */
    public Integer getWaitingDelay() {
        return waitingDelay;
    }

    /**
     * Setter for waitingDelay;
     */
    public void setWaitingDelay(Integer waitingDelay) {
        this.waitingDelay = waitingDelay;
    }



    /**
     * Getter for forceChunkModeInputStream;
     */
    public Boolean isForceChunkModeInputStream() {
        return forceChunkModeInputStream;
    }

    /**
     * Setter for forceChunkModeInputStream;
     */
    public void setForceChunkModeInputStream(Boolean forceChunkModeInputStream) {
        this.forceChunkModeInputStream = forceChunkModeInputStream;
    }

    /**
     * Getter for allowGzipEncoding;
     */
    public Boolean isAllowGzipEncoding() {
        return allowGzipEncoding;
    }

    /**
     * Setter for allowGzipEncoding;
     */
    public void setAllowGzipEncoding(Boolean allowGzipEncoding) {
        this.allowGzipEncoding = allowGzipEncoding;
    }

    /**
     * Getter for allowGzipDecoding;
     */
    public Boolean isAllowGzipDecoding() {
        return allowGzipDecoding;
    }

    /**
     * Setter for allowGzipDecoding;
     */
    public void setAllowGzipDecoding(Boolean allowGzipDecoding) {
        this.allowGzipDecoding = allowGzipDecoding;
    }

    /**
     * Getter for BUFFER_NUMBER;
     */
    public Integer getBufferNumber() {
        return BUFFER_NUMBER;
    }

    /**
     * Setter for BUFFER_NUMBER;
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
     * Getter for exportScore;
     */
    public Boolean isExportScore() {
        return exportScore;
    }

    /**
     * Setter for exportScore;
     */
    public void setExportScore(boolean exportScore) {
        this.exportScore = exportScore;
    }



    /**
     * Getter for distributeurBatchSize;
     */
    public Integer getDistributeurBatchSize() {
        return distributeurBatchSize;
    }

    /**
     * Setter for distributeurBatchSize;
     */
    public void setDistributeurBatchSize(Integer distributeurBatchSize) {
        this.distributeurBatchSize = distributeurBatchSize;
    }


    /**
     * Getter for worker bulk size
     *
     * @return
     */
    public Integer getWorkerBulkSize() {
        return workerBulkSize;
    }

    /**
     * Setter for worker bulk size
     *
     * @param workerBulkSize
     */
    public void setWorkerBulkSize(Integer workerBulkSize) {
        this.workerBulkSize = workerBulkSize;
    }


    /**
     * Getter for storeGraphElementsPerFile
     *
     * @return storeGraphElementsPerFile
     */
    public Integer getStoreGraphElementsPerFile() {
        return storeGraphElementsPerFile;
    }

    /**
     * Setter for storeGraphElementsPerFile
     *
     * @param storeGraphElementsPerFile
     */
    public void setStoreGraphElementsPerFile(Integer storeGraphElementsPerFile) {
        this.storeGraphElementsPerFile = storeGraphElementsPerFile;
    }


    /**
     * Get store graph overlap delay
     *
     * @return storeGraphOverlapDelay
     */
    public Integer getStoreGraphOverlapDelay() {
        return storeGraphOverlapDelay;
    }

    /**
     * Set store graph overlap delay
     *
     * @param storeGraphOverlapDelay
     */
    public void setStoreGraphOverlapDelay(Integer storeGraphOverlapDelay) {
        this.storeGraphOverlapDelay = storeGraphOverlapDelay;
    }

    /**
     * Set data migration bulk size
     */
    public Integer getMigrationBulkSize() {
        return migrationBulkSize;
    }

    /**
     * Get data migration bulk size
     *
     * @param migrationBulkSize
     */
    public void setMigrationBulkSize(Integer migrationBulkSize) {
        this.migrationBulkSize = migrationBulkSize;
    }

    /**
     * Get the delay of deleting incomplete reconstructed units
     *
     * @return deleteIncompleteReconstructedUnitDelay
     */
    public Integer getDeleteIncompleteReconstructedUnitDelay() {
        return deleteIncompleteReconstructedUnitDelay;
    }

    /**
     * Set the delay of deleting incomplete reconstructed units
     *
     * @param deleteIncompleteReconstructedUnitDelay
     */
    public void setDeleteIncompleteReconstructedUnitDelay(Integer deleteIncompleteReconstructedUnitDelay) {
        this.deleteIncompleteReconstructedUnitDelay = deleteIncompleteReconstructedUnitDelay;
    }


    /**
     * Get optimistic lock retry number
     *
     * @return optimisticLockRetryNumber
     */
    public Integer getOptimisticLockRetryNumber() {
        return optimisticLockRetryNumber;
    }

    /**
     * Set optimistic lock retry number
     *
     * @param optimisticLockRetryNumber
     */
    public void setOptimisticLockRetryNumber(Integer optimisticLockRetryNumber) {
        this.optimisticLockRetryNumber = optimisticLockRetryNumber;
    }

    /**
     * Get optimistic lock sleep time
     *
     * @return optimisticLockSleepTime
     */
    public Integer getOptimisticLockSleepTime() {
        return optimisticLockSleepTime;
    }

    /**
     * Set optimistic lock sleep time
     *
     * @param optimisticLockSleepTime
     */
    public void setOptimisticLockSleepTime(Integer optimisticLockSleepTime) {
        this.optimisticLockSleepTime = optimisticLockSleepTime;
    }


    /**
     * Getter
     * @return keywordMaxLength
     */
    public Integer getKeywordMaxLength() {
        return keywordMaxLength;
    }

    /**
     * Setter
     * @param keywordMaxLength
     */
    public void setKeywordMaxLength(Integer keywordMaxLength) {
        this.keywordMaxLength = keywordMaxLength;
    }

    /**
     * Getter
     * @return textMaxLength
     */
    public Integer getTextMaxLength() {
        return textMaxLength;
    }

    /**
     * Setter
     * @param textMaxLength
     */
    public void setTextMaxLength(Integer textMaxLength) {
        this.textMaxLength = textMaxLength;
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
     * Getter for maxElasticsearchBulk;
     */
    public Integer getMaxElasticsearchBulk() {
        return maxElasticsearchBulk;
    }

    /**
     * Setter for maxElasticsearchBulk;
     */
    public void setMaxElasticsearchBulk(Integer maxElasticsearchBulk) {
        this.maxElasticsearchBulk = maxElasticsearchBulk;
    }

    /**
     * Getter for numberDbClientThread;
     */
    public Integer getNumberDbClientThread() {
        return numberDbClientThread;
    }

    /**
     * Setter for numberDbClientThread;
     */
    public void setNumberDbClientThread(Integer numberDbClientThread) {
        this.numberDbClientThread = numberDbClientThread;
    }

    /**
     * Getter for numberEsQueue;
     */
    public Integer getNumberEsQueue() {
        return numberEsQueue;
    }

    /**
     * Setter for numberEsQueue;
     */
    public void setNumberEsQueue(Integer numberEsQueue) {
        this.numberEsQueue = numberEsQueue;
    }

    /**
     * Getter for distributionThreshold;
     *
     * @return distributionThreshold
     */
    public Long getDistributionThreshold() {
        return distributionThreshold;
    }

    /**
     * Setter for distributionThreshold;
     *
     * @param distributionThreshold
     */
    public void setDistributionThreshold(Long distributionThreshold) {
        this.distributionThreshold = distributionThreshold;
    }

    public Long getEliminationAnalysisThreshold() {
        return eliminationAnalysisThreshold;
    }

    public void setEliminationAnalysisThreshold(Long eliminationAnalysisThreshold) {
        this.eliminationAnalysisThreshold = eliminationAnalysisThreshold;
    }

    public Long getEliminationActionThreshold() {
        return eliminationActionThreshold;
    }

    public void setEliminationActionThreshold(Long eliminationActionThreshold) {
        this.eliminationActionThreshold = eliminationActionThreshold;
    }

    /**
     * Getter for cacheControlDelay;
     */
    public Integer getCacheControlDelay() {
        return cacheControlDelay;
    }

    /**
     * Getter for maxCacheEntries;
     */
    public Integer getMaxCacheEntries() {
        return maxCacheEntries;
    }

    /**
     * Setter for maxCacheEntries;
     */
    public void setMaxCacheEntries(int maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
    }


    /**
     * Setter for expireCacheEntriesDelay
     *
     * @return expireCacheEntriesDelay
     */
    public Integer getExpireCacheEntriesDelay() {
        return expireCacheEntriesDelay;
    }

    /**
     * Getter for expireCacheEntriesDelay
     *
     * @param expireCacheEntriesDelay
     */
    public void setExpireCacheEntriesDelay(Integer expireCacheEntriesDelay) {
        this.expireCacheEntriesDelay = expireCacheEntriesDelay;
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

    /**
     * Max dsl queries per reclassification request
     */
    public Integer getReclassificationMaxBulkThreshold() {
        return reclassificationMaxBulkThreshold;
    }

    /**
     * Max dsl queries per reclassification request
     */
    public void setReclassificationMaxBulkThreshold(Integer reclassificationMaxBulkThreshold) {
        this.reclassificationMaxBulkThreshold = reclassificationMaxBulkThreshold;
    }

    /**
     * Max units to update per reclassification request
     */
    public Integer getReclassificationMaxUnitsThreshold() {
        return reclassificationMaxUnitsThreshold;
    }

    /**
     * Max units to update per reclassification request
     */
    public void setReclassificationMaxUnitsThreshold(Integer reclassificationMaxUnitsThreshold) {
        this.reclassificationMaxUnitsThreshold = reclassificationMaxUnitsThreshold;
    }

    /**
     * Max guid to store in logbook operation in evDetData
     */
    public Integer getReclassificationMaxGuildListSizeInLogbookOperation() {
        return reclassificationMaxGuildListSizeInLogbookOperation;
    }

    /**
     * Max guid to store in logbook operation in evDetData
     */
    public void setReclassificationMaxGuildListSizeInLogbookOperation(
        Integer reclassificationMaxGuildListSizeInLogbookOperation) {
        this.reclassificationMaxGuildListSizeInLogbookOperation = reclassificationMaxGuildListSizeInLogbookOperation;
    }

    public ClassificationLevel getClassificationLevel() {
        return classificationLevel;
    }

    public void setClassificationLevel(ClassificationLevel classificationLevel) {
        this.classificationLevel = classificationLevel;
    }


    /**
     * Get environmentName
     *
     * @return environmentName value
     */
    public String getEnvironmentName() {
        return environmentName;
    }
    /**
     * set the environmentName
     */
    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public Integer getOntologyCacheMaxEntries() {
        return ontologyCacheMaxEntries;
    }

    public void setOntologyCacheMaxEntries(int ontologyCacheMaxEntries) {
        this.ontologyCacheMaxEntries = ontologyCacheMaxEntries;
    }

    public Integer getOntologyCacheTimeoutInSeconds() {
        return ontologyCacheTimeoutInSeconds;
    }

    public void setOntologyCacheTimeoutInSeconds(int ontologyCacheTimeoutInSeconds) {
        this.ontologyCacheTimeoutInSeconds = ontologyCacheTimeoutInSeconds;
    }
}
