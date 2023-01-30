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
package fr.gouv.vitam.collect.internal.resource;

import fr.gouv.vitam.collect.internal.server.AdminCollectApplication;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.FluxService;
import fr.gouv.vitam.collect.internal.service.MetadataService;
import fr.gouv.vitam.collect.internal.service.ProjectService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.server.HeaderIdContainerFilter;
import fr.gouv.vitam.common.serverv2.VitamStarter;
import fr.gouv.vitam.common.serverv2.application.ApplicationParameter;
import io.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

public class CollectResourceBaseTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectResourceBaseTest.class);

    static final String COLLECT_CONF = "collect-external-test.conf";
    private static final String COLLECT_RESOURCE_URI = "collect-external/v1/";

    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();

    private static VitamStarter application;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    protected static ProjectService projectService = Mockito.mock(ProjectService.class);
    protected static TransactionService transactionService = mock(TransactionService.class);
    protected static MetadataService metadataService = mock(MetadataService.class);
    protected static SipService sipService = mock(SipService.class);
    protected static FluxService fluxService = mock(FluxService.class);
    protected static CollectService collectService = mock(CollectService.class);
    protected static SecureEndpointRegistry secureEndpointRegistry = mock(SecureEndpointRegistry.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        try {
            application = new VitamStarter(CollectConfiguration.class, COLLECT_CONF,
                CollectResourceBaseTest.BusinessApplication.class, AdminCollectApplication.class);
            application.start();
            RestAssured.port = port;
            RestAssured.basePath = COLLECT_RESOURCE_URI;

            LOGGER.debug("Beginning tests");
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access Application Server", e);
        }

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        junitHelper.releasePort(port);
        if (application != null) {
            application.stop();
        }
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() {
        reset(projectService);
        reset(transactionService);
        reset(sipService);
        reset(fluxService);
        reset(metadataService);
        reset(collectService);
    }

    public static class BusinessApplication extends Application {

        private Set<Object> singletons;
        private Set<Class<?>> classes;

        private String configurationFile;

        public BusinessApplication(@Context ServletConfig servletConfig) {
            classes = new HashSet<>();
            classes.add(HeaderIdContainerFilter.class);
            this.configurationFile =
                servletConfig.getInitParameter(ApplicationParameter.CONFIGURATION_FILE_APPLICATION);
        }

        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }

        @Override
        public Set<Object> getSingletons() {
            if (singletons == null) {
                singletons = new HashSet<>();
                singletons.add(new SanityCheckerCommonFilter());
                final ProjectResource projectResource =
                    new ProjectResource(projectService, transactionService, metadataService);
                final TransactionResource transactionResource =
                    new TransactionResource(transactionService, sipService, metadataService, fluxService,
                        projectService);
                final CollectMetadataResource collectMetadataResource =
                    new CollectMetadataResource(metadataService, collectService);
                final EndpointsResource endpointsResource = new EndpointsResource(secureEndpointRegistry);
                singletons.add(projectResource);
                singletons.add(transactionResource);
                singletons.add(collectMetadataResource);
                singletons.add(endpointsResource);
            }
            return singletons;
        }
    }
}