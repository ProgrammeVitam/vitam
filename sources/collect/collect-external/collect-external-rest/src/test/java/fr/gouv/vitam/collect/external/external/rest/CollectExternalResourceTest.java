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

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class CollectExternalResourceTest {
    private static CollectExternalMain application;
    private static JunitHelper junitHelper;
    private static int portAvailable;

    @BeforeClass
    public static void setUpBeforeMethod() throws VitamApplicationServerException {
        junitHelper = JunitHelper.getInstance();
        portAvailable = junitHelper.findAvailablePort();
        RestAssured.port = portAvailable;
        RestAssured.basePath = "collect-external/v1";
        application = new CollectExternalMain("collect-external-test.conf",
                BusinessApplicationTest.class, null);
        application.start();
    }

    @Test
    public void shouldNotHaveDuplicatesInApiListResponse() {
        final EndpointInfo[] endpointInfoArray = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, "0")
                .when()
                .options()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(EndpointInfo[].class);
        final List<EndpointInfo> endpointInfoList = Arrays.asList(endpointInfoArray);
        final Set<EndpointInfo> endpointInfoSet = new HashSet<>(endpointInfoList);

        assertEquals(endpointInfoArray.length, endpointInfoSet.size());
    }
}