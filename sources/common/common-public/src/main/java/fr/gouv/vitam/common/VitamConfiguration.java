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
package fr.gouv.vitam.common;

import com.google.common.base.Strings;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.SysErrLogger;

import java.io.File;
import java.util.Locale;

/**
 * This class contains default values shared among all services in Vitam
 */
public class VitamConfiguration {

    private static final VitamConfiguration DEFAULT_CONFIGURATION = new VitamConfiguration();
    /**
     * General Admin path
     */
    public static final String ADMIN_PATH = "/admin/v1";
    /**
     * General status path
     */
    public static final String STATUS_URL = "/status";
    /**
     * General tenants path
     */
    public static final String TENANTS_URL = "/tenants";
    /**
     * Property Vitam Config Folder
     */
    protected static  final String VITAM_CONFIG_PROPERTY = "vitam.config.folder";

    /**

     * Property Vitam Data Folder
     */
    protected static final String VITAM_DATA_PROPERTY = "vitam.data.folder";
    /**
     * Property Vitam Log Folder
     */
    protected static  final String VITAM_LOG_PROPERTY = "vitam.log.folder";
    /**
     * Property Vitam Tmp Folder
     */
    protected static  final String VITAM_TMP_PROPERTY = "vitam.tmp.folder";
    /**
     * Default Vitam Config Folder
     */
    private static  String VITAM_CONFIG_FOLDER_DEFAULT = "/vitam/conf";
    /**
     * Default Vitam Config Folder
     */
    private static  String VITAM_DATA_FOLDER_DEFAULT = "/vitam/data";
    /**
     * Default Vitam Config Folder
     */
    private static  String VITAM_LOG_FOLDER_DEFAULT = "/vitam/log";
    /**
     * Default Vitam Config Folder
     */
    private static  String VITAM_TMP_FOLDER_DEFAULT = "/vitam/data/tmp";

    private static Integer VITAM_CLEAN_PERIOD = 1 ;
    /**
     * Property Vitam Tmp Folder
     */
    private static final String VITAM_JUNIT_PROPERTY = "vitam.test.junit";
    /**
     * Default Chunk Size
     */
    private static  Integer CHUNK_SIZE = 65536;
    /**
     * Default Recv Buffer Size
     */
    private static  Integer RECV_BUFFER_SIZE = 0;
    /**
     * Default Connection timeout
     */
    private static  Integer CONNECT_TIMEOUT = 2000;
    /**
     * Default Read Timeout
     */
    private static  Integer READ_TIMEOUT = 86400000;

    /**
     *  Max shutdown timeout 2 minute
     */
    private final static long MAX_SHUTDOWN_TIMEOUT = 2*60*1000; 

    /**
     * Max total concurrent clients
     */
    private static  Integer MAX_TOTAL_CLIENT = 1000;
    /**
     * Max concurrent clients associated to one host
     */
    private static  Integer MAX_CLIENT_PER_HOST = 200;
    /**

     * Max delay to check an unused client in pool before being returned (Apache Only)
     */
    private static  Integer DELAY_VALIDATION_AFTER_INACTIVITY = 60000;
    /**
     * Max delay to check if no buffer is available while trying to continue to read (MultipleInputStreamHandler Only)
     *
     * Not final to allow Junit to decrease it
     */
    private static Integer DELAY_MULTIPLE_INPUTSTREAM = 60000;
    /**
     * Max delay to check if no buffer is available while trying to continue to read (SubStreams Only)
     */
    private static  Integer DELAY_MULTIPLE_SUBINPUTSTREAM = 6000;
    /**
     * Default minimum thread pool size
     */
    private static  Integer MINIMUM_THREAD_POOL_SIZE = 100;
    /**
     * No check of unused client within pool (Apache Only)
     */
    private static  Integer NO_VALIDATION_AFTER_INACTIVITY = -1;
    /**
     * Max delay to get a client (Apache Only)
     */
    private static  Integer DELAY_GET_CLIENT = 60000;

    /**
     * Specify the delay where connections returned to pool will be checked (Apache Only) (5 minutes)
     */
    private static  int INTERVAL_DELAY_CHECK_IDLE = 300000;

    /**
     * Specify the delay of unused connection returned in the pool before being really closed (Apache Only) (5 minutes)
     */
    private static  int MAX_DELAY_UNUSED_CONNECTION = 300000;

    /**
     * Use a new JAX_RS client each time
     */
    private static  Boolean USE_NEW_JAXR_CLIENT = true;

