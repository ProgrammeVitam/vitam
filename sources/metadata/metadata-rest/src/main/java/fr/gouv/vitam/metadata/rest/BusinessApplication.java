package fr.gouv.vitam.metadata.rest;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

import com.google.common.base.Throwables;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class BusinessApplication extends Application {

    private CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;

    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final MetaDataConfiguration
                metaDataConfiguration = PropertiesUtils.readYaml(yamlIS, MetaDataConfiguration.class);
            commonBusinessApplication = new CommonBusinessApplication();
            final MetaDataResource metaDataResource = new MetaDataResource(metaDataConfiguration);

            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(metaDataResource);
            singletons.add(new SanityCheckerCommonFilter());
            singletons.add(new SanityDynamicFeature());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
