package fr.gouv.vitam.access.external.rest;

import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.server.application.resources.VitamStatusService;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminManagementExternalResourcePreservationTest {

    public @Rule MockitoRule mockitoJUnit = MockitoJUnit.rule();

    @Mock private AdminManagementClientFactory managementClientFactory;
    @Mock private IngestInternalClientFactory ingestInternalClientFactory;
    @Mock private AccessInternalClientFactory accessInternalClientFactory;
    @Mock private AdminManagementClient adminManagementClient;
    @Mock private IngestInternalClient ingestInternalClient;
    @Mock private AccessInternalClient accessInternalClient;

    private AdminManagementExternalResource externalResource;

    @Before
    public void setUp() {
        when(managementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(ingestInternalClientFactory.getClient()).thenReturn(ingestInternalClient);
        when(accessInternalClientFactory.getClient()).thenReturn(accessInternalClient);

        SecureEndpointRegistry registry = mock(SecureEndpointRegistry.class);
        VitamStatusService statusService = mock(VitamStatusService.class);
        externalResource = new AdminManagementExternalResource(statusService, registry, managementClientFactory,
            ingestInternalClientFactory, accessInternalClientFactory);
    }

    @Test
    public void shouldImportGriffin() throws AdminManagementClientServerException, InvalidParseOperationException {
        //Given
        when(adminManagementClient.importGriffins(any())).thenReturn(new RequestResponseOK().setHttpCode(200));
        //when
        Response response = externalResource.importGriffin(getFromString("{\"test\":\"test\"}"));
        //then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldCheckGriffinImportRequest() {
        assertThatIllegalArgumentException().isThrownBy(() -> externalResource.importGriffin(null));
    }

    @Test
    public void shouldGetGriffin() throws Exception {
        //Given
        GriffinModel griffinModel = new GriffinModel();
        griffinModel.setName("imageMagic");
        RequestResponse<GriffinModel> requestResponse = new RequestResponseOK<GriffinModel>().addResult(griffinModel);

        //when
        when(adminManagementClient.findGriffinByID("id")).thenReturn(requestResponse);

        Response response = externalResource.findGriffinByID("id");
        //Then
        @SuppressWarnings("unchecked")
        RequestResponse<GriffinModel> entity = (RequestResponse<GriffinModel>) response.getEntity();
        assertThat(toJsonNode(entity).get("$results").get(0).get("Name").textValue()).isEqualTo(griffinModel.getName());
    }

    @Test
    public void shouldImportPreservationScenario()
        throws AdminManagementClientServerException, InvalidParseOperationException {
        //Given
        when(adminManagementClient.importPreservationScenarios(any()))
            .thenReturn(new RequestResponseOK().setHttpCode(200));
        //when
        Response response = externalResource.importPreservationScenario(getFromString("{\"test\":\"test\"}"));
        //then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldCheckScenarioImportRequest() {
        assertThatIllegalArgumentException().isThrownBy(() -> externalResource.importPreservationScenario(null));
    }

    @Test
    public void shouldGetPreservationScenario()
        throws Exception {
        //Given
        PreservationScenarioModel scenario = new PreservationScenarioModel();
        scenario.setName("preservation");
        RequestResponse<PreservationScenarioModel> requestResponse =
            new RequestResponseOK<PreservationScenarioModel>().addResult(scenario);

        //when
        when(adminManagementClient.findPreservationByID("id")).thenReturn(requestResponse);

        Response response = externalResource.findPreservationByID("id");
        //Then
        RequestResponse<GriffinModel> entity = (RequestResponse<GriffinModel>) response.getEntity();
        assertThat(toJsonNode(entity).get("$results").get(0).get("Name").textValue()).isEqualTo(scenario.getName());
    }
}
