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
package fr.gouv.vitam.access.external.rest;

import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.server.application.resources.VitamStatusService;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminManagementExternalResourceManagementContractTest {

    public @Rule MockitoRule mockitoJUnit = MockitoJUnit.rule();

    @Mock
    private AdminManagementClientFactory managementClientFactory;

    @Mock
    private IngestInternalClientFactory ingestInternalClientFactory;

    @Mock
    private AccessInternalClientFactory accessInternalClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private IngestInternalClient ingestInternalClient;

    @Mock
    private AccessInternalClient accessInternalClient;

    private AdminManagementExternalResource externalResource;

    @Before
    public void setUp() {
        when(managementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(ingestInternalClientFactory.getClient()).thenReturn(ingestInternalClient);
        when(accessInternalClientFactory.getClient()).thenReturn(accessInternalClient);

        SecureEndpointRegistry registry = mock(SecureEndpointRegistry.class);
        VitamStatusService statusService = mock(VitamStatusService.class);
        externalResource = new AdminManagementExternalResource(
            statusService,
            registry,
            managementClientFactory,
            ingestInternalClientFactory,
            accessInternalClientFactory
        );
    }

    @Test
    public void shouldImportManagementContracts()
        throws AdminManagementClientServerException, InvalidParseOperationException, FileNotFoundException {
        // Given
        when(adminManagementClient.importManagementContracts(any())).thenReturn(Status.OK);
        // when
        Response response = externalResource.importManagementContracts(
            getFromFile(PropertiesUtils.getResourceFile("contracts_management_ok.json"))
        );
        // then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldCheckManagementContractImportRequest() {
        assertThatIllegalArgumentException().isThrownBy(() -> externalResource.importManagementContracts(null));
    }

    @Test
    public void shouldUpdateManagementContract()
        throws AdminManagementClientServerException, InvalidParseOperationException, FileNotFoundException, ReferentialNotFoundException, InvalidCreateOperationException {
        // Given
        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", "mcContract1"));
        final SetAction setActionPermission = UpdateActionHelper.set("Description", "new description");
        update.addActions(setActionPermission);
        when(adminManagementClient.updateManagementContract(any(), any())).thenReturn(
            new RequestResponseOK<ManagementContractModel>()
        );
        // when
        Response response = externalResource.updateManagementContract("id", update.getFinalUpdateById());
        // then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldReturnNotFoundUpdateManagementContract()
        throws AdminManagementClientServerException, InvalidParseOperationException, FileNotFoundException, ReferentialNotFoundException, InvalidCreateOperationException {
        // Given
        when(adminManagementClient.updateManagementContract(any(), any())).thenThrow(
            new ReferentialNotFoundException("not found")
        );
        // when
        Response response = externalResource.updateManagementContract("id", new Update().getFinalUpdateById());
        // then
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    public void shouldGetManagementContract() throws Exception {
        // Given
        ManagementContractModel managementContractModel = new ManagementContractModel();
        managementContractModel.setName("mcContract1");
        RequestResponse<ManagementContractModel> requestResponse = new RequestResponseOK<
            ManagementContractModel
        >().addResult(managementContractModel);
        when(adminManagementClient.findManagementContractsByID("id")).thenReturn(requestResponse);
        // when
        Response response = externalResource.findManagementContractsByID("id");
        // Then
        @SuppressWarnings("unchecked")
        RequestResponse<ManagementContractModel> entity = (RequestResponse<
                ManagementContractModel
            >) response.getEntity();
        assertThat(toJsonNode(entity).get("$results").get(0).get("Name").textValue()).isEqualTo(
            managementContractModel.getName()
        );
    }

    @Test
    public void shouldReturnNotFoundGetManagementContract() throws Exception {
        // Given
        when(adminManagementClient.findManagementContractsByID("id")).thenThrow(
            new ReferentialNotFoundException("not found")
        );
        // when
        Response response = externalResource.findManagementContractsByID("id");
        // Then
        @SuppressWarnings("unchecked")
        RequestResponse<ManagementContractModel> entity = JsonHandler.getFromString(
            (String) response.getEntity(),
            RequestResponseOK.class
        );
        assertThat(entity.getHttpCode()).isEqualTo(404);
    }

    @Test
    public void shouldFindManagementContract() throws Exception {
        // Given
        ManagementContractModel managementContractModel = new ManagementContractModel();
        managementContractModel.setName("mcContract1");
        RequestResponse<ManagementContractModel> requestResponse = new RequestResponseOK<
            ManagementContractModel
        >().addResult(managementContractModel);
        when(adminManagementClient.findManagementContracts(any())).thenReturn(requestResponse);
        // when
        Response response = externalResource.findManagementContracts(new Select().getFinalSelect());
        // Then
        @SuppressWarnings("unchecked")
        RequestResponse<ManagementContractModel> entity = (RequestResponse<
                ManagementContractModel
            >) response.getEntity();
        assertThat(toJsonNode(entity).get("$results").get(0).get("Name").textValue()).isEqualTo(
            managementContractModel.getName()
        );
    }
}
