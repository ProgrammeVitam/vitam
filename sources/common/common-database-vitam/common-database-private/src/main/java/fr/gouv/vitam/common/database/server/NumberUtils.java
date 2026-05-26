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

package fr.gouv.vitam.common.database.server;

public final class NumberUtils {

    private NumberUtils() {
        // Utility class
    }

    public static Number max(final Number left, final Number right) {
        validate(left);
        validate(right);

        if (isDecimal(left) || isDecimal(right)) {
            return narrowDecimal(Math.max(left.doubleValue(), right.doubleValue()));
        }

        return narrowInteger(Math.max(left.longValue(), right.longValue()));
    }

    public static Number min(final Number left, final Number right) {
        validate(left);
        validate(right);

        if (isDecimal(left) || isDecimal(right)) {
            return narrowDecimal(Math.min(left.doubleValue(), right.doubleValue()));
        }

        return narrowInteger(Math.min(left.longValue(), right.longValue()));
    }

    public static Number add(final Number left, final Number right) {
        validate(left);
        validate(right);

        if (isDecimal(left) || isDecimal(right)) {
            return narrowDecimal(left.doubleValue() + right.doubleValue());
        }

        return narrowInteger(left.longValue() + right.longValue());
    }

    public static boolean isDecimal(final Number number) {
        return number instanceof Float || number instanceof Double;
    }

    public static Number narrowInteger(final long value) {
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return (int) value;
        }

        return value;
    }

    public static Number narrowDecimal(final double value) {
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE && value == (int) value) {
            return (int) value;
        }

        if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE && value == (long) value) {
            return (long) value;
        }

        return value;
    }

    private static void validate(final Number number) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }
    }
}
