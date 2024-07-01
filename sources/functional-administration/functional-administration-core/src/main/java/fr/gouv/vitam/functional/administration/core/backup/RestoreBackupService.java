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
package fr.gouv.vitam.functional.administration.core.backup;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterBackupModel;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Service used to recover a Backup copy of the given Vitam collection.<br/>
 */
public interface RestoreBackupService {
    /**
     * get the latest file name according to the name suffix.<br/>
     *
     * @param strategy the storage strategy to be applied
     * @param offerId offer from the object should be readed
     * @param collection the collection to be restored
     * @param type the storage collection type.
     * @return the last version.
     */
    Optional<String> getLatestSavedFileName(
        final String strategy,
        final String offerId,
        final DataCategory type,
        final FunctionalAdminCollections collection
    );

    /**
     * Read the latest file using the name requested by getLatestSavedFileName.<br/>
     * Be careful, this method use tenant that is requested from the VitamSession
     *
     * @param strategy the storage strategy to be applied
     * @param collection the collection to be restored
     * @return the backup copy.
     */
    Optional<CollectionBackupModel> readLatestSavedFile(
        final String strategy,
        final String offerId,
        final FunctionalAdminCollections collection
    );

    /**
     * Retrieve list of offer log defining objects to reconstruct from offer log
     *
     * @param strategy storage strategy
     * @param category collection
     * @param offset offset
     * @param limit limit
     * @return list of offer log by bulk
     * @throws VitamRuntimeException storage error
     * @throws IllegalArgumentException input error
     */
    Iterator<List<OfferLog>> getListing(String strategy, DataCategory category, Long offset, int limit, Order order)
        throws StorageServerClientException, StorageNotFoundClientException;

    /**
     * Load data from storage
     *
     * @param strategy storage strategy
     * @param collection collection
     * @param filename name of file to load
     * @param offset offset
     * @return data
     * @throws VitamRuntimeException storage error
     * @throws IllegalArgumentException input error
     */
    AccessionRegisterBackupModel loadData(
        String strategy,
        FunctionalAdminCollections collection,
        String filename,
        long offset
    );
}
