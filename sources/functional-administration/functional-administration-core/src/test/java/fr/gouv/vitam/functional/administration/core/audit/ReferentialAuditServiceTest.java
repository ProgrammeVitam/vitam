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
package fr.gouv.vitam.functional.administration.core.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.functional.administration.common.exception.AuditVitamException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.bson.BsonDocument;
import org.bson.Document;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReferentialAuditServiceTest {

    private static final int TENANT_ID = 0;
    private static final String OFFER_ONE_ID = "offer-one";
    private static final String OFFER_TWO_ID = "offer-two";
    private static final String PROFILE_FILE = "1_Profile_1.json";

    private static final String DIGEST = "digest";
    private static final String HASH =
        "5a95a72c714bc8c7d5b6855cf205c7dd33cac566302ab1fc3e41e2534a446746a63d5259db93138b2c9f66881fdcbbde0e38e92d78df1280ba690cf3ee8ffc37";
    private static final String FAKE_HASH = "fakeHash";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(),
        FunctionalAdminCollections.PROFILE.getName()
    );

    @Mock
    StorageClientFactory storageClientFactory;

    @Mock
    StorageClient storageClient;

    @Mock
    FunctionalBackupService functionalBackupService;

    ReferentialAuditService referentialAuditService;
    MongoDbAccess mongoDbAccess;

    @Before
    public void setUp() throws Exception {
        mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        when(storageClient.getOffers(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            List.of(OFFER_ONE_ID, OFFER_TWO_ID)
        );
        referentialAuditService = new ReferentialAuditService(storageClientFactory, functionalBackupService);

        populateDatabase();
    }

    @Test
    public void should_execute_audit_without_error() throws Exception {
        File profileFile = PropertiesUtils.getResourceFile(PROFILE_FILE);

        ArrayNode objects = JsonHandler.getFromJsonNode(
            JsonHandler.getFromFile(profileFile).get(FunctionalBackupService.FIELD_COLLECTION),
            new TypeReference<>() {}
        );

        when(functionalBackupService.getCollectionInJson(any(), anyInt())).thenReturn(objects);

        Iterator<ObjectEntry> objectsEntry = List.of(new ObjectEntry(PROFILE_FILE, 564)).iterator();
        when(
            storageClient.listContainer(VitamConfiguration.getDefaultStrategy(), null, DataCategory.BACKUP)
        ).thenReturn(CloseableIteratorUtils.toCloseableIterator(objectsEntry));

        when(
            storageClient.getInformation(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.BACKUP),
                eq(PROFILE_FILE),
                eq(List.of(OFFER_ONE_ID, OFFER_TWO_ID)),
                anyBoolean()
            )
        ).thenReturn(
            createObjectNode()
                .setAll(
                    Map.of(
                        OFFER_ONE_ID,
                        createObjectNode().put(DIGEST, HASH),
                        OFFER_TWO_ID,
                        createObjectNode().put(DIGEST, HASH)
                    )
                )
        );

        Response responseMock = mock(BuiltResponse.class);
        doReturn(new FileInputStream(profileFile)).when(responseMock).readEntity(eq(InputStream.class));
        when(
            storageClient.getContainerAsync(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(PROFILE_FILE),
                eq(DataCategory.BACKUP),
                any()
            )
        ).thenReturn(responseMock);

        referentialAuditService.runAudit(FunctionalAdminCollections.PROFILE.getName(), TENANT_ID);
    }

    @Test(expected = AuditVitamException.class)
    public void given_unexisted_file_then_execute_audit_withKO() throws Exception {
        File profileFile = PropertiesUtils.getResourceFile(PROFILE_FILE);

        ArrayNode objects = JsonHandler.getFromJsonNode(
            JsonHandler.getFromFile(profileFile).get(FunctionalBackupService.FIELD_COLLECTION),
            new TypeReference<>() {}
        );

        when(functionalBackupService.getCollectionInJson(any(), anyInt())).thenReturn(objects);

        Iterator<ObjectEntry> objectsEntry = Collections.emptyIterator();
        when(
            storageClient.listContainer(VitamConfiguration.getDefaultStrategy(), null, DataCategory.BACKUP)
        ).thenReturn(CloseableIteratorUtils.toCloseableIterator(objectsEntry));

        referentialAuditService.runAudit(FunctionalAdminCollections.PROFILE.getName(), TENANT_ID);
    }

    @Test(expected = AuditVitamException.class)
    public void given_different_hash_should_execute_audit_with_KO() throws Exception {
        File profileFile = PropertiesUtils.getResourceFile(PROFILE_FILE);

        ArrayNode objects = JsonHandler.getFromJsonNode(
            JsonHandler.getFromFile(profileFile).get(FunctionalBackupService.FIELD_COLLECTION),
            new TypeReference<>() {}
        );

        when(functionalBackupService.getCollectionInJson(any(), anyInt())).thenReturn(objects);

        Iterator<ObjectEntry> objectsEntry = List.of(new ObjectEntry(PROFILE_FILE, 564)).iterator();
        when(
            storageClient.listContainer(VitamConfiguration.getDefaultStrategy(), null, DataCategory.BACKUP)
        ).thenReturn(CloseableIteratorUtils.toCloseableIterator(objectsEntry));

        when(
            storageClient.getInformation(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.BACKUP),
                eq(PROFILE_FILE),
                eq(List.of(OFFER_ONE_ID, OFFER_TWO_ID)),
                anyBoolean()
            )
        ).thenReturn(
            createObjectNode()
                .setAll(
                    Map.of(
                        OFFER_ONE_ID,
                        createObjectNode().put(DIGEST, HASH),
                        OFFER_TWO_ID,
                        createObjectNode().put(DIGEST, FAKE_HASH)
                    )
                )
        );

        Response offerOneResponse = mock(BuiltResponse.class);
        doReturn(new FileInputStream(profileFile)).when(offerOneResponse).readEntity(eq(InputStream.class));

        when(
            storageClient.getContainerAsync(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(OFFER_ONE_ID),
                eq(PROFILE_FILE),
                eq(DataCategory.BACKUP),
                any()
            )
        ).thenReturn(offerOneResponse);

        Response offerTwoResponse = mock(BuiltResponse.class);

        String jsonString = JsonHandler.unprettyPrint(createObjectNode());
        doReturn(new ByteArrayInputStream(jsonString.getBytes()))
            .when(offerTwoResponse)
            .readEntity(eq(InputStream.class));

        when(
            storageClient.getContainerAsync(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(OFFER_TWO_ID),
                eq(PROFILE_FILE),
                eq(DataCategory.BACKUP),
                any()
            )
        ).thenReturn(offerTwoResponse);

        referentialAuditService.runAudit(FunctionalAdminCollections.PROFILE.getName(), TENANT_ID);
    }

    @Test(expected = AuditVitamException.class)
    public void given_unexisted_element_in_database_should_execute_audit_with_KO() throws Exception {
        when(functionalBackupService.getCollectionInJson(any(), anyInt())).thenReturn(JsonHandler.createArrayNode());

        Iterator<ObjectEntry> objectsEntry = List.of(new ObjectEntry(PROFILE_FILE, 564)).iterator();
        when(
            storageClient.listContainer(VitamConfiguration.getDefaultStrategy(), null, DataCategory.BACKUP)
        ).thenReturn(CloseableIteratorUtils.toCloseableIterator(objectsEntry));

        when(
            storageClient.getInformation(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.BACKUP),
                eq(PROFILE_FILE),
                eq(List.of(OFFER_ONE_ID, OFFER_TWO_ID)),
                anyBoolean()
            )
        ).thenReturn(
            createObjectNode()
                .setAll(
                    Map.of(
                        OFFER_ONE_ID,
                        createObjectNode().put(DIGEST, HASH),
                        OFFER_TWO_ID,
                        createObjectNode().put(DIGEST, HASH)
                    )
                )
        );

        Response responseMock = mock(BuiltResponse.class);
        doReturn(PropertiesUtils.getResourceAsStream(PROFILE_FILE))
            .when(responseMock)
            .readEntity(eq(InputStream.class));
        when(
            storageClient.getContainerAsync(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(PROFILE_FILE),
                eq(DataCategory.BACKUP),
                any()
            )
        ).thenReturn(responseMock);

        mongoDbAccess
            .getMongoDatabase()
            .getCollection(FunctionalAdminCollections.PROFILE.getName())
            .deleteOne(new BsonDocument());

        referentialAuditService.runAudit(FunctionalAdminCollections.PROFILE.getName(), TENANT_ID);
    }

    @Test(expected = AuditVitamException.class)
    public void given_unexisted_element_in_secondary_offer_should_execute_audit_with_KO() throws Exception {
        File profileFile = PropertiesUtils.getResourceFile(PROFILE_FILE);

        ArrayNode objects = JsonHandler.getFromJsonNode(
            JsonHandler.getFromFile(profileFile).get(FunctionalBackupService.FIELD_COLLECTION),
            new TypeReference<>() {}
        );

        when(functionalBackupService.getCollectionInJson(any(), anyInt())).thenReturn(objects);

        Iterator<ObjectEntry> objectsEntry = List.of(new ObjectEntry(PROFILE_FILE, 564)).iterator();
        when(
            storageClient.listContainer(VitamConfiguration.getDefaultStrategy(), null, DataCategory.BACKUP)
        ).thenReturn(CloseableIteratorUtils.toCloseableIterator(objectsEntry));

        HashMap<String, JsonNode> map = new HashMap<>();
        map.put(OFFER_ONE_ID, createObjectNode().put(DIGEST, HASH));
        map.put(OFFER_TWO_ID, null);
        when(
            storageClient.getInformation(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.BACKUP),
                eq(PROFILE_FILE),
                eq(List.of(OFFER_ONE_ID, OFFER_TWO_ID)),
                anyBoolean()
            )
        ).thenReturn(createObjectNode().setAll(map));

        Response offerOneResponse = mock(BuiltResponse.class);
        doReturn(new FileInputStream(profileFile)).when(offerOneResponse).readEntity(eq(InputStream.class));

        when(
            storageClient.getContainerAsync(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(OFFER_ONE_ID),
                eq(PROFILE_FILE),
                eq(DataCategory.BACKUP),
                any()
            )
        ).thenReturn(offerOneResponse);

        referentialAuditService.runAudit(FunctionalAdminCollections.PROFILE.getName(), TENANT_ID);
    }

    private void populateDatabase() throws Exception {
        JsonNode jsonNode = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(PROFILE_FILE)).get(
            FunctionalBackupService.FIELD_COLLECTION
        );
        List<Document> profilesList = JsonHandler.getFromJsonNode(jsonNode, new TypeReference<>() {});
        mongoDbAccess
            .getMongoDatabase()
            .getCollection(FunctionalAdminCollections.PROFILE.getName())
            .insertMany(profilesList);
    }
}
