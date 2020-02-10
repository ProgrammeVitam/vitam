/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.serverv2.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import fr.gouv.vitam.common.metrics.VitamMetricRegistry;
import fr.gouv.vitam.common.metrics.VitamMetrics;
import fr.gouv.vitam.common.metrics.VitamMetricsType;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Optional;

@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class MetricsFeature implements DynamicFeature {

    private VitamMetricRegistry registry;

    public MetricsFeature() {
        VitamMetrics metrics =
            CommonBusinessApplication.metrics.get(VitamMetricsType.REST);
        if (null != metrics) {
            this.registry = metrics.getRegistry();
        }
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {

        if (null == registry) {
            return;
        }

        final Timer timer = registry.timer(buildMetricsName(resourceInfo, "timer"));
        final Meter meter = registry.meter(buildMetricsName(resourceInfo, "meter"));

        context.register(new MetricsInterceptor(timer, meter));
    }

    /**
     * Build a name that completely describes a REST resource
     * @param resourceInfo
     * @param suffix Optional suffix to add (skipped if null)
     * @return
     */
    public final static String buildMetricsName(final ResourceInfo resourceInfo, final String suffix) {

        final Method resourceMethod = resourceInfo.getResourceMethod();

        final String rootPath = Optional.ofNullable(resourceInfo.getResourceClass().getAnnotation(Path.class)).map(p -> p.value()).orElse("/");
        final String methodPath = Optional.ofNullable(resourceMethod.getAnnotation(Path.class)).map(p -> p.value()).orElse("");
        final String fullPath = rootPath.endsWith("/")?  rootPath + methodPath : rootPath +"/"+ methodPath;

        final String method = getMethod(resourceMethod);

        final String consumes = Optional.ofNullable(resourceMethod.getAnnotation(Consumes.class)).map(a -> Joiner.on(",").join(a.value())).orElse("*");

        final String produces = Optional.ofNullable(resourceMethod.getAnnotation(Produces.class)).map(a -> Joiner.on(",").join(a.value())).orElse("*");

        return Joiner.on(":").skipNulls().join(fullPath, method, consumes, produces, suffix);
    }

    private final static String getMethod(Method resourceMethod) {
        if (resourceMethod.isAnnotationPresent(GET.class)) {
            return HttpMethod.GET;
        }
        if (resourceMethod.isAnnotationPresent(POST.class)) {
            return HttpMethod.POST;
        }
        if (resourceMethod.isAnnotationPresent(PUT.class)) {
            return HttpMethod.PUT;
        }
        if (resourceMethod.isAnnotationPresent(DELETE.class)) {
            return HttpMethod.DELETE;
        }
        if (resourceMethod.isAnnotationPresent(HEAD.class)) {
            return HttpMethod.HEAD;
        }
        if (resourceMethod.isAnnotationPresent(OPTIONS.class)) {
            return HttpMethod.OPTIONS;
        }

        throw new IllegalStateException("Resource method without GET, POST, PUT, DELETE, HEAD or OPTIONS annotation");
    }
}
