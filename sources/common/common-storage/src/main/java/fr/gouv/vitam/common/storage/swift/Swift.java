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
package fr.gouv.vitam.common.storage.swift;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.NullInputStream;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.ConnectionException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.model.storage.object.options.ObjectLocation;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Swift abstract implementation
 * Manage with all common swift methods
 */
public class Swift extends ContentAddressableStorageAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Swift.class);

    public static final String X_OBJECT_META_DIGEST = "X-Object-Meta-Digest";
    public static final String X_OBJECT_META_DIGEST_TYPE = "X-Object-Meta-Digest-Type";
    public static final String X_OBJECT_MANIFEST = "X-Object-Manifest";

    private final Supplier<OSClient> osClient;

    private final Long swiftLimit;

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
    }

    /**
     * Abstract method to get authenticated openstack client, allow to switch between Keystone V2 and Keystone V3
     */
    @Override
    public void createContainer(String containerName) throws ContentAddressableStorageServerException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);

        ActionResponse response = osClient.get().objectStorage().containers().create(containerName);
        if (!response.isSuccess()) {
            LOGGER.error("Error when try to create container with name: {}", containerName);
            LOGGER.error("Reason: {}", response.getFault());
            throw new ContentAddressableStorageServerException("Error when try to create container: " + response
                .getFault());
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
    public String putObject(String containerName, String objectName, InputStream stream, DigestType digestType,
        Long size) throws
        ContentAddressableStorageException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        // Swift has a limit on the size of a single uploaded object; by default this is 5GB.
        // However, the download size of a single object is virtually unlimited with the concept of segmentation.
        // Segments of the larger object are uploaded and a special manifest file is created that,
        // when downloaded, sends all the segments concatenated as a single object.
        // This also offers much greater upload speed with the possibility of parallel uploads of the segments.

        Digest digest = new Digest(digestType);

        String largeObjectPrefix;
        try (InputStream digestInputStream = digest.getDigestInputStream(stream);
            ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(digestInputStream, size)) {
            if (size > swiftLimit) {
                largeObjectPrefix = bigFile(containerName, objectName, exactSizeInputStream, size);
            } else {
                smallFile(containerName, objectName, exactSizeInputStream);
                largeObjectPrefix = null;
            }
        } catch (IOException | ConnectionException e) {
            throw new ContentAddressableStorageException("Could not put object " + containerName + "/" + objectName, e);
        }

        String streamDigest = digest.digestHex();

        String computedDigest = computeObjectDigest(containerName, objectName, digestType);
        if (!streamDigest.equals(computedDigest)) {
            throw new ContentAddressableStorageException(
                "Illegal state for container " + containerName + " and object " + objectName +
                    ". Stream digest " + streamDigest + " is not equal to computed digest " +
                    computedDigest);
        }

        storeDigest(containerName, objectName, digestType, streamDigest, largeObjectPrefix);
        return streamDigest;
    }

    private String bigFile(String containerName, String objectName, InputStream stream, Long size)
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
                BoundedInputStream segmentInputStream =
                    new BoundedInputStream(stream, segmentSize);
                // Prevent closing inner stream by swift client
                segmentInputStream.setPropagateClose(false);

                // Double check segment size
                try (InputStream exactSizeInputStream = new ExactSizeInputStream(segmentInputStream, segmentSize)) {

                    // Prevent retry uploading stream twice on token expiration
                    VitamAutoCloseInputStream autoCloseInputStream =
                        new VitamAutoCloseInputStream(exactSizeInputStream);

                    segmentTime.start();
                    LOGGER.info("Uploading segment: " + segmentName);
                    getObjectStorageService().
                        put(containerName, segmentName, Payloads.create(autoCloseInputStream));

                    PerformanceLogger.getInstance().
                        log("STP_Offer_" + getConfiguration().getProvider(), containerName,
                            "REAL_SWIFT_PUT_OBJECT_SEGMENT", segmentTime.elapsed(
                                TimeUnit.MILLISECONDS));
                    segmentTime.reset();
                }


                segmentIndex++;
                remainingSize -= segmentSize;
            }

            ObjectPutOptions objectPutOptions = ObjectPutOptions.create();
            String largeObjectPrefix = containerName + "/" + objectName + "/";
            objectPutOptions.getOptions().put(X_OBJECT_MANIFEST, largeObjectPrefix);
            getObjectStorageService().put(
                containerName,
                objectName,
                Payloads.create(new NullInputStream(0L)),
                objectPutOptions);
            return largeObjectPrefix;

        } finally {
            StreamUtils.closeSilently(stream);
            PerformanceLogger.getInstance().log("STP_Offer_" + getConfiguration().getProvider(), containerName,
                "REAL_SWIFT_PUT_OBJECT", times.elapsed(TimeUnit.MILLISECONDS));
        }

    }

    private void smallFile(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageException {
        Stopwatch times = Stopwatch.createStarted();
        try {
            // Prevent retry uploading stream twice on token expiration
            InputStream autoCloseInputStream = new VitamAutoCloseInputStream(stream);

            getObjectStorageService()
                .put(containerName, objectName, Payloads.create(autoCloseInputStream));
        } finally {
            PerformanceLogger.getInstance().log("STP_Offer_" + getConfiguration().getProvider(),
                containerName, "REAL_SWIFT_PUT_OBJECT", times.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private void storeDigest(String containerName, String objectName, DigestType digestType, String digest,
        String largeObjectPrefix)
        throws ContentAddressableStorageException {

        Stopwatch stopwatch = Stopwatch.createStarted();
        Map<String, String> headers = new HashMap<>();
        // Not necessary to put the "X-Object-Meta-"
        headers.put(X_OBJECT_META_DIGEST, digest);
        headers.put(X_OBJECT_META_DIGEST_TYPE, digestType.getName());
        if (largeObjectPrefix != null) {
            headers.put(X_OBJECT_MANIFEST, largeObjectPrefix);
        }

        getObjectStorageService()
            .updateMetadata(ObjectLocation.create(containerName, objectName), headers);
        PerformanceLogger.getInstance().log("STP_Offer_" + getConfiguration().getProvider(),
            containerName, "STORE_DIGEST_IN_METADATA", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public String getObjectDigest(String containerName, String objectName, DigestType digestType, boolean noCache)
        throws ContentAddressableStorageException {

        if (!noCache) {

            Stopwatch stopwatch = Stopwatch.createStarted();
            Map<String, String> metadata = getObjectStorageService()
                .getMetadata(containerName, objectName);
            PerformanceLogger.getInstance().log("STP_Offer_" + getConfiguration().getProvider(),
                containerName, "READ_DIGEST_FROM_METADATA", stopwatch.elapsed(TimeUnit.MILLISECONDS));

            if (metadata != null
                && metadata.containsKey(X_OBJECT_META_DIGEST)
                && metadata.containsKey(X_OBJECT_META_DIGEST_TYPE)
                && digestType.getName().equals(metadata.get(X_OBJECT_META_DIGEST_TYPE))) {

                return metadata.get(X_OBJECT_META_DIGEST);
            }

            LOGGER.warn(String.format(
                "Could not retrieve cached digest for object '%s' in container '%s'", objectName, containerName));
        }

        return computeObjectDigest(containerName, objectName, digestType);
    }

    @Override
    public ObjectContent getObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        return getObjectStorageService().download(containerName, objectName);
    }

    @Override
    public String createReadOrderRequest(String containerName, List<String> objectsIds) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void removeReadOrderRequest(String readRequestID) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteObject(String containerName, String objectName) throws
        ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        getObjectStorageService().delete(containerName, objectName);
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName) throws ContentAddressableStorageException {
        return getObjectStorageService().getObjectInformation(containerName, objectName).isPresent();
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
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectId);
        MetadatasStorageObject result = new MetadatasStorageObject();
        Optional<SwiftObject> object =
            getObjectStorageService().getObjectInformation(containerName, objectId);
        if (object.isEmpty()) {
            throw new ContentAddressableStorageNotFoundException("The Object" + objectId +
                " can not be found for container " + containerName);
        }
        result.setType(containerName.split("_")[1]);
        result.setObjectName(objectId);
        result.setDigest(object.get().getMetadata().get(X_OBJECT_META_DIGEST));
        result.setFileSize(object.get().getSizeInBytes());
        result.setLastModifiedDate(object.get().getLastModified().toString());

        return result;
    }

    @Override
    public void listContainer(String containerName, ObjectListingListener objectListingListener)
        throws ContentAddressableStorageException, IOException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);

        String nextMarker = null;
        do {
            //TODO #8205 bug  pour récupérer la liste des objet dans le container
            // supprimer le path dans ObjectListOptions pour récupérer la liste des objets dans le container
            ObjectListOptions objectListOptions = ObjectListOptions.create()
                .path(containerName)
                .limit(LISTING_MAX_RESULTS);

            if (nextMarker != null) {
                objectListOptions.marker(nextMarker);
            }

            List<? extends SwiftObject> swiftObjects =
                getObjectStorageService().list(containerName, objectListOptions);

            if (swiftObjects.isEmpty()) {
                break;
            }

            for (SwiftObject swiftObject : swiftObjects) {
                objectListingListener.handleObjectEntry(new ObjectEntry(
                    swiftObject.getName(),
                    swiftObject.getSizeInBytes()
                ));
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

    public Supplier<OSClient> getOsClient() {
        return osClient;
    }
}
