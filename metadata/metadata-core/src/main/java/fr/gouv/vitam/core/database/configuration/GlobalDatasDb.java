/**
 * 
 */
package fr.gouv.vitam.core.database.configuration;

import java.util.HashSet;
import java.util.Set;

import fr.gouv.vitam.parser.request.parser.GlobalDatasParser;
import fr.gouv.vitam.common.UUIDMultiple;

/**
 * Default configuration for DB support
 * 
 *
 */
public class GlobalDatasDb extends GlobalDatasParser {
    /**
     * Default UUIDMultiple (For Request)
     */
    public static final UUIDMultiple UUID_MULTIPLE = new UUIDMultiple(UUID_FACTORY);
    /**
     * set of Roots Domain : must be updated each time a new Domain is created
     */
    public static final Set<String> ROOTS = new HashSet<>();
    /**
     * Should we use filter to select from graph parents, or within request
     * (filtering seems better)
     */
    public static boolean useFilter = true;
    /**
     * Shall we use FilteredRequest or postFilter for queries
     */
    public static boolean useFilteredRequest = true;
    /**
     * Default behavior of ElasticSearch connection (False tends to minimize the
     * number of concurrent connections)
     */
    public static boolean useNewNode = false;
    /**
     * Default Index name for ElasticSearch
     */
    public static final String INDEXNAME = "vitamidx";
    /**
     * Default local address to be used by the client (null means no specific
     * address)
     */
    public static String localNetworkAddress = null;
    /**
     * limit before using ES in 1 level only (set to -1 means use ES at all
     * time)
     */
    public static long limitES = 10001;
    /**
     * limit before flushing ES with Bulk
     */
    public static final int LIMIT_ES_NEW_INDEX = 10000;
    /**
     * limit before flushing MongoDB with Bulk
     */
    public static final int LIMIT_MDB_NEW_INDEX = 10000;
    /**
     * In Debug mode : shall we print the request
     */
    public static final boolean PRINT_REQUEST = false;
    /**
     * Shall new entries insertion in the ElasticSearch index be in blocking
     * mode
     */
    public static final boolean BLOCKING = false;
    /**
     * Shall we save ResultCache
     */
    public static final boolean SAVERESULT = true;
    /**
     * Shall we use SynchronizedLruCache for cache of results
     */
    public static final boolean USELRUCACHE = false;
    /**
     * Default TTL in ms : 1H
     */
    public static final int TTLMS = 3600 * 1000;
    /**
     * Default TTL in s : 1H
     */
    public static final int TTL = TTLMS / 1000;
    /**
     * Default LRU Size
     */
    public static final int MAXLRU = 1000000;
    /**
     * Shall we use Redis for cache of results
     */
    public static final boolean USEREDIS = false;

    protected GlobalDatasDb() {
        // empty
    }

}
