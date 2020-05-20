/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckAttachementActionHandlerTest {

    private static final String OPI_ID = "OPI_ID";
    private static final String GOT_ID = "GOT_ID";
    private static final String UNIT_ID = "UNIT_ID";
    private static final String EXISTING_GOT_JSON = "{\"" + GOT_ID + "\": \"" + UNIT_ID + "\"}";
    private static final String EXISTING_UNIT_JSON =
        "{\"" + IngestWorkflowConstants.EXISTING_UNITS + "\": [\"" + UNIT_ID + "\"] }";


    private MetaDataClientFactory metaDataClientFactory;
    private ProcessingManagementClientFactory processingManagementClientFactory;

    private MetaDataClient metaDataClient;
    private ProcessingManagementClient processingManagementClient;

    private CheckAttachementActionHandler checkAttachementActionHandler;

    @Before()
    public void setUp() throws Exception {
        metaDataClient = mock(MetaDataClient.class);
        metaDataClientFactory = mock(MetaDataClientFactory.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);

        processingManagementClient = mock(ProcessingManagementClient.class);
        processingManagementClientFactory = mock(ProcessingManagementClientFactory.class);
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);

        checkAttachementActionHandler =
            new CheckAttachementActionHandler(metaDataClientFactory, processingManagementClientFactory);
    }

    @Test
    public void given_MD_when_attach_with_unexisting_MD_then_OK() throws Exception {
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(JsonHandler.getFromString("{}"));

        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        assertEquals(itemStatus.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void given_MD_when_attach_with_existing_GOT_with_status_KO_Then_KO() throws Exception {
        //given
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(ArgumentMatchers.anyString())).thenReturn(JsonHandler.getFromString("[]"));
        when(handlerIO
            .getJsonFromWorkspace(CheckAttachementActionHandler.MAPS_EXISITING_GOT_TO_NEW_GOT_FOR_ATTACHMENT_FILE))
            .thenReturn(JsonHandler.getFromString(EXISTING_GOT_JSON));

        JsonNode gotNode = mock(JsonNode.class);
        RequestResponseOK result = new RequestResponseOK();
        result.addAllResults(Collections.singletonList(gotNode));
        when(gotNode.get(MetadataDocument.OPI)).thenReturn(new TextNode(OPI_ID));

        when(metaDataClient.getObjectGroupsByIdsRaw(eq(Collections.singleton(GOT_ID)))).thenReturn(
            result);

        when(processingManagementClient.getOperationProcessStatus(OPI_ID))
            .thenReturn(new ItemStatus().increment(StatusCode.KO));

        //when
        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        //then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void given_MD_when_attach_with_existing_Unit_with_status_KO_Then_KO() throws Exception {
        //given
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(ArgumentMatchers.anyString())).thenReturn(JsonHandler.getFromString("{}"));
        when(handlerIO
            .getJsonFromWorkspace(CheckAttachementActionHandler.MAPS_EXISITING_UNITS_FOR_ATTACHMENT_FILE))
            .thenReturn(JsonHandler.getFromString(EXISTING_UNIT_JSON));

        JsonNode unitNode = mock(JsonNode.class);
        RequestResponseOK result = new RequestResponseOK();
        result.addAllResults(Collections.singletonList(unitNode));
        when(unitNode.get(MetadataDocument.OPI)).thenReturn(new TextNode(OPI_ID));

        when(metaDataClient.getUnitsByIdsRaw(eq(Collections.singleton(UNIT_ID)))).thenReturn(
            result);

        when(processingManagementClient.getOperationProcessStatus(OPI_ID))
            .thenReturn(new ItemStatus().increment(StatusCode.KO));

        //when
        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        //then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void given_MD_when_attach_with_existing_GOT_with_status_OK_Then_OK() throws Exception {
        //given
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(ArgumentMatchers.anyString())).thenReturn(JsonHandler.getFromString("{}"));
        when(handlerIO
            .getJsonFromWorkspace(CheckAttachementActionHandler.MAPS_EXISITING_GOT_TO_NEW_GOT_FOR_ATTACHMENT_FILE))
            .thenReturn(JsonHandler.getFromString(EXISTING_GOT_JSON));

        JsonNode gotNode = mock(JsonNode.class);
        RequestResponseOK result = new RequestResponseOK();
        result.addAllResults(Collections.singletonList(gotNode));
        when(gotNode.get(MetadataDocument.OPI)).thenReturn(new TextNode(OPI_ID));

        when(metaDataClient.getObjectGroupsByIdsRaw(eq(Collections.singleton(GOT_ID)))).thenReturn(
            result);

        when(processingManagementClient.getOperationProcessStatus(OPI_ID))
            .thenReturn(new ItemStatus().increment(StatusCode.OK));

        //when
        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        //then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void given_MD_when_attach_with_existing_Unit_with_status_OK_Then_OK() throws Exception {
        //given
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(ArgumentMatchers.anyString())).thenReturn(JsonHandler.getFromString("{}"));
        when(handlerIO
            .getJsonFromWorkspace(CheckAttachementActionHandler.MAPS_EXISITING_UNITS_FOR_ATTACHMENT_FILE))
            .thenReturn(JsonHandler.getFromString(EXISTING_UNIT_JSON));

        JsonNode unitNode = mock(JsonNode.class);
        RequestResponseOK result = new RequestResponseOK();
        result.addAllResults(Collections.singletonList(unitNode));
        when(unitNode.get(MetadataDocument.OPI)).thenReturn(new TextNode(OPI_ID));

        when(metaDataClient.getUnitsByIdsRaw(eq(Collections.singleton(UNIT_ID)))).thenReturn(
            result);

        when(processingManagementClient.getOperationProcessStatus(OPI_ID))
            .thenReturn(new ItemStatus().increment(StatusCode.OK));

        //when
        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        //then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.OK);
    }
}
