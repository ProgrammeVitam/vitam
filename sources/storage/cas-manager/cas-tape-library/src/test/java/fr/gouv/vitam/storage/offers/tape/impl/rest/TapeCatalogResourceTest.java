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
package fr.gouv.vitam.storage.offers.tape.impl.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.rest.TapeCatalogResource;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
        TapeCatalog tapeCatalog = getTapeModel(id);
        given(tapeCatalogService.findById(id)).willReturn(tapeCatalog);

        // When
        Response result = tapeCatalogResource.getTape(id);

        // Then
        assertThat(((RequestResponseOK<JsonNode>) result.getEntity()).getResults().get(0)).isEqualTo(
            JsonHandler.toJsonNode(tapeCatalog)
        );
    }

    private TapeCatalog getTapeModel(String id) {
        TapeCatalog tapeCatalog = new TapeCatalog();
        tapeCatalog.setId(id);
        tapeCatalog.setCapacity(10000L);
        tapeCatalog.setTapeState(TapeState.OPEN);
        tapeCatalog.setFileCount(200);
        tapeCatalog.setCode("VIT0001");
        tapeCatalog.setLabel(new TapeCatalogLabel(GUIDFactory.newGUID().getId(), "VIT-TAPE-1"));
        tapeCatalog.setLibrary("VIT-LIB-1");
        tapeCatalog.setType("LTO-6");
        tapeCatalog.setCompressed(false);
        tapeCatalog.setWorm(false);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.DRIVE));
        tapeCatalog.setPreviousLocation(new TapeLocation(2, TapeLocationType.SLOT));
        return tapeCatalog;
    }
}
