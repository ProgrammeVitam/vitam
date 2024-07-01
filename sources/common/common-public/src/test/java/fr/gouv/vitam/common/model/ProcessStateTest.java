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
package fr.gouv.vitam.common.model;

import fr.gouv.vitam.common.exception.StateNotAllowedException;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class ProcessStateTest {

    @Test
    public void pauseEvalPauseStateThenOK() throws Exception {
        assertThatCode(() -> ProcessState.PAUSE.eval(ProcessState.PAUSE)).doesNotThrowAnyException();
    }

    @Test
    public void pauseEvalCompletedStateThenOK() throws Exception {
        assertThatCode(() -> ProcessState.PAUSE.eval(ProcessState.COMPLETED)).doesNotThrowAnyException();
    }

    @Test
    public void pauseEvalRunningStateThenOK() throws Exception {
        assertThatCode(() -> ProcessState.PAUSE.eval(ProcessState.RUNNING)).doesNotThrowAnyException();
    }

    @Test
    public void runningEvalPauseStateThenOK() throws Exception {
        assertThatCode(() -> ProcessState.RUNNING.eval(ProcessState.PAUSE)).doesNotThrowAnyException();
    }

    @Test
    public void runningEvalCompletedStateThenOK() throws Exception {
        assertThatCode(() -> ProcessState.RUNNING.eval(ProcessState.COMPLETED)).doesNotThrowAnyException();
    }

    @Test(expected = StateNotAllowedException.class)
    public void runningEvalRunningStateThenOK() throws Exception {
        ProcessState.RUNNING.eval(ProcessState.RUNNING);
    }

    @Test(expected = StateNotAllowedException.class)
    public void completedEvalPauseStateThenOK() throws Exception {
        ProcessState.COMPLETED.eval(ProcessState.PAUSE);
    }

    @Test(expected = StateNotAllowedException.class)
    public void completedEvalCompletedStateThenOK() throws Exception {
        ProcessState.COMPLETED.eval(ProcessState.COMPLETED);
    }

    @Test(expected = StateNotAllowedException.class)
    public void completedEvalRunningStateThenOK() throws Exception {
        ProcessState.COMPLETED.eval(ProcessState.RUNNING);
    }

    @Test
    public void should_convert_workflow_status_to_http_status() throws Exception {
        assertThat(ProcessState.RUNNING.getEquivalentHttpStatus()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(ProcessState.PAUSE.getEquivalentHttpStatus()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(ProcessState.COMPLETED.getEquivalentHttpStatus()).isEqualTo(Response.Status.OK);
    }
}
