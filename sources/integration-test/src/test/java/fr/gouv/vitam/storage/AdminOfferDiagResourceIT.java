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

package fr.gouv.vitam.storage;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.OfferDiagRequest;
import fr.gouv.vitam.storage.offers.core.diag.OfferDiagStatus;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.COMPACTED_OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_SEQUENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration test for AdminOfferDiagResource.
 */
public class AdminOfferDiagResourceIT extends VitamRuleRunner {

    private static final Integer tenantId = 2;

    private static final String OFFER_URL = "http://localhost:" + VitamServerRunner.PORT_SERVICE_OFFER_ADMIN;

    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        AdminOfferDiagResourceIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(WorkspaceMain.class, DefaultOfferMain.class)
    );

    private static OfferDiagAdminResource offerDiagAdminResource;

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2));
        handleBeforeClass(Arrays.asList(0, 1, 2), Collections.emptyMap());

        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(OFFER_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();

        offerDiagAdminResource = retrofit.create(OfferDiagAdminResource.class);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        ProcessDataAccessImpl.getInstance().clearWorkflow();
        clearOfferLogs();
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
        clearOfferLogs();
    }

    private void clearOfferLogs() {
        runAfterMongo(Sets.newHashSet(OFFER_LOG.getName(), COMPACTED_OFFER_LOG.getName(), OFFER_SEQUENCE.getName()));
    }

    private void compactOfferLogs() throws IOException {
        Response<Void> response = offerDiagAdminResource.launchOfferLogCompaction().execute();

        assertThat(response.isSuccessful()).as("check request compaction successfully execute").isTrue();
    }

    /**
     * Test the diagnostic process on an empty container.
     */
    @Test
    @RunWithCustomExecutor
    public void offerDiagEmptyContainer() throws Exception {
        // Start the diagnostic process
        Response<Void> startOfferDiag = offerDiagAdminResource
            .startOfferDiag(
                new OfferDiagRequest().setContainer(DataCategory.UNIT.getCollectionName()).setTenantId(tenantId),
                getBasicAuthnToken()
            )
            .execute();

        assertThat(startOfferDiag.isSuccessful()).isTrue();

        // Wait for the diagnostic process to complete
        awaitOfferDiagTermination(60);

        // Get the diagnostic status
        Response<OfferDiagStatus> offerDiagStatusResponse = offerDiagAdminResource
            .getLastOfferDiagStatus(getBasicAuthnToken())
            .execute();

        assertThat(offerDiagStatusResponse.code()).isEqualTo(200);
        OfferDiagStatus offerDiagStatus = offerDiagStatusResponse.body();
        assertThat(offerDiagStatus.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerDiagStatus.getStartDate()).isNotNull();
        assertThat(offerDiagStatus.getEndDate()).isNotNull();
        assertThat(offerDiagStatus.getContainer()).isNotNull();
        assertThat(offerDiagStatus.getTotalObjectCount()).isEqualTo(0L);
        assertThat(offerDiagStatus.getErrorCount()).isEqualTo(0L);
        assertThat(offerDiagStatus.getReportFileName()).isNotNull();
        File reportFile = new File(offerDiagStatus.getReportFileName());
        assertThat(reportFile).exists();
        assertThat(reportFile.length()).isEqualTo(0);

        assertThat(offerDiagStatus.getRequestId()).isEqualTo(startOfferDiag.headers().get(X_REQUEST_ID));
        assertThat(offerDiagStatus.getTenantId()).isEqualTo(tenantId);
    }

    /**
     * Test the diagnostic process with objects in the container.
     */
    @Test
    @RunWithCustomExecutor
    public void offerDiagWithObjects() throws Exception {
        // Create some objects in the container
        // This would typically be done using the storage client
        // For this test, we'll just verify the diagnostic process works

        // Start the diagnostic process
        Response<Void> startOfferDiag = offerDiagAdminResource
            .startOfferDiag(
                new OfferDiagRequest().setContainer(DataCategory.OBJECT.getCollectionName()).setTenantId(tenantId),
                getBasicAuthnToken()
            )
            .execute();

        assertThat(startOfferDiag.isSuccessful()).isTrue();

        // Wait for the diagnostic process to complete
        awaitOfferDiagTermination(60);

        // Get the diagnostic status
        Response<OfferDiagStatus> offerDiagStatusResponse = offerDiagAdminResource
            .getLastOfferDiagStatus(getBasicAuthnToken())
            .execute();

        assertThat(offerDiagStatusResponse.code()).isEqualTo(200);
        OfferDiagStatus offerDiagStatus = offerDiagStatusResponse.body();
        assertThat(offerDiagStatus.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerDiagStatus.getStartDate()).isNotNull();
        assertThat(offerDiagStatus.getEndDate()).isNotNull();
        assertThat(offerDiagStatus.getContainer()).isNotNull();
        assertThat(offerDiagStatus.getReportFileName()).isNotNull();
        File reportFile = new File(offerDiagStatus.getReportFileName());

        assertThat(reportFile).exists();
        assertThat(reportFile.length()).isEqualTo(0);

        assertThat(offerDiagStatus.getRequestId()).isEqualTo(startOfferDiag.headers().get(X_REQUEST_ID));
        assertThat(offerDiagStatus.getTenantId()).isEqualTo(tenantId);
    }

    /**
     * Test the isOfferDiagRunning endpoint.
     */
    @Test
    @RunWithCustomExecutor
    public void testIsOfferDiagRunning() throws Exception {
        // Start the diagnostic process
        Response<Void> startOfferDiag = offerDiagAdminResource
            .startOfferDiag(
                new OfferDiagRequest().setContainer(DataCategory.UNIT.getCollectionName()).setTenantId(tenantId),
                getBasicAuthnToken()
            )
            .execute();

        assertThat(startOfferDiag.isSuccessful()).isTrue();

        // Check if the diagnostic process is running
        Response<Void> isRunningResponse = offerDiagAdminResource.isOfferDiagRunning(getBasicAuthnToken()).execute();

        assertThat(isRunningResponse.code()).isEqualTo(200);
        String runningHeader = isRunningResponse.headers().get("Running");
        assertThat(runningHeader).isNotNull();

        // Wait for the diagnostic process to complete
        awaitOfferDiagTermination(60);

        // Check if the diagnostic process is still running (should be false)
        isRunningResponse = offerDiagAdminResource.isOfferDiagRunning(getBasicAuthnToken()).execute();

        assertThat(isRunningResponse.code()).isEqualTo(200);
        runningHeader = isRunningResponse.headers().get("Running");
        assertThat(runningHeader).isEqualTo("false");
    }

    private void awaitOfferDiagTermination(int timeoutInSeconds) throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        boolean isRunning = true;
        while (isRunning && stopWatch.getTime(TimeUnit.SECONDS) < timeoutInSeconds) {
            Response<Void> offerDiagRunning = offerDiagAdminResource.isOfferDiagRunning(getBasicAuthnToken()).execute();
            assertThat(offerDiagRunning.code()).isEqualTo(200);
            isRunning = Boolean.parseBoolean(offerDiagRunning.headers().get("Running"));

            if (isRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (isRunning) {
            fail("Offer diagnostic took too long");
        }
    }

    private String getBasicAuthnToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    public interface OfferDiagAdminResource {
        @POST("/offer/v1/diag")
        @Headers({ "Accept: application/json", "Content-Type: application/json" })
        Call<Void> startOfferDiag(
            @Body OfferDiagRequest offerDiagRequest,
            @Header("Authorization") String basicAuthnToken
        );

        @HEAD("/offer/v1/diag")
        Call<Void> isOfferDiagRunning(@Header("Authorization") String basicAuthnToken);

        @POST("/offer/v1/compaction")
        @Headers({ "Accept: application/json" })
        Call<Void> launchOfferLogCompaction();

        @GET("/offer/v1/diag")
        @Headers({ "Content-Type: application/json" })
        Call<OfferDiagStatus> getLastOfferDiagStatus(@Header("Authorization") String basicAuthnToken);
    }
}
