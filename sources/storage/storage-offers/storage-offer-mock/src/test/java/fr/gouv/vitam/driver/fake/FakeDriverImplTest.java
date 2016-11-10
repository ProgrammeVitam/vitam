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
package fr.gouv.vitam.driver.fake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.GetObjectRequest;
import fr.gouv.vitam.storage.driver.model.GetObjectResult;
import fr.gouv.vitam.storage.driver.model.PutObjectRequest;
import fr.gouv.vitam.storage.driver.model.RemoveObjectRequest;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;

public class FakeDriverImplTest {

    private static FakeDriverImpl driver;

    private static final String DRIVER_NAME = "Fake driver";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        driver = new FakeDriverImpl();
    }

    @Test(expected = StorageDriverException.class)
    public void givenIncorrectPropertiesThenRaiseAnException() throws Exception {
        final Properties props = new Properties();
        props.setProperty("fail", "fail");
        driver.connect("props", props);
    }


    @Test
    public void givenCorrectPropertiesThenConnect() throws Exception {
        final Properties props = new Properties();
        final Connection connect = driver.connect("props", props);
        assertNotNull(connect);
        StorageCapacityResult storageCapacityResult = connect.getStorageCapacity("0");
        assertEquals(storageCapacityResult.getUsableSpace(), 1000000);
        assertEquals(storageCapacityResult.getUsedSpace(), 99999);

        try {
            storageCapacityResult = connect.getStorageCapacity("daFakeTenant");
            fail("Should raized an exception");
        } catch (final StorageDriverException e) {

        }

        final GetObjectResult getObjectResult = connect.getObject(new GetObjectRequest("0" + this, "guid", "folder"));
        assertNotNull(getObjectResult);
        assertNotNull(getObjectResult.getObject());
        final PutObjectRequest putObjectRequest =
            new PutObjectRequest("tenantId" + this, VitamConfiguration.getDefaultDigestType().getName(), "guid", IOUtils.toInputStream("Vitam" +
                " test"),
                "type");
        assertNotNull(connect.putObject(putObjectRequest));

        try {
            final PutObjectRequest putObjectRequest2 =
                new PutObjectRequest("tenantId" + this, "fakeAlgorithm", "guid", IOUtils.toInputStream("Vitam test"),
                    "type");
            connect.putObject(putObjectRequest2);
            fail("Should raized an exception");
        } catch (final StorageDriverException e) {

        }

        final PutObjectRequest putObjectRequest3 =
            new PutObjectRequest("tenantId" + this, VitamConfiguration.getDefaultDigestType().getName(), "digest_bad_test",
                IOUtils.toInputStream("Vitam test"),
                "type");
        assertNotNull(connect.putObject(putObjectRequest3));

        assertNotNull(connect.removeObject(new RemoveObjectRequest()));
        assertTrue(connect.objectExistsInOffer(new GetObjectRequest("0" + this, "already_in_offer", "folder")));

        connect.close();
    }

    @Test
    public void getNameOK() throws Exception {
        assertEquals(DRIVER_NAME, driver.getName());
    }

    @Test
    public void isStorageOfferAvailableOK() throws Exception {
        final Properties props = new Properties();
        assertEquals(true, driver.isStorageOfferAvailable("s", props));
    }

    @Test(expected = StorageDriverException.class)
    public void isStorageOfferAvailableKO() throws Exception {
        final Properties props = new Properties();
        props.setProperty("fail", "fail");
        assertEquals(true, driver.isStorageOfferAvailable("s", props));
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
