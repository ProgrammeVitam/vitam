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
package fr.gouv.vitam.security.internal.filter;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BasicAuthenticationFilterTest {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BasicAuthenticationFilterTest.class);

    private static final AuthenticationLevel AUTHENTICATION_LEVEL = AuthenticationLevel.BASIC_AUTHENT;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ResourceInfo resourceInfo;

    @Mock
    private FeatureContext featureContext;

    @Mock
    private AdminManagementConfiguration configuration;

    @Captor
    private ArgumentCaptor<EndpointAuthenticationFilter> endpointAuthenticationFilter;

    @Before
    public void setup() {
        when(resourceInfo.getResourceClass()).thenReturn((Class) BasicAuthenticationFilterTest.class);
    }

    @Test
    public void checkEndpointAuthenticationFilterRegistration() throws Exception {
        // mock resource info.
        when(resourceInfo.getResourceMethod()).thenReturn(
            BasicAuthenticationFilterTest.class.getMethod("basedVitamAuthenticationMethod")
        );

        // instanciate BasicAuthenticationFilter class
        BasicAuthenticationFilter instance = new BasicAuthenticationFilter(configuration);
        instance.configure(resourceInfo, featureContext);

        verify(featureContext).register(endpointAuthenticationFilter.capture(), eq(Priorities.AUTHORIZATION + 10));
        verifyNoMoreInteractions(featureContext);
        Assert.assertEquals(AUTHENTICATION_LEVEL, endpointAuthenticationFilter.getValue().getAuthentLevel());
    }

    @Test
    public void checkEndpointAuthenticationFilterRegistrationUnusedVitamAuthentication() throws Exception {
        // mock resource info.
        when(resourceInfo.getResourceMethod()).thenReturn(this.getClass().getMethod("UnusedVitamAuthenticationMethod"));

        // instanciate BasicAuthenticationFilter class
        BasicAuthenticationFilter instance = new BasicAuthenticationFilter(configuration);
        instance.configure(resourceInfo, featureContext);

        verify(featureContext, never()).register(any(), anyInt());
    }

    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public void basedVitamAuthenticationMethod() {
        LOGGER.debug("Method using Vitam basic authentication annotation.");
    }

    public void UnusedVitamAuthenticationMethod() {
        LOGGER.debug("No use of the Vitam basic authentication annotation.");
    }
}
