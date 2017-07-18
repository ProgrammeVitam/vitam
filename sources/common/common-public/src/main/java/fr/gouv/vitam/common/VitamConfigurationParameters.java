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

/**
 * This class contains Vitam Data
 */
public class VitamConfigurationParameters {

    private  Boolean EXPORT_SCORE  ;
    private String secret;
    private Boolean filterActivation = true;
    private    Integer MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER ;

    /**
     * Property Vitam Config Folder
     */
    private String VITAM_CONFIG_PROPERTY;
    /**
     *
     */
    public  Boolean ENABLE_JAXB_PARSER ;
    /**
     *
     */
    private Integer DISTRIBUTEUR_BATCH_SIZE;
    /**
     *
     */
    private Integer MAX_ELASTICSEARCH_BULK;
    /**
     *
     */
    private Integer NUMBER_DB_CLIENT_THREAD;
    /**
     *
     */
    private Integer NUMBER_ES_QUEUE;
    private Integer MAX_CACHE_ENTRIES;
    private Integer CACHE_CONTROL_DELAY;



    /**
     * Setter for   ENABLE_JAXB_PARSER;
     */
    public void setEnableJaxbParser(boolean ENABLE_JAXB_PARSER) {
        this.ENABLE_JAXB_PARSER = ENABLE_JAXB_PARSER;
    }


    public  Boolean ENABLE_DISTRIBUTOR_V2 ;
    /**

     * Property Vitam Data Folder
     */
    private String VITAM_DATA_PROPERTY;
    /**
     * Property Vitam Log Folder
     */
    private String VITAM_LOG_PROPERTY;
    /**
     * Property Vitam Tmp Folder
     */
    private String VITAM_TMP_PROPERTY;
    /**
     * Default Vitam Config Folder
     */
    private String VITAM_CONFIG_FOLDER_DEFAULT;
    /**
     * Default Vitam Config Folder
     */
    private String VITAM_DATA_FOLDER_DEFAULT;
    /**
     * Default Vitam Config Folder
     */
    private String VITAM_LOG_FOLDER_DEFAULT;
    /**
     * Default Vitam Config Folder
     */
    private String VITAM_TMP_FOLDER_DEFAULT;
    /**
     * Default Chunk Size
     */
    private Integer CHUNK_SIZE;

    /**
     * Default Async Workspace Queue Size
     */
    private Integer ASYNC_WORKSPACE_QUEUE_SIZE;

    /**
     * Default Recv Buffer Size
     */
    private Integer RECV_BUFFER_SIZE;
    /**
     * Default Connection timeout
     */
    private Integer CONNECT_TIMEOUT;
    /**
     * Default Read Timeout
     */
    private Integer READ_TIMEOUT;

    /**
     * Max total concurrent clients
     */
    private Integer MAX_TOTAL_CLIENT;
    /**
     * Max concurrent clients associated to one host
     */
    private Integer MAX_CLIENT_PER_HOST;
    /**
     * Max delay to check an unused client in pool before being returned (Apache Only)
     */
    private Integer DELAY_VALIDATION_AFTER_INACTIVITY;
    /**
     * Max delay to check if no buffer is available while trying to continue to read (MultipleInputStreamHandler Only)
     *
     * Not final to allow Junit to decrease it
     */
    private Integer DELAY_MULTIPLE_INPUTSTREAM;
    /**
     * Max delay to check if no buffer is available while trying to continue to read (SubStreams Only)
     */
    private Integer DELAY_MULTIPLE_SUBINPUTSTREAM;
    /**
     * Default minimum thread pool size
     */
    private Integer MINIMUM_THREAD_POOL_SIZE;
    /**
     * No check of unused client within pool (Apache Only)
     */
    private Integer NO_VALIDATION_AFTER_INACTIVITY;
    /**
     * Max delay to get a client (Apache Only)
     */
    private Integer DELAY_GET_CLIENT;
    /**
     * Specify the delay where connections returned to pool will be checked (Apache Only)
     */
    private Integer INTERVAL_DELAY_CHECK_IDLE;
    /**
     * Specify the delay of unused connection returned in the pool before being really closed (Apache Only)
     */
    private Integer MAX_DELAY_UNUSED_CONNECTION;
    /**
     * Use a new JAX_RS client each time
     */
    private Boolean USE_NEW_JAXR_CLIENT;

