package fr.gouv.vitam.functional.administration.rest;

import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.administration.GriffinModel;
import fr.gouv.vitam.functional.administration.griffin.GriffinService;
import fr.gouv.vitam.functional.administration.griffin.PreservationScenarioService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRule;
import org.mockito.junit.MockitoRule;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.Collections;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreservationResourceTest {

    public @Rule MockitoRule mockitoJUnit = MockitoJUnit.rule();

    private @Mock PreservationScenarioService preservationScenarioService;
    private @Mock GriffinService griffinService;
    private PreservationResource preservationResource;

    @Before
    public void setUp() {
        preservationResource = new PreservationResource(preservationScenarioService, griffinService);
    }



    @Test
    public void shouldImportGriffin() throws Exception {

        //Given
        //When
        Response griffinResponse = preservationResource.importGriffin(new ArrayList<>(), mock(UriInfo.class));
        //Then
        assertThat(griffinResponse.getStatus()).isEqualTo(201);
    }

    @Test
    public void shouldFindGriffin() throws Exception {
        //Given
        //When
        Response griffinResponse = preservationResource.findGriffin(getFromString("{}"));
        //Then
        assertThat(griffinResponse.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldFindScenario() throws Exception {
        //Given
        //When
        Response griffinResponse = preservationResource.findPreservation(getFromString("{}"));
        //Then
        assertThat(griffinResponse.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldImportScenario() throws Exception {

        //Given
        //When
        Response griffinResponse = preservationResource.importPreservationScenario(new ArrayList<>(), mock(UriInfo.class));
        //Then
        assertThat(griffinResponse.getStatus()).isEqualTo(201);
    }

}
