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

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * LocalDateTime utilities
 *
 */
public final class LocalDateUtil {

    /**
     *
     * @return the LocalDateTime now in UTC
     */
    public static final LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * @param date
     * @return the corresponding Date from date string
     * @throws IllegalArgumentException date null or empty
     */
    public static final Date getDate(String date) {
        ParametersChecker.checkParameter("Date", date);
        return getDate(LocalDateTime.parse(date));
    }

    /**
     * @param millis
     * @return the corresponding LocalDateTime in UTC
     */
    public static final LocalDateTime fromMillis(long millis) {
        if (millis < 0) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofEpochSecond(millis / 1000, (int) (millis % 1000 * 1000),
            ZoneOffset.UTC);
    }

    /**
     * @param ldt
     * @return the millis in epoch
     * @throws IllegalArgumentException ldt null or empty
     */
    public static final long getMillis(LocalDateTime ldt) {
        ParametersChecker.checkParameter("LocalDateTime", ldt);
        return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * @param date
     * @return the corresponding LocalDateTime in UTC
     */
    public static final LocalDateTime fromDate(Date date) {
        if (date == null) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    /**
     * @param fileTime
     * @return the corresponding LocalDateTime in UTC
     */
    public static final LocalDateTime fromDate(FileTime fileTime) {
        if (fileTime == null) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC);
    }

    /**
     * @param ldt
     * @return the corresponding date
     */
    public static final Date getDate(LocalDateTime ldt) {
        if (ldt == null) {
            return new Date();
        }
        return Date.from(ldt.toInstant(ZoneOffset.UTC));
    }

    private LocalDateUtil() {
        // empty
    }

}
