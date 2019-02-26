package fr.gouv.vitam.access.internal.rest;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.access.internal.serve.filter.AccessContractIdContainerFilter;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfigurationParameters;
import fr.gouv.vitam.common.server.application.resources.VitamServiceRegistry;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;

/**
 * Business application for access internal declaring resources and filters
 */
public class BusinessApplication extends Application {

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;

    static AccessInternalModule mock = null;

    /**
     * Constructor
     *
     * @param servletConfig the servlet configuration
     */
    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final AccessInternalConfiguration accessInternalConfiguration =
                PropertiesUtils.readYaml(yamlIS, AccessInternalConfiguration.class);
            commonBusinessApplication = new CommonBusinessApplication();

            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());

            if (mock != null) {
                singletons.add(new AccessInternalResourceImpl(mock));
            } else {

                singletons.add(new AccessInternalResourceImpl(accessInternalConfiguration
                ));
                singletons.add(new LogbookInternalResourceImpl());
            }
            singletons.add(new AccessContractIdContainerFilter());
        } catch (IOException e) {
            throw new RuntimeException(e);
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
