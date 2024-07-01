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
package fr.gouv.vitam.storage.engine.server.spi;

import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverMapperException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.server.spi.mapper.DriverMapper;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * WARNING:
 *
 * This tests are using two drivers implementations. These drivers are
 * implemented in two externals projects, with maven packaging to jar. They only
 * implements the driver interface (and they absolutely do nothing, as fake
 * driver
 * Also don't forget to make the file to
 * declare the implementation to the {@link java.util.ServiceLoader} in the
 * META-INF/services directory.
 *
 * So, if the driver implementation change, do not forget to change the jars for
 * the two implementations in this way.
 */
public class DriverManagerTest {

    private static final String MON_MODULE_DRIVER_MON_DRIVER = "fr.gouv.vitam.driver.fake.FakeDriverImpl";
    private static final String THE_DRIVER_THE_DRIVER = "the.driver.TheDriver";
    private static final String OFFER_1 = "DriverManagerTestoffer1";
    private static final String OFFER_3 = "DriverManagerTestoffer3";

    @After
    public void removePersistFiles() throws IOException {
        Files.deleteIfExists(Paths.get(MON_MODULE_DRIVER_MON_DRIVER));
        Files.deleteIfExists(Paths.get(THE_DRIVER_THE_DRIVER));
    }

    @Test(expected = StorageDriverMapperException.class)
    public void addOfferAndRetrieveDriverTest() throws Exception {
        DriverManager.addOfferToDriver(MON_MODULE_DRIVER_MON_DRIVER, OFFER_1);
        Driver driver = DriverManager.getDriverFor(OFFER_1);
        assertNotNull(driver);
        assertEquals(MON_MODULE_DRIVER_MON_DRIVER, driver.getClass().getName());

        DriverManager.addOfferToDriver(THE_DRIVER_THE_DRIVER, OFFER_1);
        driver = DriverManager.getDriverFor(OFFER_1);
        assertNotNull(driver);
        assertEquals(MON_MODULE_DRIVER_MON_DRIVER, driver.getClass().getName());
    }

    @Test
    public void addOffersAndretriveDriverTest() throws Exception {
        final List<String> offers = new ArrayList<>();
        offers.add("addOffersAndretriveDriverTest");
        DriverManager.addOffersToDriver(MON_MODULE_DRIVER_MON_DRIVER, offers);

        final Driver driver = DriverManager.getDriverFor("addOffersAndretriveDriverTest");
        assertNotNull(driver);
    }

    @Test(expected = StorageDriverNotFoundException.class)
    public void getUnexistingDriver() throws Exception {
        DriverManager.getDriverFor("test");
    }

    @Test
    public void addAndRemoveOffers() throws Exception {
        DriverManager.addOfferToDriver(MON_MODULE_DRIVER_MON_DRIVER, OFFER_3);

        Driver driver = DriverManager.getDriverFor(OFFER_3);
        assertNotNull(driver);

        DriverManager.removeOffer(OFFER_3);
        try {
            driver = DriverManager.getDriverFor(OFFER_3);
            fail("Excepted Storage Driver Not Found Exception");
        } catch (final StorageDriverNotFoundException exc) {
            // Nothing, it's ok
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeDriverMapperNullTest() throws Exception {
        DriverManager.changeDriverMapper(null);
    }

    @Test
    public void changeDriverMapperTest() {
        DriverManager.changeDriverMapper(new FakeDriverMapper());
    }

    class FakeDriverMapper implements DriverMapper {

        @Override
        public List<String> getOffersFor(String driverName) throws StorageDriverMapperException {
            return Arrays.asList("default");
        }

        @Override
        public void addOfferTo(String offerId, String driverName) throws StorageDriverMapperException {
            // Nothing
        }

        @Override
        public void addOffersTo(List<String> offersIdsToAdd, String driverName) throws StorageDriverMapperException {
            // Nothing
        }

        @Override
        public void removeOfferTo(String offerId, String driverName) throws StorageDriverMapperException {
            // Nothing
        }

        @Override
        public void removeOffersTo(List<String> offersIdsToRemove, String driverName)
            throws StorageDriverMapperException {
            // Nothing
        }
    }
}
