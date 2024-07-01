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
package fr.gouv.vitam.common.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class ItemStatusTest {

    private static final String MESSAGE = "message";
    private static final String ITEM_ID_1 = "id1";

    @Test
    public void testItemStatus() throws Exception {
        final ItemStatus itemStatus1 = new ItemStatus(ITEM_ID_1);
        assertEquals(StatusCode.UNKNOWN, itemStatus1.getGlobalStatus());

        assertEquals(StatusCode.FATAL.getStatusLevel() + 1, itemStatus1.getStatusMeter().size());
        assertEquals(0, itemStatus1.getData().size());
        itemStatus1.setMessage(MESSAGE);
        assertEquals(MESSAGE, itemStatus1.getMessage());

        itemStatus1.setItemId(ITEM_ID_1);
        assertEquals(ITEM_ID_1, itemStatus1.getItemId());

        final StatusCode statusKO = StatusCode.KO;
        itemStatus1.increment(statusKO);
        assertEquals(StatusCode.KO, itemStatus1.getGlobalStatus());

        assertEquals(Integer.valueOf(1), itemStatus1.getStatusMeter().get(StatusCode.KO.getStatusLevel()));

        itemStatus1.setData("key", "value");
        assertEquals("value", itemStatus1.getData("key"));

        final ItemStatus itemStatus2 = new ItemStatus(ITEM_ID_1);
        final StatusCode statusOK = StatusCode.OK;
        itemStatus1.increment(statusOK);

        final ItemStatus itemStatus3 = itemStatus2.increment(itemStatus1, itemStatus2);
        assertEquals(Integer.valueOf(1), itemStatus3.getStatusMeter().get(StatusCode.OK.getStatusLevel()));
        assertEquals(Integer.valueOf(1), itemStatus3.getStatusMeter().get(StatusCode.KO.getStatusLevel()));
        assertEquals(StatusCode.KO, itemStatus3.getGlobalStatus());

        final List<Integer> statusMeter = Arrays.asList(0, 0, 1, 0, 0, 0);

        final ItemStatus itemStatus4 = new ItemStatus(
            ITEM_ID_1,
            "message",
            StatusCode.OK,
            statusMeter,
            new HashMap<>(),
            null,
            ProcessState.COMPLETED
        );
        assertEquals(StatusCode.OK, itemStatus4.getGlobalStatus());
    }

    @Test
    public void should_stop_when_blocking_and_KO() throws Exception {
        ItemStatus itemStatus = new ItemStatus();
        ItemStatus increment = itemStatus.increment(StatusCode.KO);

        assertThat(increment.shallStop(true)).isTrue();
    }
}
