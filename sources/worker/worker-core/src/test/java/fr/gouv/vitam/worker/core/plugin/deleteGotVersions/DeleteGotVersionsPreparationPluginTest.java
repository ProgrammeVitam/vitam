/*
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
 */

package fr.gouv.vitam.worker.core.plugin.deleteGotVersions;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.deleteGotVersions.handlers.DeleteGotVersionsPreparationPlugin;
import fr.gouv.vitam.worker.core.plugin.deleteGotVersions.services.DeleteGotVersionsReportService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class DeleteGotVersionsPreparationPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    DeleteGotVersionsReportService reportService;

    @InjectMocks
    private DeleteGotVersionsPreparationPlugin deleteGotVersionsPreparationPlugin;

    @Mock
    private WorkerParameters params;

    public static final String UNITS_BY_GOT_FILE = "deleteGotVersions/unitsByGot.jsonl";
    public static final String DELETE_GOT_VERSIONS_OBJECT_GROUP_RESULT_JSON =
        "/deleteGotVersions/objectGroupResult.json";
    public static final String DELETE_GOT_VERSIONS_RESULT_REQUEST_JSON = "/deleteGotVersions/resultRequest.json";
    public static final String DELETE_GOT_VERSIONS_REQUEST = "deleteGotVersionsRequest";

    @Before
    public void setUp() throws Exception {

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);

        deleteGotVersionsPreparationPlugin =
            new DeleteGotVersionsPreparationPlugin(metaDataClientFactory, reportService);

        when(metaDataClient.selectUnits(any()))
            .thenReturn(getFromInputStream(getClass().getResourceAsStream(DELETE_GOT_VERSIONS_RESULT_REQUEST_JSON)));

        when(metaDataClient.selectObjectGroups(any())).thenReturn(
            getFromInputStream(getClass().getResourceAsStream(DELETE_GOT_VERSIONS_OBJECT_GROUP_RESULT_JSON)));

        VitamThreadUtils.getVitamSession().setTenantId(0);
    }

    @Test
    @RunWithCustomExecutor
    public void givenInvalidUsageNameInRequestThenReturnKO() throws Exception {
        DeleteGotVersionsRequest deleteGotVersionsRequest =
            new DeleteGotVersionsRequest(new Select().getFinalSelect(), "UsageNameTest", List.of(1, 2));
        when(handlerIO.getJsonFromWorkspace(DELETE_GOT_VERSIONS_REQUEST))
            .thenReturn(toJsonNode(deleteGotVersionsRequest));

        ItemStatus itemStatus = deleteGotVersionsPreparationPlugin.execute(params, handlerIO);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(itemStatus.getData("eventDetailData").toString()).contains("Usage name is unknown.");
    }

    @Test
    @RunWithCustomExecutor
    public void givenInvalidSpecificVersionsInRequestThenReturnKO() throws Exception {
        DeleteGotVersionsRequest deleteGotVersionsRequest =
            new DeleteGotVersionsRequest(new Select().getFinalSelect(), BINARY_MASTER.getName(), null);
        when(handlerIO.getJsonFromWorkspace(DELETE_GOT_VERSIONS_REQUEST))
            .thenReturn(toJsonNode(deleteGotVersionsRequest));

        ItemStatus itemStatus = deleteGotVersionsPreparationPlugin.execute(params, handlerIO);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(itemStatus.getData("eventDetailData").toString()).contains("Specific versions list is empty.");
    }

    @Test
    @RunWithCustomExecutor
    public void givenDuplicatedSpecificVersionsInRequestThenReturnKO() throws Exception {
        DeleteGotVersionsRequest deleteGotVersionsRequest =
            new DeleteGotVersionsRequest(new Select().getFinalSelect(), BINARY_MASTER.getName(), List.of(2, 2));
        when(handlerIO.getJsonFromWorkspace(DELETE_GOT_VERSIONS_REQUEST))
            .thenReturn(toJsonNode(deleteGotVersionsRequest));

        ItemStatus itemStatus = deleteGotVersionsPreparationPlugin.execute(params, handlerIO);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(itemStatus.getData("eventDetailData").toString()).contains("Duplicated versions are detected.");
    }

    @Test
    @RunWithCustomExecutor
    public void givenValidRequestThenReturnOK() throws Exception {
        DeleteGotVersionsRequest deleteGotVersionsRequest =
            new DeleteGotVersionsRequest(new Select().getFinalSelect(), BINARY_MASTER.getName(), List.of(5));
        when(handlerIO.getJsonFromWorkspace(DELETE_GOT_VERSIONS_REQUEST))
            .thenReturn(toJsonNode(deleteGotVersionsRequest));

        final File unitsByGotFile = PropertiesUtils.getResourceFile(UNITS_BY_GOT_FILE);
        when(handlerIO.getFileFromWorkspace("unitsByGot.jsonl")).thenReturn(unitsByGotFile);

        Map<String, File> files = new HashMap<>();
        doAnswer((args) -> {
            File file = temporaryFolder.newFile();
            files.put(args.getArgument(0), file);
            return file;
        }).when(handlerIO).getNewLocalFile(anyString());

        ItemStatus itemStatus = deleteGotVersionsPreparationPlugin.execute(params, handlerIO);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }
}
