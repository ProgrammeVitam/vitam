/*
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
 */

package fr.gouv.vitam.common.metrics;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Clock;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * A reporter which outputs measurements to a {@link PrintStream}, like {@code System.out}.
 */
public class LogbackReporter extends ScheduledReporter {
    // TODO Should this logger really be static ? (same one for ALL the metrics) perhaps not...
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbackReporter.class);

    private static final int CONSOLE_WIDTH = 80;
    private static final AtomicInteger UNIQUE_RANK = new AtomicInteger(0);

    private final PrintStream output;
    private final ByteArrayOutputStream byteArrayOutput;
    private final Locale locale;
    private final Clock clock;
    private final DateFormat dateFormat;
    private final VitamLogger privateLogger;

    /**
     * Returns a new {@link Builder} for {@link ConsoleReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link ConsoleReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link ConsoleReporter} instances. Defaults to using the default locale and time zone, writing to
     * {@code System.out}, converting rates to events/second, converting durations to milliseconds, and not filtering
     * metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Locale locale;
        private Clock clock;
        private TimeZone timeZone;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private VitamLogLevel logLevel;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            locale = Locale.getDefault();
            clock = Clock.defaultClock();
            timeZone = TimeZone.getDefault();
            rateUnit = TimeUnit.SECONDS;
            durationUnit = TimeUnit.MILLISECONDS;
            filter = MetricFilter.ALL;
            logLevel = VitamLogLevel.INFO;
        }

        /**
         * Format numbers for the given {@link Locale}.
         *
         * @param locale a {@link Locale}
         * @return {@code this}
         */
        public Builder formattedFor(Locale locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Use the given {@link TimeZone} for the time.
         *
         * @param timeZone a {@link TimeZone}
         * @return {@code this}
         */
        public Builder formattedFor(TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Set the LogBack log level
         *
         * @param logLevel {@link VitamLogLevel}
         * @return {@code this}
         */
        public Builder logLevel(VitamLogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Builds a {@link ConsoleReporter} with the given properties.
         *
         * @return a {@link ConsoleReporter}
         */
        public LogbackReporter build() {
            return new LogbackReporter(registry,
                locale,
                clock,
                timeZone,
                rateUnit,
                durationUnit,
                filter,
                logLevel);
        }
    }

    private LogbackReporter(MetricRegistry registry,
        Locale locale,
        Clock clock,
        TimeZone timeZone,
        TimeUnit rateUnit,
        TimeUnit durationUnit,
        MetricFilter filter,
        VitamLogLevel logLevel) {
        super(registry, "logback-reporter", filter, rateUnit, durationUnit);
        byteArrayOutput = new ByteArrayOutputStream();
        output = new PrintStream(byteArrayOutput);
        this.locale = locale;
        this.clock = clock;
        dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
            DateFormat.MEDIUM,
            locale);
        dateFormat.setTimeZone(timeZone);
        privateLogger = VitamLoggerFactory.getInstance("logback-reporter" + UNIQUE_RANK.incrementAndGet());
        privateLogger.setLevel(logLevel);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void report(SortedMap<String, Gauge> gauges,
        SortedMap<String, Counter> counters,
        SortedMap<String, Histogram> histograms,
        SortedMap<String, Meter> meters,
        SortedMap<String, Timer> timers) {
        byteArrayOutput.reset();
        final String dateTime = dateFormat.format(new Date(clock.getTime()));
        printWithBanner(dateTime, '=');
        output.println();

        if (!gauges.isEmpty()) {
            printWithBanner("-- Gauges", '-');
            for (final Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                output.println(entry.getKey());
                printGauge(entry);
            }
            output.println();
        }

        if (!counters.isEmpty()) {
            printWithBanner("-- Counters", '-');
            for (final Map.Entry<String, Counter> entry : counters.entrySet()) {
                output.println(entry.getKey());
                printCounter(entry);
            }
            output.println();
        }

        if (!histograms.isEmpty()) {
            printWithBanner("-- Histograms", '-');
            for (final Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                output.println(entry.getKey());
                printHistogram(entry.getValue());
            }
            output.println();
        }

        if (!meters.isEmpty()) {
            printWithBanner("-- Meters", '-');
            for (final Map.Entry<String, Meter> entry : meters.entrySet()) {
                output.println(entry.getKey());
                printMeter(entry.getValue());
            }
            output.println();
        }

        if (!timers.isEmpty()) {
            printWithBanner("-- Timers", '-');
            for (final Map.Entry<String, Timer> entry : timers.entrySet()) {
                output.println(entry.getKey());
                printTimer(entry.getValue());
            }
            output.println();
        }

        output.println();
        try {
            final String msg = byteArrayOutput.toString(CharsetUtils.UTF_8);
            privateLogger.log(privateLogger.getLevel(), msg);
        } catch (final UnsupportedEncodingException e) {
            LOGGER.error(e);
        }
    }

    private void printMeter(Meter meter) {
        output.printf(locale, "             count = %d%n", meter.getCount());
        output.printf(locale, "         mean rate = %2.2f events/%s%n", convertRate(meter.getMeanRate()),
            getRateUnit());
        output.printf(locale, "     1-minute rate = %2.2f events/%s%n", convertRate(meter.getOneMinuteRate()),
            getRateUnit());
        output.printf(locale, "     5-minute rate = %2.2f events/%s%n", convertRate(meter.getFiveMinuteRate()),
            getRateUnit());
        output.printf(locale, "    15-minute rate = %2.2f events/%s%n", convertRate(meter.getFifteenMinuteRate()),
            getRateUnit());
    }

    private void printCounter(Map.Entry<String, Counter> entry) {
        output.printf(locale, "             count = %d%n", entry.getValue().getCount());
    }

    @SuppressWarnings("rawtypes")
    private void printGauge(Map.Entry<String, Gauge> entry) {
        output.printf(locale, "             value = %s%n", entry.getValue().getValue());
    }

    private void printHistogram(Histogram histogram) {
        output.printf(locale, "             count = %d%n", histogram.getCount());
        final Snapshot snapshot = histogram.getSnapshot();
        output.printf(locale, "               min = %d%n", snapshot.getMin());
        output.printf(locale, "               max = %d%n", snapshot.getMax());
        output.printf(locale, "              mean = %2.2f%n", snapshot.getMean());
        output.printf(locale, "            stddev = %2.2f%n", snapshot.getStdDev());
        output.printf(locale, "            median = %2.2f%n", snapshot.getMedian());
        output.printf(locale, "              75%% <= %2.2f%n", snapshot.get75thPercentile());
        output.printf(locale, "              95%% <= %2.2f%n", snapshot.get95thPercentile());
        output.printf(locale, "              98%% <= %2.2f%n", snapshot.get98thPercentile());
        output.printf(locale, "              99%% <= %2.2f%n", snapshot.get99thPercentile());
        output.printf(locale, "            99.9%% <= %2.2f%n", snapshot.get999thPercentile());
    }

    private void printTimer(Timer timer) {
        final Snapshot snapshot = timer.getSnapshot();
        output.printf(locale, "             count = %d%n", timer.getCount());
        output.printf(locale, "         mean rate = %2.2f calls/%s%n", convertRate(timer.getMeanRate()), getRateUnit());
        output.printf(locale, "     1-minute rate = %2.2f calls/%s%n", convertRate(timer.getOneMinuteRate()),
            getRateUnit());
        output.printf(locale, "     5-minute rate = %2.2f calls/%s%n", convertRate(timer.getFiveMinuteRate()),
            getRateUnit());
        output.printf(locale, "    15-minute rate = %2.2f calls/%s%n", convertRate(timer.getFifteenMinuteRate()),
            getRateUnit());

        output.printf(locale, "               min = %2.2f %s%n", convertDuration(snapshot.getMin()), getDurationUnit());
        output.printf(locale, "               max = %2.2f %s%n", convertDuration(snapshot.getMax()), getDurationUnit());
        output.printf(locale, "              mean = %2.2f %s%n", convertDuration(snapshot.getMean()),
            getDurationUnit());
        output.printf(locale, "            stddev = %2.2f %s%n", convertDuration(snapshot.getStdDev()),
            getDurationUnit());
        output.printf(locale, "            median = %2.2f %s%n", convertDuration(snapshot.getMedian()),
            getDurationUnit());
        output.printf(locale, "              75%% <= %2.2f %s%n", convertDuration(snapshot.get75thPercentile()),
            getDurationUnit());
        output.printf(locale, "              95%% <= %2.2f %s%n", convertDuration(snapshot.get95thPercentile()),
            getDurationUnit());
        output.printf(locale, "              98%% <= %2.2f %s%n", convertDuration(snapshot.get98thPercentile()),
            getDurationUnit());
        output.printf(locale, "              99%% <= %2.2f %s%n", convertDuration(snapshot.get99thPercentile()),
            getDurationUnit());
        output.printf(locale, "            99.9%% <= %2.2f %s%n", convertDuration(snapshot.get999thPercentile()),
            getDurationUnit());
    }

    private void printWithBanner(String s, char c) {
        output.print(s);
        output.print(' ');
        for (int i = 0; i < CONSOLE_WIDTH - s.length() - 1; i++) {
            output.print(c);
        }
        output.println();
    }
}
