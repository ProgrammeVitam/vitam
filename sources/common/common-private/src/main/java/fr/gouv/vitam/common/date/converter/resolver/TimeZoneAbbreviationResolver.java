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

import fr.gouv.vitam.common.LocalDateUtil;

import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.Map;

public class TimeZoneAbbreviationResolver implements DateResolver {

    private static final Map<String, String> SHORT_IDS = ZoneId.SHORT_IDS;

    @Override
    public boolean supports(String input) {
        // Matches a string that ends with a space followed by a timezone abbreviation (e.g., "2024-03-26T12:00 PST")
        return input.matches("[^\\s]*\\s[A-Z]{2,4}$");
    }

    @Override
    public TemporalAccessor resolve(String input) {
        String[] parts = input.trim().split(" ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid format with timezone abbreviation: " + input);
        }

        String dateTimePart = parts[0];
        String abbreviation = parts[1];

        ZoneId zoneId = SHORT_IDS.containsKey(abbreviation)
            ? ZoneId.of(SHORT_IDS.get(abbreviation))
            : ZoneId.of(abbreviation);

        if (dateTimePart.contains("T")) {
            LocalDateTime ldt = LocalDateUtil.parseMongoFormattedDate(dateTimePart);
            return ldt.atZone(zoneId);
        } else {
            LocalDate ld = LocalDate.parse(dateTimePart);
            return ld.atStartOfDay(zoneId);
        }
    }
}
