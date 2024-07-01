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
package fr.gouv.vitam.ihmrecette.appserver;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;

import java.io.InputStream;
import java.util.Collections;

/**
 * StorageCRUDUtils class
 * this class is a tool for crud operation for storage
 */
public class StorageCRUDUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageCRUDUtils.class);

    StorageClient storageClient;

    StorageCRUDUtils() {
        this.storageClient = StorageClientFactory.getInstance().getClient();
    }

    @VisibleForTesting
    public StorageCRUDUtils(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    /**
     * get the list of strategies containing the offers
     */
    public RequestResponse<StorageStrategy> getStrategies() throws StorageServerClientException {
        return storageClient.getStorageStrategies();
    }

    /**
     * deleteFile
     *
     * @param dataCategory category
     * @param uid uid of file
     * @param strategyId strategy identifier
     * @param offerId offer identifier
     */
    public boolean deleteFile(DataCategory dataCategory, String uid, String strategyId, String offerId)
        throws StorageServerClientException {
        return storageClient.delete(strategyId, dataCategory, uid, Collections.singletonList(offerId));
    }

    /**
     * Create file or erase it if exists
     *
     * @param dataCategory dataCategory
     * @param uid uid
     * @param strategyId strategyID
     * @param offerId offerID
     * @param stream stream
     */
    public void storeInOffer(
        DataCategory dataCategory,
        String uid,
        String strategyId,
        String offerId,
        Long size,
        InputStream stream
    ) throws BackupServiceException {
        boolean delete = false;

        try {
            delete = deleteFile(dataCategory, uid, strategyId, offerId);
            if (!delete) {
                throw new BackupServiceException("file do not exits or can not deleted ");
            }
        } catch (StorageServerClientException e) {
            LOGGER.error("error when deleting file ", e);
        }

        try {
            storageClient.create(strategyId, uid, dataCategory, stream, size, Collections.singletonList(offerId));
        } catch (StorageServerClientException | InvalidParseOperationException e) {
            LOGGER.error("error when deleting file ", e);
            throw new BackupServiceException("fail to create");
        }
    }
}
