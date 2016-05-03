/**
 * This file is part of Vitam Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * All Vitam Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Vitam . If not, see <http://www.gnu.org/licenses/>.
 */

package fr.gouv.vitam.common;

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * LocalDateTime utils
 *
 */
public class LocalDateUtil {

    /**
     * 
     * @return the LocalDateTime now in UTC
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * @param date
     * @return the corresponding Date from date string
     */
    public static Date getDate(String date) {
        return getDate(LocalDateTime.parse(date));
    }

    /**
     * @param millis
     * @return the corresponding LocalDateTime in UTC
     */
    public static LocalDateTime fromMillis(long millis) {
        if (millis < 0) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofEpochSecond(millis / 1000, (int) ((millis % 1000) * 1000),
                ZoneOffset.UTC);
    }

    /**
     * @param ldt
     * @return the millis in epoch
     */
    public static long getMillis(LocalDateTime ldt) {
        return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * @param date
     * @return the corresponding LocalDateTime in UTC
     */
    public static LocalDateTime fromDate(Date date) {
        if (date == null) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    /**
     * @param fileTime
     * @return the corresponding LocalDateTime in UTC
     */
    public static LocalDateTime fromDate(FileTime fileTime) {
        if (fileTime == null) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC);
    }

    /**
     * @param ldt
     * @return the corresponding date
     */
    public static Date getDate(LocalDateTime ldt) {
        if (ldt == null) {
            return new Date();
        }
        return Date.from(ldt.toInstant(ZoneOffset.UTC));
    }

    private LocalDateUtil() {
    }

}
