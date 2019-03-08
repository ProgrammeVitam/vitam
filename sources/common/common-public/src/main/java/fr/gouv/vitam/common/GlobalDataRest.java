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

/**
 * Global Variables and eventually method used by REST services
 */
public class GlobalDataRest {
    /**
     * X_HTTP_METHOD_OVERRIDE : used in case of POST methods overriding GET methods
     */
    public static final String X_HTTP_METHOD_OVERRIDE = "X-Http-Method-Override";

    /**
     * Header Parameter X_SECURITY_CONTEXT_ID
     */
    public static final String X_SECURITY_CONTEXT_ID = "X-Security-Context-ID";

    /**
     * Header Parameter X_APPLICATION_ID
     */
    public static final String X_APPLICATION_ID = "X-Application-Id";

    /**
     * Header Parameter X_PERSONAL_CERTIFICATE
     */
    public static final String X_PERSONAL_CERTIFICATE = "X-Personal-Certificate";

    /**
     * Header Parameter X_ACTION
     * Is required only if X_ACTION_INIT in (START, NEXT, RESUME, REPLAY, PAUSE) but not INT
     */
    public static final String X_ACTION = "X-ACTION";

    /**
     * Header Parameter X_ACTION_INIT
     */
    public static final String X_ACTION_INIT = "X_ACTION_INIT";

    /**
     * Logbook type process
     */
    public static final String X_TYPE_PROCESS = "X_TYPE_PROCESS";

    /**
     * Header Parameter X_REQUEST_ID
     */
    public static final String X_REQUEST_ID = "X-Request-Id";

    /**
     * X-Command header used on storage resources
     */
    public static final String X_COMMAND = "X-Command";

    /**
     * X-Tenant-Id header used on REST request to identify the concerned tenant
     */
    public static final String X_TENANT_ID = "X-Tenant-Id";
    /**
     * X-Qualifier header used on REST request to identify the concerned qualifier
     */
    public static final String X_QUALIFIER = "X-Qualifier";
    /**
     * X-Version header used on REST request to identify the concerned version
     */
    public static final String X_VERSION = "X-Version";

    /**
     * The X_STRATEGY_ID header, used in requests to use a particular strategy
     */
    public static final String X_STRATEGY_ID = "X-Strategy-Id";

    /**
     * The X_OFFER_IDS header, used in requests to give list of offer ids
     */
    public static final String X_OFFER_IDS = "X-Offer-Ids";
    /**
     * The X_OFFER_NO_CACHE header, used in requests to set cache policy when computing object digest
     */
    public static final String X_OFFER_NO_CACHE = "X-Offer-No-Cache";
    /**
     * The X-CONTENT-DESTINATIONnofferId destination for copy
     */
    public static final String X_CONTENT_DESTINATION = "X-CONTENT-DESTINATION";
    /**
     * The X-CONTENT-SOURCE offerId source for copy
     */
    public static final String X_CONTENT_SOURCE = "X-CONTENT-SOURCE";



    /**
     * The X_PLATFORM_ID header
     */
    public static final String X_PLATFORM_ID = "X-Platform-Id";

    /**
     * The X_TIMESTAMP header
     */
    public static final String X_TIMESTAMP = "X-Timestamp";

    /**
     * The X_CSRF_TOKEN header
     */
    public static final String X_CSRF_TOKEN = "X-CSRF-TOKEN";

    /**
     * Ask to request using a http based cursor
     */
    public static final String X_CURSOR = "X-Cursor";
    /**
     * Identifier for a Http based cursor
     */
    public static final String X_CURSOR_ID = "X-Cursor-Id";
    /**
     * Timeout (in epoch) for Http based cursor
     */
    public static final String X_CURSOR_TIMEOUT = "X-Cursor-Timeout";

    /**
     * Digest value
     */
    public static final String X_DIGEST = "X-digest";
    /**
     * Digest algorithm
     */
    public static final String X_DIGEST_ALGORITHM = "X-digest-algorithm";
    /**
     * Recursive deletion
     */
    public static final String X_RECURSIVE = "X-Recursive";

    /**
     * Header use to have the body (object) size even if Content-Type is chunkec
     */
    public static final String X_CONTENT_LENGTH = "X-Content-Length";
    /**
     * Header use to  have size before swift upload
     */
    public static final String VITAM_CONTENT_LENGTH = "Vitam-Content-Length";

    /**
     * Header to passe certificate pem
     */
    public static final String X_SSL_CLIENT_CERT = "X-SSL-CLIENT-CERT";

    /**
     * X_CONTEXT_ID : used in case of POST and PUT methods to transmit workFlow execution context(stepByStep or not)
     */
    public static final String X_CONTEXT_ID = "X-Context-Id";

    /**
     * Header use to contain the id of the access contract
     */
    public static final String X_ACCESS_CONTRAT_ID = "X-Access-Contract-Id";

    /**
     * Filename Header
     */
    public static final String X_FILENAME = "X-Filename";

    /**
     * Transfer Encoding Header
     */
    public static final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding";

    /**
     * FIRST_PRIORITY_FILTER
     */
    public static final int FIRST_PRIORITY_FILTER = 1000;

    /**
     * SECOND_PRIORITY_FILTER
     */
    public static final int SECOND_PRIORITY_FILTER = 2000;

    /**
     * Event status
     */
    public static final String X_EVENT_STATUS = "X-Event-Status";


    /**
     * Tenant List for Initialisation of Tenant Filter
     */
    public static final String TENANT_LIST = "tenantList";

    /**
     * Timeout for finish worker task in millisecond
     */
    public static final long TIMEOUT_END_WORKER_MILLISECOND = 600;

    /**
     * number of check status retry call
     */
    public static final int STATUS_CHECK_RETRY = 3;

    /**
     * Global execution state
     */
    public static final String X_GLOBAL_EXECUTION_STATE = "X-Global-Execution-State";
    /**
     * Global execution status
     */
    public static final String X_GLOBAL_EXECUTION_STATUS = "X-Global-Execution-Status";


    /**
     * Force update Header
     */
    public static final String FORCE_UPDATE = "Force-Update";


    /**
     * The X_DATA_CATEGORY header, used in requests to give data category
     */
    public static final String X_DATA_CATEGORY = "X-Data-Category";

    private GlobalDataRest() {
        // empty
    }

}