    /**
     * Default Digest Type for SECURITY
     */
    private String SECURITY_DIGEST_TYPE;
    /**
     * Default Digest Type for Vitam
     */
    private String DEFAULT_DIGEST_TYPE;
    /**
     * Default Digest Type for time stamp generation
     */
    private String DEFAULT_TIMESTAMP_DIGEST_TYPE;
    /**
     * Acceptable Request Time elaps
     */
    private Long ACCEPTABLE_REQUEST_TIME;
    /**
     * MongoDB client configuration
     */
    private Integer THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS;
    /**
     * Retry repetition
     */
    private Integer RETRY_NUMBER;
    /**
     * Retry delay
     */
    private Integer RETRY_DELAY;
    /**
     * Waiting delay (for wait(delay) method)
     */
    private Integer WAITING_DELAY;
    /**
     * Allow client and Server Encoding request or response in GZIP format
     */
    private Boolean ALLOW_GZIP_ENCODING;
    /**
     * Allow client to receive GZIP encoded response
     */
    private Boolean ALLOW_GZIP_DECODING;
    /**
     * Read ahead x4 Buffers
     */
    private Integer BUFFER_NUMBER;

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
     *
     * @return this
     */
    public VitamConfigurationParameters setFilterActivation(Boolean filterActivation) {
        this.filterActivation = filterActivation;
        return this;
    }

    /**
     * Getter for   VITAM_CONFIG_PROPERTY;
     */
    public String getVitamConfigProperty() {
        return VITAM_CONFIG_PROPERTY;
    }

    /**
     * Setter for   VITAM_CONFIG_PROPERTY;
     */
    public void setVitamConfigProperty(String vitamConfigProperty) {
        VITAM_CONFIG_PROPERTY = vitamConfigProperty;
    }

    /**
     * Getter for   VITAM_DATA_PROPERTY;
     */
    public String getVitamDataProperty() {
        return VITAM_DATA_PROPERTY;
    }

    /**
     * Setter for   VITAM_DATA_PROPERTY;
     */
    public void setVitamDataProperty(String vitamDataProperty) {
        VITAM_DATA_PROPERTY = vitamDataProperty;
    }

    /**
     * Getter for   VITAM_LOG_PROPERTY;
     */
    public String getVitamLogProperty() {
        return VITAM_LOG_PROPERTY;
    }

    /**
     * Setter for   VITAM_LOG_PROPERTY;
     */
    public void setVitamLogProperty(String vitamLogProperty) {
        VITAM_LOG_PROPERTY = vitamLogProperty;
    }

    /**
     * Getter for   VITAM_TMP_PROPERTY;
     */
    public String getVitamTmpProperty() {
        return VITAM_TMP_PROPERTY;
    }

    /**
     * Setter for   VITAM_TMP_PROPERTY;
     */
    public void setVitamTmpProperty(String vitamTmpProperty) {
        VITAM_TMP_PROPERTY = vitamTmpProperty;
    }

    /**
     * Getter for   VITAM_CONFIG_FOLDER_DEFAULT;
     */
    public String getVitamConfigFolderDefault() {
        return VITAM_CONFIG_FOLDER_DEFAULT;
    }

    /**
     * Setter for   VITAM_CONFIG_FOLDER_DEFAULT;
     */
    public void setVitamConfigFolderDefault(String vitamConfigFolderDefault) {
        VITAM_CONFIG_FOLDER_DEFAULT = vitamConfigFolderDefault;
    }

    /**
     * Getter for   VITAM_DATA_FOLDER_DEFAULT;
     */
    public String getVitamDataFolderDefault() {
        return VITAM_DATA_FOLDER_DEFAULT;
    }

    /**
     * Setter for   VITAM_DATA_FOLDER_DEFAULT;
     */
    public void setVitamDataFolderDefault(String vitamDataFolderDefault) {
        VITAM_DATA_FOLDER_DEFAULT = vitamDataFolderDefault;
    }