    /**
     * Default Digest Type for SECURITY
     */
    private static  DigestType SECURITY_DIGEST_TYPE = DigestType.SHA256;
    /**
     * Default Digest Type for Vitam
     */
    private static  DigestType DEFAULT_DIGEST_TYPE = DigestType.SHA512;
    /**
     * Default Digest Type for time stamp generation
     */
    private static  DigestType DEFAULT_TIMESTAMP_DIGEST_TYPE = DigestType.SHA512;
    /**
     * Acceptable Request Time elaps
     */
    private static  Long ACCEPTABLE_REQUEST_TIME = 10L;
    /**
     * MongoDB client configuration
     */
    private static  Integer THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS = 1500;
    /**
     * Retry repetition
     */
    private static  Integer RETRY_NUMBER = 3;
    /**
     * Retry delay
     */
    private static  Integer RETRY_DELAY = 30000;
    /**
     * Waiting delay (for wait(delay) method)
     */
    private static  Integer WAITING_DELAY = 1000;
    /**
     * Allow client and Server Encoding request or response in GZIP format
     */
    private static  Boolean ALLOW_GZIP_ENCODING = false;
    /**
     * Allow client to receive GZIP encoded response
     */
    private static  Boolean ALLOW_GZIP_DECODING = false;
    /**
     * Read ahead x4 Buffers
     */
    private static  Integer BUFFER_NUMBER = 4;
    // TODO make it configurable
    /**
     * Max concurrent multiple inputstream (memory size bounded = n x BUFFER_NUMBER * CHUNK_SIZE) 
     */
    private static  Integer MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER = 200;

    /**
     * Should we export #score (only Unit and ObjectGroup)
     */

    private static  boolean EXPORT_SCORE = false;

    private static  int DISTRIBUTEUR_BATCH_SIZE = 10;
    /*
     * Cache delay = 60 seconds
     */
    private static  int CACHE_CONTROL_DELAY = 60;



    /*
     * Max cache entries
     */
    private static  int MAX_CACHE_ENTRIES = 10000;

    /**
     * Max Elasticsearch Bulk
     */
    private static  int MAX_ELASTICSEARCH_BULK = 1000;
    /*
     * Max Thread for ES and MongoDB
     */
    private static  int NUMBER_DB_CLIENT_THREAD = 200;

    /**
     * Max queue in ES
     */
    private static  int NUMBER_ES_QUEUE = 5000;
    /**
     * Default LANG
     */
    private static  String DEFAULT_LANG = Locale.FRENCH.toString();

    private String config;
    private String log;
    private String data;
    private String tmp;
    private static String secret;
    private static Boolean filterActivation;
    private Integer connectTimeout = CONNECT_TIMEOUT;



    private static  boolean ENABLE_DISTRIBUTOR_V2 = true;
    private static  Boolean ENABLE_JAXB_PARSER = true;

    private static int ASYNC_WORKSPACE_QUEUE_SIZE = 10;


    static {
        getConfiguration().setDefault();
    }

    /**
     * Empty constructor
     */
    VitamConfiguration() {
        // empty
    }

    /**
     * Full argument constructor
     *
     * @param config
     * @param log
     * @param data
     * @param tmp
     *
     * @throws IllegalArgumentException if one argument is null or empty
     */
    VitamConfiguration(String config, String log, String data, String tmp) {
        setConfig(config).setData(data).setLog(log).setTmp(tmp);
        checkValues();
    }

    /**
     *
     * @return the default Vitam Configuration
     */
    public static VitamConfiguration getConfiguration() {
        return DEFAULT_CONFIGURATION;
    }

    /**
     *
     * @param vitamConfiguration
     */
    void setInternalConfiguration(VitamConfiguration vitamConfiguration) {
        setConfig(vitamConfiguration.getConfig())
            .setData(vitamConfiguration.getData())
            .setLog(vitamConfiguration.getLog())
            .setTmp(vitamConfiguration.getTmp()).checkValues();
    }

    /**
     * Replace the default values with values embedded in the VitamConfiguration parameter
     *
     * @param vitamConfiguration the new parameter
     * @throws IllegalArgumentException if one argument is null or empty
     */
    static void setConfiguration(VitamConfiguration vitamConfiguration) {
        DEFAULT_CONFIGURATION.setConfig(vitamConfiguration.getConfig())
            .setData(vitamConfiguration.getData())
            .setLog(vitamConfiguration.getLog())
            .setTmp(vitamConfiguration.getTmp()).checkValues();
    }

    /**
     * Replace the default values with values embedded in the VitamConfiguration parameter
     *
     * @param config
     * @param log
     * @param data
     * @param tmp
     * @throws IllegalArgumentException if one argument is null or empty
     */
    static void setConfiguration(String config, String log, String data, String tmp) {
        DEFAULT_CONFIGURATION.setConfig(config)
            .setData(data).setLog(log).setTmp(tmp).checkValues();
    }

    /**
     * Set the default values
     *
     * @return this
     */
    private VitamConfiguration setDefault() {
        connectTimeout = CONNECT_TIMEOUT;
        checkVitamConfiguration();
        checkValues();
        return this;
    }

    /**
     * Get Config directory
     *
     * @return the Config directory
     */
    public String getConfig() {
        return config;
    }

    /**
     * Set Config directory
     *
     * @param config the config directory
     * @return this
     */
    public VitamConfiguration setConfig(String config) {
        ParametersChecker.checkParameter("Config directory", config);
        this.config = config;
        return this;
    }

    /**
     * Get Log directory
     *
     * @return the Log directory
     */
    public String getLog() {
        return log;
    }

    /**
     * Set Log directory
     *
     * @param log the Log directory
     * @return this
     */
    public VitamConfiguration setLog(String log) {
        ParametersChecker.checkParameter("Log directory", log);
        this.log = log;
        return this;
    }

    /**
     * Get Data directory
     *
     * @return the Data directory
     */
    public String getData() {
        return data;
    }

