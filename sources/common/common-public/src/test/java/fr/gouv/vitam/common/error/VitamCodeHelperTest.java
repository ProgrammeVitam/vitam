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

package fr.gouv.vitam.common.error;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VitamCodeHelperTest {

    @Test
    public void isAlphanumericCodeTest() {
        assertFalse(VitamCodeHelper.isAlphanumericCode("ee"));
        assertFalse(VitamCodeHelper.isAlphanumericCode("0-"));
        assertFalse(VitamCodeHelper.isAlphanumericCode("A*"));
        assertTrue(VitamCodeHelper.isAlphanumericCode("00"));
        assertTrue(VitamCodeHelper.isAlphanumericCode("0F"));
        assertTrue(VitamCodeHelper.isAlphanumericCode("ZO"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFromStringEmptyParameterTest() {
        VitamCodeHelper.getFrom("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFromStringNullParameterTest() {
        VitamCodeHelper.getFrom(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFromStringNotFoundTest() {
        VitamCodeHelper.getFrom("ZZZZZZ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFromStringWrongCodeSizeTest() {
        VitamCodeHelper.getFrom("00");
    }

    @Test
    public void getFromStringTest() {
        final VitamCode code = VitamCodeHelper.getFrom("000000");
        assertNotNull(code);
        assertEquals(VitamCode.TEST, code);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFromIllegalExceptionTest() {
        VitamCodeHelper.getFrom(ServiceName.VITAM, DomainName.TEST, "ZZ");
    }

    @Test
    public void getMessageWrongParamters() {
        try {
            VitamCodeHelper.getMessage(null, DomainName.TEST, "00");
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getMessage(ServiceName.VITAM, null, "00");
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getMessage(ServiceName.VITAM, DomainName.TEST, "");
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getMessage(ServiceName.VITAM, DomainName.TEST, null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMessageNotFoundTest() {
        VitamCodeHelper.getMessage(ServiceName.VITAM, DomainName.TEST, "ZZ");
    }

    @Test
    public void getMessageTest() {
        final String message = VitamCodeHelper.getMessage(ServiceName.VITAM, DomainName.TEST, "00");
        assertNotNull(message);
        assertEquals(VitamCode.TEST.getMessage(), message);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMessageFromStringEmptyParameterTest() {
        VitamCodeHelper.getMessage("ZZZZZZ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMessageFromStringNullParameterTest() {
        VitamCodeHelper.getMessage(null);
    }

    @Test
    public void getMessageFromStringTest() {
        final String message = VitamCodeHelper.getMessage("000000");
        assertNotNull(message);
        assertEquals(VitamCode.TEST.getMessage(), message);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMessageFromVitamCodeNullParameterTest() {
        VitamCodeHelper.getMessageFromVitamCode(null);
    }

    @Test
    public void getMessageFromVitamCodeTest() {
        final VitamCode code = VitamCodeHelper.getFrom("000000");
        final String message = VitamCodeHelper.getMessageFromVitamCode(code);
        assertNotNull(message);
        assertEquals(VitamCode.TEST.getMessage(), message);
    }

    @Test
    public void getTranslatedMessageFromVitamCodeWrongParametersTest() {
        try {
            VitamCodeHelper.getParametrizedMessageFromVitamCode(null, null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessageFromVitamCode(VitamCode.TEST, null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }
    }

    @Test
    public void getTranslatedMessageFromVitamCodeTest() {
        final String message = VitamCodeHelper.getParametrizedMessageFromVitamCode(VitamCode.TEST, "test");
        assertNotNull(message);
        assertEquals(String.format(VitamCode.TEST.getMessage(), "test"), message);
    }

    @Test
    public void getTranslatedMessageFromCodeWrongParametersTest() {
        try {
            VitamCodeHelper.getParametrizedMessageFromCode(null, null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessageFromCode("", null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }
        try {
            VitamCodeHelper.getParametrizedMessageFromCode("ZZZZZZ", null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessageFromCode("ZZZZZZ", "", "test");
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessageFromCode("ZZZZZZ", "test", null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }
    }

    @Test
    public void getTranslatedMessageFromCodeTest() {
        final String message = VitamCodeHelper.getParametrizedMessageFromCode("000000", "test");
        assertNotNull(message);
        assertEquals(String.format(VitamCode.TEST.getMessage(), "test"), message);
    }

    @Test
    public void getTranslatedMessageWrongParametersTest() {
        try {
            VitamCodeHelper.getParametrizedMessage(null, null, null, null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessage(ServiceName.VITAM, null, null, null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessage(null, DomainName.TEST, null, null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessage(null, null, "ZZ", "test");
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessage(ServiceName.VITAM, DomainName.TEST, null, null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessage(ServiceName.VITAM, DomainName.TEST, "", null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessage(ServiceName.VITAM, DomainName.TEST, "ZZ", null);
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }

        try {
            VitamCodeHelper.getParametrizedMessage(ServiceName.VITAM, DomainName.TEST, "ZZ", "");
            fail("Must thrown an IllegalArgumentException");
        } catch (final IllegalArgumentException exc) {
            // nothing, wanted
        }
    }

    @Test
    public void getTranslatedMessageTest() {
        final String message = VitamCodeHelper.getParametrizedMessage(ServiceName.VITAM, DomainName.TEST, "00", "test");
        assertNotNull(message);
        assertEquals(String.format(VitamCode.TEST.getMessage(), "test"), message);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFromDomainIllegalExceptionTest() {
        VitamCodeHelper.getFromDomain(null);
    }

    @Test
    public void getFromDomainTest() {
        final List<VitamCode> codes = VitamCodeHelper.getFromDomain(DomainName.TEST);
        assertNotNull(codes);
        assertEquals(1, codes.size());
        assertEquals(VitamCode.TEST, codes.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFromServiceIllegalExceptionTest() {
        VitamCodeHelper.getFromService(null);
    }

    @Test
    public void getFromServiceTest() {
        final List<VitamCode> codes = VitamCodeHelper.getFromService(ServiceName.VITAM);
        assertNotNull(codes);
        assertEquals(VitamCode.TEST, codes.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getLogMessageIllegalExceptionTest() {
        VitamCodeHelper.getLogMessage(null);
    }

    @Test
    public void getCodeTest() {
        final String code = VitamCodeHelper.getCode(VitamCode.TEST);
        assertNotNull(code);
        assertEquals("000000", code);
    }

    @Test
    public void getLogMessageTest() {
        final String message = VitamCodeHelper.getLogMessage(VitamCode.TEST, "test");
        assertNotNull(message);
        assertEquals(
            String.format(
                "[%s] %s",
                VitamCodeHelper.getCode(VitamCode.TEST),
                String.format(VitamCode.TEST.getMessage(), "test")
            ),
            message
        );
    }
}
