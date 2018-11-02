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
package fr.gouv.vitam.common.storage.swift;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.cas.container.api.VitamPageSet;
import fr.gouv.vitam.common.storage.cas.container.api.VitamStorageMetadata;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.CountingInputStream;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.model.storage.object.options.ObjectLocation;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;

/**
 * Swift abstract implementation
 * Manage with all common swift methods
 */
public class Swift extends ContentAddressableStorageAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Swift.class);

    private static final String X_OBJECT_META_DIGEST = "X-Object-Meta-Digest";
    private static final String X_OBJECT_META_DIGEST_TYPE = "X-Object-Meta-Digest-Type";

    private final Supplier<OSClient> osClient;

    protected StorageConfiguration configuration;

    private Long swiftLimit;

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
    Swift(Supplier<OSClient> osClient, StorageConfiguration configuration, Long swiftLimit) {
        this.osClient = osClient;
        this.configuration = configuration;
        this.swiftLimit = swiftLimit;
    }

    /**
     * Abstract method to get authenticated openstack client, allow to switch between Keystone V2 and Keystone V3
     */
    @Override
    public void createContainer(String containerName) throws ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageServerException {
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
            throw new ContentAddressableStorageAlreadyExistException(
                ErrorMessage.CONTAINER_ALREADY_EXIST + containerName);
        }
    }

    @Override
    public boolean isExistingContainer(String containerName) {
        // Is this the best way to do that ?
        Map<String, String> metadata = osClient.get().objectStorage().containers().getMetadata(containerName);
        // more than 2 metadata then container exists (again, is this the best way ?)
        return metadata.size() > 2;
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

        if (size != null && size > swiftLimit) {
            bigFile(containerName, objectName, stream, size);
        } else {
            smallFile(containerName, objectName, stream);
        }
        return computeAndStoreDigestInMetadata(containerName, objectName, digestType);
    }

    private void bigFile(String containerName, String objectName, InputStream stream, Long size) {
        try {
            CountingInputStream segmentInputStream;
            int i = 1;
            long fileSizeRead = 0;
            do {
                final String objectNameToPut = objectName + "/" + i;
                BoundedInputStream boundedInputStream =
                    new BoundedInputStream(stream, swiftLimit);
                // for prevent closed stream in swift client
                boundedInputStream.setPropagateClose(false);
                LOGGER.info("number of segment: " + objectNameToPut);
                // for get the number of byte read to the stream
                segmentInputStream = new CountingInputStream(boundedInputStream);
                osClient.get().objectStorage().objects()
                    .put(containerName, objectNameToPut, Payloads.create(segmentInputStream));
                i++;
                fileSizeRead = fileSizeRead + segmentInputStream.getByteCount();
            } while (fileSizeRead != size);

            String dloManifest = "";
            ObjectPutOptions objectPutOptions = ObjectPutOptions.create();
            objectPutOptions.getOptions().put("X-Object-Manifest", containerName + "/" + objectName + "/");
            osClient.get().objectStorage().objects().put(
                containerName,
                objectName,
                Payloads.create(new ByteArrayInputStream(dloManifest.getBytes())),
                objectPutOptions);
        } finally {
            StreamUtils.closeSilently(stream);
        }

    }

    private void smallFile(String containerName, String objectName, InputStream stream) {
        osClient.get().objectStorage().objects().put(containerName, objectName, Payloads.create(stream));
    }

    private String computeAndStoreDigestInMetadata(String containerName, String objectName, DigestType digestType)
        throws ContentAddressableStorageException {
        // Same as the others (like HashFileSystem) but clearly not the best way
        String digest = super.computeObjectDigest(containerName, objectName, digestType);
        Map<String, String> metadataToUpdate = new HashMap<>();
        // Not necessary to put the "X-Object-Meta-"
        metadataToUpdate.put(X_OBJECT_META_DIGEST, digest);
        metadataToUpdate.put(X_OBJECT_META_DIGEST_TYPE, digestType.getName());
        if (!osClient.get().objectStorage().objects().updateMetadata(ObjectLocation.create(containerName, objectName),
            metadataToUpdate)) {
            LOGGER.error("Failed to update object metadata -> remove object");
            osClient.get().objectStorage().objects().delete(containerName, objectName);
            throw new ContentAddressableStorageServerException("Cannot put object " + objectName + " on container " +
                containerName);
        }
        return digest;
    }

    @Override
    public Response getObject(String containerName, String objectName) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        SwiftObject object = osClient.get().objectStorage().objects().get(containerName, objectName);
        if (object == null) {
            LOGGER.error(
                ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName + " in container '" + containerName + "'");
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.OBJECT_NOT_FOUND.getMessage() + objectName);
        }
        // Size in bytes ???
        return new AbstractMockClient.FakeInboundResponse(Response.Status.OK, object.download().getInputStream(),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, getXContentLengthHeader(object));
    }

    @Override
    public Response getObjectAsync(String containerName, String objectName) throws ContentAddressableStorageException {
        return getObject(containerName, objectName);
    }

    @Override
    public void deleteObject(String containerName, String objectName) throws
        ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        ActionResponse response = osClient.get().objectStorage().objects().delete(containerName, objectName);
        if (!response.isSuccess()) {
            if (response.getCode() == 404) {
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND + objectName);
            } else {
                throw new ContentAddressableStorageServerException("Error on deleting object " + objectName);
            }
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName) {
        SwiftObject object = osClient.get().objectStorage().objects().get(containerName, objectName);
        return object != null;
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
    public MetadatasObject getObjectMetadatas(String containerName, String objectId) {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectId);
        MetadatasStorageObject result = new MetadatasStorageObject();
        SwiftObject object = osClient.get().objectStorage().objects().get(containerName, objectId);
        // ugly
        result.setFileOwner("Vitam_" + containerName.split("_")[0]);
        // ugly
        result.setType(containerName.split("_")[1]);
        result.setObjectName(objectId);
        result.setDigest(object.getMetadata().get(X_OBJECT_META_DIGEST));
        result.setFileSize(object.getSizeInBytes());
        result.setLastModifiedDate(object.getLastModified().toString());
        return result;
    }

    @Override
    public VitamPageSet<? extends VitamStorageMetadata> listContainer(String containerName)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        List<? extends SwiftObject> list =
            osClient.get().objectStorage().objects().list(containerName, ObjectListOptions
                .create().path(containerName).limit(LISTING_MAX_RESULTS));
        if (list != null) {
            return OpenstackPageSetImpl.wrap(list);
        } else {
            throw new ContentAddressableStorageNotFoundException(containerName + " not found");
        }
    }

    @Override
    public VitamPageSet<? extends VitamStorageMetadata> listContainerNext(String containerName, String nextMarker)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        List<? extends SwiftObject> list =
            osClient.get().objectStorage().objects().list(containerName, ObjectListOptions
                .create().path(containerName).limit(LISTING_MAX_RESULTS).marker(nextMarker));
        if (list != null) {
            return OpenstackPageSetImpl.wrap(list);
        } else {
            throw new ContentAddressableStorageNotFoundException(containerName + " not found");
        }
    }

    @Override
    public void close() {
        // nothing ? have to do something ?
    }

    private MultivaluedHashMap<String, Object> getXContentLengthHeader(SwiftObject object) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        List<Object> headersList = new ArrayList<>();
        headersList.add(object.getSizeInBytes());
        headers.put(VitamHttpHeader.X_CONTENT_LENGTH.getName(), headersList);
        return headers;
    }

}
