package fr.gouv.vitam.common.storage.swift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.storage.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;

public class OpenstackSwiftTest {

    private ContentAddressableStorageAbstract storage;

    private static final String TENANT_ID = "1";
    private static final String TYPE = "object";
    private static final String OBJECT_ID = "aeaaaaaaaaaam7mxaa2pkak2bnhxy5aaaaaq";
    private static final String OBJECT_ID2 = "aeaaaaaaaaaam7mxaa2pkak2bnhxy4aaaaaq";

    private InputStream getInputStream(String file) throws IOException {
        return PropertiesUtils.getResourceAsStream(file);
    }

    @Before
    public void setup() throws IOException {
        final StorageConfiguration configuration = new StorageConfiguration();
        configuration.setProvider("openstack-swift").setCredential("vitam-cdh_password").setCephMode(true)
                .setSwiftUid("vitam-cdh").setSwiftSubUser("swift").setKeystoneEndPoint("http://143.126.93.21:8080/auth/v1.0");
        storage = new OpenstackSwift(configuration);
    }

    @After
    public void ending() {
        storage.close();
    }

    @Ignore
    @Test
    public void givenObjectAlreadyExistsWhenGetObjectMetadataThenReturnMetadatasObjectResult()
            throws ContentAddressableStorageException, IOException {
        String containerName = TENANT_ID + "_" + TYPE;
        // if (storage.isExistingContainer(containerName)){
        // storage.deleteContainer(containerName, true);
        // }
        storage.createContainer(containerName);

        storage.putObject(containerName, OBJECT_ID, getInputStream("file1.pdf"));

        // get metadata of file
        MetadatasObject result = storage.getObjectMetadatas(containerName, OBJECT_ID);
        assertNotNull(result);

        assertEquals(OBJECT_ID, result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals(
                "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418",
                result.getDigest());
        assertEquals(6906, result.getFileSize());
        assertNotNull(result.getFileOwner());
        assertNotNull(result.getLastModifiedDate());

        storage.putObject(containerName, OBJECT_ID2, getInputStream("file2.pdf"));
        // get metadata of directory
        result = storage.getObjectMetadatas(containerName, null);
        assertEquals("object_1", result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals(null, result.getDigest());
        assertEquals(13843, result.getFileSize());
        assertEquals("Vitam_" + TENANT_ID, result.getFileOwner());
    }

}
