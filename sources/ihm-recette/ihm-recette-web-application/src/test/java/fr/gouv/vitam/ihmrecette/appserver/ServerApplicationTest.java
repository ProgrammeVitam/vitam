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
import fr.gouv.vitam.common.junit.JunitHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.io.File;

import static io.restassured.RestAssured.given;

public class ServerApplicationTest {

    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-recette";
    private static final String IHM_RECETTE_CONF = "ihm-recette.conf";
    private static final String IHM_RECETTE_NO_PORT_CONF = "ihm-recette-noPort.conf";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"user\", \"credentials\": \"user\"}}";

    @Test
    public void givenFileAlreadyExistsWhenConfigureApplicationOThenRunServer() throws Exception {
        final File conf = PropertiesUtils.findFile(IHM_RECETTE_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test", IHM_RECETTE_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        new IhmRecetteMainWithoutMongo(newConf.getAbsolutePath());
        newConf.delete();
    }

    @Test
    public void givenFileWhenConfigureApplicationThenRunServer() throws Exception {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(IHM_RECETTE_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test", IHM_RECETTE_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        final IhmRecetteMainWithoutMongo application = new IhmRecetteMainWithoutMongo(newConf.getAbsolutePath());
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";
        given()
            .contentType(ContentType.JSON)
            .body(CREDENTIALS)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("login");
        application.stop();
        newConf.delete();
        junitHelper.releasePort(port);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullArgumentWhenConfigureApplicationOThenRunServerWithDefaultParms() throws Exception {
        new IhmRecetteMainWithoutMongo((String) null);
    }

    @Test
    public void givenFileWithoutPortWhenConfigureApplicationThenRunServer() throws Exception {
        final JunitHelper junitHelper = JunitHelper.getInstance();
        final int port = junitHelper.findAvailablePort();
        final File conf = PropertiesUtils.findFile(IHM_RECETTE_NO_PORT_CONF);
        final WebApplicationConfig config = PropertiesUtils.readYaml(conf, WebApplicationConfig.class);
        final File newConf = File.createTempFile("test-noPort", IHM_RECETTE_NO_PORT_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, config);
        final IhmRecetteMainWithoutMongo application = new IhmRecetteMainWithoutMongo(newConf.getAbsolutePath());
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";
        given()
            .contentType(ContentType.JSON)
            .body(CREDENTIALS)
            .expect()
            .statusCode(Status.OK.getStatusCode())
            .when()
            .post("login");
        application.stop();
        newConf.delete();
        junitHelper.releasePort(port);
    }
}
