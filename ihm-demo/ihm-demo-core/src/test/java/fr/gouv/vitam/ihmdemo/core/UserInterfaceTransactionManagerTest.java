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
package fr.gouv.vitam.ihmdemo.core;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.client.AccessClient;
import fr.gouv.vitam.access.client.AccessClientFactory;
import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Tests UserInterfaceTransactionManager class
 *
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AccessClientFactory.class })
public class UserInterfaceTransactionManagerTest {
	private static String SELECT_ID_DSL_QUERY = "{ $roots : [ '1' ] }";
	private static String SEARCH_UNIT_DSL_QUERY = "{ \"$queries\": [$eq : { '#id' : 1 }], \"$filter\": {$orderby : { TransactedDate : 1 } }, "
			+ "\"$projection\": {$fields : {#id : 1, Title : 1, TransactedDate:1 }}}";
	private static String ID_UNIT = "1";
	private static String UNIT_DETAILS = "{_id: '1', Title: 'Archive 1', DescriptionLevel: 'Archive Mock'}";
	private static String SEARCH_RESULT = "{$hint: {'total':'1'}, $result:[{'_id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}";
    private static String UPDATE_FIELD_IMPACTED_RESULT =
        "{$hint: {'total':'1'}, $result:[{'_id': '1', 'Title': 'Archive 1', 'DescriptionLevel': 'Archive Mock'}]}";
    private static String UPDATE_UNIT_DSL_QUERY =
        "{ \"$queries\": [$eq : { '#id' : 1 }], \"$filter\": {$orderby : { TransactedDate : 1 } }, " +
            "\"$actions\": {#id : 1, Title : 1, TransactedDate:1 }}";
	private static JsonNode unitDetails;
	private static JsonNode searchResult;
	private static JsonNode updateResult;

	private static AccessClientFactory accessClientFactory;
	private static AccessClient accessClient;

	@BeforeClass
	public static void setup()
			throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException {
		unitDetails = JsonHandler.getFromString(UNIT_DETAILS);
		searchResult = JsonHandler.getFromString(SEARCH_RESULT);
	    updateResult = JsonHandler.getFromString(UPDATE_FIELD_IMPACTED_RESULT);
		PowerMockito.mockStatic(AccessClientFactory.class);
		accessClientFactory = PowerMockito.mock(AccessClientFactory.class);
		accessClient = org.mockito.Mockito.mock(AccessClient.class);
		PowerMockito.when(AccessClientFactory.getInstance()).thenReturn(accessClientFactory);
		PowerMockito.when(AccessClientFactory.getInstance().getAccessOperationClient()).thenReturn(accessClient);
	}

	@Test
	public void testSuccessSearchUnits()
			throws AccessClientServerException, AccessClientNotFoundException, InvalidParseOperationException {
		when(accessClient.selectUnits(SEARCH_UNIT_DSL_QUERY)).thenReturn(searchResult);

		// Test method
		JsonNode searchResult = UserInterfaceTransactionManager.searchUnits(SEARCH_UNIT_DSL_QUERY);
		assertTrue(searchResult.get("$hint").get("total").textValue().equals("1"));
	}

	@Test
	public void testSuccessGetArchiveUnitDetails()
			throws AccessClientServerException, AccessClientNotFoundException, InvalidParseOperationException {
		when(accessClient.selectUnitbyId(SELECT_ID_DSL_QUERY, ID_UNIT)).thenReturn(unitDetails);

		// Test method
		JsonNode archiveDetails = UserInterfaceTransactionManager.getArchiveUnitDetails(SELECT_ID_DSL_QUERY, ID_UNIT);
		assertTrue(archiveDetails.get("Title").textValue().equals("Archive 1"));
	}
	
    @Test
    public void testSuccessUpdateUnits()
        throws AccessClientServerException, AccessClientNotFoundException, InvalidParseOperationException {
        when(accessClient.updateUnitbyId(UPDATE_UNIT_DSL_QUERY, ID_UNIT)).thenReturn(updateResult);

        // Test method
        JsonNode updateResult = UserInterfaceTransactionManager.updateUnits(UPDATE_UNIT_DSL_QUERY, "1");
        assertTrue(updateResult.get("$hint").get("total").textValue().equals("1"));
    }

}
