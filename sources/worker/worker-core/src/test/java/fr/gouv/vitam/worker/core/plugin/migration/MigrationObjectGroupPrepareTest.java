package fr.gouv.vitam.worker.core.plugin.migration;


import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.model.ChainedFileModel;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MigrationObjectGroupPrepareTest {


    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock private MetaDataClientFactory metaDataClientFactory;
    @Mock private MetaDataClient metaDataClient;
    @Mock private HandlerIO handlerIO;
    @Mock private WorkerParameters defaultWorkerParameters = mock(WorkerParameters.class);
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
    }

    @Test
    public void should_prepare_Linked_got_files() throws Exception {
        //GIVEN

        given(metaDataClient.selectObjectGroups(any(JsonNode.class))).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/migration/resultObjectGroup.json")));

        File chainedFile0 = tempFolder.newFile();
        given(handlerIO.getNewLocalFile("chainedFile_0.json")).willReturn(chainedFile0);


        File chainedFile1 = tempFolder.newFile();
        given(handlerIO.getNewLocalFile("chainedFile_1.json")).willReturn(chainedFile1);

        MigrationObjectGroupPrepare migrationGotPrepare = new MigrationObjectGroupPrepare(metaDataClientFactory, 2);

        //WHEN
        ItemStatus execute = migrationGotPrepare.execute(defaultWorkerParameters, handlerIO);

        //THEN

        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);

        ChainedFileModel model0 = JsonHandler.getFromFile(chainedFile0, ChainedFileModel.class);
        ChainedFileModel model1 = JsonHandler.getFromFile(chainedFile1, ChainedFileModel.class);

        assertThat(model0.getElements().size()).isEqualTo(2);
        assertThat(model0.getNextFile()).isEqualTo("chainedFile_1.json");

        assertThat(model1.getElements().size()).isEqualTo(1);
        assertThat(model1.getNextFile()).isNull();

    }

}
