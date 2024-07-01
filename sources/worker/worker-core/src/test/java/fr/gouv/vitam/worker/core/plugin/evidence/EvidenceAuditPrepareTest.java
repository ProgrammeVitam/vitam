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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EvidenceAuditPrepareTest class
 */
public class EvidenceAuditPrepareTest {

    private static String query =
        "{\n" +
        "  \"$roots\" : [ ],\n" +
        "  \"$query\" : [ {\n" +
        "    \"$eq\" : {\n" +
        "      \"Title\" : \"monsip\"\n" +
        "    }\n" +
        "  } ],\n" +
        "  \"$projection\" : {\n" +
        "    \"$fields\" : {\n" +
        "      \"$rules\" : 1\n" +
        "    }\n" +
        "  }\n" +
        "}";
    private static String query2 =
        "{\"$roots\":[],\"$query\":[{\"$eq\":{\"Title\":\"monsip\"},\"$depth\":1000}],\"$filter\":{\"$scrollId\":\"START\",\"$limit\":10000,\"$scrollTimeout\":300000},\"$projection\":{\"$fields\":{\"#id\":1,\"#object\":1}},\"$facets\":[]}";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    public HandlerIO handlerIO;

    @Mock
    private EvidenceService evidenceService;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    private EvidenceAuditPrepare evidenceAuditPrepare;

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        evidenceAuditPrepare = new EvidenceAuditPrepare(metaDataClientFactory, null);
    }

    @Test
    public void should_prepare_evidence_audit() throws Exception {
        WorkerParameters defaultWorkerParameters = mock(WorkerParameters.class);

        when(defaultWorkerParameters.getObjectName()).thenReturn("test");
        JsonNode query = JsonHandler.getFromString(EvidenceAuditPrepareTest.query);
        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(query);

        given(metaDataClient.selectUnits(JsonHandler.getFromString(query2))).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/evidenceAudit/selectResult.json"))
        );

        given(handlerIO.getNewLocalFile("aeaqaaaaaaebta56aaoc4alcdk4hlcqaaaaq")).willReturn(tempFolder.newFile());
        given(handlerIO.getNewLocalFile("aeaqaaaaaaebta56aam5ualcdnzc4wiaaabq")).willReturn(tempFolder.newFile());
        given(handlerIO.getJsonFromWorkspace("evidenceOptions")).willReturn(
            JsonHandler.createObjectNode().put("correctiveOption", false)
        );

        ItemStatus execute = evidenceAuditPrepare.execute(defaultWorkerParameters, handlerIO);
        Assertions.assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }
}
