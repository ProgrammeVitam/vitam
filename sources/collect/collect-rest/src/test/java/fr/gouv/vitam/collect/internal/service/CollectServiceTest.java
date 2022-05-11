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
package fr.gouv.vitam.collect.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class CollectServiceTest {

    private static final String SAMPLE_ARCHIVE_UNIT = "archive_unit_from_metadata.json";
    private static final String SAMPLE_OBJECT_GROUP = "object_group_from_metadata.json";
    // this guid was generated with tenant = 0
    private static final String ID = "aeaqaaaaaaevelkyaa6teak73hlewtiaaabq";
    private static final String REQUEST_ID = "aeaqaaaaaitxll67abarqaktftcfyniaaaaq";
    private static JunitHelper junitHelper;
    private static int serverPort;
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    private CollectService collectService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private MetaDataClientFactory metaDataClientFactory;
    @Mock
    private MetaDataClient metaDataClient;
    @Mock
    private WorkspaceClientFactory workspaceClientFactory;
    @Mock
    private WorkspaceClient workspaceClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    public JsonNode fromStringToJson(String query) throws InvalidParseOperationException {
        return JsonHandler.getFromString(query);
    }

    @Before
    public void setUp() {
        reset(metaDataClient);
        reset(workspaceClient);

        reset(metaDataClientFactory);
        reset(workspaceClientFactory);

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        collectService =
            new CollectService(transactionService, metaDataClientFactory);
    }

    @Test
    public void givenUnitIdWhenSelectArchiveUnitThenOK()
        throws Exception {
        String unitId = "aeeaaaaaacfm6tqsaawpgamadc4j5baaaaaq";
        when(metaDataClient.selectUnitbyId(anyObject(), any()))
            .thenReturn(JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_ARCHIVE_UNIT)));
        ArchiveUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
        Assertions.assertThat(archiveUnitModel).isNotNull();
        Assertions.assertThat(archiveUnitModel.getId()).isEqualTo(unitId);
    }

    @Test
    public void givenObjectGroupIdWhenSelectObjectGroupThenOK()
        throws Exception {
        String objectGroupId = "aeeaaaaaacfm6tqsaa4kkamaddhfjfyaaaaq";

        when(metaDataClient.getObjectGroupByIdRaw(any())).thenReturn(
            new RequestResponseOK<JsonNode>()
                .addResult(JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_OBJECT_GROUP))));

        JsonNode objectGroupJsonNode = collectService.getObjectGroupByIdInMetaData(objectGroupId);
        Assertions.assertThat(objectGroupJsonNode).isNotNull();
        Assertions.assertThat(objectGroupJsonNode.path("$results").get(0).path("_id").asText())
            .isEqualTo(objectGroupId);
    }


    @Test
    public void givenTransactionIdWhenSelectUnitsThenOK()
        throws Exception {
        String unitId = "aeeaaaaaacfm6tqsaawpgamadc4j5baaaaaq";
        String opi = "aeeaaaaaacfm6tqsaawpgamadc37f5iaaaaq";
        when(metaDataClient.selectUnits(any()))
            .thenReturn(JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_ARCHIVE_UNIT)));
        JsonNode archiveUnitJson = collectService.getUnitsByTransactionIdInMetaData(opi);
        Assertions.assertThat(archiveUnitJson).isNotNull();
        Assertions.assertThat(archiveUnitJson.path("$results").get(0).path("#id").asText()).isEqualTo(unitId);
        Assertions.assertThat(archiveUnitJson.path("$results").get(0).path("#opi").asText()).isEqualTo(opi);
    }

}
