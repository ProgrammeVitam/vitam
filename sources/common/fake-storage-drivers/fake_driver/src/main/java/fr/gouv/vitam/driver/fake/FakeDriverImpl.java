/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.driver.fake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.client.VitamRestEasyConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.driver.AbstractConnection;
import fr.gouv.vitam.storage.driver.AbstractDriver;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.driver.model.StorageGetMetadataRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StorageOfferLogRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Driver implementation for test only
 */
public class FakeDriverImpl extends AbstractDriver {

    @Override
    protected VitamClientFactoryInterface addInternalOfferAsFactory(final StorageOffer offer,
        final Properties parameters) {
        VitamClientFactoryInterface<FakeConnectionImpl> factory =
            new VitamClientFactoryInterface<FakeConnectionImpl>() {
                private StorageOffer offerf = offer;

                @Override
                public Client getHttpClient() {
                    return null;
                }

                @Override
                public Client getHttpClient(boolean useChunkedMode) {
                    return null;
                }

                @Override
                public FakeConnectionImpl getClient() {
                    return new FakeConnectionImpl(offer.getId());
                }

                @Override
                public String getResourcePath() {
                    return null;
                }

                @Override
                public String getServiceUrl() {
                    return offerf.getBaseUrl();
                }

                @Override
                public Map<VitamRestEasyConfiguration, Object> getDefaultConfigCient() {
                    return null;
                }

                @Override
                public Map<VitamRestEasyConfiguration, Object> getDefaultConfigCient(boolean chunkedMode) {
                    return null;
                }

                @Override
                public ClientConfiguration getClientConfiguration() {
                    return null;
                }

                @Override
                public fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType getVitamClientType() {
                    return null;
                }

                @Override
                public VitamClientFactoryInterface<?> setVitamClientType(
                    fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType vitamClientType) {
                    return null;
                }

                @Override
                public void changeResourcePath(String resourcePath) {
                    // empty
                }

                @Override
                public void changeServerPort(int port) {
                    // empty
                }

                @Override
                public void shutdown() {
                    // empty
                }

                @Override
                public void resume(Client client, boolean chunk) {
                    // Empty
                }

            };
        return factory;
    }

    @Override
    public Connection connect(String offerId) throws StorageDriverException {
        if (offerId.contains("fail")) {
            throw new StorageDriverException(getName(),
                "Intentionaly thrown", false);
        }
        return new FakeConnectionImpl(offerId);
    }

    @Override
    public boolean isStorageOfferAvailable(String offerId) throws StorageDriverException {
        if (offerId.contains("fail")) {
            throw new StorageDriverException(getName(),
                "Intentionaly thrown", false);
        }
        return true;
    }

    @Override
    public String getName() {
        return "Fake driver";
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    class FakeConnectionImpl extends AbstractConnection {

        private final String offerId;

        FakeConnectionImpl(String offerId) {
            super("FakeDriverName", new TestVitamClientFactory<AbstractConnection>(1324, "/chemin/"));
            this.offerId = offerId;
        }

        @Override
        public StorageCapacityResult getStorageCapacity(Integer tenantId) throws StorageDriverException {
            Integer fakeTenant = -1;
            if (fakeTenant.equals(tenantId)) {
                throw new StorageDriverException("driverInfo",
                    "ExceptionTest", false);
            }

            final StorageCapacityResult result = new StorageCapacityResult(tenantId, 1000000);
            return result;
        }

        @Override
        public StorageGetResult getObject(StorageObjectRequest objectRequest) throws StorageDriverException {

            return new StorageGetResult(objectRequest.getTenantId(), objectRequest.getType(), objectRequest.getGuid(),
                new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null));
        }

        @Override
        public RequestResponse<TapeReadRequestReferentialEntity> createReadOrderRequest(StorageObjectRequest request)
            throws StorageDriverException {
            return new RequestResponseOK<>();
        }

        @Override
        public RequestResponse<TapeReadRequestReferentialEntity> getReadOrderRequest(String readOrderRequestId,
            int tenant) throws StorageDriverException {
            return new RequestResponseOK<>();
        }

        @Override
        public void removeReadOrderRequest(String readOrderRequestId, int tenant) throws StorageDriverException {
            //Empty
        }

        @Override
        public StoragePutResult putObject(StoragePutRequest objectRequest) throws StorageDriverException {
            if ("digest_bad_test".equals(objectRequest.getGuid())) {
                return new StoragePutResult(objectRequest.getTenantId(), objectRequest.getType(),
                    objectRequest.getGuid(),
                    objectRequest.getGuid(), "different_digest_hash", 0);
            }

            if (("fail-offer-" + offerId).equals(objectRequest.getGuid())) {
                throw new StorageDriverException(getName(), "Fake offer " + offerId + " failed", false);
            }

            if ("conflict".equals(objectRequest.getGuid())) {
                throw new StorageDriverConflictException(getName(), "conflict");
            }
            if ("retry_test".equals(objectRequest.getGuid())) {
                throw new StorageDriverException(getName(), "retry_test", false);
            } else {
                try {
                    final byte[] bytes = IOUtils.toByteArray(objectRequest.getDataStream());
                    final MessageDigest messageDigest = MessageDigest.getInstance(objectRequest.getDigestAlgorithm());
                    return new StoragePutResult(objectRequest.getTenantId(), objectRequest.getType(),
                        objectRequest.getGuid(),
                        objectRequest.getGuid(), BaseXx.getBase16(messageDigest.digest(bytes)), bytes.length);
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new StorageDriverException(getName(), "Digest or Storage Put in error", false, e);
                }
            }
        }

        @Override
        public StorageBulkPutResult bulkPutObjects(StorageBulkPutRequest request) {
            throw new IllegalStateException("Stop using mocks in production");
        }

        @Override
        public StorageRemoveResult removeObject(StorageRemoveRequest objectRequest) throws StorageDriverException {

            return new StorageRemoveResult(objectRequest.getTenantId(), objectRequest.getType(),
                objectRequest.getGuid(), true);

        }

        @Override
        public boolean objectExistsInOffer(StorageObjectRequest request) throws StorageDriverException {
            return "already_in_offer".equals(request.getGuid());
        }

        @Override
        public StorageMetadataResult getMetadatas(StorageGetMetadataRequest request) throws StorageDriverException {
            return new StorageMetadataResult(null);
        }

        @Override
        public RequestResponse<JsonNode> listObjects(StorageListRequest request) throws StorageDriverException {
            final RequestResponseOK<JsonNode> response = new RequestResponseOK<>(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<JsonNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            response.setHttpCode(Status.OK.getStatusCode());

            response.addHeader(GlobalDataRest.X_CURSOR, String.valueOf(false));
            response.addHeader(GlobalDataRest.X_CURSOR_ID, "newcursor");

            return response;
        }

        @Override
        public RequestResponse<OfferLog> getOfferLogs(StorageOfferLogRequest storageOfferLogRequest) {
            OfferLog offerLog = new OfferLog();
            offerLog.setContainer(storageOfferLogRequest.getType() + "_" + storageOfferLogRequest.getTenantId());
            offerLog.setFileName("fileName_" + (storageOfferLogRequest.getOffset() + 1));
            offerLog.setSequence(storageOfferLogRequest.getOffset() + 1);
            offerLog.setTime(LocalDateTime.of(2017, 12, 13, 12, 0, 0, 0));

            RequestResponseOK<OfferLog> requestResponseOK = new RequestResponseOK<>();
            requestResponseOK.addResult(offerLog);

            return requestResponseOK;
        }
    }

}
