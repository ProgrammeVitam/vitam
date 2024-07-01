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
package fr.gouv.vitam.access.internal.serve.filter;

import fr.gouv.vitam.access.internal.serve.exception.MissingAccessContractIdException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class AccessContractIdHeaderHelperTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
    private static final AdminManagementClientFactory adminManagementClientFactory = mock(
        AdminManagementClientFactory.class
    );

    private static final Integer TENANT_ID = 0;
    private static final String CONTRACT_ID = "CONTRACT_ID";
    private static final String CONTRACT_IDENTIFIER = "CONTRACT_IDENTIFIER";

    @Before
    public void setUp() {
        reset(adminManagementClient);
        reset(adminManagementClientFactory);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
    }

    @Test
    @RunWithCustomExecutor
    public void testForResponseKO() throws AdminManagementClientServerException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // Prepare mocks
        RequestResponse<AccessContractModel> mockedResponse = new VitamError("KO");
        mockedResponse.setHttpCode(500);

        when(adminManagementClient.findAccessContracts(any())).thenReturn(mockedResponse);

        // launch test
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID);

        try {
            AccessContractIdHeaderHelper.manageAccessContractFromHeader(requestHeaders, adminManagementClientFactory);
            fail("No exception was thrown");
        } catch (MissingAccessContractIdException e) {
            // Must throw an exception: Technical error
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testForEmptyContracts() throws AdminManagementClientServerException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // Prepare mocks
        RequestResponseOK<AccessContractModel> mockedResponse = new RequestResponseOK<>();
        mockedResponse.setHttpCode(200);

        when(adminManagementClient.findAccessContracts(any())).thenReturn(mockedResponse);

        // launch test
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID);

        try {
            AccessContractIdHeaderHelper.manageAccessContractFromHeader(requestHeaders, adminManagementClientFactory);
            fail("No exception was thrown");
        } catch (MissingAccessContractIdException e) {
            // Must throw an exception: No matching contracts
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testForSomeOriginatingAgency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // Prepare mocks
        AccessContractModel mockedContract = new AccessContractModel();
        mockedContract.setId(CONTRACT_ID);
        mockedContract.setEveryOriginatingAgency(false);
        Set<String> originatingAgencies = new HashSet<>();
        originatingAgencies.add("FRAN_AGENCY_1");
        originatingAgencies.add("FRAN_AGENCY_2");
        mockedContract.setOriginatingAgencies(originatingAgencies);
        RequestResponseOK<AccessContractModel> mockedResponse = new RequestResponseOK<>();
        mockedResponse.setHttpCode(200);
        mockedResponse.addResult(mockedContract);

        when(adminManagementClient.findAccessContracts(any())).thenReturn(mockedResponse);

        // launch test
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_ID);

        AccessContractIdHeaderHelper.manageAccessContractFromHeader(requestHeaders, adminManagementClientFactory);

        // Expect responses
        assertFalse(VitamThreadUtils.getVitamSession().getContract().getEveryOriginatingAgency());
        assertEquals(originatingAgencies, VitamThreadUtils.getVitamSession().getContract().getOriginatingAgencies());
    }

    @Test
    @RunWithCustomExecutor
    public void testForEveryOriginatingAgency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // Prepare mocks
        AccessContractModel mockedContract = new AccessContractModel();
        mockedContract.setId(CONTRACT_ID);
        mockedContract.setIdentifier(CONTRACT_IDENTIFIER);
        mockedContract.setEveryOriginatingAgency(true);
        Set<String> originatingAgencies = new HashSet<>();
        originatingAgencies.add("FRAN_AGENCY_1");
        originatingAgencies.add("FRAN_AGENCY_2");
        mockedContract.setOriginatingAgencies(originatingAgencies);

        Set<String> rootUnits = new HashSet<>();
        rootUnits.add("guid");
        mockedContract.setRootUnits(rootUnits);

        RequestResponseOK<AccessContractModel> mockedResponse = new RequestResponseOK<>();
        mockedResponse.setHttpCode(200);
        mockedResponse.addResult(mockedContract);

        when(adminManagementClient.findAccessContracts(any())).thenReturn(mockedResponse);

        // launch test
        MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.putSingle(GlobalDataRest.X_ACCESS_CONTRAT_ID, CONTRACT_IDENTIFIER);

        AccessContractIdHeaderHelper.manageAccessContractFromHeader(requestHeaders, adminManagementClientFactory);

        // Expect responses
        assertTrue(VitamThreadUtils.getVitamSession().getContract().getEveryOriginatingAgency());
        assertEquals(originatingAgencies, VitamThreadUtils.getVitamSession().getContract().getOriginatingAgencies());
        assertEquals(rootUnits, VitamThreadUtils.getVitamSession().getContract().getRootUnits());
    }
}
