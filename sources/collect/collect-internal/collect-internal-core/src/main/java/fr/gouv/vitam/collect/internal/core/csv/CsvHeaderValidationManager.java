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

package fr.gouv.vitam.collect.internal.core.csv;

import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidCsvFormatException;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static fr.gouv.vitam.collect.internal.core.csv.CsvHelper.sanitizeStringForLog;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.MAX_HEADER_NAME_LENGTH;
import static java.util.function.Predicate.not;

public class CsvHeaderValidationManager implements AutoCloseable {

    private final LinkedHashSet<String> headerNames;
    private final Set<String> invalidHeaderNames = new HashSet<>();
    private final CsvErrorAccumulator errorAccumulator = new CsvErrorAccumulator();

    public CsvHeaderValidationManager(List<String> headerNames) {
        this.headerNames = new LinkedHashSet<>(headerNames);
    }

    public Iterable<String> getRemainingHeaderNamesToValidate() {
        return () -> headerNames.stream().filter(not(invalidHeaderNames::contains)).iterator();
    }

    public Iterable<String> getRemainingHeaderNamesToValidate(Predicate<String> filter) {
        return () -> headerNames.stream().filter(not(invalidHeaderNames::contains)).filter(filter).iterator();
    }

    public Iterable<String> getRemainingHeaderNamesToValidateByPrefix(String prefix) {
        return getRemainingHeaderNamesToValidate(headerName -> CsvMetadataUtils.equalsOrStartsWith(headerName, prefix));
    }

    public Iterable<String> getRemainingContentHeaderNamesToValidate() {
        return getRemainingHeaderNamesToValidate(CsvMetadataUtils::isContentField);
    }

    public Iterable<String> getRemainingMainContentHeaderNamesToValidate() {
        return getRemainingHeaderNamesToValidate(
            headerName ->
                !CsvMetadataUtils.isContentTitleField(headerName) &&
                !CsvMetadataUtils.isContentDescriptionField(headerName)
        );
    }

    public Iterable<String> getRemainingManagementHeaderNamesToValidate() {
        return getRemainingHeaderNamesToValidate(CsvMetadataUtils::isManagementField);
    }

    public boolean containsHeaderName(String headerName) {
        return headerNames.contains(headerName);
    }

    public void report(String headerName, String message) throws CollectInvalidCsvFormatException {
        if (!headerNames.contains(headerName)) {
            throw new IllegalStateException("Invalid header name " + headerName + " (msg=" + message + ")");
        }
        invalidHeaderNames.add(headerName);
        errorAccumulator.report(
            "Invalid header name '" + sanitizeStringForLog(headerName, MAX_HEADER_NAME_LENGTH) + "': " + message
        );
    }

    @Override
    public void close() throws CollectInvalidCsvFormatException {
        this.errorAccumulator.close();
    }
}
