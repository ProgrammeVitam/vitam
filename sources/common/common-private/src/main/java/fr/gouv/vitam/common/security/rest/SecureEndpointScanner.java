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
package fr.gouv.vitam.common.security.rest;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.text.StrBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Jax-rs scanner for secure endpoint auto-discovery on extarnal apps startup
 */
public class SecureEndpointScanner implements DynamicFeature {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SecureEndpointScanner.class);

    private static final Class<Annotation>[] HTTP_VERB_ANNOTATIONS = new Class[] {
        HEAD.class,
        GET.class,
        POST.class,
        PUT.class,
        DELETE.class,
        OPTIONS.class,
    };

    private final SecureEndpointRegistry secureEndpointRegistry;

    private Set<String> uniqueIds = new HashSet<>();
    private Set<Method> scannedMethods = new HashSet<>();

    public SecureEndpointScanner(SecureEndpointRegistry secureEndpointRegistry) {
        this.secureEndpointRegistry = secureEndpointRegistry;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (isMethodAlreadyRegistred(resourceInfo)) {
            return;
        }

        // Scanning method
        LOGGER.debug(
            "Scanning resource method " +
            resourceInfo.getResourceClass().getName() +
            " . " +
            resourceInfo.getResourceMethod().getName()
        );

        if (isInsecured(resourceInfo)) {
            LOGGER.debug("Skipping unsecured resource");
            return;
        }

        // Retrieve @Secure annotation
        Secured securedAnnotation = resourceInfo.getResourceMethod().getAnnotation(Secured.class);
        if (securedAnnotation == null) {
            throw new IllegalStateException(
                "Missing @" +
                Secured.class.getName() +
                " annotation for method " +
                resourceInfo.getResourceClass().getName() +
                " . " +
                resourceInfo.getResourceMethod().getName()
            );
        }

        ensureUniqueEndpointId(securedAnnotation.permission().getPermission());

        registerEndpoints(resourceInfo, securedAnnotation);
    }

    private boolean isMethodAlreadyRegistred(ResourceInfo resourceInfo) {
        // Don't know why configure method is invoked twice.
        // Just skip second invocation
        return !scannedMethods.add(resourceInfo.getResourceMethod());
    }

    private boolean isInsecured(ResourceInfo resourceInfo) {
        Unsecured unsecuredAnnotation = resourceInfo.getResourceMethod().getAnnotation(Unsecured.class);
        return (unsecuredAnnotation != null);
    }

    private void ensureUniqueEndpointId(String id) {
        if (!uniqueIds.add(id)) {
            throw new IllegalStateException("Duplicate secured endpoint id '" + id + "'");
        }
    }

    private void registerEndpoints(ResourceInfo resourceInfo, Secured securedAnnotation) {
        List<String> httpVerbs = getHttpVerbs(resourceInfo);

        String endpointPath = getEndpointPath(resourceInfo);

        String[] producedMediaTypes = getProducedMediaTypes(resourceInfo);

        String[] consumedMediaTypes = getConsumedMediaTypes(resourceInfo);

        for (String verb : httpVerbs) {
            EndpointInfo endpointInfo = new EndpointInfo();
            endpointInfo.setPermission(securedAnnotation.permission().getPermission());
            endpointInfo.setVerb(verb);
            endpointInfo.setEndpoint(endpointPath);
            endpointInfo.setConsumedMediaTypes(consumedMediaTypes);
            endpointInfo.setProducedMediaTypes(producedMediaTypes);
            endpointInfo.setDescription(securedAnnotation.description());

            secureEndpointRegistry.add(endpointInfo);
        }
    }

    private String[] getConsumedMediaTypes(ResourceInfo resourceInfo) {
        String[] consumedMediaTypes;
        Consumes classConsumes = resourceInfo.getResourceClass().getAnnotation(Consumes.class);
        Consumes methodConsumes = resourceInfo.getResourceMethod().getAnnotation(Consumes.class);

        if (methodConsumes != null) {
            consumedMediaTypes = methodConsumes.value();
        } else if (classConsumes != null) {
            consumedMediaTypes = classConsumes.value();
        } else {
            consumedMediaTypes = ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return consumedMediaTypes;
    }

    private String[] getProducedMediaTypes(ResourceInfo resourceInfo) {
        String[] producedMediaTypes;
        Produces classProduces = resourceInfo.getResourceClass().getAnnotation(Produces.class);
        Produces methodProduces = resourceInfo.getResourceMethod().getAnnotation(Produces.class);

        if (methodProduces != null) {
            producedMediaTypes = methodProduces.value();
        } else if (classProduces != null) {
            producedMediaTypes = classProduces.value();
        } else {
            producedMediaTypes = ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return producedMediaTypes;
    }

    private String getEndpointPath(ResourceInfo resourceInfo) {
        Path classPathAnnotation = resourceInfo.getResourceClass().getAnnotation(Path.class);
        Path methodPathAnnotation = resourceInfo.getResourceMethod().getAnnotation(Path.class);

        StrBuilder pathBuilder = new StrBuilder();

        pathBuilder.append('/');
        if (classPathAnnotation != null) {
            pathBuilder.append(classPathAnnotation.value());
        }
        pathBuilder.append('/');
        if (methodPathAnnotation != null) {
            pathBuilder.append(methodPathAnnotation.value());
        }
        pathBuilder.append('/');

        pathBuilder.replaceAll("//", "/");

        return pathBuilder.toString();
    }

    private List<String> getHttpVerbs(ResourceInfo resourceInfo) {
        List<String> httpVerbs = new ArrayList<>();
        for (Class verbAnnotationClass : HTTP_VERB_ANNOTATIONS) {
            Annotation annotation = resourceInfo.getResourceMethod().getAnnotation(verbAnnotationClass);
            if (annotation != null) {
                httpVerbs.add(verbAnnotationClass.getSimpleName());
            }
        }
        return httpVerbs;
    }
}
