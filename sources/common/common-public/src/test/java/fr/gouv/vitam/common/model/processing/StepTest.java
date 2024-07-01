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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class StepTest {

    private static final String TEST = "test";

    @Test
    public void testConstructor() {
        assertEquals("", new Step().getStepName());
        assertEquals(null, new Step().getBehavior());
        assertEquals("DefaultWorker", new Step().getWorkerGroupId());
        assertEquals(true, new Step().getActions().isEmpty());
        assertEquals(DistributionKind.REF, new Step().getDistribution().getKind());
        assertEquals(
            DistributionKind.LIST_ORDERING_IN_FILE,
            new Step()
                .setDistribution(new Distribution().setKind(DistributionKind.LIST_ORDERING_IN_FILE))
                .getDistribution()
                .getKind()
        );

        final List<Action> actions = new ArrayList<>();
        actions.add(new Action());
        assertEquals(TEST, new Step().setStepName(TEST).getStepName());
        assertEquals(ProcessBehavior.BLOCKING, new Step().setBehavior(ProcessBehavior.BLOCKING).getBehavior());
        assertEquals(TEST, new Step().setWorkerGroupId(TEST).getWorkerGroupId());
        assertEquals(false, new Step().setActions(actions).getActions().isEmpty());
    }

    @Test
    public void should_not_log_id_we_distribute_only_one_element() {
        // Given
        Distribution distribution = new Distribution();
        distribution.setKind(DistributionKind.REF);

        Action action = new Action();
        action.setActionDefinition(
            new ActionDefinition("actionKey", ProcessBehavior.NOBLOCKING, null, new ArrayList<>(), new ArrayList<>())
        );
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(action);
        Step step = new Step(null, "workerId", "stepName", ProcessBehavior.NOBLOCKING, distribution, actions, null);
        step.defaultLifecycleLog(LifecycleState.TEMPORARY);

        // When
        boolean lifecycleEnabled = action.getActionDefinition().lifecycleEnabled();

        // Then
        assertThat(lifecycleEnabled).isFalse();
    }
}
