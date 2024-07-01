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
package fr.gouv.vitam.storage.driver;

import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

import javax.annotation.Nonnull;
import java.util.Properties;

/**
 * Driver interface that all storage offer drivers MUST implement to be
 * discovered by the Vitam driver manager. </br>
 *
 * It describes all the services the storage offer MUST at least provide to the
 * Vitam engine.
 */

public interface Driver extends VitamAutoCloseable {
    /**
     * Create a connection to the distant offer service based on given offer Id name.
     * If no connection could be made, the driver
     * MUST throw a StorageException
     *
     * @param offerId the offerId name
     * @return a connection which MUST contains all necessary parameters and
     * initial configurations to allow further requests to the distant
     * offer service without needing to pass parameters/configurations.
     * @throws StorageDriverException if any problem occurs during connection
     */
    @Nonnull
    Connection connect(String offerId) throws StorageDriverException;

    /**
     * The driver MUST provide a way to check the availability of the storage
     * offer Id name.
     *
     * @param offerId the offerId name
     * @return MUST return true if the distant offer service is available to
     * accept further requests, false otherwise, including if the offer is not yet added
     * @throws StorageDriverException if any problem occurs during request
     */
    boolean isStorageOfferAvailable(String offerId) throws StorageDriverException;

    /**
     * Remove one offer from the Driver (from DriverManager)
     *
     * @param offer
     * @return True if the offer was removed, false if not existing
     */
    boolean removeOffer(String offer);

    /**
     * Add one offer to the Driver (from DriverManager)
     * The driver MUST provide a way to check the availability of the storage
     * offer based on storage offer and configuration parameters. For
     * example it can be used to pass user and password properties in for
     * authentication.
     * <p>
     * The parameters argument can also be used to pass arbitrary string
     * tag/value pairs as connection arguments.
     * </p>
     *
     * @param offer the storage offer configuration
     * @param parameters other extra parameters
     * @return True if the offer was removed, false if not existing
     */
    boolean addOffer(StorageOffer offer, Properties parameters);

    /**
     * Return true if offer exists for the driver, false else
     *
     * @param offerId
     * @return True if the offer is declared in the driver
     */
    boolean hasOffer(String offerId);

    /**
     * The driver implementation MUST provide a constant name which SHOULD be
     * shared accross instances of the same driver implementation. Then it is
     * <em>strongly recommended</em> to use a static final field in your driver
     * implementation.
     * <p>
     * This name MAY be used in user interface to provide information on the
     * driver.
     * </p>
     *
     * @return The name of the driver which SHOULD be constant
     */
    String getName();

    /**
     * Retrieves the driver's major version number.
     * <p>
     * This number MAY be used in user interface to provide information on the
     * driver.
     * </p>
     *
     * @return this driver's major version number
     */
    int getMajorVersion();

    /**
     * Retrieves the driver's minor version number.
     * <p>
     * This number MAY be used in user interface to provide information on the
     * driver.
     * </p>
     *
     * @return this driver's minor version number
     */
    int getMinorVersion();
}