    /**
     * Getter for   VITAM_LOG_FOLDER_DEFAULT;
     */
    public String getVitamLogFolderDefault() {
        return VITAM_LOG_FOLDER_DEFAULT;
    }

    /**
     * Setter for   VITAM_LOG_FOLDER_DEFAULT;
     */
    public void setVitamLogFolderDefault(String vitamLogFolderDefault) {
        VITAM_LOG_FOLDER_DEFAULT = vitamLogFolderDefault;
    }

    /**
     * Getter for   VITAM_TMP_FOLDER_DEFAULT;
     */
    public String getVitamTmpFolderDefault() {
        return VITAM_TMP_FOLDER_DEFAULT;
    }

    /**
     * Setter for   VITAM_TMP_FOLDER_DEFAULT;
     */
    public void setVitamTmpFolderDefault(String vitamTmpFolderDefault) {
        VITAM_TMP_FOLDER_DEFAULT = vitamTmpFolderDefault;
    }

    /**
     * Getter for   CHUNK_SIZE;
     */
    public Integer getChunkSize() {
        return CHUNK_SIZE;
    }

    /**
     * Setter for   CHUNK_SIZE;
     */
    public void setChunkSize(Integer chunkSize) {
        CHUNK_SIZE = chunkSize;
    }

    /**
     * @return the size of the queue of async workspace
     */
    public Integer getAsyncWorkspaceQueueSize() {
        return ASYNC_WORKSPACE_QUEUE_SIZE;
    }


    /**
     * @return the size of the queue of async workspace
     */
    public void setAsyncWorkspaceQueueSize(int queueSize) {
        ASYNC_WORKSPACE_QUEUE_SIZE = queueSize;
    }


    /**
     * Getter for   RECV_BUFFER_SIZE;
     */
    public Integer getRecvBufferSize() {
        return RECV_BUFFER_SIZE;
    }

    /**
     * Setter for   RECV_BUFFER_SIZE;
     */
    public void setRecvBufferSize(Integer recvBufferSize) {
        RECV_BUFFER_SIZE = recvBufferSize;
    }

    /**
     * Getter for   CONNECT_TIMEOUT;
     */
    public Integer getConnectTimeout() {
        return CONNECT_TIMEOUT;
    }

    /**
     * Setter for   CONNECT_TIMEOUT;
     */
    public void setConnectTimeout(Integer connectTimeout) {
        CONNECT_TIMEOUT = connectTimeout;
    }

    /**
     * Getter for   READ_TIMEOUT;
     */
    public Integer getReadTimeout() {
        return READ_TIMEOUT;
    }

    /**
     * Setter for   READ_TIMEOUT;
     */
    public void setReadTimeout(Integer readTimeout) {
        READ_TIMEOUT = readTimeout;
    }

    /**
     * Getter for   MAX_TOTAL_CLIENT;
     */
    public Integer getMaxTotalClient() {
        return MAX_TOTAL_CLIENT;
    }

    /**
     * Setter for   MAX_TOTAL_CLIENT;
     */
    public void setMaxTotalClient(Integer maxTotalClient) {
        MAX_TOTAL_CLIENT = maxTotalClient;
    }

    /**
     * Getter for   MAX_CLIENT_PER_HOST;
     */
    public Integer getMaxClientPerHost() {
        return MAX_CLIENT_PER_HOST;
    }

    /**
     * Setter for   MAX_CLIENT_PER_HOST;
     */
    public void setMaxClientPerHost(Integer maxClientPerHost) {
        MAX_CLIENT_PER_HOST = maxClientPerHost;
    }

    /**
     * Getter for   DELAY_VALIDATION_AFTER_INACTIVITY;
     */
    public Integer getDelayValidationAfterInactivity() {
        return DELAY_VALIDATION_AFTER_INACTIVITY;
    }

    /**
     * Setter for   DELAY_VALIDATION_AFTER_INACTIVITY;
     */
    public void setDelayValidationAfterInactivity(Integer delayValidationAfterInactivity) {
        DELAY_VALIDATION_AFTER_INACTIVITY = delayValidationAfterInactivity;
    }

