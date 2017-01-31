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
     * Header Parameter X_APPLICATION_ID
     */
    public static final String X_APPLICATION_ID = "X-APPLICATION-ID";

    /**
     * Header Parameter X_REQUEST_ID
     */
    public static final String X_REQUEST_ID = "X-REQUEST-ID";

    /**
     * Header Parameter X_REQUEST_TRACE which contains at first X-Request-ID then adding the X-REQUESTER
     */
    public static final String X_REQUEST_TRACE = "X-REQUEST-TRACE";

    /**
     * Header Parameter X_REQUESTER which contains the service and host name
     */
    public static final String X_REQUESTER = "X-REQUESTER";

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
     * The X_PLATFORM_ID header
     */
    public static final String X_PLATFORM_ID = "X-Platform-Id";

    /**
     * The X_TIMESTAMP header
     */
    public static final String X_TIMESTAMP = "X-Timestamp";

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


    private GlobalDataRest() {
        // empty
    }

}
