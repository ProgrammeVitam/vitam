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
package fr.gouv.vitam.collect.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.collect.external.dto.FileInfoDto;
import fr.gouv.vitam.collect.external.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.FluxService;
import fr.gouv.vitam.collect.internal.service.ProjectService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.BDDMockito.given;

@Ignore
public class TransactionResourceTest {
    private static final String SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME = "init_transaction_response.json";
    private static final String SAMPLE_UPLOAD_ARCHIVE_UNIT_FILENAME = "upload_au_response.json";
    @Mock
    private static WorkspaceClientFactory workspaceClientFactory;
    @Mock
    private static MetaDataClientFactory metaDataClientFactory;
    private static JsonNode sampleInitTransaction;
    private static JsonNode sampleUploadArchiveUnit;
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private TransactionResource transactionResource;
    @Mock
    private TransactionService transactionService;
    @Mock
    private CollectConfiguration collectConfiguration;
    private WorkspaceClient workspaceClient;
    @Mock
    private MetaDataClient metaDataClient;
    @Mock
    private CollectService collectService;
    @Mock
    private ProjectService projectService;
    @Mock
    private SipService sipService;
    @Mock
    private FluxService fluxService;

    @Before
    public void setUp() {
        given(collectConfiguration.getWorkspaceUrl()).willReturn("http://localhost:8082");
        transactionResource = new TransactionResource(transactionService, collectService, sipService, projectService,fluxService);
    }

