package fr.gouv.vitam.ingest.external.rest;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.SecureEndpointScanner;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.security.internal.filter.InternalSecurityFilter;

public class BusinessApplication extends Application {

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;

    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        SecureEndpointRegistry secureEndpointRegistry = new SecureEndpointRegistry();
        SecureEndpointScanner secureEndpointScanner = new SecureEndpointScanner(secureEndpointRegistry);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final IngestExternalConfiguration configuration =
                PropertiesUtils.readYaml(yamlIS, IngestExternalConfiguration.class);
            commonBusinessApplication = new CommonBusinessApplication();
            singletons = new HashSet<>();
            singletons.add(new InternalSecurityFilter());
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(new IngestExternalResource(configuration, secureEndpointRegistry));
            singletons.add(new SanityCheckerCommonFilter());
            singletons.add(new SanityDynamicFeature());
            singletons.add(secureEndpointScanner);

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
