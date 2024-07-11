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
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        LOGGER.info("Year 0 but -1: " + dt.toString());
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusYears(1);
        // should be -2, not -1
        assertEquals(-1, dt.getYear());
        LOGGER.info("Year -1 but -2: " + dt.toString());
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        assertTrue(LocalDateUtil.getFormattedDate(date).contains("T"));
        assertTrue(LocalDateUtil.getFormattedDate(date).contains("+"));
        assertTrue(LocalDateUtil.getFormattedDate(LocalDateUtil.now()).length() == 19);
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
            LOGGER.error(ResourcesPublicUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE);
        }
        Assume.assumeTrue(ResourcesPublicUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE, file != null);
        assertNotNull(LocalDateUtil.fromDate(Files.getLastModifiedTime(file.toPath())));
    }

    @Test
    public void checkFormattedDateForMongo() {
        String date = "2016-09-27T15:37:53";
        String dateFormatted = "2016-09-27T15:37:53.000";
        String dateNoTime = "2016-09-27";
        String dateNoTimeFormatted = "2016-09-27T00:00:00.000";
        String dateWithHAndM = "2016-09-27T15:37";
        String dateWithHAndMFormatted = "2016-09-27T15:37:00.000";
        String dateWithMillis = "2016-09-27T15:37:53.54";
        String dateWithMillisFormatted = "2016-09-27T15:37:53.540";
        String dateWithMillisZone = "2016-09-27T15:37:53.548Z";
        String dateWithMillisZoneFormatted = "2016-09-27T15:37:53.548";
        String dateWithMillisZonePST = "2016-09-27T15:37:53.548PST";
        String dateWithMillisZonePSTFormatted = "2016-09-27T15:37:53.548";
        String dateWithMillisZonePSTNoMillis = "2016-09-27T15:37:53PST";
        String dateWithMillisZonePSTNoMillisFormatted = "2016-09-27T15:37:53.000";

        String slashedDate = "27/09/2016";
        String slashedDateFormatted = "2016-09-27T00:00:00.000";

        assertEquals(dateFormatted, LocalDateUtil.getFormattedDateForMongo(date));
        assertEquals(dateNoTimeFormatted, LocalDateUtil.getFormattedDateForMongo(dateNoTime));
        assertEquals(dateWithHAndMFormatted, LocalDateUtil.getFormattedDateForMongo(dateWithHAndM));
        assertEquals(dateWithMillisFormatted, LocalDateUtil.getFormattedDateForMongo(dateWithMillis));
        assertEquals(dateWithMillisZoneFormatted, LocalDateUtil.getFormattedDateForMongo(dateWithMillisZone));
        assertEquals(dateWithMillisZonePSTFormatted, LocalDateUtil.getFormattedDateForMongo(dateWithMillisZonePST));
        assertEquals(
            dateWithMillisZonePSTNoMillisFormatted,
            LocalDateUtil.getFormattedDateForMongo(dateWithMillisZonePSTNoMillis)
        );

        assertEquals(slashedDateFormatted, LocalDateUtil.getFormattedDateForMongo(slashedDate));
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
}
