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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.apache.commons.io.input.ProxyInputStream;
import org.openstack4j.api.OSClient;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.model.storage.object.options.ObjectLocation;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;
import org.openstack4j.openstack.storage.object.domain.SwiftObjectImpl;
import org.openstack4j.openstack.storage.object.functions.MapWithoutMetaPrefixFunction;
import org.openstack4j.openstack.storage.object.functions.ParseObjectFunction;
import org.openstack4j.openstack.storage.object.internal.BaseObjectStorageService;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.openstack4j.core.transport.HttpEntityHandler.closeQuietly;

/**
 * Custom Object Storage service alternative for openstack4j
 *
 * Adapted from {@link org.openstack4j.openstack.storage.object.internal.ObjectStorageObjectServiceImpl} openstack4j lib (apache2 license)
 */
@NotThreadSafe
public class VitamSwiftObjectStorageService extends BaseObjectStorageService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamSwiftObjectStorageService.class);

    private static final String CONTENT_LENGTH = "Content-Length";

    public VitamSwiftObjectStorageService(Supplier<OSClient> osClientFactory) {
        initializeClient(osClientFactory);
    }

    private void initializeClient(Supplier<OSClient> osClientFactory) {
        // Dirty API hack : initializes call context in static/ThreadLocal
        osClientFactory.get();
    }

    public List<? extends SwiftObject> list(
        String containerName,
        ObjectListOptions options,
        Map<String, String> headers
    ) throws ContentAddressableStorageException {
        checkNotNull(containerName);

        Map<String, String> params = options != null ? options.getOptions() : Collections.emptyMap();

        LOGGER.debug("Listing container {} with params {}", containerName, params);

        HttpResponse resp = get(Void.class, uri("/%s", containerName))
            .param("format", "json")
            .params(params)
            .headers(headers)
            .executeWithResponse();
        try {
            if (isSuccessResponse(resp)) {
                List<? extends SwiftObject> objects = resp.getEntity(SwiftObjectImpl.SwiftObjects.class);
                LOGGER.debug("Listing container {} returned {} entries", containerName, objects.size());
                return objects;
            }

            if (isNotFoundResponse(resp)) {
                throw new ContentAddressableStorageNotFoundException(containerName + " not found");
            }

            throw new ContentAddressableStorageException(
                "Could not get list objects for container " +
                containerName +
                " with params " +
                params +
                ". Got status code: " +
                resp.getStatus()
            );
        } finally {
            closeQuietly(resp);
        }
    }

    public Optional<SwiftObject> getObjectInformation(
        String containerName,
        String objectName,
        Map<String, String> headers
    ) throws ContentAddressableStorageException {
        checkNotNull(containerName);
        checkNotNull(objectName);

        ObjectLocation location = ObjectLocation.create(containerName, objectName);

        LOGGER.debug("Getting object information {}/{}", location.getContainerName(), location.getObjectName());

        HttpResponse resp = head(Void.class, location.getURI()).headers(headers).executeWithResponse();
        try {
            if (isNotFoundResponse(resp)) {
                LOGGER.debug("Object {}/{} Not Found", location.getContainerName(), location.getObjectName());
                return Optional.empty();
            }

            if (isSuccessResponse(resp)) {
                LOGGER.debug("Getting object {}/{} succeeded", location.getContainerName(), location.getObjectName());
                return Optional.of(ParseObjectFunction.create(location).apply(resp));
            }

            throw new ContentAddressableStorageException(
                "Get object " +
                location.getContainerName() +
                "/" +
                location.getObjectName() +
                " failed with unexpected status code: " +
                resp.getStatus()
            );
        } finally {
            closeQuietly(resp);
        }
    }

    public ObjectContent download(String containerName, String objectName, Map<String, String> headers)
        throws ContentAddressableStorageException {
        checkNotNull(containerName);
        checkNotNull(objectName);

        ObjectLocation location = ObjectLocation.create(containerName, objectName);

        LOGGER.debug("Getting object {}/{}", location.getContainerName(), location.getObjectName());

        HttpResponse resp = get(Void.class, location.getURI()).headers(headers).executeWithResponse();

        boolean keepResponseOpen = false;

        try {
            if (isSuccessResponse(resp)) {
                LOGGER.debug("Getting object {}/{} succeeded", containerName, objectName);

                keepResponseOpen = true;

                String contentLengthStr = resp.header(CONTENT_LENGTH);
                if (contentLengthStr == null) {
                    // Content-Length is mandatory according to Swift Object Store API specification
                    // https://docs.openstack.org/api-ref/object-store/index.html?expanded=get-object-content-and-metadata-detail
                    throw new ContentAddressableStorageException(
                        "Could not read object length for " +
                        location.getContainerName() +
                        "/" +
                        location.getObjectName()
                    );
                }
                long contentLength = Long.parseLong(contentLengthStr);

                // Wrapper around response input stream to ensure response is not closed / garbage collected.
                InputStream inputStream = new AutoCloseResponseInputStream(resp);
                return new ObjectContent(inputStream, contentLength);
            }

            if (isNotFoundResponse(resp)) {
                throw new ContentAddressableStorageNotFoundException(
                    "Object not found " + containerName + "/" + objectName
                );
            }

            throw new ContentAddressableStorageException(
                "Get object " +
                location.getContainerName() +
                "/" +
                location.getObjectName() +
                " failed with unexpected status code: " +
                resp.getStatus()
            );
        } finally {
            if (!keepResponseOpen) {
                closeQuietly(resp);
            }
        }
    }

    public void put(String containerName, String name, Payload<?> payload) throws ContentAddressableStorageException {
        put(containerName, name, payload, ObjectPutOptions.NONE);
    }

    public void put(String containerName, String objectName, Payload<?> payload, ObjectPutOptions options)
        throws ContentAddressableStorageException {
        checkNotNull(containerName);
        checkNotNull(options);
        checkNotNull(payload);
        checkNotNull(objectName);

        if (options.getPath() != null && objectName.indexOf('/') == -1) objectName = options.getPath() +
        "/" +
        objectName;

        LOGGER.debug("Uploading object {}/{}", containerName, objectName);

        HttpResponse resp = put(Void.class, uri("/%s/%s", containerName, objectName))
            .entity(payload)
            .headers(options.getOptions())
            .contentType(options.getContentType())
            .paramLists(options.getQueryParams())
            .executeWithResponse();
        try {
            if (isSuccessResponse(resp)) {
                LOGGER.debug("Object uploaded successfully {}/{}", containerName, objectName);
                return;
            }

            if (isNotFoundResponse(resp)) {
                throw new ContentAddressableStorageNotFoundException(containerName + " not found");
            }

            throw new ContentAddressableStorageException(
                "Could not upload object " + containerName + "/" + objectName + ". Got status code: " + resp.getStatus()
            );
        } finally {
            closeQuietly(resp);
        }
    }

    public void deleteFullObject(
        String containerName,
        String objectName,
        List<String> objectNameSegments,
        Map<String, String> headers
    ) throws ContentAddressableStorageException {
        checkNotNull(containerName);
        checkNotNull(objectName);

        // DELETE SEGMENTS
        if (objectNameSegments != null && objectNameSegments.size() > 0) {
            for (String objectNameSegment : objectNameSegments) {
                ObjectLocation segmentLocation = ObjectLocation.create(containerName, objectNameSegment);
                HttpResponse respForDeletedSegment = delete(Void.class, segmentLocation.getURI()).executeWithResponse();
                LOGGER.debug(
                    "Deleting object segment {}/{}",
                    segmentLocation.getContainerName(),
                    segmentLocation.getObjectName()
                );

                if (isNotFoundResponse(respForDeletedSegment)) {
                    LOGGER.debug(
                        "Cannot delete object segment. Not found {}/{}",
                        segmentLocation.getContainerName(),
                        segmentLocation.getObjectName()
                    );
                    throw new ContentAddressableStorageNotFoundException(
                        "Object segment not found " +
                        segmentLocation.getContainerName() +
                        "/" +
                        segmentLocation.getObjectName()
                    );
                }
            }
            LOGGER.debug(
                "Object segments sized {} for manifest {} were deleted successfully.",
                objectNameSegments.size(),
                objectName
            );
        }

        // DELETE MANIFEST
        ObjectLocation location = ObjectLocation.create(containerName, objectName);
        LOGGER.debug("Deleting object {}/{}", location.getContainerName(), location.getObjectName());

        HttpResponse resp = delete(Void.class, location.getURI()).headers(headers).executeWithResponse();

        try {
            if (isSuccessResponse(resp)) {
                LOGGER.debug(
                    "Object deleted successfully {}/{}",
                    location.getContainerName(),
                    location.getObjectName()
                );
                return;
            }

            if (isNotFoundResponse(resp)) {
                LOGGER.debug(
                    "Cannot delete object. Not found {}/{}",
                    location.getContainerName(),
                    location.getObjectName()
                );
                throw new ContentAddressableStorageNotFoundException(
                    "Object not found " + location.getContainerName() + "/" + location.getObjectName()
                );
            }

            throw new ContentAddressableStorageException(
                "Could not delete object " +
                location.getContainerName() +
                "/" +
                location.getObjectName() +
                ". Got status code: " +
                resp.getStatus()
            );
        } finally {
            closeQuietly(resp);
        }
    }

    public Map<String, String> getMetadata(String containerName, String objectName, Map<String, String> headers)
        throws ContentAddressableStorageException {
        checkNotNull(containerName, objectName);

        LOGGER.debug("Getting metadata for object {}/{}", containerName, objectName);

        ObjectLocation location = ObjectLocation.create(containerName, objectName);
        HttpResponse resp = head(Void.class, location.getURI()).headers(headers).executeWithResponse();
        try {
            if (isSuccessResponse(resp)) {
                LOGGER.debug(
                    "Metadata retrieved successfully for object {}/{}",
                    location.getContainerName(),
                    location.getObjectName()
                );
                TreeMap<String, String> metadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                metadata.putAll(MapWithoutMetaPrefixFunction.INSTANCE.apply(resp.headers()));
                return metadata;
            }

            if (isNotFoundResponse(resp)) {
                throw new ContentAddressableStorageNotFoundException(
                    "No such object " + location.getContainerName() + "/" + location.getObjectName()
                );
            }

            throw new ContentAddressableStorageException(
                "Could not get metadata for object " +
                location.getContainerName() +
                "/" +
                location.getObjectName() +
                ". Got status code: " +
                resp.getStatus()
            );
        } finally {
            closeQuietly(resp);
        }
    }

    public void updateMetadata(ObjectLocation location, Map<String, String> headers)
        throws ContentAddressableStorageException {
        checkNotNull(location);
        checkNotNull(headers);

        LOGGER.debug(
            "Updating metadata for object {}/{} with headers {}",
            location.getContainerName(),
            location.getObjectName(),
            headers
        );

        HttpResponse resp = post(Void.class, location.getURI()).headers(headers).executeWithResponse();

        try {
            if (isSuccessResponse(resp)) {
                LOGGER.debug(
                    "Metadata updated successfully for object {}/{}",
                    location.getContainerName(),
                    location.getObjectName()
                );
                return;
            }

            throw new ContentAddressableStorageException(
                "Could not update metadata for object " +
                location.getContainerName() +
                "/" +
                location.getObjectName() +
                ". Got status code: " +
                resp.getStatus()
            );
        } finally {
            closeQuietly(resp);
        }
    }

    private boolean isSuccessResponse(HttpResponse httpResponse) {
        return httpResponse.getStatus() >= 200 && httpResponse.getStatus() <= 299;
    }

    private boolean isNotFoundResponse(HttpResponse resp) {
        return resp.getStatus() == 404;
    }

    private static final class AutoCloseResponseInputStream extends ProxyInputStream {

        private final HttpResponse resp;

        public AutoCloseResponseInputStream(HttpResponse resp) {
            super(resp.getInputStream());
            this.resp = resp;
        }

        @Override
        public void close() throws IOException {
            super.close();
            resp.close();
        }
    }
}
