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
package fr.gouv.vitam.storage.engine.client;

import java.time.LocalDateTime;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * Mock client implementation for storage
 */
class StorageClientMock extends AbstractMockClient implements StorageClient {
    static final String MOCK_POST_RESULT = "{\"_id\": \"{id}\",\"status\": \"OK\"}";
    static final String MOCK_INFOS_RESULT = "{\"offerId\": \"offer1\",\"usableSpace\": 838860800}";
    static final String MOCK_INFOS_RESULT_ARRAY = "{\"capacities\": [{\"offerId\": \"offer1\",\"usableSpace\": " +
        "838860800}," + "{\"offerId\": " + "\"offer2\",\"usableSpace\": 838860800}]}";
    static final String MOCK_INFOS_EMPTY_RESULT_ARRAY = "{\"capacities\": []}";
    static final String MOCK_GET_FILE_CONTENT =
        "Vitam test of long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long file";

    @Override
    public JsonNode getStorageInformation(String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {
        Integer tenantId = 0;
        try {
            tenantId = ParameterHelper.getTenantParameter();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        try {
            if (tenantId == -1) {
                return null;
            } else if (tenantId == -2) {
                return JsonHandler.getFromString(MOCK_INFOS_EMPTY_RESULT_ARRAY);
            } else {
                return JsonHandler.getFromString(MOCK_INFOS_RESULT_ARRAY);
            }
        } catch (final InvalidParseOperationException e) {
            throw new StorageServerClientException(e);
        }
    }

    @Override
    public StoredInfoResult storeFileFromWorkspace(String strategyId, StorageCollectionType type, String guid,
        ObjectDescription description)
        throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException {
        return generateStoredInfoResult(guid);
    }

    @Override
    public boolean deleteContainer(String strategyId) throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean delete(String strategyId, StorageCollectionType type, String guid, String digest,
        DigestType digestAlgorithm)
        throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean existsContainer(String strategyId) throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean exists(String strategyId, StorageCollectionType type, String guid)
        throws StorageServerClientException {
        return true;
    }

    private StoredInfoResult generateStoredInfoResult(String guid) {
        final StoredInfoResult result = new StoredInfoResult();
        result.setId(guid);
        result.setInfo("Stockage de l'objet réalisé avec succès");
        result.setCreationTime(LocalDateUtil.getString(LocalDateTime.now()));
        result.setLastModifiedTime(LocalDateUtil.getString(LocalDateTime.now()));
        return result;
    }

    @Override
    public Response getContainerAsync(String strategyId, String guid, StorageCollectionType type)
        throws StorageServerClientException, StorageNotFoundException {
        return new FakeInboundResponse(Status.OK, IOUtils.toInputStream(MOCK_GET_FILE_CONTENT),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public VitamRequestIterator<JsonNode> listContainer(String strategyId, DataCategory type)
        throws StorageServerClientException {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_CURSOR, true);
        return new VitamRequestIterator<>(this, HttpMethod.GET, type.getFolder(), JsonNode.class, headers, null);
    }

    @Override
    public RequestResponseOK secureStorageLogbook()
        throws StorageServerClientException, InvalidParseOperationException {
        return new RequestResponseOK<String>()
            .setHits(1, 0, 1)
            .addResult(GUIDFactory.newGUID());
    }

}
