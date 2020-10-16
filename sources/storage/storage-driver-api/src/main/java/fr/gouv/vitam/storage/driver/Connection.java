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
package fr.gouv.vitam.storage.driver;

import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
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

/**
 * Represents a connection to the distant storage offer service that is provided by the driver when calling the connect
 * method:
 * <p>
 * {@code myDriver.connect(serviceUrl, parameters);}
 * </p>
 * The connection implementation is driver dependent but MUST hold enough informations/parameters/configurations to be
 * able to contact the distant offer service without the need to give additional connection related parameters on
 * further request done with this connection. In some cases it may be considered as a "session".
 *
 * Note: Connection extends {@link AutoCloseable} so the connection implementation MUST provide a close() method which
 * responsibility is to cleanly close and remove.
 */

public interface Connection extends AutoCloseable {
    /**
     * Retrieve the remaining storage capacity available on the distant offer. Return values MUST in bytes
     *
     * @param tenantId the tenant id needed to get storage capacity
     * @return the usable and used space in bytes and a remind of the given tenantId
     * @throws StorageDriverPreconditionFailedException if a bad request is encountered
     * @throws StorageDriverNotFoundException if container is not found
     * @throws StorageDriverException if any problem occurs during request
     */
    StorageCapacityResult getStorageCapacity(Integer tenantId)
        throws StorageDriverPreconditionFailedException, StorageDriverNotFoundException, StorageDriverException;

    /**
     * Retrieve an object from the storage offer based on criterias defined in request argument.
     *
     * @param request the request to send. It contains informations needed to retrieve a given object.
     * @return a result that may contains metadatas as well as the binary file
     * @throws StorageDriverException if any problem occurs during request
     * @throws IllegalArgumentException if request is wrong
     */
    StorageGetResult getObject(StorageObjectRequest request) throws StorageDriverException;

    /**
     * Asynchronous retrieve an object from the storage offer based on criterias defined in request argument.
     *
     * @param request the request to send. It contains informations needed to retrieve a given object.
     * @return a result that may contains metadatas as well as the binary file
     * @throws StorageDriverException if any problem occurs during request
     * @throws IllegalArgumentException if request is wrong
     */
    RequestResponse<TapeReadRequestReferentialEntity> createReadOrderRequest(StorageObjectRequest request)
        throws StorageDriverException;

    RequestResponse<TapeReadRequestReferentialEntity> getReadOrderRequest(String readOrderRequestId, int tenant)
        throws StorageDriverException;

    void removeReadOrderRequest(String readOrderRequestId, int tenant) throws StorageDriverException;

    /**
     * Put the object file into the storage offer based on criterias defined in request argument and underlaying
     * connection parameters.
     *
     * @param request the request to send. It may contains informations needed to store the file.
     * @return a result that may contains metadatas or statistics about the object put operation.
     * @throws StorageDriverException if any problem occurs during request
     */

    StoragePutResult putObject(StoragePutRequest request) throws StorageDriverException;

    /**
     * Bulk put object files into the storage offer.
     *
     * @throws StorageDriverException if any problem occurs during request
     */
    StorageBulkPutResult bulkPutObjects(StorageBulkPutRequest request) throws StorageDriverException;

    /**
     * Delete an object on the distant storage offer.
     *
     * @param request the request to send, it contains information needed to delete an object on the distant store
     * @return a result that may contains metadatas or statistics about the object removal operation.
     * @throws StorageDriverException if any problem occurs during request
     */
    StorageRemoveResult removeObject(StorageRemoveRequest request) throws StorageDriverException;

    /**
     * Check if an object is present in the offer
     *
     * @param request the request to send. It contains informations needed to retrieve a given object.
     * @return true if exists, else false
     * @throws StorageDriverException if any problem occurs during request
     */
    boolean objectExistsInOffer(StorageObjectRequest request) throws StorageDriverException;

    /**
     * Get metadata of object
     *
     * @param request
     * @return a result that may contain information about the storage metadata
     * @throws StorageDriverException
     */
    StorageMetadataResult getMetadatas(StorageGetMetadataRequest request) throws StorageDriverException;

    /**
     * List object on a container type
     *
     * @param request the request contains data needed to list container type
     * @return an iterator with each object metadata
     * @throws StorageDriverException
     */
    CloseableIterator<ObjectEntry> listObjects(StorageListRequest request)
        throws StorageDriverException, StorageDriverNotFoundException;

    /**
     * Get a listing of offer logs on a container type
     *
     * @param request the request contains data needed to retrieve the listing of the container
     * @return the listing of last objects save according to the request
     * @throws StorageDriverException to be thrown in case of any driver exception
     */
    RequestResponse<OfferLog> getOfferLogs(StorageOfferLogRequest request) throws StorageDriverException;

    /**
     * Override AutoCloseable implementation to specify the exception
     *
     * @throws StorageDriverException to be thrown in case of any driver exception
     */
    @Override
    void close() throws StorageDriverException;
}
