package fr.gouv.vitam.logbook.rest;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;

public class BusinessApplication extends Application {

    private Set<Object> singletons;

    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final LogbookConfiguration
                configuration = PropertiesUtils.readYaml(yamlIS, LogbookConfiguration.class);
            CommonBusinessApplication commonBusinessApplication = new CommonBusinessApplication();

            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(new LogbookResource(configuration));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
