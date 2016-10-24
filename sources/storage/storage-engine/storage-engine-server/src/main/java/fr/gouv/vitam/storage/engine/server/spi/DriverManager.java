/*******************************************************************************
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
 *******************************************************************************/

package fr.gouv.vitam.storage.engine.server.spi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverMapperException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.server.spi.mapper.DriverMapper;
import fr.gouv.vitam.storage.engine.server.spi.mapper.FileDriverMapper;

/**
 * DriverManager implementation.
 *
 * Use to register storage driver and associates it with offers.
 *
 * Actually, it is not possible to add driver without a server restart (you can add the driver, do association with
 * offers but you have to restart the server to have the new driver).
 */
public class DriverManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DriverManager.class);

    private static final List<OfferDriverInfo> INSTANTIATED_DRIVERS = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, OfferDriverInfo> DRIVERS_OFFERS = Collections.synchronizedMap(new HashMap<>());

    private static final String DRIVER_MANAGER_CONF_FILE = "driver-location.conf";

    private static Optional<DriverMapper> mapper;

    static {
        try {
            // default mapper, use changeDriverMapper
            mapper = Optional.ofNullable(FileDriverMapper.getInstance());
        } catch (final StorageDriverMapperException exc) {
            LOGGER.warn("The driver mapper is not initialize. Association driver and offer will not be load and " +
                "modification will not be save. Change the mapper.");
        }
        final URLClassLoader ucl = getUrlClassLoader();
        ServiceLoader<Driver> loadedDrivers;
        if (ucl != null) {
            loadedDrivers = ServiceLoader.load(Driver.class, ucl);
        } else {
            loadedDrivers = ServiceLoader.load(Driver.class);
        }
        final Iterator<Driver> driversIterator = loadedDrivers.iterator();

        while (driversIterator.hasNext()) {
            final OfferDriverInfo driverInfo = new OfferDriverInfo(driversIterator.next());
            INSTANTIATED_DRIVERS.add(driverInfo);
            loadOffersIdsFor(driverInfo);
        }
    }

    private static URLClassLoader getUrlClassLoader() {
        File conf = null;
        try {
            conf = PropertiesUtils.findFile(DRIVER_MANAGER_CONF_FILE);
        } catch (final FileNotFoundException exc) {
            LOGGER.warn("Not configuration file found. Only use standard class loader (systemClassLoader)", exc);
        }
        URLClassLoader ucl = null;
        if (conf != null) {
            DriverManagerConfiguration configuration = null;
            try {
                configuration = PropertiesUtils.readYaml(conf, DriverManagerConfiguration.class);
            } catch (final IOException exc) {
                LOGGER.warn("cannot read configuration file for storage driver. Only use standard class loader " +
                    "(systemClassLoader)", exc);
            }
            if (configuration != null) {
                final File dir = new File(configuration.getDriverLocation());
                final File[] fDir = dir.listFiles(file -> file.getPath().toLowerCase().endsWith(".jar"));
                if (fDir != null) {
                    final URL[] urls = new URL[fDir.length];
                    for (int i = 0; i < fDir.length; i++) {
                        try {
                            urls[i] = fDir[i].toURI().toURL();
                        } catch (final MalformedURLException exc) {
                            LOGGER
                                .warn("Class loader error for {}, driver not loaded", fDir[i].getName());
                        }
                    }
                    ucl = new URLClassLoader(urls);
                }
            }
        }
        return ucl;
    }

    private DriverManager() {
        // Do nothing
    }

    /**
     * Change the driver mapper
     *
     * @param newMapper the driver mapper to use
     */
    public static void changeDriverMapper(DriverMapper newMapper) {
        ParametersChecker.checkParameter("The mapper cannot be null", newMapper);
        mapper = Optional.of(newMapper);
    }

    /**
     * Add offer to a driver
     *
     * @param name the driver name
     * @param offerId the offer ID to add
     * @throws StorageDriverMapperException thrown if error on driver mapper (persisting part) append
     */
    public static void addOfferToDriver(String name, String offerId) throws StorageDriverMapperException {
        for (final OfferDriverInfo driverInfo : INSTANTIATED_DRIVERS) {
            if (driverInfo.name.equals(name)) {
                final List<String> offersIds = new ArrayList<>();
                offersIds.add(offerId);
                addOffersToDriver(driverInfo, offersIds);
            }
        }
    }

    /**
     * Add offers IDs to a driver TODO : change link direction between driver and offer and persist it differently
     * 
     * @param name the driver name
     * @param offersIds the offers IDs list
     * @throws StorageDriverMapperException thrown if error on driver mapper (persisting part) append
     */
    public static void addOffersToDriver(String name, List<String> offersIds) throws StorageDriverMapperException {
        for (final OfferDriverInfo driverInfo : INSTANTIATED_DRIVERS) {
            if (driverInfo.name.equals(name)) {
                addOffersToDriver(driverInfo, offersIds);
            }
        }
    }

    /**
     * Remove one offer to a driver
     *
     * @param offerId the offer ID to remove
     * @throws StorageDriverMapperException thrown if error on driver mapper (persisting part) append
     * @throws StorageDriverNotFoundException thrown if the associated driver is not found (no driver / offer
     *         association)
     */
    public static void removeOffer(String offerId) throws StorageDriverMapperException, StorageDriverNotFoundException {
        final Driver driver = getDriverFor(offerId);
        if (offerIdAvailable(offerId)) {
            DRIVERS_OFFERS.remove(offerId);
            final List<String> offersIds = new ArrayList<>();
            offersIds.add(offerId);
            persistRemoveOffers(offersIds, driver.getClass().getName());
        } else {
            LOGGER.warn("Cannot remove no suitable driver associated to the offer ID {}", offerId);
        }
    }

    /**
     * Get the driver for one offer
     *
     * @param offerId required the offer ID
     * @return the associated driver
     * @throws StorageDriverNotFoundException thrown if the associated driver is not found (no driver / offer
     *         association)
     */
    public static Driver getDriverFor(String offerId) throws StorageDriverNotFoundException {
        final OfferDriverInfo driverInfo = DRIVERS_OFFERS.get(offerId);
        if (driverInfo != null) {
            return driverInfo.driver;
        }
        LOGGER.error("No suitable driver for offer ID : " + offerId);
        throw new StorageDriverNotFoundException("No suitable driver for offer ID : " + offerId);
    }

    private static void addOffersToDriver(OfferDriverInfo driverInfo, List<String> offersIds)
        throws StorageDriverMapperException {
        ParametersChecker.checkParameter("Offers id list cannot be null", offersIds);
        for (final String offerId : offersIds) {
            if (!offerIdAvailable(offerId)) {
                DRIVERS_OFFERS.put(offerId, driverInfo);
            } else {
                LOGGER.warn("Cannot add driver {} with name {} to the offer ID {}, offer already define" +
                    " for another  driver name {}", driverInfo.driver, driverInfo.name, offerId,
                    DRIVERS_OFFERS.get(offerId));
            }
        }
        persistAddOffers(offersIds, driverInfo.name);
    }

    private static void loadOffersIdsFor(OfferDriverInfo driverInfo) {
        if (mapper.isPresent()) {
            try {
                final List<String> offersIds = mapper.get().getOffersFor(driverInfo.name);
                for (final String offerId : offersIds) {
                    DRIVERS_OFFERS.put(offerId, driverInfo);
                }
            } catch (final StorageDriverMapperException exc) {
                LOGGER.warn("The driver mapper failed to load offers IDs for driver name {}",
                    driverInfo.name, exc);
            }
        }
    }

    private static void persistAddOffers(List<String> offersIds, String driverName)
        throws StorageDriverMapperException {
        if (mapper.isPresent()) {
            mapper.get().addOffersTo(offersIds, driverName);
        }
    }

    private static void persistRemoveOffers(List<String> offersIds, String driverName)
        throws StorageDriverMapperException {
        if (mapper.isPresent()) {
            mapper.get().removeOffersTo(offersIds, driverName);
        }
    }

    private static boolean offerIdAvailable(String offerId) {
        return DRIVERS_OFFERS.containsKey(offerId);
    }

}


/**
 * OfferDriverInfo use to easily have driver information. Do no hesitate to add required parameters
 */
class OfferDriverInfo {

    final Driver driver;
    final String name;

    /**
     * Constructor
     * 
     * @param driver the driver
     */
    OfferDriverInfo(Driver driver) {
        ParametersChecker.checkParameter("Driver cannot be null", driver);
        this.driver = driver;
        name = this.driver.getClass().getName();
    }

    @Override
    public String toString() {
        return "driver[className=" + name + "]";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OfferDriverInfo && driver == ((OfferDriverInfo) other).driver;
    }

    @Override
    public int hashCode() {
        return driver.hashCode();
    }
}
