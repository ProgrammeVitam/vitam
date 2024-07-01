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
package fr.gouv.vitam.storage.offers.core;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import fr.gouv.vitam.common.stream.MultiplexedStreamReader;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Path Sanity check wrapper around {@link DefaultOfferService}.
 * Ensures that all {@link DefaultOfferService} methods (current and future) properly implement file path sanity checks.
 */
public class SanityCheckOfferServiceDecorator implements DefaultOfferService {

    private final DefaultOfferService innerService;
    private final String rootPath;

    public SanityCheckOfferServiceDecorator(DefaultOfferService innerService, StorageConfiguration configuration) {
        this.innerService = innerService;

        StorageProvider provider = StorageProvider.getStorageProvider(configuration.getProvider());
        this.rootPath = provider.hasStoragePath()
            ? configuration.getStoragePath()
            : VitamConfiguration.getVitamTmpFolder();
    }

    @Override
    public String getObjectDigest(String containerName, String objectId, DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        checkSafeObjectPath(containerName, objectId);
        return innerService.getObjectDigest(containerName, objectId, digestAlgorithm);
    }

    @Override
    public ObjectContent getObject(String containerName, String objectId) throws ContentAddressableStorageException {
        checkSafeObjectPath(containerName, objectId);
        return innerService.getObject(containerName, objectId);
    }

    @Override
    public String createAccessRequest(String containerName, List<String> objectIds)
        throws ContentAddressableStorageException {
        for (String objectId : objectIds) {
            checkSafeObjectPath(containerName, objectId);
        }
        return innerService.createAccessRequest(containerName, objectIds);
    }

    @Override
    public Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws ContentAddressableStorageException {
        return innerService.checkAccessRequestStatuses(accessRequestIds, adminCrossTenantAccessRequestAllowed);
    }

    @Override
    public void removeAccessRequest(String accessRequestId, boolean adminCrossTenantAccessRequestAllowed)
        throws ContentAddressableStorageException {
        innerService.removeAccessRequest(accessRequestId, adminCrossTenantAccessRequestAllowed);
    }

    @Override
    public boolean checkObjectAvailability(String containerName, List<String> objectIds)
        throws ContentAddressableStorageException {
        for (String objectId : objectIds) {
            checkSafeObjectPath(containerName, objectId);
        }
        return innerService.checkObjectAvailability(containerName, objectIds);
    }

    @Override
    public String createObject(
        String containerName,
        String objectId,
        InputStream objectPart,
        DataCategory type,
        long size,
        DigestType digestType
    ) throws ContentAddressableStorageException {
        checkSafeObjectPath(containerName, objectId);
        return innerService.createObject(containerName, objectId, objectPart, type, size, digestType);
    }

    @Override
    public StorageBulkPutResult bulkPutObjects(
        String containerName,
        List<String> objectIds,
        MultiplexedStreamReader multiplexedStreamReader,
        DataCategory type,
        DigestType digestType
    ) throws ContentAddressableStorageException, IOException {
        for (String objectId : objectIds) {
            checkSafeObjectPath(containerName, objectId);
        }
        return innerService.bulkPutObjects(containerName, objectIds, multiplexedStreamReader, type, digestType);
    }

    @Override
    public boolean isObjectExist(String containerName, String objectId) throws ContentAddressableStorageException {
        checkSafeObjectPath(containerName, objectId);
        return innerService.isObjectExist(containerName, objectId);
    }

    @Override
    public ContainerInformation getCapacity(String containerName) throws ContentAddressableStorageException {
        checkSafeContainerPath(containerName);
        return innerService.getCapacity(containerName);
    }

    @Override
    public void deleteObject(String containerName, String objectId, DataCategory type)
        throws ContentAddressableStorageException {
        checkSafeObjectPath(containerName, objectId);
        innerService.deleteObject(containerName, objectId, type);
    }

    @Override
    public StorageMetadataResult getMetadata(String containerName, String objectId, boolean noCache)
        throws ContentAddressableStorageException {
        checkSafeObjectPath(containerName, objectId);
        return innerService.getMetadata(containerName, objectId, noCache);
    }

    @Override
    public List<OfferLog> getOfferLogs(String containerName, Long offset, int limit, Order order)
        throws ContentAddressableStorageException {
        checkSafeContainerPath(containerName);
        return innerService.getOfferLogs(containerName, offset, limit, order);
    }

    @Override
    public void listObjects(String containerName, ObjectListingListener objectListingListener)
        throws IOException, ContentAddressableStorageException {
        checkSafeContainerPath(containerName);
        innerService.listObjects(containerName, objectListingListener);
    }

    @Override
    public void compactOfferLogs() throws Exception {
        innerService.compactOfferLogs();
    }

    @Override
    public StorageBulkMetadataResult getBulkMetadata(String containerName, List<String> objectIds, Boolean noCache)
        throws ContentAddressableStorageException {
        for (String objectId : objectIds) {
            checkSafeObjectPath(containerName, objectId);
        }
        return innerService.getBulkMetadata(containerName, objectIds, noCache);
    }

    private void checkSafeObjectPath(String containerName, String objectId) throws ContentAddressableStorageException {
        try {
            SafeFileChecker.checkSafeFilePath(this.rootPath, containerName, objectId);
        } catch (IllegalPathException e) {
            throw new ContentAddressableStorageException("Illegal object path " + containerName + "/" + objectId, e);
        }
    }

    private void checkSafeContainerPath(String containerName) throws ContentAddressableStorageException {
        try {
            SafeFileChecker.checkSafeFilePath(this.rootPath, containerName);
        } catch (IllegalPathException e) {
            throw new ContentAddressableStorageException("Illegal container name " + containerName, e);
        }
    }
}
