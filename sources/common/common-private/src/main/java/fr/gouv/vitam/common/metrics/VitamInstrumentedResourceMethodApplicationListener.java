/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/

package fr.gouv.vitam.common.metrics;

import java.lang.reflect.Method;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import jersey.repackaged.com.google.common.collect.ImmutableMap;

/**
 * A fork of the {@link InstrumentedResourceMethodApplicationListener}
 * <p>
 * This class enables the automatic generation of Metrics/Jersey annotations such as : @Timed, @Metered
 * and @ExceptionMetered on any API end-points of the resources inside the application.
 * </p>
 * Metric names are automatically generated under the form:
 * <p>
 * URI:HTTP_METHOD:METRIC_TYPE
 * </p>
 * Example:
 * <p>
 * /application/test:GET:Timer
 * </p>
 * <p>
 * WARNING This class doesn't support nested Jersey resource
 * </p>
 */
public final class VitamInstrumentedResourceMethodApplicationListener
    extends InstrumentedResourceMethodApplicationListener {

    private final MetricRegistry metrics;
    private ImmutableMap<Method, Timer> timers = ImmutableMap.of();
    private ImmutableMap<Method, Meter> meters = ImmutableMap.of();
    private ImmutableMap<Method, Meter> exceptionMeters = ImmutableMap.of();
    private static final String METRIC_NAME_DELIMITER = ":";
    private static final String METRIC_METER_NAME = "meter";
    private static final String METRIC_TIMER_NAME = "timer";
    private static final String METRIC_EXCEPTION_METER_NAME = "exceptionMeter";
    private static final String METRIC_NAME_CONFIGURATION_PARAMETERS = "Metric name configuration parameters";

    /****************************************************************************************************
     * * THE CODE STARTING HERE DIFFERS FROM {@link InstrumentedResourceMethodApplicationListener} * *
     ***************************************************************************************************/

    /**
     * Construct an application event listener using the given metrics registry.
     * <p>
     * When using this constructor, the {@link VitamInstrumentedResourceMethodApplicationListener} should be added to a
     * Jersey {@code ResourceConfig} as a singleton.
     * </p>
     *
     * @param metrics a {@link MetricRegistry}
     */
    public VitamInstrumentedResourceMethodApplicationListener(MetricRegistry metrics) {
        super(metrics);
        ParametersChecker.checkParameter("MetricRegistry", metrics);
        this.metrics = metrics;
    }

    /**
     * Appends to a given {@code String} the meter metric name and a delimiter character.
     * {@see VitamInstrumentedResourceMethodApplicationListener#METRIC_METER_NAME}
     * {@see VitamInstrumentedResourceMethodApplicationListener#METRIC_NAME_DELIMITER}
     *
     * @param name
     * @return String
     */
    public static final String metricMeterName(final String name) {
        ParametersChecker.checkParameterNullOnly(METRIC_NAME_CONFIGURATION_PARAMETERS, name);

        return name + METRIC_NAME_DELIMITER + METRIC_METER_NAME;
    }

    /**
     * Appends to a given {@code String} the meter metric name and a delimiter character.
     * {@see VitamInstrumentedResourceMethodApplicationListener#METRIC_TIMER_NAME}
     * {@see VitamInstrumentedResourceMethodApplicationListener#METRIC_NAME_DELIMITER}
     *
     * @param name
     * @return String
     */
    public static final String metricTimerName(final String name) {
        ParametersChecker.checkParameterNullOnly(METRIC_NAME_CONFIGURATION_PARAMETERS, name);

        return name + METRIC_NAME_DELIMITER + METRIC_TIMER_NAME;
    }

    /**
     * Appends to a given {@code String} the meter metric name and a delimiter character.
     * {@see VitamInstrumentedResourceMethodApplicationListener#METRIC_EXCEPTION_METER_NAME}
     * {@see VitamInstrumentedResourceMethodApplicationListener#METRIC_NAME_DELIMITER}
     *
     * @param name
     * @return String
     */
    public static final String metricExceptionMeterName(final String name) {
        ParametersChecker.checkParameterNullOnly(METRIC_NAME_CONFIGURATION_PARAMETERS, name);

        return name + METRIC_NAME_DELIMITER + METRIC_EXCEPTION_METER_NAME;
    }

    /**
     * Concat two strings together making sure at least one slash character '/' exists between them.
     *
     * @param first
     * @param second
     * @return String
     */
    final private String concatURI(String first, String second) {
        final StringBuilder stringBuilder = new StringBuilder();

        if (first.length() > 0 && first.charAt(first.length() - 1) != '/' && second.length() > 0 &&
            second.charAt(0) != '/') {
            return stringBuilder.append(first).append('/').append(second).toString();
        } else {
            return stringBuilder.append(first).append(second).toString();
        }
    }

    final private String getConsumedTypesAsString(final ResourceMethod method) {
        final StringBuilder stringBuilder = new StringBuilder();

        if (!method.getConsumedTypes().isEmpty()) {
            for (final MediaType type : method.getConsumedTypes()) {
                stringBuilder.append(type.toString()).append(',');
            }
            return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
        } else {
            return "*";
        }
    }

    final private String getProducedTypesAsString(final ResourceMethod method) {
        final StringBuilder stringBuilder = new StringBuilder();

        if (!method.getProducedTypes().isEmpty()) {
            for (final MediaType type : method.getProducedTypes()) {
                stringBuilder.append(type.toString());
                stringBuilder.append(',');
            }
            return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
        } else {
            return "*";
        }
    }

    /**
     * Creates a generic name for the API end-point of the type:
     * <p>
     * URI:HTTP_METHOD:CONSUME_MEDIA_TYPES:PRODUCE_MEDIA_TYPE
     * </p>
     *
     * @param method {@link ResourceMethod}
     * @param URI {@link String} the end-point URI
     * @return String
     */
    final private String metricGenericName(final ResourceMethod method, final String uri) {
        return uri +
            METRIC_NAME_DELIMITER +
            method.getHttpMethod() +
            METRIC_NAME_DELIMITER +
            getConsumedTypesAsString(method) +
            METRIC_NAME_DELIMITER +
            getProducedTypesAsString(method);
    }

    /**
     * Register a new TimerMetric on a given registry with a given name.
     * <p>
     * Appends the metric type "Timer" to the name.
     * </p>
     *
     * @param registry {@link MetricRegistry}
     * @param name {@link String}
     * @return {@link Timer}
     */
    final private Timer timerMetric(String name) {
        return metrics.timer(metricTimerName(name));
    }

    /**
     * Register a new MeterMetric on a given registry with a given name.
     * <p>
     * Appends the metric type "Meter" to the name.
     * </p>
     *
     * @param registry {@link MetricRegistry}
     * @param name {@link String}
     * @return {@link Meter}
     */
    final private Meter meterMetric(String name) {
        return metrics.meter(metricMeterName(name));
    }

    /**
     * Register a new MeterMetric on a given registry with a given name.
     * <p>
     * Appends the metric type "ExceptionMeter" to the name.
     * </p>
     *
     * @param registry {@link MetricRegistry}
     * @param name {@link String}
     * @return {@link Meter}
     */
    final private Meter exceptionMeterMetric(String name) {
        return metrics.meter(metricExceptionMeterName(name));
    }

    /**
     * This ExceptionMeterRequestEventListener differs from the original one because the
     * {@link ExceptionMeterRequestEventListener#onEvent(RequestEvent)} method is no longer checking if the raised
     * Exception should be caught, instead every exception that occurs marks the meter.
     */
    private static class ExceptionMeterRequestEventListener implements RequestEventListener {
        private final ImmutableMap<Method, Meter> exceptionMeters;

        public ExceptionMeterRequestEventListener(final ImmutableMap<Method, Meter> exceptionMeters) {
            this.exceptionMeters = exceptionMeters;
        }

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() == RequestEvent.Type.ON_EXCEPTION) {
                final ResourceMethod method = event.getUriInfo().getMatchedResourceMethod();
                final Meter meter =
                    method != null ? exceptionMeters.get(method.getInvocable().getDefinitionMethod()) : null;
                if (meter != null) {
                    meter.mark();
                }
            }
        }
    }

    /**
     * <p>
     * Registers a meter, timer and exceptionMeter for a given Jersey end-point.
     * </p>
     * This method excludes extended Jersey methods.
     *
     * @param timerBuilder
     * @param meterBuilder
     * @param exceptionMeterBuilder
     * @param method
     * @param path
     * @param rootPath
     */
    private void registerMetricsForMethod(
        final ImmutableMap.Builder<Method, Timer> timerBuilder,
        final ImmutableMap.Builder<Method, Meter> meterBuilder,
        final ImmutableMap.Builder<Method, Meter> exceptionMeterBuilder,
        final ResourceMethod method,
        final String path,
        final String rootPath) {
        final Method definitionMethod = method.getInvocable().getDefinitionMethod();
        final String metricName;

        // Note : an extended method is a method not present in the original API, but created by Jersey for technical
        // purposes (ex: mediatype transformation, ...)
        if (!method.isExtended() && method.getHttpMethod() != null) {
            // TODO P2 /admin/v1/... and .../status URI are removed here but should be removed with regex in Kibana the
            // day it is possible
            if (rootPath != null && ("/admin/v1".equals(rootPath) || "/status".equals(path) || VitamConfiguration.TENANTS_URL.equals(path))) {
                return;
            } else if (rootPath == null) {
                metricName = metricGenericName(method, path);
            } else {
                metricName = metricGenericName(method, concatURI(rootPath, path));
            }

            meterBuilder.put(definitionMethod, meterMetric(metricName));
            timerBuilder.put(definitionMethod, timerMetric(metricName));
            exceptionMeterBuilder.put(definitionMethod, exceptionMeterMetric(metricName));
        }
    }

    /**
     * This function is called every time the application registers an event.
     * <p>
     * If the event is of type {@code ApplicationEvent.Type.INITIALIZATION_APP_FINISHED} the function will parse the
     * different methods of each {@see Resource} and automatically create metrics.
     * </p>
     */
    @Override
    public void onEvent(ApplicationEvent event) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            final ImmutableMap.Builder<Method, Timer> timerBuilder = ImmutableMap.<Method, Timer>builder();
            final ImmutableMap.Builder<Method, Meter> meterBuilder = ImmutableMap.<Method, Meter>builder();
            final ImmutableMap.Builder<Method, Meter> exceptionMeterBuilder = ImmutableMap.<Method, Meter>builder();

            /*
             * TODO P1: This class does not handle nested resources for the moment. This feature should be implemented
             */
            for (final Resource resource : event.getResourceModel().getResources()) {

                /* TODO P1: Remove the application.wadl resources with a better option */
                if ("application.wadl".equals(resource.getPath())) {
                    continue;
                }
                for (final ResourceMethod method : resource.getResourceMethods()) {
                    registerMetricsForMethod(
                        timerBuilder, meterBuilder, exceptionMeterBuilder, method, resource.getPath(), null);
                }

                for (final Resource childResource : resource.getChildResources()) {
                    for (final ResourceMethod method : childResource.getResourceMethods()) {
                        registerMetricsForMethod(
                            timerBuilder, meterBuilder, exceptionMeterBuilder, method, childResource.getPath(),
                            resource.getPath());
                    }
                }
            }

            timers = timerBuilder.build();
            meters = meterBuilder.build();
            exceptionMeters = exceptionMeterBuilder.build();
        }
    }

    /****************************************************************************************************
     * * THE CODE ENDING HERE DIFFERS FROM {@link InstrumentedResourceMethodApplicationListener} * *
     ***************************************************************************************************/

    private static class TimerRequestEventListener implements RequestEventListener {
        private final ImmutableMap<Method, Timer> timers;
        private Timer.Context context = null;

        public TimerRequestEventListener(final ImmutableMap<Method, Timer> timers) {
            this.timers = timers;
        }

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                final Timer timer = timers.get(event.getUriInfo()
                    .getMatchedResourceMethod().getInvocable().getDefinitionMethod());
                if (timer != null) {
                    context = timer.time();
                }
            } else if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_FINISHED && context != null) {
                context.close();
            }
        }
    }

    private static class MeterRequestEventListener implements RequestEventListener {
        private final ImmutableMap<Method, Meter> meters;

        public MeterRequestEventListener(final ImmutableMap<Method, Meter> meters) {
            this.meters = meters;
        }

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                final Meter meter = meters.get(event.getUriInfo()
                    .getMatchedResourceMethod().getInvocable().getDefinitionMethod());
                if (meter != null) {
                    meter.mark();
                }
            }
        }
    }

    private static class ChainedRequestEventListener implements RequestEventListener {
        private final RequestEventListener[] listeners;

        private ChainedRequestEventListener(final RequestEventListener... listeners) {
            this.listeners = listeners;
        }

        @Override
        public void onEvent(final RequestEvent event) {
            for (final RequestEventListener listener : listeners) {
                listener.onEvent(event);
            }
        }
    }

    @Override
    public RequestEventListener onRequest(final RequestEvent event) {
        final RequestEventListener listener = new ChainedRequestEventListener(
            new TimerRequestEventListener(timers),
            new MeterRequestEventListener(meters),
            new ExceptionMeterRequestEventListener(exceptionMeters));

        return listener;
    }

}
