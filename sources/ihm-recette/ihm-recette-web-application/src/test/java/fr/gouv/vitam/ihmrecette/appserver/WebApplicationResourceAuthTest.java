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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
// FIXME Think about Unit tests
public class WebApplicationResourceAuthTest {

    // Take it from conf file
    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-recette";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"user\", \"credentials\": \"user\"}}";

    private static JunitHelper junitHelper;
    private static int port;
    private static IhmRecetteMainWithoutMongo application;

    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        final File adminConfig = PropertiesUtils.findFile("ihm-recette.conf");
        application = new IhmRecetteMainWithoutMongo(adminConfig.getAbsolutePath());
        application.start();
    }

    @AfterClass
    public static void afterClass() {
        VitamClientFactory.resetConnections();
        try {
            application.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
        junitHelper.releasePort(port);
    }

    @Test
    public void test() {
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";

        Response response = given().contentType(ContentType.JSON).body(CREDENTIALS).post("/login");
        assertThat(response.getCookie("JSESSIONID")).isNotEmpty();
        assertThat(response.getBody().prettyPrint()).contains("tokenCSRF");
    }
}
