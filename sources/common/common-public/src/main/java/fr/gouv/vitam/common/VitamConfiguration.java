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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.configuration.ClassificationLevel;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.SysErrLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class contains default values shared among all services in Vitam
 */
public class VitamConfiguration {

    private static final VitamConfiguration DEFAULT_CONFIGURATION = new VitamConfiguration();

    /* FINAL ATTRIBUTES */
    /**
     * General Admin path
     */
    public static final String ADMIN_PATH = "/admin/v1";
    /**
     * General admin auto test path
     */
    public static final String AUTOTEST_URL = "/autotest";
    /**
     * General admin version path
     */
    public static final String VERSION_URL = "/version";
    /**
     * General status path
     */
    public static final String STATUS_URL = "/status";
    /**
     * General tenants path
     */
    public static final String TENANTS_URL = "/tenants";
    /**
     * login path
     */
    public static final String LOGIN_URL = "/login";
    /**
     * logout path
     */
    public static final String LOGOUT_URL = "/logout";
    /**
     * path for messages/logbook
     */
    public static final String MESSAGES_LOGBOOK_URL = "/messages/logbook";
    /**
     * path for download object data
     */
    public static final String OBJECT_DOWNLOAD_URL = "/archiveunit/objects/download";

    /**
     * path for exporting DIP
     */
    public static final String DIP_EXPORT_URL = "/archiveunit/dipexport";
    /**
     * path for securemode
     */
    public static final String SECURE_MODE_URL = "/securemode";
    /**
     * path for admintenant
     */
    public static final String ADMIN_TENANT_URL = "/admintenant";
    /**
     * path for permissions
     */
    public static final String PERMISSIONS_URL = "permissions";
    /**
     * Property Vitam Config Folder
     */
    protected static final String VITAM_CONFIG_PROPERTY = "vitam.config.folder";

    /**
     * Property Vitam Data Folder
     */
    protected static final String VITAM_DATA_PROPERTY = "vitam.data.folder";
    /**
     * Property Vitam Log Folder
     */
    protected static final String VITAM_LOG_PROPERTY = "vitam.log.folder";
    /**
     * Property Vitam Tmp Folder
     */
    protected static final String VITAM_TMP_PROPERTY = "vitam.tmp.folder";

    /**
     * Property Vitam Tmp Folder
     */
    private static final String VITAM_JUNIT_PROPERTY = "vitam.test.junit";

    /**
     * Default Connection timeout
     */
    private static final Integer CONNECT_TIMEOUT = 2000;
    /**
     * swift file limit to upload
     */
    private static final long SWIFT_FILE_LIMIT = 4_000_000_000L;

    /**
     * Max shutdown timeout 2 minute
     */
    private final static long MAX_SHUTDOWN_TIMEOUT = 2 * 60 * 1000;


    /**
     * OTHERS ATTRIBUTES
     */
    /**
     * Default Vitam Config Folder
     */
    private static String vitamConfigFolderDefault = "/vitam/conf";
    /**
     * Default Vitam Config Folder
     */
    private static String vitamDataFolderDefault = "/vitam/data";
    /**
     * Default Vitam Config Folder
     */
    private static String vitamLogFolderDefault = "/vitam/log";
    /**
     * Default Vitam Config Folder
     */
    private static String vitamTmpFolderDefault = "/vitam/data/tmp";
    /**
     * Default Vitam griffin Folder executor
     */
    private static String vitamGriffinExecFolder = "/vitam/bin/worker/griffins";
    /**
     * Default Vitam griffin folder for transformed data
     */
    private static String vitamGriffinInputFilesFolder = "/vitam/tmp/worker/griffins";

    private static String workspaceWorkflowsFolder = "workflows";


    private static Integer vitamCleanPeriod = 1;

    private static Integer elasticSearchScrollTimeoutInMilliseconds = 60_000 * 5;

    private static Integer elasticSearchScrollLimit = 10_000;

    /**
     * Default Chunk Size
     */
    private static Integer chunkSize = 65536;
    /**
     * Default Recv Buffer Size
     */
    private static Integer recvBufferSize = 0;

    /**
     * Default Read Timeout
     */
    private static Integer readTimeout = 86400000;

    /**
     * Max total concurrent clients
     */
    private static Integer maxTotalClient = 1000;
    /**
     * Max concurrent clients associated to one host
     */
    private static Integer maxClientPerHost = 200;
    /**
     * Max delay to check an unused client in pool before being returned (Apache Only)
     */
    private static Integer delayValidationAfterInactivity = 60000;
    /**
     * Max delay to check if no buffer is available while trying to continue to read (MultipleInputStreamHandler Only)
     * <p>
     * Not final to allow Junit to decrease it
     */
    private static Integer delayMultipleInputstream = 60000;
    /**
     * Max delay to check if no buffer is available while trying to continue to read (SubStreams Only)
     */
    private static Integer delayMultipleSubinputstream = 6000;

    /**
     * Default minimum thread pool size
     */
    private static Integer minimumThreadPoolSize = 100;
    /**
     * No check of unused client within pool (Apache Only)
     */
    private static Integer noValidationAfterInactivity = -1;
    /**
     * Max delay to get a client (Apache Only)
     */
    private static Integer delayGetClient = 60000;

    /**
     * Specify the delay where connections returned to pool will be checked (Apache Only) (5 minutes)
     */
    private static int intervalDelayCheckIdle = 300000;

    /**
     * Specify the delay of unused connection returned in the pool before being really closed (Apache Only) (5 minutes)
     */
    private static int maxDelayUnusedConnection = 300000;

    /**
     * Use a new JAX_RS client each time
     */
    private static Boolean useNewJaxrClient = false;

    /**
     * Default Digest Type for SECURITY
     */
    private static DigestType securityDigestType = DigestType.SHA256;
    /**
     * Default Digest Type for Vitam
     */
    private static DigestType defaultDigestType = DigestType.SHA512;
    /**
     * Default Digest Type for time stamp generation
     */
    private static DigestType defaultTimestampDigestType = DigestType.SHA512;
    /**
     * Acceptable Request Time elaps
     */
    private static Long acceptableRequestTime = 10L;
    /**
     * MongoDB client configuration
     */
    private static Integer threadsAllowedToBlockForConnectionMultipliers = 1500;
    /**
     * Retry repetition
     */
    private static Integer retryNumber = 3;
    /**
     * Retry delay
     */
    private static Integer retryDelay = 30000;
    /**
     * Waiting delay (for wait(delay) method)
     */
    private static Integer waitingDelay = 1000;
    /**
     * Allow client and Server Encoding request or response in GZIP format
     */
    private static Boolean allowGzipEncoding = false;
    /**
     * Allow client to receive GZIP encoded response
     */
    private static Boolean allowGzipDecoding = false;
    /**
     * Read ahead x4 Buffers
     */
    private static Integer bufferNumber = 4;
    // TODO make it configurable
    /**
     * Max concurrent multiple inputstream (memory size bounded = n x bufferNumber * chunkSize)
     */
    private static Integer maxConcurrentMultipleInputstreamHandler = 200;

