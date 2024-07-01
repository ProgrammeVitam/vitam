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

/**
 * Enum of process workflow execution mode
 */
public enum ProcessAction {
    /**
     * initial execution mode for process workflow
     */
    INIT("init"),
    /**
     * execute only next step
     */
    NEXT("next"),
    /**
     * resume execution mode (all step will be executed)
     */
    RESUME("resume"),
    /**
     * pause execution mode
     */
    PAUSE("pause"),
    /**
     * execute one more time last executed step
     */
    REPLAY("replay"),
    /**
     * start execution mode for process workflow
     */
    START("start");

    private String value;

    /**
     * Constructor
     *
     * @param processAction
     */

    ProcessAction(String processAction) {
        this.value = processAction;
    }

    /**
     * @return value of process workflow in String format
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Returns Enum from value
     *
     * @param value of process workflow in String format
     * @return ProcessAction
     */
    public static ProcessAction getProcessAction(String value) {
        if (value != null) {
            for (ProcessAction action : ProcessAction.values()) {
                if (value.equalsIgnoreCase(action.value)) {
                    return action;
                }
            }
        }
        return null;
    }
}
