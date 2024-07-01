/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Parameters Checker Test
 */
public class ParametersCheckerTest {

    /**
     * Test method for
     * {@link fr.gouv.vitam.common.ParametersChecker#checkParameter(java.lang.String, java.lang.String[])}.
     */
    @Test
    public final void testCheckParamaterStringStringArray() {
        try {
            ParametersChecker.checkParameter("test message", (String[]) null);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameter("test message", null, "notnull");
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameter("test message", "notnull", null);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameter("test message", "", "notnull");
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameter("test message", "notnull", "");
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameter("test message", "notNull", "notnull");
            ParametersChecker.checkParameter("test message", "notnull");
        } catch (final IllegalArgumentException e) { // NOSONAR
            fail(ResourcesPublicUtilTest.SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        }
    }

    /**
     * Test method for
     * {@link fr.gouv.vitam.common.ParametersChecker#checkParameter(java.lang.String, java.lang.String[])}.
     */
    @Test
    public final void testCheckParamaterNullOnlyStringStringArray() {
        try {
            ParametersChecker.checkParameterNullOnly("test message", (String[]) null);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameterNullOnly("test message", null, "notnull");
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameterNullOnly("test message", "notnull", null);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameterNullOnly("test message", "", "notnull");
        } catch (final IllegalArgumentException e) { // NOSONAR
            fail(ResourcesPublicUtilTest.SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        }
        try {
            ParametersChecker.checkParameterNullOnly("test message", "notnull", "");
        } catch (final IllegalArgumentException e) { // NOSONAR
            fail(ResourcesPublicUtilTest.SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        }
        try {
            ParametersChecker.checkParameterNullOnly("test message", "notNull", "notnull");
            ParametersChecker.checkParameterNullOnly("test message", "notnull");
        } catch (final IllegalArgumentException e) { // NOSONAR
            fail(ResourcesPublicUtilTest.SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        }
    }

    /**
     * Test method for
     * {@link fr.gouv.vitam.common.ParametersChecker#checkParameter(java.lang.String, java.lang.Object[])}.
     */
    @Test
    public final void testCheckParamaterStringObjectArray() {
        try {
            ParametersChecker.checkParameter("test message", (Object[]) null);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        final List<String> list = new ArrayList<>();
        try {
            ParametersChecker.checkParameter("test message", null, list);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameter("test message", list, null);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) {} // NOSONAR
        try {
            ParametersChecker.checkParameter("test message", list, list);
            ParametersChecker.checkParameter("test message", list);
        } catch (final IllegalArgumentException e) { // NOSONAR
            fail(ResourcesPublicUtilTest.SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        }
    }

    @Test
    public final void testCheckValue() {
        try {
            ParametersChecker.checkValue("test", 1, 2);
            fail(ResourcesPublicUtilTest.SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION);
        } catch (final IllegalArgumentException e) { // NOSONAR
            // ok
        }
        ParametersChecker.checkValue("test", 1, 1);
        ParametersChecker.checkValue("test", 1, 0);
    }
}
