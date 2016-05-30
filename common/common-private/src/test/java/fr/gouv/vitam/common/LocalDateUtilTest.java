/**
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
package fr.gouv.vitam.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.Assume;
import org.junit.Test;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 *
 *
 */
@SuppressWarnings("javadoc")
public class LocalDateUtilTest {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(LocalDateUtilTest.class);

    @Test
    public void check1970() {
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
    public void checkConversion() throws IOException {
        final LocalDateTime ldt = LocalDateUtil.now();
        assertEquals(ldt, LocalDateUtil.fromDate(LocalDateUtil.getDate(ldt)));
        assertNotNull(LocalDateUtil.fromMillis(LocalDateUtil.getMillis(ldt)));
        assertNotNull(LocalDateUtil.getDate(ldt.toString()));
        assertNotNull(LocalDateUtil.fromMillis(-1));
        assertNotNull(LocalDateUtil.fromDate((Date) null));
        assertNotNull(LocalDateUtil.fromDate((FileTime) null));
        assertNotNull(LocalDateUtil.getDate((LocalDateTime) null));
    }

    @Test
    public void checkConversionFileTime() throws IOException {
        final File file = ResourcesPrivateUtilTest.getInstance().getServerIdentityPropertiesFile();
        if (file == null) {
            LOGGER.error(ResourcesPrivateUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE);
        }
        Assume.assumeTrue(ResourcesPrivateUtilTest.CANNOT_FIND_RESOURCES_TEST_FILE, file != null);
        assertNotNull(LocalDateUtil.fromDate(Files.getLastModifiedTime(file.toPath())));
    }
}
