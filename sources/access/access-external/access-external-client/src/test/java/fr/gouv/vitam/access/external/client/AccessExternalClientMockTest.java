/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.external.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class AccessExternalClientMockTest {

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
        " \"$filter\": { \"$orderby\": \"#id\" }, " +
        " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
        " }";
    final String queryDsqlForGot =
        "{ \"$query\" : [ { \"$exists\": \"#id\" } ], " +
        " \"$filter\": { \"$orderby\": \"#id\" }, " +
        " \"$projection\" : { } " +
        " }";
    final String ID = "identifier1";
    final String CONTRACT = "contract";
    final int TENANT_ID = 0;
    AccessExternalClient client;

    @Before
    public void givenMockConfExistWhenAccessExternalCreateMockedClientThenReturnOK() {
        AccessExternalClientFactory.changeMode((SecureClientConfiguration) null);
        client = AccessExternalClientFactory.getInstance().getClient();
        assertNotNull(client);
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectUnitsThenReturnResult() throws Exception {
        assertNotNull(
            client.selectUnits(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql)
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectUnitsWithInheritedRulesThenReturnResult() {
        assertThatThrownBy(
            () ->
                client.selectUnitsWithInheritedRules(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    JsonHandler.getFromString(queryDsql)
                )
        ).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectUnitbyIDThenReturnResult() throws Exception {
        assertNotNull(
            client.selectUnitbyId(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql),
                ID
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternalUpdateUnitbyIDThenReturnResult() throws Exception {
        assertNotNull(
            client.updateUnitbyId(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql),
                ID
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectObjectbyIDThenReturnResult() throws Exception {
        assertNotNull(
            client.selectObjectMetadatasByUnitId(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql),
                ID
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectOperationLogbook_ThenRetururnResult() throws Exception {
        assertNotNull(
            client.selectOperations(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsql)
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectOperationbyIdLogbook_ThenRetururnResult() throws Exception {
        assertNotNull(
            client.selectOperationbyId(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                ID,
                JsonHandler.getFromString(queryDsql)
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectUnitLifeCycleByIdLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(
            client.selectUnitLifeCycleById(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                ID,
                JsonHandler.getFromString(queryDsql)
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternal_selectObjectGroupLifeCycleByIdLogbook_ThenRetururnResult()
        throws Exception {
        assertNotNull(
            client.selectObjectGroupLifeCycleById(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                ID,
                JsonHandler.getFromString(queryDsql)
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternalSelectObjectsThenReturnResult() throws Exception {
        assertNotNull(
            client.selectObjects(
                new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                JsonHandler.getFromString(queryDsqlForGot)
            )
        );
    }

    @Test
    public void givenMockConfExistWhenAccessExternalStartEliminationAnalysisThenReturnResult() {
        assertThatThrownBy(
            () ->
                client.startEliminationAnalysis(
                    new VitamContext(TENANT_ID).setAccessContract(CONTRACT),
                    mock(EliminationRequestBody.class)
                )
        ).isInstanceOf(UnsupportedOperationException.class);
    }
}
