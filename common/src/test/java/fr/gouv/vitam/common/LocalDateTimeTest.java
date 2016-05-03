/**
 * This file is part of Vitam Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Vitam Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Vitam . If not, see
 * <http://www.gnu.org/licenses/>.
 */

package fr.gouv.vitam.common;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.Test;

import fr.gouv.vitam.common.LocalDateUtil;

/**
 * 
 *
 */
@SuppressWarnings("javadoc")
public class LocalDateTimeTest {

    @Test
    public void check1970() {
        LocalDateTime dt = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime dt0 = LocalDateUtil.fromMillis(0);
        System.out.println(dt+" vs "+dt0);
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        dt = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        Date date = new Date(0);
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
        Date date = new Date(dt.toEpochSecond(ZoneOffset.UTC));
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
        Date date = new Date(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusDays(1);
        // should be -1, not 0
        assertEquals(0, dt.getYear());
        System.out.println("Year 0 but -1: "+dt.toString());
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusYears(1);
        // should be -2, not -1
        assertEquals(-1, dt.getYear());
        System.out.println("Year -1 but -2: "+dt.toString());
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
        Date date = new Date(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
        dt = dt.minusDays(1);
        assertEquals(-1000, dt.getYear());
        dt0 = LocalDateTime.parse(dt.toString());
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), dt0.toEpochSecond(ZoneOffset.UTC));
        date.setTime(dt.toEpochSecond(ZoneOffset.UTC));
        assertEquals(dt.toEpochSecond(ZoneOffset.UTC), date.getTime());
    }
}
