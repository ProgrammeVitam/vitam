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
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DriverManagerPersistanceTest {

    private static final String MON_MODULE_DRIVER_MON_DRIVER = "fr.gouv.vitam.driver.fake.FakeDriverImpl";
    private static final String THE_DRIVER_THE_DRIVER = "fr.gouv.vitam.driver.fake.FakeDriverImpl";
    private static final String OFFER_1 = "DriverManagerPersistanceTestoffer1";
    private static final String OFFER_7 = "DriverManagerPersistanceTestoffer7";

    @AfterClass
    public static void deleteFiles() throws IOException {
        Files.deleteIfExists(Paths.get(MON_MODULE_DRIVER_MON_DRIVER));
        Files.deleteIfExists(Paths.get(THE_DRIVER_THE_DRIVER));
    }

    @Test
    public void firstLoadTest() throws Exception {
        final List<String> offersDriver1 = new ArrayList<>();
        offersDriver1.add(OFFER_1);
        offersDriver1.add("DriverManagerPersistanceTestoffer2");
        offersDriver1.add("DriverManagerPersistanceTestoffer3");
        offersDriver1.add("DriverManagerPersistanceTestoffer4");

        DriverManager.addOffersToDriver(MON_MODULE_DRIVER_MON_DRIVER, offersDriver1);

        final Driver driver1 = DriverManager.getDriverFor(OFFER_1);

        assertNotNull(driver1);

        final List<String> offersDriver2 = new ArrayList<>();
        offersDriver2.add("DriverManagerPersistanceTestoffer5");
        offersDriver2.add("DriverManagerPersistanceTestoffer6");
        offersDriver2.add(OFFER_7);

        DriverManager.addOffersToDriver(THE_DRIVER_THE_DRIVER, offersDriver2);
        final Driver driver2 = DriverManager.getDriverFor(OFFER_7);
        assertNotNull(driver2);

        final Driver driver3 = DriverManager.getDriverFor(OFFER_1);
        assertNotNull(driver3);
        assertEquals(MON_MODULE_DRIVER_MON_DRIVER, driver3.getClass().getName());

        final Driver driver4 = DriverManager.getDriverFor(OFFER_7);
        assertNotNull(driver4);
        assertEquals(THE_DRIVER_THE_DRIVER, driver4.getClass().getName());
    }
}