    /**
     * Set Data directory
     *
     * @param data the Data directory
     * @return this
     */
    public VitamConfiguration setData(String data) {
        ParametersChecker.checkParameter("Data directory", data);
        this.data = data;
        return this;
    }

    /**
     * Get Tmp directory
     *
     * @return the Tmp directory
     */
    public String getTmp() {
        return tmp;
    }

    /**
     * Set Tmp directory
     *
     * @param tmp tmp the Tmp directory
     * @return this
     */
    public VitamConfiguration setTmp(String tmp) {
        ParametersChecker.checkParameter("Tmp directory", tmp);
        this.tmp = tmp;
        return this;
    }

    /**
     * import not null parameters configuration from VitamConfigurationParameters
     * @param parameters
     */
    public static void importConfigurationParameters(VitamConfigurationParameters parameters) {

        if (null != parameters.getVitamConfigFolderDefault()) {
            setVitamConfigFolderDefault(parameters.getVitamConfigFolderDefault());
        }
        if (null != parameters.getVitamDataFolderDefault()) {
            setVitamDataFolderDefault(parameters.getVitamDataFolderDefault());
        }
        if (null != parameters.getVitamLogFolderDefault()) {
            setVitamLogFolderDefault(parameters.getVitamLogFolderDefault());
        }
        if (null != parameters.getVitamTmpFolderDefault()) {
            setVitamTmpFolderDefault(parameters.getVitamTmpFolderDefault());
        }
        if (null != parameters.getChunkSize()) {
            setChunkSize(parameters.getChunkSize());
        }

        if (null != parameters.getRecvBufferSize()) {
            setRecvBufferSize(parameters.getRecvBufferSize());
        }

        if (null != parameters.getRecvBufferSize()) {
            setRecvBufferSize(parameters.getRecvBufferSize());
        }
        if (null != parameters.getConnectTimeout()) {
            setConnectTimeout(parameters.getConnectTimeout());
        }
        if (null != parameters.getReadTimeout()) {
            setReadTimeout(parameters.getReadTimeout());
        }
        if (null != parameters.getMaxTotalClient()) {
            setMaxTotalClient(parameters.getMaxTotalClient());
        }
        if (null != parameters.getMaxClientPerHost()) {
            setMaxClientPerHost(parameters.getMaxClientPerHost());
        }
        if (null != parameters.getDelayValidationAfterInactivity()) {
            setDelayValidationAfterInactivity(parameters.getDelayValidationAfterInactivity());
        }
        if (null != parameters.getDelayMultipleInputstream()) {
            setDelayMultipleInputstream(parameters.getDelayMultipleInputstream());
        }
        if (null != parameters.getDelayMultipleSubinputstream()) {
            setDelayMultipleSubinputstream(parameters.getDelayMultipleSubinputstream());
        }
        if (null != parameters.getMinimumThreadPoolSize()) {
            setMinimumThreadPoolSize(parameters.getMinimumThreadPoolSize());
        }
        if (null != parameters.getNoValidationAfterInactivity()) {
            setNoValidationAfterInactivity(parameters.getNoValidationAfterInactivity());
        }
        if (null != parameters.getDelayGetClient()) {
            setDelayGetClient(parameters.getDelayGetClient());
        }
        if (null != parameters.getIntervalDelayCheckIdle()) {
            setIntervalDelayCheckIdle(parameters.getIntervalDelayCheckIdle());
        }
        if (null != parameters.getMaxDelayUnusedConnection()) {
            setMaxDelayUnusedConnection(parameters.getMaxDelayUnusedConnection());
        }
        if (null != parameters.getSecurityDigestType()) {
                DigestType digestType = DigestType.valueOf(parameters.getSecurityDigestType());
                setSecurityDigestType(digestType);

        }
        if (null != parameters.getDefaultDigestType()) {
                DigestType digestType = DigestType.valueOf(parameters.getDefaultDigestType());
                setDefaultDigestType(digestType);

        }
        if (null != parameters.getDefaultTimestampDigestType()) {
                DigestType digestType = DigestType.valueOf(parameters.getDefaultTimestampDigestType());
                setDefaultTimestampDigestType(digestType);

        }
        if (null != parameters.getAcceptableRequestTime()) {
            setAcceptableRequestTime(parameters.getAcceptableRequestTime());
        }
        if (null != parameters.getThreadsAllowedToBlockForConnectionMultipliers()) {
            setThreadsAllowedToBlockForConnectionMultipliers(
                parameters.getThreadsAllowedToBlockForConnectionMultipliers());
        }
        if (null != parameters.getRetryNumber()) {
            setRetryNumber(parameters.getRetryNumber());
        }
        if (null != parameters.getRetryDelay()) {
            setRetryDelay(parameters.getRetryDelay());
        }
        if (null != parameters.getWaitingDelay()) {
            setWaitingDelay(parameters.getWaitingDelay());
        }
        if (null != parameters.getBufferNumber()) {
            setBufferNumber(parameters.getBufferNumber());
        }

        if (null != parameters.isAllowGzipEncoding()) {
            setAllowGzipEncoding(parameters.isAllowGzipEncoding());
        }
        if (null != parameters.isAllowGzipDecoding()) {
            setAllowGzipDecoding(parameters.isAllowGzipDecoding());
        }
        if (null != parameters.isUseNewJaxrClient()) {
            setUseNewJaxrClient(parameters.isUseNewJaxrClient());
        }
        if (null != parameters.isFilterActivation()) {
            setFilterActivation(parameters.isFilterActivation());
        }

        if (null != parameters.getMaxConcurrentMultipleInputstreamHandler()) {
            setMaxConcurrentMultipleInputstreamHandler(parameters.getMaxConcurrentMultipleInputstreamHandler());
        }
        if (null != parameters.isEnableJaxbParser()) {
            setEnableJaxbParser(parameters.isEnableJaxbParser());
        }
        if (null != parameters.getVitamCleanPeriod()) {
            setVitamCleanPeriod(parameters.getVitamCleanPeriod());
        }

        if (null != parameters.isEnableJaxbParser()) {
            setEnableJaxbParser(parameters.isEnableJaxbParser());
        }
        if (null != parameters.isEnableDistributorV2()) {
            setEnableDistributorV2(parameters.isEnableDistributorV2());
        }
        if (null != parameters.isExportScore()) {
            setExportScore(parameters.isExportScore());
        }
        if (null != parameters.getDistributeurBatchSize()) {
            setDistributeurBatchSize(parameters.getDistributeurBatchSize());
        }

        if (null != parameters.getMaxElasticsearchBulk()) {
            setMaxElasticsearchBulk(parameters.getMaxElasticsearchBulk());
        }
        if (null != parameters.getNumberDbClientThread()) {
            setNumberDbClientThread(parameters.getNumberDbClientThread());
        }
        if (null != parameters.getNumberEsQueue()) {
            setNumberEsQueue(parameters.getNumberEsQueue());
        }


        if (null != parameters.getCacheControlDelay()) {
            setCacheControlDelay(parameters.getCacheControlDelay());
        }
        if (null != parameters.getMaxCacheEntries()) {
            setMaxCacheEntries(parameters.getMaxCacheEntries());
        }

        if (null != parameters.isEnableJaxbParser()) {
            setEnableJaxbParser(parameters.isEnableJaxbParser());
        }


    }

