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
package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class EliminationAnalysisPreparationHandlerTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Spy
    private EliminationAnalysisService eliminationAnalysisService;

    @InjectMocks
    private EliminationAnalysisPreparationHandler instance;

    @Mock
    private HandlerIO handlerIO;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Map<String, InputStream> writtenFiles = new HashMap<>();
    private WorkerParameters parameters;

    @Before
    public void init() throws Exception {
        File vitamTempFolder = folder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);

        doAnswer(args -> folder.newFile(args.getArgument(0))).when(handlerIO).getNewLocalFile(any());

        doAnswer(args -> {
            writtenFiles.put(
                args.getArgument(0),
                new ByteArrayInputStream(FileUtils.readFileToByteArray(args.getArgument(1)))
            );
            return null;
        })
            .when(handlerIO)
            .transferFileToWorkspace(any(), any(), eq(true), eq(false));

        this.parameters = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectNameList(Collections.emptyList())
            .setCurrentStep("StepName");
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute_OK() throws Exception {
        // Given
        givenValidRequestJson();

        List<JsonNode> unitsWithInheritedRules = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("EliminationAnalysis/PreparationUnitsWithInheritedRules.json"),
            List.class,
            JsonNode.class
        );

        doReturn(new RequestResponseOK<JsonNode>().addAllResults(unitsWithInheritedRules).toJsonNode())
            .when(metaDataClient)
            .selectUnitsWithInheritedRules(any());

        // When
        ItemStatus execute = instance.execute(parameters, handlerIO);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(writtenFiles).hasSize(1);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(writtenFiles.get("units.jsonl")))) {
            JsonNode actual_unit1 = JsonHandler.getFromString(reader.readLine());
            JsonNode actual_unit2 = JsonHandler.getFromString(reader.readLine());
            JsonNode actual_unit4 = JsonHandler.getFromString(reader.readLine());

            assertJsonEquals("EliminationAnalysis/PreparationExpectedDistribution_unit1.json", actual_unit1);
            assertJsonEquals("EliminationAnalysis/PreparationExpectedDistribution_unit2.json", actual_unit2);
            assertJsonEquals("EliminationAnalysis/PreparationExpectedDistribution_unit4.json", actual_unit4);
            assertThat(reader.readLine()).isNull();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute_OnInvalidJsonRequestThenFatal() throws Exception {
        // Given
        doThrow(ContentAddressableStorageNotFoundException.class).when(handlerIO).getInputStreamFromWorkspace(any());

        // When
        ItemStatus execute = instance.execute(parameters, handlerIO);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        verifyNoMoreInteractions(metaDataClient);
        assertThat(writtenFiles).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute_OnInvalidJsonRequestThenKO() throws Exception {
        // Given
        doReturn(new ByteArrayInputStream("INVALID_DATA".getBytes()))
            .when(handlerIO)
            .getInputStreamFromWorkspace("request.json");

        // When
        ItemStatus execute = instance.execute(parameters, handlerIO);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.KO);
        verifyNoMoreInteractions(metaDataClient);
        assertThat(writtenFiles).isEmpty();
    }

    private void givenValidRequestJson()
        throws InvalidCreateOperationException, IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, InvalidParseOperationException {
        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.eq("#id", "test"));

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2018-01-23",
            select.getFinalSelect()
        );

        doReturn(JsonHandler.writeToInpustream(eliminationRequestBody))
            .when(handlerIO)
            .getInputStreamFromWorkspace("request.json");
    }

    private void assertJsonEquals(String resourcesFile, JsonNode actual)
        throws FileNotFoundException, InvalidParseOperationException {
        JsonNode expected = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(resourcesFile));
        JsonAssert.assertJsonEquals(expected, actual);
    }
}
