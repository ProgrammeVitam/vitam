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
package fr.gouv.vitam.driver.fake;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.client.VitamRestEasyConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.storage.driver.AbstractConnection;
import fr.gouv.vitam.storage.driver.AbstractDriver;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverConflictException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.storage.driver.model.StorageAccessRequestCreationRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResultEntry;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.driver.model.StorageCheckObjectAvailabilityRequest;
import fr.gouv.vitam.storage.driver.model.StorageGetBulkMetadataRequest;
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
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Driver implementation for test only
 */
public class FakeDriverImpl extends AbstractDriver {

    private final Map<String, FakeConnectionImpl> fakeConnection = new ConcurrentHashMap<>();

    @Override
    protected VitamClientFactoryInterface<FakeConnectionImpl> addInternalOfferAsFactory(
        final StorageOffer offer,
        final Properties parameters
    ) {
        return new VitamClientFactoryInterface<>() {
            private final StorageOffer offerf = offer;

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
                return fakeConnection.computeIfAbsent(offer.getId(), FakeConnectionImpl::new);
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
            public VitamClientType getVitamClientType() {
                return null;
            }

            @Override
            public VitamClientFactoryInterface<?> setVitamClientType(VitamClientType vitamClientType) {
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
    }

    @Override
    @Nonnull
    public Connection connect(String offerId) throws StorageDriverException {
        if (offerId.contains("fail")) {
            throw new StorageDriverException(getName(), "Intentionaly thrown", false);
        }
        return fakeConnection.computeIfAbsent(offerId, FakeConnectionImpl::new);
    }

    @Override
    public boolean isStorageOfferAvailable(String offerId) throws StorageDriverException {
        if (offerId.contains("fail")) {
            throw new StorageDriverException(getName(), "Intentionaly thrown", false);
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

    public void clear() {
        this.fakeConnection.clear();
    }

    @FunctionalInterface
    public interface ThrowableFunction<T, R> {
        R apply(T t) throws StorageDriverException;
    }

    public class FakeConnectionImpl extends AbstractConnection {

        private final String offerId;
        private ThrowableFunction<StorageObjectRequest, StorageGetResult> getObjectFunction;

        FakeConnectionImpl(String offerId) {
            super("FakeDriverName", new TestVitamClientFactory<>(1324, "/chemin/"));
            this.offerId = offerId;

            // Default getObjectFunction
            getObjectFunction = objectRequest -> {
                if (
                    this.offerId.equals("myTapeOffer1") &&
                    objectRequest.getGuid().equals("MyUnavailableFromAsyncOfferObjectId")
                ) {
                    throw new StorageDriverUnavailableDataFromAsyncOfferException("any", "msg");
                }

                MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
                headers.add(VitamHttpHeader.X_CONTENT_LENGTH.getName(), "4");
                return new StorageGetResult(
                    objectRequest.getTenantId(),
                    objectRequest.getType(),
                    objectRequest.getGuid(),
                    new AbstractMockClient.FakeInboundResponse(
                        Status.OK,
                        new ByteArrayInputStream("test".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE,
                        headers
                    )
                );
            };
        }

        public String getOfferId() {
            return offerId;
        }

        public void setGetObjectFunction(ThrowableFunction<StorageObjectRequest, StorageGetResult> getObjectFunction) {
            this.getObjectFunction = getObjectFunction;
        }

        @Override
        public StorageCapacityResult getStorageCapacity(Integer tenantId) throws StorageDriverException {
            Integer fakeTenant = -1;
            if (fakeTenant.equals(tenantId)) {
                throw new StorageDriverException("driverInfo", "ExceptionTest", false);
            }

            return new StorageCapacityResult(tenantId, 1000000);
        }

        @Override
        public StorageGetResult getObject(StorageObjectRequest objectRequest) throws StorageDriverException {
            return this.getObjectFunction.apply(objectRequest);
        }

        @Override
        public String createAccessRequest(StorageAccessRequestCreationRequest request) {
            if (this.offerId.equals("myTapeOffer1")) {
                return "myAccessRequestId1";
            }
            if (this.offerId.equals("myTapeOffer2")) {
                return "myAccessRequestId2";
            }
            throw new IllegalStateException(
                "createAccessRequest should not be invoked with sync offer '" + this.offerId + "'"
            );
        }

        @Override
        public Map<String, AccessRequestStatus> checkAccessRequestStatuses(
            List<String> accessRequestIds,
            int tenant,
            boolean adminCrossTenantAccessRequestAllowed
        ) {
            if (this.offerId.equals("myTapeOffer1")) {
                if (!adminCrossTenantAccessRequestAllowed) {
                    throw new IllegalStateException("expected adminCrossTenantAccessRequestAllowed flag to be set");
                }

                return accessRequestIds
                    .stream()
                    .collect(
                        Collectors.toMap(
                            accessRequestId -> accessRequestId,
                            accessRequestId -> AccessRequestStatus.READY
                        )
                    );
            }
            if (this.offerId.equals("myTapeOffer2")) {
                if (adminCrossTenantAccessRequestAllowed) {
                    throw new IllegalStateException("expected adminCrossTenantAccessRequestAllowed flag to not be set");
                }

                return accessRequestIds
                    .stream()
                    .collect(
                        Collectors.toMap(
                            accessRequestId -> accessRequestId,
                            accessRequestId -> AccessRequestStatus.NOT_READY
                        )
                    );
            }
            throw new IllegalStateException(
                "checkAccessRequestStatuses should not be invoked with sync offer '" + this.offerId + "'"
            );
        }

        @Override
        public void removeAccessRequest(
            String accessRequestId,
            int tenant,
            boolean adminCrossTenantAccessRequestAllowed
        ) {
            if (!adminCrossTenantAccessRequestAllowed) {
                throw new IllegalStateException("expected adminCrossTenantAccessRequestAllowed flag to be set");
            }
            if (this.offerId.equals("myTapeOffer1") || this.offerId.equals("myTapeOffer2")) {
                return;
            }
            throw new IllegalStateException(
                "removeAccessRequest should not be invoked with sync offer '" + this.offerId + "'"
            );
        }

        @Override
        public boolean checkObjectAvailability(StorageCheckObjectAvailabilityRequest request) {
            if (this.offerId.equals("myTapeOffer1")) {
                return true;
            }
            if (this.offerId.equals("myTapeOffer2")) {
                return false;
            }
            throw new IllegalStateException(
                "checkObjectAvailability should not be invoked with sync offer '" + this.offerId + "'"
            );
        }

        @Override
        public StoragePutResult putObject(StoragePutRequest objectRequest) throws StorageDriverException {
            if ("digest_bad_test".equals(objectRequest.getGuid())) {
                return new StoragePutResult(
                    objectRequest.getTenantId(),
                    objectRequest.getType(),
                    objectRequest.getGuid(),
                    objectRequest.getGuid(),
                    "different_digest_hash",
                    0
                );
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
                    return new StoragePutResult(
                        objectRequest.getTenantId(),
                        objectRequest.getType(),
                        objectRequest.getGuid(),
                        objectRequest.getGuid(),
                        BaseXx.getBase16(messageDigest.digest(bytes)),
                        bytes.length
                    );
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new StorageDriverException(getName(), "Digest or Storage Put in error", false, e);
                }
            }
        }

        @Override
        public StorageBulkPutResult bulkPutObjects(StorageBulkPutRequest request) {
            throw new IllegalStateException("Fake driver not implemented");
        }

        @Override
        public StorageRemoveResult removeObject(StorageRemoveRequest objectRequest) {
            return new StorageRemoveResult(
                objectRequest.getTenantId(),
                objectRequest.getType(),
                objectRequest.getGuid(),
                true
            );
        }

        @Override
        public boolean objectExistsInOffer(StorageObjectRequest request) {
            return "already_in_offer".equals(request.getGuid());
        }

        @Override
        public StorageMetadataResult getMetadatas(StorageGetMetadataRequest request) {
            return new StorageMetadataResult(
                new StorageMetadataResult(request.getGuid(), request.getType(), "digest", 1234L, "now", "now")
            );
        }

        @Override
        public StorageBulkMetadataResult getBulkMetadata(StorageGetBulkMetadataRequest request) {
            return new StorageBulkMetadataResult(
                request
                    .getGuids()
                    .stream()
                    .map(objectId -> new StorageBulkMetadataResultEntry(objectId, "digest-" + objectId, 50L))
                    .collect(Collectors.toList())
            );
        }

        @Override
        public CloseableIterator<ObjectEntry> listObjects(StorageListRequest request) {
            return CloseableIteratorUtils.toCloseableIterator(
                IteratorUtils.singletonListIterator(new ObjectEntry("objectId", 100L))
            );
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

        @Override
        public Response launchOfferLogCompaction(VitamContext vitamContext) {
            return Response.status(Response.Status.OK).build();
        }
    }
}
