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
package fr.gouv.vitam.ihmdemo.common.pagination;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OffsetBasedPaginationTest {

    private static HttpServletRequest httpHeadersMock;

    @Before
    public void init() {
        httpHeadersMock = Mockito.mock(HttpServletRequest.class);
    }

    @Test
    public void testPaginationConstructor() {
        final OffsetBasedPagination paginationEmptyConstructor = new OffsetBasedPagination();
        assertEquals(PaginationParameters.DEFAULT_OFFSET, paginationEmptyConstructor.getOffset());
        assertEquals(PaginationParameters.DEFAULT_LIMIT, paginationEmptyConstructor.getLimit());

        OffsetBasedPagination paginationOffsetLimit = new OffsetBasedPagination(11, 100);
        assertEquals(11, paginationOffsetLimit.getOffset());
        assertEquals(100, paginationOffsetLimit.getLimit());
        assertEquals(0, paginationOffsetLimit.getTotal());

        paginationOffsetLimit = new OffsetBasedPagination(12, 101, 1000);
        assertEquals(12, paginationOffsetLimit.getOffset());
        assertEquals(101, paginationOffsetLimit.getLimit());
        assertEquals(1000, paginationOffsetLimit.getTotal());

        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            new Vector(Collections.singletonList("1")).elements()
        );
        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            new Vector(Collections.singletonList("2")).elements()
        );

        try {
            final OffsetBasedPagination paginationHeaders = new OffsetBasedPagination(httpHeadersMock);
            assertEquals(1, paginationHeaders.getOffset());
            assertEquals(2, paginationHeaders.getLimit());
            assertEquals(0, paginationHeaders.getTotal());
        } catch (final VitamException e) {
            fail();
        }

        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            new Vector(Collections.singletonList("1A")).elements()
        );

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (final VitamException e) {}

        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            new Vector(Collections.singletonList("-1")).elements()
        );

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (final VitamException e) {}

        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            new Vector(Collections.singletonList("1")).elements()
        );
        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            new Vector(Collections.singletonList("2A")).elements()
        );

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (final VitamException e) {}

        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            new Vector(Collections.singletonList("0")).elements()
        );

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (final VitamException e) {}
    }
}
