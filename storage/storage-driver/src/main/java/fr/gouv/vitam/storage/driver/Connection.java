/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

package fr.gouv.vitam.storage.driver;

import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.GetObjectRequest;
import fr.gouv.vitam.storage.driver.model.GetObjectResult;
import fr.gouv.vitam.storage.driver.model.PutObjectRequest;
import fr.gouv.vitam.storage.driver.model.PutObjectResult;
import fr.gouv.vitam.storage.driver.model.RemoveObjectRequest;
import fr.gouv.vitam.storage.driver.model.RemoveObjectResult;

import java.io.File;

/**
 * Represents a connection to the distant storage offer service that is provided by the driver when calling the
 * connect method:
 * <p>
 * {@code myDriver.connect(serviceUrl, parameters);}
 * </p>
 * The connection implementation is driver dependent but MUST hold enough informations/parameters/configurations to be
 * able to contact the distant offer service without the need to give additional connection related parameters on
 * further request done with this connection. In some cases it may be considered as a "session".
 *
 * Note: Connection extends {@link AutoCloseable} so the connection implementation MUST provide a close() method
 * which responsibility is to cleanly close and remove.
 */
public interface Connection extends AutoCloseable {
    /**
     * Retrieve the remaining storage capacity available on the distant offer.
     * Return value MUST in bytes
     *
     * @return the capacity available in bytes
     * @throws StorageDriverException if any problem occurs during request
     */
    long getStorageRemainingCapacity() throws StorageDriverException;

    /**
     * Retrieve an object from the storage offer based on criterias defined in request argument.
     *
     * @param request the request to send. It contains informations needed to retrieve a given object.
     * @return a result that may contains metadatas as well as the binary file
     * @throws StorageDriverException if any problem occurs during request
     */
    GetObjectResult getObject(GetObjectRequest request) throws StorageDriverException;

    /**
     * Put the object file into the storage offer based on criterias defined in request argument and underlaying
     * connection parameters.
     *
     * @param request the request to send. It may contains informations needed to store the file.
     * @return a result that may contains metadatas or statistics about the object put operation.
     * @throws StorageDriverException if any problem occurs during request
     */
    PutObjectResult putObject(PutObjectRequest request) throws StorageDriverException;

    /**
     * Put the object file into the storage offer based on criterias defined in request argument and underlaying
     * connection parameters.
     *
     * @param request the request to send. It may contains informations needed to store the file.
     * @param object  the file to store as a {@link File} object to be used in replacement of the InpuStream
     *                contained in the request
     * @return a result that may contains metadata or statistics about the object put operation.
     * @throws StorageDriverException if any problem occurs during request
     */
    PutObjectResult putObject(PutObjectRequest request, File object) throws StorageDriverException;

    /**
     * Delete an object on the distant storage offer.
     *
     * @param request the request to send, it contains information needed to delete an object on the distant store
     * @return a result that may contains metadatas or statistics about the object removal operation.
     * @throws StorageDriverException if any problem occurs during request
     */
    RemoveObjectResult removeObject(RemoveObjectRequest request) throws StorageDriverException;

    /**
     * Override AutoCloseable implementation to specify the exception
     * @throws StorageDriverException to be thrown in case of any driver exception
     */
    @Override
    void close() throws StorageDriverException;
}