    /**
     * Check if each directory not null and exists
     *
     * @throws IllegalArgumentException if one condition failed
     */
    private void checkValues() {
        ParametersChecker.checkParameter("Check directories", tmp, data, log, config);
        final File tmpDir = new File(tmp);
        final File logDir = new File(log);
        final File dataDir = new File(data);
        final File configDir = new File(config);
        if (!tmpDir.isDirectory()) {
            tmpDir.mkdirs();
        }
        if (!(tmpDir.isDirectory() && logDir.isDirectory() && dataDir.isDirectory() && configDir.isDirectory())) {
            SysErrLogger.FAKE_LOGGER.syserr("One of the directories in the VitamConfiguration is not correct");
        }
    }

    /**
     * Check if Vitam Configuration is specified using directives on JVM. If an issue is detected, it only logs the
     * status on STDERR.
     */
    static void checkVitamConfiguration() {
        if (!(SystemPropertyUtil.contains(VITAM_TMP_PROPERTY) && SystemPropertyUtil.contains(VITAM_CONFIG_PROPERTY) &&
            SystemPropertyUtil.contains(VITAM_DATA_PROPERTY) &&
            SystemPropertyUtil.contains(VITAM_LOG_PROPERTY))) {
            SysErrLogger.FAKE_LOGGER.syserr(
                "One of the directives is not specified: -Dxxx=path where xxx is one of -D" + VITAM_TMP_PROPERTY +
                    " -D" + VITAM_CONFIG_PROPERTY + " -D" + VITAM_DATA_PROPERTY + " -D" + VITAM_LOG_PROPERTY);
        }
        String data = VITAM_DATA_FOLDER_DEFAULT;
        if (SystemPropertyUtil.contains(VITAM_DATA_PROPERTY)) {
            data = SystemPropertyUtil.get(VITAM_DATA_PROPERTY);
        }
        String tmp = VITAM_TMP_FOLDER_DEFAULT;
        if (SystemPropertyUtil.contains(VITAM_TMP_PROPERTY)) {
            tmp = SystemPropertyUtil.get(VITAM_TMP_PROPERTY);
        }
        String config = VITAM_CONFIG_FOLDER_DEFAULT;
        if (SystemPropertyUtil.contains(VITAM_CONFIG_PROPERTY)) {
            config = SystemPropertyUtil.get(VITAM_CONFIG_PROPERTY);
        }
        String log = VITAM_LOG_FOLDER_DEFAULT;
        if (SystemPropertyUtil.contains(VITAM_LOG_PROPERTY)) {
            log = SystemPropertyUtil.get(VITAM_LOG_PROPERTY);
        }
        setConfiguration(config, log, data, tmp);
    }

    /**
     *
     * @return the VitamTmpFolder path
     */
    public static String getVitamTmpFolder() {
        if (SystemPropertyUtil.contains(VITAM_TMP_PROPERTY)) {
            return SystemPropertyUtil.get(VITAM_TMP_PROPERTY);
        }
        return getConfiguration().getTmp();
    }

