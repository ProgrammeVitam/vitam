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
package fr.gouv.vitam.collect.external.external.rest;

import fr.gouv.vitam.collect.internal.client.CollectInternalClientRestMock;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;

public class TransactionExternalResourceTest {
    private static CollectExternalMain application;
    private final static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int portAvailable;

    @BeforeClass
    public static void setUpBeforeMethod() throws VitamApplicationServerException {
        portAvailable = junitHelper.findAvailablePort();
        RestAssured.port = portAvailable;
        RestAssured.basePath = "collect-external/v1";
        application = new CollectExternalMain("collect-external-test.conf",
            BusinessApplicationTest.class, null);
        application.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (application != null && application.getVitamServer() != null &&
                application.getVitamServer() != null) {

                application.stop();
            }
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }

        junitHelper.releasePort(portAvailable);
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public void uploadArchiveUnit_OK() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("{\n" +
                "  \"DescriptionLevel\": \"RecordGrp\",\n" +
                "  \"Title\": \"Bulletins de salaire : mars 2020\"\n" +
                "}")
            .when().log().all()
            .post("/transactions/" + CollectInternalClientRestMock.TRANSACTION_ID + "/units")
            .then().log().all()
            .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void uploadArchiveUnit_with_bad_transaction_id() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("{\n" +
                "  \"DescriptionLevel\": \"RecordGrp\",\n" +
                "  \"Title\": \"Bulletins de salaire : mars 2020\"\n" +
                "}")
            .when().log().all()
            .post("/transactions/BAD_TRANSACTION_ID/units")
            .then().log().all()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }


    @Test
    public void bad_endpoint_match_pattern() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("{\n" +
                "  \"DescriptionLevel\": \"RecordGrp\",\n" +
                "  \"Title\": \"Bulletins de salaire : mars 2020\"\n" +
                "}")
            .when().log().all()
            .post("/transactions/units")
            .then().log().all()
            .statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void bad_endpoint_no_match() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "0")
            .body("{\n" +
                "  \"DescriptionLevel\": \"RecordGrp\",\n" +
                "  \"Title\": \"Bulletins de salaire : mars 2020\"\n" +
                "}")
            .when().log().all()
            .post("/transactions//units")
            .then().log().all()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }
}