    /**
     * Getter for   DELAY_MULTIPLE_INPUTSTREAM;
     */
    public Integer getDelayMultipleInputstream() {
        return DELAY_MULTIPLE_INPUTSTREAM;
    }

    /**
     * Setter for   DELAY_MULTIPLE_INPUTSTREAM;
     */
    public void setDelayMultipleInputstream(Integer delayMultipleInputstream) {
        DELAY_MULTIPLE_INPUTSTREAM = delayMultipleInputstream;
    }

    /**
     * Getter for   DELAY_MULTIPLE_SUBINPUTSTREAM;
     */
    public Integer getDelayMultipleSubinputstream() {
        return DELAY_MULTIPLE_SUBINPUTSTREAM;
    }

    /**
     * Setter for   DELAY_MULTIPLE_SUBINPUTSTREAM;
     */
    public void setDelayMultipleSubinputstream(Integer delayMultipleSubinputstream) {
        DELAY_MULTIPLE_SUBINPUTSTREAM = delayMultipleSubinputstream;
    }

    /**
     * Getter for   MINIMUM_THREAD_POOL_SIZE;
     */
    public Integer getMinimumThreadPoolSize() {
        return MINIMUM_THREAD_POOL_SIZE;
    }

    /**
     * Setter for   MINIMUM_THREAD_POOL_SIZE;
     */
    public void setMinimumThreadPoolSize(Integer minimumThreadPoolSize) {
        MINIMUM_THREAD_POOL_SIZE = minimumThreadPoolSize;
    }

    /**
     * Getter for   NO_VALIDATION_AFTER_INACTIVITY;
     */
    public Integer getNoValidationAfterInactivity() {
        return NO_VALIDATION_AFTER_INACTIVITY;
    }

    /**
     * Setter for   NO_VALIDATION_AFTER_INACTIVITY;
     */
    public void setNoValidationAfterInactivity(Integer noValidationAfterInactivity) {
        NO_VALIDATION_AFTER_INACTIVITY = noValidationAfterInactivity;
    }

    /**
     * Getter for   DELAY_GET_CLIENT;
     */
    public Integer getDelayGetClient() {
        return DELAY_GET_CLIENT;
    }

    /**
     * Setter for   DELAY_GET_CLIENT;
     */
    public void setDelayGetClient(Integer delayGetClient) {
        DELAY_GET_CLIENT = delayGetClient;
    }

    /**
     * Getter for   INTERVAL_DELAY_CHECK_IDLE;
     */
    public Integer getIntervalDelayCheckIdle() {
        return INTERVAL_DELAY_CHECK_IDLE;
    }

    /**
     * Setter for   IntegerERVAL_DELAY_CHECK_IDLE;
     */
    public void setIntegerervalDelayCheckIdle(Integer IntegerervalDelayCheckIdle) {
        INTERVAL_DELAY_CHECK_IDLE = IntegerervalDelayCheckIdle;
    }

    /**
     * Getter for   MAX_DELAY_UNUSED_CONNECTION;
     */
    public Integer getMaxDelayUnusedConnection() {
        return MAX_DELAY_UNUSED_CONNECTION;
    }

    /**
     * Setter for   MAX_DELAY_UNUSED_CONNECTION;
     */
    public void setMaxDelayUnusedConnection(Integer maxDelayUnusedConnection) {
        MAX_DELAY_UNUSED_CONNECTION = maxDelayUnusedConnection;
    }

    /**
     * Getter for   USE_NEW_JAXR_CLIENT;
     */
    public Boolean isUseNewJaxrClient() {
        return USE_NEW_JAXR_CLIENT;
    }

    /**
     * Setter for   USE_NEW_JAXR_CLIENT;
     */
    public void setUseNewJaxrClient(Boolean useNewJaxrClient) {
        USE_NEW_JAXR_CLIENT = useNewJaxrClient;
    }

    /**
     * Getter for   SECURITY_DIGEST_TYPE;
     */
    public String getSecurityDigestType() {
        return SECURITY_DIGEST_TYPE;
    }

