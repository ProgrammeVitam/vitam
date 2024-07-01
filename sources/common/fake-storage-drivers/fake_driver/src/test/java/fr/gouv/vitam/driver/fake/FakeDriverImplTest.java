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
package fr.gouv.vitam.driver.fake;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StorageOfferLogRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StorageRemoveRequest;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FakeDriverImplTest {

    private static FakeDriverImpl driver;
    private static int tenant;
    private static StorageOffer offer = new StorageOffer();

    private static final String DRIVER_NAME = "Fake driver";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        driver = new FakeDriverImpl();
        tenant = Instant.now().getNano();
        offer.setId("FakeOffer");
        driver.addOffer(offer, null);
    }

    @Test(expected = StorageDriverException.class)
    public void givenIncorrectPropertiesThenRaiseAnException() throws Exception {
        final Properties props = new Properties();
        props.setProperty("fail", "fail");
        StorageOffer offer = new StorageOffer();
        offer.setId("fail");
        driver.connect(offer.getId());
    }

    @Test
    public void givenCorrectPropertiesThenConnect() throws Exception {
        try (Connection connect = driver.connect(offer.getId())) {
            assertNotNull(connect);
            StorageCapacityResult storageCapacityResult = connect.getStorageCapacity(1);
            assertEquals(storageCapacityResult.getUsableSpace(), 1000000);

            assertThatCode(() -> {
                connect.getStorageCapacity(-1);
            }).isInstanceOf(StorageDriverException.class);

            final StorageGetResult getObjectResult = connect.getObject(
                new StorageObjectRequest(tenant, "object", "guid")
            );
            assertNotNull(getObjectResult);
            assertNotNull(getObjectResult.getTenantId());
            assertNotNull(getObjectResult.getType());
            assertNotNull(getObjectResult.getGuid());
            assertNotNull(getObjectResult.getObject());
            final StoragePutRequest putObjectRequest = new StoragePutRequest(
                tenant,
                "type",
                "guid",
                VitamConfiguration.getDefaultDigestType().getName(),
                StreamUtils.toInputStream("Vitam" + " test")
            );
            assertNotNull(connect.putObject(putObjectRequest));

            assertThatCode(() -> {
                final StoragePutRequest putObjectRequest2 = new StoragePutRequest(
                    tenant,
                    "type",
                    "guid",
                    "fakeAlgorithm",
                    StreamUtils.toInputStream("Vitam test")
                );
                connect.putObject(putObjectRequest2);
            }).isInstanceOf(StorageDriverException.class);

            final StoragePutRequest putObjectRequest3 = new StoragePutRequest(
                tenant,
                "type",
                "digest_bad_test",
                VitamConfiguration.getDefaultDigestType().getName(),
                StreamUtils.toInputStream("Vitam test")
            );
            assertNotNull(connect.putObject(putObjectRequest3));

            assertNotNull(connect.removeObject(new StorageRemoveRequest(tenant, "type", "guid")));

            assertTrue(connect.objectExistsInOffer(new StorageObjectRequest(tenant, "object", "already_in_offer")));

            assertThatCode(() -> {
                StorageOfferLogRequest request = new StorageOfferLogRequest(1, "type", 0L, 0, Order.ASC);
                connect.getOfferLogs(request);
            }).doesNotThrowAnyException();
        }
    }

    @Test
    public void getNameOK() throws Exception {
        assertEquals(DRIVER_NAME, driver.getName());
    }

    @Test
    public void isStorageOfferAvailableOK() throws Exception {
        final StorageOffer offer = new StorageOffer();
        offer.setParameters(new HashMap<>());
        offer.getParameters().put("s", "s");
        offer.setId("FakeOffer");
        assertEquals(true, driver.isStorageOfferAvailable(offer.getId()));
    }

    @Test(expected = StorageDriverException.class)
    public void isStorageOfferAvailableKO() throws Exception {
        final StorageOffer offer = new StorageOffer();
        offer.setId("fail");
        assertEquals(true, driver.isStorageOfferAvailable(offer.getId()));
    }

    @Test
    public void getMajorVersionOK() throws Exception {
        assertEquals(1, driver.getMajorVersion());
    }

    @Test
    public void getMinorVersionOK() throws Exception {
        assertEquals(0, driver.getMinorVersion());
    }
}
