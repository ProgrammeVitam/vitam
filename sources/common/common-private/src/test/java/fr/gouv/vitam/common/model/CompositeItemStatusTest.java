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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CompositeItemStatusTest {

    private static final String MESSAGE = "message";
    private static final String ITEM_ID_1 = "item_id1";
    private static final String STEP_ID_1 = "step_id1";
    private static final String ITEM_ID_2 = "item_id2";
    private static final String STEP_ID_2 = "step_id2";

    @Test
    public void testCompositeItemStatus() throws Exception {
        final ItemStatus parentItem1 = new ItemStatus(STEP_ID_1);
        assertEquals(StatusCode.UNKNOWN, parentItem1.getGlobalStatus());

        final ItemStatus itemStatus1 = new ItemStatus(ITEM_ID_1);
        itemStatus1.setMessage(MESSAGE);
        itemStatus1.setItemId(ITEM_ID_1);
        final StatusCode statusKO = StatusCode.KO;
        itemStatus1.increment(statusKO);

        parentItem1.setItemsStatus(ITEM_ID_1, itemStatus1);
        assertEquals(StatusCode.KO, parentItem1.getGlobalStatus());

        final ItemStatus parentItem2 = new ItemStatus(STEP_ID_1);
        final StatusCode statusOK = StatusCode.OK;
        final ItemStatus itemStatus2 = new ItemStatus(ITEM_ID_2);
        itemStatus2.increment(statusOK);
        parentItem2.setItemsStatus(ITEM_ID_2, itemStatus2);
        parentItem1.setItemsStatus(parentItem2);

        assertEquals(StatusCode.KO, parentItem1.getGlobalStatus());

        final ItemStatus parentItem3 = new ItemStatus(STEP_ID_1);
        final ItemStatus itemStatus3 = new ItemStatus(ITEM_ID_1);
        itemStatus3.increment(statusKO);
        parentItem3.setItemsStatus(ITEM_ID_1, itemStatus3);
        parentItem1.setItemsStatus(parentItem3);

        assertEquals(StatusCode.KO, parentItem1.getGlobalStatus());
        int i = 0;

        for (final Entry<String, ItemStatus> itemStatus : parentItem1.getItemsStatus().entrySet()) {
            if (i == 0) {
                if (itemStatus.getValue().getItemId().equals(ITEM_ID_1)) {
                    i++;
                } else {
                    fail();
                }
            } else if (i == 1) {
                if (!itemStatus.getValue().getItemId().equals(ITEM_ID_2)) {
                    fail();
                }
            }
        }

        final List<Integer> statusMeter = Arrays.asList(0, 0, 1, 0, 0, 0);
        final ItemStatus parentItem4 = new ItemStatus(
            STEP_ID_2,
            "message",
            StatusCode.OK,
            statusMeter,
            new HashMap<>(),
            new LinkedHashMap<>(),
            ProcessState.COMPLETED
        );
        assertEquals(StatusCode.OK, parentItem4.getGlobalStatus());
    }
}
