/**
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
 */

package fr.gouv.vitam.storage.engine.server.distribution.impl;

import java.util.Properties;
import java.util.concurrent.Callable;

import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

/**
 * Thread to delete object on offer from its GUID TODO: how to test it ???
 */
public class DeleteThread implements Callable<Boolean> {

    private Driver driver;
    private StorageRemoveRequest request;
    private String offerId;

    /**
     * Default constructor
     *
     * @param driver
     *            the driver for the offer
     * @param request
     *            the remove request
     * @param offerId
     *            the offerId
     */
    public DeleteThread(Driver driver, StorageRemoveRequest request, String offerId) {
        this.driver = driver;
        this.request = request;
        this.offerId = offerId;
    }

    @Override
    public Boolean call() throws StorageException, StorageDriverException, InterruptedException {
        try (Connection connection = driver.connect(offerId)) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            StorageRemoveResult storageRemoveResult = connection.removeObject(request);
            if (storageRemoveResult.isObjectDeleted()) {
                return true;
            }
        }
        return false;
    }
}
