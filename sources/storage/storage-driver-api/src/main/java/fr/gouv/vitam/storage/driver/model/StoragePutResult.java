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
package fr.gouv.vitam.storage.driver.model;

/**
 * Holds result data that come as a result of a request to put an object on the
 * distant storage offer
 */
public class StoragePutResult extends StorageObjectResult {

    private final String distantObjectId;
    private final String digestHashBase16;
    private final long objectSize;

    /**
     * Initialize the needed parameters for put results
     *
     * @param tenantId The request tenantId
     * @param type the type The request type
     * @param guid the object guid
     * @param distantObjectId The distant object id
     * @param digestHashBase16 the object digest hash
     * @param objectSize size of the object
     */
    public StoragePutResult(
        Integer tenantId,
        String type,
        String guid,
        String distantObjectId,
        String digestHashBase16,
        long objectSize
    ) {
        super(tenantId, type, guid);
        this.distantObjectId = distantObjectId;
        this.digestHashBase16 = digestHashBase16;
        this.objectSize = objectSize;
    }

    /**
     * @return the object Id
     */
    public String getDistantObjectId() {
        return distantObjectId;
    }

    /**
     * @return the digest
     */
    public String getDigestHashBase16() {
        return digestHashBase16;
    }

    /**
     * @return the object Size
     */
    public long getObjectSize() {
        return objectSize;
    }

    @Override
    public String toString() {
        return "DigestObjectId: " + distantObjectId + " Digest:" + digestHashBase16 + " Size: " + objectSize;
    }
}
