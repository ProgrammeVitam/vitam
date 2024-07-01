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

import javax.ws.rs.core.Response;

/**
 * The different states of the ProcessWorkflow
 *
 * For each state (PAUSE, RUNNING, COMPLETED) we can send an number of events.
 */
public enum ProcessState {
    /**
     * Represent the current pause state Accept all Pause =&gt; do nothing Resume =&gt; change the state to running then run
     * all steps at the end change step to completed Next =&gt; Change the state to running then run the next step only. If
     * the last step then change the state to completed else change the state to pause Cancel =&gt; Change the state to
     * completed
     */
    PAUSE {
        @Override
        public void eval(ProcessState targetState) throws StateNotAllowedException {
            if (null == targetState) {
                throw new StateNotAllowedException("ProcessState must not be null");
            }
            switch (targetState) {
                case RUNNING:
                case PAUSE:
                case COMPLETED:
                    break;
                default:
                    super.eval(targetState);
            }
        }
    },
    /**
     * Represent the current running state Accept Pause and cancel Pause =&gt; continue execution of current step (only for
     * step by step) then change state to : - pause if not last step - completed Cancel =&gt; continue execution of current
     * process (step or all steps) then change state to complete Throws StateNotAllowedException for other
     */
    RUNNING {
        @Override
        public void eval(ProcessState targetState) throws StateNotAllowedException {
            if (null == targetState) {
                throw new StateNotAllowedException("ProcessState must not be null");
            }
            switch (targetState) {
                case PAUSE:
                case COMPLETED:
                    break;
                default:
                    super.eval(targetState);
            }
        }
    },

    /**
     * Represent the current completed state Throws StateNotAllowedException for all
     */
    COMPLETED;

    /**
     * Evaluate for the current state if the given state is permitted or not
     *
     * @throws StateNotAllowedException
     */
    public void eval(ProcessState targetState) throws StateNotAllowedException {
        throw new StateNotAllowedException(
            "The processState " + targetState.name() + " is not allowed for the current state " + this.name()
        );
    }

    /**
     * get equivalent http status
     *
     * @return the status
     */
    public Response.Status getEquivalentHttpStatus() {
        switch (this) {
            case COMPLETED:
                return Response.Status.OK;
            default:
                return Response.Status.ACCEPTED;
        }
    }
}
