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
package fr.gouv.vitam.storage.cold.client;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.security.codec.URLCodec;
import fr.gouv.vitam.storage.cold.client.invoker.ApiClient;
import fr.gouv.vitam.storage.cold.client.invoker.ApiException;
import fr.gouv.vitam.storage.cold.client.invoker.Pair;
import jakarta.ws.rs.core.GenericType;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ConfiguredApiClient extends ApiClient {

    @Override
    public <T> T invokeAPI(
        String path,
        String method,
        List<Pair> queryParams,
        Object body,
        Map<String, String> headerParams,
        Map<String, String> cookieParams,
        Map<String, Object> formParams,
        String accept,
        String contentType,
        String[] authNames,
        GenericType<T> returnType
    ) throws ApiException {
        // Add timestamp header (in seconds since epoch)
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        headerParams.put(GlobalDataRest.X_TIMESTAMP, timestamp);

        // Generate request signature (X-Platform-Id)
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        String signature = URLCodec.encodeURL(
            method,
            decodedPath,
            timestamp,
            VitamConfiguration.getSecret(),
            VitamConfiguration.getSecurityDigestType()
        );
        headerParams.put(GlobalDataRest.X_PLATFORM_ID, signature);

        // Delegate the actual API call to the parent class
        return super.invokeAPI(
            path,
            method,
            queryParams,
            body,
            headerParams,
            cookieParams,
            formParams,
            accept,
            contentType,
            authNames,
            returnType
        );
    }
}
