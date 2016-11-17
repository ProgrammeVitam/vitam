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

package fr.gouv.vitam.storage.engine.server.distribution.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;
import fr.gouv.vitam.storage.driver.exception.StorageObjectAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

/**
 *
 */
public class StorageDistributionImplTest {
    // FIXME P1 Fix Fake Driver

    private static final String STRATEGY_ID = "strategyId";
    private static final String TENANT_ID = "tenantId";
    private static StorageDistribution simpleDistribution;
    private static StorageDistribution customDistribution;
    private static WorkspaceClient client;

    @BeforeClass
    public static void initStatic() throws StorageDriverNotFoundException {

        final StorageConfiguration configuration = new StorageConfiguration();
        configuration.setUrlWorkspace("http://localhost:8080");
        client = Mockito.mock(WorkspaceClient.class);
        simpleDistribution = new StorageDistributionImpl(configuration);
        customDistribution = new StorageDistributionImpl(client, DigestType.SHA1);
    }

    @Test
    public void testStoreData_IllegalArguments()
        throws StorageNotFoundException, StorageTechnicalException, StorageObjectAlreadyExistsException {
        // storeData(String tenantId, String strategyId, String objectId,
        // CreateObjectDescription createObjectDescription, DataCategory category,
        // JsonNode jsonData)
        final CreateObjectDescription emptyDescription = new CreateObjectDescription();
        checkInvalidArgumentException(null, null, null, null, null);
        checkInvalidArgumentException("tenant_id", null, null, null, null);
        checkInvalidArgumentException("tenant_id", "strategy_id", null, null, null);
        checkInvalidArgumentException("tenant_id", "strategy_id", "object_id", null, null);
        checkInvalidArgumentException("tenant_id", "strategy_id", "object_id", emptyDescription, null);
        checkInvalidArgumentException("tenant_id", "strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);

        emptyDescription.setWorkspaceContainerGUID("ddd");
        checkInvalidArgumentException("tenant_id", "strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);

        emptyDescription.setWorkspaceContainerGUID(null);
        emptyDescription.setWorkspaceObjectURI("ddd");
        checkInvalidArgumentException("tenant_id", "strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);
    }

    @Test
    // FIXME P1 Update Fake driver : Add objectExistsInOffer
    public void testStoreData_OK() throws Exception {
        final String objectId = "id1";
        StoredInfoResult storedInfoResult = null;
        final CreateObjectDescription createObjectDescription = new CreateObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);

        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).build());
        try {
            // Store object
            storedInfoResult = customDistribution
                .storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf")).thenThrow(IllegalStateException.class);
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        assertNull(storedInfoResult.getObjectGroupId());
        String info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Object") && info.contains("successfully"));
        assertNotNull(storedInfoResult.getCreationTime());
        assertNotNull(storedInfoResult.getLastAccessTime());
        assertNotNull(storedInfoResult.getLastCheckedTime());
        assertNotNull(storedInfoResult.getLastModifiedTime());
        assertNull(storedInfoResult.getUnitIds());

        // Store Unit
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).build());
        try {
            storedInfoResult =
                customDistribution.storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription,
                    DataCategory.UNIT, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Unit") && info.contains("successfully"));

        // Store logbook
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).build());
        try {
            storedInfoResult =
                customDistribution.storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription,
                    DataCategory.LOGBOOK, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Logbook") && info.contains("successfully"));

        // Store object group
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).build());
        try {
            storedInfoResult =
                customDistribution.storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription,
                    DataCategory.OBJECT_GROUP, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("ObjectGroup") && info.contains("successfully"));
    }

    @Test(expected = StorageTechnicalException.class)
    public void testStoreData_DigestKO() throws Exception {
        final String objectId = "digest_bad_test";
        final CreateObjectDescription createObjectDescription = new CreateObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        final FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).build());
        try {
            customDistribution
                .storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @Test(expected = StorageObjectAlreadyExistsException.class)
    public void testObjectAlreadyInOffer() throws Exception {
        final String objectId = "already_in_offer";
        StoredInfoResult storedInfoResult = null;
        final CreateObjectDescription createObjectDescription = new CreateObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        final FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).build());
        try {
            // Store object
            storedInfoResult = customDistribution
                .storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf")).thenThrow(IllegalStateException.class);
    }

    @Test
    public void testStoreData_NotFoundAndWorspaceErrorToTechnicalError() throws Exception {
        final String objectId = "id1";
        final CreateObjectDescription createObjectDescription = new CreateObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenThrow(ContentAddressableStorageNotFoundException.class);
        try {
            customDistribution
                .storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
            fail("Should produce exception");
        } catch (final StorageTechnicalException exc) {
            // Expection
        }

        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenThrow(ContentAddressableStorageServerException.class);
        try {
            customDistribution
                .storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
            fail("Should produce exception");
        } catch (final StorageTechnicalException exc) {
            // Expection
        }

        final FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        IOUtils.closeQuietly(stream);
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).build());
        try {
            customDistribution
                .storeData(TENANT_ID + this, STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
            fail("Should produce exception");
        } catch (final StorageTechnicalException exc) {
            // Expection
        }
    }

    private void checkInvalidArgumentException(String tenantId, String strategyId, String objectId,
        CreateObjectDescription createObjectDescription, DataCategory category)
        throws StorageNotFoundException, StorageTechnicalException, StorageObjectAlreadyExistsException {
        try {
            simpleDistribution.storeData(tenantId, strategyId, objectId, createObjectDescription, category,
                "testRequester");
            fail("Parameter should be considered invalid");
        } catch (final IllegalArgumentException exc) {
            // test OK
        }
    }

    @Test
    public void getContainerInformationOK() throws Exception {
        final JsonNode jsonNode = simpleDistribution.getContainerInformation(TENANT_ID + this, STRATEGY_ID);
        assertNotNull(jsonNode);
    }

    @Test(expected = StorageTechnicalException.class)
    public void getContainerInformationTechnicalException() throws Exception {
        customDistribution.getContainerInformation("daFakeTenant", STRATEGY_ID);
    }

    @Test
    public void testGetContainerByCategoryIllegalArgumentException() throws Exception {
        try {
            simpleDistribution.getContainerByCategory(null, null, null, null, new AsyncResponseJunitTest());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
        try {
            simpleDistribution.getContainerByCategory(TENANT_ID + this, null, null, null, new AsyncResponseJunitTest());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
        try {
            simpleDistribution.getContainerByCategory(TENANT_ID + this, STRATEGY_ID, null, null, new AsyncResponseJunitTest());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
    }

    @Test
    public void testGetContainerByCategoryNotFoundException() throws Exception {
        simpleDistribution.getContainerByCategory(TENANT_ID + this, STRATEGY_ID, "0", DataCategory.OBJECT,
            new AsyncResponseJunitTest());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetStorageContainer() throws Exception {
        simpleDistribution.getStorageContainer(null, null);
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testCreateContainer() throws Exception {
        simpleDistribution.createContainer(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteContainer() throws Exception {
        simpleDistribution.deleteContainer(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerByCategorys() throws Exception {
        simpleDistribution.getContainerObjects(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerByCategoryInformations() throws Exception {
        simpleDistribution.getContainerObjectInformations(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteObject() throws Exception {
        simpleDistribution.deleteObject(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerLogbook() throws Exception {
        simpleDistribution.getContainerLogbook(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerLogbooks() throws Exception {
        simpleDistribution.getContainerLogbooks(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteLogbook() throws Exception {
        simpleDistribution.deleteLogbook(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerUnits() throws Exception {
        simpleDistribution.getContainerUnits(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerUnit() throws Exception {
        simpleDistribution.getContainerUnit(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteUnit() throws Exception {
        simpleDistribution.deleteUnit(null, null, null);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerByCategoryGroups() throws Exception {
        simpleDistribution.getContainerObjectGroups(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerByCategoryGroup() throws Exception {
        simpleDistribution.getContainerObjectGroup(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteObjectGroup() throws Exception {
        simpleDistribution.deleteObjectGroup(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testStatus() throws Exception {
        simpleDistribution.status();
    }
}