    /**
     *
     * @return the VitamLogFolder path
     */
    public static String getVitamLogFolder() {
        return getConfiguration().getLog();
    }

    /**
     *
     * @return the VitamDataFolder path
     */
    public static String getVitamDataFolder() {
        return getConfiguration().getData();
    }

    /**
     *
     * @return the VitamConfigFolder path
     */
    public static String getVitamConfigFolder() {
        return getConfiguration().getConfig();
    }

    /**
     * @return the default chunk size
     */
    public static Integer getChunkSize() {
        return CHUNK_SIZE;
    }

    /**
     * @return the default connect timeout
     */
    public static Integer getConnectTimeout() {
        return getConfiguration().connectTimeout;
    }

    /**
     * Junit facility
     *
     * @param timeout to set
     */
    public static void setConnectTimeout(int timeout) {
        getConfiguration().connectTimeout = timeout;
    }

    /**
     * @return the default read timeout
     */
    public static Integer getReadTimeout() {
        return READ_TIMEOUT;
    }
    
    /**
     * @return the default read timeout
     */
    public static long getShutdownTimeout() {
        return MAX_SHUTDOWN_TIMEOUT;
    }

    /**
     * @return the maxTotalClient
     */
    public static Integer getMaxTotalClient() {
        return MAX_TOTAL_CLIENT;
    }

    /**
     * @return the maxClientPerHost
     */
    public static Integer getMaxClientPerHost() {
        return MAX_CLIENT_PER_HOST;
    }

    /**
     * @return the delayValidationAfterInactivity
     */
    public static Integer getDelayValidationAfterInactivity() {
        return DELAY_VALIDATION_AFTER_INACTIVITY;
    }

    /**
     * @return the delayGetClient
     */
    public static Integer getDelayGetClient() {
        return DELAY_GET_CLIENT;
    }

    /**
     * @return the intervalDelayCheckIdle
     */
    public static Integer getIntervalDelayCheckIdle() {
        return INTERVAL_DELAY_CHECK_IDLE;
    }

    /**
     * @return the maxDelayUnusedConnection
     */
    public static Integer getMaxDelayUnusedConnection() {
        return MAX_DELAY_UNUSED_CONNECTION;
    }

    /**
     * @return the secret
     */
    public static String getSecret() {
        if (Strings.isNullOrEmpty(secret)) {
            return "";
        }
        return secret;
    }

    /**
     * @param secretValue the secret to set
     *
     */
    public static void setSecret(String secretValue) {
        ParametersChecker.checkParameter("Platform secret", secretValue);
        secret = secretValue;
    }

    /**
     * @return the filterActivation
     */
    public static Boolean isFilterActivation() {
        return filterActivation;
    }

    /**
     * @param filterActivationValue the filterActivation to set
     *
     */
    public static void setFilterActivation(Boolean filterActivationValue) {
        filterActivation = filterActivationValue;
    }

    /**
     * @return the acceptableRequestTime
     */
    public static Long getAcceptableRequestTime() {
        return ACCEPTABLE_REQUEST_TIME;
    }

    /**
     * @return the securityDigestType
     */
    public static DigestType getSecurityDigestType() {
        return SECURITY_DIGEST_TYPE;
    }

    /**
     * @return the Default DigestType
     */
    public static DigestType getDefaultDigestType() {
        return DEFAULT_DIGEST_TYPE;
    }

    /**
     * @return the Default DigestType for time stamp generation
     */
    public static DigestType getDefaultTimestampDigestType() {
        return DEFAULT_TIMESTAMP_DIGEST_TYPE;
    }


    /**
     * @return the threadsAllowedToBlockForConnectionMultipliers for MongoDb Client
     */
    public static Integer getThreadsAllowedToBlockForConnectionMultipliers() {
        return THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS;
    }

    /**
     * @return the retryNumber
     */
    public static Integer getRetryNumber() {
        return RETRY_NUMBER;
    }

    /**
     * @return the retryDelay
     */
    public static Integer getRetryDelay() {
        return RETRY_DELAY;
    }

    /**
     * @return the waiting Delay (wait)
     */
    public static Integer getWaitingDelay() {
        return WAITING_DELAY;
    }
    

    /**
     * @return the size of the queue of async workspace
     */
    public static Integer getAsyncWorkspaceQueueSize() {
        return ASYNC_WORKSPACE_QUEUE_SIZE;
    }


    /**
     * @return the receive Buffer Size
     */
    public static Integer getRecvBufferSize() {
        return RECV_BUFFER_SIZE;
    }

    /**
     * @return the use New Jaxr Client each time a getClient() from Factory is used
     */
    public static Boolean isUseNewJaxrClient() {
        return USE_NEW_JAXR_CLIENT;
    }


    /**
     * @return true if is integration Test
     */
    public static boolean isIntegrationTest() {
        return SystemPropertyUtil.get(VITAM_JUNIT_PROPERTY, false);
    }
    
    /**
     * setIntegrationTest
     */
    public static void setIntegrationTest(boolean value) {
        SystemPropertyUtil.set(VITAM_JUNIT_PROPERTY, value);
    }