    /**
     * Should we export #score (only Unit and ObjectGroup)
     */

    private static boolean exportScore = false;

    /**
     * Distributor batch size
     */
    private static int distributeurBatchSize = 100;
    /**
     * Worker bulk size
     */
    private static int workerBulkSize = 10;
    /**
     * Restore bulk size
     */
    private static int restoreBulkSize = 1000;

    /*
     * Cache delay = 60 seconds
     */
    private static int cacheControlDelay = 60;
    /*
     * Max cache entries
     */
    private static int maxCacheEntries = 10000;

    /**
     * Expire time for the cache entries in seconds (5 minutes by default)
     */
    private static int expireCacheEntriesDelay = 300;

    /**
     * Max Elasticsearch Bulk
     */
    private static int maxElasticsearchBulk = 1000;

    /**
     * batchSize for mongodb and used in lifecycleSpliterator and store graph service for the limit
     */
    private static int batchSize = 1000;

    /*
     * Number of elements per file use for store graph service
     */
    private static int storeGraphElementsPerFile = 10000;
    /**
     * The overlap delay (in seconds) for store graph operation. Used to do not treat elements in critical state due to
     * clock difference or GC slow down or VM freeze
     */
    private static int storeGraphOverlapDelay = 300;

    /*
     * Data migration bulk size
     */
    private static int migrationBulkSize = 10000;

    /**
     * The time in seconds 60*60*24*30 (default 30 days) to wait before deleting reconstructed with only graph data
     * units The unit should contains only graph data and the graph last persisted date should be 30 day older
     */
    private static int deleteIncompleteReconstructedUnitDelay = 2592000;

    /**
     * The number of retry executing action when optimistic lock occurs
     */
    private static int optimisticLockRetryNumber = 50;
    /**
     * Optimistic lock sleep time in milliseconds, the sleep time after each retry
     */
    private static int optimisticLockSleepTime = 20;


    /**
     * This is a limitation of lucene.
     * Fields whose UTF8 encoding is longer than the max length 32766 are not accepted
     */
    private static int keywordMaxLength = 32766;
    /**
     * There is not a limitation in lucene for text fields.
     * In VITAM, to enable sorting on some fields (title, ...), those fields are also not analysed (fielddata set to true)
     *
     * Problem:
     * - Indexing text fields with value length > keywordMaxLength
     * - Change mapping on ES to set fielddata = true on those fields
     * - Re-index
     * => Lucene will throws an exception as keywords can't be longer than max length (keywordMaxLength)
     *
     * So this is a vitam limitation.
     */
    private static int textMaxLength = 32766;


    /**
     * default offset for lifecycleSpliterator
     */
    private static int defaultOffset = 0;

    /**
     * Max Thread for ES and MongoDB
     */
    private static int numberDbClientThread = 200;
    /**
     * Max queue in ES
     */
    private static int numberEsQueue = 5000;
    /**
     * Threshold for distribution
     */
    private static long distributionThreshold = 100000L;
    /**
     * Threshold for elimination analysis
     */
    private static long eliminationAnalysisThreshold = 100_000L;
    /**
     * Threshold for elimination action
     */
    private static long eliminationActionThreshold = 10_000L;
    private static int ontologyCacheMaxEntries = 100;
    private static int ontologyCacheTimeoutInSeconds = 300;
    /**
     * Default OriginatingAgency for DIP export with multiple originating agencies
     */
    private static final String DEFAULT_ORIGINATING_AGENCY_FOR_EXPORT = "Export VITAM";
    /**
     * Map to override defalt Originating agency for each tenant for a DIP Export with multiple originating agencies
     */
    private static Map<Integer, String> defaultOriginatingAgencyByTenant = new HashMap<Integer, String>();
    /**
     * Default LANG
     */
    private static String DEFAULT_LANG = Locale.FRENCH.toString();

    private String config;

    private String log;

    private String data;

    private String tmp;

    private static String secret;

    private static boolean purgeTemporaryLFC = true;

    private static Boolean filterActivation;

    private Integer connectTimeout = CONNECT_TIMEOUT;

    private static int asyncWorkspaceQueueSize = 10;

    /**
     * Force chunked mode
     */
    private static Boolean forceChunkModeInputStream = false;


    /*
     * maxResultWindow
     */
    private static int maxResultWindow = 10000;

    /*
     * List of TENANTS
     */
    private static List<Integer> TENANTS = new ArrayList<>();

    /*
     * Admin Tenant
     */
    private static int ADMIN_TENANT = -1;

    /**
     * Environment name used for storage offer container prefix (by default, set to empty string)
     */
    private static String environmentName;

    /**
     * Max dsl queries per reclassification request
     */
    private static int reclassificationMaxBulkThreshold = 1000;
    /**
     * Max units to update per reclassification request
     */
    private static int reclassificationMaxUnitsThreshold = 10000;
    /**
     * Max dsl queries per reclassification request
     */
    private static int reclassificationMaxGuildListSizeInLogbookOperation = 1000;

    /**
     * classification level for the Vitam plateform useful for worker ingest / mass update / update unit
     */
    private static ClassificationLevel classificationLevel;

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
     * @throws IllegalArgumentException if one argument is null or empty
     */
    VitamConfiguration(String config, String log, String data, String tmp) {
        setConfig(config).setData(data).setLog(log).setTmp(tmp);
        checkValues();
    }

    /**
     * @return the default Vitam Configuration
     */
    public static VitamConfiguration getConfiguration() {
        return DEFAULT_CONFIGURATION;
    }

    /**
     * Getter for admin Tenant
     *
     * @return adminTenant
     */
    public static Integer getAdminTenant() {
        return ADMIN_TENANT;
    }

