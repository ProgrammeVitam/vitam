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
package fr.gouv.vitam.ihmdemo.appserver;

import static com.jayway.restassured.RestAssured.given;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({ UserInterfaceTransactionManager.class, DslQueryHelper.class, LogbookClientFactory.class })

public class WebApplicationResourceTest {

	private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
	private static final String DEFAULT_STATIC_CONTENT = "webapp";
	private static final String OPTIONS = "{name: \"myName\"}";
    private static final String UPDATE = "{title: \"myarchive\"}";
	private static final String DEFAULT_HOST = "localhost";
	private static JunitHelper junitHelper;
	private static int port;

	@BeforeClass
	public static void setup() throws Exception {
		junitHelper = new JunitHelper();
		port = junitHelper.findAvailablePort();
		ServerApplication.run(new WebApplicationConfig().setPort(port).setBaseUrl(DEFAULT_WEB_APP_CONTEXT)
				.setServerHost(DEFAULT_HOST).setStaticContent(DEFAULT_STATIC_CONTENT));
		RestAssured.port = port;
		RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		junitHelper.releasePort(port);
	}

	@Before
	public void initStaticMock() {
		PowerMockito.mockStatic(UserInterfaceTransactionManager.class);
		PowerMockito.mockStatic(DslQueryHelper.class);
	}

	@Test
	public void givenEmptyPayloadWhenSearchOperationsThenReturnBadRequest() {
		given().contentType(ContentType.JSON).body("{}").expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
				.post("/logbook/operations");
	}

