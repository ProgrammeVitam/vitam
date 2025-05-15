/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

package fr.gouv.vitam.common.date.converter.strategy;

import fr.gouv.vitam.common.date.converter.DateFormatUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;

public class DefaultMongoFormatterStrategy implements DateFormatterStrategy {

    @Override
    public String format(TemporalAccessor date) {
        if (date instanceof LocalDate ld) {
            return ld.format(DateFormatUtil.ISO_DATE);
        }

        if (date instanceof LocalDateTime ldt) {
            return ldt.getNano() > 0
                ? ldt.truncatedTo(ChronoUnit.MILLIS).format(DateFormatUtil.ISO_DATE_TIME_MILLIS)
                : ldt.format(DateFormatUtil.ISO_DATE_TIME);
        }

        if (date instanceof OffsetDateTime odt) {
            OffsetDateTime normalized = odt;

            boolean isMidnight =
                odt.getHour() == 0 && odt.getMinute() == 0 && odt.getSecond() == 0 && odt.getNano() == 0;
            boolean isUTC = odt.getOffset().equals(ZoneOffset.UTC);

            // ➤ Case 1: pure date (T00:00:00Z → display only the date without the Z)
            if (isMidnight && isUTC) {
                return odt.toLocalDate().format(DateFormatUtil.ISO_DATE);
            }

            // ➤ Case 2: date + time with millis
            if (odt.getNano() > 0) {
                return normalized
                    .truncatedTo(ChronoUnit.MILLIS)
                    .format(isUTC ? DateFormatUtil.ISO_DATE_TIME_MILLIS : DateFormatUtil.ISO_DATE_TIME_OFFSET_MILLIS);
            }

            // ➤ Case 3: date + time without millis
            return normalized.format(isUTC ? DateFormatUtil.ISO_DATE_TIME : DateFormatUtil.ISO_DATE_TIME_OFFSET);
        }

        if (date instanceof ZonedDateTime zdt) {
            return format(zdt.toOffsetDateTime());
        }

        throw new IllegalArgumentException("Unsupported TemporalAccessor type: " + date.getClass());
    }
}
