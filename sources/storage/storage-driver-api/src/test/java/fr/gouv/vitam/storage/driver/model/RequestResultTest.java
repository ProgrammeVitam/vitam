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

package fr.gouv.vitam.storage.driver.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.VitamConfiguration;

/**
 * TEst for PutObjectRequestTest
 */
public class RequestResultTest {
    private static final ByteArrayInputStream BYTES = new ByteArrayInputStream("dsds".getBytes());
    private static StorageObjectRequest getObjectRequest;
    private static StorageGetResult getObjectResult;
    private static StorageRemoveRequest removeObjectRequest;
    private static StorageRemoveResult removeObjectResult;
    private static StorageCapacityResult storageCapacityResult;
    private static final Integer TENANT_ID = 0;

    @BeforeClass
    public static void init() {
        getObjectRequest = new StorageObjectRequest(TENANT_ID, "object", "oi");
        getObjectResult = new StorageGetResult(TENANT_ID, "object", "oi", Response.ok(BYTES).build());
        removeObjectRequest = new StorageRemoveRequest(TENANT_ID, "object", "oi", VitamConfiguration.getDefaultDigestType(),
                "digest");
        removeObjectResult = new StorageRemoveResult(TENANT_ID, "object", "oi", VitamConfiguration.getDefaultDigestType(),
                "digest", true);
        storageCapacityResult = new StorageCapacityResult(TENANT_ID, 1000, 100);
    }

    @Test
    public void testGetObject() throws Exception {
        assertNotNull(getObjectRequest);
        assertNotNull(getObjectResult);
    }

    @Test
    public void testRemoveObject() throws Exception {
        assertNotNull(removeObjectRequest);
        assertNotNull(removeObjectResult);
    }

    @Test
    public void testStorageCapacity() throws Exception {
        assertNotNull(storageCapacityResult);
        assertEquals(1000, storageCapacityResult.getUsableSpace());
        assertEquals(100, storageCapacityResult.getUsedSpace());
    }
}
