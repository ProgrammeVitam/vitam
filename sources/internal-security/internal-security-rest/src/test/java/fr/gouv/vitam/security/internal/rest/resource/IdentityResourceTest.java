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
package fr.gouv.vitam.security.internal.rest.resource;

import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.rest.service.IdentityService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

public class IdentityResourceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private IdentityResource identityResource;

    @Mock
    private IdentityService identityService;

    @Test
    public void should_read_certificate() throws Exception {
        // Given
        byte[] bytes = new byte[] {1, 2};
        IdentityModel identityModel = new IdentityModel();
        identityModel.setContextId("contextId");
        given(identityService.findIdentity(bytes)).willReturn(of(identityModel));

        // When
        IdentityModel result = identityResource.findIdentityByCertificate(bytes);

        // Then
        assertThat(result).isEqualTo(identityModel);
    }

    @Test
    public void should_return_not_found_exception_when_certificate_is_missing() throws Exception {
        // Given
        byte[] bytes = new byte[] {1, 2};
        IdentityModel identityModel = new IdentityModel();
        identityModel.setContextId("contextId");
        given(identityService.findIdentity(bytes)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> identityResource.findIdentityByCertificate(bytes))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void shouldFindContextIsUsed() {
        // Given
        final String CONTEXT_ID = "contextId";
        IdentityModel identityModel = new IdentityModel();
        identityModel.setContextId(CONTEXT_ID);
        given(identityService.contextIsUsed(CONTEXT_ID)).willReturn(true);

        // When / Then
        Response response = identityResource.contextIsUsed(CONTEXT_ID);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }
}