    /**
     * getter for VITAM_CONFIG_PROPERTY
     * @return
     */
    public static String getVitamConfigProperty() {
        return VITAM_CONFIG_PROPERTY;
    }



    /**
     * getter for VITAM_DATA_PROPERTY
     * @return
     */
    public static String getVitamDataProperty() {
        return VITAM_DATA_PROPERTY;
    }


    /**
     * getter for VITAM_LOG_PROPERTY
     * @return
     */
    public static String getVitamLogProperty() {
        return VITAM_LOG_PROPERTY;
    }

    /**
     * getter for VITAM_TMP_PROPERTY
     * @return
     */
    public static String getVitamTmpProperty() {
        return VITAM_TMP_PROPERTY;
    }


    /**
     * getter for VITAM_CONFIG_FOLDER_DEFAULT
     * @return
     */
    public static String getVitamConfigFolderDefault() {
        return VITAM_CONFIG_FOLDER_DEFAULT;
    }

    /**
     * setter for VITAM_CONFIG_FOLDER_DEFAULT
     * @return
     */
    private static void setVitamConfigFolderDefault(String vitamConfigFolderDefault) {
        VITAM_CONFIG_FOLDER_DEFAULT = vitamConfigFolderDefault;
    }

    /**
     * getter for VITAM_DATA_FOLDER_DEFAULT
     * @return
     */
    public static String getVitamDataFolderDefault() {
        return VITAM_DATA_FOLDER_DEFAULT;
    }

    /**
     * setter for VITAM_DATA_FOLDER_DEFAULT
     * @return
     */
    private static void setVitamDataFolderDefault(String vitamDataFolderDefault) {
        VITAM_DATA_FOLDER_DEFAULT = vitamDataFolderDefault;
    }

    /**
     * getter for VITAM_LOG_FOLDER_DEFAULT
     * @return
     */
    public static String getVitamLogFolderDefault() {
        return VITAM_LOG_FOLDER_DEFAULT;
    }

    /**
     * setter for VITAM_LOG_FOLDER_DEFAULT
     * @return
     */
    private static void setVitamLogFolderDefault(String vitamLogFolderDefault) {
        VITAM_LOG_FOLDER_DEFAULT = vitamLogFolderDefault;
    }

    /**
     * getter for VITAM_TMP_FOLDER_DEFAULT
     * @return
     */
    public static Integer getVitamCleanPeriod() {
        return VITAM_CLEAN_PERIOD;
    }


    /**
     * setter for VITAM_LOG_FOLDER_DEFAULT
     * @return
     */
    private static void setVitamCleanPeriod(Integer vitamCleanPeriod) {
        VITAM_CLEAN_PERIOD = vitamCleanPeriod;
    }

    /**
     * getter for VITAM_TMP_FOLDER_DEFAULT
     * @return
     */
    public static String getVitamTmpFolderDefault() {
        return VITAM_TMP_FOLDER_DEFAULT;
    }
    /**
     * setter for VITAM_TMP_FOLDER_DEFAULT
     * @return
     */
    private static void setVitamTmpFolderDefault(String vitamTmpFolderDefault) {
        VITAM_TMP_FOLDER_DEFAULT = vitamTmpFolderDefault;
    }
    /**
     * setter for CHUNK_SIZE
     * @return
     */
    private static void setChunkSize(int chunkSize) {
        CHUNK_SIZE = chunkSize;
    }

    /**
     * @return the size of the queue of async workspace
     */
    public static void setAsyncWorkspaceQueueSize(int queueSize) {
        ASYNC_WORKSPACE_QUEUE_SIZE = queueSize;
    }


    /**
     * setter for RECV_BUFFER_SIZE
     * @return
     */
    private static void setRecvBufferSize(int recvBufferSize) {
        RECV_BUFFER_SIZE = recvBufferSize;
    }

    /**
     * setter for READ_TIMEOUT
     * @return
     */
    private static void setReadTimeout(int readTimeout) {
        READ_TIMEOUT = readTimeout;
    }

    /**
     * setter for MAX_TOTAL_CLIENT
     * @return
     */
    private static void setMaxTotalClient(int maxTotalClient) {
        MAX_TOTAL_CLIENT = maxTotalClient;
    }

    /**
     * setter for MAX_CLIENT_PER_HOST
     * @return
     */
    private static void setMaxClientPerHost(int maxClientPerHost) {
        MAX_CLIENT_PER_HOST = maxClientPerHost;
    }

    /**
     * setter for DELAY_VALIDATION_AFTER_INACTIVITY
     * @return
     */
    private static void setDelayValidationAfterInactivity(int delayValidationAfterInactivity) {
        DELAY_VALIDATION_AFTER_INACTIVITY = delayValidationAfterInactivity;
    }

    /**
     * getter for DELAY_MULTIPLE_INPUTSTREAM
     * @return
     */
    public static Integer getDelayMultipleInputstream() {
        return DELAY_MULTIPLE_INPUTSTREAM;
    }

    /**
     * setter for DELAY_MULTIPLE_SUBINPUTSTREAM
     * @return
     */
    public static void setDelayMultipleInputstream(int delayMultipleInputstream) {
        DELAY_MULTIPLE_INPUTSTREAM = delayMultipleInputstream;
    }

