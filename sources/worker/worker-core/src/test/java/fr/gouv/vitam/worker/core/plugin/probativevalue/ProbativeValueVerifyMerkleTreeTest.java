package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;

import static fr.gouv.vitam.common.PropertiesUtils.getResourceFile;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ProbativeValueVerifyMerkleTreeTest {

    public @Rule MockitoRule mockitoRule = MockitoJUnit.rule();
    private @Mock LogbookOperationsClientFactory logbookOperationsClientFactory;
    private @Mock LogbookOperationsClient logbookOperationsClient;

    @Before
    public void setUp() throws Exception {
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

    }



    @Test
    public void shouldcheck_lfc_object_group_traceability() throws Exception {

        JsonNode secureLogbook = getFromFile(getResourceFile("certification/logbook.json"));

        File secured_data = PropertiesUtils.getResourceFile("certification/data.txt");
        File merkleFile = PropertiesUtils.getResourceFile("certification/merkleTree.json");
        ProbativeValueVerifyMerkleTree verifier = new ProbativeValueVerifyMerkleTree(
            logbookOperationsClientFactory);
        when(logbookOperationsClient.selectOperationById("aecaaaaaacfpcnnvabc4ialfdxp5jviaaaaq"))
            .thenReturn(secureLogbook);


            verifier.checkMerkleTree(
                "aecaaaaaacfpcnnvabc4ialfdxp5jviaaaaq",secured_data,merkleFile);

        assertThat(verifier.getMerkleJsonRootHash()).isEqualTo(JsonHandler.getFromFile(merkleFile).get("Root").textValue());

    }
}
