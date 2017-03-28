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
package fr.gouv.vitam.storage.offers.workspace.driver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Driver factory
 */
public class DriverFactory {
    /**
     * All drivers allocated (from DriverManager)
     */
    private final Map<String, DriverImplNew> drivers = new ConcurrentHashMap<>();

    private static final DriverFactory INSTANCE = new DriverFactory();

    /**
     * Empty constructor
     */
    private DriverFactory() {
        // Emptyt
    }

    /**
     * 
     * @return the DriverFactory
     */
    public static DriverFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 
     * @param driverName
     * @return the driver from the factory
     * @throws IllegalArgumentException if the driver does not exist
     */
    public DriverImplNew getDriver(String driverName) {
        if (drivers.containsKey(driverName)) {
            return drivers.get(driverName);
        }
        throw new IllegalArgumentException("Driver does not exist");
    }
    /**
     * Add a driver from the DriverManager
     * 
     * @param driverName
     * @return this
     */
    public DriverFactory addDriver(String driverName) {
        if (drivers.containsKey(driverName)) {
            throw new IllegalArgumentException("Driver already exists");
        }
        final DriverImplNew driver = new DriverImplNew(driverName);
        drivers.put(driverName, driver);
        return this;
    }

    /**
     * Remove a Driver from the DriverManager and shutdown all Offers Factory attached to it
     * 
     * @param driverName
     * @return this
     */
    public DriverFactory removeDriver(String driverName) {
        if (!drivers.containsKey(driverName)) {
            return this;
        }
        final DriverImplNew driver = drivers.remove(driverName);
        driver.close();
        return this;
    }
}
