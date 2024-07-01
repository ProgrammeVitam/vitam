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
package fr.gouv.vitam.cas.container.swift;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageJcloudsAbstract;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import org.jclouds.openstack.swift.v1.SwiftApi;
import org.jclouds.openstack.swift.v1.features.AccountApi;
import org.jclouds.openstack.swift.v1.features.ContainerApi;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpenstackSwiftTest {

    private ContentAddressableStorageJcloudsAbstract storage;

    private static final String TENANT_ID = "1";
    private static final String TYPE = "object";
    private static final String OBJECT_ID = "aeaaaaaaaaaam7mxaa2pkak2bnhxy5aaaaaq";
    private static final String OBJECT_ID2 = "aeaaaaaaaaaam7mxaa2pkak2bnhxy4aaaaaq";

    SwiftApi swiftApi = mock(SwiftApi.class);
    ContainerApi containerApi = mock(ContainerApi.class);
    AccountApi accountApi = mock(AccountApi.class);

    private InputStream getInputStream(String file) throws IOException {
        return PropertiesUtils.getResourceAsStream(file);
    }

    @Before
    public void setup() throws IOException {
        final StorageConfiguration configuration = new StorageConfiguration();
        configuration
            .setProvider("openstack-swift")
            .setSwiftPassword("vitam-cdh_password")
            .setSwiftDomain("vitam-cdh")
            .setSwiftUser("swift")
            .setSwiftKeystoneAuthUrl("http://143.126.93.21:8080/auth/v1.0");

        storage = new OpenstackSwift(configuration, swiftApi, containerApi, accountApi);
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
        storage.createContainer(containerName);

        storage.putObject(containerName, OBJECT_ID, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);

        // get metadata of file
        MetadatasObject result = storage.getObjectMetadata(containerName, OBJECT_ID, false);
        assertNotNull(result);

        assertEquals(OBJECT_ID, result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals(
            "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418",
            result.getDigest()
        );
        assertEquals(6906, result.getFileSize());
        assertNotNull(result.getLastModifiedDate());

        storage.putObject(containerName, OBJECT_ID2, getInputStream("file2.pdf"), DigestType.SHA512, 6937L);
        // get metadata of directory
        result = storage.getObjectMetadata(containerName, null, false);
        assertEquals("object_1", result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals(null, result.getDigest());
        assertEquals(13843, result.getFileSize());
    }

    @Test
    public void when_getObjectMetadata_not_found_should_throw_exception() {
        //GIVEN
        String containerName = TENANT_ID + "_" + TYPE;

        HashSet<String> conf = new HashSet<>();
        conf.add("conf");
        when(swiftApi.getConfiguredRegions()).thenReturn(conf);

        ObjectApi objectApi = mock(ObjectApi.class);
        when(swiftApi.getObjectApi("conf", containerName)).thenReturn(objectApi);
        when(objectApi.get(OBJECT_ID)).thenReturn(null);

        //WHEN and THEN
        assertThatThrownBy(() -> storage.getObjectMetadata(containerName, OBJECT_ID, false)).isInstanceOf(
            ContentAddressableStorageException.class
        );
    }
}
