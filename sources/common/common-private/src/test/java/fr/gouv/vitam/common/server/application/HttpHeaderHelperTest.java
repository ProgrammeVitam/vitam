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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpHeaderHelperTest {

    private static HttpHeaders httpHeadersMock;

    @Before
    public void init() {
        httpHeadersMock = Mockito.mock(HttpHeaders.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVitamHeadersNullTest() {
        HttpHeaderHelper.checkVitamHeaders(null);
    }

    @Test
    public void checkAllVitamHeadersRegexValidTest() {
        final MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        // correct if at least one of the value matches a the regex (same rule for all vitam header)
        map.put(VitamHttpHeader.METHOD_OVERRIDE.getName(), Collections.singletonList("get"));
        map.put(VitamHttpHeader.STRATEGY_ID.getName(), Collections.singletonList("an id"));
        map.put(VitamHttpHeader.TENANT_ID.getName(), Collections.singletonList("an id"));
        Mockito.when(httpHeadersMock.getRequestHeaders()).thenReturn(map);
        HttpHeaderHelper.checkVitamHeaders(httpHeadersMock);
    }

    @Test
    public void checkAllVitamHeadersRegexInValidTest() {
        final MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        Mockito.when(httpHeadersMock.getRequestHeaders()).thenReturn(map);
        final List<String> emptyStringList = Collections.singletonList("");
        testInvalidHeader(map, VitamHttpHeader.METHOD_OVERRIDE.getName(), Collections.singletonList("bla"));
        testInvalidHeader(map, VitamHttpHeader.STRATEGY_ID.getName(), emptyStringList);
        testInvalidHeader(map, VitamHttpHeader.TENANT_ID.getName(), emptyStringList);
    }

    private void testInvalidHeader(MultivaluedMap<String, String> map, String headerName, List<String> headerValues) {
        map.clear();
        map.put(headerName, headerValues);
        try {
            HttpHeaderHelper.checkVitamHeaders(httpHeadersMock);
            fail("Should raise an exception because header is invalid");
        } catch (final IllegalStateException exc) {
            // Expected exception
        }
    }

    @Test
    public void checkVitamHeadersTest() {
        final MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.put(VitamHttpHeader.METHOD_OVERRIDE.getName(), Collections.singletonList("get"));
        map.put(VitamHttpHeader.STRATEGY_ID.getName(), Collections.singletonList("get"));
        map.put(VitamHttpHeader.TENANT_ID.getName(), Collections.singletonList("get"));
        Mockito.when(httpHeadersMock.getRequestHeaders()).thenReturn(map);
        HttpHeaderHelper.checkVitamHeaders(httpHeadersMock);
    }

    @Test
    public void getHeaderValuesTest() {
        Mockito.when(httpHeadersMock.getRequestHeader(VitamHttpHeader.METHOD_OVERRIDE.getName())).thenReturn(
            Collections.singletonList("get")
        );
        final List<String> values = HttpHeaderHelper.getHeaderValues(httpHeadersMock, VitamHttpHeader.METHOD_OVERRIDE);
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("get", values.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getHeaderValuesStringNullTest() {
        HttpHeaderHelper.getHeaderValues(httpHeadersMock, (String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getHeaderValuesEnumNullTest() {
        HttpHeaderHelper.getHeaderValues(httpHeadersMock, (VitamHttpHeader) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getHeaderValuesHeaderNullTest() {
        HttpHeaderHelper.getHeaderValues(null, VitamHttpHeader.STRATEGY_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateHeaderNullTest() {
        HttpHeaderHelper.validateHeaderValue(null, new MultivaluedHashMap<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateHeaderValueNullTest() {
        Mockito.when(httpHeadersMock.getRequestHeader(VitamHttpHeader.METHOD_OVERRIDE.getName())).thenReturn(null);
        final MultivaluedHashMap<String, String> wanted = new MultivaluedHashMap<>();
        wanted.add(VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET);
        HttpHeaderHelper.validateHeaderValue(httpHeadersMock, wanted);
    }

    @Test
    public void validateHeaderValueTest() {
        Mockito.when(httpHeadersMock.getRequestHeader(VitamHttpHeader.METHOD_OVERRIDE.getName())).thenReturn(
            Collections.singletonList("get")
        );
        final MultivaluedHashMap<String, String> wanted = new MultivaluedHashMap<>();
        wanted.add(VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET);

        HttpHeaderHelper.validateHeaderValue(httpHeadersMock, wanted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateHeaderValueWantedMatchErrorTest() {
        Mockito.when(httpHeadersMock.getRequestHeader(VitamHttpHeader.METHOD_OVERRIDE.getName())).thenReturn(
            Collections.singletonList("get")
        );
        final MultivaluedHashMap<String, String> wanted = new MultivaluedHashMap<>();
        wanted.add(VitamHttpHeader.METHOD_OVERRIDE.getName(), "got");
        HttpHeaderHelper.validateHeaderValue(httpHeadersMock, wanted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateHeaderValueMatchErrorTest() {
        Mockito.when(httpHeadersMock.getRequestHeader(VitamHttpHeader.METHOD_OVERRIDE.getName())).thenReturn(
            Collections.singletonList("got")
        );
        final MultivaluedHashMap<String, String> wanted = new MultivaluedHashMap<>();
        wanted.add(VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET);
        HttpHeaderHelper.validateHeaderValue(httpHeadersMock, wanted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasValueForVitamHeaderNullTest() {
        HttpHeaderHelper.hasValuesFor(null, null);
    }

    @Test
    public void hasValueForTest() {
        Mockito.when(httpHeadersMock.getRequestHeader(VitamHttpHeader.METHOD_OVERRIDE.getName())).thenReturn(
            Collections.singletonList("get")
        );
        assertTrue(HttpHeaderHelper.hasValuesFor(httpHeadersMock, VitamHttpHeader.METHOD_OVERRIDE));
    }
}
