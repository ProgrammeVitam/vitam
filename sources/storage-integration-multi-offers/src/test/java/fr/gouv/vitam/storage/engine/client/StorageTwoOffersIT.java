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
package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.util.Lists;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;


import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StorageTwoOffersIT class
 */
public class StorageTwoOffersIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(StorageTwoOffersIT.class);
    static final String WORKSPACE_CONF = "storage-test/workspace.conf";
    static final String DEFAULT_OFFER_CONF = "storage-test/storage-default-offer-ssl.conf";
    static final String DEFAULT_SECOND_CONF = "storage-test/storage-default-offer2-ssl.conf";
    static final String STORAGE_CONF = "storage-test/storage-engine.conf";
    static final int PORT_SERVICE_WORKSPACE = 8987;
    static final int PORT_SERVICE_STORAGE = 8583;
    static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;

    private static final int TENANT_0 = 0;
    private static final String DIGEST = "digest";
    private static final String SECOND_OFFER_ID = "default2";
    private static final String OFFER_ID = "default";
    private static final String STRATEGY_ID = "default";

    static WorkspaceMain workspaceMain;
    static StorageMain storageMain;
    static StorageClient storageClient;
    static DefaultOfferMain firstOfferApplication;
    static WorkspaceClient workspaceClient;
    static final String STORAGE_CONF_FILE_NAME = "default-storage.conf";
    private static final String OFFER_FOLDER = "offer";
    private static final String SECOND_FOLDER = "offer2";



    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(VitamCollection.getMongoClientOptions(), "Vitam-Test",
        OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        SetupStorageAndOffers.setupStorageAndTwoOffer();
    }

    private void storeObjectInAllOffers(String id, DataCategory category, InputStream inputStream) throws Exception {
        final ObjectDescription description = new ObjectDescription();
        description.setWorkspaceContainerGUID(id);
        description.setWorkspaceObjectURI(id);
        workspaceClient.createContainer(id);
        workspaceClient.putObject(id, id, inputStream);
        StreamUtils.closeSilently(inputStream);
        storageClient.storeFileFromWorkspace(STRATEGY_ID, category, id, description);

    }

    @Test
    @RunWithCustomExecutor
    public void checkStoreInOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        //Given
        String id = GUIDFactory.newGUID().getId();
        //storeObjectInAllOffers(id, OBJECT, new ByteArrayInputStream(id.getBytes()));
        ArrayList<String> offerIds = Lists.newArrayList(OFFER_ID, SECOND_OFFER_ID);
        storageClient.create(id,OBJECT,new ByteArrayInputStream(id.getBytes()), (long) id.getBytes().length,  offerIds);
        //When
        JsonNode information =
            storageClient.getInformation(STRATEGY_ID, OBJECT, id, newArrayList(OFFER_ID,
                SECOND_OFFER_ID));
        //Then
        assertThat(information.get(OFFER_ID).get(DIGEST)).isNotNull();
        assertThat(information.get(SECOND_OFFER_ID).get(DIGEST)).isNotNull();
        assertThat(information.get(OFFER_ID).get(DIGEST)).isEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));
    }

    @Test
    @RunWithCustomExecutor
    public void testCopyFromOneOfferToAnother() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        //GIVEN
        String id = GUIDFactory.newGUID().getId();
        String id2 = GUIDFactory.newGUID().getId();
        //WHEN
        storeObjectInAllOffers(id, OBJECT, new ByteArrayInputStream(id.getBytes()));
        storeObjectInAllOffers(id2, OBJECT, new ByteArrayInputStream(id2.getBytes()));

        JsonNode information;
        information = storageClient.getInformation(STRATEGY_ID, OBJECT, id, newArrayList(OFFER_ID, SECOND_OFFER_ID));
        assertThat(information.get(OFFER_ID).get(DIGEST)).isEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));

        alterFileInSecondOffer(id);
        //verify that offer2 is modified
        information = storageClient.getInformation(STRATEGY_ID, OBJECT, id, newArrayList(OFFER_ID, SECOND_OFFER_ID));
        assertThat(information.get(OFFER_ID).get(DIGEST)).isNotEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));

        // correct the offer 2
        storageClient.copyObjectToOneOfferAnother(id, DataCategory.OBJECT, OFFER_ID, SECOND_OFFER_ID);

        // verify That the copy has been correctly done
        information = storageClient.getInformation(STRATEGY_ID, OBJECT, id, newArrayList(OFFER_ID, SECOND_OFFER_ID));
        assertThat(information.get(OFFER_ID).get(DIGEST)).isEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));

    }

    // write directly in file of second offer
    private void alterFileInSecondOffer(String id) throws IOException {
        String path = SECOND_FOLDER + File.separator + "0_object" + File.separator + id;
        Writer writer = new BufferedWriter(new FileWriter(path));
        writer.write(id + "test");
    }

    @Test
    @RunWithCustomExecutor
    public void whenDeleteInOneOfferVerifyObjectDoNotExist() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        //GIVEN
        String object = GUIDFactory.newGUID().getId();
        String object2 = GUIDFactory.newGUID().getId();
        storeObjectInAllOffers(object, OBJECT, new ByteArrayInputStream(object.getBytes()));
        storeObjectInAllOffers(object2, OBJECT, new ByteArrayInputStream(object2.getBytes()));

        JsonNode informationObject1 =
            storageClient.getInformation(STRATEGY_ID, OBJECT, object, newArrayList(OFFER_ID,
                SECOND_OFFER_ID));
        JsonNode informationObject2 =
            storageClient.getInformation(STRATEGY_ID, OBJECT, object2, newArrayList(OFFER_ID,
                SECOND_OFFER_ID));

        //just verify the  object  stored and equal
        assertThat(informationObject1.get(OFFER_ID).get(DIGEST))
            .isEqualTo(informationObject1.get(SECOND_OFFER_ID).get(DIGEST));

        String digestObject1SecondOffer = informationObject1.get(SECOND_OFFER_ID).get(DIGEST).textValue();
        String digestObject2SecondOffer = informationObject2.get(SECOND_OFFER_ID).get(DIGEST).textValue();


        //WHEN
        //delete object1 in second offer
        storageClient
            .delete(STRATEGY_ID, DataCategory.OBJECT, object, digestObject1SecondOffer, singletonList(SECOND_OFFER_ID));

        storageClient
            .delete(STRATEGY_ID, DataCategory.OBJECT, object2, digestObject2SecondOffer, newArrayList(OFFER_ID, SECOND_OFFER_ID));


        //THEN
        //delete object2 in the two offers
        informationObject2 =
            storageClient.getInformation(STRATEGY_ID, OBJECT, object2, newArrayList(OFFER_ID, SECOND_OFFER_ID));

        informationObject1 =
            storageClient.getInformation(STRATEGY_ID, OBJECT, object, newArrayList(OFFER_ID,
                SECOND_OFFER_ID));
        //verify Object2 is  deleted in the two offers
        assertThat(informationObject2.get(SECOND_OFFER_ID)).isNull();
        assertThat(informationObject2.get(OFFER_ID)).isNull();

        //verify Object1 is  deleted in only offer 2
        assertThat(informationObject1.get(SECOND_OFFER_ID)).isNull();
        assertThat(informationObject1.get(OFFER_ID)).isNotNull();
    }

    @AfterClass
    public static void afterClass() throws Exception {

        SetupStorageAndOffers.close();

        cleanOffer(new File(OFFER_FOLDER));
        cleanOffer(new File(SECOND_FOLDER));
    }

    private static void cleanOffer(File folder) {
        if (folder.exists()) {
            try {
                cleanDirectory(folder);
                deleteDirectory(folder);
            } catch (Exception e) {
                LOGGER.error("ERROR: Exception has been thrown when cleaning offer:", e);
            }

        }
    }
}
