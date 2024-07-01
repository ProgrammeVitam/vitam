/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.client;

import java.util.Map;

/**
 * Various key for RestEsy configuration
 */
public enum VitamRestEasyConfiguration {
    /**
     * Apache Connection Manager
     */
    CONNECTION_MANAGER,
    /**
     * Chunked encoding size
     */
    CHUNKED_ENCODING_SIZE,
    /**
     * Recv buffer decoding size
     */
    RECV_BUFFER_SIZE,
    /**
     * Request EntityProcessing
     */
    REQUEST_ENTITY_PROCESSING,
    /**
     * Connect timeout
     */
    CONNECT_TIMEOUT,
    /**
     * Read Timeout
     */
    READ_TIMEOUT,
    /**
     * connectionRequestTimeout
     */
    CONNECTIONREQUESTTIMEOUT,
    /**
     * connectTimeout
     */
    CONNECTTIMEOUT,
    /**
     * socketTimeout
     */
    SOCKETTIMEOUT,
    /**
     * is connection manager shared
     */
    CONNECTION_MANAGER_SHARED,
    /**
     * Disable automatic retries
     */
    DISABLE_AUTOMATIC_RETRIES,
    /**
     * Request configuration
     */
    REQUEST_CONFIG,
    /**
     * SslContext
     */
    SSL_CONTEXT,
    /**
     * Credential providers
     */
    CREDENTIALS_PROVIDER,
    /**
     * PROXY_URI
     */
    PROXY_URI,
    /**
     * PROXY_USERNAME
     */
    PROXY_USERNAME,
    /**
     * PROXY_PASSWORD
     */
    PROXY_PASSWORD,
    /**
     * contentCompressionEnabled
     */
    CONTENTCOMPRESSIONENABLED,
    /**
     * Cache enable
     */
    CACHE_ENABLED;

    /**
     * Chunk mode
     */
    public static final String CHUNKED = "CHUNKED";
    /**
     * Buffer mode
     */
    public static final String BUFFERED = "BUFFERED";

    /**
     * @param config
     * @return True of the associated Map contains True for this key
     */
    public final boolean isTrue(Map<VitamRestEasyConfiguration, Object> config) {
        final Boolean isTrue = (Boolean) config.get(this);
        if (isTrue != null) {
            return isTrue;
        }
        return false;
    }

    /**
     * @param config
     * @return The Object of the associated Map for this key
     */
    public final Object getObject(Map<VitamRestEasyConfiguration, Object> config) {
        return config.get(this);
    }

    /**
     * @param config
     * @param defaultValue
     * @return The String of the associated Map for this key or default value if not found
     */
    public final String getString(Map<VitamRestEasyConfiguration, Object> config, String defaultValue) {
        String value = (String) config.get(this);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * @param config
     * @param defaultValue
     * @return The int of the associated Map for this key or default value if not found
     */
    public final int getInt(Map<VitamRestEasyConfiguration, Object> config, int defaultValue) {
        if (config.containsKey(this)) {
            return (int) config.get(this);
        }
        return defaultValue;
    }
}
