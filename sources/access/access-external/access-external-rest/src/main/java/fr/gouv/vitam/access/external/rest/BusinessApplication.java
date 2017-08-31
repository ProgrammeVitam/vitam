package fr.gouv.vitam.access.external.rest;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import com.google.common.base.Throwables;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;

public class BusinessApplication extends Application {

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;

    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            commonBusinessApplication = new CommonBusinessApplication();
            
            final AccessExternalResourceImpl accessExternalResource = new AccessExternalResourceImpl();
            final LogbookExternalResourceImpl logbookExternalResource = new LogbookExternalResourceImpl();
            final AdminManagementExternalResourceImpl adminManagementExternalResource = new AdminManagementExternalResourceImpl();
            
            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(accessExternalResource);
            singletons.add(logbookExternalResource);
            singletons.add(adminManagementExternalResource);
            singletons.add(new SanityCheckerCommonFilter());
            singletons.add(new SanityDynamicFeature());
            singletons.add(new HttpMethodOverrideFilter());
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
