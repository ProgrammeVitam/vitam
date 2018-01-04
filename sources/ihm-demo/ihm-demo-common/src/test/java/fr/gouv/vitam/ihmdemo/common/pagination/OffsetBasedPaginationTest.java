package fr.gouv.vitam.ihmdemo.common.pagination;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
            new Vector(Collections.singletonList("1")).elements());
        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            new Vector(Collections.singletonList("2")).elements());

        try {
            final OffsetBasedPagination paginationHeaders = new OffsetBasedPagination(httpHeadersMock);
            assertEquals(1, paginationHeaders.getOffset());
            assertEquals(2, paginationHeaders.getLimit());
            assertEquals(0, paginationHeaders.getTotal());
        } catch (final VitamException e) {
            fail();
        }


        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            new Vector(Collections.singletonList("1A")).elements());

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (final VitamException e) {}

        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            new Vector(Collections.singletonList("-1")).elements());

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (final VitamException e) {}


        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            new Vector(Collections.singletonList("1")).elements());
        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            new Vector(Collections.singletonList("2A")).elements());

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (final VitamException e) {}

        Mockito.when(httpHeadersMock.getHeaders(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            new Vector(Collections.singletonList("0")).elements());

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (final VitamException e) {}

    }
}