	@Test
	public void givenNoArchiveUnitWhenSearchOperationsThenReturnOK() {
		given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.OK.getStatusCode()).when()
				.post("/archivesearch/units");
	}

	@Test
	public void testSuccessStatus() {
		given().expect().statusCode(Status.OK.getStatusCode()).when().get("status");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLogbookResultLogbookClientException()
			throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {

		PowerMockito.mockStatic(LogbookClientFactory.class);
		LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
		LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
		PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
		PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

		Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
		String preparedDslQuery = "";
		PowerMockito.when(DslQueryHelper.createLogBookSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		PowerMockito.when(logbookClient.selectOperation(preparedDslQuery)).thenThrow(LogbookClientException.class);
		given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
				.post("/logbook/operations");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLogbookResultRemainingExceptions()
			throws InvalidParseOperationException, InvalidCreateOperationException, LogbookClientException {

		PowerMockito.mockStatic(LogbookClientFactory.class);
		LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
		LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
		PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
		PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

		Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
		String preparedDslQuery = "";
		PowerMockito.when(DslQueryHelper.createLogBookSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		PowerMockito.when(logbookClient.selectOperation(preparedDslQuery)).thenThrow(Exception.class);
		given().contentType(ContentType.JSON).body(OPTIONS).expect()
				.statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/logbook/operations");
	}

	@Test
	public void testSuccessGetLogbookResultById() throws InvalidParseOperationException, LogbookClientException {
		PowerMockito.mockStatic(LogbookClientFactory.class);
		LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
		LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
		PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
		PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

		JsonNode result = JsonHandler.getFromString("{}");

		PowerMockito.when(logbookClient.selectOperationbyId("1")).thenReturn(result);
		given().param("idOperation", "1").expect().statusCode(Status.OK.getStatusCode()).when()
				.post("/logbook/operations/1");
	}

	@Test
	public void testSuccessGetLogbookResult()
			throws InvalidParseOperationException, LogbookClientException, InvalidCreateOperationException {

		PowerMockito.mockStatic(LogbookClientFactory.class);
		LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
		LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
		PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
		PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);

		Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
		String preparedDslQuery = "";
		PowerMockito.when(DslQueryHelper.createLogBookSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		JsonNode result = JsonHandler.getFromString("{}");
		PowerMockito.when(logbookClient.selectOperation(preparedDslQuery)).thenReturn(result);
		given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.OK.getStatusCode()).when()
				.post("/logbook/operations");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetLogbookResultByIdLogbookClientException()
			throws InvalidParseOperationException, LogbookClientException {
		PowerMockito.mockStatic(LogbookClientFactory.class);
		LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
		LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
		PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
		PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);
		PowerMockito.when(logbookClient.selectOperationbyId("1")).thenThrow(LogbookClientException.class);

		given().param("idOperation", "1").expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
				.post("/logbook/operations/1");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetLogbookResultByIdLogbookRemainingException()
			throws InvalidParseOperationException, LogbookClientException {
		PowerMockito.mockStatic(LogbookClientFactory.class);
		LogbookClient logbookClient = PowerMockito.mock(LogbookClient.class);
		LogbookClientFactory logbookFactory = PowerMockito.mock(LogbookClientFactory.class);
		PowerMockito.when(LogbookClientFactory.getInstance()).thenReturn(logbookFactory);
		PowerMockito.when(LogbookClientFactory.getInstance().getLogbookOperationClient()).thenReturn(logbookClient);
		PowerMockito.when(logbookClient.selectOperationbyId("1")).thenThrow(Exception.class);

		given().param("idOperation", "1").expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
				.post("/logbook/operations/1");
	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void testArchiveSearchResultDslQueryHelperExceptions()
			throws InvalidParseOperationException, InvalidCreateOperationException {

		Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);

		// DslqQueryHelper Exceptions : InvalidParseOperationException,
		// InvalidCreateOperationException
		PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap))
				.thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

		given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.BAD_REQUEST.getStatusCode())
				.when().post("/archivesearch/units");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testArchiveSearchResultAccessClientServerException() throws AccessClientServerException,
			AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
		Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
		String preparedDslQuery = "";

		PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		// UserInterfaceTransactionManager Exception 1 :
		// AccessClientServerException
		PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery))
				.thenThrow(AccessClientServerException.class);

		given().contentType(ContentType.JSON).body(OPTIONS).expect()
				.statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/archivesearch/units");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testArchiveSearchResultAccessClientNotFoundException() throws AccessClientServerException,
			AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
		Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
		String preparedDslQuery = "";

		PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		// UserInterfaceTransactionManager Exception 1 :
		// AccessClientServerException
		PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery))
				.thenThrow(AccessClientNotFoundException.class);

		given().contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
				.post("/archivesearch/units");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testArchiveSearchResultRemainingExceptions() throws AccessClientServerException,
			AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
		Map<String, String> searchCriteriaMap = JsonHandler.getMapStringFromString(OPTIONS);
		String preparedDslQuery = "";

		PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		// UserInterfaceTransactionManager Exception 1 :
		// AccessClientServerException
		PowerMockito.when(UserInterfaceTransactionManager.searchUnits(preparedDslQuery)).thenThrow(Exception.class);

		given().contentType(ContentType.JSON).body(OPTIONS).expect()
				.statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when().post("/archivesearch/units");
	}

	@Test
	public void testGetArchiveUnitDetails() {
		given().param("id", "1").expect().statusCode(Status.OK.getStatusCode()).when().get("/archivesearch/unit/1");
	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void testArchiveUnitDetailsDslQueryHelperExceptions()
			throws InvalidParseOperationException, InvalidCreateOperationException {

		Map<String, String> searchCriteriaMap = new HashMap<String, String>();
		searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

		// DslqQueryHelper Exceptions : InvalidParseOperationException,
		// InvalidCreateOperationException
		PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap))
				.thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

		given().param("id", "1").expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
				.get("/archivesearch/unit/1");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testArchiveUnitDetailsAccessClientServerException() throws AccessClientServerException,
			AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
		Map<String, String> searchCriteriaMap = new HashMap<String, String>();
		searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

		String preparedDslQuery = "";

		PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		// UserInterfaceTransactionManager Exception 1 :
		// AccessClientServerException
		PowerMockito.when(UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1"))
				.thenThrow(AccessClientServerException.class);

		given().param("id", "1").expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
				.get("/archivesearch/unit/1");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testArchiveUnitDetailsAccessClientNotFoundException() throws AccessClientServerException,
			AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
		Map<String, String> searchCriteriaMap = new HashMap<String, String>();
		searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

		String preparedDslQuery = "";

		PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		// UserInterfaceTransactionManager Exception 2 :
		// AccessClientNotFoundException
		PowerMockito.when(UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1"))
				.thenThrow(AccessClientNotFoundException.class);

		given().param("id", "1").expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
				.get("/archivesearch/unit/1");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testArchiveUnitDetailsRemainingExceptions() throws AccessClientServerException,
			AccessClientNotFoundException, InvalidParseOperationException, InvalidCreateOperationException {
		Map<String, String> searchCriteriaMap = new HashMap<String, String>();
		searchCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

		String preparedDslQuery = "";

		PowerMockito.when(DslQueryHelper.createSelectDSLQuery(searchCriteriaMap)).thenReturn(preparedDslQuery);

		// All exceptions
		PowerMockito.when(UserInterfaceTransactionManager.getArchiveUnitDetails(preparedDslQuery, "1"))
				.thenThrow(Exception.class);

		given().param("id", "1").expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).when()
				.get("/archivesearch/unit/1");
	}

    /**
     * Update Unit Treatment
     */

    @Test
    public void testUpdateArchiveUnitDetails() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().put("/archiveupdate/units/1");
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testUpdateArchiveUnitDetailsDsl()
        throws InvalidParseOperationException, InvalidCreateOperationException {

        Map<String, String> updateCriteriaMap = new HashMap<String, String>();
        updateCriteriaMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        updateCriteriaMap.put("title", "archive1");

        // DslqQueryHelper Exceptions : InvalidParseOperationException,
        // InvalidCreateOperationException
        PowerMockito.when(DslQueryHelper.createUpdateDSLQuery(updateCriteriaMap))
            .thenThrow(InvalidParseOperationException.class, InvalidCreateOperationException.class);

        given().contentType(ContentType.JSON).body(UPDATE).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .put("/archiveupdate/units/1");
    }

}
