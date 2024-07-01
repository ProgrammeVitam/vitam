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
package fr.gouv.vitam.ihmrecette.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.xsrf.filter.XSRFFilter;
import fr.gouv.vitam.common.xsrf.filter.XSRFHelper;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response.Status;
import java.io.File;

import static fr.gouv.vitam.ihmrecette.appserver.WebApplicationResource.DEFAULT_CONTRACT_NAME;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

public class WebApplicationResourceTest {

    // take it from conf file
    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-recette";
    private static final String TRACEABILITY_URI = "/operations/traceability";
    private static final String TRACEABILITY_UNIT_LFC_URI = "/lifecycles/units/traceability";
    private static final String TRACEABILITY_OBJECTGROUP_LFC_URI = "/lifecycles/objectgroups/traceability";

    private static final String FAKE_OPERATION_ID = "1";
    private static JsonNode sampleLogbookOperation;
    private static final String SAMPLE_LOGBOOKOPERATION_FILENAME = "logbookoperation_sample.json";
    private static JunitHelper junitHelper;
    private static int port;
    private static final Cookie COOKIE = new Cookie.Builder("JSESSIONID", "testId").build();

    final int TENANT_ID = 0;
    static final String tokenCSRF = XSRFHelper.generateCSRFToken();

    private static IhmRecetteMainWithoutMongo application;
    private UserInterfaceTransactionManager userInterfaceTransactionManager;

    private static File adminConfigFile;

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        final File adminConfig = PropertiesUtils.findFile("ihm-recette.conf");
        final WebApplicationConfig realAdminConfig = PropertiesUtils.readYaml(adminConfig, WebApplicationConfig.class);
        realAdminConfig.setSipDirectory(Thread.currentThread().getContextClassLoader().getResource("sip").getPath());
        realAdminConfig.setAuthentication(false);
        realAdminConfig.setEnableSession(true);
        realAdminConfig.setEnableXsrFilter(true);
        adminConfigFile = File.createTempFile("test", "ihm-recette.conf", adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";

        XSRFFilter.addToken("testId", tokenCSRF);

        sampleLogbookOperation = JsonHandler.getFromFile(PropertiesUtils.findFile(SAMPLE_LOGBOOKOPERATION_FILENAME));

        try {
            application = new IhmRecetteMainWithoutMongo(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the Logbook Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            application.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
        junitHelper.releasePort(port);
    }

    @Before
    public void initStaticMock() {
        userInterfaceTransactionManager = ServerApplicationWithoutMongo.getUserInterfaceTransactionManager();
        Mockito.reset(userInterfaceTransactionManager);
    }

    @Test
    public void testGetLogbookStatisticsWithSuccess() throws Exception {
        VitamContext context = new VitamContext(TENANT_ID);
        context.setAccessContract(DEFAULT_CONTRACT_NAME).setApplicationSessionId(getAppSessionId());
        when(userInterfaceTransactionManager.selectOperationbyId(FAKE_OPERATION_ID, context)).thenReturn(
            RequestResponseOK.getFromJsonNode(sampleLogbookOperation, LogbookOperation.class)
        );
        given()
            .param("id_op", FAKE_OPERATION_ID)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/stat/" + FAKE_OPERATION_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetLogbookStatisticsWithInternalServerErrorWhenInvalidParseOperationException() throws Exception {
        VitamContext context = new VitamContext(TENANT_ID);
        context.setAccessContract(DEFAULT_CONTRACT_NAME).setApplicationSessionId(getAppSessionId());
        when(userInterfaceTransactionManager.selectOperationbyId(FAKE_OPERATION_ID, context)).thenThrow(
            RuntimeException.class
        );
        given()
            .param("id_op", FAKE_OPERATION_ID)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/stat/" + FAKE_OPERATION_ID);
    }

    @Test
    public void testMessagesLogbook() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().get("/messages/logbook");
    }

    @Test
    public void testSecureMode() {
        given()
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .get("/securemode")
            .then()
            .body(equalTo("[\"x509\"]"));
    }

    @Test
    public final void testTraceabilityEndpointIsWorking() {
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .cookie(COOKIE)
            .body("")
            .post(TRACEABILITY_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testTraceabilityUnitLfcEndpointIsWorking() {
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .cookie(COOKIE)
            .body("")
            .post(TRACEABILITY_UNIT_LFC_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testTraceabilityObjectGroupLfcEndpointIsWorking() {
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .cookie(COOKIE)
            .body("")
            .post(TRACEABILITY_OBJECTGROUP_LFC_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    private static String getAppSessionId() {
        return "MyApplicationId-ChangeIt";
    }
}
