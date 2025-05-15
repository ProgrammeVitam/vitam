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

package fr.gouv.vitam.common.date.converter.resolver;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OffsetOnlyResolver implements DateResolver {

    @Override
    public boolean supports(String input) {
        // Matches a date with an optional leading sign, followed by yyyy-MM-dd and a timezone offset (e.g., 2024-03-26+01:00 or -0450-12-01-08:00)
        return input.matches("^[+-]?\\d{4,}-\\d{2}-\\d{2}[+-]\\d{2}:\\d{2}$");
    }

    @Override
    public TemporalAccessor resolve(String input) {
        Pattern pattern = Pattern.compile("^([+-]?\\d{4,}-\\d{2}-\\d{2})([+-]\\d{2}:\\d{2})$");
        Matcher matcher = pattern.matcher(input);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid date format for OffsetOnlyResolver: " + input);
        }

        String datePart = matcher.group(1); // e.g. -24581-10-12
        String offsetPart = matcher.group(2); // e.g. +01:00

        LocalDate date = LocalDate.parse(datePart);
        ZoneOffset offset = ZoneOffset.of(offsetPart);

        return OffsetDateTime.of(date, LocalTime.MIDNIGHT, offset);
    }
}
