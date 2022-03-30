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
package fr.gouv.vitam.common.model.processing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WorkFlowTest {
    private static final String TEST = "test";

    @Test
    public void testConstructor() {
        assertEquals("", new WorkFlow().getComment());
        assertEquals("", new WorkFlow().getId());
        assertEquals(true, new WorkFlow().getSteps().isEmpty());

        final List<Step> steps = new ArrayList<>();
        steps.add(new Step().setStepName(TEST));

        assertEquals(TEST, new WorkFlow().setComment(TEST).getComment());
        assertEquals(TEST, new WorkFlow().setId(TEST).getId());
        assertEquals(false, new WorkFlow().setSteps(steps).getSteps().isEmpty());
        assertEquals("ID=test\nname=test\nidentifier=test\ntypeProc=test\ncomments=test\nlifecycleLog=FINAL\n",
            new WorkFlow().setId(TEST).setIdentifier(TEST).setName(TEST).setTypeProc(TEST).setComment(TEST)
                .setLifecycleLog(LifecycleState.FINAL).toString());

        WorkFlow workflow =
            new WorkFlow().setId(TEST).setIdentifier(TEST).setName(TEST).setTypeProc(TEST).setComment(TEST);

    }

}
