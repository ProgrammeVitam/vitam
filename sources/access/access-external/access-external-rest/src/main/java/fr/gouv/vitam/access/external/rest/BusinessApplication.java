package fr.gouv.vitam.access.external.rest;

import com.google.common.base.Throwables;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.SecureEndpointScanner;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.security.internal.filter.InternalSecurityFilter;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

public class BusinessApplication extends Application {

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;

    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        SecureEndpointRegistry secureEndpointRegistry = new SecureEndpointRegistry();
        SecureEndpointScanner secureEndpointScanner = new SecureEndpointScanner(secureEndpointRegistry);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            commonBusinessApplication = new CommonBusinessApplication();

            final AccessExternalResourceImpl accessExternalResource = new AccessExternalResourceImpl(secureEndpointRegistry);
            final LogbookExternalResourceImpl logbookExternalResource = new LogbookExternalResourceImpl();
            final AdminManagementExternalResourceImpl adminManagementExternalResource = new AdminManagementExternalResourceImpl(secureEndpointRegistry);

            singletons = new HashSet<>();
            singletons.add(new InternalSecurityFilter());
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(accessExternalResource);
            singletons.add(logbookExternalResource);
            singletons.add(adminManagementExternalResource);
            singletons.add(new SanityCheckerCommonFilter());
            singletons.add(new SanityDynamicFeature());
            singletons.add(new HttpMethodOverrideFilter());
            singletons.add(secureEndpointScanner);

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
