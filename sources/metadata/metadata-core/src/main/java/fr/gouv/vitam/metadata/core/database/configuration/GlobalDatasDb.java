/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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
package fr.gouv.vitam.metadata.core.database.configuration;

import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;

/**
 * Default configuration for DB support
 */
public class GlobalDatasDb extends GlobalDatasParser {

    /**
     * Should we use filter to select from graph parents, or within request (filtering seems better)
     */
    public static final boolean USE_FILTER = true;
    /**
     * Shall we use FilteredRequest or postFilter for queries
     */
    public static final boolean USE_FILTERED_REQUEST = true;
    /**
     * Default behavior of ElasticSearch connection (False tends to minimize the number of concurrent connections)
     */
    public static final boolean USE_NEW_NODE = false;
    /**
     * Default Index name for ElasticSearch
     */
    public static final String INDEXNAME = "vitamidx";
    /**
     * Default local address to be used by the client (null means no specific address)
     */
    public static String localNetworkAddress = null; // NOSONAR Change can be done
    /**
     * limit before using ES in 1 level only (set to -1 means use ES at all time)
     */
    public static long limitES = 10001; // NOSONAR Change can be done
    /**
     * limit before flushing ES with Bulk
     */
    public static final int LIMIT_ES_NEW_INDEX = 10000;
    /**
     * limit before flushing MongoDB with Bulk
     */
    public static final int LIMIT_MDB_NEW_INDEX = 10000;
    /**
     * Shall new entries insertion in the ElasticSearch index be in blocking mode
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

    /**
     * KEYWORD to activate scroll
     */
    public static final String SCROLL_ACTIVATE_KEYWORD = "START";

    /**
     * default limit scroll size
     */
    public static final int DEFAULT_LIMIT_SCROLL = 100;
}
