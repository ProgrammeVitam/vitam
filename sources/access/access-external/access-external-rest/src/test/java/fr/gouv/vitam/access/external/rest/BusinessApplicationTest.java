package fr.gouv.vitam.access.external.rest;

import fr.gouv.vitam.access.external.rest.v2.rest.AccessExternalResourceV2;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.common.dsl.schema.DslDynamicFeature;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.SecureEndpointScanner;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.server.application.resources.VitamStatusService;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class BusinessApplicationTest extends Application {

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;
    private final AccessInternalClientFactory accessInternalClientFactory;
    private final AdminManagementClientFactory adminManagementClientFactory;
    private final IngestInternalClientFactory ingestInternalClientFactory;
    private final VitamStatusService vitamStatusService;

    public BusinessApplicationTest() {
        this.accessInternalClientFactory = mock(AccessInternalClientFactory.class);
        this.adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        this.ingestInternalClientFactory = mock(IngestInternalClientFactory.class);
        this.vitamStatusService = mock(VitamStatusService.class);
        commonBusinessApplication = new CommonBusinessApplication(true);
        prepare();
    }

    public BusinessApplicationTest(@Context ServletConfig servletConfig) {
        this.accessInternalClientFactory = AccessInternalClientFactory.getInstance();
        this.adminManagementClientFactory = AdminManagementClientFactory.getInstance();
        this.ingestInternalClientFactory = IngestInternalClientFactory.getInstance();
        this.vitamStatusService = new BasicVitamStatusServiceImpl();
        commonBusinessApplication = new CommonBusinessApplication(true);
        prepare();
    }

    public void prepare() {
        SecureEndpointRegistry secureEndpointRegistry = new SecureEndpointRegistry();
        SecureEndpointScanner secureEndpointScanner = new SecureEndpointScanner(secureEndpointRegistry);

        final AccessExternalResource accessExternalResource =
            new AccessExternalResource(secureEndpointRegistry, accessInternalClientFactory);
        final AccessExternalResourceV2 accessExternalResourceV2 =
            new AccessExternalResourceV2(secureEndpointRegistry, accessInternalClientFactory);
        final LogbookExternalResource logbookExternalResource =
            new LogbookExternalResource(accessInternalClientFactory);
        final AdminManagementExternalResource adminManagementExternalResource =
            new AdminManagementExternalResource(vitamStatusService, secureEndpointRegistry,
                adminManagementClientFactory, ingestInternalClientFactory, accessInternalClientFactory);

        singletons = new HashSet<>();
        singletons.addAll(commonBusinessApplication.getResources());
        singletons.add(accessExternalResource);
        singletons.add(accessExternalResourceV2);
        singletons.add(logbookExternalResource);
        singletons.add(adminManagementExternalResource);
        singletons.add(new SanityCheckerCommonFilter());
        singletons.add(new SanityDynamicFeature());
        singletons.add(new HttpMethodOverrideFilter());
        singletons.add(secureEndpointScanner);
        singletons.add(new DslDynamicFeature());

    }

    @Override
    public Set<Class<?>> getClasses() {
        return commonBusinessApplication.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    public AccessInternalClientFactory getAccessInternalClientFactory() {
        return accessInternalClientFactory;
    }

    public AdminManagementClientFactory getAdminManagementClientFactory() {
        return adminManagementClientFactory;
    }

    public IngestInternalClientFactory getIngestInternalClientFactory() {
        return ingestInternalClientFactory;
    }

    public VitamStatusService getVitamStatusService() {
        return vitamStatusService;
    }
}
