package fr.gouv.vitam.access.external.rest;

import com.google.common.base.Throwables;
import fr.gouv.vitam.access.external.rest.v2.rest.AccessExternalResourceV2;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.dsl.schema.DslDynamicFeature;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.SecureEndpointScanner;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

public class BusinessApplicationTest extends Application {

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;

    public BusinessApplicationTest(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);
        SecureEndpointRegistry secureEndpointRegistry = new SecureEndpointRegistry();
        SecureEndpointScanner secureEndpointScanner = new SecureEndpointScanner(secureEndpointRegistry);


        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {

            commonBusinessApplication = new CommonBusinessApplication(true);

            final AccessExternalResource accessExternalResource =
                new AccessExternalResource(secureEndpointRegistry);
            final AccessExternalResourceV2 accessExternalResourceV2 =
                    new AccessExternalResourceV2(secureEndpointRegistry);
            final LogbookExternalResource logbookExternalResource = new LogbookExternalResource();
            final AdminManagementExternalResource adminManagementExternalResource =
                new AdminManagementExternalResource(secureEndpointRegistry);

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
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return commonBusinessApplication.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
