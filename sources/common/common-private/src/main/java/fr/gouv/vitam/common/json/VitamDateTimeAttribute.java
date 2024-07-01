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
package fr.gouv.vitam.common.json;

import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.google.common.collect.ImmutableList;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

import java.util.List;

import static org.joda.time.DateTimeFieldType.dayOfMonth;
import static org.joda.time.DateTimeFieldType.hourOfDay;
import static org.joda.time.DateTimeFieldType.millisOfSecond;
import static org.joda.time.DateTimeFieldType.minuteOfHour;
import static org.joda.time.DateTimeFieldType.monthOfYear;
import static org.joda.time.DateTimeFieldType.secondOfMinute;
import static org.joda.time.DateTimeFieldType.year;

/**
 * Vitam version of Validator for the {@code date-time-vitam} format attribute
 */
public final class VitamDateTimeAttribute extends AbstractFormatAttribute {

    private static final List<String> FORMATS = ImmutableList.of("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter FORMATTER;

    static {
        final DateTimeParser msParser = new DateTimeFormatterBuilder()
            .appendLiteral('.')
            .appendDecimal(millisOfSecond(), 1, 3)
            .toParser();
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder = builder
            .appendFixedDecimal(year(), 4)
            .appendLiteral('-')
            .appendFixedDecimal(monthOfYear(), 2)
            .appendLiteral('-')
            .appendFixedDecimal(dayOfMonth(), 2)
            .appendLiteral('T')
            .appendFixedDecimal(hourOfDay(), 2)
            .appendLiteral(':')
            .appendFixedDecimal(minuteOfHour(), 2)
            .appendLiteral(':')
            .appendFixedDecimal(secondOfMinute(), 2)
            .appendOptional(msParser);
        FORMATTER = builder.toFormatter();
    }

    private static final FormatAttribute INSTANCE = new VitamDateTimeAttribute();

    public static FormatAttribute getInstance() {
        return INSTANCE;
    }

    private VitamDateTimeAttribute() {
        super("date-time-vitam", NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
        throws ProcessingException {
        final String value = data.getInstance().getNode().textValue();
        try {
            FORMATTER.parseDateTime(value);
        } catch (IllegalArgumentException ignored) {
            report.error(
                newMsg(data, bundle, "err.format.invalidDate")
                    .putArgument("value", value)
                    .putArgument("expected", FORMATS)
            );
        }
    }
}