    @Test
    public void initTransactionTest_OK() throws Exception {
        // Given
        sampleInitTransaction =
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME));
        given(CollectService.createRequestId()).willReturn("082aba2d-817f-4e5f-8fa4-f12ba7d7642f");
        Mockito.doNothing().when(transactionService).createTransaction(Mockito.isA(TransactionDto.class));
        // When
        //        RequestResponseOK result = transactionResource.initTransaction(null);
        // Then
        //        Assertions.assertThat(result.toString()).hasToString(sampleInitTransaction.toString());
    }

    @Test
    public void initTransactionTest_KO() throws Exception {
        // Given
        sampleInitTransaction =
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME));
        given(CollectService.createRequestId()).willReturn("082aba2d-817f-4e5f-8fa4-f12ba7d764");
        Mockito.doNothing().when(transactionService).createTransaction(Mockito.isA(TransactionDto.class));
        // When
        //        RequestResponseOK result = transactionResource.initTransaction(null);
        // Then
        //        Assertions.assertThat(result.toString()).isNotEqualTo(sampleInitTransaction.toString());
    }


    //    @Test
    //    public void upload_OK() throws Exception {
    //        // Given
    //        String TransactionId = "082aba2d-817f-4e5f-8fa4-f12ba7d7642f";
    //        final InputStream inputStreamZip =
    //                PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
    //        sampleInitTransaction = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME));
    //        Optional<CollectModel> collectModel = Optional.of(new CollectModel(TransactionId));
    //        given(collectService.findCollect(TransactionId)).willReturn(collectModel);
    //        TransactionResource transactionResourceSpy = Mockito.spy(transactionResource);
    //        Mockito.doNothing().when(transactionResourceSpy).pushSipStreamToWorkspace(Mockito.any(), Mockito.any());
    //        // When
    //        Response result = transactionResourceSpy.upload(TransactionId, inputStreamZip);
    //        // Then
    //        Assertions.assertThat(result.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    //    }

    //    @Test
    //    public void upload_KO() throws Exception {
    //        // Given
    //        String TransactionId = "082aba2d-817f-4e5f-8fa4-f12ba7d7642f";
    //        final InputStream inputStreamZip =
    //                PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
    //        sampleInitTransaction = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_INIT_TRANSACTION_RESPONSE_FILENAME));
    //        Optional<CollectModel> collectModel = Optional.empty();
    //        given(collectService.findCollect(TransactionId)).willReturn(collectModel);
    //        TransactionResource transactionResourceSpy = Mockito.spy(transactionResource);
    //        Mockito.doNothing().when(transactionResourceSpy).pushSipStreamToWorkspace(Mockito.any(), Mockito.any());
    //        // When
    //        Response result = transactionResourceSpy.upload(TransactionId, inputStreamZip);
    //        // Then
    //        Assertions.assertThat(result.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    //    }



    @Test
    public void uploadAu_OK() throws Exception {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        String resultMetaData = "{\"httpCode\": 200}";
        JsonNode jsonResultMetaData = mapper.readTree(resultMetaData);
        String transactionId = "082aba2d-817f-4e5f-8fa4-f12ba7d7642f";
        sampleUploadArchiveUnit =
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_UPLOAD_ARCHIVE_UNIT_FILENAME));
        Optional<TransactionModel> collectModel =
            Optional.of(new TransactionModel(transactionId, null, null, null, null));
        given(transactionService.findTransaction(transactionId)).willReturn(collectModel);
        //        CollectUnitDto archiveCollectUnitDto = new CollectUnitDto(null,new ArchiveUnitContent("title", "description", "Item"),null, null, null, new ManagementModel());
        TransactionResource transactionResourceSpy = Mockito.spy(transactionResource);
        given(CollectService.createRequestId()).willReturn("082aba2d-817f-4e5f-8fa4-f12ba7d7642f");
        given(collectService.saveArchiveUnitInMetaData(Mockito.any())).willReturn(jsonResultMetaData);
        //Mockito.doReturn(jsonResultMetaData).when(transactionResourceSpy).saveArchiveUnitInMetaData(Mockito.any());
        // When
        //        RequestResponseOK result = transactionResourceSpy.uploadArchiveUnit(TransactionId, archiveCollectUnitDto);
        //        // Then
        //        Assertions.assertThat(result.toString()).hasToString(sampleUploadArchiveUnit.toString());
    }


    @Test
    public void uploadAu_KO() throws Exception {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        String resultMetaData = "{\"httpCode\": 200}";
        JsonNode jsonResultMetaData = mapper.readTree(resultMetaData);
        String transactionId = "082aba2d-817f-4e5f-8fa4-f12ba7d7642f";
        sampleUploadArchiveUnit =
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_UPLOAD_ARCHIVE_UNIT_FILENAME));
        Optional<TransactionModel> collectModel =
            Optional.of(new TransactionModel(transactionId, null, null, null, null));
        given(transactionService.findTransaction(transactionId)).willReturn(collectModel);
        //        CollectUnitDto archiveCollectUnitDto = new CollectUnitDto(null,new ArchiveUnitContent("title", "description", "Item"),null, null, null, new ManagementModel());
        TransactionResource transactionResourceSpy = Mockito.spy(transactionResource);
        given(CollectService.createRequestId()).willReturn("082aba2d-817f-4e5f-8fa4-f12ba7d764");
        //Mockito.doReturn(jsonResultMetaData).when(transactionResourceSpy).saveArchiveUnitInMetaData(Mockito.any());
        // When
        //        RequestResponseOK result = transactionResourceSpy.uploadArchiveUnit(TransactionId, archiveCollectUnitDto);
        //        // Then
        //        Assertions.assertThat(result.toString()).isNotEqualTo(sampleUploadArchiveUnit.toString());
    }

    @Test
    public void uploadGot_OK() throws Exception {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        String lastModified = LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now());
        String sampleUploadGotResponse =
            "{\"httpCode\":200,\"$hits\":{\"total\":1,\"offset\":0,\"limit\":0,\"size\":1},\"$results\":[{\"id\":\"082aba2d-817f-4e5f-8fa4-f12ba7d7642f\",\"fileInfo\":{\"LastModified\":\"" +
                lastModified + "\",\"filename\":\"Pereire.txt\"}}],\"$facetResults\":[],\"$context\":{}}";
        String resultMetaData = "{\"httpCode\": 200}";
        String resultArchiveUnitMetaData =
            "[{\"data\":\"data1\",\"Description\":\"pop\",\"Title\":\"titre exemple\",\"DescriptionLevel\":\"Item\",\"#id\":\"1a9a3e4e-26b6-45eb-b8d1-5fd41cba8a59\",\"#tenant\":1,\"#object\":\"efaef0d4-762d-4c06-884d-6127d906cade\",\"#unitups\":[],\"#min\":1,\"#max\":1,\"#allunitups\":[],\"#management\":{},\"#originating_agencies\":[],\"#version\":2}]";
        JsonNode jsonResultMetaData = mapper.readTree(resultMetaData);
        ArrayNode jsonResultArchiveUnit = (ArrayNode) mapper.readTree(resultArchiveUnitMetaData);
        String transactionId = "082aba2d-817f-4e5f-8fa4-f12ba7d7642f";
        String archiveUnitId = "1a9a3e4e-26b6-45eb-b8d1-5fd41cba8a59";
        sampleUploadArchiveUnit =
            JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_UPLOAD_ARCHIVE_UNIT_FILENAME));
        Optional<TransactionModel> collectModel =
            Optional.of(new TransactionModel(transactionId, null, null, null, null));
        given(transactionService.findTransaction(transactionId)).willReturn(collectModel);

        ObjectGroupDto objectGroupDto = new ObjectGroupDto();
        objectGroupDto.setFileInfo(new FileInfoDto("Pereire.txt", lastModified));
        TransactionResource transactionResourceSpy = Mockito.spy(transactionResource);
        given(CollectService.createRequestId()).willReturn("082aba2d-817f-4e5f-8fa4-f12ba7d7642f");
        //given(transactionService.saveObjectGroupInMetaData(Mockito.any(), Mockito.any())).willReturn(jsonResultMetaData);
        //given(transactionService.getArchiveUnitById(Mockito.any())).willReturn(jsonResultArchiveUnit);
        // When
        //RequestResponseOK result = transactionResourceSpy.uploadObjectGroup(transactionId, archiveUnitId, objectGroupDto);
        // Then
        //Assertions.assertThat(result.toString()).hasToString(sampleUploadGotResponse);
    }


}