    /**
     * Setter for admin Tenant
     *
     * @param adminTenant
     */
    public static void setAdminTenant(Integer adminTenant) {
        ADMIN_TENANT = adminTenant;
    }

    /**
     * Getter for list of tenant
     *
     * @return tenant list
     */
    public static List<Integer> getTenants() {
        return TENANTS;
    }

    /**
     * @param tenants
     */
    public static void setTenants(List<Integer> tenants) {
        TENANTS = tenants;
    }

    public static boolean isPurgeTemporaryLFC() {
        return purgeTemporaryLFC;
    }

    public static void setPurgeTemporaryLFC(boolean purgeTemporaryLFC) {
        VitamConfiguration.purgeTemporaryLFC = purgeTemporaryLFC;
    }

    public static int getOntologyCacheMaxEntries() {
        return ontologyCacheMaxEntries;
    }

    public static void setOntologyCacheMaxEntries(int ontologyCacheMaxEntries) {
        VitamConfiguration.ontologyCacheMaxEntries = ontologyCacheMaxEntries;
    }

    public static int getOntologyCacheTimeoutInSeconds() {
        return ontologyCacheTimeoutInSeconds;
    }

    public static void setOntologyCacheTimeoutInSeconds(int ontologyCacheTimeoutInSeconds) {
        VitamConfiguration.ontologyCacheTimeoutInSeconds = ontologyCacheTimeoutInSeconds;
    }

    /**
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
     *
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
        if (null != parameters.getVitamCleanPeriod()) {
            setVitamCleanPeriod(parameters.getVitamCleanPeriod());
        }
        if (null != parameters.isExportScore()) {
            setExportScore(parameters.isExportScore());
        }
        if (null != parameters.getDistributeurBatchSize()) {
            setDistributeurBatchSize(parameters.getDistributeurBatchSize());
        }
        if (null != parameters.getWorkerBulkSize()) {
            setWorkerBulkSize(parameters.getWorkerBulkSize());
        }
        if (null != parameters.getRestoreBulkSize()) {
            setWorkerBulkSize(parameters.getRestoreBulkSize());
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
        if (null != parameters.getDistributionThreshold()) {
            setDistributionThreshold(parameters.getDistributionThreshold());
        }
        if (null != parameters.getEliminationAnalysisThreshold()) {
            setEliminationAnalysisThreshold(parameters.getEliminationAnalysisThreshold());
        }
        if (null != parameters.getEliminationActionThreshold()) {
            setEliminationActionThreshold(parameters.getEliminationActionThreshold());
        }
        if (null != parameters.getCacheControlDelay()) {
            setCacheControlDelay(parameters.getCacheControlDelay());
        }
        if (null != parameters.getMaxCacheEntries()) {
            setMaxCacheEntries(parameters.getMaxCacheEntries());
        }

        if (null != parameters.getExpireCacheEntriesDelay()) {
            setExpireCacheEntriesDelay(parameters.getExpireCacheEntriesDelay());
        }

        if (null != parameters.getAdminTenant()) {
            setAdminTenant(parameters.getAdminTenant());
        }

        if (null != parameters.getTenants()) {
            setTenants(parameters.getTenants());
        }

        if (null != parameters.getDefaultOriginatingAgencyForExport()) {
            setDefaultOriginatingAgencyByTenant(parameters.getDefaultOriginatingAgencyForExport());
        }

        if (null != parameters.getStoreGraphElementsPerFile()) {
            setStoreGraphElementsPerFile(parameters.getStoreGraphElementsPerFile());
        }

        if (null != parameters.getStoreGraphOverlapDelay()) {
            setStoreGraphOverlapDelay(parameters.getStoreGraphOverlapDelay());
        }

        if (null != parameters.getMigrationBulkSize()) {
            setMigrationBulkSize(parameters.getMigrationBulkSize());
        }

        if (null != parameters.getDeleteIncompleteReconstructedUnitDelay()) {
            setDeleteIncompleteReconstructedUnitDelay(parameters.getDeleteIncompleteReconstructedUnitDelay());
        }

        if (null != parameters.getOptimisticLockRetryNumber()) {
            setOptimisticLockRetryNumber(parameters.getOptimisticLockRetryNumber());
        }

        if (null != parameters.getOptimisticLockSleepTime()) {
            setOptimisticLockSleepTime(parameters.getOptimisticLockSleepTime());
        }

        if (null != parameters.isForceChunkModeInputStream()) {
            setForceChunkModeInputStream(parameters.isForceChunkModeInputStream());
        }

        if (null != parameters.getReclassificationMaxBulkThreshold()) {
            setReclassificationMaxBulkThreshold(parameters.getReclassificationMaxBulkThreshold());
        }

        if (null != parameters.getReclassificationMaxUnitsThreshold()) {
            setReclassificationMaxUnitsThreshold(parameters.getReclassificationMaxUnitsThreshold());
        }

        if (null != parameters.getReclassificationMaxGuildListSizeInLogbookOperation()) {
            setReclassificationMaxGuildListSizeInLogbookOperation(
                parameters.getReclassificationMaxGuildListSizeInLogbookOperation());
        }

        if (null != parameters.getKeywordMaxLength()) {
            setKeywordMaxLength(parameters.getKeywordMaxLength());
        }

        if (null != parameters.getTextMaxLength()) {
            setTextMaxLength(parameters.getTextMaxLength());
        }

        if (null != parameters.getClassificationLevel()) {
            setClassificationLevel(parameters.getClassificationLevel());
        }

        if (null != parameters.getEnvironmentName()) {
            setEnvironmentName(parameters.getEnvironmentName());
        }

        if (null != parameters.getOntologyCacheMaxEntries()) {
            setOntologyCacheMaxEntries(parameters.getOntologyCacheMaxEntries());
        }

        if (null != parameters.getOntologyCacheTimeoutInSeconds()) {
            setOntologyCacheTimeoutInSeconds(parameters.getOntologyCacheTimeoutInSeconds());
        }

        if (null != parameters.getElasticSearchScrollTimeoutInMilliseconds()) {
            setElasticSearchScrollTimeoutInMilliseconds(parameters.getElasticSearchScrollTimeoutInMilliseconds());
        }

        if (null != parameters.getElasticSearchScrollLimit()) {
            setElasticSearchScrollLimit(parameters.getElasticSearchScrollLimit());
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

    @VisibleForTesting
    public static void reinit() {
        checkVitamConfiguration();
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
        String data = vitamDataFolderDefault;
        if (SystemPropertyUtil.contains(VITAM_DATA_PROPERTY)) {
            data = SystemPropertyUtil.get(VITAM_DATA_PROPERTY);
        }
        String tmp = vitamTmpFolderDefault;
        if (SystemPropertyUtil.contains(VITAM_TMP_PROPERTY)) {
            tmp = SystemPropertyUtil.get(VITAM_TMP_PROPERTY);
        }
        String config = vitamConfigFolderDefault;
        if (SystemPropertyUtil.contains(VITAM_CONFIG_PROPERTY)) {
            config = SystemPropertyUtil.get(VITAM_CONFIG_PROPERTY);
        }
        String log = vitamLogFolderDefault;
        if (SystemPropertyUtil.contains(VITAM_LOG_PROPERTY)) {
            log = SystemPropertyUtil.get(VITAM_LOG_PROPERTY);
        }
        setConfiguration(config, log, data, tmp);
    }

    /**
     * @return the VitamTmpFolder path
     */
    public static String getVitamTmpFolder() {
        if (SystemPropertyUtil.contains(VITAM_TMP_PROPERTY)) {
            return SystemPropertyUtil.get(VITAM_TMP_PROPERTY);
        }
        return getConfiguration().getTmp();
    }

