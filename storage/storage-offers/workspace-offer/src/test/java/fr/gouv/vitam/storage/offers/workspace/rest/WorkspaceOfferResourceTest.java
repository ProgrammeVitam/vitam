/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.storage.offers.workspace.rest;


import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.fail;

import java.io.File;

import org.hamcrest.Matchers;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusMessage;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.storage.offers.workspace.core.ObjectInit;

/**
 * WorkspaceOfferResource Test
 */
public class WorkspaceOfferResourceTest {

    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String WORKSPACE_OFFER_CONF = "workspace-offer.conf";
    private static File newWorkspaceOfferConf;

    private static VitamServer vitamServer;

    private static final String REST_URI = "/offer/v1";
    private static int serverPort;
    private static JunitHelper junitHelper;
    private static final String OBJECTS_URI = "/objects";
    private static final String OBJECT_ID_URI = "/{id}";
    private static final String STATUS_URI = "/status";


    private static final ObjectMapper OBJECT_MAPPER;

    static {
        
        OBJECT_MAPPER = new ObjectMapper(new JsonFactory());
        OBJECT_MAPPER.disable(SerializationFeature.INDENT_OUTPUT);
    }

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceOfferResourceTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = new JunitHelper();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        final File workspaceOffer = PropertiesUtils.findFile(WORKSPACE_OFFER_CONF);
        final WorkspaceOfferConfiguration realWorkspaceOffer =
            PropertiesUtils.readYaml(workspaceOffer, WorkspaceOfferConfiguration.class);
        newWorkspaceOfferConf = File.createTempFile("test", WORKSPACE_OFFER_CONF, workspaceOffer.getParentFile());
        PropertiesUtils.writeYaml(newWorkspaceOfferConf, realWorkspaceOffer);

        try {
            vitamServer = WorkspaceOfferApplication.startApplication(new String[] {
                newWorkspaceOfferConf.getAbsolutePath(),
                Integer.toString(serverPort)});
            ((BasicVitamServer) vitamServer).start();
        } catch (VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Wokspace Offer Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
    }


    @Test
    public void getObjectsTest() {
        given().when().get(OBJECTS_URI).then().statusCode(501);
    }

    @Test
    public void getObjectTest() {
        given().when().get(OBJECTS_URI + OBJECT_ID_URI, "id1").then().statusCode(501);
    }

    @Test
    public void postObjectsTest() {
        given().contentType(ContentType.JSON).when().post(OBJECTS_URI).then().statusCode(501);
        ObjectInit init = new ObjectInit();
        init.setId(GUIDFactory.newGUID().toString());
        init.setSize(1024);
        init.setType("Unit");
        init.setDigestAlgorithm(DigestType.SHA256);
        given().contentType(ContentType.JSON).body(new ObjectInit()).when().post(OBJECTS_URI).then().statusCode(501);
    }


    @Test
    public void putObjectTest() {
        given().contentType(ContentType.JSON).when().put(OBJECTS_URI + OBJECT_ID_URI, "id1").then().statusCode(501);
    }

    @Test
    public void headObjectTest() {
        given().head(OBJECTS_URI + OBJECT_ID_URI, "id1").then().statusCode(501);
    }

    @Test
    public void deleteObjectTest() {
        given().delete(OBJECTS_URI + OBJECT_ID_URI, "id1").then().statusCode(501);
    }

    @Test
    public void statusTest() {
        given().get(STATUS_URI).then().contentType(ContentType.JSON).statusCode(200);

        try {
            given().get(STATUS_URI).then().contentType(ContentType.JSON).statusCode(200)
                .body(Matchers
                    .equalTo(OBJECT_MAPPER.writeValueAsString(new StatusMessage(ServerIdentity.getInstance()))));
        } catch (JsonProcessingException exc) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }

}
