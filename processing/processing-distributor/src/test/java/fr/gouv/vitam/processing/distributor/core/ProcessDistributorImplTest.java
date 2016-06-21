/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.processing.distributor.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.Distribution;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.worker.core.WorkerImpl;
import fr.gouv.vitam.processing.worker.handler.ExtractSedaActionHandler;

public class ProcessDistributorImplTest {
    private ProcessDistributorImpl processDistributorImpl;
    private WorkParams params;
    private static final String WORKFLOW_ID = "workflowJSONv1";

    @Before
    public void setUp() throws Exception {
        params = new WorkParams().setServerConfiguration(new ServerConfiguration()).setGuuid("aa125487");
    }

    @Test
    public void givenProcessDistributorWhendistributeThenCatchTheOtherException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        final Step step = new Step();
        step.setStepName("Traiter_archives");
        final List<Action> actions = new ArrayList<Action>();
        final Action action = new Action();
        action.setActionKey(ExtractSedaActionHandler.getId());
        System.out.println(action.getActionKey());
        actions.add(action);
        step.setActions(actions);

        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenProcessDistributorWhendistributeThenCatchIllegalArgumentException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        final Step step = new Step();
        final Action a = new Action();
        a.setActionKey("notExist");
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, null, WORKFLOW_ID);
    }

    @Test
    public void givenProcessDistributorWhenDistributeWithListKindThenCatchHandlerNotFoundException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        final Step step = new Step().setDistribution(new Distribution().setKind(DistributionKind.LIST));
        final Action a = new Action();
        a.setActionKey("notExist");
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }

    @Test
    public void givenProcessDistributorWhenDistributeWithRefThenCatchHandlerNotFoundException() {
        processDistributorImpl = new ProcessDistributorImplFactory().create();
        final Step step = new Step().setDistribution(new Distribution().setKind(DistributionKind.REF));
        final Action a = new Action();
        a.setActionKey("notExist");
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        processDistributorImpl.distribute(params, step, WORKFLOW_ID);
    }

    @Test
    public void test() throws IllegalArgumentException, ProcessingException {
        final WorkerImpl worker = mock(WorkerImpl.class);
        final List<EngineResponse> response = new ArrayList<EngineResponse>();
        response.add(new ProcessResponse().setStatus(StatusCode.OK));
        when(worker.run(anyObject(), anyObject())).thenReturn(response);

        processDistributorImpl = new ProcessDistributorImplFactory().create(worker);
        processDistributorImpl.distribute(params, new Step(), WORKFLOW_ID);
    }
}