    /**
     * getter for DELAY_MULTIPLE_SUBINPUTSTREAM
     * @return
     */
    public static Integer getDelayMultipleSubinputstream() {
        return DELAY_MULTIPLE_SUBINPUTSTREAM;
    }

    /**
     * setter for DELAY_MULTIPLE_SUBINPUTSTREAM
     * @return
     */
    private static void setDelayMultipleSubinputstream(int delayMultipleSubinputstream) {
        DELAY_MULTIPLE_SUBINPUTSTREAM = delayMultipleSubinputstream;
    }

    /**
     * getter for MINIMUM_THREAD_POOL_SIZE
     * @return
     */
    public static Integer getMinimumThreadPoolSize() {
        return MINIMUM_THREAD_POOL_SIZE;
    }

    /**
     * setter for MINIMUM_THREAD_POOL_SIZE
     * @return
     */
    private static void setMinimumThreadPoolSize(int minimumThreadPoolSize) {
        MINIMUM_THREAD_POOL_SIZE = minimumThreadPoolSize;
    }

    /**
     * getter for NO_VALIDATION_AFTER_INACTIVITY
     * @return
     */
    public static Integer getNoValidationAfterInactivity() {
        return NO_VALIDATION_AFTER_INACTIVITY;
    }

    /**
     * setter for NO_VALIDATION_AFTER_INACTIVITY
     * @return
     */
    private static void setNoValidationAfterInactivity(int noValidationAfterInactivity) {
        NO_VALIDATION_AFTER_INACTIVITY = noValidationAfterInactivity;
    }

    /**
     * setter for DELAY_GET_CLIENT
     * @return
     */
    private static void setDelayGetClient(int delayGetClient) {
        DELAY_GET_CLIENT = delayGetClient;
    }

    /**
     * setter for INTERVAL_DELAY_CHECK_IDLE
     * @return
     */
    private static void setIntervalDelayCheckIdle(int intervalDelayCheckIdle) {
        INTERVAL_DELAY_CHECK_IDLE = intervalDelayCheckIdle;
    }

    /**
     * setter for MAX_DELAY_UNUSED_CONNECTION
     * @return
     */
    private static void setMaxDelayUnusedConnection(int maxDelayUnusedConnection) {
        MAX_DELAY_UNUSED_CONNECTION = maxDelayUnusedConnection;
    }

    /**
     * setter for USE_NEW_JAXR_CLIENT
     * @return
     */
    private static void setUseNewJaxrClient(Boolean useNewJaxrClient) {
        USE_NEW_JAXR_CLIENT = useNewJaxrClient;
    }




    /**
     * setter for SECURITY_DIGEST_TYPE
     * @return
     */
    private static void setSecurityDigestType(DigestType securityDigestType) {
        SECURITY_DIGEST_TYPE = securityDigestType;
    }

    /**
     * setter for DEFAULT_DIGEST_TYPE
     * @return
     */
    private static void setDefaultDigestType(DigestType defaultDigestType) {
        DEFAULT_DIGEST_TYPE = defaultDigestType;
    }

    /**
     * setter for DEFAULT_TIMESTAMP_DIGEST_TYPE
     * @return
     */
    private static void setDefaultTimestampDigestType(DigestType defaultTimestampDigestType) {
        DEFAULT_TIMESTAMP_DIGEST_TYPE = defaultTimestampDigestType;
    }

    /**
     * setter for ACCEPTABLE_REQUEST_TIME
     * @return
     */
    private static void setAcceptableRequestTime(Long acceptableRequestTime) {
        ACCEPTABLE_REQUEST_TIME = acceptableRequestTime;
    }

    /**
     * setter for THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS
     * @return
     */
    private static void setThreadsAllowedToBlockForConnectionMultipliers(
        Integer threadsAllowedToBlockForConnectionMultipliers) {
        THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIERS = threadsAllowedToBlockForConnectionMultipliers;
    }

    /**
     * setter for RETRY_NUMBER
     * @return
     */
    private static void setRetryNumber(int retryNumber) {
        RETRY_NUMBER = retryNumber;
    }

    /**
     * setter for RETRY_DELAY
     * @return
     */
    private static void setRetryDelay(int retryDelay) {
        RETRY_DELAY = retryDelay;
    }

    /**
     * setter for WAITING_DELAY
     * @return
     */
    private static void setWaitingDelay(int waitingDelay) {
        WAITING_DELAY = waitingDelay;
    }

    /**
     * getter for ALLOW_GZIP_ENCODING
     * @return
     */
    public static Boolean isAllowGzipEncoding() {
        return ALLOW_GZIP_ENCODING;
    }

    /**
     * setter for ALLOW_GZIP_ENCODING
     * @return
     */
    private static void setAllowGzipEncoding(Boolean allowGzipEncoding) {
        ALLOW_GZIP_ENCODING = allowGzipEncoding;
    }

    /**
     * getter for ALLOW_GZIP_DECODING
     * @return
     */
    public static Boolean isAllowGzipDecoding() {
        return ALLOW_GZIP_DECODING;
    }

    /**
     * setter for ALLOW_GZIP_DECODING
     * @return
     */
    private static void setAllowGzipDecoding(Boolean allowGzipDecoding) {
        ALLOW_GZIP_DECODING = allowGzipDecoding;
    }

