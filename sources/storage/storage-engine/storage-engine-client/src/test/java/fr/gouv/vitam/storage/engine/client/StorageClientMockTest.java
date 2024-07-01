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
package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * StorageClientMock test
 */
public class StorageClientMockTest {

    @Before
    public void before() {
        StorageClientFactory.changeMode((ClientConfiguration) null);
    }

    @Test
    public void statusTest() throws VitamApplicationServerException {
        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertNotNull(client);
        client.checkStatus();
    }

    @Test
    public void storageInfos() throws Exception {
        final JsonNode expectedResult = JsonHandler.getFromString(StorageClientMock.MOCK_INFOS_RESULT_ARRAY);
        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertNotNull(client);
        final JsonNode result = client.getStorageInformation("idStrategy");
        assertEquals(result, expectedResult);
    }

    @Test
    public void store() throws VitamClientException {
        final ObjectDescription description = new ObjectDescription();
        description.setWorkspaceContainerGUID("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq");
        description.setWorkspaceObjectURI(
            "SIP/content/e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odt"
        );
        final StoredInfoResult expectedResult = generateStoredInfoResult("guid");

        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertNotNull(client);

        final StoredInfoResult result = client.storeFileFromWorkspace(
            "idStrategy",
            DataCategory.OBJECT,
            "guid",
            description
        );
        assertEquals(result.getId(), expectedResult.getId());
    }

    @Test
    public void checkExists() throws VitamClientException {
        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertNotNull(client);
        Map<String, Boolean> existsResult = client.exists(
            "idStrategy",
            DataCategory.OBJECT,
            "guid",
            Arrays.asList("offerId")
        );
        assertNotNull(existsResult);
        assertEquals(existsResult.size(), 1);
        assertTrue(existsResult.containsKey("offerId"));
        assertEquals(existsResult.get("offerId"), Boolean.TRUE);
        assertFalse(existsResult.containsKey("offerIdFake"));
    }

    @Test
    public void getContainerObjectTest() throws Exception {
        final StorageClient client = StorageClientFactory.getInstance().getClient();
        assertNotNull(client);
        final InputStream stream = client
            .getContainerAsync("strategyId", "guid", DataCategory.OBJECT, AccessLogUtils.getNoLogAccessLog())
            .readEntity(InputStream.class);
        final InputStream stream2 = StreamUtils.toInputStream(StorageClientMock.MOCK_GET_FILE_CONTENT);
        assertNotNull(stream);
        assertTrue(IOUtils.contentEquals(stream, stream2));
    }

    private StoredInfoResult generateStoredInfoResult(String guid) {
        final StoredInfoResult result = new StoredInfoResult();
        result.setId(guid);
        result.setInfo("Creation OK");
        result.setCreationTime(LocalDateUtil.getString(LocalDateUtil.now()));
        result.setLastModifiedTime(LocalDateUtil.getString(LocalDateUtil.now()));
        return result;
    }
}
