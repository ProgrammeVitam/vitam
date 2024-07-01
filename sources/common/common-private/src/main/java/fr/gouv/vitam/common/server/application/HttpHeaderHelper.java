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
package fr.gouv.vitam.common.server.application;

import com.google.common.base.Joiner;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class helper to manage specifics Vitam headers
 */

// TODO P1: This is a copy of the api-design module header management. In another item we should refactor both to make
// this
// http header management common vitam.

public final class HttpHeaderHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HttpHeaderHelper.class);

    private static final String CASE_INSENSITIVE = "(?i)";

    private HttpHeaderHelper() {
        // Nothing
    }

    /**
     * Get header values from {@link HttpHeaders} for {@link VitamHttpHeader}
     *
     * @param headers the headers list
     * @param name the {@link VitamHttpHeader} with wanted header name
     * @return the list of values for specified header name
     * @throws IllegalArgumentException if headers or name is null
     */
    public static List<String> getHeaderValues(HttpHeaders headers, VitamHttpHeader name) {
        ParametersChecker.checkParameter("Name cannot be null", name);
        return getHeaderValues(headers, name.getName());
    }

    /**
     * Retrieve header values from {@link HttpHeaders} for {@link VitamHttpHeader}
     *
     * @param headers the headers list
     * @param name the header name
     * @return the list of values for specified header name
     * @throws IllegalArgumentException if headers is null or name is null or empty
     */
    public static List<String> getHeaderValues(HttpHeaders headers, String name) {
        ParametersChecker.checkParameter("Name cannot be null", name);
        ParametersChecker.checkParameter("Headers cannot be null", headers);
        return headers.getRequestHeader(name);
    }

    /**
     * Check if headers are declared
     *
     * @param headers the headers list to check
     * @param vitamHeader the header to retreive
     * @return true if the header is defined, false otherwise
     * @throws IllegalArgumentException if headers or vitamHeader is null
     */
    public static boolean hasValuesFor(HttpHeaders headers, VitamHttpHeader vitamHeader) {
        ParametersChecker.checkParameter("Header name cannot be null", vitamHeader);
        ParametersChecker.checkParameter("Headers cannot be null", headers);
        final List<String> values = headers.getRequestHeader(vitamHeader.getName());
        return values != null && !values.isEmpty();
    }

    /**
     * Check specific vitam headers values with regular expression from the (define in {@link VitamHttpHeader}) At the
     * first wrong value, treatment stops and throws an {@link IllegalStateException} Note that, the regular expression
     * is case insensitive.
     *
     * @param headers HTTP headers list to check
     * @throws IllegalStateException when a header value doesn't match with teh defined regular expression
     * @throws IllegalArgumentException if headers is null
     */
    public static void checkVitamHeaders(HttpHeaders headers) {
        ParametersChecker.checkParameter("Headers cannot be null", headers);
        final MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        checkVitamHeadersMap(requestHeaders);
    }

    /**
     * Check specific vitam headers values with regular expression from the (define in {@link VitamHttpHeader}) At the
     * first wrong value, treatment stops and throws an {@link IllegalStateException} Note that, the regular expression
     * is case insensitive.
     *
     * @param requestHeaders HTTP headers list to check
     * @throws IllegalStateException when a header value doesn't match with the defined regular expression
     */
    public static void checkVitamHeadersMap(MultivaluedMap<String, String> requestHeaders) {
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            for (final VitamHttpHeader vitamHttpHeader : VitamHttpHeader.values()) {
                final List<String> values = requestHeaders.get(vitamHttpHeader.getName());
                if (
                    values != null &&
                    !values.stream().anyMatch(value -> value.matches(CASE_INSENSITIVE + vitamHttpHeader.getRegExp()))
                ) {
                    throw new IllegalStateException(
                        String.format("%s header has wrong value", vitamHttpHeader.getName())
                    );
                }
            }
        }
    }

    /**
     * Validate HTTP header values. If header is known by the {@link VitamHttpHeader} then check if format matches with
     * the defined regular expression. Also check the wantedHeaders map values with {@link VitamHttpHeader}. Values of
     * headers (from HTTP or from wanted list) can not be null (throw an exception).
     *
     * In case of error, the exception contains all errors in its message.
     *
     * @param headers the headers list to validate
     * @param wantedHeaders the map representing wanted header values with the key for header name and the value (list)
     * for the wanted values for this specific header name
     * @throws IllegalArgumentException if headers is null
     * @throws IllegalStateException if one or more header values does not equal to wanted values, header values do not
     * match with the defined regular expression, wanted values for a header are null, header values are null.
     * This exception contains all errors in its message.
     */
    public static void validateHeaderValue(HttpHeaders headers, MultivaluedHashMap<String, String> wantedHeaders) {
        ParametersChecker.checkParameter("Headers cannot be null", headers);

        final List<String> errorDetails = new ArrayList<>();

        for (final String wantedHeaderName : wantedHeaders.keySet()) {
            final List<String> wantedValues = toLowerCaseList(wantedHeaders.get(wantedHeaderName));
            final VitamHttpHeader vitamHeader = VitamHttpHeader.get(wantedHeaderName);
            final List<String> headersValues = toLowerCaseList(headers.getRequestHeader(wantedHeaderName));
            List<String> tmpHeadersValues;
            final List<String> tmpWantedValues = toLowerCaseList(wantedValues);

            if (headersValues.isEmpty()) {
                errorDetails.add(String.format("Header %s values are null", wantedHeaderName));
            } else {
                tmpHeadersValues = toLowerCaseList(headersValues);
                if (vitamHeader != null) {
                    errorDetails.addAll(
                        collectNonMatchingItems(
                            vitamHeader,
                            wantedValues,
                            wantedHeaderName,
                            "Wanted " + "value %s for header %s does not match with define regular %s"
                        )
                    );
                    errorDetails.addAll(
                        collectNonMatchingItems(
                            vitamHeader,
                            headersValues,
                            wantedHeaderName,
                            "Found " + "value %s for header %s does not match with define regular %s"
                        )
                    );
                }
                tmpHeadersValues.removeAll(wantedValues);
                tmpWantedValues.removeAll(headersValues);
                if (!tmpHeadersValues.isEmpty()) {
                    // error ?
                    LOGGER.warn(
                        "Some values ({}) from header {} were not check (not asked)",
                        Joiner.on(", ").join(tmpHeadersValues),
                        wantedHeaderName
                    );
                }
            }
            if (!tmpWantedValues.isEmpty()) {
                errorDetails.add(
                    String.format(
                        "Some WANTED values from header %s were not found (%s)",
                        wantedHeaderName,
                        Joiner.on(", ").join(tmpWantedValues)
                    )
                );
            }
        }
        if (!errorDetails.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("There are %d errors %n%s", errorDetails.size(), Joiner.on("\n").join(errorDetails))
            );
        }
    }

    /**
     * Transform string in input list to their lowercase value
     *
     * @param list a list of strings to be lowercase
     * @return a list of string containing all elements of the input list but each elements is in lowercase
     */
    private static List<String> toLowerCaseList(List<String> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    private static List<String> collectNonMatchingItems(
        VitamHttpHeader vitamHeader,
        List<String> valuesTocheck,
        String wantedHeaderName,
        String messageTemplate
    ) {
        final List<String> result = new ArrayList<>();
        if (vitamHeader != null && valuesTocheck != null) {
            result.addAll(
                valuesTocheck
                    .stream()
                    .filter(headerValue -> !headerValue.matches(CASE_INSENSITIVE + vitamHeader.getRegExp()))
                    .map(
                        headerValue ->
                            String.format(messageTemplate, headerValue, wantedHeaderName, vitamHeader.getRegExp())
                    )
                    .collect(Collectors.toList())
            );
        }
        return result;
    }
}
