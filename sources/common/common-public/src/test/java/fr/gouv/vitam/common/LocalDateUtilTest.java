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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@SuppressWarnings("javadoc")
public class LocalDateUtilTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LocalDateUtilTest.class);

    @Test
    public void check1970() throws ParseException {
        LocalDateTime dt = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime dt0 = LocalDateUtil.fromMillis(0);
        LOGGER.info(dt + " vs " + dt0);
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        dt = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        final Date date = new Date(0);
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusDays(1);
        assertEquals(1969, dt.getYear());
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        assertNotNull(LocalDateUtil.getDate("2016-06-07T07:00"));
        assertNotNull(LocalDateUtil.getDate("2016-06-07T07:00:14"));
        assertNotNull(LocalDateUtil.getDate("2016-06-07T07:00:14.14"));
        assertNotNull(LocalDateUtil.getDate("2017-04-23T15:01:03.43Z"));
        assertNotNull(LocalDateUtil.getDate("2017-04-23T15:01:03.430PST"));
    }

    @Test
    public void check1000() {
        LocalDateTime dt = LocalDateTime.of(1000, 1, 1, 0, 0);
        LocalDateTime dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        final Date date = new Date(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusDays(1);
        assertEquals(999, dt.getYear());
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
    }

    @Test
    public void check1() {
        LocalDateTime dt = LocalDateTime.of(1, 1, 1, 0, 0);
        LocalDateTime dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        final Date date = new Date(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusDays(1);
        // should be -1, not 0
        assertEquals(0, dt.getYear());
        LOGGER.info("Year 0 but -1: " + dt);
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusYears(1);
        // should be -2, not -1
        assertEquals(-1, dt.getYear());
        LOGGER.info("Year -1 but -2: " + dt);
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        assertTrue(LocalDateUtil.getFormattedDate(date).contains("T"));
        assertTrue(LocalDateUtil.getFormattedDate(date).contains("+"));
        assertEquals(19, LocalDateUtil.getFormattedDate(LocalDateUtil.now()).length());
    }

    @Test
    public void checkMinus999() {
        LocalDateTime dt = LocalDateTime.of(-999, 1, 1, 0, 0);
        LocalDateTime dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        final Date date = new Date(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusDays(1);
        assertEquals(-1000, dt.getYear());
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
    }

    @Test
    public void checkConversion() throws Exception {
        final LocalDateTime ldt = LocalDateUtil.now();
        assertEquals(ldt.format(DateTimeFormatter.ISO_DATE_TIME), LocalDateUtil.getString(ldt));
        assertEquals(ldt, LocalDateUtil.fromDate(LocalDateUtil.getDate(ldt)));
        assertNotNull(LocalDateUtil.getDate(ldt.toString()));
        assertNotNull(LocalDateUtil.getDate("2017-05-12"));
        assertNotNull(LocalDateUtil.fromMillis(-1));
        assertNotNull(LocalDateUtil.fromDate((Date) null));
        assertNotNull(LocalDateUtil.fromDate((FileTime) null));
        assertNotNull(LocalDateUtil.getDate((LocalDateTime) null));
    }

    @Test
    public void checkConversionFileTime() throws IOException {
        final File file = ResourcesPublicUtilTest.getInstance().getGuidTestPropertiesFile();
        if (file == null) {
            LOGGER.error("CANNOT FIND RESOURCES TEST FILE");
        }
        Assume.assumeTrue("CANNOT FIND RESOURCES TEST FILE", file != null);
        assertNotNull(LocalDateUtil.fromDate(Files.getLastModifiedTime(file.toPath())));
    }

    @Test
    public void getFormattedDateTimeForMongo_tests() {
        assertEquals("2024-12-25T00:00:00.000", LocalDateUtil.getFormattedDateTimeForMongo("25/12/2024"));
        assertEquals("2024-12-25T00:00:00.000", LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25T12"));
        assertEquals("2024-12-25T12:00:00.000", LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25T12:00"));
        assertEquals("2024-12-25T12:34:00.000", LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25T12:34"));
        assertEquals("2024-12-25T12:34:56.000", LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25T12:34:56"));
        assertEquals("2024-12-25T12:34:56.000", LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25T12:34:56PST"));
        assertEquals("2024-12-25T12:34:56.120", LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25T12:34:56.12"));
        assertEquals("2024-12-25T12:34:56.123", LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25T12:34:56.123Z"));
        assertEquals(
            "2024-12-25T12:34:56.123",
            LocalDateUtil.getFormattedDateTimeForMongo("2024-12-25T12:34:56.123PST")
        );
    }

    @Test
    public void getFormattedDateTimeForMongo_tests_2() {
        assertEquals(
            "2024-12-25T12:34:56.123",
            LocalDateUtil.getFormattedDateTimeForMongo(LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123000000))
        );
        assertEquals(
            "2024-12-25T12:34:56.000",
            LocalDateUtil.getFormattedDateTimeForMongo(LocalDateTime.of(2024, 12, 25, 12, 34, 56, 0))
        );
        assertEquals(
            "2024-12-25T12:34:00.000",
            LocalDateUtil.getFormattedDateTimeForMongo(LocalDateTime.of(2024, 12, 25, 12, 34, 0, 0))
        );
        assertEquals(
            "2024-12-25T12:00:00.000",
            LocalDateUtil.getFormattedDateTimeForMongo(LocalDateTime.of(2024, 12, 25, 12, 0, 0, 0))
        );
        assertEquals(
            "2024-12-25T00:00:00.000",
            LocalDateUtil.getFormattedDateTimeForMongo(LocalDateTime.of(2024, 12, 25, 0, 0, 0, 0))
        );
    }

    @Test
    public void getFormattedDateForEsIndexes_tests() {
        assertEquals(
            "20241225_123456",
            LocalDateUtil.getFormattedDateForEsIndexes(LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123000000))
        );
        assertEquals(
            "20241225_123456",
            LocalDateUtil.getFormattedDateForEsIndexes(LocalDateTime.of(2024, 12, 25, 12, 34, 56, 0))
        );
        assertEquals(
            "20241225_123400",
            LocalDateUtil.getFormattedDateForEsIndexes(LocalDateTime.of(2024, 12, 25, 12, 34, 0, 0))
        );
        assertEquals(
            "20241225_120000",
            LocalDateUtil.getFormattedDateForEsIndexes(LocalDateTime.of(2024, 12, 25, 12, 0, 0, 0))
        );
        assertEquals(
            "20241225_000000",
            LocalDateUtil.getFormattedDateForEsIndexes(LocalDateTime.of(2024, 12, 25, 0, 0, 0, 0))
        );
    }

    @Test
    public void parseMongoFormattedDate_tests() {
        assertThat(LocalDateUtil.parseMongoFormattedDate("2024-12-25T12:34:56.123")).isEqualTo(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123000000)
        );
        assertThat(LocalDateUtil.parseMongoFormattedDate("2024-12-25T12:34:56.000")).isEqualTo(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56)
        );
        assertThat(LocalDateUtil.parseMongoFormattedDate("2024-12-25T12:34:56")).isEqualTo(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56)
        );
        assertThat(LocalDateUtil.parseMongoFormattedDate("2024-12-25T12:34")).isEqualTo(
            LocalDateTime.of(2024, 12, 25, 12, 34)
        );
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseMongoFormattedDate("2024-12-25T12"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseMongoFormattedDate("2024-12-25T"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseMongoFormattedDate("2024-12-25"));
    }

    @Test
    public void testMax() {
        LocalDateTime date1 = LocalDateUtil.fromMillis(1234567890L);
        LocalDateTime date2 = LocalDateUtil.fromMillis(1234567891L);

        assertThat(LocalDateUtil.max(date1, date1)).isEqualTo(date1);
        assertThat(LocalDateUtil.max(date2, date1)).isEqualTo(date2);
        assertThat(LocalDateUtil.max(date1, date2)).isEqualTo(date2);
        assertThat(LocalDateUtil.max(null, date1)).isEqualTo(date1);
        assertThat(LocalDateUtil.max(date1, null)).isEqualTo(date1);
        assertThat(LocalDateUtil.max(null, null)).isNull();
    }

    @Test
    public void getString_tests() {
        assertEquals("2024-12-25T00:00:00", LocalDateUtil.getString(LocalDateTime.of(2024, 12, 25, 0, 0, 0)));
        assertEquals("2024-12-25T12:00:00", LocalDateUtil.getString(LocalDateTime.of(2024, 12, 25, 12, 0, 0)));
        assertEquals("2024-12-25T12:34:00", LocalDateUtil.getString(LocalDateTime.of(2024, 12, 25, 12, 34, 0)));
        assertEquals("2024-12-25T12:34:56", LocalDateUtil.getString(LocalDateTime.of(2024, 12, 25, 12, 34, 56)));
        assertEquals(
            "2024-12-25T12:00:00.123",
            LocalDateUtil.getString(LocalDateTime.of(2024, 12, 25, 12, 0, 0, 123000000))
        );
    }

    @Test
    @Ignore("toString should not be used")
    public void toString_tests() {
        assertEquals("2024-12-25T00:00", LocalDateTime.of(2024, 12, 25, 0, 0, 0).toString());
        assertEquals("2024-12-25T12:00", LocalDateTime.of(2024, 12, 25, 12, 0, 0).toString());
        assertEquals("2024-12-25T12:34", LocalDateTime.of(2024, 12, 25, 12, 34, 0).toString());
        assertEquals("2024-12-25T12:34:56", LocalDateTime.of(2024, 12, 25, 12, 34, 56).toString());
        assertEquals("2024-12-25T12:00:00.123", LocalDateTime.of(2024, 12, 25, 12, 0, 0, 123000000).toString());
    }

    @Test
    public void getFormattedSimpleDate_tests() {
        assertEquals("2024-12-25", LocalDateUtil.getFormattedSimpleDate(LocalDate.of(2024, 12, 25)));
    }

    @Test
    @Ignore("toString should not be used")
    public void parseDateTime_tests() {
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 0, 0, 0),
            LocalDateUtil.parseDateTime("2024-12-25T00:00:00.000000000")
        );
        assertEquals(LocalDateTime.of(2024, 12, 25, 0, 0, 0), LocalDateUtil.parseDateTime("2024-12-25T00:00:00"));
        assertEquals(LocalDateTime.of(2024, 12, 25, 0, 0, 0), LocalDateUtil.parseDateTime("2024-12-25T00:00"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T00"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T"));
        assertEquals(LocalDateTime.of(2024, 12, 25, 0, 0, 0), LocalDateUtil.parseDateTime("2024-12-25"));
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123456789),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.123456789")
        );
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123456780),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.12345678")
        );
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123456700),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.1234567")
        );
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123456000),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.123456")
        );
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123450000),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.12345")
        );
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123400000),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.1234")
        );
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 123000000),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.123")
        );
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 120000000),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.12")
        );
        assertEquals(
            LocalDateTime.of(2024, 12, 25, 12, 34, 56, 100000000),
            LocalDateUtil.parseDateTime("2024-12-25T12:34:56.1")
        );
        assertEquals(LocalDateTime.of(2024, 12, 25, 12, 34, 56), LocalDateUtil.parseDateTime("2024-12-25T12:34:56."));
        assertEquals(LocalDateTime.of(2024, 12, 25, 12, 34, 56), LocalDateUtil.parseDateTime("2024-12-25T12:34:56"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T12:34:5"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T12:34:"));
        assertEquals(LocalDateTime.of(2024, 12, 25, 12, 34, 0), LocalDateUtil.parseDateTime("2024-12-25T12:34"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T12:3"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T12:"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T12"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T1"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-25T"));
        assertEquals(LocalDateTime.of(2024, 12, 25, 0, 0, 0), LocalDateUtil.parseDateTime("2024-12-25"));
        assertThrows(DateTimeParseException.class, () -> LocalDateUtil.parseDateTime("2024-12-2"));
    }
}
