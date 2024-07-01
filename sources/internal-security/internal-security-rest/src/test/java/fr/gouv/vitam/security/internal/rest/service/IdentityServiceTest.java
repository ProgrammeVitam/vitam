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
package fr.gouv.vitam.security.internal.rest.service;

import fr.gouv.vitam.security.internal.common.model.IdentityInsertModel;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.InputStream;
import java.util.Optional;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

public class IdentityServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private IdentityService identityService;

    @Mock
    private IdentityRepository identityRepository;

    @Test
    public void should_store_certificate() throws Exception {
        // Given
        IdentityInsertModel identityInsertModel = new IdentityInsertModel();
        identityInsertModel.setContextId("contextId");
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        identityInsertModel.setCertificate(certificate);

        // When
        identityService.createIdentity(identityInsertModel);

        // Then
        ArgumentCaptor<IdentityModel> identityModelCaptor = ArgumentCaptor.forClass(IdentityModel.class);
        then(identityRepository).should().createIdentity(identityModelCaptor.capture());
        IdentityModel identityModel = identityModelCaptor.getValue();

        assertThat(identityModel.getSubjectDN()).isEqualTo("CN=userAdmin, O=VITAM, L=Paris, C=FR");
        assertThat(identityModel.getSerialNumber()).isEqualTo("3");
        assertThat(identityModel.getIssuerDN()).isEqualTo("O=VITAM, L=Paris, C=FR");
        assertThat(identityModel.getCertificate()).isEqualTo(certificate);
        assertThat(identityModel.getExpirationDate()).isEqualTo("9999-12-31T23:59:59.000");
    }

    @Test
    public void should_read_certificate() throws Exception {
        // Given
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certBinary = toByteArray(stream);
        IdentityModel identityModel = new IdentityModel();
        identityModel.setCertificate(certBinary);
        doReturn(Optional.of(identityModel)).when(identityRepository).findIdentity(any(), any());

        // When
        identityService.findIdentity(certBinary);

        // Then
        then(identityRepository).should().findIdentity("CN=userAdmin, O=VITAM, L=Paris, C=FR", "3");
    }

    @Test
    public void should_update_certificate() throws Exception {
        // Given
        IdentityInsertModel identityInsertModel = new IdentityInsertModel();
        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        identityInsertModel.setCertificate(toByteArray(stream));
        String contextId = "contextId";

        identityService.createIdentity(identityInsertModel);
        IdentityModel identityModel = new IdentityModel();
        given(identityRepository.findIdentity("CN=userAdmin, O=VITAM, L=Paris, C=FR", "3")).willReturn(
            of(identityModel)
        );

        // When
        identityInsertModel.setContextId(contextId);
        Optional<IdentityModel> result = identityService.linkContextToIdentity(identityInsertModel);

        // Then
        then(identityRepository)
            .should()
            .linkContextToIdentity(
                identityModel.getSubjectDN(),
                identityModel.getContextId(),
                identityModel.getSerialNumber()
            );
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(identity -> assertThat(identity.getContextId()).isEqualTo(contextId));
    }

    @Test
    public void shouldFindContextIsUsed() {
        // Given
        final String CONTEXT_ID = "contextId";
        given(identityRepository.contextIsUsed(CONTEXT_ID)).willReturn(true);

        // When / Then
        assertTrue(identityService.contextIsUsed(CONTEXT_ID));
    }
}