    /**
     * @return the VitamLogFolder path
     */
    public static String getVitamLogFolder() {
        return getConfiguration().getLog();
    }

    /**
     * @return the VitamDataFolder path
     */
    public static String getVitamDataFolder() {
        return getConfiguration().getData();
    }

    /**
     * @return the VitamConfigFolder path
     */
    public static String getVitamConfigFolder() {
        return getConfiguration().getConfig();
    }

    /**
     * @return the default chunk size
     */
    public static Integer getChunkSize() {
        return chunkSize;
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
        return readTimeout;
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
        return maxTotalClient;
    }

    /**
     * @return the maxClientPerHost
     */
    public static Integer getMaxClientPerHost() {
        return maxClientPerHost;
    }

    /**
     * @return the delayValidationAfterInactivity
     */
    public static Integer getDelayValidationAfterInactivity() {
        return delayValidationAfterInactivity;
    }

    /**
     * @return the delayGetClient
     */
    public static Integer getDelayGetClient() {
        return delayGetClient;
    }

    /**
     * @return the intervalDelayCheckIdle
     */
    public static Integer getIntervalDelayCheckIdle() {
        return intervalDelayCheckIdle;
    }

    /**
     * @return the maxDelayUnusedConnection
     */
    public static Integer getMaxDelayUnusedConnection() {
        return maxDelayUnusedConnection;
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
     */
    public static void setFilterActivation(Boolean filterActivationValue) {
        filterActivation = filterActivationValue;
    }

    /**
     * @return the acceptableRequestTime
     */
    public static Long getAcceptableRequestTime() {
        return acceptableRequestTime;
    }

    /**
     * @return the securityDigestType
     */
    public static DigestType getSecurityDigestType() {
        return securityDigestType;
    }

    /**
     * @return the Default DigestType
     */
    public static DigestType getDefaultDigestType() {
        return defaultDigestType;
    }

    /**
     * @return the Default DigestType for time stamp generation
     */
    public static DigestType getDefaultTimestampDigestType() {
        return defaultTimestampDigestType;
    }


    /**
     * @return the threadsAllowedToBlockForConnectionMultipliers for MongoDb Client
     */
    public static Integer getThreadsAllowedToBlockForConnectionMultipliers() {
        return threadsAllowedToBlockForConnectionMultipliers;
    }

    /**
     * @return the retryNumber
     */
    public static Integer getRetryNumber() {
        return retryNumber;
    }

    /**
     * @return the retryDelay
     */
    public static Integer getRetryDelay() {
        return retryDelay;
    }

    /**
     * @return the waiting Delay (wait)
     */
    public static Integer getWaitingDelay() {
        return waitingDelay;
    }


    /**
     * @return the size of the queue of async workspace
     */
    public static Integer getAsyncWorkspaceQueueSize() {
        return asyncWorkspaceQueueSize;
    }


    /**
     * @return the receive Buffer Size
     */
    public static Integer getRecvBufferSize() {
        return recvBufferSize;
    }

    /**
     * @return the use New Jaxr Client each time a getClient() from Factory is used
     */
    public static Boolean isUseNewJaxrClient() {
        return useNewJaxrClient;
    }


    /**
     * @return true if is integration Test
     */
    public static boolean isIntegrationTest() {
        return SystemPropertyUtil.get(VITAM_JUNIT_PROPERTY, false);
    }

    /**
     * setIntegrationTest
     *
     * @param value
     */
    public static void setIntegrationTest(boolean value) {
        SystemPropertyUtil.set(VITAM_JUNIT_PROPERTY, value);
    }


    /**
     * getter for VITAM_CONFIG_PROPERTY
     *
     * @return vitam config property
     */
    public static String getVitamConfigProperty() {
        return VITAM_CONFIG_PROPERTY;
    }



    /**
     * getter for VITAM_DATA_PROPERTY
     *
     * @return VITAM_DATA_PROPERTY
     */
    public static String getVitamDataProperty() {
        return VITAM_DATA_PROPERTY;
    }


    /**
     * getter for VITAM_LOG_PROPERTY
     *
     * @return VITAM_LOG_PROPERTY
     */
    public static String getVitamLogProperty() {
        return VITAM_LOG_PROPERTY;
    }

    /**
     * getter for VITAM_TMP_PROPERTY
     *
     * @return VITAM_TMP_PROPERTY
     */
    public static String getVitamTmpProperty() {
        return VITAM_TMP_PROPERTY;
    }


    /**
     * getter for vitamConfigFolderDefault
     *
     * @return vitamConfigFolderDefault
     */
    public static String getVitamConfigFolderDefault() {
        return vitamConfigFolderDefault;
    }

    /**
     * setter for vitamConfigFolderDefault
     *
     * @param vitamConfigFolderDefault
     */
    private static void setVitamConfigFolderDefault(String vitamConfigFolderDefault) {
        VitamConfiguration.vitamConfigFolderDefault = vitamConfigFolderDefault;
    }

    /**
     * getter for vitamDataFolderDefault
     *
     * @return vitamDataFolderDefault
     */
    public static String getVitamDataFolderDefault() {
        return vitamDataFolderDefault;
    }

    /**
     * setter for vitamDataFolderDefault
     *
     * @param vitamDataFolderDefault
     */
    private static void setVitamDataFolderDefault(String vitamDataFolderDefault) {
        VitamConfiguration.vitamDataFolderDefault = vitamDataFolderDefault;
    }

    /**
     * getter for vitamLogFolderDefault
     *
     * @return vitamLogFolderDefault
     */
    public static String getVitamLogFolderDefault() {
        return vitamLogFolderDefault;
    }

    /**
     * setter for vitamLogFolderDefault
     *
     * @param vitamLogFolderDefault
     */
    private static void setVitamLogFolderDefault(String vitamLogFolderDefault) {
        VitamConfiguration.vitamLogFolderDefault = vitamLogFolderDefault;
    }

    /**
     * getter for vitamTmpFolderDefault
     *
     * @return vitamCleanPeriod
     */
    public static Integer getVitamCleanPeriod() {
        return vitamCleanPeriod;
    }


    /**
     * setter for vitamLogFolderDefault
     *
     * @param vitamCleanPeriod
     */
    private static void setVitamCleanPeriod(Integer vitamCleanPeriod) {
        VitamConfiguration.vitamCleanPeriod = vitamCleanPeriod;
    }

    /**
     * getter for vitamTmpFolderDefault
     *
     * @return vitamTmpFolderDefault
     */
    public static String getVitamTmpFolderDefault() {
        return vitamTmpFolderDefault;
    }

    /**
     * setter for vitamTmpFolderDefault
     *
     * @param vitamTmpFolderDefault
     */
    private static void setVitamTmpFolderDefault(String vitamTmpFolderDefault) {
        VitamConfiguration.vitamTmpFolderDefault = vitamTmpFolderDefault;
    }

    /**
     * setter for chunkSize
     *
     * @param chunkSize
     */
    private static void setChunkSize(int chunkSize) {
        VitamConfiguration.chunkSize = chunkSize;
    }

    /**
     * set the size of the queue of async workspace
     *
     * @param queueSize
     */
    public static void setAsyncWorkspaceQueueSize(int queueSize) {
        asyncWorkspaceQueueSize = queueSize;
    }


    /**
     * setter for recvBufferSize
     *
     * @param recvBufferSize
     */
    private static void setRecvBufferSize(int recvBufferSize) {
        VitamConfiguration.recvBufferSize = recvBufferSize;
    }

    /**
     * setter for readTimeout
     *
     * @param readTimeout
     */
    private static void setReadTimeout(int readTimeout) {
        VitamConfiguration.readTimeout = readTimeout;
    }

    /**
     * setter for maxTotalClient
     *
     * @param maxTotalClient
     */
    private static void setMaxTotalClient(int maxTotalClient) {
        VitamConfiguration.maxTotalClient = maxTotalClient;
    }

    /**
     * setter for maxClientPerHost
     *
     * @param maxClientPerHost
     */
    private static void setMaxClientPerHost(int maxClientPerHost) {
        VitamConfiguration.maxClientPerHost = maxClientPerHost;
    }

    /**
     * setter for delayValidationAfterInactivity
     *
     * @param delayValidationAfterInactivity
     */
    private static void setDelayValidationAfterInactivity(int delayValidationAfterInactivity) {
        VitamConfiguration.delayValidationAfterInactivity = delayValidationAfterInactivity;
    }

    /**
     * getter for delayMultipleInputstream
     *
     * @return delayMultipleInputstream
     */
    public static Integer getDelayMultipleInputstream() {
        return delayMultipleInputstream;
    }

    /**
     * setter for delayMultipleSubinputstream
     *
     * @param delayMultipleInputstream
     */
    public static void setDelayMultipleInputstream(int delayMultipleInputstream) {
        VitamConfiguration.delayMultipleInputstream = delayMultipleInputstream;
    }

    /**
     * getter for delayMultipleSubinputstream
     *
     * @return delayMultipleSubinputstream
     */
    public static Integer getDelayMultipleSubinputstream() {
        return delayMultipleSubinputstream;
    }

    /**
     * setter for delayMultipleSubinputstream
     *
     * @param delayMultipleSubinputstream
     */
    private static void setDelayMultipleSubinputstream(int delayMultipleSubinputstream) {
        VitamConfiguration.delayMultipleSubinputstream = delayMultipleSubinputstream;
    }

    /**
     * getter for minimumThreadPoolSize
     *
     * @return minimumThreadPoolSize
     */
    public static Integer getMinimumThreadPoolSize() {
        return minimumThreadPoolSize;
    }

    /**
     * setter for minimumThreadPoolSize
     *
     * @param minimumThreadPoolSize
     */
    private static void setMinimumThreadPoolSize(int minimumThreadPoolSize) {
        VitamConfiguration.minimumThreadPoolSize = minimumThreadPoolSize;
    }

    /**
     * getter for noValidationAfterInactivity
     *
     * @return noValidationAfterInactivity
     */
    public static Integer getNoValidationAfterInactivity() {
        return noValidationAfterInactivity;
    }

    /**
     * setter for noValidationAfterInactivity
     *
     * @param noValidationAfterInactivity
     */
    private static void setNoValidationAfterInactivity(int noValidationAfterInactivity) {
        VitamConfiguration.noValidationAfterInactivity = noValidationAfterInactivity;
    }

    /**
     * setter for delayGetClient
     *
     * @param delayGetClient
     */
    private static void setDelayGetClient(int delayGetClient) {
        VitamConfiguration.delayGetClient = delayGetClient;
    }

    /**
     * setter for intervalDelayCheckIdle
     *
     * @param intervalDelayCheckIdle
     */
    private static void setIntervalDelayCheckIdle(int intervalDelayCheckIdle) {
        VitamConfiguration.intervalDelayCheckIdle = intervalDelayCheckIdle;
    }

    /**
     * setter for maxDelayUnusedConnection
     *
     * @param maxDelayUnusedConnection
     */
    private static void setMaxDelayUnusedConnection(int maxDelayUnusedConnection) {
        VitamConfiguration.maxDelayUnusedConnection = maxDelayUnusedConnection;
    }

    /**
     * setter for useNewJaxrClient
     *
     * @param useNewJaxrClient
     */
    private static void setUseNewJaxrClient(Boolean useNewJaxrClient) {
        VitamConfiguration.useNewJaxrClient = useNewJaxrClient;
    }



    /**
     * setter for securityDigestType
     *
     * @param securityDigestType
     */
    private static void setSecurityDigestType(DigestType securityDigestType) {
        VitamConfiguration.securityDigestType = securityDigestType;
    }

    /**
     * setter for defaultDigestType
     *
     * @param defaultDigestType
     */
    private static void setDefaultDigestType(DigestType defaultDigestType) {
        VitamConfiguration.defaultDigestType = defaultDigestType;
    }

    /**
     * setter for defaultTimestampDigestType
     *
     * @param defaultTimestampDigestType
     */
    private static void setDefaultTimestampDigestType(DigestType defaultTimestampDigestType) {
        VitamConfiguration.defaultTimestampDigestType = defaultTimestampDigestType;
    }

    /**
     * setter for acceptableRequestTime
     *
     * @param acceptableRequestTime
     */
    private static void setAcceptableRequestTime(Long acceptableRequestTime) {
        VitamConfiguration.acceptableRequestTime = acceptableRequestTime;
    }

    /**
     * setter for threadsAllowedToBlockForConnectionMultipliers
     *
     * @param threadsAllowedToBlockForConnectionMultipliers
     */
    private static void setThreadsAllowedToBlockForConnectionMultipliers(
        Integer threadsAllowedToBlockForConnectionMultipliers) {
        VitamConfiguration.threadsAllowedToBlockForConnectionMultipliers =
            threadsAllowedToBlockForConnectionMultipliers;
    }

    /**
     * setter for retryNumber
     *
     * @param retryNumber
     */
    private static void setRetryNumber(int retryNumber) {
        VitamConfiguration.retryNumber = retryNumber;
    }

    /**
     * setter for retryDelay
     *
     * @param retryDelay
     */
    private static void setRetryDelay(int retryDelay) {
        VitamConfiguration.retryDelay = retryDelay;
    }

    /**
     * setter for waitingDelay
     *
     * @param waitingDelay
     */
    private static void setWaitingDelay(int waitingDelay) {
        VitamConfiguration.waitingDelay = waitingDelay;
    }

    /**
     * getter for allowGzipEncoding
     *
     * @return allowGzipEncoding
     */
    public static Boolean isAllowGzipEncoding() {
        return allowGzipEncoding;
    }

    /**
     * setter for allowGzipEncoding
     *
     * @param allowGzipEncoding
     */
    private static void setAllowGzipEncoding(Boolean allowGzipEncoding) {
        VitamConfiguration.allowGzipEncoding = allowGzipEncoding;
    }


    /**
     * getter for forceChunkModeInputStream
     *
     * @return forceChunkModeInputStream
     */
    public static Boolean isForceChunkModeInputStream() {
        return forceChunkModeInputStream;
    }

    /**
     * setter for forceChunkModeInputStream
     *
     * @param forceChunkModeInputStream
     */
    private static void setForceChunkModeInputStream(Boolean forceChunkModeInputStream) {
        VitamConfiguration.forceChunkModeInputStream = forceChunkModeInputStream;
    }

    /**
     * getter for allowGzipDecoding
     *
     * @return allowGzipDecoding
     */
    public static Boolean isAllowGzipDecoding() {
        return allowGzipDecoding;
    }

    /**
     * setter for allowGzipDecoding
     *
     * @param allowGzipDecoding
     */
    private static void setAllowGzipDecoding(Boolean allowGzipDecoding) {
        VitamConfiguration.allowGzipDecoding = allowGzipDecoding;
    }

    /**
     * getter for bufferNumber
     *
     * @return bufferNumber
     */
    public static Integer getBufferNumber() {
        return bufferNumber;
    }

    /**
     * setter for bufferNumber
     *
     * @param bufferNumber
     */
    private static void setBufferNumber(int bufferNumber) {
        VitamConfiguration.bufferNumber = bufferNumber;
    }

    /**
     * getter for maxConcurrentMultipleInputstreamHandler
     *
     * @return maxConcurrentMultipleInputstreamHandler
     */
    public static Integer getMaxConcurrentMultipleInputstreamHandler() {
        return maxConcurrentMultipleInputstreamHandler;
    }

    /**
     * setter for maxConcurrentMultipleInputstreamHandler
     *
     * @param maxConcurrentMultipleInputstreamHandler
     */
    private static void setMaxConcurrentMultipleInputstreamHandler(int maxConcurrentMultipleInputstreamHandler) {
        VitamConfiguration.maxConcurrentMultipleInputstreamHandler = maxConcurrentMultipleInputstreamHandler;
    }

    /**
     * getter for DEFAULT_LANG
     *
     * @return DEFAULT_LANG
     */
    public static String getDefaultLang() {
        return DEFAULT_LANG;
    }

    /**
     * setter for DEFAULT_LANG
     *
     * @param defaultLang
     */
    public static void setDefaultLang(String defaultLang) {
        DEFAULT_LANG = defaultLang;
    }

    /**
     * Getter for distributeurBatchSize;
     *
     * @return distributeurBatchSize
     */
    public static int getDistributeurBatchSize() {
        return distributeurBatchSize;
    }

    /**
     * Setter for distributeurBatchSize;
     *
     * @param distributeurBatchSize
     */
    public static void setDistributeurBatchSize(int distributeurBatchSize) {
        VitamConfiguration.distributeurBatchSize = distributeurBatchSize;
    }

    /**
     * Getter for worker bulk size
     *
     * @return getWorkerBulkSize
     */
    public static int getWorkerBulkSize() {
        return workerBulkSize;
    }

    /**
     * Setter worker bulk size
     *
     * @param workerBulkSize
     */
    public static void setWorkerBulkSize(int workerBulkSize) {
        VitamConfiguration.workerBulkSize = workerBulkSize;
    }

    /**
     * Getter restore bulk size
     *
     * @return restoreBulkSize
     */
    public static int getRestoreBulkSize() {
        return restoreBulkSize;
    }

    /**
     * Setter restore bulk size
     *
     * @param restoreBulkSize
     */
    public static void setRestoreBulkSize(int restoreBulkSize) {
        VitamConfiguration.restoreBulkSize = restoreBulkSize;
    }

    /**
     * Getter for cacheControlDelay;
     *
     * @return cacheControlDelay
     */
    public static int getCacheControlDelay() {
        return cacheControlDelay;
    }

    /**
     * Setter for cacheControlDelay;
     *
     * @param cacheControlDelay
     */
    public static void setCacheControlDelay(int cacheControlDelay) {
        VitamConfiguration.cacheControlDelay = cacheControlDelay;
    }

    /**
     * Getter for maxCacheEntries;
     *
     * @return maxCacheEntries
     */
    public static int getMaxCacheEntries() {
        return maxCacheEntries;
    }

    /**
     * Setter for maxCacheEntries;
     *
     * @param maxCacheEntries
     */
    public static void setMaxCacheEntries(int maxCacheEntries) {
        VitamConfiguration.maxCacheEntries = maxCacheEntries;
    }



    /**
     * Setter for expireCacheEntriesDelay
     *
     * @return expireCacheEntriesDelay
     */
    public static int getExpireCacheEntriesDelay() {
        return expireCacheEntriesDelay;
    }

    /**
     * Getter for expireCacheEntriesDelay
     *
     * @param expireCacheEntriesDelay
     */
    public static void setExpireCacheEntriesDelay(int expireCacheEntriesDelay) {
        VitamConfiguration.expireCacheEntriesDelay = expireCacheEntriesDelay;
    }

    /**
     * Getter for exportScore;
     *
     * @return exportScore
     */
    public static boolean isExportScore() {
        return exportScore;
    }

    /**
     * Setter for exportScore;
     *
     * @param exportScore
     */
    private static void setExportScore(boolean exportScore) {
        VitamConfiguration.exportScore = exportScore;
    }



    /**
     * Getter for maxElasticsearchBulk;
     *
     * @return maxElasticsearchBulk
     */
    public static int getMaxElasticsearchBulk() {
        return maxElasticsearchBulk;
    }

    /**
     * Setter for maxElasticsearchBulk;
     *
     * @param maxElasticsearchBulk
     */
    private static void setMaxElasticsearchBulk(int maxElasticsearchBulk) {
        VitamConfiguration.maxElasticsearchBulk = maxElasticsearchBulk;
    }

    /**
     * Getter for numberDbClientThread;
     *
     * @return numberDbClientThread
     */
    public static int getNumberDbClientThread() {
        return numberDbClientThread;
    }

    /**
     * Setter for numberDbClientThread;
     *
     * @param numberDbClientThread
     */
    private static void setNumberDbClientThread(int numberDbClientThread) {
        VitamConfiguration.numberDbClientThread = numberDbClientThread;
    }

    /**
     * Getter for numberEsQueue;
     *
     * @return numberEsQueue
     */
    public static Integer getNumberEsQueue() {
        return numberEsQueue;
    }

    /**
     * Setter for numberEsQueue;
     *
     * @param numberEsQueue
     */
    private static void setNumberEsQueue(int numberEsQueue) {
        VitamConfiguration.numberEsQueue = numberEsQueue;
    }

    /**
     * Getter for distributionThreshold;
     *
     * @return distributionThreshold
     */
    public static long getDistributionThreshold() {
        return distributionThreshold;
    }

    /**
     * Setter for distributionThreshold;
     *
     * @param distributionThreshold
     */
    public static void setDistributionThreshold(long distributionThreshold) {
        VitamConfiguration.distributionThreshold = distributionThreshold;
    }

    public static long getEliminationAnalysisThreshold() {
        return eliminationAnalysisThreshold;
    }

    public static void setEliminationAnalysisThreshold(long eliminationAnalysisThreshold) {
        VitamConfiguration.eliminationAnalysisThreshold = eliminationAnalysisThreshold;
    }

    public static long getEliminationActionThreshold() {
        return eliminationActionThreshold;
    }

    public static void setEliminationActionThreshold(long eliminationActionThreshold) {
        VitamConfiguration.eliminationActionThreshold = eliminationActionThreshold;
    }

    /**
     * Getter for default OriginatingAgency for DIP export OriginatingAgency conflict
     *
     * @return default originatingAgency for export
     */
    public static String getDefaultOriginatingAgencyForExport(Integer tenant) {
        if (defaultOriginatingAgencyByTenant.containsKey(tenant)) {
            return defaultOriginatingAgencyByTenant.get(tenant);
        }
        return DEFAULT_ORIGINATING_AGENCY_FOR_EXPORT;
    }

    /**
     * Setter for default OriginatingAgency for DIP export OriginatingAgency conflict
     *
     * @param defaultOriginatingAgencyForExport originatingAgency for export
     */
    public static void setDefaultOriginatingAgencyByTenant(Map<Integer, String> defaultOriginatingAgencyForExport) {
        VitamConfiguration.defaultOriginatingAgencyByTenant = defaultOriginatingAgencyForExport;
    }

    /**
     * Get the maxResultWindow
     *
     * @return maxResultWindow
     */
    public static int getMaxResultWindow() {
        return maxResultWindow;
    }

    /**
     * Set the maxResultWindow
     *
     * @param maxResultWindow
     */
    public static void setMaxResultWindow(int maxResultWindow) {
        VitamConfiguration.maxResultWindow = maxResultWindow;
    }

    /**
     * Get the batchSize.
     *
     * @return batchSize
     */
    public static int getBatchSize() {
        return batchSize;
    }


    /**
     * Get the store graph elements per file
     *
     * @return storeGraphElementsPerFile
     */
    public static int getStoreGraphElementsPerFile() {
        return storeGraphElementsPerFile;
    }


    /**
     * Set store graph elements per file
     *
     * @param storeGraphElementsPerFile
     */
    public static void setStoreGraphElementsPerFile(Integer storeGraphElementsPerFile) {
        VitamConfiguration.storeGraphElementsPerFile = storeGraphElementsPerFile;
    }


    /**
     * Get store graph overlap delay
     *
     * @return storeGraphOverlapDelay
     */
    public static Integer getStoreGraphOverlapDelay() {
        return storeGraphOverlapDelay;
    }

    /**
     * Set store graph overlap delay
     *
     * @param storeGraphOverlapDelay
     */
    public static void setStoreGraphOverlapDelay(Integer storeGraphOverlapDelay) {
        VitamConfiguration.storeGraphOverlapDelay = storeGraphOverlapDelay;
    }

    /**
     * Get the delay of deleting incomplete reconstructed units
     *
     * @return deleteIncompleteReconstructedUnitDelay
     */
    public static int getDeleteIncompleteReconstructedUnitDelay() {
        return deleteIncompleteReconstructedUnitDelay;
    }

    /**
     * Set the delay of deleting incomplete reconstructed units
     *
     * @param deleteIncompleteReconstructedUnitDelay
     */
    public static void setDeleteIncompleteReconstructedUnitDelay(int deleteIncompleteReconstructedUnitDelay) {
        VitamConfiguration.deleteIncompleteReconstructedUnitDelay = deleteIncompleteReconstructedUnitDelay;
    }

    /**
     * Get optimistic lock retry number
     *
     * @return optimisticLockRetryNumber
     */
    public static int getOptimisticLockRetryNumber() {
        return optimisticLockRetryNumber;
    }

    /**
     * Set optimistic lock retry number
     *
     * @param optimisticLockRetryNumber
     */
    public static void setOptimisticLockRetryNumber(int optimisticLockRetryNumber) {
        VitamConfiguration.optimisticLockRetryNumber = optimisticLockRetryNumber;
    }

    /**
     * Get optimistic lock sleep time
     *
     * @return optimisticLockSleepTime
     */
    public static int getOptimisticLockSleepTime() {
        return optimisticLockSleepTime;
    }

    /**
     * Set optimistic lock sleep time
     *
     * @param optimisticLockSleepTime
     */
    public static void setOptimisticLockSleepTime(int optimisticLockSleepTime) {
        VitamConfiguration.optimisticLockSleepTime = optimisticLockSleepTime;
    }


    /**
     * Getter
     *
     * @return keywordMaxLength
     */
    public static int getKeywordMaxLength() {
        return keywordMaxLength;
    }

    /**
     * Setter
     *
     * @param keywordMaxLength
     */
    public static void setKeywordMaxLength(int keywordMaxLength) {
        VitamConfiguration.keywordMaxLength = keywordMaxLength;
    }

    /**
     * Getter
     *
     * @return textMaxLength
     */
    public static int getTextMaxLength() {
        return textMaxLength;
    }

    /**
     * Setter
     *
     * @param textMaxLength
     */
    public static void setTextMaxLength(int textMaxLength) {
        VitamConfiguration.textMaxLength = textMaxLength;
    }

    /**
     * Get defaultOffset
     *
     * @return defaultOffset
     */
    public static int getDefaultOffset() {
        return defaultOffset;
    }

    /**
     * Set the batchSize.
     *
     * @param batchSize
     */
    public static void setBatchSize(int batchSize) {
        VitamConfiguration.batchSize = batchSize;
    }

    public static long getSwiftFileLimit() {
        return SWIFT_FILE_LIMIT;
    }

    /**
     * Get migrationBulkSize
     *
     * @return migrationBulkSize
     */
    public static int getMigrationBulkSize() {
        return migrationBulkSize;
    }

    /**
     * Set the migrationBulkSize
     *
     * @param migrationBulkSize
     */
    public static void setMigrationBulkSize(int migrationBulkSize) {
        VitamConfiguration.migrationBulkSize = migrationBulkSize;
    }

    public static String getWorkspaceWorkflowsFolder() {
        return workspaceWorkflowsFolder;
    }

    public static void setWorkspaceWorkflowsFolder(String workspaceWorkflowsFolder) {
        VitamConfiguration.workspaceWorkflowsFolder = workspaceWorkflowsFolder;
    }

    /**
     * Get environmentName
     *
     * @return environmentName value
     */
    public static String getEnvironmentName() {
        return environmentName;
    }

    public static /**
     * set the environmentName
     */
    void setEnvironmentName(String environmentName) {
        VitamConfiguration.environmentName = environmentName;
    }

    /**
     * Max dsl queries per reclassification request
     */
    public static int getReclassificationMaxBulkThreshold() {
        return reclassificationMaxBulkThreshold;
    }

    /**
     * Max dsl queries per reclassification request
     */
    public static void setReclassificationMaxBulkThreshold(int reclassificationMaxBulkThreshold) {
        VitamConfiguration.reclassificationMaxBulkThreshold = reclassificationMaxBulkThreshold;
    }

    /**
     * Max units to update per reclassification request
     */
    public static int getReclassificationMaxUnitsThreshold() {
        return reclassificationMaxUnitsThreshold;
    }

    /**
     * Max units to update per reclassification request
     */
    public static void setReclassificationMaxUnitsThreshold(int reclassificationMaxUnitsThreshold) {
        VitamConfiguration.reclassificationMaxUnitsThreshold = reclassificationMaxUnitsThreshold;
    }

    /**
     * Max guid to store in logbook operation in evDetData
     */
    public static int getReclassificationMaxGuildListSizeInLogbookOperation() {
        return reclassificationMaxGuildListSizeInLogbookOperation;
    }

    /**
     * Max guid to store in logbook operation in evDetData
     */
    public static void setReclassificationMaxGuildListSizeInLogbookOperation(
        int reclassificationMaxGuildListSizeInLogbookOperation) {
        VitamConfiguration.reclassificationMaxGuildListSizeInLogbookOperation =
            reclassificationMaxGuildListSizeInLogbookOperation;
    }

    public static ClassificationLevel getClassificationLevel() {
        return classificationLevel;
    }

    public static void setClassificationLevel(ClassificationLevel classificationLevel) {
        VitamConfiguration.classificationLevel = classificationLevel;
    }

    public static String getVitamGriffinExecFolder() {
        return vitamGriffinExecFolder;
    }

    public static void setVitamGriffinExecFolder(String vitamGriffinExecFolder) {
        VitamConfiguration.vitamGriffinExecFolder = vitamGriffinExecFolder;
    }

    public static String getVitamGriffinInputFilesFolder() {
        return vitamGriffinInputFilesFolder;
    }

    public static void setVitamGriffinInputFilesFolder(String vitamGriffinInputFilesFolder) {
        VitamConfiguration.vitamGriffinInputFilesFolder = vitamGriffinInputFilesFolder;
    }

    public static Integer getElasticSearchScrollTimeoutInMilliseconds() {
        return elasticSearchScrollTimeoutInMilliseconds;
    }

    public static void setElasticSearchScrollTimeoutInMilliseconds(Integer elasticSearchScrollTimeoutInMilliseconds) {
        VitamConfiguration.elasticSearchScrollTimeoutInMilliseconds = elasticSearchScrollTimeoutInMilliseconds;
    }

    public static Integer getElasticSearchScrollLimit() {
        return elasticSearchScrollLimit;
    }

    public static void setElasticSearchScrollLimit(Integer elasticSearchScrollLimit) {
        VitamConfiguration.elasticSearchScrollLimit = elasticSearchScrollLimit;
    }
}
