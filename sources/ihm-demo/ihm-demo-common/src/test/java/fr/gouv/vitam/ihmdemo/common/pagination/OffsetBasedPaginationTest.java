package fr.gouv.vitam.ihmdemo.common.pagination;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;

import javax.ws.rs.core.HttpHeaders;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;

public class OffsetBasedPaginationTest {

    private static HttpHeaders httpHeadersMock;

    @Before
    public void init() {
        httpHeadersMock = Mockito.mock(HttpHeaders.class);
    }

    @Test
    public void testPaginationConstructor(){
        OffsetBasedPagination paginationEmptyConstructor = new OffsetBasedPagination();
        assertEquals(PaginationParameters.DEFAULT_OFFSET, paginationEmptyConstructor.getOffset());
        assertEquals(PaginationParameters.DEFAULT_LIMIT, paginationEmptyConstructor.getLimit());

        OffsetBasedPagination paginationOffsetLimit = new OffsetBasedPagination(11, 100);
        assertEquals(11, paginationOffsetLimit.getOffset());
        assertEquals(100, paginationOffsetLimit.getLimit());
        assertEquals(0, paginationOffsetLimit.getTotal());

        paginationOffsetLimit = new OffsetBasedPagination(12, 101,1000);
        assertEquals(12, paginationOffsetLimit.getOffset());
        assertEquals(101, paginationOffsetLimit.getLimit());
        assertEquals(1000, paginationOffsetLimit.getTotal());

        Mockito.when(httpHeadersMock.getRequestHeader(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            Collections.singletonList("1"));
        Mockito.when(httpHeadersMock.getRequestHeader(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            Collections.singletonList("2"));

        try {
            OffsetBasedPagination paginationHeaders= new OffsetBasedPagination(httpHeadersMock);
            assertEquals(1, paginationHeaders.getOffset());
            assertEquals(2, paginationHeaders.getLimit());
            assertEquals(0, paginationHeaders.getTotal());
        } catch (VitamException e) {
            fail();
        }


        Mockito.when(httpHeadersMock.getRequestHeader(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            Collections.singletonList("1A"));

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (VitamException e) {
        }

        Mockito.when(httpHeadersMock.getRequestHeader(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            Collections.singletonList("-1"));

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (VitamException e) {
        }


        Mockito.when(httpHeadersMock.getRequestHeader(IhmWebAppHeader.OFFSET.getName())).thenReturn(
            Collections.singletonList("1"));
        Mockito.when(httpHeadersMock.getRequestHeader(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            Collections.singletonList("2A"));

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (VitamException e) {
        }

        Mockito.when(httpHeadersMock.getRequestHeader(IhmWebAppHeader.LIMIT.getName())).thenReturn(
            Collections.singletonList("0"));

        try {
            new OffsetBasedPagination(httpHeadersMock);
            fail();
        } catch (VitamException e) {
        }

    }
}