    /**
     * getter for BUFFER_NUMBER
     * @return
     */
    public static Integer getBufferNumber() {
        return BUFFER_NUMBER;
    }

    /**
     * setter for BUFFER_NUMBER
     * @return
     */
    private static void setBufferNumber(int bufferNumber) {
        BUFFER_NUMBER = bufferNumber;
    }

    /**
     * getter for MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER
     * @return
     */
    public static Integer getMaxConcurrentMultipleInputstreamHandler() {
        return MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER;
    }

    /**
     * setter for MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER
     * @return
     */
    private static void setMaxConcurrentMultipleInputstreamHandler(int maxConcurrentMultipleInputstreamHandler) {
        MAX_CONCURRENT_MULTIPLE_INPUTSTREAM_HANDLER = maxConcurrentMultipleInputstreamHandler;
    }

    /**
     * getter for ENABLE_JAXB_PARSER
     * @return
     */
    public static Boolean isEnableJaxbParser() {
        return ENABLE_JAXB_PARSER;
    }

    /**
     * setter for ENABLE_JAXB_PARSER
     * @return
     */
    private static void setEnableJaxbParser(Boolean enableJaxbParser) {
        ENABLE_JAXB_PARSER = enableJaxbParser;
    }

    /**
     * getter for DEFAULT_LANG
     * @return
     */
    public static String getDefaultLang() {
        return DEFAULT_LANG;
    }

    /**
     * setter for DEFAULT_LANG
     * @return
     */
    public static void setDefaultLang(String defaultLang) {
        DEFAULT_LANG = defaultLang;
    }


    /**
     * Setter for   ENABLE_JAXB_PARSER;
     */
    public static void setEnableJaxbParser(boolean enableJaxbParser) {
        ENABLE_JAXB_PARSER = enableJaxbParser;
    }

    /**
     * Getter for   ENABLE_DISTRIBUTOR_V2;
     */
    public static boolean isEnableDistributorV2() {
        return ENABLE_DISTRIBUTOR_V2;
    }

    /**
     * Setter for   ENABLE_DISTRIBUTOR_V2;
     */
    private static void setEnableDistributorV2(boolean enableDistributorV2) {
        ENABLE_DISTRIBUTOR_V2 = enableDistributorV2;
    }



    /**
     * Getter for   DISTRIBUTEUR_BATCH_SIZE;
     */
    public static int getDistributeurBatchSize() {
        return DISTRIBUTEUR_BATCH_SIZE;
    }

    /**
     * Setter for   DISTRIBUTEUR_BATCH_SIZE;
     */
    private static void setDistributeurBatchSize(int distributeurBatchSize) {
        DISTRIBUTEUR_BATCH_SIZE = distributeurBatchSize;
    }

    /**
     * Getter for   CACHE_CONTROL_DELAY;
     */
    public static int getCacheControlDelay() {
        return CACHE_CONTROL_DELAY;
    }

    /**
     * Setter for   CACHE_CONTROL_DELAY;
     */
    public static void setCacheControlDelay(int cacheControlDelay) {
        CACHE_CONTROL_DELAY = cacheControlDelay;
    }

    /**
     * Getter for   MAX_CACHE_ENTRIES;
     */
    public static int getMaxCacheEntries() {
        return MAX_CACHE_ENTRIES;
    }

    /**
     * Setter for   MAX_CACHE_ENTRIES;
     */
    public static void setMaxCacheEntries(int maxCacheEntries) {
        MAX_CACHE_ENTRIES = maxCacheEntries;
    }
    /**
     * Getter for   EXPORT_SCORE;
     */
    public static boolean isExportScore() {
        return EXPORT_SCORE;
    }
    /**
     * Setter for   EXPORT_SCORE;
     */
    private static void setExportScore(boolean exportScore) {
        EXPORT_SCORE = exportScore;
    }





    /**
     * Getter for   MAX_ELASTICSEARCH_BULK;
     */
    public static int getMaxElasticsearchBulk() {
        return MAX_ELASTICSEARCH_BULK;
    }

    /**
     * Setter for   MAX_ELASTICSEARCH_BULK;
     */
    private static void setMaxElasticsearchBulk(int maxElasticsearchBulk) {
        MAX_ELASTICSEARCH_BULK = maxElasticsearchBulk;
    }

    /**
     * Getter for   NUMBER_DB_CLIENT_THREAD;
     */
    public static int getNumberDbClientThread() {
        return NUMBER_DB_CLIENT_THREAD;
    }

    /**
     * Setter for   NUMBER_DB_CLIENT_THREAD;
     */
    private static void setNumberDbClientThread(int numberDbClientThread) {
        NUMBER_DB_CLIENT_THREAD = numberDbClientThread;
    }

    /**
     * Getter for   NUMBER_ES_QUEUE;
     */

    public static Integer getNumberEsQueue() {
        return NUMBER_ES_QUEUE;
    }

    /**
     * Setter for   NUMBER_ES_QUEUE;
     */
    private static void setNumberEsQueue(int numberEsQueue) {
        NUMBER_ES_QUEUE = numberEsQueue;
    }
}
