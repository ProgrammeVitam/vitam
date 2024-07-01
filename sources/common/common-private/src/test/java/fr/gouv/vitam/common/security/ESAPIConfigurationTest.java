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
package fr.gouv.vitam.common.security;

import org.junit.Test;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.ValidationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ESAPIConfigurationTest {

    private static final String ESAPI_HTTPQUERYSTRING = "HTTPQueryString";
    private static final String ESAPI_HTTPHEADERVALUE = "HTTPHeaderValue";
    private static final String INVALID_MSG = "Invalid input";

    @Test
    public void givenStringContainsValidQueryString() throws Exception {
        final String goodUrl1 = "/resources";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput("HTTP query string: " + goodUrl1, goodUrl1, ESAPI_HTTPQUERYSTRING, 2000, true)
        ).doesNotThrowAnyException();

        final String goodUrl2 = "/resources/54554-5454545";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput("HTTP query string: " + goodUrl2, goodUrl2, ESAPI_HTTPQUERYSTRING, 2000, true)
        ).doesNotThrowAnyException();

        final String goodUrl3 = "/resources/./54554-5454545";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput("HTTP query string: " + goodUrl3, goodUrl3, ESAPI_HTTPQUERYSTRING, 2000, true)
        ).doesNotThrowAnyException();

        final String goodUrl4 = "/";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput("HTTP query string: " + goodUrl4, goodUrl4, ESAPI_HTTPQUERYSTRING, 2000, true)
        ).doesNotThrowAnyException();

        final String goodUrl5 = ".";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput("HTTP query string: " + goodUrl5, goodUrl5, ESAPI_HTTPQUERYSTRING, 2000, true)
        ).doesNotThrowAnyException();
    }

    @Test
    public void givenStringContainsInvalidQueryString() throws Exception {
        final String badUrl1 = "..";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput("HTTP query string: " + badUrl1, badUrl1, ESAPI_HTTPQUERYSTRING, 2000, true)
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badUrl1);

        final String badUrl2 = "/resources/..";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput("HTTP query string: " + badUrl2, badUrl2, ESAPI_HTTPQUERYSTRING, 2000, true)
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badUrl2);

        final String badUrl3 = "../parent";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput("HTTP query string: " + badUrl3, badUrl3, ESAPI_HTTPQUERYSTRING, 2000, true)
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badUrl3);

        final String badUrl4 = "/resources/../parent";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput("HTTP query string: " + badUrl4, badUrl4, ESAPI_HTTPQUERYSTRING, 2000, true)
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badUrl4);

        final String badUrl5 = "resources..parent";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput("HTTP query string: " + badUrl5, badUrl5, ESAPI_HTTPQUERYSTRING, 2000, true)
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badUrl5);
    }

    @Test
    public void givenStringContainsValidHeaderValueString() throws Exception {
        final String goodHeader1 = "value";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput(
                        "HTTP Header value string: " + goodHeader1,
                        goodHeader1,
                        ESAPI_HTTPHEADERVALUE,
                        2000,
                        true
                    )
        ).doesNotThrowAnyException();

        final String goodHeader2 = "/resources";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput(
                        "HTTP Header value string: " + goodHeader2,
                        goodHeader2,
                        ESAPI_HTTPHEADERVALUE,
                        2000,
                        true
                    )
        ).doesNotThrowAnyException();

        final String goodHeader3 = "/resources/./54554-5454545";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput(
                        "HTTP Header value string: " + goodHeader3,
                        goodHeader3,
                        ESAPI_HTTPHEADERVALUE,
                        2000,
                        true
                    )
        ).doesNotThrowAnyException();

        final String goodHeader4 = "./";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput(
                        "HTTP Header value string: " + goodHeader4,
                        goodHeader4,
                        ESAPI_HTTPHEADERVALUE,
                        2000,
                        true
                    )
        ).doesNotThrowAnyException();

        final String goodHeader5 = ".";
        assertThatCode(
            () ->
                ESAPI.validator()
                    .getValidInput(
                        "HTTP Header value string: " + goodHeader5,
                        goodHeader5,
                        ESAPI_HTTPHEADERVALUE,
                        2000,
                        true
                    )
        ).doesNotThrowAnyException();
    }

    @Test
    public void givenStringContainsInvalidHeaderValueString() throws Exception {
        final String badUrl1 = "..";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput(
                            "HTTP Header value string: " + badUrl1,
                            badUrl1,
                            ESAPI_HTTPHEADERVALUE,
                            2000,
                            true
                        )
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badUrl1);

        final String badUrl2 = "/resources/..";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput(
                            "HTTP Header value string: " + badUrl2,
                            badUrl2,
                            ESAPI_HTTPHEADERVALUE,
                            2000,
                            true
                        )
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badUrl2);

        final String badUrl3 = "../parent";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput(
                            "HTTP Header value string: " + badUrl3,
                            badUrl3,
                            ESAPI_HTTPHEADERVALUE,
                            2000,
                            true
                        )
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badUrl3);

        final String badHeader4 = "/////..";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput(
                            "HTTP Header value string: " + badHeader4,
                            badHeader4,
                            ESAPI_HTTPHEADERVALUE,
                            2000,
                            true
                        )
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badHeader4);

        final String badHeader5 = "value..hack";
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(
                () ->
                    ESAPI.validator()
                        .getValidInput(
                            "HTTP Header value string: " + badHeader5,
                            badHeader5,
                            ESAPI_HTTPHEADERVALUE,
                            2000,
                            true
                        )
            )
            .withMessageContaining(INVALID_MSG)
            .withMessageContaining(badHeader5);
    }
}
