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
package fr.gouv.vitam.metadata.management.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionRequestItem;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionResponseItem;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.OkHttpClient;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.VitamServerRunner.getOfferPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for the reconstruction services. <br/>
 */
public class BackupAndReconstructionLogbookIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        BackupAndReconstructionLogbookIT.class,
        mongoRule.getMongoDatabase().getName(),
        elasticsearchRule.getClusterName(),
        Sets.newHashSet(
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            StorageMain.class,
            DefaultOfferMain.class
        )
    );

    private static final String ACCESS_CONTRACT =
        "integration-metadata-management/data/access_contract_every_originating_angency.json";

    private static final String LOGBOOK_0_GUID = "aecaaaaaaceeytj5abrzmalbvy426faaaaaq";
    private static final String LOGBOOK_0_EVENT_GUID = "aedqaaaaaceeytj5abrzmalbvy43cpyaaaba";
    private static final String REGISTER_0_JSON = "integration-metadata-management/data/register_0.json";
    private static final String LOGBOOK_1_GUID = "aecaaaaaaceeytj5abrzmalbvy42qsaaaaaq";
    private static final String LOGBOOK_1_EVENT_GUID = "aedqaaaaaceeytj5abrzmalbvy42vhyaaaba";
    private static final String REGISTER_1_JSON = "integration-metadata-management/data/register_1.json";
    private static final String LOGBOOK_2_GUID = "aecaaaaaaceeytj5abrzmalbvybpaeiaaaaq";
    private static final String LOGBOOK_2_EVENT_GUID = "aedqaaaaaceeytj5abrzmalbvybpkaiaaaba";

    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 1;

    public static final String LOGBOOK = "logbook";
    public static final String OFFER_ID = "default";

    private static StorageClient storageClient;
    private static LogbookOperationsClient logbookOperationsClient;
    private static AdminManagementClient adminManagementClient;
    private static LogbookReconstructionService reconstructionService;
    private static OffsetRepository offsetRepository;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        // reconstruct service interface - replace non existing client
        // uncomment timeouts for debug mode
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(VitamServerRunner.LOGBOOK_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
        reconstructionService = retrofit.create(LogbookReconstructionService.class);
        storageClient = StorageClientFactory.getInstance().getClient();
        logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient();
        adminManagementClient = AdminManagementClientFactory.getInstance().getClient();

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        offsetRepository = new OffsetRepository(mongoDbAccess);
    }

    @AfterClass
    public static void afterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(VitamConstants.EVERY_ORIGINATING_AGENCY);
    }

    @After
    public void tearDown() {
        runAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void testBackupAndReconstructOperationOk() throws Exception {
        VitamConfiguration.setEnvironmentName("PREFIX_ENV");
        List<ReconstructionRequestItem> reconstructionItems;
        ReconstructionRequestItem reconstructionItem1;
        ReconstructionRequestItem reconstructionItem2;
        Response<List<ReconstructionResponseItem>> response;
        JsonNode logbookResponse;

        String TENANT_0_PREFIXED = VitamConfiguration.getEnvironmentName() + "_" + TENANT_0;
        String TENANT_1_PREFIXED = VitamConfiguration.getEnvironmentName() + "_" + TENANT_1;

        // 0. Init data
        Path backup0Folder = Paths.get(
            getOfferPath(),
            TENANT_0_PREFIXED + "_" + DataCategory.BACKUP_OPERATION.getFolder()
        );
        Path backup1Folder = Paths.get(
            getOfferPath(),
            TENANT_1_PREFIXED + "_" + DataCategory.BACKUP_OPERATION.getFolder()
        );

        assertThat(java.nio.file.Files.exists(backup0Folder)).isFalse();
        assertThat(java.nio.file.Files.exists(backup1Folder)).isFalse();

        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_0_GUID))).isFalse();
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_1_GUID))).isFalse();
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_2_GUID))).isFalse();

        logbookOperationsClient.create(getParamatersStart(LOGBOOK_0_GUID));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_0_GUID))).isTrue();
        logbookOperationsClient.update(
            getParamatersAppend(LOGBOOK_0_GUID, LOGBOOK_0_EVENT_GUID, REGISTER_0_JSON, StatusCode.OK)
        );
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_0_GUID))).isTrue();

        logbookOperationsClient.create(getParamatersStart(LOGBOOK_1_GUID));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_1_GUID))).isTrue();
        logbookOperationsClient.update(
            getParamatersAppend(LOGBOOK_1_GUID, LOGBOOK_1_EVENT_GUID, REGISTER_1_JSON, StatusCode.WARNING)
        );
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_1_GUID))).isTrue();

        logbookOperationsClient.create(getParamatersStart(LOGBOOK_2_GUID));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_2_GUID))).isTrue();
        logbookOperationsClient.update(getParamatersAppend(LOGBOOK_2_GUID, LOGBOOK_2_EVENT_GUID, null, StatusCode.KO));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_2_GUID))).isTrue();

        // import access contract
        File fileAccessContracts = PropertiesUtils.getResourceFile(ACCESS_CONTRACT);
        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileAccessContracts,
            new TypeReference<List<AccessContractModel>>() {}
        );
        adminManagementClient.importAccessContracts(accessContractModelList);

        mongoRule.getMongoCollection(LogbookCollections.OPERATION.getName()).deleteMany(new Document());

        assertThatCode(() -> {
            logbookOperationsClient.selectOperationById(LOGBOOK_0_GUID);
        }).isInstanceOf(LogbookClientNotFoundException.class);

        RequestResponse<OfferLog> offerLogResponse1 = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.BACKUP_OPERATION,
            0L,
            10,
            Order.ASC
        );
        assertThat(offerLogResponse1).isNotNull();
        assertThat(offerLogResponse1.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().size()).isEqualTo(9);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getSequence()).isEqualTo(1L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getContainer()).isEqualTo(
            TENANT_0_PREFIXED + "_" + DataCategory.BACKUP_OPERATION.getFolder()
        );
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getFileName()).isEqualTo(
            LOGBOOK_0_GUID
        );
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getSequence()).isEqualTo(2L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getContainer()).isEqualTo(
            TENANT_0_PREFIXED + "_" + DataCategory.BACKUP_OPERATION.getFolder()
        );
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getFileName()).isEqualTo(
            LOGBOOK_0_GUID
        );

        RequestResponse<OfferLog> offerLogResponse2 = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.BACKUP_OPERATION,
            1L,
            10,
            Order.DESC
        );
        assertThat(offerLogResponse2).isNotNull();
        assertThat(offerLogResponse2.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().get(0).getSequence()).isEqualTo(1L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().get(0).getFileName()).isEqualTo(
            LOGBOOK_0_GUID
        );

        RequestResponse<OfferLog> offerLogResponse3 = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            OFFER_ID,
            DataCategory.BACKUP_OPERATION,
            null,
            10,
            Order.DESC
        );
        assertThat(offerLogResponse3).isNotNull();
        assertThat(offerLogResponse3.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse3).getResults().size()).isEqualTo(9);

        // 1. reconstruct operations
        reconstructionItems = new ArrayList<>();
        reconstructionItem1 = new ReconstructionRequestItem();
        reconstructionItem1.setLimit(2);
        reconstructionItem1.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem1);
        response = reconstructionService.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(response.body().get(0).getTenant()).isEqualTo(TENANT_0);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).isEqualTo(
            2L
        );

        logbookResponse = logbookOperationsClient.selectOperationById(LOGBOOK_0_GUID);
        RequestResponseOK<JsonNode> logbookOK = new RequestResponseOK<JsonNode>().getFromJsonNode(logbookResponse);
        assertThat((logbookOK).getResults().size()).isEqualTo(1);
        assertThat((logbookOK).getFirstResult().get("_id").asText()).isEqualTo(LOGBOOK_0_GUID);
        assertThat((logbookOK).getFirstResult().get("evId").asText()).isEqualTo(LOGBOOK_0_GUID);
        assertThat((logbookOK).getFirstResult().get("_v").asInt()).isEqualTo(1);

        // 2. relaunch reconstruct operations
        reconstructionItems = new ArrayList<>();
        reconstructionItems.add(reconstructionItem1);
        offsetRepository.createOrUpdateOffset(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK, 0);

        response = reconstructionService.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(response.body().get(0).getTenant()).isEqualTo(TENANT_0);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).isEqualTo(
            2L
        );

        // 3. reconstruct next operation
        reconstructionItems = new ArrayList<>();
        offsetRepository.createOrUpdateOffset(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK, 4L);

        reconstructionItem2 = new ReconstructionRequestItem();
        reconstructionItem2.setLimit(2);
        reconstructionItem2.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem2);
        response = reconstructionService.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).isEqualTo(
            6L
        );
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        logbookResponse = logbookOperationsClient.selectOperationById(LOGBOOK_0_GUID);
        assertThat(logbookResponse.get("httpCode").asInt()).isEqualTo(200);
        assertThatCode(() -> {
            JsonNode localeResponse = logbookOperationsClient.selectOperationById(LOGBOOK_1_GUID);
            System.out.println(JsonHandler.prettyPrint(localeResponse));
        }).isInstanceOf(LogbookClientNotFoundException.class);
        logbookResponse = logbookOperationsClient.selectOperationById(LOGBOOK_2_GUID);
        assertThat(logbookResponse.get("httpCode").asInt()).isEqualTo(200);

        // 4. reconstruct nothing for logbook operation
        reconstructionItems = new ArrayList<>();
        reconstructionItems.add(reconstructionItem1);
        offsetRepository.createOrUpdateOffset(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK, 6L);

        response = reconstructionService.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).isEqualTo(
            9L
        );
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        // 5. reconstruct on unused tenants
        reconstructionItems = new ArrayList<>();
        offsetRepository.createOrUpdateOffset(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK, 0L);
        reconstructionItem1.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem1);

        response = reconstructionService.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).isEqualTo(
            0L
        );
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        // 5. reconstruct all operations
        reconstructionItems = new ArrayList<>();
        offsetRepository.createOrUpdateOffset(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK, 0L);
        reconstructionItem1.setTenant(TENANT_0);
        reconstructionItem1.setLimit(15);
        reconstructionItems.add(reconstructionItem1);

        response = reconstructionService.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).isEqualTo(
            10L
        );
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        VitamConfiguration.setEnvironmentName("");
    }

    private LogbookOperationParameters getParamatersStart(String eip) throws InvalidGuidOperationException {
        return LogbookParameterHelper.newLogbookOperationParameters(
            GUIDReader.getGUID(eip),
            "eventType",
            GUIDReader.getGUID(eip),
            LogbookTypeProcess.INGEST,
            StatusCode.STARTED,
            "start ingest",
            GUIDReader.getGUID(eip)
        );
    }

    private LogbookOperationParameters getParamatersAppend(
        String eip,
        String eiEvent,
        String evDetDataFile,
        StatusCode statusCode
    ) throws InvalidGuidOperationException, InvalidParseOperationException, FileNotFoundException {
        LogbookOperationParameters params = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDReader.getGUID(eiEvent),
            "ACCESSION_REGISTRATION",
            GUIDReader.getGUID(eip),
            LogbookTypeProcess.INGEST,
            statusCode,
            "end ingest",
            GUIDReader.getGUID(eip)
        );
        if (evDetDataFile != null) {
            params.putParameterValue(
                LogbookParameterName.eventDetailData,
                JsonHandler.unprettyPrint(
                    JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(evDetDataFile))
                )
            );
        }
        return params;
    }

    public interface LogbookReconstructionService {
        @POST("/logbook/v1/reconstruction/operations")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<List<ReconstructionResponseItem>> reconstructCollection(
            @Body List<ReconstructionRequestItem> reconstructionItems
        );
    }
}
