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
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class PrepareUpdateUnitsTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    private static final int TENANT_ID = 0;
    private static final int DISTRIBUTION_FILE_RANK = 0;

    private PrepareUpdateUnits prepareUpdateUnits;

    @Before
    public void setUp() throws Exception {
        prepareUpdateUnits = new PrepareUpdateUnits(metaDataClientFactory, 5);
    }

    @Test
    @RunWithCustomExecutor
    public void givingQueryThenGenerateUnitsListInFile() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/PrepareUpdateUnits/query.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);

        given(metaDataClient.selectUnits(any())).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/PrepareUpdateUnits/metadataResult.json"))
        );

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK)).willReturn(
            new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath())
        );
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareUpdateUnits.execute(WorkerParametersFactory.newWorkerParameters(), handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        List<String> lines = Files.readAllLines(Paths.get(distributionFile.toURI()));
        assertThat(lines).isNotNull();
        assertThat(lines.size()).isEqualTo(16);
        assertThat(lines).containsOnlyOnce("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdnaaaaba\"}");
        assertThat(lines).containsOnlyOnce("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdnaaaabp\"}");
    }

    @Test
    @RunWithCustomExecutor
    public void givingQueryWhenMetadataExceptionThenReturnFatal() throws Exception {
        // given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryNode = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/PrepareUpdateUnits/query.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryNode);

        given(metaDataClient.selectUnits(any())).willThrow(MetaDataClientServerException.class);

        File distributionFile = tempFolder.newFile();
        given(handlerIO.getOutput(DISTRIBUTION_FILE_RANK)).willReturn(
            new ProcessingUri(UriPrefix.WORKSPACE, distributionFile.getPath())
        );
        given(handlerIO.getNewLocalFile(distributionFile.getPath())).willReturn(distributionFile);

        // when
        ItemStatus itemStatus = prepareUpdateUnits.execute(WorkerParametersFactory.newWorkerParameters(), handlerIO);

        // then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }
}
