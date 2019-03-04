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
package fr.gouv.vitam.storage.offers.tape.impl.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLocation;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLocationType;
import fr.gouv.vitam.storage.offers.tape.model.TapeModel;
import fr.gouv.vitam.storage.offers.tape.rest.TapeCatalogResource;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TapeCatalogResourceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private TapeCatalogResource tapeCatalogResource;

    @Mock
    private TapeCatalogService tapeCatalogService;
    
    
    @Test
    public void shouldFindTape() throws Exception {
        // Given
        String id = "tapeId";
        TapeModel tapeModel = getTapeModel(id);
        tapeModel.setId(id);
        given(tapeCatalogService.findById(id)).willReturn(tapeModel);

        // When
        Response result = tapeCatalogResource.getTape(id);

        // Then
        assertThat(((RequestResponseOK<JsonNode>)result.getEntity()).getResults().get(0)).isEqualTo(JsonHandler.toJsonNode(tapeModel));
    }

    /*@Test
    public void should_return_not_found_exception_when_certificate_is_missing() throws Exception {
        // Given
        byte[] bytes = new byte[] {1, 2};
        TapeModel tapeModel = new TapeModel();
        tapeModel.setContextId("contextId");
        given(tapeCatalogService.findIdentity(bytes)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> tapeCatalogResource.findIdentityByCertificate(bytes))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void shouldFindContextIsUsed() {
        // Given
        final String CONTEXT_ID = "contextId";
        TapeModel tapeModel = new TapeModel();
        tapeModel.setContextId(CONTEXT_ID);
        given(tapeCatalogService.contextIsUsed(CONTEXT_ID)).willReturn(true);

        // When / Then
        Response response = tapeCatalogResource.contextIsUsed(CONTEXT_ID);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }*/

    private TapeModel getTapeModel(String id) {
        TapeModel tapeModel = new TapeModel();
        tapeModel.setId(id);
        tapeModel.setCapacity(10000L);
        tapeModel.setUsedSize(5000L);
        tapeModel.setFileCount(200L);
        tapeModel.setCode("VIT0001");
        tapeModel.setLabel("VIT-TAPE-1");
        tapeModel.setLibrary("VIT-LIB-1");
        tapeModel.setType("LTO-6");
        tapeModel.setCompressed(false);
        tapeModel.setWorm(false);
        tapeModel.setCurrentLocation(new TapeLocation(1, TapeLocationType.DIRVE));
        tapeModel.setPreviousLocation(new TapeLocation(2, TapeLocationType.SLOT));
        return tapeModel;
    }
}