    /**
     * Setter for   SECURITY_DIGEST_TYPE;
     */
    public void setSecurityDigestType(String securityDigestType) {
        SECURITY_DIGEST_TYPE = securityDigestType;
    }

    /**
     * Getter for   DEFAULT_DIGEST_TYPE;
     */
    public String getDefaultDigestType() {
        return DEFAULT_DIGEST_TYPE;
    }

    /**
     * Setter for   DEFAULT_DIGEST_TYPE;
     */
    public void setDefaultDigestType(String defaultDigestType) {
        DEFAULT_DIGEST_TYPE = defaultDigestType;
    }

    /**
     * Getter for   DEFAULT_TIMESTAMP_DIGEST_TYPE;
     */
    public String getDefaultTimestampDigestType() {
        return DEFAULT_TIMESTAMP_DIGEST_TYPE;
    }

    /**
     * Setter for   DEFAULT_TIMESTAMP_DIGEST_TYPE;
     */
    public void setDefaultTimestampDigestType(String defaultTimestampDigestType) {
        DEFAULT_TIMESTAMP_DIGEST_TYPE = defaultTimestampDigestType;
    }

    /**
     * Getter for   ACCEPTABLE_REQUEST_TIME;
     */
    public Long getAcceptableRequestTime() {
        return ACCEPTABLE_REQUEST_TIME;
    }

    /**
     * Setter for   ACCEPTABLE_REQUEST_TIME;
     */
    public void setAcceptableRequestTime(Long acceptableRequestTime) {
        ACCEPTABLE_REQUEST_TIME = acceptableRequestTime;
    }

    /**
     * Getter for   THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS;
     */
    public Integer getThreadsAllowedToBlockForConnectionMultipliers() {
        return THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS;
    }

    /**
     * Setter for   THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS;
     */
    public void setThreadsAllowedToBlockForConnectionMultipliers(
        Integer threadsAllowedToBlockForConnectionMultipliers) {
        THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS = threadsAllowedToBlockForConnectionMultipliers;
    }

    /**
     * Getter for   RETRY_NUMBER;
     */
    public Integer getRetryNumber() {
        return RETRY_NUMBER;
    }

    /**
     * Setter for   RETRY_NUMBER;
     */
    public void setRetryNumber(Integer retryNumber) {
        RETRY_NUMBER = retryNumber;
    }

    /**
     * Getter for   RETRY_DELAY;
     */
    public Integer getRetryDelay() {
        return RETRY_DELAY;
    }

    /**
     * Setter for   RETRY_DELAY;
     */
    public void setRetryDelay(Integer retryDelay) {
        RETRY_DELAY = retryDelay;
    }

    /**
     * Getter for   WAITING_DELAY;
     */
    public Integer getWaitingDelay() {
        return WAITING_DELAY;
    }

    /**
     * Setter for   WAITING_DELAY;
     */
    public void setWaitingDelay(Integer waitingDelay) {
        WAITING_DELAY = waitingDelay;
    }

    /**
     * Getter for   ALLOW_GZIP_ENCODING;
     */
    public Boolean isAllowGzipEncoding() {
        return ALLOW_GZIP_ENCODING;
    }

    /**
     * Setter for   ALLOW_GZIP_ENCODING;
     */
    public void setAllowGzipEncoding(Boolean allowGzipEncoding) {
        ALLOW_GZIP_ENCODING = allowGzipEncoding;
    }

    /**
     * Getter for   ALLOW_GZIP_DECODING;
     */
    public Boolean isAllowGzipDecoding() {
        return ALLOW_GZIP_DECODING;
    }

