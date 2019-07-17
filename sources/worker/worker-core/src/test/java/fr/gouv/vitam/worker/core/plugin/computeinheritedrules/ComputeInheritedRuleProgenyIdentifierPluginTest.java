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

package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComputeInheritedRuleProgenyIdentifierPluginTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock private MetaDataClientFactory metaDataClientFactory;

    @Mock private MetaDataClient metaDataClient;

    @Mock private BatchReportClientFactory batchReportClientFactory;

    @Mock private BatchReportClient batchReportClient;

    private ComputeInheritedRuleProgenyIdentifierPlugin computeInheritedRuleProgenyIdentifierPlugin;
    private static final TypeReference<JsonLineModel> jsonLineModelTypeReference = new TypeReference<JsonLineModel>() {};

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(batchReportClientFactory.getClient()).thenReturn(batchReportClient);
        computeInheritedRuleProgenyIdentifierPlugin = new ComputeInheritedRuleProgenyIdentifierPlugin(metaDataClientFactory, batchReportClientFactory, 2);
    }

    @Test
    public void should_generate_invalid_distribution_file() throws Exception {
        String unitId = "aeaqaaaaaeehslhxaanxoallxuao7dyaaaaq";
        File unitToInvalidateFile = temporaryFolder.newFile();

        HandlerIO handlerIO = givenHandlerIo(unitId, unitToInvalidateFile);
        List<String> parentsAndProgenyUnitsList = givenMetaDataClient(unitId);
        WorkerParameters workerParameters = givenWorkerParameters();

        computeInheritedRuleProgenyIdentifierPlugin.executeList(workerParameters, handlerIO);

        JsonLineGenericIterator<JsonLineModel> lines = new JsonLineGenericIterator<>(new FileInputStream(unitToInvalidateFile), jsonLineModelTypeReference);
        List<String> unitIdsInResultingFile = lines.stream().map(JsonLineModel::getId).collect(Collectors.toList());

        assertThat(unitIdsInResultingFile.size()).isEqualTo(parentsAndProgenyUnitsList.size());
        assertThat(unitIdsInResultingFile).containsAll(parentsAndProgenyUnitsList);
    }

    private WorkerParameters givenWorkerParameters() {
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        when(workerParameters.getObjectNameList()).thenReturn(Collections.singletonList("aeeaaaaaagehslhxabfh2all2cnh4zyaaaaq"));
        return workerParameters;
    }

    private List<String> givenMetaDataClient(String unitId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException, VitamClientInternalException {
        String daughterUnitId = "aeaqaaaaaeehslhxaanxoallxuao67yaaaba";
        List<String> parentsAndProgenyUnitsList = Arrays.asList(unitId, daughterUnitId);
        List<JsonNode> unitJsonList = parentsAndProgenyUnitsList.stream().map(object -> {
            try {
                return JsonHandler.toJsonNode(object);
            } catch (InvalidParseOperationException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        RequestResponse<JsonNode> results = new RequestResponseOK<JsonNode>().addAllResults(unitJsonList);
        Object response = Response.status(Response.Status.OK).entity(results.setHttpCode(200)).build().getEntity();
        when(metaDataClient.selectUnits(any(JsonNode.class))).thenReturn(JsonHandler.toJsonNode(response)); // return all units (parents + children)
        when(batchReportClient.getUnitsToInvalidate(anyString())).thenReturn(JsonHandler.toJsonNode(response)); // where are not here to test batchReportClient
        return parentsAndProgenyUnitsList;
    }

    private HandlerIO givenHandlerIo(String unitId, File unitToInvalidateFile) throws Exception {
        HandlerIO handlerIO = mock(HandlerIO.class);
        ProcessingUri processingUri = mock(ProcessingUri.class);

        File distributionFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(distributionFile, "{\"id\":\"" + unitId + "\"}", Charset.defaultCharset());

        when(handlerIO.getInput(anyInt())).thenReturn(distributionFile);
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(unitToInvalidateFile);
        when(processingUri.getPath()).thenReturn("");
        when(handlerIO.getOutput(anyInt())).thenReturn(processingUri);

        return handlerIO;
    }
}
