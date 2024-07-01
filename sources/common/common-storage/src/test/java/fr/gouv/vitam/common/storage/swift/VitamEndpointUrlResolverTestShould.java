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
package fr.gouv.vitam.common.storage.swift;

import fr.gouv.vitam.common.storage.StorageConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.identity.URLResolverParams;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.openstack.identity.internal.DefaultEndpointURLResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class VitamEndpointUrlResolverTestShould {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageConfiguration configuration;

    @Mock
    private DefaultEndpointURLResolver defaultResolver;

    @Mock
    private URLResolverParams urlResolverParams;

    @Mock
    private Token token;

    private VitamEndpointUrlResolver vitamEndpointUrlResolver;

    @Before
    public void setUp() throws Exception {
        vitamEndpointUrlResolver = new VitamEndpointUrlResolver(defaultResolver, configuration);
        urlResolverParams = URLResolverParams.create(token, ServiceType.OBJECT_STORAGE);
    }

    @Test
    public void findURLV2_when_StorageConfiguration_getSwiftUrl_return_null() throws Exception {
        // Given
        when(defaultResolver.findURLV2(any())).thenReturn("/swift/v1");
        when(configuration.getSwiftUrl()).thenReturn(null);
        // When
        String urlv2 = vitamEndpointUrlResolver.findURLV2(urlResolverParams);
        // Then
        assertThat(urlv2).isEqualTo("/swift/v1");
    }

    @Test
    public void findUrlV2_when_StorageConfiguration_getSwiftUrl_return_empty_String() throws Exception {
        // Given
        when(defaultResolver.findURLV2(any())).thenReturn("/swift/v1");
        when(configuration.getSwiftUrl()).thenReturn(" ");
        // When
        String urlv2 = vitamEndpointUrlResolver.findURLV2(urlResolverParams);
        // Then
        assertThat(urlv2).isEqualTo("/swift/v1");
    }

    @Test
    public void findUrlV2_when_StorageConfiguration_getSwiftUrl_return_good_Url() throws Exception {
        // Given
        when(defaultResolver.findURLV2(any())).thenReturn("/swift/v1");
        when(configuration.getSwiftUrl()).thenReturn("/swift/v3");
        // When
        String urlv2 = vitamEndpointUrlResolver.findURLV2(urlResolverParams);
        // Then
        assertThat(urlv2).isEqualTo("/swift/v3");
    }

    @Test
    public void findURLV3_when_StorageConfiguration_getSwiftUrl_return_null() throws Exception {
        // Given
        when(defaultResolver.findURLV3(any())).thenReturn("/swift/v1");
        when(configuration.getSwiftUrl()).thenReturn(null);
        // When
        String urlv3 = vitamEndpointUrlResolver.findURLV3(urlResolverParams);
        // Then
        assertThat(urlv3).isEqualTo("/swift/v1");
    }

    @Test
    public void findURLV3_when_StorageConfiguration_getSwiftUrl_return_empty_String() throws Exception {
        // Given
        when(defaultResolver.findURLV3(any())).thenReturn("/swift/v1");
        when(configuration.getSwiftUrl()).thenReturn(" ");
        // When
        String urlv3 = vitamEndpointUrlResolver.findURLV3(urlResolverParams);
        // Then
        assertThat(urlv3).isEqualTo("/swift/v1");
    }

    @Test
    public void findURLV3_when_StorageConfiguration_getSwiftUrl_return_good_Url() throws Exception {
        // Given
        when(defaultResolver.findURLV3(any())).thenReturn("/swift/v1");
        when(configuration.getSwiftUrl()).thenReturn("/swift/v3");
        // When
        String urlv3 = vitamEndpointUrlResolver.findURLV3(urlResolverParams);
        // Then
        assertThat(urlv3).isEqualTo("/swift/v3");
    }
}
