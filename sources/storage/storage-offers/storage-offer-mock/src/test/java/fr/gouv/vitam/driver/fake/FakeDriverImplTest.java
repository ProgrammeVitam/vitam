package fr.gouv.vitam.driver.fake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.GetObjectRequest;
import fr.gouv.vitam.storage.driver.model.GetObjectResult;
import fr.gouv.vitam.storage.driver.model.PutObjectRequest;
import fr.gouv.vitam.storage.driver.model.RemoveObjectRequest;
import fr.gouv.vitam.storage.driver.model.StorageCapacityRequest;
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
        Properties props = new Properties();
        props.setProperty("fail", "fail");
        driver.connect("props", props);
    }
    

    @Test
    public void givenCorrectPropertiesThenConnect() throws Exception {
        Properties props = new Properties();        
        Connection connect = driver.connect("props", props);
        assertNotNull(connect);
        StorageCapacityRequest storageCapacityRequest = new StorageCapacityRequest();
        StorageCapacityResult storageCapacityResult = connect.getStorageCapacity(storageCapacityRequest);
        assertEquals(storageCapacityResult.getUsableSpace(), 1000000);
        assertEquals(storageCapacityResult.getUsedSpace(), 99999);        

        try {
            storageCapacityRequest.setTenantId("daFakeTenant");
            storageCapacityResult = connect.getStorageCapacity(storageCapacityRequest);
            fail("Should raized an exception");
        } catch (final StorageDriverException e) {

        }
        
        GetObjectResult getObjectResult = connect.getObject(new GetObjectRequest());
        assertNotNull(getObjectResult);
        assertNotNull(getObjectResult.getObject());
        PutObjectRequest putObjectRequest = new PutObjectRequest();
        putObjectRequest.setGuid("digest_bad_test");
        assertNotNull(connect.putObject(putObjectRequest));        
        assertNotNull(connect.putObject(putObjectRequest, null));
        
        assertNotNull(connect.removeObject(new RemoveObjectRequest()));
        GetObjectRequest getORequest = new GetObjectRequest();
        getORequest.setGuid("already_in_offer");
        assertTrue(connect.objectExistsInOffer(getORequest));
        
        connect.close();
    }

    @Test
    public void getNameOK() throws Exception {
        assertEquals(DRIVER_NAME, driver.getName());
    }

    @Test
    public void isStorageOfferAvailableOK() throws Exception {
        Properties props = new Properties();
        assertEquals(true, driver.isStorageOfferAvailable("s", props));
    }

    @Test(expected = StorageDriverException.class)
    public void isStorageOfferAvailableKO() throws Exception {
        Properties props = new Properties();
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
