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

package fr.gouv.vitam.common.storage.swift;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDigestMismatchException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.ConnectionException;
import org.openstack4j.connectors.httpclient.HttpClientFactory;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.model.storage.object.options.ObjectLocation;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Swift abstract implementation
 * Manage with all common swift methods
 */
public class Swift extends ContentAddressableStorageAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Swift.class);

    public static final String X_OBJECT_META_DIGEST = "X-Object-Meta-Digest";
    public static final String X_OBJECT_META_DIGEST_TYPE = "X-Object-Meta-Digest-Type";
    public static final String X_OBJECT_MANIFEST = "X-Object-Manifest";
    public static final String X_NEWEST = "X-Newest";

    private final Supplier<OSClient> osClient;

    private final Long swiftLimit;
    private final int swiftNbRetries;
    private final int swiftWaitingTimeInMilliseconds;
    private final int swiftRandomRangeSleepInMilliseconds;

    /**
     * Constructor
     *
     * @param osClient the given type of osClient can be OSClientV2, OSClientV3
     * @param configuration StorageConfiguration
     */
    public Swift(Supplier<OSClient> osClient, StorageConfiguration configuration) {
        this(osClient, configuration, VitamConfiguration.getSwiftFileLimit());
    }

    @VisibleForTesting
    public Swift(Supplier<OSClient> osClient, StorageConfiguration configuration, Long swiftLimit) {
        super(configuration);
        this.osClient = osClient;
        this.swiftLimit = swiftLimit;
        this.swiftNbRetries = configuration.getSwiftNbRetries();
        this.swiftWaitingTimeInMilliseconds = configuration.getSwiftWaitingTimeInMilliseconds();
        this.swiftRandomRangeSleepInMilliseconds = configuration.getSwiftRandomRangeSleepInMilliseconds();
        customizeOpenstack4jHttpClient(configuration);
    }

    private static void customizeOpenstack4jHttpClient(StorageConfiguration configuration) {
        HttpClientFactory.registerInterceptor((httpClientBuilder, requestConfig, config) -> {
            if (configuration.isSwiftDisableKeepAlive()) {
                // Force disable keep-alive in request
                httpClientBuilder.setDefaultHeaders(List.of(new BasicHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE)));

                // Drop keep-alive from response header (just in case)
                httpClientBuilder.setConnectionReuseStrategy(new SwiftPreventKeepAliveStrategy());
            }
        });
    }

    /**
     * Abstract method to get authenticated openstack client, allow to switch between Keystone V2 and Keystone V3
     */
    @Override
    public void createContainer(String containerName) throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );

        ActionResponse response = osClient.get().objectStorage().containers().create(containerName);
        if (!response.isSuccess()) {
            LOGGER.error("Error when try to create container with name: {}", containerName);
            LOGGER.error("Reason: {}", response.getFault());
            throw new ContentAddressableStorageServerException(
                "Error when try to create container: " + response.getFault()
            );
        }
        if (response.isSuccess() && response.getCode() == 202) {
            LOGGER.warn("Container " + containerName + " already exists");
        }
    }

    @Override
    public boolean isExistingContainer(String containerName) {
        if (super.isExistingContainerInCache(containerName)) {
            return true;
        }
        // Is this the best way to do that ?
        Map<String, String> metadata = osClient.get().objectStorage().containers().getMetadata(containerName);
        // more than 2 metadata then container exists (again, is this the best way ?)
        boolean exists = metadata.size() > 2;
        cacheExistsContainer(containerName, exists);
        return exists;
    }

    @Override
    public void writeObject(
        String containerName,
        String objectName,
        InputStream inputStream,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        // Swift has a limit on the size of a single uploaded object; by default this is 5GB.
        // However, the download size of a single object is virtually unlimited with the concept of segmentation.
        // Segments of the larger object are uploaded and a special manifest file is created that,
        // when downloaded, sends all the segments concatenated as a single object.
        // This also offers much greater upload speed with the possibility of parallel uploads of the segments.

        try {
            if (size > swiftLimit) {
                bigFile(containerName, objectName, inputStream, size);
            } else {
                smallFile(containerName, objectName, inputStream);
            }
        } catch (IOException | ConnectionException e) {
            throw new ContentAddressableStorageException("Could not put object " + containerName + "/" + objectName, e);
        }
    }

    private void bigFile(String containerName, String objectName, InputStream stream, long size)
        throws ContentAddressableStorageException, IOException {
        Stopwatch times = Stopwatch.createStarted();
        Stopwatch segmentTime = Stopwatch.createUnstarted();

        try {
            long remainingSize = size;
            int segmentIndex = 1;
            while (remainingSize > 0) {
                long segmentSize = Math.min(swiftLimit, remainingSize);
                final String segmentName = objectName + "/" + String.format("%08d", segmentIndex);

                // Get current segment stream
                BoundedInputStream segmentInputStream = new BoundedInputStream(stream, segmentSize);
                // Prevent closing inner stream by swift client
                segmentInputStream.setPropagateClose(false);

                // Double check segment size
                try (InputStream exactSizeInputStream = new ExactSizeInputStream(segmentInputStream, segmentSize)) {
                    // Prevent retry uploading stream twice on token expiration
                    VitamAutoCloseInputStream autoCloseInputStream = new VitamAutoCloseInputStream(
                        exactSizeInputStream
                    );

                    segmentTime.start();
                    ObjectPutOptions objectPutOptions = ObjectPutOptions.create();
                    objectPutOptions.getOptions().putAll(enrichHeadersRequestWithVitamCookie(new HashMap<>()));

                    LOGGER.info("Uploading segment: " + segmentName);
                    getObjectStorageService()
                        .put(containerName, segmentName, Payloads.create(autoCloseInputStream), objectPutOptions);

                    PerformanceLogger.getInstance()
                        .log(
                            "STP_Offer_" + getConfiguration().getProvider(),
                            containerName,
                            "REAL_SWIFT_PUT_OBJECT_SEGMENT",
                            segmentTime.elapsed(TimeUnit.MILLISECONDS)
                        );
                    segmentTime.reset();
                }

                segmentIndex++;
                remainingSize -= segmentSize;
            }

            ObjectPutOptions objectPutOptions = ObjectPutOptions.create();
            String largeObjectPrefix = getLargeObjectPrefix(containerName, objectName);
            objectPutOptions.getOptions().put(X_OBJECT_MANIFEST, largeObjectPrefix);
            enrichHeadersRequestWithVitamCookie(objectPutOptions.getOptions());
            getObjectStorageService()
                .put(containerName, objectName, Payloads.create(new NullInputStream(0L)), objectPutOptions);
        } finally {
            StreamUtils.closeSilently(stream);
            PerformanceLogger.getInstance()
                .log(
                    "STP_Offer_" + getConfiguration().getProvider(),
                    containerName,
                    "REAL_SWIFT_PUT_OBJECT",
                    times.elapsed(TimeUnit.MILLISECONDS)
                );
        }
    }

    private String getLargeObjectPrefix(String containerName, String objectName) {
        return containerName + "/" + objectName + "/";
    }

    private void smallFile(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            // Prevent retry uploading stream twice on token expiration
            InputStream autoCloseInputStream = new VitamAutoCloseInputStream(stream);
            ObjectPutOptions objectPutOptions = ObjectPutOptions.create();
            enrichHeadersRequestWithVitamCookie(objectPutOptions.getOptions());
            getObjectStorageService()
                .put(containerName, objectName, Payloads.create(autoCloseInputStream), objectPutOptions);
        } finally {
            PerformanceLogger.getInstance()
                .log(
                    "STP_Offer_" + getConfiguration().getProvider(),
                    containerName,
                    "REAL_SWIFT_PUT_OBJECT",
                    times.elapsed(TimeUnit.MILLISECONDS)
                );
        }
    }

    @Override
    public void checkObjectDigestAndStoreDigest(
        String containerName,
        String objectName,
        String objectDigest,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException {
        checkObjectDigest(containerName, objectName, digestType, objectDigest);
        updateMetadataObject(containerName, objectName, digestType, objectDigest, size);
    }

    private void checkObjectDigest(String containerName, String objectName, DigestType digestType, String objectDigest)
        throws ContentAddressableStorageException {
        // Try read object WITHOUT X-Newest header. On retry (2nd+ calls), we'll read object WITH X-Newest=true header
        // Compute object digest and compare to expected (written) object digest:
        //   > On digest match (200 with valid digest)                      ==> OK
        //   > On digest mismatch (HTTP 200 with wrong digest or HTTP 404)  ==> Retry ONCE & right away with X-Newest header.
        //   > On technical error (HTTP 500)                                ==> Retry N times after delay with X-Newest header.
        // Total number of tries must not exceed "swiftNbRetries"

        for (int i = 0; i < this.swiftNbRetries; i++) {
            // Only add X-Newest header for retries
            boolean forceCheckInAllReplicas = (i > 0);

            // At most retry "swiftNbRetries" times
            boolean isLastRetry = (i == this.swiftNbRetries - 1);

            try {
                checkObjectDigest(containerName, objectName, digestType, objectDigest, forceCheckInAllReplicas);
                return;
            } catch (ContentAddressableStorageDigestMismatchException | ContentAddressableStorageNotFoundException e) {
                // Retry at most once
                if (forceCheckInAllReplicas || isLastRetry) {
                    throw e;
                }
                LOGGER.warn("Object digest check failed. Retrying with " + X_NEWEST + "...", e);
            } catch (ContentAddressableStorageException e) {
                if (isLastRetry) {
                    // Max retries reached
                    throw e;
                }
                LOGGER.warn("Object digest computation failed. Retrying after a delay...", e);
                sleepBeforeRetry();
            }
        }
    }

    private void checkObjectDigest(
        String containerName,
        String objectName,
        DigestType digestType,
        String objectDigest,
        boolean checkAllReplicas
    ) throws ContentAddressableStorageException {
        Map<String, String> headers = new HashMap<>();
        if (checkAllReplicas) {
            headers.put(X_NEWEST, "true");
        }
        try (
            InputStream stream = getObjectStorageService()
                .download(containerName, objectName, enrichHeadersRequestWithVitamCookie(headers))
                .getInputStream()
        ) {
            // Compute digest
            final Digest digest = new Digest(digestType);
            digest.update(stream);
            String computedDigest = digest.toString();

            // Check digest
            if (computedDigest.equals(objectDigest)) {
                LOGGER.debug("Object {}/{} digest {} checked successfully", containerName, objectName, computedDigest);
                return;
            }

            throw new ContentAddressableStorageDigestMismatchException(
                "Illegal state for container " +
                containerName +
                " and  object " +
                objectName +
                ". Stream digest " +
                objectDigest +
                " is not equal to computed digest " +
                computedDigest
            );
        } catch (ContentAddressableStorageNotFoundException e) {
            throw new ContentAddressableStorageNotFoundException(
                String.format(
                    "Inconsistent object swift state. Written object %s/%s not found...",
                    containerName,
                    objectName
                ),
                e
            );
        } catch (final IOException e) {
            throw new ContentAddressableStorageException(
                "Cannot check digest for object " + containerName + "/" + objectName,
                e
            );
        }
    }

    private void updateMetadataObject(
        String containerName,
        String objectName,
        DigestType digestType,
        String digest,
        long size
    ) throws ContentAddressableStorageException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Map<String, String> headers = new HashMap<>();
        if (size > swiftLimit) {
            headers.put(X_OBJECT_MANIFEST, getLargeObjectPrefix(containerName, objectName));
        }
        headers.put(X_OBJECT_META_DIGEST, digest);
        headers.put(X_OBJECT_META_DIGEST_TYPE, digestType.getName());

        enrichHeadersRequestWithVitamCookie(headers);

        RetryableOnException<Void, ContentAddressableStorageException> retryableOnException =
            new RetryableOnException<>(getRetryableParameters());
        retryableOnException.exec(() -> {
            getObjectStorageService().updateMetadata(ObjectLocation.create(containerName, objectName), headers);
            return null;
        });

        PerformanceLogger.getInstance()
            .log(
                "STP_Offer_" + getConfiguration().getProvider(),
                containerName,
                "STORE_DIGEST_IN_METADATA",
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
    }

    @Override
    public String getObjectDigest(String containerName, String objectName, DigestType digestType, boolean noCache)
        throws ContentAddressableStorageException {
        if (noCache) {
            return computeObjectDigest(containerName, objectName, digestType);
        }

        Map<String, String> metadata = getObjectMetadata(containerName, objectName);

        if (checkDigestProperty(metadata) && checkDigestTypeProperty(metadata, digestType)) {
            return metadata
                .entrySet()
                .stream()
                .filter(e -> e.getKey().equalsIgnoreCase(X_OBJECT_META_DIGEST))
                .map(Map.Entry::getValue)
                .findFirst()
                .get();
        } else {
            LOGGER.warn(
                String.format(
                    "Could not retrieve cached digest for object '%s' in container '%s'",
                    objectName,
                    containerName
                )
            );
            Pair<String, Long> objectDigestAndSize = computeObjectDigestAndSize(containerName, objectName, digestType);
            updateMetadataObject(
                containerName,
                objectName,
                digestType,
                objectDigestAndSize.getLeft(),
                objectDigestAndSize.getRight()
            );
            return objectDigestAndSize.getLeft();
        }
    }

    private Map<String, String> getObjectMetadata(String containerName, String objectName)
        throws ContentAddressableStorageException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            // First call ==> get object metadata WITHOUT X-Newest header
            // 2nd+ call  ==> get object metadata object WITH X-Newest=true header
            // Error handling :
            //   > HTTP 200 ==> OK
            //   > HTTP 404 ==> Retry ONCE & right away with X-Newest header.
            //   > HTTP 500 ==> Retry N times after delay with X-Newest header.
            // Total number of tries must not exceed "swiftNbRetries"

            for (int i = 0; i < this.swiftNbRetries; i++) {
                // Only add X-Newest header for retries
                boolean forceCheckInAllReplicas = (i > 0);
                boolean isLastRetry = (i == this.swiftNbRetries - 1);

                try {
                    Map<String, String> headers = new HashMap<>();
                    if (forceCheckInAllReplicas) {
                        headers.put(X_NEWEST, "true");
                    }
                    return getObjectStorageService()
                        .getMetadata(containerName, objectName, enrichHeadersRequestWithVitamCookie(headers));
                } catch (ContentAddressableStorageNotFoundException e) {
                    // Retry at most once
                    if (forceCheckInAllReplicas || isLastRetry) {
                        throw e;
                    }
                    LOGGER.warn("Object not found. Retrying with " + X_NEWEST + "...", e);
                } catch (ContentAddressableStorageException e) {
                    if (isLastRetry) {
                        // Max retries reached
                        throw e;
                    }
                    LOGGER.warn("Object metadata failed. Retrying after a delay...", e);
                    sleepBeforeRetry();
                }
            }
        } finally {
            PerformanceLogger.getInstance()
                .log(
                    "STP_Offer_" + getConfiguration().getProvider(),
                    containerName,
                    "READ_DIGEST_FROM_METADATA",
                    stopwatch.elapsed(TimeUnit.MILLISECONDS)
                );
        }
        throw new IllegalStateException("Unreachable statement");
    }

    private boolean checkDigestTypeProperty(Map<String, String> metadata, DigestType digestType) {
        return metadata
            .entrySet()
            .stream()
            .anyMatch(
                e -> e.getKey().equalsIgnoreCase(X_OBJECT_META_DIGEST_TYPE) && e.getValue().equals(digestType.getName())
            );
    }

    private boolean checkDigestProperty(Map<String, String> metadata) {
        return metadata
            .entrySet()
            .stream()
            .anyMatch(e -> e.getKey().equalsIgnoreCase(X_OBJECT_META_DIGEST) && e.getValue() != null);
    }

    @Override
    public ObjectContent getObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        // First call ==> read object WITHOUT X-Newest header
        // 2nd+ call  ==> read object WITH X-Newest=true header
        // Error handling :
        //   > HTTP 200 ==> OK
        //   > HTTP 404 ==> Retry ONCE & right away with X-Newest header.
        //   > HTTP 500 ==> Retry N times after delay with X-Newest header.
        // Total number of tries must not exceed "swiftNbRetries"

        for (int i = 0; i < this.swiftNbRetries; i++) {
            // Only add X-Newest header for retries
            boolean forceCheckInAllReplicas = (i > 0);

            // At most retry "swiftNbRetries" times
            boolean isLastRetry = (i == this.swiftNbRetries - 1);

            try {
                HashMap<String, String> headers = new HashMap<>();
                if (forceCheckInAllReplicas) {
                    headers.put(X_NEWEST, "true");
                }
                return getObjectStorageService()
                    .download(containerName, objectName, enrichHeadersRequestWithVitamCookie(headers));
            } catch (ContentAddressableStorageNotFoundException e) {
                // Retry at most once
                if (forceCheckInAllReplicas || isLastRetry) {
                    throw e;
                }
                LOGGER.warn("Object not found. Retrying with " + X_NEWEST + "...", e);
            } catch (ContentAddressableStorageException e) {
                if (isLastRetry) {
                    // Max retries reached
                    throw e;
                }
                LOGGER.warn("Object digest computation failed. Retrying after a delay...", e);
                sleepBeforeRetry();
            }
        }

        throw new IllegalStateException("Unreachable statement");
    }

    @Override
    public void deleteObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        RetryableOnException<Void, ContentAddressableStorageException> retryableOnException =
            new RetryableOnException<>(getRetryableParameters());
        retryableOnException.exec(() -> {
            // On error, retry both listing & deleting (in case some segments have been deleted, retrying them would throw exception)
            List<ObjectEntry> swiftObjects = new ArrayList<>();
            listObjectSegments(containerName, objectName, swiftObjects::add);
            getObjectStorageService()
                .deleteFullObject(
                    containerName,
                    objectName,
                    swiftObjects.stream().map(ObjectEntry::getObjectId).collect(Collectors.toList()),
                    enrichHeadersRequestWithVitamCookie(new HashMap<>())
                );
            return null;
        });
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName) throws ContentAddressableStorageException {
        RetryableOnException<Optional<SwiftObject>, ContentAddressableStorageException> retryableOnException =
            new RetryableOnException<>(getRetryableParameters());
        Optional<SwiftObject> object = retryableOnException.exec(
            () ->
                getObjectStorageService()
                    .getObjectInformation(
                        containerName,
                        objectName,
                        enrichHeadersRequestWithVitamCookie(new HashMap<>())
                    )
        );
        return object.isPresent();
    }

    @Override
    public ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter("Container name may not be null", containerName);
        final ContainerInformation containerInformation = new ContainerInformation();
        Map<String, String> metadata = osClient.get().objectStorage().containers().getMetadata(containerName);
        if (metadata.size() > 2) {
            containerInformation.setUsableSpace(-1);
        } else {
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND + containerName);
        }
        return containerInformation;
    }

    @Override
    public MetadatasObject getObjectMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectId
        );
        MetadatasStorageObject result = new MetadatasStorageObject();
        RetryableOnException<Optional<SwiftObject>, ContentAddressableStorageException> retryableOnException =
            new RetryableOnException<>(getRetryableParameters());
        Optional<SwiftObject> object = retryableOnException.exec(
            () ->
                getObjectStorageService()
                    .getObjectInformation(containerName, objectId, enrichHeadersRequestWithVitamCookie(new HashMap<>()))
        );

        if (object.isEmpty()) {
            throw new ContentAddressableStorageNotFoundException(
                "The Object" + objectId + " can not be found for container " + containerName
            );
        }

        result.setType(containerName.split("_")[1]);
        result.setObjectName(objectId);
        result.setDigest(getObjectDigest(containerName, objectId, VitamConfiguration.getDefaultDigestType(), noCache));
        result.setFileSize(object.get().getSizeInBytes());
        result.setLastModifiedDate(object.get().getLastModified().toString());

        return result;
    }

    @Override
    public void listContainer(String containerName, ObjectListingListener objectListingListener)
        throws ContentAddressableStorageException, IOException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );

        /*
         * List container objets.
         * Swift API reports all objects with their "raw" size :
         *  - regular objects (<= 4Gb) : {objectName} + {size}
         *  - large objects :
         *    - manifest : {objectName} with 0 byte {size}
         *    - segments : {objectName}/{index} with segment {size}
         *  Entries are sorted by name according to Swift Object Storage API specifications.
         *
         * Example listing :
         *
         *   object size   object name
         *   -------------------------
         *          1500   smallObject
         *             0   largeObject
         *    4000000000   largeObject/00000001
         *    4000000000   largeObject/00000002
         *     156035069   largeObject/00000003
         *       9853320   anotherSmallObject
         *
         * To compute large object total size, we need to sum its segments sizes.
         */

        String lastEntryName = null;
        long lastEntryTotalSize = 0;

        String nextMarker = null;
        do {
            ObjectListOptions objectListOptions = ObjectListOptions.create()
                .limit(getConfiguration().getSwiftListObjectBulkSize());

            if (nextMarker != null) {
                objectListOptions.marker(nextMarker);
            }

            // Swift container listing (with or without marker) is somehow idempotent (since result sorted ins ascending order, starting from nextMarker if any)
            // We can retry retrieving them on error.
            RetryableOnException<List<? extends SwiftObject>, ContentAddressableStorageException> retryable =
                new RetryableOnException<>(getRetryableParameters());

            List<? extends SwiftObject> swiftObjects = retryable.exec(
                () ->
                    getObjectStorageService()
                        .list(containerName, objectListOptions, enrichHeadersRequestWithVitamCookie(new HashMap<>()))
            );

            if (swiftObjects.isEmpty()) {
                break;
            }

            for (SwiftObject swiftObject : swiftObjects) {
                // process large object segment
                if (swiftObject.getName().contains("/")) {
                    if (lastEntryName != null && swiftObject.getName().startsWith(lastEntryName + "/")) {
                        lastEntryTotalSize += swiftObject.getSizeInBytes();
                        LOGGER.debug(
                            "Found large object segment " +
                            containerName +
                            "/" +
                            swiftObject.getName() +
                            ". Segment size: " +
                            swiftObject.getSizeInBytes() +
                            " bytes"
                        );
                    } else {
                        LOGGER.warn(
                            "Found orphan large object segment without matching object manifest " +
                            containerName +
                            "/" +
                            swiftObject.getName()
                        );
                    }
                    continue;
                }

                // This is either a regular object (with actual size), or a large object manifest (with 0-byte size).

                // We finished computing previous object total size, if any
                if (lastEntryName != null) {
                    objectListingListener.handleObjectEntry(new ObjectEntry(lastEntryName, lastEntryTotalSize));
                }

                // Mark current object
                lastEntryName = swiftObject.getName();
                lastEntryTotalSize = swiftObject.getSizeInBytes();
            }

            nextMarker = swiftObjects.get(swiftObjects.size() - 1).getName();
        } while (nextMarker != null);

        // Report very last object, if any
        if (lastEntryName != null) {
            objectListingListener.handleObjectEntry(new ObjectEntry(lastEntryName, lastEntryTotalSize));
        }
    }

    private void listObjectSegments(
        String containerName,
        String objectName,
        ObjectListingListener objectListingListener
    ) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );

        String nextMarker = null;
        do {
            ObjectListOptions objectListOptions = ObjectListOptions.create()
                .path(objectName + "/")
                .limit(getConfiguration().getSwiftListObjectBulkSize());

            if (nextMarker != null) {
                objectListOptions.marker(nextMarker);
            }

            // Swift container listing (with or without marker) is somehow idempotent (since result sorted ins ascending order, starting from nextMarker if any)
            // We can retry retrieving them on error.
            RetryableOnException<List<? extends SwiftObject>, ContentAddressableStorageException> retryable =
                new RetryableOnException<>(getRetryableParameters());

            List<? extends SwiftObject> swiftObjects = retryable.exec(
                () ->
                    getObjectStorageService()
                        .list(containerName, objectListOptions, enrichHeadersRequestWithVitamCookie(new HashMap<>()))
            );

            if (swiftObjects.isEmpty()) {
                break;
            }
            for (SwiftObject swiftObject : swiftObjects) {
                try {
                    objectListingListener.handleObjectEntry(
                        new ObjectEntry(swiftObject.getName(), swiftObject.getSizeInBytes())
                    );
                } catch (IOException e) {
                    throw new ContentAddressableStorageException(
                        "A problem occured while reading segments in the container : " + containerName
                    );
                }
            }
            nextMarker = swiftObjects.get(swiftObjects.size() - 1).getName();
        } while (nextMarker != null);
    }

    @Override
    public void close() {
        // nothing ? have to do something ?
    }

    private VitamSwiftObjectStorageService getObjectStorageService() {
        return new VitamSwiftObjectStorageService(this.osClient);
    }

    private RetryableParameters getRetryableParameters() {
        return new RetryableParameters(
            this.swiftNbRetries,
            this.swiftWaitingTimeInMilliseconds,
            this.swiftWaitingTimeInMilliseconds,
            this.swiftRandomRangeSleepInMilliseconds,
            TimeUnit.MILLISECONDS,
            VitamLogLevel.ERROR
        );
    }

    public Supplier<OSClient> getOsClient() {
        return osClient;
    }

    private Map<String, String> enrichHeadersRequestWithVitamCookie(Map<String, String> headers) {
        if (getConfiguration().getEnableCustomHeaders() == null) {
            LOGGER.debug("The vitam enable custom header property used by offers is not filled!");
        } else if (getConfiguration().getEnableCustomHeaders()) {
            LOGGER.debug("The vitam enable custom header used by offers is enabled!");
            if (getConfiguration().getCustomHeaders() == null || getConfiguration().getCustomHeaders().isEmpty()) {
                LOGGER.warn("No vitam custom headers have been filled!");
            } else {
                getConfiguration()
                    .getCustomHeaders()
                    .forEach(cookie -> headers.put(cookie.getKey(), cookie.getValue()));
            }
        } else {
            LOGGER.debug("The vitam enable custom header property used by offers is disabled!");
        }
        return headers;
    }

    private void sleepBeforeRetry() throws ContentAddressableStorageException {
        int randomRangeSleep = this.swiftRandomRangeSleepInMilliseconds == 0
            ? 0
            : ThreadLocalRandom.current().nextInt(this.swiftRandomRangeSleepInMilliseconds);
        try {
            Thread.sleep(randomRangeSleep + this.swiftWaitingTimeInMilliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ContentAddressableStorageException("Interrupted thread", e);
        }
    }
}
