/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * LocalDateTime utilities
 */
public final class LocalDateUtil {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LocalDateUtil.class);

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .appendPattern("[zz]")
        .toFormatter();
    private static final DateTimeFormatter ZONED_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm[:ss][.SSS][zz]"
    );
    private static final DateTimeFormatter SLASHED_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_OFFSET_DATE_FORMATTER = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_OFFSET_DATE)
        .parseDefaulting(ChronoField.SECOND_OF_DAY, 0)
        .toFormatter();
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final int THOUSAND = 1000;
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";
    public static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd";

    private static final DateTimeFormatter INDEX_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    public static final String LONG_SECOND_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    private static Clock clock = Clock.systemUTC();

    private LocalDateUtil() {
        // empty
    }

    /**
     * @param localDateTime in format LocalDateTime to transform
     * @return the ISO Date Time
     */
    public static final String getString(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * @param localDateTime in format LocalDateTime to transform
     * @return the ISO Date Time
     */
    public static final String getStringFormatted(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * @param date in format date to transform
     * @return the ISO Date Time
     */
    public static String getString(Date date) {
        return fromDate(date).format(DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * @return the LocalDateTime now in UTC
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * @param date in format String to transform
     * @return the corresponding Date from date string
     * @throws ParseException
     * @throws IllegalArgumentException date null or empty
     */
    public static Date getDate(String date) throws ParseException {
        ParametersChecker.checkParameter("Date", date);
        if (date.length() == SIMPLE_DATE_FORMAT.length()) {
            return getSimpleFormattedDate(date);
        }
        if (date.indexOf('T') == -1) {
            return Date.from(
                LocalDate.parse(date, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
        }
        return getDate(LocalDateTime.parse(date, DATE_FORMATTER));
    }

    /**
     * @param millis in format long to transform
     * @return the corresponding LocalDateTime in UTC
     */
    public static LocalDateTime fromMillis(long millis) {
        if (millis < 0) {
            return now();
        }
        return LocalDateTime.ofEpochSecond(millis / THOUSAND, (int) ((millis % THOUSAND) * THOUSAND), ZoneOffset.UTC);
    }

    /**
     * @param date in format Date to transform
     * @return the corresponding LocalDateTime in UTC
     */
    public static LocalDateTime fromDate(Date date) {
        if (date == null) {
            return now();
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    /**
     * @param fileTime in format FileTime to transform
     * @return the corresponding LocalDateTime in UTC
     */
    public static LocalDateTime fromDate(FileTime fileTime) {
        if (fileTime == null) {
            return now();
        }
        return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC);
    }

    /**
     * @param ldt in format LocalDateTime to transform
     * @return the corresponding date
     */
    public static Date getDate(LocalDateTime ldt) {
        if (ldt == null) {
            return new Date();
        }
        return Date.from(ldt.toInstant(ZoneOffset.UTC));
    }

    /**
     * @param date date
     * @return formatted date
     */
    public static String getFormattedDate(Date date) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        return dateFormat.format(date);
    }

    /**
     * @param date date
     * @return formatted date
     */
    public static String getFormattedDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern(LONG_SECOND_DATE_FORMAT));
    }

    /**
     * @param date date
     * @return formatted date
     */
    public static String getFormattedSimpleDate(Date date) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
        return dateFormat.format(date);
    }

    /**
     * @param date localDate
     * @return formatted date
     */
    public static String getFormattedSimpleDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(SIMPLE_DATE_FORMAT));
    }

    /**
     * @param date date
     * @return formatted date
     * @throws ParseException
     */
    public static Date getSimpleFormattedDate(final String date) throws ParseException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
        return dateFormat.parse(date);
    }

    /**
     * @param date formatted date
     * @return the corresponding LocalDate
     */
    public static LocalDate getLocalDateFromSimpleFormattedDate(String date) {
        if (date == null) {
            return LocalDate.now(clock);
        }
        return LocalDate.parse(date, DateTimeFormatter.ofPattern(SIMPLE_DATE_FORMAT));
    }

    /**
     * Use to have homogeneous String date format on database
     *
     * @param date the date to format for database
     * @return the formatted date for database
     * @throws DateTimeParseException thrown when cannot parse String date (not ISO_LOCAL_DATE_TIME, not
     * ZONED_DATE_TIME_FORMAT and not ISO_DATE date format)
     */
    public static String getFormattedDateForMongo(String date) {
        LocalDateTime ldt;
        try {
            ldt = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            LOGGER.debug("Cannot use ISO_LOCAL_DATE_TIME formatter, try with Zoned one");
            try {
                ldt = LocalDateTime.parse(date, ZONED_DATE_TIME_FORMAT);
            } catch (DateTimeParseException ex) {
                LOGGER.debug(
                    "Cannot use Zoned LOCAL_DATE_TIME formatter, try with ISO_DATE one and time to " + "00:00:00.000"
                );
                try {
                    ldt = LocalDate.parse(date, DateTimeFormatter.ISO_DATE).atTime(0, 0, 0, 0);
                } catch (DateTimeParseException exc) {
                    LOGGER.debug("Cannot use ISO_DATE formatter, try with SLASH_DATE on and set time to 00:00:00.000");
                    ldt = LocalDate.parse(date, SLASHED_DATE).atTime(0, 0, 0, 0);
                }
            }
        }
        return getFormattedDateForMongo(ldt);
    }

    /**
     * Transform ISO_OFFSET_DATE to ISO_OFFSET_DATE_TIME
     *
     * @param date the date to format for elastic
     * @return the formatted date for elastic
     */
    public static String transformIsoOffsetDateToIsoOffsetDateTime(String date) {
        // BUG #3844
        if (date == null) {
            return null;
        }
        try {
            final TemporalAccessor parse1 = ISO_OFFSET_DATE_FORMATTER.parse(date);
            return ZonedDateTime.from(parse1).format(ISO_OFFSET_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exc) {
            // We do nothing on the date if the date is not in ISO_OFFSET_DATE format
            return date;
        }
    }

    /**
     * Use to have homogeneous String date format on database
     *
     * @param date the date to format for database
     * @return the formatted date for database
     */

    public static String getFormattedDateForMongo(LocalDateTime date) {
        return date.format(ZONED_DATE_TIME_FORMAT);
    }

    /**
     * Parses a mongo formated date
     *
     * @param str formatted date in database
     * @return the parsed local date time
     */

    public static LocalDateTime parseMongoFormattedDate(String str) {
        return LocalDateTime.parse(str, ZONED_DATE_TIME_FORMAT);
    }

    /**
     * Use to have homogeneous String date format on ES indexes
     *
     * @param date the date to format for database
     * @return the formatted date for database
     */

    public static String getFormattedDateForEsIndexes(LocalDateTime date) {
        return date.format(INDEX_DATE_TIME_FORMAT);
    }

    /**
     * return a DateTimeFormatter suitable for filename in the format yyyyMMddHHmmssSSS
     *
     * @return
     */
    public static DateTimeFormatter getDateTimeFormatterForFileNames() {
        // Cannot use yyyyMMddHHmmssSSS due to Java 8 bug https://bugs.java.com/view_bug.do?bug_id=8031085
        return new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmss")
            .appendValue(ChronoField.MILLI_OF_SECOND, 3)
            .toFormatter()
            .withZone(ZoneOffset.UTC);
    }

    public static LocalDate parseDate(String endDateStr) {
        if (endDateStr == null) {
            return null;
        }

        return LocalDate.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static long currentTimeMillis() {
        return clock.millis();
    }

    public static LocalDateTime max(LocalDateTime localDateTime1, LocalDateTime localDateTime2) {
        if (localDateTime1 == null) {
            return localDateTime2;
        }

        if (localDateTime2 == null) {
            return localDateTime1;
        }

        if (localDateTime1.isAfter(localDateTime2)) {
            return localDateTime1;
        }
        return localDateTime2;
    }

    public static Instant getInstant() {
        return clock.instant();
    }

    @VisibleForTesting
    public static Clock getClock() {
        return clock;
    }

    @VisibleForTesting
    public static void setClock(Clock clock) {
        LocalDateUtil.clock = clock;
    }

    @VisibleForTesting
    public static void resetClock() {
        LocalDateUtil.clock = Clock.systemUTC();
    }
}