    /**
     * Setter for   ALLOW_GZIP_DECODING;
     */
    public void setAllowGzipDecoding(Boolean allowGzipDecoding) {
        ALLOW_GZIP_DECODING = allowGzipDecoding;
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
     * getter for ENABLE_JAXB_PARSER
     * @return
     */
    public  Boolean isEnableJaxbParser() {
        return ENABLE_JAXB_PARSER;
    }

    /**
     * setter for ENABLE_JAXB_PARSER
     * @return
     */
    public  void setEnableJaxbParser(Boolean enableJaxbParser) {
        ENABLE_JAXB_PARSER = enableJaxbParser;
    }

    /**
     * getter for MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER
     * @return
     */
    public  Integer getMaxConcurrentMultipleInputstreamHandler() {
        return MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER;
    }

    /**
     * setter for MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER
     * @return
     */
    public  void setMaxConcurrentMultipleInputstreamHandler(int maxConcurrentMultipleInputstreamHandler) {
        MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER = maxConcurrentMultipleInputstreamHandler;
    }

    /**
     * Getter for   ENABLE_DISTRIBUTOR_V2;
     */
    public Boolean isEnableDistributorV2() {
        return ENABLE_DISTRIBUTOR_V2;
    }

    /**
     * Setter for   ENABLE_DISTRIBUTOR_V2;
     */
    public void setEnableDistributorV2(boolean ENABLE_DISTRIBUTOR_V2) {
        this.ENABLE_DISTRIBUTOR_V2 = ENABLE_DISTRIBUTOR_V2;
    }
    public Integer getVitamCleanPeriod() {
        return vitamCleanPeriod;
    }


    /**
     * Getter for   EXPORT_SCORE;
     */
    public Boolean isExportScore() {
        return EXPORT_SCORE;
    }
    /**
     * Setter for   EXPORT_SCORE;
     */
    public  void setExportScore(boolean exportScore) {
        EXPORT_SCORE = exportScore;
    }



    /**
     * Getter for   DISTRIBUTEUR_BATCH_SIZE;
     */
    public  Integer getDistributeurBatchSize() {
        return DISTRIBUTEUR_BATCH_SIZE;
    }

    /**
     * Setter for   DISTRIBUTEUR_BATCH_SIZE;
     */
    public  void setDistributeurBatchSize(int distributeurBatchSize) {
        DISTRIBUTEUR_BATCH_SIZE = distributeurBatchSize;
    }


    /**
     * Getter for   MAX_ELASTICSEARCH_BULK;
     */
    public  Integer getMaxElasticsearchBulk() {
        return MAX_ELASTICSEARCH_BULK;
    }

    /**
     * Setter for   MAX_ELASTICSEARCH_BULK;
     */
    public  void setMaxElasticsearchBulk(Integer maxElasticsearchBulk) {
        MAX_ELASTICSEARCH_BULK = maxElasticsearchBulk;
    }
    /**
     * Getter for   NUMBER_DB_CLIENT_THREAD;
     */
    public  Integer getNumberDbClientThread() {
        return NUMBER_DB_CLIENT_THREAD;
    }

    /**
     * Setter for   NUMBER_DB_CLIENT_THREAD;
     */
    public  void setNumberDbClientThread(Integer numberDbClientThread) {
        NUMBER_DB_CLIENT_THREAD = numberDbClientThread;
    }

    /**
     * Getter for   NUMBER_ES_QUEUE;
     */
    public  Integer getNumberEsQueue() {
        return NUMBER_ES_QUEUE;
    }

    /**
     * Setter for   NUMBER_ES_QUEUE;
     */
    public  void setNumberEsQueue(Integer numberEsQueue) {
        NUMBER_ES_QUEUE = numberEsQueue;
    }

    /**
     * Setter for   CACHE_CONTROL_DELAY;
     */
    public  void setCacheControlDelay(int cacheControlDelay, Integer CACHE_CONTROL_DELAY) {
        CACHE_CONTROL_DELAY = cacheControlDelay;
    }


    /**
     * Getter for   CACHE_CONTROL_DELAY;
     */
    public  Integer getCacheControlDelay() {
        return CACHE_CONTROL_DELAY;
    }

    /**
     * Getter for   MAX_CACHE_ENTRIES;
     */
    public  Integer getMaxCacheEntries() {
        return MAX_CACHE_ENTRIES;
    }

    /**
     * Setter for   MAX_CACHE_ENTRIES;
     */
    public  void setMaxCacheEntries(int maxCacheEntries) {
        MAX_CACHE_ENTRIES = maxCacheEntries;
    }
}
